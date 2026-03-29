# Deli — Courier Delivery Platform

A full-stack courier delivery platform. Couriers manage daily delivery shifts via a mobile app; customers track packages in real time.

## Architecture

```
┌────────────────────────────────────────────────────┐
│   Ionic / Angular mobile app (courier + customer)  │
└──────────────────────┬─────────────────────────────┘
                       │  REST + WebSocket
┌──────────────────────▼─────────────────────────────┐
│   API Gateway  (Spring Cloud Gateway, JWT, :8080)  │
└──┬──────────┬──────────┬──────────┬────────────────┘
   ▼          ▼          ▼          ▼
route      delivery   location  notification
:8081      :8082      :8083      :8084
   │          │          │
   └──────────┴──────────┴──── Kafka ──┐
                                       │
              ┌────────────────────────┼──────────┐
              ▼                        ▼          ▼
          PostgreSQL             TimescaleDB    Redis
          (app data)             (GPS history)  (cache)
                                        MinIO (photos)
```

## Tech Stack

| Layer | Technology |
|---|---|
| Mobile | Ionic 8 / Angular 19 / Capacitor 6 |
| Gateway | Spring Cloud Gateway 4 / Spring Boot 3.4 |
| Services | Kotlin 2.1 / Spring Boot 3.4 |
| Messaging | Apache Kafka 3.7 (KRaft) |
| Primary DB | PostgreSQL 16 |
| Time-series | TimescaleDB 2.14 |
| Cache | Redis 7.4 |
| Object store | MinIO |
| Build | Gradle 8.8 (Kotlin DSL) |
| Deploy | Kubernetes / Helm |

## Prerequisites

- WSL 2 with project in native filesystem (`~/projects/deli`)
- Java 21 (Eclipse Temurin)
- Node.js 20+
- Docker Desktop with Kubernetes enabled
- Helm 3.x

## Quick Start

```bash
# 1. Start infrastructure
docker compose up -d

# 2. Start all backend services (separate terminals or IDEA run configs)
./gradlew :services:api-gateway:bootRun       # :8080
./gradlew :services:route-service:bootRun     # :8081
./gradlew :services:delivery-service:bootRun  # :8082
./gradlew :services:location-service:bootRun  # :8083
./gradlew :services:notification-service:bootRun # :8084

# 3. Start mobile app
cd mobile-app && npm install --legacy-peer-deps && npm start
# Opens at http://localhost:8100
```

## Seed Credentials

| Role | Email | Password |
|---|---|---|
| Courier | courier@deli.local | LocalDev123! |
| Customer | customer@deli.local | LocalDev123! |

## Service Ports

| Service | Port |
|---|---|
| API Gateway | 8080 |
| Route service | 8081 |
| Delivery service | 8082 |
| Location service | 8083 |
| Notification service | 8084 |
| Kafka UI | 8090 |
| MinIO console | 9001 |

## Development Scripts

```bash
# Reset all state (DB + Redis + Kafka topics)
~/projects/deli/scripts/reset-state.sh

# Inject fake GPS position for customer tracking map test
~/projects/deli/scripts/inject-gps.sh
# Then open: http://localhost:8100/customer/tracking?courierId=<id>
```

## E2E Test

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"courier@deli.local","password":"LocalDev123!"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

SHIFT=$(curl -s -X POST http://localhost:8080/api/routes/shifts \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"scheduledDate":"2026-03-29"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['shiftId'])")
```

See `docs/e2e-test.sh` for the full sequence.

## Kafka Topics

| Topic | Producer | Consumer |
|---|---|---|
| location.updated | location-service | audit |
| route.updated | route-service | notification-service |
| stop.assigned | route-service | delivery-service |
| delivery.confirmed | delivery-service | notification-service |
| delivery.failed | delivery-service | notification-service |
| shift.started | route-service | notification-service |
| shift.completed | route-service | notification-service |

## Database Schemas

| Schema | Database | Service |
|---|---|---|
| public | courierdb | api-gateway |
| route | courierdb | route-service |
| delivery | courierdb | delivery-service |
| gps | gpsdb (TimescaleDB) | location-service |

## Project Structure

```
deli/
├── buildSrc/           # Gradle convention plugins
├── shared/
│   ├── domain-model/   # Value objects, Kafka events
│   └── common-api/     # HTTP DTOs, logging filter
├── services/
│   ├── api-gateway/
│   ├── route-service/
│   ├── delivery-service/
│   ├── location-service/
│   └── notification-service/
├── mobile-app/         # Ionic / Angular
├── infra/
│   ├── docker/         # postgres init.sql
│   └── helm/           # Kubernetes charts
├── scripts/            # reset-state.sh, inject-gps.sh
├── docs/               # Additional documentation
└── docker-compose.yml
```

## CI/CD

| Workflow | Trigger | Action |
|---|---|---|
| ci-backend.yml | Push (Kotlin files) | Build + test + ktlint |
| ci-mobile.yml | Push (mobile-app/) | Type-check + build |
| cd-docker.yml | Merge to main | Push images to ghcr.io |

Enable write permissions: **Settings → Actions → General → Read and write permissions**

## Kubernetes Deployment

```bash
# Deploy infrastructure
~/projects/deli/infra/helm/deploy-local.sh --infra

# Build images then deploy services
~/projects/deli/infra/helm/build-images.sh
~/projects/deli/infra/helm/deploy-local.sh --services
```

## Known Limitations

- No dedicated package service — packages created via route-service API
- Customer tracking requires `?courierId=` parameter manually — needs delivery→courier lookup
- Detekt disabled pending detekt 2.0 stable (Kotlin 2.1 support)
- Signature capture UI not yet implemented (S3 infrastructure is ready)
