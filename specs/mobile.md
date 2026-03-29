# Deli — Mobile Device Testing Guide

How to run the app on a real phone with native geolocation, WebSocket GPS streaming,
and the customer tracking map updating from a real device position.

---

## Overview

Testing on a real device requires three things:

1. The backend services are reachable from the phone over the network
2. The app is served over **HTTPS** (Android and iOS refuse geolocation on plain HTTP)
3. The Angular environment file points at the correct URLs

The recommended approach is **ngrok**, which creates public HTTPS tunnels to your
local services with no firewall or router configuration required.

---

## Prerequisites

- Android phone with USB debugging enabled, **or** any phone on the same WiFi network
- A free [ngrok account](https://ngrok.com) (takes 30 seconds to create)
- All backend services running locally (see `docs/workflow.md`)
- Android Studio installed (only needed for native APK builds)

---

## Step 1 — Find your machine's local IP

```bash
# Run in WSL
ip route show default | awk '{print $3}'
# or
hostname -I | awk '{print $1}'
```

Note the result — something like `192.168.1.45`. Your phone must be on the same
WiFi network as this machine for direct IP access to work.

---

## Step 2 — Install and configure ngrok

```bash
# Install ngrok in WSL
curl -sSL https://ngrok-agent.s3.amazonaws.com/ngrok.asc \
  | sudo tee /etc/apt/trusted.gpg.d/ngrok.asc >/dev/null
echo "deb https://ngrok-agent.s3.amazonaws.com buster main" \
  | sudo tee /etc/apt/sources.list.d/ngrok.list
sudo apt update && sudo apt install ngrok

# Add your auth token (from https://dashboard.ngrok.com/authtokens)
ngrok config add-authtoken <your-token>
```

---

## Step 3 — Create tunnels

You need three tunnels: the API gateway, the location service WebSocket, and the app itself.
Open three separate terminals:

```bash
# Terminal A — API gateway (REST)
ngrok http 8080
# Note the https:// URL, e.g. https://abc123.ngrok-free.app

# Terminal B — Location service (WebSocket)
ngrok http 8083
# Note the https:// URL, e.g. https://def456.ngrok-free.app

# Terminal C — Mobile app dev server
ngrok http 8100
# Note the https:// URL, e.g. https://ghi789.ngrok-free.app
```

Keep all three terminals open. Free ngrok URLs change every time you restart —
note the current URLs before continuing.

---

## Step 4 — Update the Angular environment file

Open `mobile-app/src/environments/environment.ts` and replace the URLs:

```typescript
export const environment = {
  production: false,
  apiUrl: 'https://abc123.ngrok-free.app',   // Terminal A URL
  wsUrl: 'wss://def456.ngrok-free.app',      // Terminal B URL — note wss:// not ws://
};
```

> **Important:** use `wss://` (secure WebSocket) for the location service URL.
> ngrok terminates TLS, so the app sees a secure connection. Browsers and Android
> require WSS for WebSocket connections from HTTPS pages.

---

## Step 5 — Allow ngrok origins in the API gateway

Add the ngrok wildcard to the gateway's CORS configuration so the phone's browser
can make cross-origin requests.

Open `services/api-gateway/src/main/resources/application.yml` and add to the
`local` profile:

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins:
              - "http://localhost:8100"
              - "https://*.ngrok-free.app"    # add this line
            allowedMethods: ["*"]
            allowedHeaders: ["*"]
            allowCredentials: true
```

Restart the API gateway after saving.

---

## Step 6 — Start the mobile app dev server on all interfaces

```bash
cd ~/projects/deli/mobile-app
npx ng serve --host 0.0.0.0 --port 8100 --disable-host-check
```

The `--host 0.0.0.0` flag makes the dev server accept connections from any
network interface, not just localhost. The `--disable-host-check` flag prevents
Angular from rejecting requests from the ngrok domain.

---

## Step 7 — Open the app on the phone

### Option A — Mobile browser (quickest, no install required)

Open the Terminal C ngrok URL (`https://ghi789.ngrok-free.app`) in Chrome on
Android or Safari on iOS.

If ngrok shows an interstitial warning page, tap **Visit Site** to proceed.
You can dismiss this permanently by passing a header — see Troubleshooting below.

To make it feel more native, use **Add to Home Screen** from the browser menu.
The app will open full-screen without browser chrome.

### Option B — Native Android APK (full native features)

This gives you real Capacitor geolocation (higher accuracy, background updates)
and FCM push notifications.

**Update `capacitor.config.ts`** to point at your ngrok URLs:

```typescript
import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.deli.courier',
  appName: 'Deli',
  webDir: 'www',
  server: {
    // Point at your ngrok app tunnel so the APK loads from your dev server
    // Remove this block for production builds
    url: 'https://ghi789.ngrok-free.app',
    cleartext: false,
  },
  plugins: {
    Geolocation: {},
    PushNotifications: {
      presentationOptions: ['badge', 'sound', 'alert'],
    },
    SplashScreen: {
      launchAutoHide: true,
      backgroundColor: '#0f0f0f',
    },
    StatusBar: {
      style: 'Dark',
      backgroundColor: '#0f0f0f',
    },
  },
};

export default config;
```

**Build and sync:**

```bash
cd ~/projects/deli/mobile-app

# Build the web assets
npm run build:prod

# Sync web assets to the Android project
npx cap sync android

# Open Android Studio
npx cap open android
```

**In Android Studio:**

1. Connect your phone via USB
2. Enable developer mode on the phone: Settings → About phone → tap Build number 7 times
3. Enable USB debugging: Settings → Developer options → USB debugging
4. Select your device in the device dropdown at the top
5. Click **Run ▶**

The app installs and launches on your phone.

---

## Step 8 — Test geolocation

### As the courier

1. Open the app on the phone and log in as the courier:
    - Email: `courier@deli.local`
    - Password: `LocalDev123!`

2. The Dashboard appears. If a shift is active, the GPS starts automatically.
   If not, tap **Start shift** first.

3. The phone prompts for location permission — tap **Allow**.

4. Watch the **location-service terminal** on your laptop. You should see:

   ```
   INFO  ... : Courier fb30f9fa connected via WebSocket (session xyz)
   DEBUG ... : Processed ping for courier fb30f9fa at (52.xxxx, 13.xxxx)
   DEBUG ... : Updated position cache for courier fb30f9fa
   DEBUG ... : Published LocationUpdated for courier fb30f9fa
   ```

5. The GPS indicator in the toolbar turns yellow (active) when the WebSocket
   connection is established and pings are flowing.

### As the customer (on a laptop browser)

While the courier app is streaming GPS from the phone, open the tracking page
on your laptop browser:

```
http://localhost:8100/customer/tracking?courierId=fb30f9fa-0337-44b7-a32f-93a8a4ee7aac
```

Log in as the customer when prompted. The map should show a blue marker at your
phone's current GPS position, updating every 10 seconds as new pings arrive.

### Verify the pipeline end-to-end

```bash
# Check Redis has the real GPS position
docker exec deli-redis redis-cli -a redis_local \
  GET "courier:position:fb30f9fa-0337-44b7-a32f-93a8a4ee7aac"

# Check the API returns it
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"courier@deli.local","password":"LocalDev123!"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

curl -s "http://localhost:8080/api/locations/couriers/fb30f9fa-0337-44b7-a32f-93a8a4ee7aac" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

The API response should show the latitude and longitude matching your phone's
real location.

---

## Reverting after mobile testing

When you are done testing on the phone, revert the environment file so the
app works locally again:

```typescript
// mobile-app/src/environments/environment.ts — restore to local URLs
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  wsUrl: 'ws://localhost:8083',
};
```

And revert `capacitor.config.ts` if you changed the `server.url`.

Then restart the dev server normally:

```bash
npm start
# Opens at http://localhost:8100
```

---

## Troubleshooting

### Geolocation permission denied in the mobile browser

The page must be served over HTTPS. Confirm you are using the ngrok `https://`
URL for the app, not the local IP directly. Chrome on Android refuses the
Geolocation API on non-HTTPS origins.

### ngrok shows "Visit Site" interstitial on every request

This is the free tier warning. To skip it programmatically, add this header to
API requests: `ngrok-skip-browser-warning: true`. For the app itself, the
interstitial only appears on the first load — tap Visit Site once and it does
not repeat for that session.

### WebSocket connection fails on the phone

- Confirm `wsUrl` uses `wss://` not `ws://`
- Confirm the location-service ngrok tunnel is still running (check Terminal B)
- Check the browser console on the phone via Chrome DevTools remote debugging:
  `chrome://inspect` on your laptop while the phone is connected via USB

### ngrok URLs changed after a restart

Free ngrok URLs are random and change on every restart. After restarting ngrok,
update `environment.ts` with the new URLs and restart the Angular dev server.
A paid ngrok account lets you reserve a stable custom subdomain like
`deli-api.ngrok.io` that never changes.

### GPS pings arrive but customer map does not update

The tracking page polls every 10 seconds. Wait up to 10 seconds after the first
ping arrives. If it still does not update, check:

1. The customer is logged in (the tracking page requires authentication)
2. The `?courierId=` query parameter matches the courier's actual user ID
3. The Redis key has not expired (real pings set a 60-second TTL — if the phone
   goes idle and stops sending pings, the key expires and the tracker shows offline)

### `MissingKotlinParameterException` in location-service logs

The `CourierPosition` data class has a non-nullable field that is missing from
a Redis entry. Ensure `CourierPosition.kt` has nullable defaults for `shiftId`
and `updatedAt`:

```kotlin
@JsonIgnoreProperties(ignoreUnknown = true)
data class CourierPosition(
    val courierId: String,
    val shiftId: String? = null,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Double? = null,
    val headingDegrees: Double? = null,
    val updatedAt: java.time.Instant? = null,
    val isOnline: Boolean = true,
)
```

### App installed as APK but API calls fail

The Capacitor `server.url` in `capacitor.config.ts` overrides where the native
WebView loads content from, but HTTP requests from the Angular app still go to
whatever `environment.apiUrl` is set to. Confirm `environment.ts` is pointing
at the ngrok API gateway URL, not `localhost`.

### Location accuracy is low indoors

This is normal — GPS requires line of sight to satellites. Indoors the device
falls back to WiFi and cell tower triangulation, which gives accuracy of 10–50
metres rather than 3–5 metres. For testing purposes this is fine. The
`accuracyMetres` field in each ping reflects the device's own accuracy estimate.

---

## Quick Reference

```
ngrok API gateway    ngrok http 8080
ngrok location WS    ngrok http 8083
ngrok mobile app     ngrok http 8100

Start app (all IF)   npx ng serve --host 0.0.0.0 --port 8100 --disable-host-check
Build for Android    npm run build:prod && npx cap sync android
Open Android Studio  npx cap open android

Verify GPS in Redis  docker exec deli-redis redis-cli -a redis_local KEYS "courier:position:*"
Watch location logs  check location-service terminal for "Processed ping" lines

Revert to local dev  restore environment.ts to localhost URLs, npm start
```
