package com.deli.location.kafka

import com.deli.location.domain.LocationPing
import com.deli.shared.domain.events.EventEnvelope
import com.deli.shared.domain.events.EventTypes
import com.deli.shared.domain.events.KafkaTopics
import com.deli.shared.domain.events.LocationUpdatedEvent
import com.deli.shared.domain.valueobject.Coordinates
import com.deli.shared.domain.valueobject.CourierId
import com.deli.shared.domain.valueobject.ShiftId
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class LocationEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun publishLocationUpdated(ping: LocationPing) {
        val event =
            EventEnvelope(
                eventType = EventTypes.LOCATION_UPDATED,
                payload =
                    LocationUpdatedEvent(
                        courierId = CourierId.of(ping.courierId),
                        shiftId = ShiftId.of(ping.shiftId),
                        coordinates = Coordinates(ping.latitude, ping.longitude),
                        accuracyMetres = ping.accuracyMetres,
                        speedKmh = ping.speedKmh,
                        headingDegrees = ping.headingDegrees,
                        recordedAt = ping.recordedAt,
                    ),
            )

        // Partition by courierId — guarantees ordered processing per courier
        kafkaTemplate.send(
            KafkaTopics.LOCATION_UPDATED,
            ping.courierId.toString(),
            event,
        )
        log.debug("Published LocationUpdated for courier ${ping.courierId}")
    }
}
