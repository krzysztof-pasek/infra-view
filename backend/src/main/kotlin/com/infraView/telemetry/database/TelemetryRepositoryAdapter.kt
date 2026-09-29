package com.infraView.telemetry.database

import com.infraView.device.database.DeviceJpaEntity
import com.infraView.incident.database.IncidentJpaEntity
import com.infraView.telemetry.domain.Telemetry
import com.infraView.telemetry.domain.TelemetryRepository
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Limit
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class TelemetryRepositoryAdapter(
    private val jpaRepository: TelemetryJpaRepository,
    private val entityManager: EntityManager
) : TelemetryRepository {

    override fun getByIncidentId(incidentId: Long): List<Telemetry> {
        return jpaRepository.findAllByIncidentId(incidentId).map { it.toDomain() }
    }

    override fun getRecentByDeviceId(deviceId: Long, limit: Int): List<Telemetry> {
        return jpaRepository.findByDeviceIdOrderByRecordedAtDescIdDesc(deviceId, Limit.of(limit))
            .map { it.toDomain() }
            .asReversed()
    }

    override fun getById(id: Long): Telemetry? {
        return jpaRepository.findByIdOrNull(id)?.toDomain()
    }

    override fun save(telemetry: Telemetry): Telemetry {
        val incidentRef = telemetry.incidentId?.let { entityManager.getReference(IncidentJpaEntity::class.java, it) }
        val deviceRef = telemetry.deviceId?.let { entityManager.getReference(DeviceJpaEntity::class.java, it) }
        return jpaRepository.save(telemetry.toJpaEntity(incidentRef, deviceRef)).toDomain()
    }

    override fun delete(id: Long) {
        jpaRepository.deleteById(id)
    }
}


private fun Telemetry.toJpaEntity(incidentRef: IncidentJpaEntity?, deviceRef: DeviceJpaEntity?): TelemetryJpaEntity {
    return TelemetryJpaEntity(
        incident = incidentRef,
        device = deviceRef,
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
        gasVoltage = this.gasVoltage,
        co2Ppm = this.co2Ppm,
        tvocPpb = this.tvocPpb,
        motionState = this.motionState
    ).apply {
        this.id = this@toJpaEntity.id
    }
}

private fun TelemetryJpaEntity.toDomain(): Telemetry {
    return Telemetry(
        id = this.id,
        incidentId = this.incident?.id,
        deviceId = this.device?.id,
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
        gasVoltage = this.gasVoltage,
        co2Ppm = this.co2Ppm,
        tvocPpb = this.tvocPpb,
        motionState = this.motionState
    )
}