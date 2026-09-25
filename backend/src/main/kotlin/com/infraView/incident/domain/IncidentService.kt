package com.infraView.incident.domain

import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class IncidentService(
    private val incidentPort: IncidentPort
) : ManageIncidentUseCase {

    override fun getAll(): List<Incident> {
        return incidentPort.getAll()
    }

    override fun getById(id: Long): Incident? {
        return incidentPort.getById(id)
    }

    override fun save(incident: Incident): Incident {
        return incidentPort.save(incident)
    }

    override fun endIncident(id: Long): Incident? {
        val incident = incidentPort.getById(id) ?: return null
        if (incident.status == StatusType.RESOLVED) return incident
        return incidentPort.save(incident.copy(endedAt = OffsetDateTime.now(ZoneOffset.UTC), status = StatusType.RESOLVED))
    }

    override fun delete(id: Long) {
        incidentPort.delete(id)
    }
}
