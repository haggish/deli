package com.deli.location.controller

import com.deli.location.domain.CourierPosition
import com.deli.location.domain.LocationPing
import com.deli.location.service.LocationService
import com.deli.shared.api.response.ApiResponse
import com.deli.shared.api.response.CourierPositionResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/locations")
class LocationController(
    private val locationService: LocationService,
) {
    // ── GET /api/locations/couriers/{courierId} ────────────────────────────────
    // Customer tracking screen polls this to get the courier's current position.
    // In production this is replaced by a WebSocket push from the server.

    @GetMapping("/couriers/{courierId}")
    fun getCourierPosition(
        @PathVariable courierId: UUID,
    ): ApiResponse<CourierPositionResponse?> {
        val position = locationService.getPosition(courierId)
        return ApiResponse.ok(position?.toResponse())
    }

    // ── GET /api/locations/couriers/{courierId}/trail ─────────────────────────
    // Returns the GPS trail for a shift — used by the dispatcher UI and audit.

    @GetMapping("/couriers/{courierId}/trail")
    fun getCourierTrail(
        @PathVariable courierId: UUID,
        @RequestParam shiftId: UUID,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant,
    ): ApiResponse<List<TrailPointResponse>> {
        val trail = locationService.getTrail(courierId, shiftId, from, to)
        return ApiResponse.ok(trail.map { it.toTrailPoint() })
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private fun CourierPosition.toResponse() =
        CourierPositionResponse(
            courierId = courierId,
            latitude = latitude,
            longitude = longitude,
            speedKmh = speedKmh,
            headingDegrees = headingDegrees,
            updatedAt = updatedAt,
        )

    private fun LocationPing.toTrailPoint() =
        TrailPointResponse(
            latitude = latitude,
            longitude = longitude,
            speedKmh = speedKmh,
            headingDegrees = headingDegrees,
            recordedAt = recordedAt,
        )
}

data class TrailPointResponse(
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Double?,
    val headingDegrees: Double?,
    val recordedAt: Instant,
)
