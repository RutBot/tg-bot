# Stage 1: Build the bot module fat-jar.
FROM gradle:8.10.2-jdk21 AS build
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN gradle :bot:bootJar --no-daemon

# Stage 2: Run.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/bot/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
