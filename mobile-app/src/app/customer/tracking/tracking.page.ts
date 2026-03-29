import {
  Component, OnInit, OnDestroy, OnChanges,
  signal, computed, ViewChild,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { interval, Subscription, switchMap, catchError, of } from 'rxjs';
import {
  IonContent, IonHeader, IonToolbar, IonTitle, IonSpinner, IonIcon,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { locateOutline, alertCircleOutline } from 'ionicons/icons';
import { AuthService } from '../../shared/services/auth.service';
import { environment } from '../../../environments/environment';
import { ApiResponse, CourierPositionResponse } from '../../shared/models/api.models';
import { MapComponent, MapMarker } from '../../shared/components/map.component';
import {ActivatedRoute} from "@angular/router";

@Component({
  selector: 'app-tracking',
  standalone: true,
  imports: [
    DecimalPipe, MapComponent,
    IonContent, IonHeader, IonToolbar, IonTitle, IonSpinner, IonIcon,
  ],
  template: `
    <ion-header>
      <ion-toolbar>
        <ion-title>Track delivery</ion-title>
        @if (position()) {
          <div slot="end" class="online-indicator">
            <span class="pulse-dot"></span>
            <span class="online-label">Live</span>
          </div>
        }
      </ion-toolbar>
    </ion-header>

    <ion-content class="tracking-content">

      <!-- Live map -->
      <div class="map-wrapper">
        @if (position()) {
          <app-map
            #courierMap
            [markers]="courierMapMarkers()"
            [zoom]="15"
            height="320px"
          />
        } @else {
          <div class="map-placeholder">
            <ion-icon name="locate-outline" />
            <p>Waiting for courier…</p>
          </div>
        }
      </div>

      <!-- Status card -->
      <div class="status-card">
        @if (loading()) {
          <div class="status-loading">
            <ion-spinner name="crescent" />
            <p>Finding your courier…</p>
          </div>
        } @else if (position()) {
          <div class="courier-row">
            <div class="courier-avatar">🛵</div>
            <div class="courier-info">
              <h3>Courier is on the way</h3>
              <p class="last-update">Updated {{ secondsSinceUpdate() }}s ago</p>
            </div>
            @if (isMoving()) {
              <div class="speed-chip">{{ speedDisplay() }}</div>
            }
          </div>

          <div class="coords-row">
            <div class="coord-item">
              <span class="coord-label">Latitude</span>
              <span class="coord-value">{{ position()!.latitude | number:'1.5-5' }}</span>
            </div>
            <div class="coord-divider"></div>
            <div class="coord-item">
              <span class="coord-label">Longitude</span>
              <span class="coord-value">{{ position()!.longitude | number:'1.5-5' }}</span>
            </div>
          </div>
        } @else {
          <div class="offline-row">
            <ion-icon name="alert-circle-outline" class="offline-icon" />
            <div>
              <h3>Courier not yet active</h3>
              <p>Your delivery is scheduled. Check back soon.</p>
            </div>
          </div>
        }
      </div>

    </ion-content>
  `,
  styles: [`
    ion-toolbar {
      --background: #0a0a14;
      --color: #fff;
      --border-color: #1a1a2e;
    }
    ion-title { font-weight: 700; }

    .online-indicator {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 0 16px;
    }

    .pulse-dot {
      width: 8px;
      height: 8px;
      background: #4cff91;
      border-radius: 50%;
      animation: blink 1.5s ease-in-out infinite;
    }

    @keyframes blink {
      0%, 100% { opacity: 1; box-shadow: 0 0 6px #4cff91; }
      50% { opacity: 0.3; box-shadow: none; }
    }

    .online-label {
      color: #4cff91;
      font-size: 12px;
      font-weight: 700;
      letter-spacing: 1px;
      text-transform: uppercase;
    }

    .tracking-content { --background: #0a0a14; }

    .map-wrapper {
      width: 100%;
      height: 320px;
      border-bottom: 1px solid #1a1a2e;
    }

    .map-placeholder {
      width: 100%;
      height: 320px;
      background: linear-gradient(135deg, #0d0d1a, #111128);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      color: #333;
      gap: 12px;
    }

    .map-placeholder ion-icon { font-size: 52px; color: #222; }
    .map-placeholder p { font-size: 14px; color: #444; margin: 0; }

    .status-card {
      background: #111128;
      margin: 16px;
      border-radius: 20px;
      padding: 20px;
      border: 1px solid #1a1a2e;
    }

    .status-loading {
      display: flex;
      align-items: center;
      gap: 16px;
      color: #888;
      --color: #7c6aff;
    }

    .courier-row {
      display: flex;
      align-items: center;
      gap: 14px;
      margin-bottom: 16px;
    }

    .courier-avatar {
      font-size: 32px;
      width: 48px;
      height: 48px;
      background: #1a1a2e;
      border-radius: 14px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .courier-info { flex: 1; }

    .courier-info h3 {
      color: #fff;
      font-size: 15px;
      font-weight: 700;
      margin: 0 0 3px;
    }

    .last-update {
      color: #555;
      font-size: 12px;
      margin: 0;
    }

    .speed-chip {
      background: rgba(124, 106, 255, 0.15);
      border: 1px solid #7c6aff44;
      border-radius: 20px;
      padding: 4px 12px;
      color: #7c6aff;
      font-size: 12px;
      font-weight: 700;
      white-space: nowrap;
    }

    .coords-row {
      display: flex;
      align-items: center;
      background: #0a0a14;
      border-radius: 12px;
      overflow: hidden;
    }

    .coord-item {
      flex: 1;
      padding: 12px 16px;
    }

    .coord-divider {
      width: 1px;
      height: 40px;
      background: #1a1a2e;
    }

    .coord-label {
      display: block;
      color: #444;
      font-size: 10px;
      letter-spacing: 1px;
      text-transform: uppercase;
      margin-bottom: 3px;
    }

    .coord-value {
      display: block;
      color: #7c6aff;
      font-size: 13px;
      font-weight: 600;
      font-family: monospace;
    }

    .offline-row {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .offline-icon { font-size: 32px; color: #333; flex-shrink: 0; }

    .offline-row h3 { color: #888; font-size: 15px; font-weight: 600; margin: 0 0 4px; }
    .offline-row p { color: #555; font-size: 13px; margin: 0; }
  `],
})
export class TrackingPage implements OnInit, OnDestroy {
  @ViewChild('courierMap') courierMap?: MapComponent;

  loading = signal(true);
  position = signal<CourierPositionResponse | null>(null);
  private secondsSince = signal(0);
  private pollSub?: Subscription;
  private tickSub?: Subscription;

  isMoving = computed(() => (this.position()?.speedKmh ?? 0) > 2);
  speedDisplay = computed(() => {
    const s = this.position()?.speedKmh;
    return s != null ? `${Math.round(s)} km/h` : '';
  });
  secondsSinceUpdate = computed(() => this.secondsSince());

  courierMapMarkers = computed((): MapMarker[] => {
    const pos = this.position();
    if (!pos) return [];
    return [{
      lat: pos.latitude,
      lng: pos.longitude,
      label: '🛵',
      color: 'blue',
      popup: `Courier · ${this.speedDisplay() || 'Stationary'}`,
    }];
  });

  constructor(
    private http: HttpClient,
    private authService: AuthService,
    private route: ActivatedRoute,
  ) {
    addIcons({ locateOutline, alertCircleOutline });
  }

  ngOnInit(): void {
    // Immediate fetch
    this.fetchPosition().subscribe((pos) => {
      this.position.set(pos);
      this.loading.set(false);
    });

    // Poll every 10 seconds
    this.pollSub = interval(10_000)
      .pipe(switchMap(() => this.fetchPosition()))
      .subscribe((pos) => {
        this.position.set(pos);
        this.secondsSince.set(0);
      });

    // Tick "updated N seconds ago"
    this.tickSub = interval(1000).subscribe(() =>
      this.secondsSince.update((n) => n + 1),
    );
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
    this.tickSub?.unsubscribe();
  }

  private fetchPosition() {
    // Use courierId from query param if provided, otherwise fall back to own userId
    const courierId = this.route.snapshot.queryParamMap.get('courierId')
      ?? this.authService.session()?.userId;

    return this.http
      .get<ApiResponse<CourierPositionResponse>>(
        `${environment.apiUrl}/api/locations/couriers/${courierId}`,
      )
      .pipe(
        catchError(() => of({ success: false, data: null, error: null })),
        switchMap((r) => of(r.data)),
      );
  }
}
