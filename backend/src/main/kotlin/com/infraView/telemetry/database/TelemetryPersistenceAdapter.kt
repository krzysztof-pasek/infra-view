package com.infraView.telemetry.database

import com.infraView.incident.database.IncidentJpaEntity
import com.infraView.telemetry.domain.Telemetry
import com.infraView.telemetry.domain.TelemetryPort
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class TelemetryPersistenceAdapter(
    private val telemetryRepository: SpringDataTelemetryRepository,
    private val entityManager: EntityManager
) : TelemetryPort {

    override fun getByIncidentId(incidentId: Long): List<Telemetry> {
        return telemetryRepository.findAllByIncidentId(incidentId).map { it.toDomain() }
    }

    override fun getById(id: Long): Telemetry? {
        return telemetryRepository.findByIdOrNull(id)?.toDomain()
    }

    override fun save(telemetry: Telemetry): Telemetry {
        val incidentRef = entityManager.getReference(IncidentJpaEntity::class.java, telemetry.incidentId)
        return telemetryRepository.save(telemetry.toJpaEntity(incidentRef)).toDomain()
    }

    override fun delete(id: Long) {
        telemetryRepository.deleteById(id)
    }
}


private fun Telemetry.toJpaEntity(incidentRef: IncidentJpaEntity): TelemetryJpaEntity {
    return TelemetryJpaEntity(
        incident = incidentRef,
        recordedAt = this.recordedAt,
        accelRawX = this.accelRawX,
        accelRawY = this.accelRawY,
        accelRawZ = this.accelRawZ,
        accelFiltX = this.accelFiltX,
        accelFiltY = this.accelFiltY,
        accelFiltZ = this.accelFiltZ,
        gyroRawX = this.gyroRawX,
        gyroRawY = this.gyroRawY,
        gyroRawZ = this.gyroRawZ,
        gyroFiltX = this.gyroFiltX,
        gyroFiltY = this.gyroFiltY,
        gyroFiltZ = this.gyroFiltZ,
        temperature = this.temperature,
        gasPpm = this.gasPpm,
        co2Ppm = this.co2Ppm,
        motionState = this.motionState
    ).apply {
        this.id = this@toJpaEntity.id
    }
}

private fun TelemetryJpaEntity.toDomain(): Telemetry {
    return Telemetry(
        id = this.id,
        incidentId = this.incident.id ?: throw IllegalStateException("Incident must have an ID"),
        recordedAt = this.recordedAt,
        accelRawX = this.accelRawX,
        accelRawY = this.accelRawY,
        accelRawZ = this.accelRawZ,
        accelFiltX = this.accelFiltX,
        accelFiltY = this.accelFiltY,
        accelFiltZ = this.accelFiltZ,
        gyroRawX = this.gyroRawX,
        gyroRawY = this.gyroRawY,
        gyroRawZ = this.gyroRawZ,
        gyroFiltX = this.gyroFiltX,
        gyroFiltY = this.gyroFiltY,
        gyroFiltZ = this.gyroFiltZ,
        temperature = this.temperature,
        gasPpm = this.gasPpm,
        co2Ppm = this.co2Ppm,
        motionState = this.motionState
    )
}