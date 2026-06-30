# Stage 1: Build
FROM gradle:8-jdk21 AS build
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN ./gradlew installDist --no-daemon

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/install/ktor-tgbot2 /app

EXPOSE 8080
ENTRYPOINT ["/app/bin/ktor-tgbot2"]
