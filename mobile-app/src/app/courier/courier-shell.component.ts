import { Component, OnInit } from '@angular/core';
import {
  IonTabs,
  IonTabBar,
  IonTabButton,
  IonIcon,
  IonLabel,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { homeOutline, mapOutline, personOutline } from 'ionicons/icons';

@Component({
  selector: 'app-courier-shell',
  standalone: true,
  imports: [IonTabs, IonTabBar, IonTabButton, IonIcon, IonLabel],
  template: `
    <ion-tabs>
      <ion-tab-bar slot="bottom">
        <ion-tab-button tab="dashboard" href="/courier/dashboard">
          <ion-icon name="home-outline" />
          <ion-label>Home</ion-label>
        </ion-tab-button>
        <ion-tab-button tab="route" href="/courier/route">
          <ion-icon name="map-outline" />
          <ion-label>Route</ion-label>
        </ion-tab-button>
      </ion-tab-bar>
    </ion-tabs>
  `,
  styles: [`
    ion-tab-bar {
      --background: #0f0f0f;
      --border: 1px solid #222;
      --color: #666;
      --color-selected: #e8ff00;
    }
  `],
})
export class CourierShellComponent {
  constructor() {
    addIcons({ homeOutline, mapOutline, personOutline });
  }
}
