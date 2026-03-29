# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Infrastructure
```bash
docker compose up -d          # Start all local infrastructure
```

### Backend (Gradle)
```bash
./gradlew build               # Build all services + run tests + ktlint
./gradlew test                # Tests only
./gradlew ktlintCheck         # Lint only
./gradlew ktlintFormat        # Auto-format

# Run a single service
./gradlew :services/api-gateway:bootRun
./gradlew :services/route-service:bootRun
./gradlew :services/delivery-service:bootRun
./gradlew :services/location-service:bootRun
./gradlew :services/notification-service:bootRun
```

### Mobile App
```bash
cd mobile-app
npm install --legacy-peer-deps
npm start                                         # Dev server (:8100)
npm test -- --watch=false --browsers=ChromeHeadless
npm run lint
npm run build:prod
```

### Testing & Scripts
```bash
bash docs/e2e-test.sh         # Full curl-based E2E smoke test (requires infra + all services up)
scripts/reset-state.sh        # Clear PostgreSQL tables, Redis, Kafka topics
scripts/inject-gps.sh         # Inject fake GPS data for customer tracking tests
```

## Architecture

Deli is a courier delivery platform. Couriers manage daily delivery shifts via mobile app; customers track packages in real-time.

**Request flow:** Mobile app → API Gateway (:8080, JWT auth + routing) → downstream services

| Service | Port | Responsibility |
|---|---|---|
| api-gateway | 8080 | JWT issuance/validation, Spring Cloud Gateway routing |
| route-service | 8081 | Shifts, stops, ETA calculation |
| delivery-service | 8082 | Delivery confirmation, proof-of-delivery photos (MinIO) |
| location-service | 8083 | GPS tracking (writes to TimescaleDB) |
| notification-service | 8084 | FCM push notifications (Kafka consumer only) |

**Databases:** PostgreSQL 16 (`courierdb`, schemas: `route`, `delivery`) · TimescaleDB 2.14 (`gpsdb`, schema: `gps`) · Redis 7.4 · MinIO

**Async events via Kafka:** `location.updated`, `route.updated`, `stop.assigned`, `delivery.confirmed`, `delivery.failed`, `shift.started`, `shift.completed`

**Shared modules:**
- `shared/domain-model` — Kafka event types, domain models, value objects
- `shared/common-api` — HTTP request/response DTOs, `MdcLoggingFilter`

**Gradle convention plugins** in `buildSrc/` apply uniformly: Java 21 toolchain, ktlint 1.4.1, Kotest + Mockk + TestContainers for tests, Prometheus metrics. Detekt is present but disabled pending 2.0 stable.

**Mobile app** (`mobile-app/`) is Ionic 8 / Angular 19 / Capacitor 6. Feature folders: `auth/`, `courier/`, `customer/`. Shared services in `app/shared/services/`.

## Code Conventions

- **Kotlin**: 4-space indent, ktlint defaults; DTOs in `shared/` should be immutable data classes.
- **Service config**: `application-*.yml` per service under `src/main/resources/`; Gradle in Kotlin DSL.
- **Angular**: kebab-case feature folders and filenames; standalone components; SCSS colocated; typed RxJS streams.
- **Tests**: Backend under `src/test/kotlin/`; use `should...` naming; include Kafka contract expectations when emitting events. Mobile specs beside components.

## Commit & PR Conventions

- Commits: short imperative subject with scope (`route: handle empty shift`); squash WIP before PR.
- PRs require: summary, testing checklist (`./gradlew test`, `./gradlew ktlintCheck`, curl E2E, `npm run build:prod`), confirmation no secrets or breaking Kafka/API shape changes.

## Local Dev Credentials

| Role | Email | Password |
|---|---|---|
| Courier | `courier@deli.local` | `LocalDev123!` |
| Customer | `customer@deli.local` | `LocalDev123!` |