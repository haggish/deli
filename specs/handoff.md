# Deli Platform — Claude Code Context Handoff

This document captures the complete state of the Deli courier platform project
for continuation in a Claude Code session. Read this in full before making any
changes to the codebase.

---

## Project Location

```
~/projects/deli/
```

**Critical:** the project lives in the WSL native filesystem, NOT under `/mnt/c/`.
Never move or reference files from `/mnt/c/` — Gradle performance and file watching
break on the Windows filesystem.

---

## What Has Been Built

A full-stack courier delivery platform, fully implemented and tested across 10 phases:

| Phase | What was built | Status |
|---|---|---|
| 1 | Monorepo scaffold, Gradle 8.8, buildSrc convention plugins, docker-compose | ✅ |
| 2 | Shared domain model — value objects, Kafka events, exceptions, HTTP DTOs | ✅ |
| 3 | Helm charts — deli-platform (infra) + deli-services (microservices) | ✅ |
| 4 | JWT authentication in API gateway (login, refresh, R2DBC, Flyway) | ✅ |
| 5 | Route service (shifts, stops, JPA, Kafka) + Delivery service (confirm, fail, S3) | ✅ |
| 6 | Location service (WebSocket GPS, TimescaleDB hypertable, Redis cache, Kafka) | ✅ |
| 7 | Notification service (FCM client, Redis token registry, Kafka consumers) | ✅ |
| 8 | Ionic/Angular mobile app (courier + customer views, Leaflet maps) | ✅ |
| 9 | GitHub Actions CI/CD + Dockerfiles for all 5 services | ✅ |
| 10 | Structured logging (MDC filter, logback-spring.xml), README, E2E test script | ✅ |

---

## Repository Structure

```
~/projects/deli/
├── buildSrc/src/main/kotlin/
│   ├── deli.kotlin-library.gradle.kts
│   └── deli.spring-service.gradle.kts
├── gradle/libs.versions.toml
├── shared/
│   ├── domain-model/    value objects, Kafka events, exceptions
│   └── common-api/      HTTP DTOs, MdcLoggingFilter, logback-spring.xml
├── services/
│   ├── api-gateway/     :8080  Spring Cloud Gateway, JWT, R2DBC
│   ├── route-service/   :8081  Shifts, stops, JPA, Kafka publisher
│   ├── delivery-service/:8082  Confirm/fail, S3, Kafka consumer+publisher
│   ├── location-service/:8083  WebSocket GPS, TimescaleDB, Redis, Kafka
│   └── notification-service/:8084  FCM, Kafka consumers
├── mobile-app/          Ionic 8 / Angular 19 / Capacitor 6
├── infra/
│   ├── docker/postgres/init.sql
│   └── helm/
│       ├── deli-platform/   infra StatefulSets
│       └── deli-services/   service Deployments
├── scripts/
│   ├── reset-state.sh   wipe DB + Redis + Kafka
│   └── inject-gps.sh    fake courier GPS position into Redis
├── docs/
│   ├── e2e-test.sh      automated E2E test script
│   ├── architecture.md  full architecture document with diagrams
│   ├── workflow.md      daily development commands cheatsheet
│   └── mobile.md        real device testing guide (ngrok + Android)
├── .github/
│   ├── workflows/
│   │   ├── ci-backend.yml
│   │   ├── ci-mobile.yml
│   │   └── cd-docker.yml
│   ├── dependabot.yml
│   └── pull_request_template.md
├── .dockerignore
├── docker-compose.yml
└── README.md
```

---

## Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Mobile | Ionic / Angular / Capacitor | 8 / 19 / 6 |
| Gateway | Spring Cloud Gateway | Spring Boot 3.4 |
| Services | Kotlin + Spring Boot | 2.1 / 3.4 |
| Build | Gradle (Kotlin DSL) | 8.8 — NOT 8.11+ (metadata.bin bug) |
| Messaging | Apache Kafka (KRaft) | 3.7 |
| Primary DB | PostgreSQL | 16 |
| Time-series | TimescaleDB | 2.14 (pg16) |
| Cache | Redis | 7.4 |
| Object store | MinIO | S3-compatible |
| Deploy | Kubernetes / Helm | Docker Desktop K8s |

---

## Seed Credentials

| Role | Email | Password | User ID |
|---|---|---|---|
| Courier | courier@deli.local | LocalDev123! | fb30f9fa-0337-44b7-a32f-93a8a4ee7aac |
| Customer | customer@deli.local | LocalDev123! | (decode from JWT) |

BCrypt hashes use `$2b$` prefix. The gateway's `BCryptPasswordEncoder` is
configured with `BCryptVersion.$2B` to accept these.

---

## Infrastructure Ports

| Container | Port | Credentials |
|---|---|---|
| deli-postgres | 5432 | courier_user / courier_pass_local / courierdb |
| deli-timescale | 5433 | gps_user / gps_pass_local / gpsdb |
| deli-redis | 6379 | password: redis_local |
| deli-kafka | 9094 (external) / 9092 (internal) | — |
| deli-kafka-ui | 8090 | — |
| deli-minio | 9000 / 9001 | minioadmin / minioadmin |

---

## Critical Technical Decisions & Bug Fixes

These decisions were hard-won during implementation. Do not change these
without understanding the rationale.

### Gradle

- **Version must be 8.8** — 8.11+ has a Kotlin DSL `metadata.bin` bug
- `org.gradle.configuration-cache=false` in `gradle.properties` — Spring Boot Gradle plugin incompatibility
- No `plugins{}` block in root `build.gradle.kts`
- `settings.gradle.kts` order: pluginManagement → dependencyResolutionManagement → rootProject.name → include()
- buildSrc `dependencies` must include: kotlin-gradle-plugin, kotlin-serialization, kotlin-allopen, kotlin-noarg, spring-boot-gradle-plugin, dependency-management-plugin, ktlint-gradle
- **Detekt is disabled** — incompatible with Kotlin 2.1. Detekt 2.x still in alpha at time of writing

### Domain Model Value Classes

- All `of()` factory methods take `UUID` (not `String`) — changed during implementation
- `UUIDSerializer` object for kotlinx.serialization UUID support
- `InstantSerializer` serialises as ISO-8601 string (NOT numeric timestamp)
- `@Serializable(with = UUIDSerializer::class)` required on each inline value class property

### Kafka Serialisation (critical fix)

Spring Kafka's `JsonSerializer` serialises `Instant` as a numeric Unix timestamp by default.
The shared domain model's `InstantSerializer` expects ISO-8601 strings.

**Fix:** custom `KafkaConfig.kt` in each producer service (route-service, delivery-service,
location-service) that builds a `ProducerFactory` with a custom `ObjectMapper` that has
`disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)`.

Without this fix, notification-service throws `JsonDecodingException: Expected quotation
mark but had numeric digit` when consuming events.

### API Gateway — R2DBC + Flyway dual config

- R2DBC for reactive database access (non-blocking)
- Flyway uses separate JDBC connection (R2DBC does not support DDL)
- `RefreshToken` INSERT must use `R2dbcEntityTemplate.insert()` NOT `repository.save()`
  — `save()` sees the pre-set UUID ID and issues an UPDATE (finds nothing, silently fails)
- BCrypt: use `BCryptPasswordEncoder.BCryptVersion.$2B` to accept `$2b$` prefix hashes

### Route Service — Missing Endpoints Added During Implementation

These endpoints were added after the initial implementation when E2E tests found them missing:

1. `POST /api/routes/shifts/{shiftId}/stops` — add a stop to a shift
    - Request: `AddStopRequest(packageId, customerId, address, latitude, longitude)`
2. `PATCH /api/routes/stops/{stopId}/complete` — mark stop completed/failed
    - Request: `CompleteStopRequest(deliveryStatus, courierNote?)`
    - This is needed because delivery-service and route-service have separate databases.
      Confirming in delivery-service does NOT auto-update the stop in route-service.
      The mobile app must call both endpoints.

### Location Service — CourierPosition Deserialization

`CourierPosition` data class must have nullable defaults for Redis deserialization:

```kotlin
@JsonIgnoreProperties(ignoreUnknown = true)
data class CourierPosition(
    val courierId: String,
    val shiftId: String? = null,        // nullable — manually injected Redis entries may omit
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Double? = null,
    val headingDegrees: Double? = null,
    val updatedAt: java.time.Instant? = null,  // nullable — string format varies
    val isOnline: Boolean = true,
)
```

Without nullable defaults, `MissingKotlinParameterException` is thrown silently and
`CourierPositionCache.get()` returns `null`, causing the tracking API to return
`{"success":true,"data":null}` even when a position exists in Redis.

### Spring Profiles

All services must have this in `application.yml` or they fail to start with
"Failed to configure a DataSource — no profiles currently active":

```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}
```

### Angular / Mobile App

- `zone.js`: must be `~0.15.0` for Angular 19.2 (not `~0.14.0`)
- `import 'zone.js'` must be the **first** line in `main.ts`
- CSS in `angular.json` styles array — not `<link>` in `index.html`
- `@else if (signal(); as x)` is **invalid** — `as` only works on primary `@if` block
    - Use `@else if (signal())` and call `signal()!.property` inside
- `SlicePipe`, `DecimalPipe` must be imported explicitly in standalone components
- `wifiOff` does not exist in ionicons — use `wifiOutline` for both states
- `RouterOutlet` is not needed in Ionic shell components — IonTabs handles routing
- `ionViewWillEnter()` for page reload on navigation (replaces `ngOnInit` for Ionic lifecycle)
- Leaflet: dynamic import in `ngAfterViewInit`, run outside NgZone, CSS in `angular.json`
- `@angular/cdk/clipboard` not installed — use `navigator.clipboard` directly

### Customer Tracking Page

The tracking page uses `?courierId=` query param — it does NOT use the logged-in user's
own ID. The customer's userId will never have a position in Redis.

```typescript
const courierId = this.route.snapshot.queryParamMap.get('courierId')
  ?? this.authService.session()?.userId;
```

URL format: `http://localhost:8100/customer/tracking?courierId=<courierId>`

### Docker Compose

- Kafka: `apache/kafka:3.7.0` (not bitnami), KRaft mode, `CLUSTER_ID` required
- Kafka UI: `ghcr.io/kafbat/kafka-ui:latest` (provectuslabs abandoned)
- MinIO healthcheck removed — image has no curl/nc/grep
- `minio/mc:latest` for init container

---

## Known Limitations / Future Work

1. **No dedicated package service** — packages are created via route-service API directly
2. **Customer→courier lookup missing** — tracking page requires manual `?courierId=` param.
   In production, a package lookup endpoint would resolve the active courier automatically.
3. **Route optimisation not implemented** — stops ordered by insertion sequence
4. **Signature capture UI missing** — S3 pre-signed URL infrastructure is ready but no
   signature pad component in the mobile app
5. **Detekt disabled** — pending detekt 2.0 stable (Kotlin 2.1 support)
6. **`PATCH /api/routes/stops/{id}/complete` must be called manually** after
   `POST /api/deliveries/stops/{id}/confirm`. Long-term fix: delivery-service publishes
   a Kafka event → route-service consumes it to auto-complete the stop.

---

## Daily Workflow Commands

### Start everything

```bash
# Infrastructure
cd ~/projects/deli && docker compose up -d

# Services (separate terminals)
./gradlew :services:api-gateway:bootRun
./gradlew :services:route-service:bootRun
./gradlew :services:delivery-service:bootRun
./gradlew :services:location-service:bootRun
./gradlew :services:notification-service:bootRun

# Mobile app
cd mobile-app && npm start   # http://localhost:8100
```

### Reset state between test runs

```bash
~/projects/deli/scripts/reset-state.sh
# Then restart notification-service (Kafka topic deletion drops its connections)
```

### Get a courier auth token

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"courier@deli.local","password":"LocalDev123!"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")
```

### Run the full E2E test

```bash
bash ~/projects/deli/docs/e2e-test.sh
```

### Inject GPS for customer tracking map test

```bash
~/projects/deli/scripts/inject-gps.sh
# Open: http://localhost:8100/customer/tracking?courierId=fb30f9fa-0337-44b7-a32f-93a8a4ee7aac
```

### Build

```bash
./gradlew build --no-configuration-cache
./gradlew test --no-configuration-cache
./gradlew ktlintCheck --no-configuration-cache
```

---

## Database Schemas

| Schema | Database | Service | Key tables |
|---|---|---|---|
| public | courierdb (pg:5432) | api-gateway | gateway_users, refresh_tokens |
| route | courierdb (pg:5432) | route-service | shifts, stops |
| delivery | courierdb (pg:5432) | delivery-service | delivery_records |
| gps | gpsdb (timescale:5433) | location-service | location_pings (hypertable) |

### Redis key patterns

```
courier:position:{courierId}    TTL 60s    current GPS position
fcm:courier:{userId}            TTL 30d    FCM device token
fcm:customer:{userId}           TTL 30d    FCM device token
```

---

## Kafka Topics

| Topic | Producer | Consumer |
|---|---|---|
| shift.started | route-service | notification-service |
| shift.completed | route-service | notification-service |
| route.updated | route-service | notification-service |
| stop.assigned | route-service | delivery-service |
| delivery.confirmed | delivery-service | notification-service |
| delivery.failed | delivery-service | notification-service |
| location.updated | location-service | (future: ETA, dispatcher) |

Notification-service uses **separate consumer group IDs per topic**:
`notification-service-shift-started`, `notification-service-delivery-confirmed`, etc.

---

## Notification Service — FCM Dry Run

`deli.fcm.dry-run=true` is the default in the local profile.
All notifications are logged, not sent. To see dry-run output, first register
a test FCM token:

```bash
curl -s -X PATCH http://localhost:8080/api/notifications/fcm-token \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"fcmToken":"test-device-token-123"}'
```

Then Kafka events for this courier will log:
```
[DRY RUN] Would send FCM notification: title='Shift started' ...
```

---

## Documentation Files

All documentation lives in `~/projects/deli/docs/`:

| File | Contents |
|---|---|
| `architecture.md` | Full architecture with ASCII diagrams, decision rationale |
| `workflow.md` | Complete daily development command reference |
| `mobile.md` | Real device testing guide (ngrok, Android APK) |
| `e2e-test.sh` | Automated E2E test script — run with `bash docs/e2e-test.sh` |

---

## What to Do Next (Suggested)

The platform is complete and tested. Possible next steps in priority order:

1. **Auto-complete stop via Kafka** — delivery-service publishes `StopCompleted` event
   → route-service consumes it → stop status updated without mobile app calling two endpoints
2. **Customer delivery lookup** — endpoint to find active courier from customer's package
   → customer tracking page no longer needs `?courierId=` query param
3. **Signature capture** — add a canvas signature pad component to `delivery-confirm.page.ts`
   and upload to the S3 pre-signed URL already provided by delivery-service
4. **Route optimisation** — integrate with an open routing API (OSRM, Valhalla) to sort
   stops by optimal driving order when a shift starts
5. **Dispatcher web app** — a separate Angular web app at `/dispatcher` with a full map
   showing all active couriers, all stops, and the ability to add/reassign stops in real time
6. **Re-enable Detekt** — when detekt 2.0 stable releases with Kotlin 2.1 support

---

## How to Use This Document in Claude Code

1. Start a new Claude Code session in the project directory:
   ```bash
   cd ~/projects/deli
   claude
   ```

2. At the start of the session, tell Claude Code:
   > "Read the file `docs/CLAUDE_CODE_CONTEXT.md` before we begin.
   >  This project is a complete courier delivery platform. The context file
   >  explains everything that has been built, all critical decisions, and
   >  known limitations. Use it as the source of truth for project state."

3. Claude Code will have full filesystem access to read the actual source files
   for any detail not covered in this document.
