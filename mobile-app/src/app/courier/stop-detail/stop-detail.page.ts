import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import {
  IonContent, IonHeader, IonToolbar, IonTitle,
  IonButton, IonIcon, IonSpinner, IonBackButton,
  IonButtons, IonChip, IonLabel, ToastController,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { navigateOutline, copyOutline, warningOutline, ribbonOutline, cashOutline } from 'ionicons/icons';
import { RouteApiService } from '../../shared/services/route-api.service';
import { StopResponse } from '../../shared/models/api.models';

@Component({
  selector: 'app-stop-detail',
  standalone: true,
  imports: [
    IonContent, IonHeader, IonToolbar, IonTitle,
    IonButton, IonIcon, IonSpinner, IonBackButton,
    IonButtons, IonChip, IonLabel,
  ],
  template: `
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button defaultHref="/courier/route" />
        </ion-buttons>
        <ion-title>Stop {{ stop()?.sequenceNumber }}</ion-title>
      </ion-toolbar>
    </ion-header>
    <ion-content class="stop-content">
      @if (loading()) {
        <div class="loading-center"><ion-spinner name="crescent" /></div>
      }
      @if (!loading() && stop()) {
        <div class="card address-card">
          <p class="card-label">Delivery address</p>
          <h2 class="address-big">{{ stop()!.deliveryAddress.street }} {{ stop()!.deliveryAddress.houseNumber }}</h2>
          @if (stop()!.deliveryAddress.apartment) {
            <p class="address-detail">Apt {{ stop()!.deliveryAddress.apartment }}</p>
          }
          @if (stop()!.deliveryAddress.floor) {
            <p class="address-detail">Floor {{ stop()!.deliveryAddress.floor }}</p>
          }
          <p class="address-city">{{ stop()!.deliveryAddress.postalCode }} {{ stop()!.deliveryAddress.city }}</p>
          @if (stop()!.buzzerCode) {
            <div class="buzzer-row" (click)="copyBuzzer(stop()!.buzzerCode!)">
              <span class="buzzer-label">Buzzer</span>
              <span class="buzzer-code">{{ stop()!.buzzerCode }}</span>
              <ion-icon name="copy-outline" class="copy-icon" />
            </div>
          }
          @if (stop()!.deliveryInstructions) {
            <div class="instructions">
              <p class="instructions-label">Instructions</p>
              <p class="instructions-text">{{ stop()!.deliveryInstructions }}</p>
            </div>
          }
        </div>
        @if (stop()!.packageFlags.length > 0) {
          <div class="card flags-card">
            <p class="card-label">Package notes</p>
            <div class="flags">
              @if (stop()!.packageFlags.includes('FRAGILE')) {
                <ion-chip class="flag-chip fragile"><ion-icon name="warning-outline" /><ion-label>Fragile</ion-label></ion-chip>
              }
              @if (stop()!.packageFlags.includes('REQUIRES_SIGNATURE')) {
                <ion-chip class="flag-chip signature"><ion-icon name="ribbon-outline" /><ion-label>Signature required</ion-label></ion-chip>
              }
              @if (stop()!.packageFlags.includes('CASH_ON_DELIVERY')) {
                <ion-chip class="flag-chip cod"><ion-icon name="cash-outline" /><ion-label>Cash on delivery</ion-label></ion-chip>
              }
            </div>
          </div>
        }
        <div class="actions">
          <ion-button expand="block" class="nav-btn" (click)="openMaps()">
            <ion-icon name="navigate-outline" slot="start" />Navigate
          </ion-button>
          @if (stop()!.status === 'PENDING') {
            <ion-button expand="block" class="start-btn" (click)="startStop(stop()!.id)" [disabled]="acting()">
              @if (acting()) { <ion-spinner name="crescent" /> } @else { I've arrived }
            </ion-button>
          }
          @if (stop()!.status === 'IN_PROGRESS') {
            <ion-button expand="block" class="confirm-btn" (click)="goToConfirm(stop()!.id)">Confirm delivery</ion-button>
          }
        </div>
      }
    </ion-content>
  `,
  styles: [`
    ion-toolbar { --background: #0f0f0f; --color: #fff; --border-color: #222; }
    .stop-content { --background: #0f0f0f; }
    .loading-center { display: flex; justify-content: center; padding: 80px; --color: #e8ff00; }
    .card { background: #1a1a1a; border-radius: 20px; padding: 24px; margin: 16px; }
    .card-label { color: #666; font-size: 11px; letter-spacing: 1.5px; text-transform: uppercase; margin: 0 0 12px; }
    .address-big { color: #fff; font-size: 24px; font-weight: 800; margin: 0 0 4px; }
    .address-detail, .address-city { color: #888; font-size: 15px; margin: 0 0 4px; }
    .buzzer-row { display: flex; align-items: center; background: #2a2a2a; border-radius: 10px; padding: 12px 16px; margin-top: 16px; cursor: pointer; }
    .buzzer-label { color: #888; font-size: 12px; text-transform: uppercase; letter-spacing: 1px; margin-right: 12px; }
    .buzzer-code { color: #e8ff00; font-size: 20px; font-weight: 800; font-family: monospace; flex: 1; }
    .copy-icon { color: #666; font-size: 18px; }
    .instructions { margin-top: 16px; padding-top: 16px; border-top: 1px solid #2a2a2a; }
    .instructions-label { color: #666; font-size: 11px; letter-spacing: 1px; text-transform: uppercase; margin: 0 0 6px; }
    .instructions-text { color: #ccc; font-size: 14px; line-height: 1.5; margin: 0; }
    .flags { display: flex; flex-wrap: wrap; gap: 8px; }
    .flag-chip { --background: #2a2a2a; --color: #fff; font-size: 13px; height: 32px; }
    .flag-chip.fragile { --background: #3a1f1a; --color: #ff6b35; }
    .flag-chip.signature { --background: #1a1f3a; --color: #6b8fff; }
    .flag-chip.cod { --background: #1f3a1a; --color: #6bff8f; }
    .actions { padding: 0 16px 32px; display: flex; flex-direction: column; gap: 12px; }
    .nav-btn { --background: #2a2a2a; --color: #fff; --border-radius: 12px; height: 52px; }
    .start-btn { --background: #fff; --color: #0f0f0f; --border-radius: 12px; height: 52px; font-weight: 700; }
    .confirm-btn { --background: #e8ff00; --color: #0f0f0f; --border-radius: 12px; height: 52px; font-weight: 700; }
  `],
})
export class StopDetailPage implements OnInit {
  loading = signal(true);
  acting = signal(false);
  stop = signal<StopResponse | null>(null);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private routeApi: RouteApiService,
    private toast: ToastController,
  ) {
    addIcons({ navigateOutline, copyOutline, warningOutline, ribbonOutline, cashOutline });
  }

  ngOnInit(): void {
    const stopId = this.route.snapshot.paramMap.get('stopId')!;
    this.routeApi.getStop(stopId).subscribe({
      next: (s) => { this.stop.set(s); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  startStop(stopId: string): void {
    this.acting.set(true);
    this.routeApi.startStop(stopId).subscribe({
      next: (s) => { this.stop.set(s); this.acting.set(false); },
      error: () => this.acting.set(false),
    });
  }

  goToConfirm(stopId: string): void { this.router.navigate(['/courier/stop', stopId, 'confirm']); }

  openMaps(): void {
    const s = this.stop();
    if (!s) return;
    window.open(`https://maps.google.com/?q=${encodeURIComponent(s.deliveryAddress.formatted)}&ll=${s.latitude},${s.longitude}`, '_blank');
  }

  async copyBuzzer(code: string): Promise<void> {
    navigator.clipboard?.writeText(code);
    const t = await this.toast.create({ message: 'Buzzer code copied', duration: 1500, position: 'bottom', color: 'dark' });
    await t.present();
  }
}
