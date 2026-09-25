package com.infraView.telemetry.domain

import org.springframework.stereotype.Service


@Service
class TelemetryService(
    private val telemetryPort: TelemetryPort
) : ManageTelemetryUseCase {
    override fun getById(id: Long): Telemetry? {
        return telemetryPort.getById(id)
    }

    override fun getByIncidentId(incidentId: Long): List<Telemetry> {
        return telemetryPort.getByIncidentId(incidentId)
    }

    override fun add(telemetry: Telemetry): Telemetry {
        // TODO(fall detection)
        return telemetryPort.save(telemetry)
    }

    override fun delete(id: Long) {
        telemetryPort.delete(id)
    }

}