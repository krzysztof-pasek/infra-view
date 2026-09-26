package com.infraView.telemetry.rest

import com.infraView.telemetry.domain.Telemetry
import java.time.OffsetDateTime

data class TelemetryDto(
    val id: Long?,
    val incidentId: Long?,
    val deviceId: Long?,
    val recordedAt: OffsetDateTime,
    val accelRawX: Double?,
    val accelRawY: Double?,
    val accelRawZ: Double?,
    val accelFiltX: Double?,
    val accelFiltY: Double?,
    val accelFiltZ: Double?,
    val gyroRawX: Double?,
    val gyroRawY: Double?,
    val gyroRawZ: Double?,
    val gyroFiltX: Double?,
    val gyroFiltY: Double?,
    val gyroFiltZ: Double?,
    val temperature: Double?,
    val gasPpm: Double?,
    val gasVoltage: Double?,
    val co2Ppm: Double?,
    val tvocPpb: Double?,
    val motionState: String?
)

data class TelemetryCreateDto(
    val incidentId: Long,
    val recordedAt: OffsetDateTime,
    val accelRawX: Double? = null,
    val accelRawY: Double? = null,
    val accelRawZ: Double? = null,
    val accelFiltX: Double? = null,
    val accelFiltY: Double? = null,
    val accelFiltZ: Double? = null,
    val gyroRawX: Double? = null,
    val gyroRawY: Double? = null,
    val gyroRawZ: Double? = null,
    val gyroFiltX: Double? = null,
    val gyroFiltY: Double? = null,
    val gyroFiltZ: Double? = null,
    val temperature: Double? = null,
    val gasPpm: Double? = null,
    val gasVoltage: Double? = null,
    val co2Ppm: Double? = null,
    val tvocPpb: Double? = null,
    val motionState: String? = null
) {
    fun toDomain() = Telemetry(
        incidentId = this.incidentId,
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

fun Telemetry.toDto() = TelemetryDto(
    id = this.id,
    incidentId = this.incidentId,
    deviceId = this.deviceId,
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
