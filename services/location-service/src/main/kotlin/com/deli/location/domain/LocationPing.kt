package com.deli.location.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * A single GPS reading from a courier's device.
 *
 * Stored in TimescaleDB as a hypertable partitioned by recorded_at.
 * The hypertable conversion is performed in the Flyway migration — JPA
 * sees this as an ordinary table.
 *
 * Retention policy: 90 days (configured in migration via TimescaleDB policy).
 * Do NOT add JPA cascade delete — TimescaleDB manages chunk expiry itself.
 */
@Entity
@Table(name = "location_pings", schema = "gps")
class LocationPing(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "courier_id", nullable = false)
    val courierId: UUID,
    @Column(name = "shift_id", nullable = false)
    val shiftId: UUID,
    @Column(name = "latitude", nullable = false)
    val latitude: Double,
    @Column(name = "longitude", nullable = false)
    val longitude: Double,
    @Column(name = "accuracy_metres", nullable = false)
    val accuracyMetres: Double,
    @Column(name = "speed_kmh")
    val speedKmh: Double? = null,
    @Column(name = "heading_degrees")
    val headingDegrees: Double? = null,
    // Device-reported timestamp — used for hypertable partitioning.
    // Must be named recorded_at to match the hypertable partition column.
    @Column(name = "recorded_at", nullable = false)
    val recordedAt: Instant = Instant.now(),
    // Server-side receipt timestamp — used for latency monitoring
    @Column(name = "received_at", nullable = false)
    val receivedAt: Instant = Instant.now(),
)
