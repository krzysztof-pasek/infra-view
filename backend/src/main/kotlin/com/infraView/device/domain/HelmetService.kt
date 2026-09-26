package com.infraView.device.domain

import com.infraView.telemetry.domain.ManageTelemetryUseCase
import com.infraView.telemetry.domain.Telemetry
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class HelmetService(
    private val devicePort: DevicePort,
    private val thermalFramePort: ThermalFramePort,
    private val telemetryUseCase: ManageTelemetryUseCase
) : ManageHelmetUseCase {

    override fun register(mac: String): Device {
        return devicePort.getByMac(mac) ?: devicePort.save(
            Device(
                mac = mac,
                uuid = UUID.randomUUID().toString(),
                registeredAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
        )
    }

    override fun ingestReadings(uuid: String, readings: List<SensorReading>): Telemetry? {
        val device = devicePort.getByUuid(uuid) ?: return null
        return telemetryUseCase.add(readings.toTelemetry(device.requireId()))
    }

    override fun ingestFrame(uuid: String, jpeg: ByteArray): Boolean {
        val device = devicePort.getByUuid(uuid) ?: return false
        thermalFramePort.saveLatest(device.requireId(), jpeg)
        return true
    }

    private fun Device.requireId(): Long = id ?: throw IllegalStateException("Device must have an ID")

    private fun List<SensorReading>.toTelemetry(deviceId: Long): Telemetry {
        val values = associate { (it.sensor to it.metric) to it.value }
        return Telemetry(
            deviceId = deviceId,
            recordedAt = OffsetDateTime.now(ZoneOffset.UTC),
            accelRawX = values["bmi160" to "ax"],
            accelRawY = values["bmi160" to "ay"],
            accelRawZ = values["bmi160" to "az"],
            gyroRawX = values["bmi160" to "gx"],
            gyroRawY = values["bmi160" to "gy"],
            gyroRawZ = values["bmi160" to "gz"],
            co2Ppm = values["sgp30" to "eco2"],
            tvocPpb = values["sgp30" to "tvoc"],
            gasVoltage = values["mics5524" to "voltage"],
            temperature = values["max6675" to "temperature"]
        )
    }
}
