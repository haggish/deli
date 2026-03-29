import { Injectable, OnDestroy, signal } from '@angular/core';
import { Geolocation, Position } from '@capacitor/geolocation';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

export interface GpsState {
  isStreaming: boolean;
  lastPosition: { latitude: number; longitude: number; accuracy: number; speed: number | null; heading: number | null } | null;
  error: string | null;
}

@Injectable({ providedIn: 'root' })
export class GpsService implements OnDestroy {
  private ws: WebSocket | null = null;
  private watchId: string | null = null;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;

  readonly state = signal<GpsState>({
    isStreaming: false,
    lastPosition: null,
    error: null,
  });

  constructor(private authService: AuthService) {}

  // ── Start streaming GPS to location-service via WebSocket ─────────────────

  async startStreaming(shiftId: string): Promise<void> {
    await this.requestPermission();
    this.connectWebSocket(shiftId);
    this.startWatchingPosition(shiftId);
  }

  stopStreaming(): void {
    this.reconnectTimer && clearTimeout(this.reconnectTimer);
    this.ws?.close(1000, 'Shift ended');
    this.ws = null;

    if (this.watchId) {
      Geolocation.clearWatch({ id: this.watchId });
      this.watchId = null;
    }

    this.state.set({ isStreaming: false, lastPosition: null, error: null });
  }

  // ── WebSocket connection ───────────────────────────────────────────────────

  private connectWebSocket(shiftId: string): void {
    const session = this.authService.session();
    if (!session) return;

    const wsUrl = environment.wsUrl + '/ws/location';
    this.ws = new WebSocket(wsUrl);

    this.ws.onopen = () => {
      this.state.update((s) => ({ ...s, isStreaming: true, error: null }));
    };

    this.ws.onmessage = (event) => {
      // Server acknowledges each ping with {"status":"ok"} or {"status":"error"}
      const msg = JSON.parse(event.data);
      if (msg.status === 'error') {
        this.state.update((s) => ({ ...s, error: msg.message }));
      }
    };

    this.ws.onerror = () => {
      this.state.update((s) => ({ ...s, isStreaming: false, error: 'Connection error' }));
    };

    this.ws.onclose = (event) => {
      this.state.update((s) => ({ ...s, isStreaming: false }));
      // Auto-reconnect unless intentionally closed
      if (event.code !== 1000) {
        this.reconnectTimer = setTimeout(() => this.connectWebSocket(shiftId), 5000);
      }
    };
  }

  // ── Geolocation watching ──────────────────────────────────────────────────

  private async startWatchingPosition(shiftId: string): Promise<void> {
    this.watchId = await Geolocation.watchPosition(
      { enableHighAccuracy: true, timeout: 15000 },
      (position: Position | null, err?: GeolocationPositionError) => {
        if (err || !position) {
          this.state.update((s) => ({ ...s, error: err?.message ?? 'GPS unavailable' }));
          return;
        }

        this.state.update((s) => ({
          ...s,
          lastPosition: { latitude: position.coords.latitude, longitude: position.coords.longitude, accuracy: position.coords.accuracy, speed: position.coords.speed, heading: position.coords.heading },
          error: null,
        }));

        this.sendPing(shiftId, position);
      },
    );
  }

  private sendPing(shiftId: string, position: Position): void {
    if (this.ws?.readyState !== WebSocket.OPEN) return;

    const ping = {
      shiftId,
      latitude: position.coords.latitude,
      longitude: position.coords.longitude,
      accuracyMetres: position.coords.accuracy,
      speedKmh: position.coords.speed != null ? position.coords.speed * 3.6 : null,
      headingDegrees: position.coords.heading,
      recordedAt: new Date(position.timestamp).toISOString(),
    };

    this.ws.send(JSON.stringify(ping));
  }

  private async requestPermission(): Promise<void> {
    const permission = await Geolocation.requestPermissions();
    if (permission.location !== 'granted') {
      throw new Error('Location permission denied');
    }
  }

  ngOnDestroy(): void {
    this.stopStreaming();
  }
}
