package com.deli.route.domain

import com.deli.shared.domain.model.DeliveryStatus
import com.deli.shared.domain.model.FailureReason
import com.deli.shared.domain.model.ShiftStatus
import com.deli.shared.domain.model.StopStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

// ── Shift ─────────────────────────────────────────────────────────────────────

@Entity
@Table(name = "shifts", schema = "route")
class Shift(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "courier_id", nullable = false)
    val courierId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ShiftStatus = ShiftStatus.SCHEDULED,
    @Column(name = "scheduled_date", nullable = false)
    val scheduledDate: String, // ISO date: "2025-04-01"
    @Column(name = "started_at")
    var startedAt: Instant? = null,
    @Column(name = "completed_at")
    var completedAt: Instant? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @OneToMany(mappedBy = "shift", fetch = FetchType.LAZY)
    @OrderBy("sequence_number ASC")
    val stops: MutableList<Stop> = mutableListOf(),
) {
    val totalStops: Int get() = stops.size
    val completedStops: Int get() = stops.count { it.status == StopStatus.COMPLETED }
    val failedStops: Int get() =
        stops.count {
            it.status == StopStatus.COMPLETED && it.deliveryStatus == DeliveryStatus.FAILED
        }
    val remainingStops: Int get() = stops.count { it.status == StopStatus.PENDING }

    fun progressPercent(): Int = if (totalStops == 0) 0 else (completedStops * 100) / totalStops
}

// ── Stop ──────────────────────────────────────────────────────────────────────

@Entity
@Table(name = "stops", schema = "route")
class Stop(
    @Id
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    val shift: Shift,
    @Column(name = "package_id", nullable = false)
    val packageId: UUID,
    @Column(name = "customer_id", nullable = false)
    val customerId: UUID,
    @Column(name = "courier_id", nullable = false)
    val courierId: UUID,
    @Column(name = "sequence_number", nullable = false)
    var sequenceNumber: Int,
    // Delivery address fields (denormalised for query performance)
    @Column(name = "street", nullable = false)
    val street: String,
    @Column(name = "house_number", nullable = false)
    val houseNumber: String,
    @Column(name = "apartment")
    val apartment: String? = null,
    @Column(name = "floor")
    val floor: Int? = null,
    @Column(name = "city", nullable = false)
    val city: String,
    @Column(name = "postal_code", nullable = false)
    val postalCode: String,
    @Column(name = "country", nullable = false)
    val country: String,
    @Column(name = "buzzer_code")
    val buzzerCode: String? = null,
    @Column(name = "delivery_instructions", columnDefinition = "TEXT")
    val deliveryInstructions: String? = null,
    @Column(name = "latitude", nullable = false)
    val latitude: Double,
    @Column(name = "longitude", nullable = false)
    val longitude: Double,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: StopStatus = StopStatus.PENDING,
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status")
    var deliveryStatus: DeliveryStatus? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason")
    var failureReason: FailureReason? = null,
    @Column(name = "courier_note", columnDefinition = "TEXT")
    var courierNote: String? = null,
    @Column(name = "estimated_arrival_at")
    var estimatedArrivalAt: Instant? = null,
    @Column(name = "arrived_at")
    var arrivedAt: Instant? = null,
    @Column(name = "completed_at")
    var completedAt: Instant? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
