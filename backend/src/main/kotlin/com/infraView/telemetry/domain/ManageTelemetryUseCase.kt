package com.infraView.telemetry.domain

interface ManageTelemetryUseCase {
    fun getById(id: Long): Telemetry?
    fun getByIncidentId(incidentId: Long): List<Telemetry>
    fun add(telemetry: Telemetry): Telemetry
    fun delete(id: Long)
}