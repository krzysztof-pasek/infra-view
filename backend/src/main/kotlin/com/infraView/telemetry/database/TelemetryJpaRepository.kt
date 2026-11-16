package com.infraView.telemetry.database

import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime

interface TelemetryJpaRepository : JpaRepository<TelemetryJpaEntity, Long> {
    fun findByIncidentIdOrderByRecordedAtDescIdDesc(incidentId: Long, limit: Limit): List<TelemetryJpaEntity>
    fun findByIncidentIdAndRecordedAtGreaterThanEqualOrderByRecordedAtAscIdAsc(incidentId: Long, from: OffsetDateTime): List<TelemetryJpaEntity>
    fun findByIncidentIdAndRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtAscIdAsc(incidentId: Long, from: OffsetDateTime, to: OffsetDateTime): List<TelemetryJpaEntity>
    fun findByDeviceIdOrderByRecordedAtDescIdDesc(deviceId: Long, limit: Limit): List<TelemetryJpaEntity>
}
