package com.infraView.incident.domain

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
class IncidentService(
    private val incidentRepository: IncidentRepository
) : IncidentUseCase {

    private val log = LoggerFactory.getLogger(IncidentService::class.java)

    override fun getAll(): List<Incident> {
        return incidentRepository.getAll()
    }

    override fun getById(id: Long): Incident? {
        return incidentRepository.getById(id)
    }

    override fun save(incident: Incident): Incident {
        return incidentRepository.save(incident)
    }

    override fun endIncident(id: Long): Incident? {
        val incident = incidentRepository.getById(id) ?: return null
        if (incident.status == IncidentStatus.RESOLVED) return incident
        return incidentRepository.save(incident.copy(endedAt = OffsetDateTime.now(ZoneOffset.UTC), status = IncidentStatus.RESOLVED))
    }

    override fun getOrStartDeviceSession(deviceId: Long): Incident {
        incidentRepository.getActiveByDeviceId(deviceId)?.let { return it }
        val startedAt = OffsetDateTime.now(ZoneOffset.UTC)
        val session = incidentRepository.save(
            Incident(
                code = "HELMET-$deviceId-${startedAt.format(SESSION_CODE_TIME)}".take(20),
                startedAt = startedAt,
                status = IncidentStatus.IN_PROGRESS,
                deviceId = deviceId
            )
        )
        log.info("Session started for helmet: deviceId={}, incidentId={}, code={}", deviceId, session.id, session.code)
        return session
    }

    override fun delete(id: Long) {
        incidentRepository.delete(id)
    }

    companion object {
        private val SESSION_CODE_TIME = DateTimeFormatter.ofPattern("MMddHHmmss")
    }
}
