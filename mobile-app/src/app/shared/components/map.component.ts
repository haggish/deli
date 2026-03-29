import {
  Component,
  Input,
  OnInit,
  OnDestroy,
  OnChanges,
  SimpleChanges,
  ElementRef,
  ViewChild,
  AfterViewInit,
  NgZone,
} from '@angular/core';

export interface MapMarker {
  lat: number;
  lng: number;
  label?: string;
  color?: 'yellow' | 'blue' | 'red' | 'green';
  popup?: string;
}

/**
 * Reusable Leaflet map component.
 *
 * Uses OpenStreetMap tiles — no API key required.
 * Dynamically imports Leaflet so it only loads when a map is actually shown
 * (Leaflet manipulates the DOM and cannot be imported at module level in SSR).
 *
 * Usage:
 *   <app-map [markers]="markers" [center]="center" [zoom]="15" />
 */
@Component({
  selector: 'app-map',
  standalone: true,
  template: `<div #mapContainer class="map-container"></div>`,
  styles: [`
    :host { display: block; width: 100%; }

    .map-container {
      width: 100%;
      height: 100%;
      min-height: 200px;
      background: #1a1a2e;
      border-radius: inherit;
    }

    /* Override Leaflet default styles to match dark theme */
    :host ::ng-deep .leaflet-container {
      background: #1a1a2e;
      font-family: inherit;
      border-radius: inherit;
    }

    :host ::ng-deep .leaflet-tile-pane {
      filter: brightness(0.85) saturate(0.9) hue-rotate(180deg) invert(1);
    }

    :host ::ng-deep .leaflet-control-attribution {
      background: rgba(0,0,0,0.5) !important;
      color: #666 !important;
      font-size: 9px;
    }

    :host ::ng-deep .leaflet-control-attribution a {
      color: #888 !important;
    }

    :host ::ng-deep .leaflet-popup-content-wrapper {
      background: #1a1a1a;
      color: #fff;
      border-radius: 10px;
      box-shadow: 0 4px 20px rgba(0,0,0,0.5);
    }

    :host ::ng-deep .leaflet-popup-tip {
      background: #1a1a1a;
    }
  `],
})
export class MapComponent implements AfterViewInit, OnChanges, OnDestroy {
  @ViewChild('mapContainer', { static: true }) mapContainer!: ElementRef;

  @Input() markers: MapMarker[] = [];
  @Input() center: { lat: number; lng: number } | null = null;
  @Input() zoom = 15;
  @Input() height = '220px';

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private map: any = null;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private L: any = null;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private markerInstances: any[] = [];
  private initialized = false;

  constructor(private ngZone: NgZone) {}

  async ngAfterViewInit(): Promise<void> {
    this.mapContainer.nativeElement.style.height = this.height;
    await this.initMap();
  }

  async ngOnChanges(changes: SimpleChanges): Promise<void> {
    if (!this.initialized) return;
    if (changes['markers'] || changes['center']) {
      this.updateMarkers();
    }
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
      this.map = null;
    }
  }

  private async initMap(): Promise<void> {
    // Dynamic import — avoids SSR issues and reduces initial bundle
    this.L = await import('leaflet');

    // Fix Leaflet's default icon paths (broken by bundlers)
    delete (this.L.Icon.Default.prototype as any)._getIconUrl;
    this.L.Icon.Default.mergeOptions({
      iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
      iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
      shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
    });

    const initialCenter = this.center ?? this.markers[0] ?? { lat: 52.52, lng: 13.405 };

    // Run Leaflet outside Angular zone — it does extensive DOM manipulation
    // and we do not want change detection triggered on every map interaction
    this.ngZone.runOutsideAngular(() => {
      this.map = this.L.map(this.mapContainer.nativeElement, {
        center: [initialCenter.lat, initialCenter.lng],
        zoom: this.zoom,
        zoomControl: true,
        attributionControl: true,
      });

      this.L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap',
        maxZoom: 19,
      }).addTo(this.map);
    });

    this.initialized = true;
    this.updateMarkers();

    // Leaflet needs a size invalidation after the container becomes visible
    setTimeout(() => {
      this.ngZone.runOutsideAngular(() => this.map?.invalidateSize());
    }, 100);
  }

  private updateMarkers(): void {
    if (!this.map || !this.L) return;

    this.ngZone.runOutsideAngular(() => {
      // Remove old markers
      this.markerInstances.forEach((m) => m.remove());
      this.markerInstances = [];

      // Add new markers
      this.markers.forEach((marker, index) => {
        const icon = this.createIcon(marker.color ?? 'yellow', marker.label ?? String(index + 1));
        const instance = this.L.marker([marker.lat, marker.lng], { icon });

        if (marker.popup) {
          instance.bindPopup(marker.popup);
        }

        instance.addTo(this.map);
        this.markerInstances.push(instance);
      });

      // Fit map to show all markers, or center on provided center
      if (this.markers.length > 1) {
        const bounds = this.L.latLngBounds(
          this.markers.map((m) => [m.lat, m.lng]),
        );
        this.map.fitBounds(bounds, { padding: [40, 40] });
      } else if (this.markers.length === 1) {
        const target = this.center ?? { lat: this.markers[0].lat, lng: this.markers[0].lng };
        this.map.setView([target.lat, target.lng], this.zoom);
      } else if (this.center) {
        this.map.setView([this.center.lat, this.center.lng], this.zoom);
      }
    });
  }

  private createIcon(color: string, label: string) {
    const colors: Record<string, { bg: string; text: string }> = {
      yellow: { bg: '#e8ff00', text: '#0f0f0f' },
      blue:   { bg: '#7c6aff', text: '#ffffff' },
      red:    { bg: '#ff4444', text: '#ffffff' },
      green:  { bg: '#4cff91', text: '#0f0f0f' },
    };
    const c = colors[color] ?? colors['yellow'];

    const svg = `
      <svg xmlns="http://www.w3.org/2000/svg" width="32" height="42" viewBox="0 0 32 42">
        <path d="M16 0C7.16 0 0 7.16 0 16c0 12 16 26 16 26S32 28 32 16C32 7.16 24.84 0 16 0z"
              fill="${c.bg}" stroke="rgba(0,0,0,0.3)" stroke-width="1.5"/>
        <text x="16" y="20" text-anchor="middle" font-family="system-ui,sans-serif"
              font-size="11" font-weight="700" fill="${c.text}">${label}</text>
      </svg>`.trim();

    return this.L.divIcon({
      html: svg,
      className: '',
      iconSize: [32, 42],
      iconAnchor: [16, 42],
      popupAnchor: [0, -42],
    });
  }

  /** Public method — call to update the courier position marker in place */
  updateCourierPosition(lat: number, lng: number): void {
    if (!this.map || !this.L) return;
    // Find the courier marker (always added last) and move it
    const courierMarker = this.markerInstances[this.markerInstances.length - 1];
    if (courierMarker) {
      this.ngZone.runOutsideAngular(() => courierMarker.setLatLng([lat, lng]));
    }
  }
}
