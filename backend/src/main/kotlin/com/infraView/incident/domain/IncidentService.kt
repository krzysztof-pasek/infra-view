package com.infraView.incident.domain

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
class IncidentService(
    private val incidentPort: IncidentPort
) : ManageIncidentUseCase {

    private val log = LoggerFactory.getLogger(IncidentService::class.java)

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

    override fun getOrStartDeviceSession(deviceId: Long): Incident {
        incidentPort.getActiveByDeviceId(deviceId)?.let { return it }
        val startedAt = OffsetDateTime.now(ZoneOffset.UTC)
        val session = incidentPort.save(
            Incident(
                code = "HELMET-$deviceId-${startedAt.format(SESSION_CODE_TIME)}".take(20),
                startedAt = startedAt,
                status = StatusType.IN_PROGRESS,
                deviceId = deviceId
            )
        )
        log.info("Session started for helmet: deviceId={}, incidentId={}, code={}", deviceId, session.id, session.code)
        return session
    }

    override fun delete(id: Long) {
        incidentPort.delete(id)
    }

    companion object {
        private val SESSION_CODE_TIME = DateTimeFormatter.ofPattern("MMddHHmmss")
    }
}
