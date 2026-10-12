package com.infraView.device.domain

import com.infraView.telemetry.domain.Telemetry
import java.time.Duration

interface DeviceUseCase {
    fun register(mac: String): Device
    fun ingestReadings(uuid: String, readings: List<SensorReading>): Telemetry?
    fun ingestFrame(uuid: String, jpeg: ByteArray): Boolean
    fun getLiveDeviceId(incidentId: Long): Long?
    fun awaitLiveFrame(deviceId: Long, afterSequence: Long, timeout: Duration): LiveFrame?
}
