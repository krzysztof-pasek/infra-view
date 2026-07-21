package com.infraView.telemetry

import com.infraView.incident.IncidentEntity
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class TelemetryService (
    private val telemetryRepository: TelemetryRepository,
    private val entityManager: EntityManager
) {

    fun getAll() : List<TelemetryDto> {
        return telemetryRepository.findAll().map { it.toDto() }
    }

    fun getById(id: Long): TelemetryDto? {
        return telemetryRepository.findByIdOrNull(id)?.toDto()
    }

    fun add(dto: TelemetryCreateDto) : TelemetryDto {
        val incidentRef = entityManager.getReference(IncidentEntity::class.java, dto.incidentId)
        
        val entity = TelemetryEntity(
            incident = incidentRef,
            recordedAt = dto.recordedAt,
            accelRawX = dto.accelRawX,
            accelRawY = dto.accelRawY,
            accelRawZ = dto.accelRawZ,
            accelFiltX = dto.accelFiltX,
            accelFiltY = dto.accelFiltY,
            accelFiltZ = dto.accelFiltZ,
            gyroRawX = dto.gyroRawX,
            gyroRawY = dto.gyroRawY,
            gyroRawZ = dto.gyroRawZ,
            gyroFiltX = dto.gyroFiltX,
            gyroFiltY = dto.gyroFiltY,
            gyroFiltZ = dto.gyroFiltZ,
            temperature = dto.temperature,
            gasPpm = dto.gasPpm,
            co2Ppm = dto.co2Ppm,
            motionState = dto.motionState
        )
        return telemetryRepository.save(entity).toDto()
    }

    fun getByIncidentId(incidentId: Int): List<TelemetryDto> {
        return telemetryRepository.findAllByIncidentId(incidentId).map { it.toDto() }
    }


    fun delete(id: Long) {
        telemetryRepository.deleteById(id)
    }
}