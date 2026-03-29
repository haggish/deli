# ── Stage 1: extract layers from the fat JAR ─────────────────────────────────
# Spring Boot layered JARs split the application into four layers:
#   dependencies    → rarely changes (Maven/Gradle deps)
#   spring-boot-loader → almost never changes
#   snapshot-dependencies → changes on SNAPSHOT bumps
#   application     → changes on every commit (your code)
#
# This means only the application layer is pushed on most builds,
# dramatically reducing push times and layer cache misses.

FROM eclipse-temurin:21-jre-alpine AS builder
WORKDIR /app
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# ── Stage 2: runtime image ────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S deli && adduser -S deli -G deli
USER deli

# Copy layers in dependency order (least → most frequently changing)
COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/application/ ./

# JVM tuning for containers:
#   UseContainerSupport   → respect cgroup memory limits
#   MaxRAMPercentage=75   → use up to 75% of container memory for heap
#   TieredStopAtLevel=1   → faster startup (skip full JIT compilation tier)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:TieredStopAtLevel=1"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
