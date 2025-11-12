# URL Shortener Exercise

This is a basic URL shortener, divided into frontend (Next.js) and backend (Kotlin).

## Running Locally

To run the backend locally, navigate to the backend directory and use `./gradlew run` or `./gradlew test`; it will run at http://localhost:8080.

To run the frontend locally, navigate to the frontend directory and use `pnpm run dev`; it will run at http://localhost:3000. Set `API_BASE_URL` if you need the frontend to talk to a backend that is not on `http://localhost:8080`.

### Docker

You can containerise both services individually:

```bash
docker build -t url-shortener-backend ./backend
docker run --rm -p 8080:8080 -e REDIS_URL=redis://host.docker.internal:6379 url-shortener-backend
```

```bash
docker build -t url-shortener-frontend ./frontend
docker run --rm -p 3000:3000 url-shortener-frontend
```

The backend expects Redis to be reachable at the value of `REDIS_URL` (defaults to `redis://localhost:6379`); adjust the hostname to match your environment or compose setup.

### Docker Compose

To launch the entire stack (frontend, backend, Redis, reverse proxy) run:

```bash
docker compose up --build
```

The proxy listens on port 80 and routes traffic to the Next.js and Ktor services according to the policy above. Environment variables are already wired so the backend points to the internal Redis instance and the frontend uses the backend’s internal hostname.

# Architecture

I designed the three system layers (frontend -> backend -> database) to scale horizontally and independently. I inferred that a URL shortener could be deployed at enormous scale, serving as the entrypoint to a company's systems. Hence, I looked for a K-V or point-read database rather than a traditional relational database.

### Routing

The original spec requires that the service exposes a SPA frontend and an API on the same host that has a catch-all route at the root. My best approximation for this is a reverse proxy (nginx) configuration as follows:

```
# --- Static assets and Next.js routes ---
location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|webp|woff2?)$ {
    proxy_pass http://nextjs:3000;
}

# Root or known front-end paths (SSR, client routes)
location = / { proxy_pass http://nextjs:3000; }
location /_next/ { proxy_pass http://nextjs:3000; }
location /static/ { proxy_pass http://nextjs:3000; }

# --- Everything else goes to Kotlin ---
location / {
    proxy_pass http://ktor:8080;
}
```

This is an extremely brittle approach and with more time, I would either:

1) suggest an alteration to the requirement such that the two services are on the same host, or move the API to a directory like `/api/`
2) simplify the SPA to not need routing outside of an explicit set of assets (e.g. a react app without routing) -- possibly directly served by nginx, negating the maintenance overhead of validating that the reverse proxy policy matches the file structure of the SPA
3) require short URLs to have a symbol differentiating them
4) change the reverse proxy policy to attempt to load the SPA frontend and send failed routes to the API
5) the embedded server in Ktor has a singlePageApp mode where the kotlin app can provide the routing itself without need for a reverse proxy

### ID Structure: NanoID x16

I built the service to scale to multiple replicas, which put emphasis on ID generation. Most URL shorteners use an autoincremented ID, and then base-64 encode it. This is entirely possible to achieve, but becomes more difficult at scale.

I started in this direction, and would originally have used a distributed counter. Since I had chosen Redis as an option for the persistence layer, I validated that this would be possible. Redis has a very performent `INCR` command which adds 1 to a counter and returns the new number. The client can then use the result and base-64 encode it to create an alias. After relevant business logic (like checking for collisions with custom aliases), it can store that as a point-write into Redis. There is a risk that a client consumes a number (by incrementing the counter) and not using it after consuming it, but in that case, there isn't much cost to losing a small proportion of the ID space, assuming it's uncommon.

If using a distributed incrementor along with Redis Cluster-ing (see below), the incrementor would concentrate all of the increment+read operations to a single node. Thankfully, for a URL shortener, we can assume that there is a `1 write : 1000 reads` ratio, so it's unlikely to affect the broader architecture. If it became a problem (1000+ new URLs per second), a separate counter could be spun out.

There is a second distributed ID generation algorithm - UUIDs. The benefit of these in distributed or replicated systems is that systems can generate IDs independently with no information about the other systems. Generally, we use UUIDs as specified by RFC 9562 because the format standard is effectively universal and the risk of collision is negligible, but these aren't useful for a URL shortener where information density is important. It wouldn't be helpful if the URL shortener produced URLs longer than the original URL.

There is a project called Nano ID which generates purely random IDs given a dictionary and length. I arbitrarily chose a dictionary of `a-zA-Z0-9` and a length of 16. At this density, the system can shorten 1000 URLs per second for 1000 years before 1% of a collision (around 40 trillion IDs).

### Database: Redis

After a little research I settled on Redis with AOT writes. Redis already has excellent point-read performance, so in most cases a single node (or a small number for availability) is acceptable. I did evaluate relational databases (postgres - good horizontal scaling but performance limited), embedded database (RocksDB - very fast but hard ceiling on scale, also Badger and SQLLite) and KV stores (Redis, TiKV).

For the purpose of the exercise I have used Redis with no storage attached for ease of testing. However, Redis is only feasible for this usecase because it can be configured in AOT persistence mode. In this mode, Redis logs all requests before a write is confirmed to the client. This guarantees that, at least for the node being written to, that the URL alias has been persisted. The trade-off of this mode is that writes are slower, but reads are unimpeded. For a URL shortener, it's likely that read performance is a critical concern, but slightly slow writes is acceptable. For scale, a slow write in this case will outperform a fast relational database.

Redis Cluster divides all keys into 16384 buckets; each key is CRC-hashed to calculate which node the key belongs on. To force colocation of related keys (for performance and to enable pipelined commands), I used the explicit shard notation `redis.set("alias:{$key}", ...)` in the key to direct Redis from where to draw the hash. This way, if there is a future requirement like, say, getting the alias for a URL (a reverse query) to reuse aliases if a URL has already been shortened, the orginal record has key `alias:{myAlias}` the reverse record can be `url:{hashedurl}` (would recommend a SHA256 or similar).

Cloud providers like Azure have native managed services for Redis, if necessary.

Redis has pipelined queries but does not have transactional integrity. However, the risks can be circumvented with careful design. In this case, there's no complex writes that need that integrity in the spec.

## Cloud Deployment

Being that there are five components to deploy, the system is quite agnostic to cloud providers. All of the components are containerised and can be deployed to Kubernetes or a managed equivalent. For instance, in Azure: Azure Kubernetes Service or Azure Container Apps. Furthermore, all cloud providers have first-party managed database services which are either managed Redis, or managed services with a Redis-compatible protocol. These tend to cost more to operate but in my experience, using them comes with compliance and SLA guarantees that clients would prefer over managing their own persistence layer.

In addition, the reverse proxy as specified above can be replaced by a first-party CDN+routing offering in the cloud provider.

Whatever you use, I suggest that you use Terraform (IaC) and an OpenTelemetry collector (obervability). For instance, Azure Monitor + App Insights.

## Drawbacks and further work

With more time, there are a lot of enhancements I would like to provide; I have listed them as follows:

### More unit test coverage

I am very disappointed with the test coverage of the backend. This is my first time writing in Kotlin or even Java for a very long time, and I spend a significant amount of time (a working day) trying to get my IDE (VS Code) to work for JDK development. After that amount of time, I had managed to get basic syntax highlighting working for the main app, but the IDE made it almost impossible to write tests. I wrote as many tests as I could to prove that I understand how unit/integration tests work, but I am afraid that coverage is bad.

### Frontend library choice

After spending an extraordinary amount of time trying to learn a new language and fight my IDE, I made a conservative choice for the frontend and built it using Next.js because I'm very familiar with it. If I had more time, I would prefer to make it a simple React app without the extra features of Next.js. There's no need to use a backend for frontend in this architecture, and it added substantial complexity, not least because of the routing complexity in the reverse proxy, which currently uses a heuristic to decide where to route traffic.

### Observability

I am a huge advocate for OpenTelemetry and I would have loved to include it. My understanding is that it's trivial to add it to Ktor for collecting spans, so I decided it wasn't worth the time investment of adding instrumentation to the API.