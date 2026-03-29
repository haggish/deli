import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, AuthTokenResponse, UserSession, UserRole } from '../models/api.models';

const SESSION_KEY = 'deli_session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly _session = signal<UserSession | null>(this.loadSession());

  readonly session = this._session.asReadonly();
  readonly isLoggedIn = computed(() => this._session() !== null);
  readonly role = computed(() => this._session()?.role ?? null);
  readonly isCourier = computed(() => this._session()?.role === 'COURIER');
  readonly isCustomer = computed(() => this._session()?.role === 'CUSTOMER');

  constructor(
    private http: HttpClient,
    private router: Router,
  ) {}

  // ── Login ─────────────────────────────────────────────────────────────────

  login(email: string, password: string): Observable<ApiResponse<AuthTokenResponse>> {
    return this.http
      .post<ApiResponse<AuthTokenResponse>>(
        `${environment.apiUrl}/api/auth/login`,
        { email, password },
      )
      .pipe(
        tap((response) => {
          if (response.success && response.data) {
            const session = this.parseSession(response.data);
            this.saveSession(session);
          }
        }),
      );
  }

  // ── Logout ────────────────────────────────────────────────────────────────

  async logout(): Promise<void> {
    const session = this._session();
    if (session) {
      // Best-effort refresh token revocation
      this.http
        .post(`${environment.apiUrl}/api/auth/logout`, {
          refreshToken: session.refreshToken,
        })
        .subscribe({ error: () => {} });
    }
    this.clearSession();
    await this.router.navigate(['/login']);
  }

  // ── Token refresh ─────────────────────────────────────────────────────────

  refreshToken(): Observable<ApiResponse<AuthTokenResponse>> {
    const session = this._session();
    if (!session) return throwError(() => new Error('No session'));

    return this.http
      .post<ApiResponse<AuthTokenResponse>>(
        `${environment.apiUrl}/api/auth/refresh`,
        { refreshToken: session.refreshToken },
      )
      .pipe(
        tap((response) => {
          if (response.success && response.data) {
            const newSession = this.parseSession(response.data);
            this.saveSession(newSession);
          }
        }),
        catchError((err) => {
          this.clearSession();
          this.router.navigate(['/login']);
          return throwError(() => err);
        }),
      );
  }

  getAccessToken(): string | null {
    return this._session()?.accessToken ?? null;
  }

  // ── Navigation after login ────────────────────────────────────────────────

  navigateToHome(): Promise<boolean> {
    const role = this._session()?.role;
    if (role === 'COURIER') return this.router.navigate(['/courier/dashboard']);
    if (role === 'CUSTOMER') return this.router.navigate(['/customer/tracking']);
    return this.router.navigate(['/login']);
  }

  // ── Session persistence ───────────────────────────────────────────────────

  private parseSession(tokens: AuthTokenResponse): UserSession {
    // Decode JWT payload (no verification needed — gateway already validated)
    const payload = JSON.parse(atob(tokens.accessToken.split('.')[1]));
    return {
      userId: payload.sub,
      email: payload.email,
      role: payload.role as UserRole,
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
    };
  }

  private saveSession(session: UserSession): void {
    localStorage.setItem(SESSION_KEY, JSON.stringify(session));
    this._session.set(session);
  }

  private clearSession(): void {
    localStorage.removeItem(SESSION_KEY);
    this._session.set(null);
  }

  private loadSession(): UserSession | null {
    try {
      const raw = localStorage.getItem(SESSION_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }
}
