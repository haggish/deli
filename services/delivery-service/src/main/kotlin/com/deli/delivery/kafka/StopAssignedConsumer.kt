package com.deli.delivery.kafka

import com.deli.delivery.domain.DeliveryRecord
import com.deli.delivery.repository.DeliveryRecordRepository
import com.deli.shared.domain.events.EventEnvelope
import com.deli.shared.domain.events.KafkaTopics
import com.deli.shared.domain.events.StopAssignedEvent
import com.deli.shared.domain.model.DeliveryStatus
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class StopAssignedConsumer(
    private val deliveryRecordRepository: DeliveryRecordRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    @KafkaListener(
        topics = [KafkaTopics.STOP_ASSIGNED],
        groupId = "delivery-service",
        containerFactory = "kafkaListenerContainerFactory",
    )
    @Transactional
    fun onStopAssigned(
        @Payload message: String,
        @Header(KafkaHeaders.RECEIVED_KEY) key: String?,
        @Header(KafkaHeaders.OFFSET) offset: Long,
    ) {
        log.debug("Received StopAssigned event at offset $offset")

        try {
            val envelope = json.decodeFromString<EventEnvelope<StopAssignedEvent>>(message)
            val event = envelope.payload

            // Idempotency check — skip if already processed
            if (deliveryRecordRepository.existsByStopId(event.stopId.value)) {
                log.debug("StopAssigned event ${envelope.eventId} already processed, skipping")
                return
            }

            val record =
                DeliveryRecord(
                    stopId = event.stopId.value,
                    shiftId = event.shiftId.value,
                    packageId = event.packageId.value,
                    customerId = event.customerId.value,
                    courierId = event.courierId.value,
                    trackingNumber = "", // Will be populated when package service is added
                    status = DeliveryStatus.ASSIGNED,
                )

            deliveryRecordRepository.save(record)
            log.info("Created DeliveryRecord for stop ${event.stopId}")
        } catch (e: Exception) {
            log.error("Failed to process StopAssigned event at offset $offset", e)
            throw e // Re-throw to trigger Kafka retry/DLQ
        }
    }
}
