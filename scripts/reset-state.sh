#!/usr/bin/env bash
set -e
echo "Resetting all state..."

# 1. Database
docker exec deli-postgres psql -U courier_user -d courierdb -c "
  DELETE FROM delivery.delivery_records;
  DELETE FROM route.stops;
  DELETE FROM route.shifts;
"
echo "  ✓ Database cleared"

# 2. Redis
docker exec deli-redis redis-cli -a redis_local FLUSHDB > /dev/null
echo "  ✓ Redis cleared"

# 3. Kafka topics
docker exec deli-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --delete \
  --topic shift.started,shift.completed,route.updated,stop.assigned,delivery.confirmed,delivery.failed,location.updated \
  2>/dev/null || true
echo "  ✓ Kafka topics cleared (will be recreated on next publish)"

echo "Done. Restart notification-service before running tests."
