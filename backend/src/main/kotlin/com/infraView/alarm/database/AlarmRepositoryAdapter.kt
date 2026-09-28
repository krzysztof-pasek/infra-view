package com.infraView.alarm.database

import com.infraView.alarm.domain.Alarm
import com.infraView.alarm.domain.AlarmRepository
import com.infraView.incident.database.IncidentJpaEntity
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class AlarmRepositoryAdapter(
    private val jpaRepository: AlarmJpaRepository,
    private val entityManager: EntityManager
) : AlarmRepository {

    override fun getById(id: Long): Alarm? {
        return jpaRepository.findByIdOrNull(id)?.toDomain()
    }

    override fun getAll(): List<Alarm> {
        return jpaRepository.findAll().map { it.toDomain() }
    }

    override fun save(alarm: Alarm): Alarm {
        val incidentRef = entityManager.getReference(IncidentJpaEntity::class.java, alarm.incidentId)
        val entityToSave = alarm.toJpaEntity(incidentRef)
        return jpaRepository.save(entityToSave).toDomain()
    }

    override fun delete(id: Long) {
        jpaRepository.deleteById(id)
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
