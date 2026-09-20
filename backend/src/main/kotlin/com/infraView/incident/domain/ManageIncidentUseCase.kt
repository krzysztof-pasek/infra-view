package com.infraView.incident.domain


interface ManageIncidentUseCase {
    fun getAll(): List<Incident>
    fun getById(id: Long): Incident?
    fun save(incident: Incident): Incident
    fun endIncident(incidentId: Long): Incident?
    fun delete(incidentId: Long)
}