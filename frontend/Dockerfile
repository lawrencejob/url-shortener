# syntax=docker/dockerfile:1

FROM node:22-alpine AS deps
WORKDIR /app

RUN apk add --no-cache libc6-compat \
    && corepack enable

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile


FROM node:22-alpine AS build
WORKDIR /app

RUN corepack enable

COPY --from=deps /app/node_modules ./node_modules
COPY . .

RUN pnpm run build


FROM node:22-alpine AS runner
WORKDIR /app

ENV NODE_ENV=production

RUN corepack enable

COPY --from=deps /app/node_modules ./node_modules
COPY --from=build /app/.next ./.next
COPY --from=build /app/public ./public
COPY package.json pnpm-lock.yaml ./

EXPOSE 3000

CMD ["pnpm", "start"]
