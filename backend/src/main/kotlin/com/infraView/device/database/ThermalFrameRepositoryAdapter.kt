package com.infraView.device.database

import com.infraView.device.domain.ThermalFrame
import com.infraView.device.domain.ThermalFrameRepository
import com.infraView.incident.database.IncidentJpaEntity
import jakarta.persistence.EntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

@Component
class ThermalFrameRepositoryAdapter(
    private val jpaRepository: ThermalFrameJpaRepository,
    private val entityManager: EntityManager
) : ThermalFrameRepository {

    override fun save(frame: ThermalFrame): ThermalFrame {
        val entity = ThermalFrameJpaEntity(
            device = entityManager.getReference(DeviceJpaEntity::class.java, frame.deviceId),
            incident = entityManager.getReference(IncidentJpaEntity::class.java, frame.incidentId),
            capturedAt = frame.capturedAt,
            image = frame.image
        )
        return jpaRepository.save(entity).toDomain()
    }

    override fun getLastByIncidentId(incidentId: Long): ThermalFrame? {
        return jpaRepository.findFirstByIncidentIdOrderByCapturedAtDesc(incidentId)?.toDomain()
    }

    override fun getByIncidentIdAfter(incidentId: Long, after: OffsetDateTime, limit: Int): List<ThermalFrame> {
        return jpaRepository.findByIncidentIdAndCapturedAtAfterOrderByCapturedAt(incidentId, after, PageRequest.of(0, limit))
            .map { it.toDomain() }
    }

    private fun ThermalFrameJpaEntity.toDomain() = ThermalFrame(
        id = this.id,
        deviceId = this.device.id ?: throw IllegalStateException("Device must have an ID"),
        incidentId = this.incident.id ?: throw IllegalStateException("Incident must have an ID"),
        capturedAt = this.capturedAt,
        image = this.image
    )
}
