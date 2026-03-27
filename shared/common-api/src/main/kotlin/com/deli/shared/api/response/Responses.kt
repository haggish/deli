package com.deli.shared.api.response

import com.deli.shared.domain.model.DeliveryPlacement
import com.deli.shared.domain.model.DeliveryStatus
import com.deli.shared.domain.model.FailureReason
import com.deli.shared.domain.model.PackageFlag
import com.deli.shared.domain.model.ShiftStatus
import com.deli.shared.domain.model.StopStatus
import com.deli.shared.domain.model.VehicleType
import java.time.Instant

// ── Generic API response envelope ─────────────────────────────────────────────

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null,
    val meta: ResponseMeta? = null,
) {
    companion object {
        fun <T> ok(
            data: T,
            meta: ResponseMeta? = null,
        ) = ApiResponse(success = true, data = data, meta = meta)

        fun <T> error(error: ApiError) = ApiResponse<T>(success = false, error = error)
    }
}

data class ApiError(
    val code: String,
    val message: String,
    val details: Map<String, String> = emptyMap(),
)

data class ResponseMeta(
    val page: Int? = null,
    val pageSize: Int? = null,
    val totalElements: Long? = null,
    val totalPages: Int? = null,
)

// ── Auth responses ─────────────────────────────────────────────────────────────

data class AuthTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long,
    val tokenType: String = "Bearer",
)

data class UserProfileResponse(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val phoneNumber: String,
)

// ── Stop / route responses ────────────────────────────────────────────────────

data class StopResponse(
    val id: String,
    val sequenceNumber: Int,
    val status: StopStatus,
    val customerName: String,
    val deliveryAddress: AddressResponse,
    val latitude: Double,
    val longitude: Double,
    val packageId: String,
    val trackingNumber: String,
    val packageFlags: Set<PackageFlag>,
    val deliveryInstructions: String?,
    val buzzerCode: String?,
    val estimatedArrivalAt: Instant?,
    val arrivedAt: Instant?,
    val completedAt: Instant?,
    val distanceMetres: Double?, // from previous stop, null for first
    val estimatedMinutes: Int?,
)

data class AddressResponse(
    val street: String,
    val houseNumber: String,
    val apartment: String?,
    val floor: Int?,
    val city: String,
    val postalCode: String,
    val country: String,
    val formatted: String,
)

data class ShiftSummaryResponse(
    val shiftId: String,
    val courierId: String,
    val courierName: String,
    val status: ShiftStatus,
    val scheduledDate: String,
    val totalStops: Int,
    val completedStops: Int,
    val failedStops: Int,
    val progressPercent: Int,
    val startedAt: Instant?,
    val estimatedEndAt: Instant?,
)

data class RouteResponse(
    val shiftId: String,
    val status: ShiftStatus,
    val stops: List<StopResponse>,
    val totalStops: Int,
    val completedStops: Int,
    val remainingStops: Int,
)

// ── Delivery confirmation response ────────────────────────────────────────────

data class DeliveryConfirmationResponse(
    val stopId: String,
    val packageId: String,
    val trackingNumber: String,
    val status: DeliveryStatus,
    val placement: DeliveryPlacement?,
    val proofPhotoUploadUrl: String?, // pre-signed S3 PUT URL for photo upload
    val signatureUploadUrl: String?, // pre-signed S3 PUT URL for signature upload
    val confirmedAt: Instant,
)

data class PhotoUploadUrlResponse(
    val uploadUrl: String, // pre-signed PUT URL — expires in 5 minutes
    val fileKey: String, // key to reference the file after upload
    val expiresAt: Instant,
)

// ── Package / tracking responses ──────────────────────────────────────────────

data class PackageTrackingResponse(
    val packageId: String,
    val trackingNumber: String,
    val status: DeliveryStatus,
    val description: String,
    val weightGrams: Int,
    val currentStop: StopSummaryResponse?,
    val deliveryHistory: List<DeliveryEventResponse>,
)

data class StopSummaryResponse(
    val stopId: String,
    val status: StopStatus,
    val estimatedArrivalAt: Instant?,
    val courierName: String?,
    val courierPosition: CourierPositionResponse?,
)

data class DeliveryEventResponse(
    val status: DeliveryStatus,
    val placement: DeliveryPlacement?,
    val failureReason: FailureReason?,
    val note: String?,
    val occurredAt: Instant,
)

// ── Courier / customer position responses ─────────────────────────────────────

data class CourierPositionResponse(
    val courierId: String,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Double?,
    val headingDegrees: Double?,
    val updatedAt: Instant,
)

data class CourierProfileResponse(
    val id: String,
    val firstName: String,
    val lastName: String,
    val vehicleType: VehicleType,
    val phoneNumber: String,
    val isActive: Boolean,
)
