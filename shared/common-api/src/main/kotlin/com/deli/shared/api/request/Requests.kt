package com.deli.shared.api.request

import com.deli.shared.domain.model.DeliveryPlacement
import com.deli.shared.domain.model.DeliveryStatus
import com.deli.shared.domain.model.FailureReason
import com.deli.shared.domain.model.PackageFlag
import com.deli.shared.domain.model.VehicleType
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

// ── Auth requests ─────────────────────────────────────────────────────────────

data class LoginRequest(
    @field:Email(message = "Must be a valid email address")
    @field:NotBlank
    val email: String,
    @field:NotBlank
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val password: String,
)

data class RefreshTokenRequest(
    @field:NotBlank
    val refreshToken: String,
)

data class RegisterCourierRequest(
    @field:Email
    @field:NotBlank
    val email: String,
    @field:NotBlank
    @field:Size(min = 8)
    val password: String,
    @field:NotBlank
    @field:Size(max = 100)
    val firstName: String,
    @field:NotBlank
    @field:Size(max = 100)
    val lastName: String,
    @field:NotBlank
    @field:Pattern(regexp = "^\\+?[1-9]\\d{6,14}$", message = "Invalid phone number format")
    val phoneNumber: String,
    @field:NotNull
    val vehicleType: VehicleType,
    val licencePlate: String? = null,
)

data class RegisterCustomerRequest(
    @field:Email
    @field:NotBlank
    val email: String,
    @field:NotBlank
    @field:Size(min = 8)
    val password: String,
    @field:NotBlank
    val firstName: String,
    @field:NotBlank
    val lastName: String,
    @field:NotBlank
    @field:Pattern(regexp = "^\\+?[1-9]\\d{6,14}$")
    val phoneNumber: String,
)

data class UpdateFcmTokenRequest(
    @field:NotBlank
    val fcmToken: String,
)

// ── GPS location request ───────────────────────────────────────────────────────
// Sent by the mobile app over WebSocket every 5–15 seconds while shift is active.

data class LocationPingRequest(
    @field:NotBlank
    val shiftId: String,
    @field:DecimalMin("-90.0")
    @field:DecimalMax("90.0")
    val latitude: Double,
    @field:DecimalMin("-180.0")
    @field:DecimalMax("180.0")
    val longitude: Double,
    @field:Positive
    val accuracyMetres: Double,
    val speedKmh: Double? = null,
    val headingDegrees: Double? = null,
    /** ISO-8601 timestamp from the device — device clock may differ from server */
    val recordedAt: String,
)

// ── Delivery confirmation request ─────────────────────────────────────────────

data class ConfirmDeliveryRequest(
    @field:NotNull
    val placement: DeliveryPlacement,
    val courierNote: String? = null,
    /** Set to true once the courier has uploaded the photo via the pre-signed URL */
    val photoUploaded: Boolean = false,
    /** Set to true once the courier has uploaded the signature */
    val signatureUploaded: Boolean = false,
)

data class ReportFailedDeliveryRequest(
    @field:NotNull
    val reason: FailureReason,
    @field:Size(max = 500)
    val courierNote: String? = null,
)

data class RequestPhotoUploadUrlRequest(
    @field:NotBlank
    val stopId: String,
    /** MIME type — must be image/jpeg or image/png */
    @field:Pattern(regexp = "image/(jpeg|png)")
    val contentType: String,
)

// ── Package creation request ──────────────────────────────────────────────────

data class CreatePackageRequest(
    @field:NotBlank
    val customerId: String,
    @field:NotBlank
    @field:Size(max = 200)
    val description: String,
    @field:Positive
    val weightGrams: Int,
    @field:Positive
    val lengthCm: Int,
    @field:Positive
    val widthCm: Int,
    @field:Positive
    val heightCm: Int,
    val flags: Set<PackageFlag> = emptySet(),
    val deliveryAddress: AddressRequest,
    /** Required when flags contains CASH_ON_DELIVERY */
    val cashOnDeliveryAmountCents: Long? = null,
    val cashOnDeliveryCurrency: String? = null,
)

data class AddressRequest(
    @field:NotBlank val street: String,
    @field:NotBlank val houseNumber: String,
    val apartment: String? = null,
    val floor: Int? = null,
    @field:NotBlank val city: String,
    @field:NotBlank val postalCode: String,
    @field:NotBlank val country: String,
    val buzzerCode: String? = null,
    @field:Size(max = 500) val deliveryInstructions: String? = null,
)

data class CompleteStopRequest(
    val deliveryStatus: DeliveryStatus,
    val courierNote: String? = null,
)

// ── Shift management ──────────────────────────────────────────────────────────

data class StartShiftRequest(
    @field:NotBlank
    val scheduledDate: String, // ISO date: "2025-04-01"
)

data class AddStopRequest(
    val packageId: String,
    val customerId: String,
    val address: com.deli.shared.api.request.AddressRequest,
    val latitude: Double,
    val longitude: Double,
)
