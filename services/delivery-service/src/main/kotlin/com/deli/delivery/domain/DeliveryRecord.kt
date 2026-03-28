package com.deli.delivery.domain

import com.deli.shared.domain.model.DeliveryPlacement
import com.deli.shared.domain.model.DeliveryStatus
import com.deli.shared.domain.model.FailureReason
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * Delivery record — the delivery service's projection of a stop.
 *
 * Created when route-service publishes a StopAssignedEvent.
 * Updated when the courier confirms delivery or reports failure.
 * The source of truth for proof-of-delivery data (photo/signature URLs).
 */
@Entity
@Table(name = "delivery_records", schema = "delivery")
class DeliveryRecord(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "stop_id", nullable = false, unique = true)
    val stopId: UUID,
    @Column(name = "shift_id", nullable = false)
    val shiftId: UUID,
    @Column(name = "package_id", nullable = false)
    val packageId: UUID,
    @Column(name = "customer_id", nullable = false)
    val customerId: UUID,
    @Column(name = "courier_id", nullable = false)
    val courierId: UUID,
    @Column(name = "tracking_number", nullable = false)
    val trackingNumber: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: DeliveryStatus = DeliveryStatus.ASSIGNED,
    @Enumerated(EnumType.STRING)
    @Column(name = "placement")
    var placement: DeliveryPlacement? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason")
    var failureReason: FailureReason? = null,
    @Column(name = "courier_note", columnDefinition = "TEXT")
    var courierNote: String? = null,
    // S3 keys (not URLs — URLs are generated on demand as pre-signed links)
    @Column(name = "proof_photo_key")
    var proofPhotoKey: String? = null,
    @Column(name = "signature_key")
    var signatureKey: String? = null,
    @Column(name = "confirmed_at")
    var confirmedAt: Instant? = null,
    @Column(name = "attempt_number", nullable = false)
    var attemptNumber: Int = 1,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
