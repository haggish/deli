import { Component, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import {
  IonContent,
  IonItem,
  IonLabel,
  IonInput,
  IonButton,
  IonSpinner,
  IonText,
  ToastController,
} from '@ionic/angular/standalone';
import { AuthService } from '../../shared/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    IonContent,
    IonItem,
    IonLabel,
    IonInput,
    IonButton,
    IonSpinner,
    IonText,
  ],
  template: `
    <ion-content class="login-content">
      <div class="login-container">

        <div class="brand">
          <div class="brand-mark">D</div>
          <h1>deli</h1>
          <p>Courier Platform</p>
        </div>

        <form [formGroup]="form" (ngSubmit)="onLogin()">

          <ion-item class="input-item">
            <ion-label position="stacked">Email</ion-label>
            <ion-input
              type="email"
              formControlName="email"
              placeholder="you@example.com"
              autocomplete="email"
            />
          </ion-item>

          <ion-item class="input-item">
            <ion-label position="stacked">Password</ion-label>
            <ion-input
              type="password"
              formControlName="password"
              placeholder="••••••••"
              autocomplete="current-password"
            />
          </ion-item>

          @if (error()) {
            <ion-text color="danger">
              <p class="error-text">{{ error() }}</p>
            </ion-text>
          }

          <ion-button
            expand="block"
            type="submit"
            class="login-btn"
            [disabled]="form.invalid || loading()"
          >
            @if (loading()) {
              <ion-spinner name="crescent" />
            } @else {
              Sign in
            }
          </ion-button>

        </form>

      </div>
    </ion-content>
  `,
  styles: [`
    .login-content {
      --background: #0f0f0f;
    }

    .login-container {
      display: flex;
      flex-direction: column;
      justify-content: center;
      min-height: 100vh;
      padding: 40px 32px;
      max-width: 420px;
      margin: 0 auto;
    }

    .brand {
      text-align: center;
      margin-bottom: 56px;
    }

    .brand-mark {
      width: 64px;
      height: 64px;
      background: #e8ff00;
      color: #0f0f0f;
      border-radius: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 32px;
      font-weight: 900;
      margin: 0 auto 16px;
      letter-spacing: -2px;
    }

    .brand h1 {
      color: #ffffff;
      font-size: 36px;
      font-weight: 800;
      letter-spacing: -1px;
      margin: 0 0 4px;
    }

    .brand p {
      color: #666;
      font-size: 14px;
      margin: 0;
      letter-spacing: 2px;
      text-transform: uppercase;
    }

    .input-item {
      --background: #1a1a1a;
      --color: #ffffff;
      --border-color: #333;
      --border-radius: 12px;
      --padding-start: 16px;
      border-radius: 12px;
      margin-bottom: 12px;
    }

    ion-label {
      --color: #888 !important;
      font-size: 12px !important;
      font-weight: 600;
      letter-spacing: 0.5px;
    }

    .login-btn {
      --background: #e8ff00;
      --color: #0f0f0f;
      --border-radius: 12px;
      --box-shadow: none;
      margin-top: 24px;
      height: 52px;
      font-weight: 700;
      font-size: 16px;
      letter-spacing: 0.5px;
    }

    .login-btn:hover {
      --background: #d4eb00;
    }

    .error-text {
      color: #ff4444;
      font-size: 13px;
      padding: 8px 4px 0;
      margin: 0;
    }
  `],
})
export class LoginPage {
  form: FormGroup;
  loading = signal(false);
  error = signal<string | null>(null);

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private toast: ToastController,
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
    });
  }

  onLogin(): void {
    if (this.form.invalid || this.loading()) return;

    this.loading.set(true);
    this.error.set(null);

    const { email, password } = this.form.value;

    this.authService.login(email, password).subscribe({
      next: (response) => {
        this.loading.set(false);
        if (response.success) {
          this.authService.navigateToHome();
        } else {
          this.error.set(response.error?.message ?? 'Login failed');
        }
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Invalid email or password');
      },
    });
  }
}
