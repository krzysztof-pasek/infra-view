package com.infraView.incident.domain


interface IncidentPort {
    fun getById(id: Long): Incident?
    fun getAll(): List<Incident>
    fun save(incident: Incident): Incident
    fun deleteById(id: Long)
}