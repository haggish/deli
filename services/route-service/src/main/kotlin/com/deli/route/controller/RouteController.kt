package com.deli.route.controller

import com.deli.route.domain.Shift
import com.deli.route.domain.Stop
import com.deli.route.service.RouteService
import com.deli.shared.api.request.AddStopRequest
import com.deli.shared.api.request.CompleteStopRequest
import com.deli.shared.api.request.StartShiftRequest
import com.deli.shared.api.response.AddressResponse
import com.deli.shared.api.response.ApiResponse
import com.deli.shared.api.response.RouteResponse
import com.deli.shared.api.response.ShiftSummaryResponse
import com.deli.shared.api.response.StopResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/routes")
class RouteController(
    private val routeService: RouteService,
) {
    // ── GET /api/routes/active ────────────────────────────────────────────────
    // Returns the active shift and its ordered stops for the calling courier

    @GetMapping("/active")
    fun getActiveRoute(
        @RequestHeader("X-User-Id") userId: String,
    ): ApiResponse<RouteResponse?> {
        val shift =
            routeService.getActiveShift(UUID.fromString(userId))
                ?: return ApiResponse.ok(null)

        val fullShift = routeService.getShiftWithStops(shift.id)
        return ApiResponse.ok(fullShift.toRouteResponse())
    }

    // ── POST /api/routes/shifts ───────────────────────────────────────────────

    @PostMapping("/shifts")
    @ResponseStatus(HttpStatus.CREATED)
    fun startShift(
        @RequestHeader("X-User-Id") userId: String,
        @Valid @RequestBody request: StartShiftRequest,
    ): ApiResponse<ShiftSummaryResponse> {
        val shift = routeService.startShift(UUID.fromString(userId), request)
        return ApiResponse.ok(shift.toSummaryResponse())
    }

    // ── PATCH /api/routes/shifts/{shiftId}/complete ───────────────────────────

    @PatchMapping("/shifts/{shiftId}/complete")
    fun completeShift(
        @RequestHeader("X-User-Id") userId: String,
        @PathVariable shiftId: UUID,
    ): ApiResponse<ShiftSummaryResponse> {
        val shift = routeService.completeShift(UUID.fromString(userId), shiftId)
        return ApiResponse.ok(shift.toSummaryResponse())
    }

    // ── GET /api/routes/shifts/{shiftId} ──────────────────────────────────────

    @GetMapping("/shifts/{shiftId}")
    fun getShift(
        @PathVariable shiftId: UUID,
    ): ApiResponse<RouteResponse> {
        val shift = routeService.getShiftWithStops(shiftId)
        return ApiResponse.ok(shift.toRouteResponse())
    }

    // ── PATCH /api/routes/stops/{stopId}/start ────────────────────────────────

    @PatchMapping("/stops/{stopId}/start")
    fun startStop(
        @PathVariable stopId: UUID,
    ): ApiResponse<StopResponse> {
        val stop = routeService.markStopInProgress(stopId)
        return ApiResponse.ok(stop.toResponse())
    }

    // ── POST /api/routes/shifts/{shiftId}/stops ───────────────────────────────

    @PostMapping("/shifts/{shiftId}/stops")
    @ResponseStatus(HttpStatus.CREATED)
    fun addStop(
        @RequestHeader("X-User-Id") userId: String,
        @PathVariable shiftId: UUID,
        @RequestBody body: AddStopRequest,
    ): ApiResponse<StopResponse> {
        val stop =
            routeService.addStop(
                shiftId = shiftId,
                packageId = UUID.fromString(body.packageId),
                customerId = UUID.fromString(body.customerId),
                courierId = UUID.fromString(userId),
                address = body.address,
                latitude = body.latitude,
                longitude = body.longitude,
            )
        return ApiResponse.ok(stop.toResponse())
    }

    // ── GET /api/routes/stops/{stopId} ────────────────────────────────────────

    @GetMapping("/stops/{stopId}")
    fun getStop(
        @PathVariable stopId: UUID,
    ): ApiResponse<StopResponse> {
        val stop = routeService.getStop(stopId)
        return ApiResponse.ok(stop.toResponse())
    }

    @PatchMapping("/stops/{stopId}/complete")
    fun completeStop(
        @PathVariable stopId: UUID,
        @RequestBody body: CompleteStopRequest,
    ): ApiResponse<StopResponse> {
        val stop = routeService.markStopCompleted(stopId, body.deliveryStatus, body.courierNote)
        return ApiResponse.ok(stop.toResponse())
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private fun Shift.toRouteResponse() =
        RouteResponse(
            shiftId = id.toString(),
            status = status,
            stops = stops.map { it.toResponse() },
            totalStops = totalStops,
            completedStops = completedStops,
            remainingStops = remainingStops,
        )

    private fun Shift.toSummaryResponse() =
        ShiftSummaryResponse(
            shiftId = id.toString(),
            courierId = courierId.toString(),
            courierName = "", // populated by API gateway aggregation in future
            status = status,
            scheduledDate = scheduledDate,
            totalStops = totalStops,
            completedStops = completedStops,
            failedStops = failedStops,
            progressPercent = progressPercent(),
            startedAt = startedAt,
            estimatedEndAt = null,
        )

    private fun Stop.toResponse() =
        StopResponse(
            id = id.toString(),
            sequenceNumber = sequenceNumber,
            status = status,
            customerName = "", // populated from customer service in future
            deliveryAddress =
                AddressResponse(
                    street = street,
                    houseNumber = houseNumber,
                    apartment = apartment,
                    floor = floor,
                    city = city,
                    postalCode = postalCode,
                    country = country,
                    formatted = "$street $houseNumber, $postalCode $city",
                ),
            latitude = latitude,
            longitude = longitude,
            packageId = packageId.toString(),
            trackingNumber = "", // populated from package service in future
            packageFlags = emptySet(),
            deliveryInstructions = deliveryInstructions,
            buzzerCode = buzzerCode,
            estimatedArrivalAt = estimatedArrivalAt,
            arrivedAt = arrivedAt,
            completedAt = completedAt,
            distanceMetres = null,
            estimatedMinutes = null,
        )
}
