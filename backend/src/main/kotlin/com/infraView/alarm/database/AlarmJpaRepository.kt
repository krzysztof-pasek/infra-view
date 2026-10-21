package com.infraView.alarm.database

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AlarmJpaRepository : JpaRepository<AlarmJpaEntity, Long> {
    fun findAllByIncidentIdAndResolvedAtIsNull(incidentId: Long): List<AlarmJpaEntity>
}
