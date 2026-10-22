package com.infraView.telemetry.database

import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository

interface TelemetryJpaRepository : JpaRepository<TelemetryJpaEntity, Long> {
    fun findByIncidentIdOrderByRecordedAtDescIdDesc(incidentId: Long, limit: Limit): List<TelemetryJpaEntity>
    fun findByDeviceIdOrderByRecordedAtDescIdDesc(deviceId: Long, limit: Limit): List<TelemetryJpaEntity>
}
