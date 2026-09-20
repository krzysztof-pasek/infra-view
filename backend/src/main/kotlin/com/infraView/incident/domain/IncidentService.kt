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
        // Wyciągamy czysty obiekt domenowy przez Port (zamiast starego repozytorium)
        val incident = incidentPort.getById(incidentId) ?: throw RuntimeException("Incident not found")
        
        // Zmieniamy stan naszej czystej domeny (bez DTO i Encji!)
        incident.endedAt = OffsetDateTime.now()
        incident.status = StatusType.RESOLVED

        // Zapisujemy przez port wyjściowy i zwracamy zaktualizowaną domenę
        return incidentPort.save(incident)
    }

    override fun delete(incidentId: Long) {
        incidentPort.deleteById(incidentId)
    }
}