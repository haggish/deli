import { Component } from '@angular/core';
import { IonTabs, IonTabBar, IonTabButton, IonIcon, IonLabel } from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { locateOutline, timeOutline } from 'ionicons/icons';

@Component({
  selector: 'app-customer-shell',
  standalone: true,
  imports: [IonTabs, IonTabBar, IonTabButton, IonIcon, IonLabel],
  template: `
    <ion-tabs>
      <ion-tab-bar slot="bottom">
        <ion-tab-button tab="tracking" href="/customer/tracking">
          <ion-icon name="locate-outline" />
          <ion-label>Track</ion-label>
        </ion-tab-button>
        <ion-tab-button tab="history" href="/customer/history">
          <ion-icon name="time-outline" />
          <ion-label>History</ion-label>
        </ion-tab-button>
      </ion-tab-bar>
    </ion-tabs>
  `,
  styles: [`
    ion-tab-bar {
      --background: #0a0a14;
      --border: 1px solid #1a1a2e;
      --color: #555;
      --color-selected: #7c6aff;
    }
  `],
})
export class CustomerShellComponent {
  constructor() {
    addIcons({ locateOutline, timeOutline });
  }
}
