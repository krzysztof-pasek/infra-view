package com.infraView.incident.domain


interface ManageIncidentUseCase {
    fun getAll(): List<Incident>
    fun getById(id: Int): Incident?
    fun save(incident: Incident): Incident
    fun endIncident(incidentId: Int): Incident?
    fun delete(incidentId: Int)
}