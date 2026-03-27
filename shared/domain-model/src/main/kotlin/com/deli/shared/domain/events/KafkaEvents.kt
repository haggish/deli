package com.deli.shared.domain.events

import com.deli.shared.domain.model.DeliveryPlacement
import com.deli.shared.domain.model.DeliveryStatus
import com.deli.shared.domain.model.FailureReason
import com.deli.shared.domain.model.InstantSerializer
import com.deli.shared.domain.model.ShiftStatus
import com.deli.shared.domain.valueobject.Coordinates
import com.deli.shared.domain.valueobject.CourierId
import com.deli.shared.domain.valueobject.CustomerId
import com.deli.shared.domain.valueobject.PackageId
import com.deli.shared.domain.valueobject.ShiftId
import com.deli.shared.domain.valueobject.StopId
import com.deli.shared.domain.valueobject.TrackingNumber
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

// ── Event envelope ────────────────────────────────────────────────────────────
// Every Kafka message is wrapped in this envelope. The eventId allows consumers
// to implement idempotency — if the same eventId arrives twice (Kafka at-least-
// once delivery), the second delivery is ignored.

@Serializable
data class EventEnvelope<T>(
    val eventId: String = UUID.randomUUID().toString(),
    val eventType: String,
    val occurredAt:
        @Serializable(with = InstantSerializer::class)
        Instant = Instant.now(),
    val payload: T,
)

// ── Topic constants ───────────────────────────────────────────────────────────
// Single source of truth for topic names. Import this object in both producers
// and consumers to avoid string-literal drift.

object KafkaTopics {
    const val LOCATION_UPDATED = "location.updated"
    const val ROUTE_UPDATED = "route.updated"
    const val STOP_ASSIGNED = "stop.assigned"
    const val DELIVERY_CONFIRMED = "delivery.confirmed"
    const val DELIVERY_FAILED = "delivery.failed"
    const val SHIFT_STARTED = "shift.started"
    const val SHIFT_COMPLETED = "shift.completed"
}

// ── Event type constants ──────────────────────────────────────────────────────

object EventTypes {
    const val LOCATION_UPDATED = "LocationUpdated"
    const val ROUTE_UPDATED = "RouteUpdated"
    const val STOP_ASSIGNED = "StopAssigned"
    const val DELIVERY_CONFIRMED = "DeliveryConfirmed"
    const val DELIVERY_FAILED = "DeliveryFailed"
    const val SHIFT_STARTED = "ShiftStarted"
    const val SHIFT_COMPLETED = "ShiftCompleted"
}

// ── Location events (topic: location.updated) ─────────────────────────────────
// Published by: location-service
// Consumed by:  (TimescaleDB writer within location-service, dispatcher UI)
// Kafka key:    courierId — guarantees ordering per courier

@Serializable
data class LocationUpdatedEvent(
    val courierId: CourierId,
    val shiftId: ShiftId,
    val coordinates: Coordinates,
    val accuracyMetres: Double,
    val speedKmh: Double? = null,
    val headingDegrees: Double? = null,
    val recordedAt:
        @Serializable(with = InstantSerializer::class)
        Instant,
)

// ── Route events (topic: route.updated) ───────────────────────────────────────
// Published by: route-service
// Consumed by:  notification-service (notify courier of route change)
// Kafka key:    shiftId

@Serializable
data class RouteUpdatedEvent(
    val shiftId: ShiftId,
    val courierId: CourierId,
    val totalStops: Int,
    val updatedAt:
        @Serializable(with = InstantSerializer::class)
        Instant,
    val changeReason: RouteChangeReason,
)

enum class RouteChangeReason {
    INITIAL_ASSIGNMENT,
    STOP_ADDED,
    STOP_REMOVED,
    STOP_REORDERED,
    STOP_RESCHEDULED,
}

// ── Stop events (topic: stop.assigned) ────────────────────────────────────────
// Published by: route-service
// Consumed by:  delivery-service (register stop for confirmation tracking)
// Kafka key:    shiftId

@Serializable
data class StopAssignedEvent(
    val stopId: StopId,
    val shiftId: ShiftId,
    val courierId: CourierId,
    val packageId: PackageId,
    val customerId: CustomerId,
    val sequenceNumber: Int,
    val estimatedArrivalAt:
        @Serializable(with = InstantSerializer::class)
        Instant?,
)

// ── Delivery confirmed event (topic: delivery.confirmed) ─────────────────────
// Published by: delivery-service
// Consumed by:  notification-service (notify customer)
//               reporting-service (audit trail, SLA tracking)
// Kafka key:    packageId

@Serializable
data class DeliveryConfirmedEvent(
    val stopId: StopId,
    val packageId: PackageId,
    val trackingNumber: TrackingNumber,
    val courierId: CourierId,
    val customerId: CustomerId,
    val deliveredAt:
        @Serializable(with = InstantSerializer::class)
        Instant,
    val placement: DeliveryPlacement,
    val proofPhotoUrl: String?,
    val signatureUrl: String?,
    val courierNote: String?,
    val finalStatus: DeliveryStatus = DeliveryStatus.DELIVERED,
)

// ── Delivery failed event (topic: delivery.failed) ────────────────────────────
// Published by: delivery-service
// Consumed by:  notification-service (notify customer)
//               route-service (decide reschedule / return)
// Kafka key:    packageId

@Serializable
data class DeliveryFailedEvent(
    val stopId: StopId,
    val packageId: PackageId,
    val trackingNumber: TrackingNumber,
    val courierId: CourierId,
    val customerId: CustomerId,
    val failedAt:
        @Serializable(with = InstantSerializer::class)
        Instant,
    val reason: FailureReason,
    val courierNote: String?,
    val attemptNumber: Int = 1, // 1st, 2nd, or final attempt
    val willReschedule: Boolean = true,
)

// ── Shift events (topic: shift.started / shift.completed) ────────────────────
// Published by: route-service
// Consumed by:  notification-service, dispatcher UI

@Serializable
data class ShiftStartedEvent(
    val shiftId: ShiftId,
    val courierId: CourierId,
    val scheduledDate: String,
    val totalStops: Int,
    val startedAt:
        @Serializable(with = InstantSerializer::class)
        Instant,
)

@Serializable
data class ShiftCompletedEvent(
    val shiftId: ShiftId,
    val courierId: CourierId,
    val status: ShiftStatus,
    val completedStops: Int,
    val failedStops: Int,
    val totalStops: Int,
    val completedAt:
        @Serializable(with = InstantSerializer::class)
        Instant,
)
