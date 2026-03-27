# deli — Courier Delivery Platform

Monorepo for the Deli courier platform: Kotlin/Spring Boot microservices,
Ionic/Angular/Capacitor mobile app, and Kubernetes/Helm infrastructure.

## Repository layout

```
deli/
├── buildSrc/                   Convention plugins (shared Gradle config)
├── shared/
│   ├── domain-model/           Kotlin data classes, Kafka event DTOs
│   └── common-api/             OpenAPI HTTP request/response models
├── services/
│   ├── api-gateway/            Spring Cloud Gateway — JWT, routing, CORS
│   ├── route-service/          Stop ordering, ETA, Google Maps
│   ├── delivery-service/       Confirmation, photo/signature upload
│   ├── location-service/       WebSocket GPS ingest, Redis, TimescaleDB
│   └── notification-service/   Kafka consumer → FCM push
├── mobile-app/                 Ionic 8 / Angular 19 / Capacitor 6
├── infra/
│   ├── helm/                   Helm umbrella chart (Phase 3)
│   └── docker/                 Init scripts for local Docker containers
├── config/
│   └── detekt/detekt.yml       Kotlin static analysis rules
├── docker-compose.yml          Local infrastructure (Postgres, Kafka, Redis, MinIO)
└── gradle/
    ├── libs.versions.toml      Central dependency version catalog
    └── wrapper/                Gradle wrapper (pins Gradle 8.11.1)
```

---

## Prerequisites

### Windows 11 host

| Tool | Version | Install |
|------|---------|---------|
| WSL 2 | any | `wsl --install` in PowerShell as admin, then reboot |
| Docker Desktop | 4.28+ | https://www.docker.com/products/docker-desktop — enable WSL 2 backend |
| IntelliJ IDEA | 2024.3+ | https://www.jetbrains.com/idea/ — Ultimate recommended (Community works) |
| Git for Windows | latest | https://git-scm.com/download/win |

> **Docker Desktop setting**: Settings → Resources → WSL Integration → enable for your distro.

### Inside WSL 2 (Ubuntu 22.04 recommended)

Open WSL and run these once:

```bash
# 1. Java 21 (Temurin)
sudo apt-get update
sudo apt-get install -y wget apt-transport-https
wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo apt-key add -
echo "deb https://packages.adoptium.net/artifactory/deb jammy main" \
  | sudo tee /etc/apt/sources.list.d/adoptium.list
sudo apt-get update
sudo apt-get install -y temurin-21-jdk

# Verify
java -version    # should print: openjdk version "21.x.x"

# 2. Node 22 (for mobile-app)
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
sudo apt-get install -y nodejs

# Verify
node --version   # should print: v22.x.x
npm --version

# 3. Ionic CLI + Angular CLI
npm install -g @ionic/cli @angular/cli

# 4. kubectl (for local K8s via Docker Desktop)
curl -LO "https://dl.k8s.io/release/$(curl -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install kubectl /usr/local/bin/kubectl
rm kubectl

# 5. Helm
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
```

### WSL 2 memory config (important for 16 GB machines)

Create `C:\Users\mikao\.wslconfig`:

```ini
[wsl2]
memory=8GB
processors=4
swap=2GB
```

Then restart: `wsl --shutdown` in PowerShell, reopen Docker Desktop.

---

## First-time setup

All commands below run **inside WSL 2** unless marked `(PowerShell)`.

### 1. Clone and verify the project opens

```bash
# Clone (adjust path if needed — /mnt/c maps to C:\ in WSL)
cd /mnt/c/Users/mikao/Projects
git clone <your-repo-url> deli
cd deli

# Make wrapper executable (Windows doesn't preserve Unix permissions)
chmod +x gradlew

# Verify Gradle resolves dependencies and compiles
./gradlew build --info 2>&1 | tail -30
```

Expected output ends with `BUILD SUCCESSFUL`.

> **IntelliJ tip**: Open the project at `C:\Users\mikao\Projects\deli` in IDEA.
> When prompted "Trust Gradle project?" — click **Trust**. IDEA will import all
> modules automatically. Use the provided run configurations from the Run menu.

### 2. Start infrastructure

```bash
# From the project root in WSL:
docker compose up -d

# Watch services become healthy (takes ~60s on first run while images pull)
docker compose ps

# You should see all services as "healthy":
# deli-postgres     healthy
# deli-timescaledb  healthy
# deli-redis        healthy
# deli-kafka        healthy
# deli-minio        healthy
```

Useful local URLs once running:

| Service | URL |
|---------|-----|
| Kafka UI | http://localhost:8090 |
| MinIO console | http://localhost:9001 (minioadmin / minioadmin) |

### 3. Run a service (choose one to start with)

**From IntelliJ IDEA** — use the pre-configured run configurations in the Run menu:
- `Boot: api-gateway`
- `Boot: route-service`
- `Boot: delivery-service`
- `Boot: location-service`
- `Boot: notification-service`

**From WSL terminal**:

```bash
./gradlew :services:route-service:bootRun
```

**Health check** (once a service is running):

```bash
curl http://localhost:8081/actuator/health
# {"status":"UP","components":{"db":{"status":"UP"},...}}
```

### 4. Verify Kafka topics exist

```bash
docker exec deli-kafka \
  kafka-topics.sh --bootstrap-server localhost:9092 --list
```

Topics are auto-created when services start (`auto.create.topics.enable=true` locally).
In production they are provisioned by the Helm post-install Job.

---

## Daily development workflow

```bash
# Start infra (if not already running)
docker compose up -d

# Build changed subprojects only (Gradle incremental)
./gradlew build

# Build a specific service and run tests
./gradlew :services:route-service:build

# Run static analysis
./gradlew :services:route-service:detekt

# Format code (ktlint auto-fix)
./gradlew :services:route-service:ktlintFormat

# Tail logs for a running service
./gradlew :services:route-service:bootRun 2>&1 | tee /tmp/route.log

# Stop infra
docker compose down

# Wipe all data and start fresh
docker compose down -v && docker compose up -d
```

---

## Environment variables

Never commit secrets. For local development, create
`services/<name>/src/main/resources/application-local-secrets.yml`
(this file is `.gitignore`d):

```yaml
# services/route-service/src/main/resources/application-local-secrets.yml
deli:
  maps:
    api-key: "YOUR_GOOGLE_MAPS_API_KEY_HERE"
```

Set `SPRING_CONFIG_ADDITIONAL_LOCATION` to pick it up, or just pass it as an
environment variable in the IDEA run configuration.

---

## Build targets reference

| Command | What it does |
|---------|-------------|
| `./gradlew build` | Compile, test, and check all subprojects |
| `./gradlew :services:X:build` | Build a single service |
| `./gradlew :services:X:bootRun` | Run a service locally |
| `./gradlew :services:X:bootJar` | Produce fat JAR for Docker |
| `./gradlew :services:X:bootBuildImage` | Build Docker image via Buildpacks |
| `./gradlew :services:X:detekt` | Run static analysis |
| `./gradlew :services:X:ktlintFormat` | Auto-fix formatting |
| `./gradlew :services:X:test` | Run tests only |
| `./gradlew dependencies` | Show dependency tree |

---

## Phases

| Phase | What gets built | Status |
|-------|----------------|--------|
| 1 | Monorepo scaffold, Gradle, tooling | ✅ Complete |
| 2 | Shared domain model + Kafka contracts | ⬜ Next |
| 3 | Helm chart skeleton + local K8s | ⬜ |
| 4 | Auth service + JWT | ⬜ |
| 5 | Route service + delivery service | ⬜ |
| 6 | Location service + WebSocket GPS | ⬜ |
| 7 | API gateway + notification service | ⬜ |
| 8 | Ionic mobile app | ⬜ |
| 9 | GitHub Actions CI/CD | ⬜ |
| 10 | Integration tests + smoke suite | ⬜ |

---

## Tech stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.1 + Java 21 |
| Framework | Spring Boot 3.4 |
| Build | Gradle 8.11 (Kotlin DSL) |
| Messaging | Apache Kafka 3.7 (KRaft) |
| Primary DB | PostgreSQL 16 + Flyway |
| GPS store | TimescaleDB 2.14 |
| Cache | Redis 7.4 |
| Object store | MinIO / AWS S3 |
| Mobile | Ionic 8 + Angular 19 + Capacitor 6 |
| Maps | Google Maps SDK |
| Auth | JWT (JJWT 0.12) |
| Container | Docker + Kubernetes + Helm 3 |
| CI/CD | GitHub Actions |
| Observability | Prometheus + Grafana + Micrometer |
| Analysis | Detekt + ktlint |
