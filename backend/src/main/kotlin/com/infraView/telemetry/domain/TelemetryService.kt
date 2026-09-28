package com.infraView.telemetry.domain

import org.springframework.stereotype.Service


@Service
class TelemetryService(
    private val telemetryRepository: TelemetryRepository
) : TelemetryUseCase {
    override fun getById(id: Long): Telemetry? {
        return telemetryRepository.getById(id)
    }

    override fun getByIncidentId(incidentId: Long): List<Telemetry> {
        return telemetryRepository.getByIncidentId(incidentId)
    }

    override fun add(telemetry: Telemetry): Telemetry {
        // TODO(fall detection)
        return telemetryRepository.save(telemetry)
    }

    override fun delete(id: Long) {
        telemetryRepository.delete(id)
    }

}