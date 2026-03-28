package com.deli.location.repository

import com.deli.location.domain.LocationPing
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface LocationPingRepository : JpaRepository<LocationPing, UUID> {
    /**
     * Fetch the trail of pings for a courier during a shift.
     * Used for replay and route audit.
     * TimescaleDB makes this query fast because recorded_at is the
     * hypertable partition column — it performs a targeted chunk scan.
     */
    @Query(
        """
        SELECT p FROM LocationPing p
        WHERE p.courierId = :courierId
        AND p.shiftId = :shiftId
        AND p.recordedAt BETWEEN :from AND :to
        ORDER BY p.recordedAt ASC
        """,
    )
    fun findTrail(
        @Param("courierId") courierId: UUID,
        @Param("shiftId") shiftId: UUID,
        @Param("from") from: Instant,
        @Param("to") to: Instant,
    ): List<LocationPing>

    /**
     * Latest ping for a courier in a shift — used for ETA recalculation.
     */
    @Query(
        """
        SELECT p FROM LocationPing p
        WHERE p.courierId = :courierId
        AND p.shiftId = :shiftId
        ORDER BY p.recordedAt DESC
        LIMIT 1
        """,
    )
    fun findLatest(
        @Param("courierId") courierId: UUID,
        @Param("shiftId") shiftId: UUID,
    ): LocationPing?
}
