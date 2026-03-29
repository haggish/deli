package com.deli.notification.consumer

import com.deli.notification.service.NotificationService
import com.deli.notification.service.FcmTokenRegistry
import com.deli.shared.domain.events.DeliveryConfirmedEvent
import com.deli.shared.domain.events.DeliveryFailedEvent
import com.deli.shared.domain.events.EventEnvelope
import com.deli.shared.domain.events.KafkaTopics
import com.deli.shared.domain.events.RouteUpdatedEvent
import com.deli.shared.domain.events.ShiftCompletedEvent
import com.deli.shared.domain.events.ShiftStartedEvent
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class NotificationConsumers(
    private val notificationService: NotificationService,
    private val tokenRegistry: FcmTokenRegistry,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ── delivery.confirmed ────────────────────────────────────────────────────

    @KafkaListener(
        topics = [KafkaTopics.DELIVERY_CONFIRMED],
        groupId = "notification-service-delivery-confirmed",
    )
    fun onDeliveryConfirmed(
        @Payload message: String,
        @Header(KafkaHeaders.OFFSET) offset: Long,
    ) {
        log.debug("Received DeliveryConfirmed at offset $offset")
        runCatching {
            val envelope = json.decodeFromString<EventEnvelope<DeliveryConfirmedEvent>>(message)
            val event = envelope.payload
            val customerToken = tokenRegistry.getCustomerToken(event.customerId.value.toString())

            notificationService.notifyDeliveryConfirmed(
                customerFcmToken = customerToken,
                trackingNumber = event.trackingNumber.value,
                placement = event.placement.name.replace('_', ' ').lowercase()
                    .replaceFirstChar { it.uppercase() },
            )
        }.onFailure {
            log.error("Failed to process DeliveryConfirmed at offset $offset", it)
        }
    }

    // ── delivery.failed ───────────────────────────────────────────────────────

    @KafkaListener(
        topics = [KafkaTopics.DELIVERY_FAILED],
        groupId = "notification-service-delivery-failed",
    )
    fun onDeliveryFailed(
        @Payload message: String,
        @Header(KafkaHeaders.OFFSET) offset: Long,
    ) {
        log.debug("Received DeliveryFailed at offset $offset")
        runCatching {
            val envelope = json.decodeFromString<EventEnvelope<DeliveryFailedEvent>>(message)
            val event = envelope.payload
            val customerToken = tokenRegistry.getCustomerToken(event.customerId.value.toString())

            notificationService.notifyDeliveryFailed(
                customerFcmToken = customerToken,
                trackingNumber = event.trackingNumber.value,
                reason = event.reason.name.replace('_', ' ').lowercase()
                    .replaceFirstChar { it.uppercase() },
                willReschedule = event.willReschedule,
            )
        }.onFailure {
            log.error("Failed to process DeliveryFailed at offset $offset", it)
        }
    }

    // ── shift.started ─────────────────────────────────────────────────────────

    @KafkaListener(
        topics = [KafkaTopics.SHIFT_STARTED],
        groupId = "notification-service-shift-started",
    )
    fun onShiftStarted(
        @Payload message: String,
        @Header(KafkaHeaders.OFFSET) offset: Long,
    ) {
        log.debug("Received ShiftStarted at offset $offset")
        runCatching {
            val envelope = json.decodeFromString<EventEnvelope<ShiftStartedEvent>>(message)
            val event = envelope.payload
            val courierToken = tokenRegistry.getCourierToken(event.courierId.value.toString())

            notificationService.notifyShiftStarted(
                courierFcmToken = courierToken,
                totalStops = event.totalStops,
                scheduledDate = event.scheduledDate,
            )
        }.onFailure {
            log.error("Failed to process ShiftStarted at offset $offset", it)
        }
    }

    // ── route.updated ─────────────────────────────────────────────────────────

    @KafkaListener(
        topics = [KafkaTopics.ROUTE_UPDATED],
        groupId = "notification-service-route-updated",
    )
    fun onRouteUpdated(
        @Payload message: String,
        @Header(KafkaHeaders.OFFSET) offset: Long,
    ) {
        log.debug("Received RouteUpdated at offset $offset")
        runCatching {
            val envelope = json.decodeFromString<EventEnvelope<RouteUpdatedEvent>>(message)
            val event = envelope.payload
            val courierToken = tokenRegistry.getCourierToken(event.courierId.value.toString())

            notificationService.notifyRouteUpdated(
                courierFcmToken = courierToken,
                totalStops = event.totalStops,
                reason = event.changeReason.name.replace('_', ' ').lowercase()
                    .replaceFirstChar { it.uppercase() },
            )
        }.onFailure {
            log.error("Failed to process RouteUpdated at offset $offset", it)
        }
    }

    // ── shift.completed ───────────────────────────────────────────────────────

    @KafkaListener(
        topics = [KafkaTopics.SHIFT_COMPLETED],
        groupId = "notification-service-shift-completed",
    )
    fun onShiftCompleted(
        @Payload message: String,
        @Header(KafkaHeaders.OFFSET) offset: Long,
    ) {
        log.debug("Received ShiftCompleted at offset $offset")
        runCatching {
            val envelope = json.decodeFromString<EventEnvelope<ShiftCompletedEvent>>(message)
            val event = envelope.payload
            val courierToken = tokenRegistry.getCourierToken(event.courierId.value.toString())

            notificationService.notifyShiftCompleted(
                courierFcmToken = courierToken,
                completedStops = event.completedStops,
                failedStops = event.failedStops,
            )
        }.onFailure {
            log.error("Failed to process ShiftCompleted at offset $offset", it)
        }
    }
}
