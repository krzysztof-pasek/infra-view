package com.infraView.alarm

import com.infraView.incident.IncidentEntity
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.Optional

@Service
class AlarmService(
    private val alarmRepository: AlarmRepository,
    private val entityManager: EntityManager
) {
    fun findAll(): List<AlarmDto> {
        return alarmRepository.findAll().map { it.toDto() }
    }

    fun findById(id: Long): AlarmDto? {
        return alarmRepository.findByIdOrNull(id)?.toDto()
    }

    fun add(dto: AlarmCreateDto): AlarmDto {
        val incidentRef = entityManager.getReference(IncidentEntity::class.java, dto.incidentId)

        val entity = AlarmEntity(
            incident = incidentRef,
            alarmType = dto.alarmType,
            triggeredAt = dto.triggeredAt
        )
        return alarmRepository.save(entity).toDto()
    }

    fun resolve(id: Long): AlarmDto {
        val entity = alarmRepository.findByIdOrNull(id) ?: throw RuntimeException("Alarm not found")
        entity.resolvedAt = OffsetDateTime.now()
        return alarmRepository.save(entity).toDto()
    }

    fun delete(id: Long) {
        alarmRepository.deleteById(id)
    }
}