package com.infraView.telemetry.domain

interface TelemetryPort {
    fun getByIncidentId(incidentId: Long): List<Telemetry>
    fun getById(id: Long): Telemetry?
    fun save(telemetry: Telemetry): Telemetry
    fun delete(id: Long)
}
