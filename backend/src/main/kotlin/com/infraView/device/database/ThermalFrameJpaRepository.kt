package com.infraView.device.database

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime

interface ThermalFrameJpaRepository : JpaRepository<ThermalFrameJpaEntity, Long> {
    fun findFirstByIncidentIdOrderByCapturedAtDesc(incidentId: Long): ThermalFrameJpaEntity?
    fun findByIncidentIdAndCapturedAtAfterOrderByCapturedAt(
        incidentId: Long,
        after: OffsetDateTime,
        pageable: Pageable
    ): List<ThermalFrameJpaEntity>
}
