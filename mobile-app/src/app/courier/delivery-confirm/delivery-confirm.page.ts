import { Component, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import {
  IonContent, IonHeader, IonToolbar, IonTitle,
  IonButton, IonIcon, IonSpinner, IonBackButton,
  IonButtons, IonSegment, IonSegmentButton, IonLabel,
  IonTextarea, IonItem,
  ToastController,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { checkmarkOutline, closeOutline } from 'ionicons/icons';
import { DeliveryApiService } from '../../shared/services/route-api.service';
import { DeliveryPlacement, FailureReason } from '../../shared/models/api.models';

type Tab = 'delivered' | 'failed';

const PLACEMENTS: { value: DeliveryPlacement; label: string; emoji: string }[] = [
  { value: 'HANDED_TO_CUSTOMER', label: 'Handed to customer', emoji: '🤝' },
  { value: 'FRONT_DOOR', label: 'Front door', emoji: '🚪' },
  { value: 'RECEPTION', label: 'Reception', emoji: '🏢' },
  { value: 'NEIGHBOUR', label: 'Neighbour', emoji: '👤' },
  { value: 'PARCEL_LOCKER', label: 'Parcel locker', emoji: '📦' },
  { value: 'OTHER', label: 'Other', emoji: '📍' },
];

const FAILURE_REASONS: { value: FailureReason; label: string }[] = [
  { value: 'NO_ANSWER', label: 'No answer' },
  { value: 'NO_ACCESS', label: 'No access' },
  { value: 'ADDRESS_NOT_FOUND', label: 'Address not found' },
  { value: 'REFUSED_BY_CUSTOMER', label: 'Refused by customer' },
  { value: 'WRONG_ADDRESS', label: 'Wrong address' },
  { value: 'OTHER', label: 'Other' },
];

@Component({
  selector: 'app-delivery-confirm',
  standalone: true,
  imports: [
    FormsModule,
    IonContent, IonHeader, IonToolbar, IonTitle,
    IonButton, IonIcon, IonSpinner, IonBackButton,
    IonButtons, IonSegment, IonSegmentButton, IonLabel,
    IonTextarea, IonItem,
  ],
  template: `
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button [defaultHref]="'/courier/stop/' + stopId" />
        </ion-buttons>
        <ion-title>Confirm delivery</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content class="confirm-content">

      <!-- Delivered / Failed tabs -->
      <ion-segment [(ngModel)]="activeTab" class="outcome-segment">
        <ion-segment-button value="delivered">
          <ion-label>Delivered ✓</ion-label>
        </ion-segment-button>
        <ion-segment-button value="failed">
          <ion-label>Failed ✗</ion-label>
        </ion-segment-button>
      </ion-segment>

      @if (activeTab === 'delivered') {

        <div class="section">
          <p class="section-label">Where did you leave it?</p>
          <div class="placement-grid">
            @for (p of placements; track p.value) {
              <button
                class="placement-btn"
                [class.selected]="selectedPlacement() === p.value"
                (click)="selectedPlacement.set(p.value)"
              >
                <span class="placement-emoji">{{ p.emoji }}</span>
                <span class="placement-label">{{ p.label }}</span>
              </button>
            }
          </div>
        </div>

        <div class="section">
          <p class="section-label">Note (optional)</p>
          <ion-item class="note-item">
            <ion-textarea
              [(ngModel)]="courierNote"
              placeholder="e.g. Left next to the mat"
              rows="3"
              autocapitalize="sentences"
            />
          </ion-item>
        </div>

        <div class="submit-area">
          <ion-button
            expand="block"
            class="submit-btn delivered"
            (click)="confirm()"
            [disabled]="!selectedPlacement() || submitting()"
          >
            @if (submitting()) { <ion-spinner name="crescent" /> }
            @else {
              <ion-icon name="checkmark-outline" slot="start" />
              Confirm delivery
            }
          </ion-button>
        </div>

      } @else {

        <div class="section">
          <p class="section-label">Reason</p>
          <div class="reason-list">
            @for (r of failureReasons; track r.value) {
              <button
                class="reason-btn"
                [class.selected]="selectedReason() === r.value"
                (click)="selectedReason.set(r.value)"
              >
                {{ r.label }}
              </button>
            }
          </div>
        </div>

        <div class="section">
          <p class="section-label">Note (optional)</p>
          <ion-item class="note-item">
            <ion-textarea
              [(ngModel)]="courierNote"
              placeholder="e.g. Rang three times, no answer"
              rows="3"
              autocapitalize="sentences"
            />
          </ion-item>
        </div>

        <div class="submit-area">
          <ion-button
            expand="block"
            class="submit-btn failed"
            (click)="reportFailed()"
            [disabled]="!selectedReason() || submitting()"
          >
            @if (submitting()) { <ion-spinner name="crescent" /> }
            @else {
              <ion-icon name="close-outline" slot="start" />
              Report failure
            }
          </ion-button>
        </div>

      }

    </ion-content>
  `,
  styles: [`
    ion-toolbar { --background: #0f0f0f; --color: #fff; --border-color: #222; }
    .confirm-content { --background: #0f0f0f; }

    .outcome-segment {
      --background: #1a1a1a;
      margin: 16px;
      border-radius: 12px;
      padding: 4px;
    }

    ion-segment-button {
      --color: #666;
      --color-checked: #0f0f0f;
      --background-checked: #e8ff00;
      --border-radius: 10px;
      --indicator-height: 0;
      font-weight: 600;
      min-height: 40px;
    }

    .section {
      padding: 8px 16px;
      margin-bottom: 8px;
    }

    .section-label {
      color: #666;
      font-size: 11px;
      letter-spacing: 1.5px;
      text-transform: uppercase;
      margin: 0 0 12px 4px;
    }

    .placement-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 10px;
    }

    .placement-btn {
      background: #1a1a1a;
      border: 2px solid transparent;
      border-radius: 14px;
      padding: 16px 12px;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      transition: all 0.15s;
    }

    .placement-btn.selected {
      border-color: #e8ff00;
      background: #1a1f0f;
    }

    .placement-emoji { font-size: 28px; }

    .placement-label {
      color: #ccc;
      font-size: 12px;
      text-align: center;
      line-height: 1.3;
    }

    .placement-btn.selected .placement-label { color: #e8ff00; }

    .reason-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .reason-btn {
      background: #1a1a1a;
      border: 2px solid transparent;
      border-radius: 12px;
      padding: 14px 16px;
      color: #ccc;
      font-size: 15px;
      text-align: left;
      cursor: pointer;
      transition: all 0.15s;
    }

    .reason-btn.selected {
      border-color: #ff6b35;
      background: #1f1a17;
      color: #ff6b35;
    }

    .note-item {
      --background: #1a1a1a;
      --color: #fff;
      --border-radius: 12px;
      --padding-start: 16px;
      border-radius: 12px;
    }

    .submit-area {
      padding: 16px 16px 40px;
    }

    .submit-btn {
      --border-radius: 12px;
      height: 52px;
      font-weight: 700;
    }

    .submit-btn.delivered { --background: #e8ff00; --color: #0f0f0f; }
    .submit-btn.failed { --background: #ff6b35; --color: #fff; }
  `],
})
export class DeliveryConfirmPage {
  readonly placements = PLACEMENTS;
  readonly failureReasons = FAILURE_REASONS;

  activeTab: Tab = 'delivered';
  selectedPlacement = signal<DeliveryPlacement | null>(null);
  selectedReason = signal<FailureReason | null>(null);
  courierNote = '';
  submitting = signal(false);

  get stopId(): string {
    return this.route.snapshot.paramMap.get('stopId')!;
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private deliveryApi: DeliveryApiService,
    private toast: ToastController,
  ) {
    addIcons({ checkmarkOutline, closeOutline });
  }

  confirm(): void {
    const placement = this.selectedPlacement();
    if (!placement) return;

    this.submitting.set(true);
    this.deliveryApi
      .confirmDelivery(this.stopId, placement, this.courierNote || undefined)
      .subscribe({
        next: async () => {
          this.submitting.set(false);
          const t = await this.toast.create({
            message: 'Delivery confirmed ✓',
            duration: 2000,
            color: 'success',
            position: 'top',
          });
          await t.present();
          this.router.navigate(['/courier/route']);
        },
        error: () => this.submitting.set(false),
      });
  }

  reportFailed(): void {
    const reason = this.selectedReason();
    if (!reason) return;

    this.submitting.set(true);
    this.deliveryApi
      .reportFailure(this.stopId, reason, this.courierNote || undefined)
      .subscribe({
        next: async () => {
          this.submitting.set(false);
          const t = await this.toast.create({
            message: 'Failure reported',
            duration: 2000,
            color: 'warning',
            position: 'top',
          });
          await t.present();
          this.router.navigate(['/courier/route']);
        },
        error: () => this.submitting.set(false),
      });
  }
}
