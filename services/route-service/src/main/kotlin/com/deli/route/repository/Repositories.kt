package com.deli.route.repository

import com.deli.route.domain.Shift
import com.deli.route.domain.Stop
import com.deli.shared.domain.model.ShiftStatus
import com.deli.shared.domain.model.StopStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ShiftRepository : JpaRepository<Shift, UUID> {
    fun findByCourierIdAndStatus(
        courierId: UUID,
        status: ShiftStatus,
    ): List<Shift>

    fun findByCourierIdAndScheduledDate(
        courierId: UUID,
        scheduledDate: String,
    ): Shift?

    @Query(
        """
        SELECT s FROM Shift s
        LEFT JOIN FETCH s.stops st
        WHERE s.id = :id
        ORDER BY st.sequenceNumber ASC
        """,
    )
    fun findByIdWithStops(
        @Param("id") id: UUID,
    ): Shift?
}

@Repository
interface StopRepository : JpaRepository<Stop, UUID> {
    fun findByShiftIdOrderBySequenceNumber(shiftId: UUID): List<Stop>

    fun findByShiftIdAndStatus(
        shiftId: UUID,
        status: StopStatus,
    ): List<Stop>

    fun findByPackageId(packageId: UUID): Stop?

    @Query(
        """
        SELECT st FROM Stop st
        WHERE st.shift.courierId = :courierId
        AND st.status = 'PENDING'
        ORDER BY st.sequenceNumber ASC
        """,
    )
    fun findPendingStopsByCourierId(
        @Param("courierId") courierId: UUID,
    ): List<Stop>
}
