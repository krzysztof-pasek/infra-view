package com.infraView.incident.database

import com.infraView.incident.domain.StatusType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository


@Repository
interface SpringDataIncidentRepository : JpaRepository<IncidentJpaEntity, Long> {
    fun findFirstByDeviceIdAndStatus(deviceId: Long, status: StatusType): IncidentJpaEntity?
}