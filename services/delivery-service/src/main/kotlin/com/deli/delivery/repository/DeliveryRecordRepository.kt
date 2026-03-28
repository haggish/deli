package com.deli.delivery.repository

import com.deli.delivery.domain.DeliveryRecord
import com.deli.shared.domain.model.DeliveryStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DeliveryRecordRepository : JpaRepository<DeliveryRecord, UUID> {
    fun findByStopId(stopId: UUID): DeliveryRecord?

    fun findByPackageId(packageId: UUID): DeliveryRecord?

    fun findByCourierIdAndStatus(
        courierId: UUID,
        status: DeliveryStatus,
    ): List<DeliveryRecord>

    fun findByShiftId(shiftId: UUID): List<DeliveryRecord>

    fun existsByStopId(stopId: UUID): Boolean
}
