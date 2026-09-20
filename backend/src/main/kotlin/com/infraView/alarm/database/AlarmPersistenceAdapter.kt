package com.infraView.alarm.database

import com.infraView.alarm.domain.Alarm
import com.infraView.alarm.domain.AlarmPort
import com.infraView.incident.database.IncidentJpaEntity
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class AlarmPersistenceAdapter(
    private val alarmRepository: SpringDataAlarmRepository,
    private val entityManager: EntityManager
) : AlarmPort {

    override fun getById(id: Int): Alarm? {
        return alarmRepository.findByIdOrNull(id)?.toDomain()
    }

    override fun getAll(): List<Alarm> {
        return alarmRepository.findAll().map { it.toDomain() }
    }

    override fun save(alarm: Alarm): Alarm {
        val incidentRef = entityManager.getReference(IncidentJpaEntity::class.java, alarm.incidentId)
        val entityToSave = alarm.toJpaEntity(incidentRef)
        return alarmRepository.save(entityToSave).toDomain()
    }

    override fun deleteById(id: Int) {
        alarmRepository.deleteById(id)
    }
}

private fun Alarm.toJpaEntity(incidentRef: IncidentJpaEntity): AlarmJpaEntity {
    return AlarmJpaEntity(
        incident = incidentRef,
        alarmType = this.alarmType,
        triggeredAt = this.triggeredAt,
        resolvedAt = this.resolvedAt
    ).apply {
        this.id = this@toJpaEntity.id
    }
}

private fun AlarmJpaEntity.toDomain(): Alarm {
    return Alarm(
        id = this.id,
        alarmType = this.alarmType,
        triggeredAt = this.triggeredAt,
        resolvedAt = this.resolvedAt,
        incidentId = this.incident.id ?: throw IllegalStateException("Incident must have an ID")
    )
}
