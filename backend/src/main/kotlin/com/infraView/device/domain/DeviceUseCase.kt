package com.infraView.device.domain

import com.infraView.alarm.domain.Alarm
import com.infraView.telemetry.domain.Telemetry
import java.time.Duration
import java.time.OffsetDateTime

interface DeviceUseCase {
    fun register(mac: String): Device
    fun ingestReadings(uuid: String, readings: Map<String, Double?>): Telemetry?
    fun ingestFrame(uuid: String, jpeg: ByteArray): Boolean
    fun triggerSos(uuid: String): Alarm?
    fun getLiveDeviceId(incidentId: Long): Long?
    fun awaitLiveFrame(deviceId: Long, afterSequence: Long, timeout: Duration): LiveFrame?
    fun getLastFrameTime(incidentId: Long): OffsetDateTime?
    fun getFrameAt(incidentId: Long, at: OffsetDateTime): ThermalFrame?
    fun getFramesAfter(incidentId: Long, after: OffsetDateTime, limit: Int): List<ThermalFrame>
}
