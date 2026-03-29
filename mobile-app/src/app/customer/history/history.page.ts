import { Component } from '@angular/core';
import {
  IonContent, IonHeader, IonToolbar, IonTitle, IonIcon,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { timeOutline } from 'ionicons/icons';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [IonContent, IonHeader, IonToolbar, IonTitle, IonIcon],
  template: `
    <ion-header>
      <ion-toolbar>
        <ion-title>Delivery history</ion-title>
      </ion-toolbar>
    </ion-header>
    <ion-content class="history-content">
      <div class="empty-state">
        <ion-icon name="time-outline" />
        <h2>No deliveries yet</h2>
        <p>Your completed deliveries will appear here</p>
      </div>
    </ion-content>
  `,
  styles: [`
    ion-toolbar { --background: #0a0a14; --color: #fff; --border-color: #1a1a2e; }
    .history-content { --background: #0a0a14; }
    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 70vh;
      color: #444;
      gap: 12px;
      text-align: center;
      padding: 24px;
    }
    ion-icon { font-size: 56px; color: #222; }
    h2 { color: #666; font-size: 20px; font-weight: 700; margin: 0; }
    p { color: #444; font-size: 14px; margin: 0; }
  `],
})
export class HistoryPage {
  constructor() { addIcons({ timeOutline }); }
}
