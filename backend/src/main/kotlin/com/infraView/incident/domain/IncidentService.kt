package com.infraView.incident.domain

import com.infraView.incident.StatusType
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

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

    override fun endIncident(incidentId: Long): Incident? {
        val incident = incidentPort.getById(incidentId) ?: throw RuntimeException("Incident not found")
        incident.endedAt = OffsetDateTime.now()
        incident.status = StatusType.RESOLVED
        return incidentPort.save(incident)
    }

    override fun delete(incidentId: Long) {
        incidentPort.deleteById(incidentId)
    }
}