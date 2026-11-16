package com.infraView.telemetry.domain

import java.time.OffsetDateTime

interface TelemetryUseCase {
    fun getById(id: Long): Telemetry?
    fun getByIncidentId(incidentId: Long, limit: Int): List<Telemetry>
    fun getByIncidentIdBetween(incidentId: Long, from: OffsetDateTime, to: OffsetDateTime?): List<Telemetry>
    fun add(telemetry: Telemetry): Telemetry
    fun delete(id: Long)
}