package com.infraView.incident.domain


interface IncidentPort {
    fun getById(id: Int): Incident?
    fun getAll(): List<Incident>
    fun save(incident: Incident): Incident
    fun deleteById(id: Int)
}