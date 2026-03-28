package com.deli.route.service

import com.deli.route.domain.Shift
import com.deli.route.domain.Stop
import com.deli.route.kafka.RouteEventPublisher
import com.deli.route.repository.ShiftRepository
import com.deli.route.repository.StopRepository
import com.deli.shared.api.request.AddressRequest
import com.deli.shared.api.request.StartShiftRequest
import com.deli.shared.domain.model.DeliveryStatus
import com.deli.shared.domain.model.ShiftNotActiveException
import com.deli.shared.domain.model.ShiftNotFoundException
import com.deli.shared.domain.model.ShiftStatus
import com.deli.shared.domain.model.StopAlreadyCompletedException
import com.deli.shared.domain.model.StopNotFoundException
import com.deli.shared.domain.model.StopStatus
import com.deli.shared.domain.valueobject.ShiftId
import com.deli.shared.domain.valueobject.StopId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class RouteService(
    private val shiftRepository: ShiftRepository,
    private val stopRepository: StopRepository,
    private val eventPublisher: RouteEventPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // ── Shift management ──────────────────────────────────────────────────────

    fun startShift(
        courierId: UUID,
        request: StartShiftRequest,
    ): Shift {
        // Check for existing active shift
        val existing = shiftRepository.findByCourierIdAndStatus(courierId, ShiftStatus.ACTIVE)
        if (existing.isNotEmpty()) {
            return existing.first()
        }

        val shift =
            shiftRepository.save(
                Shift(
                    courierId = courierId,
                    scheduledDate = request.scheduledDate,
                    status = ShiftStatus.ACTIVE,
                    startedAt = Instant.now(),
                ),
            )

        eventPublisher.publishShiftStarted(shift)
        log.info("Shift ${shift.id} started for courier $courierId")
        return shift
    }

    fun completeShift(
        courierId: UUID,
        shiftId: UUID,
    ): Shift {
        val shift =
            shiftRepository.findByIdWithStops(shiftId)
                ?: throw ShiftNotFoundException(ShiftId.of(shiftId))

        shift.status = ShiftStatus.COMPLETED
        shift.completedAt = Instant.now()
        val saved = shiftRepository.save(shift)

        eventPublisher.publishShiftCompleted(saved)
        log.info("Shift $shiftId completed for courier $courierId")
        return saved
    }

    @Transactional(readOnly = true)
    fun getActiveShift(courierId: UUID): Shift? =
        shiftRepository.findByCourierIdAndStatus(courierId, ShiftStatus.ACTIVE).firstOrNull()

    @Transactional(readOnly = true)
    fun getShiftWithStops(shiftId: UUID): Shift =
        shiftRepository.findByIdWithStops(shiftId)
            ?: throw ShiftNotFoundException(ShiftId.of(shiftId))

    // ── Stop management ───────────────────────────────────────────────────────

    fun addStop(
        shiftId: UUID,
        packageId: UUID,
        customerId: UUID,
        courierId: UUID,
        address: AddressRequest,
        latitude: Double,
        longitude: Double,
    ): Stop {
        val shift =
            shiftRepository.findByIdWithStops(shiftId)
                ?: throw ShiftNotFoundException(ShiftId.of(shiftId))

        if (shift.status != ShiftStatus.ACTIVE) {
            throw ShiftNotActiveException(ShiftId.of(shiftId))
        }

        val nextSequence = (shift.stops.maxOfOrNull { it.sequenceNumber } ?: 0) + 1

        val stop =
            stopRepository.save(
                Stop(
                    shift = shift,
                    packageId = packageId,
                    customerId = customerId,
                    courierId = courierId,
                    sequenceNumber = nextSequence,
                    street = address.street,
                    houseNumber = address.houseNumber,
                    apartment = address.apartment,
                    floor = address.floor,
                    city = address.city,
                    postalCode = address.postalCode,
                    country = address.country,
                    buzzerCode = address.buzzerCode,
                    deliveryInstructions = address.deliveryInstructions,
                    latitude = latitude,
                    longitude = longitude,
                ),
            )

        eventPublisher.publishStopAssigned(stop, shift)
        log.info("Stop ${stop.id} added to shift $shiftId (sequence $nextSequence)")
        return stop
    }

    fun markStopInProgress(stopId: UUID): Stop {
        val stop =
            stopRepository.findById(stopId).orElseThrow {
                StopNotFoundException(StopId.of(stopId))
            }

        if (stop.status == StopStatus.COMPLETED) {
            throw StopAlreadyCompletedException(StopId.of(stopId))
        }

        stop.status = StopStatus.IN_PROGRESS
        stop.arrivedAt = Instant.now()
        stop.updatedAt = Instant.now()
        return stopRepository.save(stop)
    }

    fun markStopCompleted(
        stopId: UUID,
        deliveryStatus: DeliveryStatus,
        courierNote: String? = null,
    ): Stop {
        val stop =
            stopRepository.findById(stopId).orElseThrow {
                StopNotFoundException(StopId.of(stopId))
            }

        if (stop.status == StopStatus.COMPLETED) {
            throw StopAlreadyCompletedException(StopId.of(stopId))
        }

        stop.status = StopStatus.COMPLETED
        stop.deliveryStatus = deliveryStatus
        stop.courierNote = courierNote
        stop.completedAt = Instant.now()
        stop.updatedAt = Instant.now()

        val saved = stopRepository.save(stop)

        // Check if all stops are done and auto-complete the shift
        val shift = stop.shift
        val allDone = shift.stops.all { it.status == StopStatus.COMPLETED }
        if (allDone && shift.status == ShiftStatus.ACTIVE) {
            shift.status = ShiftStatus.COMPLETED
            shift.completedAt = Instant.now()
            shiftRepository.save(shift)
            eventPublisher.publishShiftCompleted(shift)
        }

        return saved
    }

    @Transactional(readOnly = true)
    fun getStopsForShift(shiftId: UUID): List<Stop> = stopRepository.findByShiftIdOrderBySequenceNumber(shiftId)

    @Transactional(readOnly = true)
    fun getStop(stopId: UUID): Stop =
        stopRepository.findById(stopId).orElseThrow {
            StopNotFoundException(StopId.of(stopId))
        }
}
