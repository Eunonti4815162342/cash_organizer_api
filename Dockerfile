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

# Build application (skip tests for faster deployment)
RUN chmod +x gradlew && \
    ./gradlew clean bootJar -x test --no-daemon

# Runtime stage
FROM bellsoft/liberica-openjre-alpine-musl:17

WORKDIR /app

# Copy built JAR from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD java -jar /app/app.jar --spring.boot.admin.server.enabled=false || exit 1

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
