package com.deli.location.service

import com.deli.location.domain.CourierPosition
import com.deli.location.domain.LocationPing
import com.deli.location.kafka.LocationEventPublisher
import com.deli.location.repository.LocationPingRepository
import com.deli.shared.api.request.LocationPingRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class LocationService(
    private val pingRepository: LocationPingRepository,
    private val positionCache: CourierPositionCache,
    private val eventPublisher: LocationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Process a single GPS ping from a courier's device.
     *
     * Execution order (all synchronous within the request):
     * 1. Persist to TimescaleDB — durable record, survives restart
     * 2. Update Redis cache — fast read path for tracking screen
     * 3. Publish Kafka event — async fanout to other consumers
     *
     * The WebSocket broadcast to watching customers happens via
     * the Kafka consumer in the same service (see LocationUpdatedConsumer).
     * This decouples the inbound GPS path from the outbound push path.
     */
    @Transactional
    fun processPing(
        courierId: UUID,
        shiftId: UUID,
        request: LocationPingRequest,
    ): LocationPing {
        val recordedAt =
            try {
                Instant.parse(request.recordedAt)
            } catch (e: Exception) {
                log.warn("Invalid recordedAt timestamp '${request.recordedAt}', using server time")
                Instant.now()
            }

        // 1. Persist to TimescaleDB
        val ping =
            pingRepository.save(
                LocationPing(
                    courierId = courierId,
                    shiftId = shiftId,
                    latitude = request.latitude,
                    longitude = request.longitude,
                    accuracyMetres = request.accuracyMetres,
                    speedKmh = request.speedKmh,
                    headingDegrees = request.headingDegrees,
                    recordedAt = recordedAt,
                    receivedAt = Instant.now(),
                ),
            )

        // 2. Update Redis position cache
        positionCache.update(
            CourierPosition(
                courierId = courierId.toString(),
                shiftId = shiftId.toString(),
                latitude = request.latitude,
                longitude = request.longitude,
                speedKmh = request.speedKmh,
                headingDegrees = request.headingDegrees,
                updatedAt = Instant.now(),
                isOnline = true,
            ),
        )

        // 3. Publish to Kafka for ETA updates, audit trail, dispatcher UI
        eventPublisher.publishLocationUpdated(ping)

        log.debug("Processed ping for courier $courierId at (${request.latitude}, ${request.longitude})")
        return ping
    }

    @Transactional(readOnly = true)
    fun getPosition(courierId: UUID): CourierPosition? = positionCache.get(courierId.toString())

    @Transactional(readOnly = true)
    fun getTrail(
        courierId: UUID,
        shiftId: UUID,
        from: Instant,
        to: Instant,
    ): List<LocationPing> = pingRepository.findTrail(courierId, shiftId, from, to)
}
