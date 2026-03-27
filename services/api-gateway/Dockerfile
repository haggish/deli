# ── Stage 1: extract layered JAR ──────────────────────────────────────────────
# Spring Boot produces a layered JAR. We extract the layers so Docker can cache
# dependencies separately from application code. Only the "application" layer
# changes on most code edits — dependencies and Spring loader stay cached.
FROM eclipse-temurin:21-jre-alpine AS builder
WORKDIR /app
COPY build/libs/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# ── Stage 2: lean runtime image ───────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user — never run as root in containers
RUN addgroup -S deli && adduser -S deli -G deli
USER deli

# Copy layers from largest-to-smallest change frequency.
# Docker invalidates cache from the first changed layer downward.
COPY --from=builder --chown=deli:deli /app/dependencies/          ./
COPY --from=builder --chown=deli:deli /app/spring-boot-loader/    ./
COPY --from=builder --chown=deli:deli /app/snapshot-dependencies/ ./
COPY --from=builder --chown=deli:deli /app/application/           ./

# Actuator health port exposed for Kubernetes liveness/readiness probes
EXPOSE 8080

# JVM tuning for containers:
#   UseContainerSupport — respect cgroup CPU/memory limits (default in JDK 11+, explicit here)
#   MaxRAMPercentage    — use 75% of container memory for heap
#   ExitOnOutOfMemoryError — crash fast rather than limping on OOM
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
