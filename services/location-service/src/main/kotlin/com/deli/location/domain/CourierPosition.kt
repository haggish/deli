package com.deli.location.domain

import java.time.Instant

/**
 * Denormalised snapshot of a courier's current position stored in Redis.
 *
 * Key pattern: courier:position:{courierId}
 * TTL: 60 seconds — if the courier stops sending pings, the key expires
 *      and the courier is considered offline.
 *
 * This is the hot path for the customer tracking screen:
 *   Customer opens tracking → reads from Redis → WebSocket pushes updates.
 * TimescaleDB stores the full history; Redis stores only the latest position.
 */
data class CourierPosition(
    val courierId: String,
    val shiftId: String,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Double?,
    val headingDegrees: Double?,
    val updatedAt: Instant,
    val isOnline: Boolean = true,
)
