# Build stage
FROM bellsoft/liberica-openjdk-alpine-musl:17 AS build

WORKDIR /app

# Copy gradle files
COPY gradle gradle
COPY gradlew gradlew
COPY gradlew.bat gradlew.bat
COPY settings.gradle settings.gradle
COPY build.gradle build.gradle

# Copy source code
COPY src src

# Build application — tests are run in the CI pipeline before this stage.
# The image is only built after all tests have passed (unit + integration + E2E).
RUN chmod +x gradlew && \
    ./gradlew clean bootJar -x test --no-daemon

# Runtime stage
FROM bellsoft/liberica-openjre-alpine-musl:17

WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Copy built JAR from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Health check - verify Spring Boot is responsive
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -f http://localhost:8085/actuator/health || exit 1

EXPOSE 8085

# Run with optimized JVM settings for ARM64
ENTRYPOINT ["java", "-XX:+UseStringDeduplication", "-jar", "app.jar"]
