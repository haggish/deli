# Deli — Daily Development Workflow

Quick reference for every command you need day-to-day.

---

## Starting a Session

### 1. Start infrastructure

```bash
cd ~/projects/deli
docker compose up -d
```

Verify everything is healthy:

```bash
docker compose ps
```

All containers should show `healthy` or `running`. If Kafka takes a moment, wait 10 seconds and check again.

### 2. Start backend services

Open five terminals (or use IntelliJ IDEA run configurations):

```bash
# Terminal 1 — API Gateway (JWT auth, routing)
./gradlew :services:api-gateway:bootRun

# Terminal 2 — Route service (shifts, stops)
./gradlew :services:route-service:bootRun

# Terminal 3 — Delivery service (confirm, fail, S3)
./gradlew :services:delivery-service:bootRun

# Terminal 4 — Location service (GPS WebSocket, TimescaleDB)
./gradlew :services:location-service:bootRun

# Terminal 5 — Notification service (Kafka → FCM)
./gradlew :services:notification-service:bootRun
```

All services activate the `local` Spring profile automatically via
`spring.profiles.active: ${SPRING_PROFILES_ACTIVE:local}` in each `application.yml`.

### 3. Start the mobile app

```bash
cd ~/projects/deli/mobile-app
npm start
# Opens at http://localhost:8100
```

---

## Seed Credentials

| Role | Email | Password | User ID |
|---|---|---|---|
| Courier | courier@deli.local | LocalDev123! | fb30f9fa-0337-44b7-a32f-93a8a4ee7aac |
| Customer | customer@deli.local | LocalDev123! | *(decode from JWT)* |

### Get a token

```bash
# Courier token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"courier@deli.local","password":"LocalDev123!"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

echo "Token: ${TOKEN:0:40}..."
```

### Decode the JWT to get userId and role

```bash
echo $TOKEN | python3 -c "
import sys, json, base64
token = sys.stdin.read().strip()
payload = token.split('.')[1]
payload += '=' * (4 - len(payload) % 4)
print(json.dumps(json.loads(base64.b64decode(payload)), indent=2))
"
```

---

## Service Ports

| Service | Port | Purpose |
|---|---|---|
| API Gateway | 8080 | All external traffic — use this for curl tests |
| Route service | 8081 | Direct access only for debugging |
| Delivery service | 8082 | Direct access only for debugging |
| Location service | 8083 | Direct access only for debugging |
| Notification service | 8084 | Direct access only for debugging |
| Kafka UI | 8090 | Browse topics at http://localhost:8090 |
| MinIO console | 9001 | Browse buckets at http://localhost:9001 (minioadmin / minioadmin) |
| PostgreSQL | 5432 | `psql -h localhost -U courier_user -d courierdb` |
| TimescaleDB | 5433 | `psql -h localhost -U gps_user -d gpsdb` |
| Redis | 6379 | `redis-cli -h localhost -a redis_local` |

---

## Reset State

### Full reset (DB + Redis + Kafka)

Run this between E2E test runs for a clean slate:

```bash
~/projects/deli/scripts/reset-state.sh
```

This deletes all shifts, stops, delivery records, clears Redis, and drops all Kafka topics.
Restart notification-service after running this (Kafka consumer connections drop when topics are deleted).

### Database only

```bash
docker exec deli-postgres psql -U courier_user -d courierdb -c "
  DELETE FROM delivery.delivery_records;
  DELETE FROM route.stops;
  DELETE FROM route.shifts;
"
```

### Redis only

```bash
docker exec deli-redis redis-cli -a redis_local FLUSHDB
```

### Kafka topics only

```bash
docker exec deli-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --delete \
  --topic shift.started,shift.completed,route.updated,stop.assigned,delivery.confirmed,delivery.failed,location.updated \
  2>/dev/null || true
```

### Kafka consumer group offsets (when topics still exist)

Stop notification-service first, then reset each group:

```bash
for group in \
  notification-service-shift-started \
  notification-service-shift-completed \
  notification-service-route-updated \
  notification-service-delivery-confirmed \
  notification-service-delivery-failed; do
  docker exec deli-kafka /opt/kafka/bin/kafka-consumer-groups.sh \
    --bootstrap-server localhost:9092 \
    --group "$group" --all-topics \
    --reset-offsets --to-latest --execute
  echo "Reset $group"
done
```

---

## E2E Test Sequence

Run the full automated E2E test:

```bash
bash ~/projects/deli/docs/e2e-test.sh
```

Or step through manually:

```bash
# 1. Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"courier@deli.local","password":"LocalDev123!"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

# 2. Start shift
SHIFT=$(curl -s -X POST http://localhost:8080/api/routes/shifts \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"scheduledDate":"2026-03-29"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['shiftId'])")

echo "Shift: $SHIFT"

# 3. Add a stop
STOP=$(curl -s -X POST "http://localhost:8080/api/routes/shifts/$SHIFT/stops" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "packageId":"00000000-0000-0000-0000-000000000001",
    "customerId":"00000000-0000-0000-0000-000000000002",
    "address":{
      "street":"Unter den Linden","houseNumber":"1",
      "city":"Berlin","postalCode":"10117","country":"DE",
      "buzzerCode":"42","deliveryInstructions":"Ring twice"
    },
    "latitude":52.5170,"longitude":13.3888
  }' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")

echo "Stop: $STOP"

# 4. Arrive at stop
curl -s -X PATCH "http://localhost:8080/api/routes/stops/$STOP/start" \
  -H "Authorization: Bearer $TOKEN" > /dev/null && echo "Arrived ✓"

# 5. Confirm delivery
curl -s -X POST "http://localhost:8080/api/deliveries/stops/$STOP/confirm" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"placement":"FRONT_DOOR","courierNote":"Left at the door"}' \
  > /dev/null && echo "Confirmed ✓"

# 6. Mark complete in route-service
curl -s -X PATCH "http://localhost:8080/api/routes/stops/$STOP/complete" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"deliveryStatus":"DELIVERED"}' \
  > /dev/null && echo "Route updated ✓"

# 7. Check active route
curl -s http://localhost:8080/api/routes/active \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

---

## Customer Tracking Map

### Inject a fake GPS position

```bash
# Default: courier at Unter den Linden, Berlin
~/projects/deli/scripts/inject-gps.sh

# Custom courier ID and location
~/projects/deli/scripts/inject-gps.sh <courierId> <lat> <lng>

# Example with real values
~/projects/deli/scripts/inject-gps.sh \
  fb30f9fa-0337-44b7-a32f-93a8a4ee7aac \
  52.5219 13.4132
```

The position is valid for 1 hour. Open the customer tracking page:

```
http://localhost:8100/customer/tracking?courierId=fb30f9fa-0337-44b7-a32f-93a8a4ee7aac
```

### Verify the position is in Redis

```bash
docker exec deli-redis redis-cli -a redis_local \
  GET "courier:position:fb30f9fa-0337-44b7-a32f-93a8a4ee7aac"
```

### Verify the API returns the position

```bash
curl -s "http://localhost:8080/api/locations/couriers/fb30f9fa-0337-44b7-a32f-93a8a4ee7aac" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

---

## Building

### Build everything

```bash
./gradlew build --no-configuration-cache
```

### Build a single service

```bash
./gradlew :services:route-service:build --no-configuration-cache
```

### Build without running tests

```bash
./gradlew build -x test --no-configuration-cache
```

### Run tests only

```bash
./gradlew test --no-configuration-cache
```

### Run ktlint (code style check)

```bash
./gradlew ktlintCheck --no-configuration-cache
```

### Auto-fix ktlint violations

```bash
./gradlew ktlintFormat --no-configuration-cache
```

### Build Docker images locally

```bash
# Build JAR first
./gradlew :services:api-gateway:bootJar --no-configuration-cache

# Build Docker image
docker build -t deli/api-gateway:local services/api-gateway/
```

---

## Database Access

### PostgreSQL — query shifts and stops

```bash
docker exec deli-postgres psql -U courier_user -d courierdb
```

Useful queries:

```sql
-- See all shifts
SELECT id, courier_id, status, scheduled_date, total_stops, completed_stops
FROM route.shifts ORDER BY created_at DESC;

-- See stops for the active shift
SELECT s.sequence_number, s.status, s.delivery_address
FROM route.stops s
JOIN route.shifts sh ON s.shift_id = sh.id
WHERE sh.status = 'ACTIVE'
ORDER BY s.sequence_number;

-- See delivery records
SELECT stop_id, status, placement, failure_reason, confirmed_at
FROM delivery.delivery_records ORDER BY created_at DESC;

-- See gateway users
SELECT id, email, role FROM gateway_users;
```

### TimescaleDB — query GPS pings

```bash
docker exec deli-timescale psql -U gps_user -d gpsdb
```

```sql
-- Recent pings for a courier
SELECT recorded_at, latitude, longitude, speed_kmh, accuracy_metres
FROM gps.location_pings
WHERE courier_id = 'fb30f9fa-0337-44b7-a32f-93a8a4ee7aac'
ORDER BY recorded_at DESC
LIMIT 20;

-- Hypertable chunk info
SELECT * FROM timescaledb_information.chunks
WHERE hypertable_name = 'location_pings';
```

### Redis — inspect keys

```bash
# List all courier position keys
docker exec deli-redis redis-cli -a redis_local KEYS "courier:position:*"

# List all FCM token keys
docker exec deli-redis redis-cli -a redis_local KEYS "fcm:*"

# Get TTL of a key
docker exec deli-redis redis-cli -a redis_local TTL "courier:position:fb30f9fa-0337-44b7-a32f-93a8a4ee7aac"

# Count all keys
docker exec deli-redis redis-cli -a redis_local DBSIZE
```

---

## Kafka Inspection

### List all topics

```bash
docker exec deli-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --list
```

### Watch messages on a topic in real time

```bash
# Watch shift events
docker exec deli-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic shift.started \
  --from-beginning

# Watch delivery events
docker exec deli-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic delivery.confirmed \
  --from-beginning
```

Or use the Kafka UI at **http://localhost:8090** — browse topics, view messages, inspect consumer group lag.

### Check consumer group lag

```bash
docker exec deli-kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group notification-service-shift-started \
  --describe
```

---

## Notification Service — Register a Test FCM Token

Register a fake token so notification-service logs dry-run sends instead of silently skipping:

```bash
curl -s -X PATCH http://localhost:8080/api/notifications/fcm-token \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"fcmToken":"test-device-token-123"}' | python3 -m json.tool
```

After registering, any Kafka event for this courier will log:

```
[DRY RUN] Would send FCM notification: title='Shift started' body='...' token=test-device-tok...
```

---

## Mobile App

### Start dev server

```bash
cd ~/projects/deli/mobile-app
npm start
# http://localhost:8100
```

### Production build (type-check included)

```bash
npm run build:prod
```

### Type-check only (fast)

```bash
npx tsc --noEmit
```

### Clear Angular build cache (if hot-reload breaks)

```bash
rm -rf .angular/cache
npm start
```

### Clear session (equivalent to logout)

In the browser console:

```javascript
localStorage.removeItem('deli_session'); location.reload();
```

---

## Stopping a Session

### Stop services

`Ctrl+C` in each service terminal.

### Stop infrastructure

```bash
cd ~/projects/deli
docker compose down
```

To also delete all data volumes (full reset):

```bash
docker compose down -v
```

---

## Common Errors and Fixes

### `Failed to configure a DataSource — no profiles currently active`

The service started without the `local` profile. Use:

```bash
./gradlew :services:route-service:bootRun --args='--spring.profiles.active=local'
```

Or ensure `spring.profiles.active: ${SPRING_PROFILES_ACTIVE:local}` is in `application.yml`.

### `Assignments can only be reset if the group is inactive`

The notification-service is still running. Stop it first, then reset consumer group offsets.

### `No static resource api/routes/shifts/{id}/stops`

The endpoint does not exist in the controller. Check `RouteController.kt` for the `POST /shifts/{shiftId}/stops` method.

### Kafka `JsonDecodingException` — unexpected token at `occurredAt`

The Kafka producer is serialising `Instant` as a numeric timestamp. Check that the service has a `KafkaConfig.kt` bean that disables `WRITE_DATES_AS_TIMESTAMPS` on the Jackson `ObjectMapper`.

### Customer tracking shows "Courier not yet active"

Either the Redis key has expired (TTL 60s for real pings, 1h for injected) or the API is called with the customer's own userId instead of the courier's. Use:

```bash
~/projects/deli/scripts/inject-gps.sh
# Then open with ?courierId=fb30f9fa-0337-44b7-a32f-93a8a4ee7aac
```

### `MissingKotlinParameterException` — NULL value for non-nullable parameter

A Redis-cached object is missing a field that Kotlin expects to be non-null. Add `= null` defaults and `@JsonIgnoreProperties(ignoreUnknown = true)` to the data class, then restart the service.

### Leaflet map is blank / tiles not loading

Check browser console for mixed-content errors (HTTPS page loading HTTP tiles). In local dev this is not an issue. Also check that `leaflet.css` is listed in the `styles` array in `angular.json` and that `npm install` has been run after adding leaflet.

---

## Quick Reference Card

```
Start infra          docker compose up -d
Stop infra           docker compose down
Start service        ./gradlew :services:<name>:bootRun
Start mobile app     cd mobile-app && npm start
Reset everything     ~/projects/deli/scripts/reset-state.sh
Inject GPS           ~/projects/deli/scripts/inject-gps.sh
Run E2E test         bash ~/projects/deli/docs/e2e-test.sh
Build all            ./gradlew build --no-configuration-cache
Run tests            ./gradlew test --no-configuration-cache
Lint (ktlint)        ./gradlew ktlintCheck --no-configuration-cache
Fix lint             ./gradlew ktlintFormat --no-configuration-cache
Kafka UI             http://localhost:8090
MinIO console        http://localhost:9001
Mobile app           http://localhost:8100
API base             http://localhost:8080
```
