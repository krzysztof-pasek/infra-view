package com.infraView.telemetry.domain

interface TelemetryPort {
    fun getAllByIncidentId(incidentId: Long): List<Telemetry>
    fun getById(id: Long): Telemetry?
    fun save(telemetry: Telemetry): Telemetry
    fun deleteById(id: Long)
    fun deleteAllByIncidentId(incidentId: Long)
}