# Deli Platform — Architecture Document

## Table of Contents

1. [Overview](#1-overview)
2. [System Context](#2-system-context)
3. [Monorepo Structure](#3-monorepo-structure)
4. [Service Architecture](#4-service-architecture)
5. [API Gateway](#5-api-gateway)
6. [Route Service](#6-route-service)
7. [Delivery Service](#7-delivery-service)
8. [Location Service](#8-location-service)
9. [Notification Service](#9-notification-service)
10. [Shared Domain Model](#10-shared-domain-model)
11. [Data Architecture](#11-data-architecture)
12. [Kafka Event Pipeline](#12-kafka-event-pipeline)
13. [Mobile Application](#13-mobile-application)
14. [Infrastructure](#14-infrastructure)
15. [CI/CD Pipeline](#15-cicd-pipeline)
16. [Key Technical Decisions](#16-key-technical-decisions)

---

## 1. Overview

Deli is a courier delivery platform consisting of five backend microservices, a mobile app serving both courier and customer roles, and a message-driven event pipeline connecting them. Couriers use the app to manage daily delivery shifts; customers track their packages in real time via a live map.

The system is built as a Gradle monorepo so shared code (domain model, DTOs, Kafka events) is compiled once and depended on by all services without requiring a separate artifact repository during development.

---

## 2. System Context

```
┌─────────────────────────────────────────────────────────────────────┐
│                          MOBILE APP                                 │
│         Ionic 8 / Angular 19 / Capacitor 6                         │
│                                                                     │
│   ┌──────────────────┐          ┌──────────────────────────────┐   │
│   │  Courier views   │          │      Customer views           │   │
│   │  - Dashboard     │          │  - Live tracking map          │   │
│   │  - Route list    │          │  - Delivery history           │   │
│   │  - Stop detail   │          │                               │   │
│   │  - Confirm/fail  │          │                               │   │
│   └────────┬─────────┘          └──────────────┬───────────────┘   │
└────────────┼───────────────────────────────────┼───────────────────┘
             │ REST + WebSocket                   │ REST (polling)
             │ Bearer JWT                         │ Bearer JWT
             ▼                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         API GATEWAY  :8080                          │
│               Spring Cloud Gateway + JWT auth filter                │
│          Validates JWT → injects X-User-Id, X-User-Role headers     │
└──────┬──────────────┬──────────────┬──────────────┬────────────────┘
       │              │              │              │
       ▼              ▼              ▼              ▼
  route-service  delivery-service  location-service  notification-service
    :8081           :8082            :8083              :8084
```

**Design principle:** the gateway is the only service that speaks JWT. Downstream services trust the `X-User-Id` and `X-User-Role` headers the gateway injects after validation. No downstream service needs a JWT library.

---

## 3. Monorepo Structure

```
deli/
├── buildSrc/                         # Gradle convention plugins
│   └── src/main/kotlin/
│       ├── deli.kotlin-library.gradle.kts   # shared library preset
│       └── deli.spring-service.gradle.kts   # Spring Boot service preset
│
├── gradle/
│   └── libs.versions.toml            # single source of truth for all versions
│
├── shared/
│   ├── domain-model/                 # value objects, entities, Kafka events
│   └── common-api/                   # HTTP DTOs, MDC filter, logback config
│
├── services/
│   ├── api-gateway/
│   ├── route-service/
│   ├── delivery-service/
│   ├── location-service/
│   └── notification-service/
│
├── mobile-app/                       # Ionic / Angular app
│
├── infra/
│   ├── docker/postgres/init.sql
│   └── helm/
│       ├── deli-platform/            # infra chart (Postgres, Kafka, Redis…)
│       └── deli-services/            # services chart (Deployments, ConfigMaps)
│
├── scripts/                          # reset-state.sh, inject-gps.sh
├── docs/                             # e2e-test.sh, this document
└── docker-compose.yml
```

**Why a monorepo?** Shared code (value objects, Kafka event schemas, HTTP DTOs) is compiled once and referenced as a Gradle project dependency. There is no need to publish artifacts to a local Maven repository during development. Refactoring a shared type causes compile errors in all consumers immediately, not at runtime after a missed publish.

**Convention plugins** (`deli.kotlin-library`, `deli.spring-service`) centralise Kotlin compiler flags, ktlint configuration, and common dependency sets. Adding a new service means creating a `build.gradle.kts` with three lines and applying one plugin — all the boilerplate is inherited.

**`libs.versions.toml`** is the single place where dependency versions are declared. All five `build.gradle.kts` files reference aliases like `libs.spring.boot.starter.web` rather than repeating version strings.

---

## 4. Service Architecture

```
                     ┌─────────────────┐
                     │   domain-model  │  Value objects, Kafka event types,
                     │   (shared lib)  │  domain exceptions
                     └────────┬────────┘
                              │ depended on by all services
              ┌───────────────┼──────────────────────┐
              ▼               ▼                       ▼
     ┌──────────────┐ ┌──────────────┐      ┌──────────────────┐
     │ route-service│ │  delivery-   │  ... │  notification-   │
     │              │ │  service     │      │  service         │
     │ JPA          │ │              │      │                  │
     │ Kafka pub    │ │ JPA          │      │ Kafka consumers  │
     │ REST API     │ │ Kafka pub+sub│      │ FCM client       │
     └──────┬───────┘ └──────┬───────┘      └──────────────────┘
            │                │
            └────────┬───────┘
                     ▼
              ┌─────────────┐
              │    Kafka    │  Event bus — decouples producers from consumers
              └─────────────┘
```

Each service:
- Has its own Spring Boot application class and `application.yml` with `local` and `kubernetes` profiles
- Owns its own database schema (never accesses another service's tables)
- Communicates with other services only via Kafka events or REST through the gateway
- Has a `GatewayAuthFilter` that reads `X-User-Id` / `X-User-Role` headers and populates the Spring `SecurityContext`

---

## 5. API Gateway

```
Request from mobile app
        │
        ▼
┌───────────────────────────────────────────────────────┐
│                     API Gateway                        │
│                                                        │
│  1. JwtAuthenticationFilter                            │
│     ├─ Extracts Bearer token from Authorization header │
│     ├─ Validates signature with shared HMAC-SHA256 key │
│     ├─ Rejects expired or malformed tokens → 401       │
│     └─ Sets X-User-Id, X-User-Role on forwarded req    │
│                                                        │
│  2. Spring Cloud Gateway routes                        │
│     /api/routes/**     → route-service:8081            │
│     /api/deliveries/** → delivery-service:8082         │
│     /api/locations/**  → location-service:8083         │
│     /api/notifications/** → notification-service:8084  │
│     /ws/**            → location-service:8083 (WS)     │
│                                                        │
│  3. Public paths (no JWT required)                     │
│     /api/auth/login, /api/auth/refresh                 │
└───────────────────────────────────────────────────────┘
```

**Auth storage:** `gateway_users` and `refresh_tokens` tables in PostgreSQL, accessed via R2DBC (reactive) for non-blocking I/O. Flyway handles migrations using a separate JDBC connection (R2DBC does not support DDL execution).

**BCrypt configuration:** uses `$2b$` prefix (BCryptVersion.$2B) to accept hashes generated by standard Python/Ruby tools. The seed migration inserts a pre-hashed password so no plaintext appears in the codebase.

**Token strategy:** short-lived access tokens (15 minutes) + long-lived refresh tokens (30 days). Refresh tokens are stored in the database and can be revoked server-side. Access tokens are stateless JWTs — verification requires only the shared secret, no database lookup.

---

## 6. Route Service

```
POST /api/routes/shifts              → create shift
POST /api/routes/shifts/{id}/stops   → add stop to shift
PATCH /api/routes/stops/{id}/start   → courier arrived
PATCH /api/routes/stops/{id}/complete → mark delivered/failed
GET  /api/routes/active              → current shift + stops

            RouteService
                 │
         ┌───────┴──────────┐
         ▼                  ▼
    ShiftRepository    StopRepository
    (JPA, schema=route) (JPA, schema=route)
                            │
                            ▼
                    RouteEventPublisher
                            │
              ┌─────────────┼──────────────┐
              ▼             ▼              ▼
       shift.started  stop.assigned  route.updated
```

**Auto-completion:** `markStopCompleted()` counts completed + failed stops after each update. When `completedStops + failedStops == totalStops`, the shift is automatically marked `COMPLETED` and a `ShiftCompleted` event is published. The courier sees "No active shift" — the shift is done.

**Why JPA over R2DBC here?** Route and stop operations are short transactional writes with simple relationships. JPA's `@Transactional` and lazy loading make the service code straightforward. R2DBC's complexity is only justified when request throughput is high enough to saturate a thread pool — not the case for a single courier's shift operations.

---

## 7. Delivery Service

```
POST /api/deliveries/stops/{id}/confirm  → mark delivered
POST /api/deliveries/stops/{id}/fail     → mark failed
POST /api/deliveries/upload-url/photo    → get S3 pre-signed URL

Kafka consumer:
  stop.assigned → creates DeliveryRecord row

Kafka publishers:
  delivery.confirmed → notification-service notifies customer
  delivery.failed    → notification-service notifies customer
```

**Separation from route-service:** `DeliveryRecord` (owned by delivery-service) and `Stop` (owned by route-service) are separate entities in separate schemas. Confirming a delivery updates the `DeliveryRecord` but does NOT automatically update the `Stop` status — the mobile app must call `PATCH /api/routes/stops/{id}/complete` explicitly. This is intentional: the two services are independently deployable and do not share a transaction boundary.

**The correct long-term fix** is for delivery-service to publish a Kafka event that route-service consumes to auto-complete the stop — closing the loop without requiring the client to call both services. This is documented in Known Limitations.

**S3 / MinIO:** proof photos are never uploaded through the application server. The delivery-service generates a pre-signed PUT URL that the mobile app uses to upload directly to MinIO. The server never handles large binary payloads.

---

## 8. Location Service

```
Mobile app (courier)
     │
     │  WebSocket  ws://gateway/ws/location
     │  Sends JSON LocationPingRequest every 5–15 seconds
     ▼
GatewayHeaderInterceptor (HTTP handshake phase)
     │  Extracts X-User-Id from header
     │  Rejects if missing → 401, connection never upgrades
     ▼
LocationWebSocketHandler
     │
     ▼
LocationService.processPing()
     │
     ├── 1. INSERT into TimescaleDB (durable record)
     │         gps.location_pings (hypertable, 7-day chunks)
     │
     ├── 2. SET in Redis (position cache, TTL=60s)
     │         courier:position:{courierId}
     │
     └── 3. Publish to Kafka
               location.updated → ETA recalc, dispatcher UI

Customer app polls GET /api/locations/couriers/{id}
     │
     └── Reads from Redis (microsecond latency)
              └── Returns null if TTL expired (courier offline)
```

**Two-tier storage rationale:**

| Concern | TimescaleDB | Redis |
|---|---|---|
| Purpose | Complete immutable history | Current position only |
| Query type | Range scan by time | Point lookup by key |
| Latency | Milliseconds | Microseconds |
| Retention | 90 days (automatic chunk drop) | 60 seconds TTL |
| Use case | Audit, replay, ETA calculation | Customer tracking screen |

**TimescaleDB hypertable:** the `location_pings` table is converted to a hypertable partitioned by `recorded_at` with 7-day chunks. A trail query for a single shift reads only one or two chunks rather than the full table. The 90-day retention policy drops entire chunk files — orders of magnitude faster than `DELETE`.

**WebSocket authentication:** standard Spring Security filters do not apply to WebSocket upgrades. A `HandshakeInterceptor` runs during the HTTP upgrade request (before the connection switches protocols) and extracts `X-User-Id`. If missing, the handshake returns 401 and the WebSocket is never established.

**Running Leaflet / Kafka outside NgZone:** Leaflet attaches dozens of DOM event listeners; if they ran inside Angular's change detection zone, every map pan would trigger a full app re-render. `ngZone.runOutsideAngular()` isolates this.

---

## 9. Notification Service

```
Kafka topics consumed:
  shift.started       → FCM to courier  "Shift started, N stops"
  shift.completed     → FCM to courier  "Great work! N delivered"
  route.updated       → FCM to courier  "Route updated, N stops"
  delivery.confirmed  → FCM to customer "Package delivered ✓"
  delivery.failed     → FCM to customer "Delivery unsuccessful"

FCM token registry (Redis):
  fcm:courier:{userId}  → device token (TTL 30 days)
  fcm:customer:{userId} → device token (TTL 30 days)

Token registration:
  PATCH /api/notifications/fcm-token
  Called by mobile app on login and when Firebase issues a new token
```

**Stateless design:** no database. State is the Kafka consumer group offsets (managed by Kafka) and the FCM token registry (Redis). The service can be scaled horizontally — multiple instances share the same consumer group and Kafka distributes partition assignments.

**Separate consumer group per topic:** each `@KafkaListener` uses a distinct group ID. If a single group ID were used for all five consumers, Kafka would assign partitions from all topics to a single group, destroying the parallelism between independent event types.

**`runCatching` on every consumer:** if a notification fails to send, the consumer logs the error and commits the Kafka offset, moving to the next message. Rethrowing would block the partition indefinitely. Missing one notification is acceptable; blocking all subsequent notifications is not.

**Dry-run mode:** `deli.fcm.dry-run=true` (default locally) logs what would be sent without hitting the FCM API. The full Kafka pipeline runs in development and CI without any Firebase credentials.

---

## 10. Shared Domain Model

```
shared/domain-model
├── valueobject/
│   ├── CourierId     @JvmInline value class wrapping UUID
│   ├── CustomerId    @JvmInline value class wrapping UUID
│   ├── ShiftId       @JvmInline value class wrapping UUID
│   ├── StopId        @JvmInline value class wrapping UUID
│   ├── PackageId     @JvmInline value class wrapping UUID
│   ├── TrackingNumber
│   └── Coordinates
│
├── events/
│   ├── EventEnvelope<T>    @Serializable wrapper with eventType + occurredAt
│   ├── KafkaTopics         const val topic names
│   ├── EventTypes          const val event type strings
│   ├── ShiftStartedEvent
│   ├── ShiftCompletedEvent
│   ├── RouteUpdatedEvent
│   ├── StopAssignedEvent
│   ├── LocationUpdatedEvent
│   ├── DeliveryConfirmedEvent
│   └── DeliveryFailedEvent
│
└── model/
    ├── Exceptions    DeliException hierarchy
    └── Enums         ShiftStatus, StopStatus, DeliveryStatus, ...
```

**Value classes** (`@JvmInline`) are zero-cost at runtime — the JVM sees a plain UUID. At compile time they prevent passing a `CourierId` where a `ShiftId` is expected, catching an entire class of bugs that UUID-everywhere code accumulates.

**`kotlinx.serialization`** is used for Kafka event serialisation rather than Jackson because it is Kotlin-native, generates no reflection, and works correctly with value classes and sealed hierarchies without annotations. Jackson is used for REST (HTTP layer) where Spring's auto-configuration provides it.

**Kafka serialisation fix:** Spring Kafka's `JsonSerializer` serialises `Instant` as a numeric Unix timestamp by default. `kotlinx.serialization`'s `InstantSerializer` expects an ISO-8601 string. The fix is a custom `KafkaConfig` in each producer service that configures Jackson's `ObjectMapper` with `WRITE_DATES_AS_TIMESTAMPS` disabled.

---

## 11. Data Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                PostgreSQL  (courierdb)                       │
│                                                             │
│  schema: public (api-gateway)                               │
│  ├── gateway_users   id, email, password_hash, role         │
│  └── refresh_tokens  id, user_id, token_hash, expires_at    │
│                                                             │
│  schema: route (route-service)                              │
│  ├── shifts   id, courier_id, status, scheduled_date, ...   │
│  └── stops    id, shift_id, sequence_number, status, ...    │
│                                                             │
│  schema: delivery (delivery-service)                        │
│  └── delivery_records  id, stop_id, status, placement, ...  │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│             TimescaleDB  (gpsdb)                             │
│                                                             │
│  schema: gps                                                │
│  └── location_pings  (hypertable, partition by recorded_at) │
│      id, courier_id, shift_id, lat, lng, accuracy, speed,  │
│      heading, recorded_at, received_at                      │
│                                                             │
│  Continuous aggregate: location_pings_1min                  │
│  Retention policy: 90 days                                  │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    Redis                                     │
│                                                             │
│  courier:position:{courierId}   CourierPosition JSON        │
│    TTL: 60 seconds                                          │
│                                                             │
│  fcm:courier:{userId}           FCM device token string     │
│  fcm:customer:{userId}          FCM device token string     │
│    TTL: 30 days                                             │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    MinIO  (S3-compatible)                    │
│                                                             │
│  Bucket: deli-delivery-proofs                               │
│  ├── photos/{stopId}/{timestamp}.jpg                        │
│  └── signatures/{stopId}/{timestamp}.png                    │
│                                                             │
│  Access: pre-signed PUT URLs only                           │
│  Files uploaded directly from mobile — never via server     │
└─────────────────────────────────────────────────────────────┘
```

**Schema isolation:** each service owns exactly one schema and never issues queries against another service's schema. This is enforced by convention (each service's `application.yml` sets `hibernate.default_schema`) rather than database-level permissions (though that would be the next hardening step).

**Single PostgreSQL instance, multiple schemas:** using one PostgreSQL instance with schema isolation rather than separate databases per service is a deliberate development simplification. In production with high traffic, each service could be migrated to its own database instance without application code changes — only the connection string in the ConfigMap changes.

---

## 12. Kafka Event Pipeline

```
Time →

Courier starts shift
        │
        ▼ route-service publishes
   [shift.started]
        │
        └──► notification-service: "Shift started, 0 stops" → courier FCM

Dispatcher adds stops
        │
        ▼ route-service publishes
   [stop.assigned] × N
        │
        └──► delivery-service: creates DeliveryRecord per stop
        └──► notification-service: "Route updated, N stops" → courier FCM

Courier arrives and confirms delivery
        │
        ▼ delivery-service publishes
   [delivery.confirmed]
        │
        └──► notification-service: "Package delivered ✓" → customer FCM

Courier reports failure
        │
        ▼ delivery-service publishes
   [delivery.failed]
        │
        └──► notification-service: "Delivery unsuccessful" → customer FCM

All stops done → auto-complete
        │
        ▼ route-service publishes
   [shift.completed]
        │
        └──► notification-service: "Great work! N delivered" → courier FCM

GPS ping every 5–15 seconds (throughout shift)
        │
        ▼ location-service publishes
   [location.updated]
        │
        └──► (future: ETA recalculation, dispatcher dashboard)
```

**Event envelope pattern:** every Kafka message is wrapped in `EventEnvelope<T>` containing `eventType: String`, `occurredAt: Instant`, and `payload: T`. Consumers can inspect `eventType` without deserialising the payload, enabling a future event router pattern. `occurredAt` is the device/server time the event occurred, not the Kafka ingestion time.

**Partition key strategy:** all events are keyed by the relevant courier or shift ID. This guarantees ordering — all events for a given shift arrive in sequence to the same partition. A courier's `StopAssigned` events will always be processed in insertion order by delivery-service.

---

## 13. Mobile Application

```
┌─────────────────────────────────────────────────────────┐
│                     Angular Router                       │
│                                                          │
│   /login          → LoginPage                            │
│   /courier/**     → CourierShellComponent (tab bar)      │
│     /dashboard    → DashboardPage                        │
│     /route        → RoutePage                            │
│     /stop/:id     → StopDetailPage + Leaflet map         │
│     /stop/:id/confirm → DeliveryConfirmPage              │
│   /customer/**    → CustomerShellComponent (tab bar)     │
│     /tracking     → TrackingPage + Leaflet map           │
│     /history      → HistoryPage                          │
└─────────────────────────────────────────────────────────┘

Guards:
  authGuard    → redirects to /login if no session
  courierGuard → redirects couriers to /courier/dashboard
  customerGuard → redirects customers to /customer/tracking

Services:
  AuthService       signals-based session, JWT decode, refresh
  RouteApiService   HTTP calls to route + delivery endpoints
  GpsService        Capacitor Geolocation + WebSocket to location-service
  CourierPositionCache  HTTP poll of /api/locations/couriers/{id}
```

**Signals over RxJS in components:** Angular 19 signals (`signal()`, `computed()`) replace `BehaviorSubject` chains in component state. Signals are synchronous, do not require `async` pipes, and automatically track dependencies without subscription management. RxJS is kept only in services where it belongs — HTTP calls, polling intervals.

**Standalone components:** no `NgModule` declarations. Each component, page, and guard declares its own imports. Tree-shaking is more aggressive — the login page bundle includes only the Ionic components it actually uses.

**Leaflet maps:**
- Dynamically imported (`await import('leaflet')`) to avoid SSR issues and reduce initial bundle
- Run entirely outside Angular's NgZone to prevent Leaflet's DOM event listeners from triggering change detection on every pan/zoom
- Dark theme achieved via CSS filter `brightness(0.85) saturate(0.9) hue-rotate(180deg) invert(1)` on tile layer — no separate dark tile provider needed
- Custom SVG pins with embedded stop sequence numbers, no external image dependencies

**GPS streaming architecture:**
```
Capacitor Geolocation.watchPosition()
        │ fires every 5–15 seconds
        ▼
GpsService.sendPing()
        │ JSON over WebSocket
        ▼
LocationWebSocketHandler (location-service)
        │
        ├── TimescaleDB write (durable)
        ├── Redis update (hot cache, TTL 60s)
        └── Kafka publish (event fanout)
```

**Customer tracking — polling over WebSocket:** the tracking screen polls every 10 seconds rather than maintaining a persistent WebSocket. Customers open the screen briefly, watch for a few minutes, then close it. A persistent WebSocket for that usage pattern wastes server connections. The 10-second poll is imperceptible and dramatically simpler.

---

## 14. Infrastructure

### Local development (Docker Compose)

```
docker-compose.yml
├── deli-postgres     PostgreSQL 16 :5432   schemas: public, route, delivery
├── deli-timescale    TimescaleDB :5433     schema: gps (hypertable)
├── deli-redis        Redis 7.4 :6379       position cache, FCM tokens
├── deli-kafka        Apache Kafka 3.7 :9094 KRaft mode (no ZooKeeper)
├── deli-kafka-ui     Kafbat Kafka UI :8090
├── deli-minio        MinIO :9000/:9001     proof photos bucket
└── deli-minio-init   one-shot: creates bucket on first start
```

**Kafka KRaft mode:** no ZooKeeper. Apache Kafka 3.7 runs in combined controller+broker mode with a `CLUSTER_ID` for metadata management. This removes an entire infrastructure component and halves the Kafka-related container count.

**MinIO healthcheck removed:** the MinIO container image contains no `curl`, `nc`, or `grep` — healthcheck commands that rely on these all fail. The `minio-init` service uses `mc alias set` in a retry loop instead of depending on a healthcheck.

### Kubernetes (Helm)

```
deli-platform chart (infra StatefulSets)
├── postgres       StatefulSet, postgres:16.2-alpine
├── timescaledb    StatefulSet, timescale/timescaledb:2.14.2-pg16
├── redis          StatefulSet, redis:7.4-alpine
├── kafka          StatefulSet, apache/kafka:3.7.0
└── minio          StatefulSet, minio/minio:RELEASE.2024-03-21

deli-services chart
├── api-gateway    Deployment, 2 replicas
├── route-service  Deployment, 2 replicas
├── delivery-service Deployment, 2 replicas
├── location-service Deployment, 2 replicas
└── notification-service Deployment, 2 replicas

deli-common library chart
└── Shared templates: Deployment, Service, ConfigMap, HPA
```

**Why direct StatefulSet templates over Bitnami charts?** Bitnami chart image tags became broken (digest mismatches) at the time of writing. Direct StatefulSets with pinned official image tags are more predictable — no chart wrapper to debug, and the image used locally in Docker Compose is identical to the one in Kubernetes.

**`deli-common` library chart:** the five service charts share identical Deployment and Service templates (container ports, readiness probes, environment variable injection from ConfigMap/Secret). The library chart provides these templates once; each service chart overrides only its specific values (image name, port, replica count).

---

## 15. CI/CD Pipeline

```
Developer pushes code
        │
        ├── Kotlin files changed?
        │       ▼
        │   ci-backend.yml
        │   ├── Build shared modules
        │   ├── Build all services (-x test)
        │   ├── Run all tests
        │   └── Run ktlint
        │
        ├── mobile-app/ changed?
        │       ▼
        │   ci-mobile.yml
        │   ├── npm ci
        │   ├── tsc --noEmit (type check)
        │   └── ng build --configuration production
        │
        └── Merge to main?
                ▼
            cd-docker.yml
            ├── Build all bootJars (Gradle)
            ├── Upload JARs as artifact
            └── Matrix: 5 services in parallel
                ├── docker/build-push-action
                ├── Layered JAR extraction
                ├── Push to ghcr.io/{owner}/deli-{service}
                └── Tags: latest, {branch}-{sha}
```

**Path filters:** each workflow declares `paths:` so only relevant changes trigger builds. A CSS change in the mobile app does not trigger a 20-minute Gradle build.

**Concurrency groups with asymmetric cancel policy:** CI workflows use `cancel-in-progress: true` — if you push twice quickly, only the second run matters. The Docker push workflow uses `cancel-in-progress: false` — cancelling a push mid-way leaves the registry in a partial state.

**Layered Docker images:** Spring Boot's `jarmode=layertools` splits the fat JAR into four layers in dependency order:

```
dependencies          ~150MB   Changes: almost never (library updates)
spring-boot-loader    ~1MB     Changes: on Spring Boot version bumps
snapshot-dependencies ~5MB     Changes: on SNAPSHOT version bumps
application           ~1MB     Changes: on every commit
```

On a typical commit, Docker pushes only the `application` layer. The first push takes minutes; subsequent pushes take seconds.

---

## 16. Key Technical Decisions

### Gradle 8.8, not 8.11+

Gradle 8.11+ introduced a bug in Kotlin DSL metadata serialisation that caused `build.gradle.kts` files to fail with obscure `metadata.bin` errors. Gradle 8.8 is stable with Kotlin 2.1 and Spring Boot 3.4.

### No Detekt

Detekt 1.x is incompatible with Kotlin 2.1 (the K2 compiler). Detekt 2.x (K2-compatible) was still in alpha at the time of writing. ktlint handles formatting; Detekt will be re-enabled when 2.0 stable releases.

### `configuration-cache=false`

Gradle's configuration cache conflicts with several Spring Boot Gradle plugin features. It is explicitly disabled in `gradle.properties` to avoid intermittent cache poisoning errors.

### `@JvmInline` value classes for all IDs

Every domain identifier is a value class rather than a type alias or raw UUID. This costs nothing at runtime but makes the compiler reject `fun getShift(id: CourierId)` being called with a `StopId`. In a codebase with many UUID parameters, this catches entire classes of argument-transposition bugs.

### R2DBC in gateway, JPA in services

The API gateway uses R2DBC (reactive JDBC) because it handles concurrent WebSocket upgrades and token refresh requests at high frequency. Blocking a thread per request would require a large thread pool. Downstream services (route, delivery) use JPA because their operations are short sequential transactions where JPA's simplicity outweighs R2DBC's added complexity.

### `RefreshToken` insert via `R2dbcEntityTemplate`, not `save()`

Spring Data R2DBC's `save()` method determines whether to INSERT or UPDATE by checking if the entity's ID field is set. `RefreshToken` has a pre-set UUID ID (generated before save), so `save()` always issues an UPDATE — which finds no row and silently does nothing. Using `R2dbcEntityTemplate.insert()` forces an INSERT regardless of ID state.

### BCrypt `$2b$` prefix

Python's `bcrypt` library generates hashes with the `$2b$` prefix. Java's Spring Security BCrypt implementation historically generated `$2a$` hashes and rejected `$2b$` by default. Using `BCryptPasswordEncoder.BCryptVersion.$2B` makes the encoder accept both prefixes, allowing seed data generated by Python scripts to work.

### Kafka `JsonSerializer` + ISO-8601 timestamps

Spring Kafka's `JsonSerializer` uses a Jackson `ObjectMapper` that serialises `Instant` as a numeric Unix timestamp by default (`1774705896.354`). The shared domain model's `InstantSerializer` (kotlinx.serialization) expects ISO-8601 strings (`"2026-03-29T12:00:00Z"`). A custom `KafkaConfig` bean in each producer service overrides the default `ObjectMapper` with `WRITE_DATES_AS_TIMESTAMPS` disabled.

### Customer tracking: HTTP poll, not WebSocket

The customer tracking screen polls `GET /api/locations/couriers/{id}` every 10 seconds rather than opening a WebSocket. The usage pattern — open briefly, watch for a few minutes, close — does not justify a persistent connection. The Redis TTL (60 seconds) means a courier who goes offline causes the key to expire naturally; the next poll returns null and the UI shows "offline" without any server-side push.

### `@else if` without `as` in Angular templates

Angular 19's new control flow syntax (`@if`, `@else if`) only supports the `as` alias expression on the primary `@if` block. `@else if (signal(); as x)` is a compile error. The workaround is to use `@else if (signal())` and dereference with `signal()!.property` in the template body, or restructure to use nested `@if` blocks.

---

*Document generated from the Deli platform implementation — all ten phases.*
