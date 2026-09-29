package com.infraView.telemetry.domain

import java.time.OffsetDateTime

data class Telemetry(
    val id: Long? = null,
    val incidentId: Long? = null,
    val deviceId: Long? = null,
    val recordedAt: OffsetDateTime,
    val accelRawX: Double? = null,
    val accelRawY: Double? = null,
    val accelRawZ: Double? = null,
    val gyroRawX: Double? = null,
    val gyroRawY: Double? = null,
    val gyroRawZ: Double? = null,
    val temperature: Double? = null,
    val gasPpm: Double? = null,
    val gasVoltage: Double? = null,
    val co2Ppm: Double? = null,
    val tvocPpb: Double? = null,
    val motionState: String? = null
)