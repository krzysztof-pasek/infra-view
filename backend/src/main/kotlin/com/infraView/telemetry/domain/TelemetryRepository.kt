package com.infraView.telemetry.domain

interface TelemetryRepository {
    fun getByIncidentId(incidentId: Long): List<Telemetry>

    /** The latest [limit] rows of the device, oldest first. */
    fun getRecentByDeviceId(deviceId: Long, limit: Int): List<Telemetry>
    fun getById(id: Long): Telemetry?
    fun save(telemetry: Telemetry): Telemetry
    fun delete(id: Long)
}
