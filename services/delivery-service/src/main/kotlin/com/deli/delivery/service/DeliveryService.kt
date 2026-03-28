package com.deli.delivery.service

import com.deli.delivery.domain.DeliveryRecord
import com.deli.delivery.kafka.DeliveryEventPublisher
import com.deli.delivery.repository.DeliveryRecordRepository
import com.deli.delivery.s3.PresignedUpload
import com.deli.delivery.s3.S3Service
import com.deli.shared.api.request.ConfirmDeliveryRequest
import com.deli.shared.api.request.ReportFailedDeliveryRequest
import com.deli.shared.api.request.RequestPhotoUploadUrlRequest
import com.deli.shared.domain.model.DeliveryStatus
import com.deli.shared.domain.model.StopAlreadyCompletedException
import com.deli.shared.domain.model.StopNotFoundException
import com.deli.shared.domain.valueobject.StopId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class DeliveryService(
    private val deliveryRecordRepository: DeliveryRecordRepository,
    private val eventPublisher: DeliveryEventPublisher,
    private val s3Service: S3Service,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // ── Query ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    fun getByStopId(stopId: UUID): DeliveryRecord =
        deliveryRecordRepository.findByStopId(stopId)
            ?: throw StopNotFoundException(StopId.of(stopId))

    @Transactional(readOnly = true)
    fun getByPackageId(packageId: UUID): DeliveryRecord? = deliveryRecordRepository.findByPackageId(packageId)

    // ── Confirm delivery ──────────────────────────────────────────────────────

    fun confirmDelivery(
        stopId: UUID,
        request: ConfirmDeliveryRequest,
    ): DeliveryRecord {
        val record =
            deliveryRecordRepository.findByStopId(stopId)
                ?: throw StopNotFoundException(StopId.of(stopId))

        if (record.status == DeliveryStatus.DELIVERED || record.status == DeliveryStatus.FAILED) {
            throw StopAlreadyCompletedException(StopId.of(stopId))
        }

        record.status = DeliveryStatus.DELIVERED
        record.placement = request.placement
        record.courierNote = request.courierNote
        record.confirmedAt = Instant.now()
        record.updatedAt = Instant.now()

        val saved = deliveryRecordRepository.save(record)
        eventPublisher.publishDeliveryConfirmed(saved)

        log.info("Delivery confirmed for stop $stopId, placement ${request.placement}")
        return saved
    }

    // ── Report failure ────────────────────────────────────────────────────────

    fun reportFailure(
        stopId: UUID,
        request: ReportFailedDeliveryRequest,
    ): DeliveryRecord {
        val record =
            deliveryRecordRepository.findByStopId(stopId)
                ?: throw StopNotFoundException(StopId.of(stopId))

        if (record.status == DeliveryStatus.DELIVERED || record.status == DeliveryStatus.FAILED) {
            throw StopAlreadyCompletedException(StopId.of(stopId))
        }

        record.status = DeliveryStatus.FAILED
        record.failureReason = request.reason
        record.courierNote = request.courierNote
        record.updatedAt = Instant.now()

        val saved = deliveryRecordRepository.save(record)
        eventPublisher.publishDeliveryFailed(saved)

        log.info("Delivery failed for stop $stopId, reason ${request.reason}")
        return saved
    }

    // ── S3 pre-signed URLs ────────────────────────────────────────────────────

    fun getPhotoUploadUrl(request: RequestPhotoUploadUrlRequest): PresignedUpload {
        val stopId = request.stopId
        // Verify the stop exists
        deliveryRecordRepository.findByStopId(UUID.fromString(stopId))
            ?: throw StopNotFoundException(StopId.of(UUID.fromString(stopId)))

        return s3Service.generatePhotoUploadUrl(stopId, request.contentType)
    }

    fun getSignatureUploadUrl(stopId: UUID): PresignedUpload {
        deliveryRecordRepository.findByStopId(stopId)
            ?: throw StopNotFoundException(StopId.of(stopId))

        return s3Service.generateSignatureUploadUrl(stopId.toString())
    }

    // ── Record photo/signature keys after upload ──────────────────────────────

    fun recordPhotoUploaded(
        stopId: UUID,
        fileKey: String,
    ): DeliveryRecord {
        val record =
            deliveryRecordRepository.findByStopId(stopId)
                ?: throw StopNotFoundException(StopId.of(stopId))

        record.proofPhotoKey = fileKey
        record.updatedAt = Instant.now()
        return deliveryRecordRepository.save(record)
    }

    fun recordSignatureUploaded(
        stopId: UUID,
        fileKey: String,
    ): DeliveryRecord {
        val record =
            deliveryRecordRepository.findByStopId(stopId)
                ?: throw StopNotFoundException(StopId.of(stopId))

        record.signatureKey = fileKey
        record.updatedAt = Instant.now()
        return deliveryRecordRepository.save(record)
    }
}
