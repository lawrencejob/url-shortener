# syntax=docker/dockerfile:1

FROM gradle:8.10.2-jdk21 AS build
WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle.kts settings.gradle.kts
COPY gradle.properties gradle.properties
COPY app app

RUN chmod +x gradlew \
    && ./gradlew :app:installDist --no-daemon


FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /opt/app

COPY --from=build /workspace/app/build/install/app ./

EXPOSE 8080

ENTRYPOINT ["./bin/app"]
