# syntax=docker/dockerfile:1

# --- build stage ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy build scripts + wrapper first to cache dependency resolution.
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# Copy sources and build the boot jar (tests run separately via `make test`).
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# --- runtime stage ---
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as a non-root user.
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /app/build/libs/*.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
