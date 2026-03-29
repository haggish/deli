#!/usr/bin/env bash
# inject-gps.sh — inserts a fake courier position into Redis for testing
# Usage:
#   ./scripts/inject-gps.sh                          # default courier, Berlin centre
#   ./scripts/inject-gps.sh <courierId>              # specific courier
#   ./scripts/inject-gps.sh <courierId> <lat> <lng>  # specific location

COURIER_ID="${1:-fb30f9fa-0337-44b7-a32f-93a8a4ee7aac}"
LAT="${2:-52.5170}"
LNG="${3:-13.3888}"

docker exec deli-redis redis-cli -a redis_local SET \
  "courier:position:$COURIER_ID" \
  "{\"courierId\":\"$COURIER_ID\",\"shiftId\":null,\"latitude\":$LAT,\"longitude\":$LNG,\"speedKmh\":28.5,\"headingDegrees\":90,\"updatedAt\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",\"isOnline\":true}" \
  EX 3600 2>/dev/null \
  && echo "Position injected for courier $COURIER_ID at ($LAT, $LNG) — valid for 1 hour" \
  && echo "Open: http://localhost:8100/customer/tracking?courierId=$COURIER_ID"
