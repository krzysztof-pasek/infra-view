package com.infraView.incident.database

import com.infraView.incident.domain.Incident
import com.infraView.incident.domain.IncidentPort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class IncidentPersistenceAdapter(
    private val incidentRepository: SpringDataIncidentRepository
) : IncidentPort {

    override fun getById(id: Int): Incident? {
        return incidentRepository.findByIdOrNull(id)?.toDomain()
    }

    override fun getAll(): List<Incident> {
        return incidentRepository.findAll().map { it.toDomain() }
    }

    override fun save(incident: Incident): Incident {
        val entityToSave = incident.toJpaEntity()
        val savedEntity = incidentRepository.save(entityToSave)
        return savedEntity.toDomain()
    }

    override fun deleteById(id: Int) {
        incidentRepository.deleteById(id)
    }
}


private fun Incident.toJpaEntity(): IncidentJpaEntity {
    return IncidentJpaEntity(
        code = this.code,
        firefighterName = this.firefighterName,
        description = this.description,
        location = this.location,
        startedAt = this.startedAt,
        endedAt = this.endedAt,
        status = this.status
    ).apply {
        this.id = this@toJpaEntity.id
    }
}

private fun IncidentJpaEntity.toDomain(): Incident {
    return Incident(
        id = this.id,
        code = this.code,
        firefighterName = this.firefighterName,
        description = this.description,
        location = this.location,
        startedAt = this.startedAt,
        endedAt = this.endedAt,
        status = this.status
    )
}