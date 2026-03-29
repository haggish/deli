import { Component, OnDestroy, signal, computed } from '@angular/core';
import { DatePipe } from '@angular/common';
import {
  IonContent,
  IonHeader,
  IonToolbar,
  IonTitle,
  IonButton,
  IonRefresher,
  IonRefresherContent,
  RefresherCustomEvent,
  IonSpinner,
  IonIcon,
  AlertController,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { wifiOutline, navigateOutline, checkmarkCircleOutline } from 'ionicons/icons';
import { RouteApiService } from '../../shared/services/route-api.service';
import { GpsService } from '../../shared/services/gps.service';
import { AuthService } from '../../shared/services/auth.service';
import { RouteResponse } from '../../shared/models/api.models';
import { Router } from '@angular/router';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    DatePipe,
    IonContent,
    IonHeader,
    IonToolbar,
    IonTitle,
    IonButton,
    IonSpinner,
    IonIcon,
    IonRefresher,
    IonRefresherContent,
  ],
  template: `
    <ion-header>
      <ion-toolbar>
        <ion-title>Dashboard</ion-title>
        <div slot="end" class="gps-indicator" [class.active]="gpsState().isStreaming">
          <ion-icon [name]='"wifi-outline"' />
        </div>
      </ion-toolbar>
    </ion-header>

    <ion-content class="dashboard-content">
      <ion-refresher slot="fixed" (ionRefresh)="onRefresh($event)">
        <ion-refresher-content />
      </ion-refresher>

      <div class="greeting">
        <p class="date">{{ today | date:'EEEE, d MMMM' }}</p>
        <h1>Good {{ timeOfDay }},<br>{{ firstName() }}</h1>
      </div>

      @if (loading()) {
        <div class="loading-center">
          <ion-spinner name="crescent" />
        </div>
      } @else if (!activeRoute()) {
        <!-- No active shift -->
        <div class="no-shift-card">
          <div class="no-shift-icon">📦</div>
          <h2>No active shift</h2>
          <p>Start your shift to begin delivering</p>
          <ion-button
            expand="block"
            class="start-btn"
            (click)="startShift()"
            [disabled]="startingShift()"
          >
            @if (startingShift()) {
              <ion-spinner name="crescent" />
            } @else {
              Start shift
            }
          </ion-button>
        </div>
      } @else {
        <!-- Active shift progress -->
        <div class="shift-card">
          <div class="progress-ring-container">
            <svg viewBox="0 0 120 120" class="progress-ring">
              <circle cx="60" cy="60" r="50" class="ring-track"/>
              <circle
                cx="60" cy="60" r="50"
                class="ring-progress"
                [style.stroke-dashoffset]="progressOffset()"
              />
            </svg>
            <div class="progress-center">
              <span class="progress-num">{{ activeRoute()!.completedStops }}</span>
              <span class="progress-denom">/ {{ activeRoute()!.totalStops }}</span>
            </div>
          </div>

          <div class="shift-stats">
            <div class="stat">
              <span class="stat-value">{{ activeRoute()!.remainingStops }}</span>
              <span class="stat-label">remaining</span>
            </div>
            <div class="stat">
              <span class="stat-value">{{ activeRoute()!.completedStops }}</span>
              <span class="stat-label">delivered</span>
            </div>
          </div>

          <ion-button
            expand="block"
            class="route-btn"
            (click)="goToRoute()"
          >
            <ion-icon name="navigate-outline" slot="start" />
            View route
          </ion-button>

          @if (activeRoute()!.remainingStops === 0) {
            <ion-button
              expand="block"
              fill="outline"
              class="complete-btn"
              (click)="completeShift()"
            >
              <ion-icon name="checkmark-circle-outline" slot="start" />
              Complete shift
            </ion-button>
          }
        </div>
      }

    </ion-content>
  `,
  styles: [`
    ion-toolbar {
      --background: #0f0f0f;
      --color: #ffffff;
      --border-color: #222;
    }

    ion-title { font-weight: 700; }

    .gps-indicator {
      padding: 0 16px;
      color: #444;
      font-size: 20px;
    }
    .gps-indicator.active { color: #e8ff00; }

    .dashboard-content { --background: #0f0f0f; }

    .greeting {
      padding: 32px 24px 16px;
    }

    .date {
      color: #666;
      font-size: 13px;
      letter-spacing: 1px;
      text-transform: uppercase;
      margin: 0 0 8px;
    }

    h1 {
      color: #fff;
      font-size: 28px;
      font-weight: 800;
      line-height: 1.2;
      margin: 0;
    }

    .loading-center {
      display: flex;
      justify-content: center;
      padding: 80px 0;
      --color: #e8ff00;
    }

    .no-shift-card {
      margin: 24px;
      background: #1a1a1a;
      border-radius: 20px;
      padding: 48px 32px;
      text-align: center;
    }

    .no-shift-icon {
      font-size: 48px;
      margin-bottom: 16px;
    }

    .no-shift-card h2 {
      color: #fff;
      font-size: 22px;
      font-weight: 700;
      margin: 0 0 8px;
    }

    .no-shift-card p {
      color: #666;
      font-size: 14px;
      margin: 0 0 32px;
    }

    .start-btn {
      --background: #e8ff00;
      --color: #0f0f0f;
      --border-radius: 12px;
      font-weight: 700;
      height: 52px;
    }

    .shift-card {
      margin: 24px;
      background: #1a1a1a;
      border-radius: 20px;
      padding: 32px 24px;
    }

    .progress-ring-container {
      position: relative;
      width: 160px;
      height: 160px;
      margin: 0 auto 24px;
    }

    .progress-ring {
      transform: rotate(-90deg);
      width: 100%;
      height: 100%;
    }

    .ring-track {
      fill: none;
      stroke: #2a2a2a;
      stroke-width: 10;
    }

    .ring-progress {
      fill: none;
      stroke: #e8ff00;
      stroke-width: 10;
      stroke-linecap: round;
      stroke-dasharray: 314;
      transition: stroke-dashoffset 0.5s ease;
    }

    .progress-center {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      text-align: center;
    }

    .progress-num {
      color: #fff;
      font-size: 36px;
      font-weight: 800;
      display: block;
    }

    .progress-denom {
      color: #666;
      font-size: 14px;
    }

    .shift-stats {
      display: flex;
      justify-content: space-around;
      margin-bottom: 24px;
    }

    .stat {
      text-align: center;
    }

    .stat-value {
      color: #fff;
      font-size: 24px;
      font-weight: 700;
      display: block;
    }

    .stat-label {
      color: #666;
      font-size: 12px;
      text-transform: uppercase;
      letter-spacing: 1px;
    }

    .route-btn {
      --background: #e8ff00;
      --color: #0f0f0f;
      --border-radius: 12px;
      font-weight: 700;
      height: 52px;
      margin-bottom: 12px;
    }

    .complete-btn {
      --border-color: #333;
      --color: #666;
      --border-radius: 12px;
      height: 48px;
    }
  `],
})
export class DashboardPage implements OnDestroy {
  today = new Date();
  loading = signal(true);
  startingShift = signal(false);
  activeRoute = signal<RouteResponse | null>(null);

  firstName = computed(() => {
    const session = this.authService.session();
    return session?.email.split('@')[0] ?? 'Courier';
  });

  gpsState = this.gpsService.state;

  progressOffset = computed(() => {
    const route = this.activeRoute();
    if (!route || route.totalStops === 0) return 314;
    const pct = route.completedStops / route.totalStops;
    return 314 * (1 - pct);
  });

  get timeOfDay(): string {
    const h = new Date().getHours();
    if (h < 12) return 'morning';
    if (h < 17) return 'afternoon';
    return 'evening';
  }

  constructor(
    private routeApi: RouteApiService,
    private gpsService: GpsService,
    private authService: AuthService,
    private router: Router,
    private alert: AlertController,
  ) {
    addIcons({ wifiOutline, navigateOutline, checkmarkCircleOutline });
  }

  ionViewWillEnter(): void {
    this.loadRoute();
  }

  ngOnDestroy(): void {}

  onRefresh(event: RefresherCustomEvent): void {
    this.routeApi.getActiveRoute().subscribe({
      next: (route) => { this.activeRoute.set(route); event.detail.complete(); },
      error: () => event.detail.complete(),
    });
  }

  loadRoute(): void {
    this.loading.set(true);
    this.routeApi.getActiveRoute().subscribe({
      next: (route) => {
        this.activeRoute.set(route);
        this.loading.set(false);
        // Auto-start GPS if shift is active
        if (route) this.startGps(route.shiftId);
      },
      error: () => this.loading.set(false),
    });
  }

  startShift(): void {
    this.startingShift.set(true);
    const today = new Date().toISOString().split('T')[0];
    this.routeApi.startShift(today).subscribe({
      next: () => {
        this.startingShift.set(false);
        this.loadRoute();
      },
      error: () => this.startingShift.set(false),
    });
  }

  async completeShift(): Promise<void> {
    const route = this.activeRoute();
    if (!route) return;

    const alertEl = await this.alert.create({
      header: 'Complete shift?',
      message: 'This will end your shift for today.',
      buttons: [
        { text: 'Cancel', role: 'cancel' },
        {
          text: 'Complete',
          handler: () => {
            this.routeApi.completeShift(route.shiftId).subscribe({
              next: () => {
                this.gpsService.stopStreaming();
                this.activeRoute.set(null);
              },
            });
          },
        },
      ],
    });
    await alertEl.present();
  }

  goToRoute(): void {
    this.router.navigate(['/courier/route']);
  }

  private startGps(shiftId: string): void {
    if (!this.gpsService.state().isStreaming) {
      this.gpsService.startStreaming(shiftId).catch(console.error);
    }
  }
}
