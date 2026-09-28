package com.infraView.device.database

import com.infraView.device.domain.ThermalFrame
import com.infraView.device.domain.ThermalFramePort
import com.infraView.incident.database.IncidentJpaEntity
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

@Component
class ThermalFramePersistenceAdapter(
    private val thermalFrameRepository: SpringDataThermalFrameRepository,
    private val entityManager: EntityManager
) : ThermalFramePort {

    override fun save(frame: ThermalFrame): ThermalFrame {
        val entity = ThermalFrameJpaEntity(
            device = entityManager.getReference(DeviceJpaEntity::class.java, frame.deviceId),
            incident = entityManager.getReference(IncidentJpaEntity::class.java, frame.incidentId),
            capturedAt = frame.capturedAt,
            image = frame.image
        )
        return thermalFrameRepository.save(entity).toDomain()
    }

    private fun ThermalFrameJpaEntity.toDomain() = ThermalFrame(
        id = this.id,
        deviceId = this.device.id ?: throw IllegalStateException("Device must have an ID"),
        incidentId = this.incident.id ?: throw IllegalStateException("Incident must have an ID"),
        capturedAt = this.capturedAt,
        image = this.image
    )
}
