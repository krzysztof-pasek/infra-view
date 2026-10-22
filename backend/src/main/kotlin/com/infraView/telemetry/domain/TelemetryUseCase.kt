package com.infraView.telemetry.domain

interface TelemetryUseCase {
    fun getById(id: Long): Telemetry?
    fun getByIncidentId(incidentId: Long, limit: Int): List<Telemetry>
    fun add(telemetry: Telemetry): Telemetry
    fun delete(id: Long)
}