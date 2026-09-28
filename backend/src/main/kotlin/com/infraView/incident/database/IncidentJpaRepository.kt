package com.infraView.incident.database

import com.infraView.incident.domain.IncidentStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository


@Repository
interface IncidentJpaRepository : JpaRepository<IncidentJpaEntity, Long> {
    fun findFirstByDeviceIdAndStatus(deviceId: Long, status: IncidentStatus): IncidentJpaEntity?
}