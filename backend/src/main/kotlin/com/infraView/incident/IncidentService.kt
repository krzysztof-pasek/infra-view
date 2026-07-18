package com.infraView.incident

import com.infraView.alarm.AlarmCreateDto
import com.infraView.alarm.AlarmDto
import com.infraView.alarm.AlarmEntity
import com.infraView.alarm.toDto
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.OffsetDateTime


@Service
class IncidentService(
    private val incidentRepository: IncidentRepository,
) {

    fun getAll(): List<IncidentDto> {
        return incidentRepository.findAll().map { it.toDto() }
    }

    fun getById(id: Int): IncidentDto? {
        return incidentRepository.findByIdOrNull(id)?.toDto()
    }

    fun add(dto: IncidentCreateDto): IncidentDto {
        val entity = IncidentEntity(
            code = dto.code,
            firefighterName = dto.firefighterName,
            description = dto.description,
            location = dto.location,
            startedAt = dto.startedAt,
            endedAt = dto.endedAt,
            status = dto.status
        )
        return incidentRepository.save(entity).toDto()
    }

    fun endIncident(id: Int): IncidentDto {
        val entity = incidentRepository.findByIdOrNull(id) ?: throw RuntimeException("Incident not found")
        entity.endedAt = OffsetDateTime.now()
        entity.status = StatusType.RESOLVED

        return incidentRepository.save(entity).toDto()
    }

    fun delete(id: Int) {
        incidentRepository.deleteById(id)
    }
}