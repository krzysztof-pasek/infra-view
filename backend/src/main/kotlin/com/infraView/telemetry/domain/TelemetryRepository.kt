package com.infraView.telemetry.domain

interface TelemetryRepository {
    fun getRecentByIncidentId(incidentId: Long, limit: Int): List<Telemetry>
    fun getRecentByDeviceId(deviceId: Long, limit: Int): List<Telemetry>
    fun getById(id: Long): Telemetry?
    fun save(telemetry: Telemetry): Telemetry
    fun delete(id: Long)
}
