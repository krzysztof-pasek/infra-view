package com.infraView.incident.domain

interface IncidentUseCase {
    fun getAll(): List<Incident>
    fun getById(id: Long): Incident?
    fun save(incident: Incident): Incident
    fun endIncident(id: Long): Incident?
    fun rename(id: Long, code: String): Incident?
    fun getOrStartDeviceSession(deviceId: Long): Incident
    fun delete(id: Long)
}
