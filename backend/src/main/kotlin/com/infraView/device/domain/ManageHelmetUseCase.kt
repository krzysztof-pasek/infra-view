package com.infraView.device.domain

import com.infraView.telemetry.domain.Telemetry

interface ManageHelmetUseCase {
    fun register(mac: String): Device
    fun ingestReadings(uuid: String, readings: List<SensorReading>): Telemetry?
    fun ingestFrame(uuid: String, jpeg: ByteArray): Boolean
}
