package com.infraView.telemetry.database

import org.springframework.data.jpa.repository.JpaRepository

interface SpringDataTelemetryRepository : JpaRepository<TelemetryJpaEntity, Long> {
    fun findAllByIncidentId(incidentId: Int): List<TelemetryJpaEntity>
    fun deleteAllByIncidentId(incidentId: Int)
}
