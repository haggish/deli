package com.deli.shared.domain.model

import com.deli.shared.domain.valueobject.Address
import com.deli.shared.domain.valueobject.Coordinates
import com.deli.shared.domain.valueobject.CourierId
import com.deli.shared.domain.valueobject.CustomerId
import com.deli.shared.domain.valueobject.Dimensions
import com.deli.shared.domain.valueobject.Money
import com.deli.shared.domain.valueobject.PackageId
import com.deli.shared.domain.valueobject.ShiftId
import com.deli.shared.domain.valueobject.StopId
import com.deli.shared.domain.valueobject.TrackingNumber
import com.deli.shared.domain.valueobject.Weight
import kotlinx.serialization.Serializable
import java.time.Instant

// ── Courier ───────────────────────────────────────────────────────────────────

@Serializable
data class Courier(
    val id: CourierId,
    val email: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val vehicleType: VehicleType,
    val licencePlate: String? = null,
    val isActive: Boolean = true,
    val createdAt:
        @Serializable(with = InstantSerializer::class)
        Instant = Instant.now(),
) {
    fun fullName(): String = "$firstName $lastName"
}

enum class VehicleType {
    BICYCLE,
    MOTORCYCLE,
    CAR,
    VAN,
    CARGO_BIKE,
}

// ── Customer ──────────────────────────────────────────────────────────────────

@Serializable
data class Customer(
    val id: CustomerId,
    val email: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val defaultAddress: Address? = null,
    val fcmToken: String? = null, // Firebase Cloud Messaging push token
    val isActive: Boolean = true,
    val createdAt:
        @Serializable(with = InstantSerializer::class)
        Instant = Instant.now(),
) {
    fun fullName(): String = "$firstName $lastName"
}

// ── Package ───────────────────────────────────────────────────────────────────

@Serializable
data class Package(
    val id: PackageId,
    val trackingNumber: TrackingNumber,
    val customerId: CustomerId,
    val description: String,
    val weight: Weight,
    val dimensions: Dimensions,
    val flags: Set<PackageFlag> = emptySet(),
    val cashOnDeliveryAmount: Money? = null, // Non-null only when COD flag is set
    val status: DeliveryStatus = DeliveryStatus.PENDING,
    val currentStopId: StopId? = null,
    val createdAt:
        @Serializable(with = InstantSerializer::class)
        Instant = Instant.now(),
    val updatedAt:
        @Serializable(with = InstantSerializer::class)
        Instant = Instant.now(),
) {
    fun isFragile(): Boolean = PackageFlag.FRAGILE in flags

    fun requiresSignature(): Boolean = PackageFlag.REQUIRES_SIGNATURE in flags

    fun isCashOnDelivery(): Boolean = PackageFlag.CASH_ON_DELIVERY in flags
}

// ── Shift ─────────────────────────────────────────────────────────────────────
// A shift represents one courier's working day — the container for all stops.

@Serializable
data class Shift(
    val id: ShiftId,
    val courierId: CourierId,
    val status: ShiftStatus = ShiftStatus.SCHEDULED,
    val scheduledDate: String, // ISO date string: "2025-04-01"
    val startedAt:
        @Serializable(with = InstantSerializer::class)
        Instant? = null,
    val completedAt:
        @Serializable(with = InstantSerializer::class)
        Instant? = null,
    val totalStops: Int = 0,
    val completedStops: Int = 0,
    val failedStops: Int = 0,
    val createdAt:
        @Serializable(with = InstantSerializer::class)
        Instant = Instant.now(),
) {
    fun progressPercent(): Int = if (totalStops == 0) 0 else (completedStops * 100) / totalStops

    fun remainingStops(): Int = totalStops - completedStops - failedStops
}

// ── Stop ──────────────────────────────────────────────────────────────────────
// One delivery attempt at one address within a shift.

@Serializable
data class Stop(
    val id: StopId,
    val shiftId: ShiftId,
    val packageId: PackageId,
    val customerId: CustomerId,
    val courierId: CourierId,
    val sequenceNumber: Int, // Ordered position in the day's route
    val deliveryAddress: Address,
    val coordinates: Coordinates,
    val status: StopStatus = StopStatus.PENDING,
    val estimatedArrivalAt:
        @Serializable(with = InstantSerializer::class)
        Instant? = null,
    val arrivedAt:
        @Serializable(with = InstantSerializer::class)
        Instant? = null,
    val completedAt:
        @Serializable(with = InstantSerializer::class)
        Instant? = null,
    val deliveryStatus: DeliveryStatus? = null,
    val placement: DeliveryPlacement? = null,
    val failureReason: FailureReason? = null,
    val courierNote: String? = null,
    val proofPhotoUrl: String? = null, // S3 pre-signed URL
    val signatureUrl: String? = null, // S3 pre-signed URL
    val createdAt:
        @Serializable(with = InstantSerializer::class)
        Instant = Instant.now(),
    val updatedAt:
        @Serializable(with = InstantSerializer::class)
        Instant = Instant.now(),
) {
    fun isCompleted(): Boolean = status == StopStatus.COMPLETED

    fun wasDelivered(): Boolean = deliveryStatus == DeliveryStatus.DELIVERED
}

// ── GPS ping ──────────────────────────────────────────────────────────────────
// Immutable record of a single location reading from a courier's device.

@Serializable
data class LocationPing(
    val courierId: CourierId,
    val shiftId: ShiftId,
    val coordinates: Coordinates,
    val accuracyMetres: Double,
    val speedKmh: Double? = null,
    val headingDegrees: Double? = null,
    val recordedAt:
        @Serializable(with = InstantSerializer::class)
        Instant = Instant.now(),
)

// ── Active courier position (Redis cache model) ───────────────────────────────
// Denormalised snapshot stored in Redis, updated on every GPS ping.
// TTL = 60 seconds — if no ping arrives, the courier is considered offline.

@Serializable
data class CourierPosition(
    val courierId: CourierId,
    val shiftId: ShiftId,
    val coordinates: Coordinates,
    val currentStopId: StopId? = null,
    val speedKmh: Double? = null,
    val headingDegrees: Double? = null,
    val updatedAt:
        @Serializable(with = InstantSerializer::class)
        Instant = Instant.now(),
)
