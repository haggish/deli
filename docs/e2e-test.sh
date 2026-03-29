#!/usr/bin/env bash
# docs/e2e-test.sh
#
# Full end-to-end test of the Deli courier platform.
# Run from the project root after all services are started.
#
# Usage: bash docs/e2e-test.sh

set -euo pipefail

BASE="http://localhost:8080"
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

step() { echo -e "\n${GREEN}── $1 ${NC}"; }
info() { echo -e "${YELLOW}   $1${NC}"; }

# ── Reset state ───────────────────────────────────────────────────────────────
step "0. Reset state"
~/projects/deli/scripts/reset-state.sh

# ── Login ─────────────────────────────────────────────────────────────────────
step "1. Login as courier"
RESPONSE=$(curl -s -X POST "$BASE/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"courier@deli.local","password":"LocalDev123!"}')

TOKEN=$(echo "$RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")
info "Token: ${TOKEN:0:40}..."

# ── Register FCM token ────────────────────────────────────────────────────────
step "2. Register FCM token (so notification-service logs dry-run sends)"
curl -s -X PATCH "$BASE/api/notifications/fcm-token" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"fcmToken":"test-device-token-courier-123"}' > /dev/null
info "Courier FCM token registered"

# ── Start shift ───────────────────────────────────────────────────────────────
step "3. Start shift"
TODAY=$(date +%Y-%m-%d)
SHIFT=$(curl -s -X POST "$BASE/api/routes/shifts" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"scheduledDate\":\"$TODAY\"}" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['shiftId'])")
info "Shift ID: $SHIFT"

# ── Add stops ─────────────────────────────────────────────────────────────────
step "4. Add two stops"
STOP1=$(curl -s -X POST "$BASE/api/routes/shifts/$SHIFT/stops" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "packageId":"00000000-0000-0000-0000-000000000001",
    "customerId":"00000000-0000-0000-0000-000000000002",
    "address":{
      "street":"Unter den Linden","houseNumber":"1","city":"Berlin",
      "postalCode":"10117","country":"DE",
      "buzzerCode":"42","deliveryInstructions":"Ring twice"
    },
    "latitude":52.5170,"longitude":13.3888
  }' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")
info "Stop 1: $STOP1"

STOP2=$(curl -s -X POST "$BASE/api/routes/shifts/$SHIFT/stops" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "packageId":"00000000-0000-0000-0000-000000000003",
    "customerId":"00000000-0000-0000-0000-000000000004",
    "address":{
      "street":"Alexanderplatz","houseNumber":"1","city":"Berlin",
      "postalCode":"10178","country":"DE"
    },
    "latitude":52.5219,"longitude":13.4132
  }' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")
info "Stop 2: $STOP2"

# ── Verify route ──────────────────────────────────────────────────────────────
step "5. Verify active route"
curl -s "$BASE/api/routes/active" \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -c "
import sys, json
d = json.load(sys.stdin)['data']
print(f'  Shift status: {d[\"status\"]}')
print(f'  Total stops: {d[\"totalStops\"]}')
for s in d['stops']:
    print(f'  Stop {s[\"sequenceNumber\"]}: {s[\"deliveryAddress\"][\"street\"]} — {s[\"status\"]}')
"

# ── Deliver stop 1 ────────────────────────────────────────────────────────────
step "6. Arrive at stop 1 → confirm delivery"
curl -s -X PATCH "$BASE/api/routes/stops/$STOP1/start" \
  -H "Authorization: Bearer $TOKEN" > /dev/null

curl -s -X POST "$BASE/api/deliveries/stops/$STOP1/confirm" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"placement":"FRONT_DOOR","courierNote":"Left at the door"}' > /dev/null

curl -s -X PATCH "$BASE/api/routes/stops/$STOP1/complete" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"deliveryStatus":"DELIVERED"}' > /dev/null
info "Stop 1 delivered ✓"

# ── Fail stop 2 ───────────────────────────────────────────────────────────────
step "7. Arrive at stop 2 → report failure"
curl -s -X PATCH "$BASE/api/routes/stops/$STOP2/start" \
  -H "Authorization: Bearer $TOKEN" > /dev/null

curl -s -X POST "$BASE/api/deliveries/stops/$STOP2/fail" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"reason":"NO_ANSWER","courierNote":"Rang three times"}' > /dev/null

curl -s -X PATCH "$BASE/api/routes/stops/$STOP2/complete" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"deliveryStatus":"FAILED"}' > /dev/null
info "Stop 2 failed (NO_ANSWER) ✓"

# ── Final state ───────────────────────────────────────────────────────────────
step "8. Final state (shift should be COMPLETED)"
curl -s "$BASE/api/routes/active" \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -c "
import sys, json
r = json.load(sys.stdin)
if r['data'] is None:
    print('  No active shift — shift auto-completed ✓')
else:
    d = r['data']
    print(f'  Shift status: {d[\"status\"]}')
    print(f'  Completed: {d[\"completedStops\"]} / {d[\"totalStops\"]}')
"

# ── Customer tracking ─────────────────────────────────────────────────────────
step "9. Inject GPS position for customer tracking map"
COURIER_ID=$(echo "$TOKEN" | python3 -c "
import sys, json, base64
token = sys.stdin.read().strip()
payload = token.split('.')[1]
payload += '=' * (4 - len(payload) % 4)
print(json.loads(base64.b64decode(payload))['sub'])
")

~/projects/deli/scripts/inject-gps.sh "$COURIER_ID" 52.5170 13.3888

echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  E2E test complete ✓${NC}"
echo ""
echo "  Check notification-service logs for FCM dry-run messages:"
echo "    - Shift started"
echo "    - Route updated (x2)"
echo "    - Delivery confirmed"
echo "    - Delivery failed"
echo ""
echo "  Open the mobile app to verify UI:"
echo "    Courier: http://localhost:8100  (courier@deli.local)"
echo "    Customer tracking: http://localhost:8100/customer/tracking?courierId=$COURIER_ID"
echo -e "${GREEN}═══════════════════════════════════════════════════════${NC}"
