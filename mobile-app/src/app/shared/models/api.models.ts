// ── Auth ──────────────────────────────────────────────────────────────────────

export interface AuthTokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  tokenType: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  error: ApiError | null;
}

export interface ApiError {
  code: string;
  message: string;
  details: Record<string, string>;
}

// ── Route / stops ─────────────────────────────────────────────────────────────

export interface RouteResponse {
  shiftId: string;
  status: ShiftStatus;
  stops: StopResponse[];
  totalStops: number;
  completedStops: number;
  remainingStops: number;
}

export interface ShiftSummaryResponse {
  shiftId: string;
  courierId: string;
  courierName: string;
  status: ShiftStatus;
  scheduledDate: string;
  totalStops: number;
  completedStops: number;
  failedStops: number;
  progressPercent: number;
  startedAt: string | null;
  estimatedEndAt: string | null;
}

export interface StopResponse {
  id: string;
  sequenceNumber: number;
  status: StopStatus;
  customerName: string;
  deliveryAddress: AddressResponse;
  latitude: number;
  longitude: number;
  packageId: string;
  trackingNumber: string;
  packageFlags: PackageFlag[];
  deliveryInstructions: string | null;
  buzzerCode: string | null;
  estimatedArrivalAt: string | null;
  arrivedAt: string | null;
  completedAt: string | null;
  distanceMetres: number | null;
  estimatedMinutes: number | null;
}

export interface AddressResponse {
  street: string;
  houseNumber: string;
  apartment: string | null;
  floor: number | null;
  city: string;
  postalCode: string;
  country: string;
  formatted: string;
}

export interface DeliveryConfirmationResponse {
  stopId: string;
  packageId: string;
  trackingNumber: string;
  status: DeliveryStatus;
  placement: DeliveryPlacement | null;
  proofPhotoUploadUrl: string | null;
  signatureUploadUrl: string | null;
  confirmedAt: string;
}

export interface CourierPositionResponse {
  courierId: string;
  latitude: number;
  longitude: number;
  speedKmh: number | null;
  headingDegrees: number | null;
  updatedAt: string;
}

// ── Enums ─────────────────────────────────────────────────────────────────────

export type ShiftStatus = 'SCHEDULED' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
export type StopStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'SKIPPED';
export type DeliveryStatus = 'PENDING' | 'ASSIGNED' | 'IN_TRANSIT' | 'DELIVERED' | 'FAILED' | 'RESCHEDULED' | 'RETURNED';
export type DeliveryPlacement = 'HANDED_TO_CUSTOMER' | 'FRONT_DOOR' | 'RECEPTION' | 'NEIGHBOUR' | 'PARCEL_LOCKER' | 'OTHER';
export type FailureReason = 'NO_ANSWER' | 'ADDRESS_NOT_FOUND' | 'NO_ACCESS' | 'REFUSED_BY_CUSTOMER' | 'DAMAGED' | 'WRONG_ADDRESS' | 'OTHER';
export type PackageFlag = 'FRAGILE' | 'KEEP_UPRIGHT' | 'KEEP_REFRIGERATED' | 'HAZARDOUS' | 'REQUIRES_SIGNATURE' | 'CASH_ON_DELIVERY' | 'PROOF_OF_AGE_REQUIRED';
export type UserRole = 'COURIER' | 'CUSTOMER' | 'DISPATCHER';

// ── Local session ─────────────────────────────────────────────────────────────

export interface UserSession {
  userId: string;
  email: string;
  role: UserRole;
  accessToken: string;
  refreshToken: string;
}
