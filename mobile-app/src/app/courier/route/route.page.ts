import { Component, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { SlicePipe } from "@angular/common";
import {
  IonContent,
  IonHeader,
  IonToolbar,
  IonTitle,
  IonList,
  IonItem,
  IonLabel,
  IonBadge,
  IonIcon,
  IonSpinner,
  IonRefresher,
  IonRefresherContent,
  RefresherCustomEvent,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import {
  checkmarkCircle,
  ellipseOutline,
  playCircleOutline,
  closeCircle,
  chevronForwardOutline,
} from 'ionicons/icons';
import { RouteApiService } from '../../shared/services/route-api.service';
import { StopResponse, StopStatus } from '../../shared/models/api.models';

@Component({
  selector: 'app-route',
  standalone: true,
  imports: [
    IonContent, IonHeader, IonToolbar, IonTitle,
    IonList, IonItem, IonLabel, IonBadge, IonIcon,
    IonSpinner, IonRefresher, IonRefresherContent,
    SlicePipe
  ],
  template: `
    <ion-header>
      <ion-toolbar>
        <ion-title>Today's Route</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content class="route-content">

      <ion-refresher slot="fixed" (ionRefresh)="refresh($event)">
        <ion-refresher-content />
      </ion-refresher>

      @if (loading()) {
        <div class="loading-center">
          <ion-spinner name="crescent" />
        </div>
      } @else if (stops().length === 0) {
        <div class="empty-state">
          <p>No stops assigned yet</p>
        </div>
      } @else {
        <ion-list class="stops-list">
          @for (stop of stops(); track stop.id) {
            <ion-item
              class="stop-item"
              [class]="'status-' + stop.status.toLowerCase()"
              (click)="openStop(stop)"
              [detail]="false"
            >
              <div class="sequence" slot="start">
                <span>{{ stop.sequenceNumber }}</span>
              </div>

              <ion-label>
                <h2 class="address-main">
                  {{ stop.deliveryAddress.street }} {{ stop.deliveryAddress.houseNumber }}
                </h2>
                <p class="address-sub">
                  {{ stop.deliveryAddress.postalCode }} {{ stop.deliveryAddress.city }}
                  @if (stop.deliveryAddress.apartment) {
                    · Apt {{ stop.deliveryAddress.apartment }}
                  }
                </p>
                @if (stop.estimatedArrivalAt) {
                  <p class="eta">ETA {{ stop.estimatedArrivalAt | slice:11:16 }}</p>
                }
                @if (stop.packageFlags.includes('FRAGILE')) {
                  <ion-badge class="flag-badge fragile">Fragile</ion-badge>
                }
                @if (stop.packageFlags.includes('REQUIRES_SIGNATURE')) {
                  <ion-badge class="flag-badge signature">Signature</ion-badge>
                }
              </ion-label>

              <div slot="end" class="stop-status-icon">
                @switch (stop.status) {
                  @case ('COMPLETED') {
                    <ion-icon name="checkmark-circle" class="icon-done" />
                  }
                  @case ('IN_PROGRESS') {
                    <ion-icon name="play-circle-outline" class="icon-active" />
                  }
                  @case ('SKIPPED') {
                    <ion-icon name="close-circle" class="icon-skipped" />
                  }
                  @default {
                    <ion-icon name="chevron-forward-outline" class="icon-pending" />
                  }
                }
              </div>
            </ion-item>
          }
        </ion-list>
      }

    </ion-content>
  `,
  styles: [`
    ion-toolbar { --background: #0f0f0f; --color: #fff; --border-color: #222; }
    ion-title { font-weight: 700; }

    .route-content { --background: #0f0f0f; }

    .loading-center {
      display: flex;
      justify-content: center;
      padding: 80px 0;
      --color: #e8ff00;
    }

    .empty-state {
      text-align: center;
      padding: 80px 24px;
      color: #666;
    }

    .stops-list {
      background: transparent;
      padding: 16px;
    }

    .stop-item {
      --background: #1a1a1a;
      --border-radius: 16px;
      --padding-start: 16px;
      --padding-end: 16px;
      --inner-padding-end: 0;
      --color: #fff;
      margin-bottom: 10px;
      border-radius: 16px;
    }

    .stop-item.status-completed {
      --background: #141f14;
      opacity: 0.7;
    }

    .stop-item.status-in_progress {
      --background: #1a1a0f;
      border: 1px solid #e8ff0044;
    }

    .sequence {
      width: 36px;
      height: 36px;
      background: #2a2a2a;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 12px;
      flex-shrink: 0;
    }

    .status-in_progress .sequence {
      background: #e8ff00;
    }

    .sequence span {
      color: #fff;
      font-size: 13px;
      font-weight: 700;
    }

    .status-in_progress .sequence span { color: #0f0f0f; }

    .address-main {
      color: #fff !important;
      font-size: 15px !important;
      font-weight: 600 !important;
      margin-bottom: 2px;
    }

    .address-sub {
      color: #888 !important;
      font-size: 13px !important;
      margin-bottom: 4px;
    }

    .eta {
      color: #e8ff00 !important;
      font-size: 12px !important;
      font-weight: 600;
    }

    .flag-badge {
      font-size: 10px;
      padding: 2px 6px;
      border-radius: 4px;
      margin-right: 4px;
    }

    .fragile { --background: #ff6b35; }
    .signature { --background: #6b8fff; }

    .stop-status-icon { padding: 0 4px; }

    .icon-done { color: #4caf50; font-size: 22px; }
    .icon-active { color: #e8ff00; font-size: 22px; }
    .icon-skipped { color: #666; font-size: 22px; }
    .icon-pending { color: #444; font-size: 18px; }
  `],
})
export class RoutePage implements OnInit {
  loading = signal(true);
  stops = signal<StopResponse[]>([]);

  constructor(
    private routeApi: RouteApiService,
    private router: Router,
  ) {
    addIcons({ checkmarkCircle, ellipseOutline, playCircleOutline, closeCircle, chevronForwardOutline });
  }

  ngOnInit(): void {
    this.loadStops();
  }

  loadStops(): void {
    this.loading.set(true);
    this.routeApi.getActiveRoute().subscribe({
      next: (route) => {
        this.stops.set(route?.stops ?? []);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  refresh(event: RefresherCustomEvent): void {
    this.routeApi.getActiveRoute().subscribe({
      next: (route) => {
        this.stops.set(route?.stops ?? []);
        event.detail.complete();
      },
      error: () => event.detail.complete(),
    });
  }

  openStop(stop: StopResponse): void {
    if (stop.status !== 'COMPLETED') {
      this.router.navigate(['/courier/stop', stop.id]);
    }
  }
}
