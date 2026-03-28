package com.deli.delivery.kafka

import com.deli.delivery.domain.DeliveryRecord
import com.deli.shared.domain.events.DeliveryConfirmedEvent
import com.deli.shared.domain.events.DeliveryFailedEvent
import com.deli.shared.domain.events.EventEnvelope
import com.deli.shared.domain.events.EventTypes
import com.deli.shared.domain.events.KafkaTopics
import com.deli.shared.domain.valueobject.CourierId
import com.deli.shared.domain.valueobject.CustomerId
import com.deli.shared.domain.valueobject.PackageId
import com.deli.shared.domain.valueobject.StopId
import com.deli.shared.domain.valueobject.TrackingNumber
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class DeliveryEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun publishDeliveryConfirmed(record: DeliveryRecord) {
        val event =
            EventEnvelope(
                eventType = EventTypes.DELIVERY_CONFIRMED,
                payload =
                    DeliveryConfirmedEvent(
                        stopId = StopId.of(record.stopId),
                        packageId = PackageId.of(record.packageId),
                        trackingNumber = TrackingNumber.generate("LOC"), // placeholder until package service
                        courierId = CourierId.of(record.courierId),
                        customerId = CustomerId.of(record.customerId),
                        deliveredAt = record.confirmedAt ?: Instant.now(),
                        placement = record.placement!!,
                        proofPhotoUrl = record.proofPhotoKey,
                        signatureUrl = record.signatureKey,
                        courierNote = record.courierNote,
                    ),
            )

        kafkaTemplate.send(KafkaTopics.DELIVERY_CONFIRMED, record.packageId.toString(), event)
        log.debug("Published DeliveryConfirmed for stop ${record.stopId}")
    }

    fun publishDeliveryFailed(record: DeliveryRecord) {
        val event =
            EventEnvelope(
                eventType = EventTypes.DELIVERY_FAILED,
                payload =
                    DeliveryFailedEvent(
                        stopId = StopId.of(record.stopId),
                        packageId = PackageId.of(record.packageId),
                        trackingNumber = TrackingNumber.generate("LOC"),
                        courierId = CourierId.of(record.courierId),
                        customerId = CustomerId.of(record.customerId),
                        failedAt = Instant.now(),
                        reason = record.failureReason!!,
                        courierNote = record.courierNote,
                        attemptNumber = record.attemptNumber,
                        willReschedule = record.attemptNumber < 3,
                    ),
            )

        kafkaTemplate.send(KafkaTopics.DELIVERY_FAILED, record.packageId.toString(), event)
        log.debug("Published DeliveryFailed for stop ${record.stopId}")
    }
}
