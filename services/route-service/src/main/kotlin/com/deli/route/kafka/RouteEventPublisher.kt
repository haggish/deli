package com.deli.route.kafka

import com.deli.route.domain.Shift
import com.deli.route.domain.Stop
import com.deli.shared.domain.events.EventEnvelope
import com.deli.shared.domain.events.EventTypes
import com.deli.shared.domain.events.KafkaTopics
import com.deli.shared.domain.events.RouteChangeReason
import com.deli.shared.domain.events.RouteUpdatedEvent
import com.deli.shared.domain.events.ShiftCompletedEvent
import com.deli.shared.domain.events.ShiftStartedEvent
import com.deli.shared.domain.events.StopAssignedEvent
import com.deli.shared.domain.valueobject.CourierId
import com.deli.shared.domain.valueobject.CustomerId
import com.deli.shared.domain.valueobject.PackageId
import com.deli.shared.domain.valueobject.ShiftId
import com.deli.shared.domain.valueobject.StopId
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class RouteEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun publishStopAssigned(
        stop: Stop,
        shift: Shift,
    ) {
        val event =
            EventEnvelope(
                eventType = EventTypes.STOP_ASSIGNED,
                payload =
                    StopAssignedEvent(
                        stopId = StopId.of(stop.id.toString()),
                        shiftId = ShiftId.of(shift.id),
                        courierId = CourierId.of(stop.courierId),
                        packageId = PackageId.of(stop.packageId),
                        customerId = CustomerId.of(stop.customerId),
                        sequenceNumber = stop.sequenceNumber,
                        estimatedArrivalAt = stop.estimatedArrivalAt,
                    ),
            )
        // Partition by shiftId so all events for a shift land on the same partition
        kafkaTemplate.send(KafkaTopics.STOP_ASSIGNED, shift.id.toString(), event)
        log.debug("Published StopAssigned for stop ${stop.id}")
    }

    fun publishRouteUpdated(
        shift: Shift,
        reason: RouteChangeReason,
    ) {
        val event =
            EventEnvelope(
                eventType = EventTypes.ROUTE_UPDATED,
                payload =
                    RouteUpdatedEvent(
                        shiftId = ShiftId.of(shift.id),
                        courierId = CourierId.of(shift.courierId),
                        totalStops = shift.totalStops,
                        updatedAt = Instant.now(),
                        changeReason = reason,
                    ),
            )
        kafkaTemplate.send(KafkaTopics.ROUTE_UPDATED, shift.id.toString(), event)
        log.debug("Published RouteUpdated for shift ${shift.id}")
    }

    fun publishShiftStarted(shift: Shift) {
        val event =
            EventEnvelope(
                eventType = EventTypes.SHIFT_STARTED,
                payload =
                    ShiftStartedEvent(
                        shiftId = ShiftId.of(shift.id),
                        courierId = CourierId.of(shift.courierId),
                        scheduledDate = shift.scheduledDate,
                        totalStops = shift.totalStops,
                        startedAt = shift.startedAt ?: Instant.now(),
                    ),
            )
        kafkaTemplate.send(KafkaTopics.SHIFT_STARTED, shift.id.toString(), event)
    }

    fun publishShiftCompleted(shift: Shift) {
        val event =
            EventEnvelope(
                eventType = EventTypes.SHIFT_COMPLETED,
                payload =
                    ShiftCompletedEvent(
                        shiftId = ShiftId.of(shift.id),
                        courierId = CourierId.of(shift.courierId),
                        status = shift.status,
                        completedStops = shift.completedStops,
                        failedStops = shift.failedStops,
                        totalStops = shift.totalStops,
                        completedAt = shift.completedAt ?: Instant.now(),
                    ),
            )
        kafkaTemplate.send(KafkaTopics.SHIFT_COMPLETED, shift.id.toString(), event)
    }
}
