import { Component, OnInit, OnDestroy, signal, computed } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { interval, Subscription, switchMap, catchError, of } from 'rxjs';
import { IonContent, IonHeader, IonToolbar, IonTitle, IonSpinner, IonIcon } from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { locateOutline, alertCircleOutline } from 'ionicons/icons';
import { AuthService } from '../../shared/services/auth.service';
import { environment } from '../../../environments/environment';
import { ApiResponse, CourierPositionResponse } from '../../shared/models/api.models';

@Component({
  selector: 'app-tracking',
  standalone: true,
  imports: [DecimalPipe, IonContent, IonHeader, IonToolbar, IonTitle, IonSpinner, IonIcon],
  template: `
    <ion-header>
      <ion-toolbar><ion-title>Track delivery</ion-title></ion-toolbar>
    </ion-header>
    <ion-content class="tracking-content">
      <div class="map-area">
        @if (position()) {
          <div class="map-mock">
            <div class="courier-pin" [class.moving]="isMoving()">🛵</div>
            <div class="pulse-ring"></div>
          </div>
          <div class="speed-badge">{{ speedDisplay() }}</div>
        } @else {
          <div class="map-placeholder">
            <ion-icon name="locate-outline" />
            <p>Waiting for courier location…</p>
          </div>
        }
      </div>
      <div class="status-card">
        @if (loading()) {
          <div class="status-loading"><ion-spinner name="crescent" /><p>Finding your courier…</p></div>
        } @else if (position()) {
          <div class="courier-online">
            <div class="online-dot"></div>
            <div class="courier-info">
              <h3>Courier is on the way</h3>
              <p class="last-update">Updated {{ secondsSinceUpdate() }}s ago</p>
            </div>
            <div class="heading-indicator" [style.transform]="'rotate(' + (position()!.headingDegrees ?? 0) + 'deg)'">↑</div>
          </div>
          <div class="position-details">
            <div class="detail-item">
              <span class="detail-label">Lat</span>
              <span class="detail-value">{{ position()!.latitude | number:'1.4-4' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Lng</span>
              <span class="detail-value">{{ position()!.longitude | number:'1.4-4' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Speed</span>
              <span class="detail-value">{{ speedDisplay() }}</span>
            </div>
          </div>
        } @else {
          <div class="courier-offline">
            <ion-icon name="alert-circle-outline" />
            <div><h3>Courier not yet active</h3><p>Your delivery is scheduled. Check back soon.</p></div>
          </div>
        }
      </div>
    </ion-content>
  `,
  styles: [`
    ion-toolbar { --background: #0a0a14; --color: #fff; --border-color: #1a1a2e; }
    ion-title { font-weight: 700; }
    .tracking-content { --background: #0a0a14; }
    .map-area { height: 280px; background: linear-gradient(135deg, #0d0d1a 0%, #111128 100%); position: relative; overflow: hidden; border-bottom: 1px solid #1a1a2e; }
    .map-placeholder { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: #444; gap: 12px; }
    .map-placeholder ion-icon { font-size: 48px; }
    .map-placeholder p { font-size: 14px; margin: 0; }
    .map-mock { display: flex; align-items: center; justify-content: center; height: 100%; position: relative; }
    .map-mock::before { content: ''; position: absolute; inset: 0; background-image: linear-gradient(#1a1a2e 1px, transparent 1px), linear-gradient(90deg, #1a1a2e 1px, transparent 1px); background-size: 40px 40px; opacity: 0.5; }
    .courier-pin { font-size: 48px; position: relative; z-index: 2; }
    .courier-pin.moving { animation: bounce 0.6s ease infinite alternate; }
    @keyframes bounce { from { transform: translateY(0); } to { transform: translateY(-8px); } }
    .pulse-ring { position: absolute; width: 80px; height: 80px; border-radius: 50%; border: 2px solid #7c6aff; animation: pulse 2s ease-out infinite; z-index: 1; }
    @keyframes pulse { 0% { transform: scale(0.8); opacity: 1; } 100% { transform: scale(2.5); opacity: 0; } }
    .speed-badge { position: absolute; bottom: 16px; right: 16px; background: rgba(124,106,255,0.2); border: 1px solid #7c6aff44; border-radius: 20px; padding: 4px 12px; color: #7c6aff; font-size: 13px; font-weight: 600; }
    .status-card { background: #111128; margin: 16px; border-radius: 20px; padding: 24px; border: 1px solid #1a1a2e; }
    .status-loading { display: flex; align-items: center; gap: 16px; color: #888; --color: #7c6aff; }
    .courier-online { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; }
    .online-dot { width: 12px; height: 12px; background: #4cff91; border-radius: 50%; flex-shrink: 0; animation: blink 2s ease-in-out infinite; }
    @keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }
    .courier-info { flex: 1; }
    .courier-info h3 { color: #fff; font-size: 16px; font-weight: 700; margin: 0 0 2px; }
    .last-update { color: #666; font-size: 12px; margin: 0; }
    .heading-indicator { font-size: 20px; color: #7c6aff; transition: transform 0.5s ease; }
    .position-details { display: flex; background: #0a0a14; border-radius: 12px; overflow: hidden; }
    .detail-item { flex: 1; padding: 12px; text-align: center; border-right: 1px solid #1a1a2e; }
    .detail-item:last-child { border-right: none; }
    .detail-label { display: block; color: #555; font-size: 10px; letter-spacing: 1px; text-transform: uppercase; margin-bottom: 4px; }
    .detail-value { display: block; color: #7c6aff; font-size: 13px; font-weight: 600; font-family: monospace; }
    .courier-offline { display: flex; align-items: center; gap: 16px; }
    .courier-offline ion-icon { font-size: 32px; color: #444; flex-shrink: 0; }
    .courier-offline h3 { color: #ccc; font-size: 15px; font-weight: 600; margin: 0 0 4px; }
    .courier-offline p { color: #666; font-size: 13px; margin: 0; }
  `],
})
export class TrackingPage implements OnInit, OnDestroy {
  loading = signal(true);
  position = signal<CourierPositionResponse | null>(null);
  private secondsSince = signal(0);
  private pollSub?: Subscription;
  private tickSub?: Subscription;

  isMoving = computed(() => (this.position()?.speedKmh ?? 0) > 2);
  speedDisplay = computed(() => { const s = this.position()?.speedKmh; return s != null ? `${Math.round(s)} km/h` : '— km/h'; });
  secondsSinceUpdate = computed(() => this.secondsSince());

  constructor(private http: HttpClient, private authService: AuthService) {
    addIcons({ locateOutline, alertCircleOutline });
  }

  ngOnInit(): void {
    this.fetchPosition().subscribe((pos) => { this.position.set(pos); this.loading.set(false); });
    this.pollSub = interval(10_000).pipe(switchMap(() => this.fetchPosition())).subscribe((pos) => { this.position.set(pos); this.secondsSince.set(0); });
    this.tickSub = interval(1000).subscribe(() => this.secondsSince.update((n) => n + 1));
  }

  ngOnDestroy(): void { this.pollSub?.unsubscribe(); this.tickSub?.unsubscribe(); }

  private fetchPosition() {
    const userId = this.authService.session()?.userId;
    return this.http
      .get<ApiResponse<CourierPositionResponse>>(`${environment.apiUrl}/api/locations/couriers/${userId}`)
      .pipe(catchError(() => of({ success: false, data: null, error: null })), switchMap((r) => of(r.data)));
  }
}
