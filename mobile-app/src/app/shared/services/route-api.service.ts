import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
  ApiResponse,
  RouteResponse,
  ShiftSummaryResponse,
  StopResponse,
  DeliveryConfirmationResponse,
  DeliveryPlacement,
  FailureReason,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class RouteApiService {
  private readonly base = `${environment.apiUrl}/api/routes`;

  constructor(private http: HttpClient) {}

  getActiveRoute(): Observable<RouteResponse | null> {
    return this.http
      .get<ApiResponse<RouteResponse | null>>(`${this.base}/active`)
      .pipe(map((r) => r.data ?? null));
  }

  startShift(scheduledDate: string): Observable<ShiftSummaryResponse> {
    return this.http
      .post<ApiResponse<ShiftSummaryResponse>>(`${this.base}/shifts`, { scheduledDate })
      .pipe(map((r) => r.data!));
  }

  completeShift(shiftId: string): Observable<ShiftSummaryResponse> {
    return this.http
      .patch<ApiResponse<ShiftSummaryResponse>>(
        `${this.base}/shifts/${shiftId}/complete`,
        {},
      )
      .pipe(map((r) => r.data!));
  }

  startStop(stopId: string): Observable<StopResponse> {
    return this.http
      .patch<ApiResponse<StopResponse>>(`${this.base}/stops/${stopId}/start`, {})
      .pipe(map((r) => r.data!));
  }

  getStop(stopId: string): Observable<StopResponse> {
    return this.http
      .get<ApiResponse<StopResponse>>(`${this.base}/stops/${stopId}`)
      .pipe(map((r) => r.data!));
  }
}

@Injectable({ providedIn: 'root' })
export class DeliveryApiService {
  private readonly base = `${environment.apiUrl}/api/deliveries`;

  constructor(private http: HttpClient) {}

  confirmDelivery(
    stopId: string,
    placement: DeliveryPlacement,
    courierNote?: string,
  ): Observable<DeliveryConfirmationResponse> {
    return this.http
      .post<ApiResponse<DeliveryConfirmationResponse>>(
        `${this.base}/stops/${stopId}/confirm`,
        { placement, courierNote },
      )
      .pipe(map((r) => r.data!));
  }

  reportFailure(
    stopId: string,
    reason: FailureReason,
    courierNote?: string,
  ): Observable<DeliveryConfirmationResponse> {
    return this.http
      .post<ApiResponse<DeliveryConfirmationResponse>>(
        `${this.base}/stops/${stopId}/fail`,
        { reason, courierNote },
      )
      .pipe(map((r) => r.data!));
  }

  getPhotoUploadUrl(
    stopId: string,
    contentType: string,
  ): Observable<{ uploadUrl: string; fileKey: string }> {
    return this.http
      .post<ApiResponse<{ uploadUrl: string; fileKey: string }>>(
        `${this.base}/upload-url/photo`,
        { stopId, contentType },
      )
      .pipe(map((r) => r.data!));
  }
}
