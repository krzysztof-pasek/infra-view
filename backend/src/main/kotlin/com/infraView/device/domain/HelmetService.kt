package com.infraView.device.domain

import com.infraView.incident.domain.ManageIncidentUseCase
import com.infraView.telemetry.domain.ManageTelemetryUseCase
import com.infraView.telemetry.domain.Telemetry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class HelmetService(
    private val devicePort: DevicePort,
    private val thermalFramePort: ThermalFramePort,
    private val telemetryUseCase: ManageTelemetryUseCase,
    private val incidentUseCase: ManageIncidentUseCase
) : ManageHelmetUseCase {

    private val log = LoggerFactory.getLogger(HelmetService::class.java)
    private val streamingDevices = ConcurrentHashMap.newKeySet<Long>()

    override fun register(mac: String): Device {
        devicePort.getByMac(mac)?.let {
            log.info("Helmet reconnected: mac={}, deviceId={}", it.mac, it.id)
            return it
        }
        val device = devicePort.save(
            Device(
                mac = mac,
                uuid = UUID.randomUUID().toString(),
                registeredAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
        )
        log.info("New helmet registered: mac={}, deviceId={}", device.mac, device.id)
        return device
    }

    override fun ingestReadings(uuid: String, readings: List<SensorReading>): Telemetry? {
        val device = devicePort.getByUuid(uuid)
        if (device == null) {
            log.warn("Rejected {} readings from unknown device", readings.size)
            return null
        }
        val deviceId = device.requireId()
        val session = incidentUseCase.getOrStartDeviceSession(deviceId)
        val saved = telemetryUseCase.add(readings.toTelemetry(deviceId, session.id))
        log.info(
            "Telemetry received: deviceId={}, incidentId={}, telemetryId={}, readings={}, temperature={} C, eco2={} ppm, tvoc={} ppb, gasVoltage={} V, accel=({}, {}, {}) g, gyro=({}, {}, {}) dps",
            deviceId, saved.incidentId, saved.id, readings.size, saved.temperature, saved.co2Ppm, saved.tvocPpb, saved.gasVoltage,
            saved.accelRawX, saved.accelRawY, saved.accelRawZ, saved.gyroRawX, saved.gyroRawY, saved.gyroRawZ
        )
        val failed = readings.filter { it.value == null }.map { "${it.sensor}.${it.metric}" }
        if (failed.isNotEmpty()) {
            log.warn("Failed sensor readings: deviceId={}, metrics={}", device.id, failed)
        }
        val unknown = readings.filter { (it.sensor to it.metric) !in KNOWN_METRICS }.map { "${it.sensor}.${it.metric}" }
        if (unknown.isNotEmpty()) {
            log.warn("Ignored unknown sensor metrics: deviceId={}, metrics={}", device.id, unknown)
        }
        return saved
    }

    override fun ingestFrame(uuid: String, jpeg: ByteArray): Boolean {
        val device = devicePort.getByUuid(uuid)
        if (device == null) {
            log.warn("Rejected thermal frame from unknown device")
            return false
        }
        val deviceId = device.requireId()
        thermalFramePort.saveLatest(deviceId, jpeg)
        if (streamingDevices.add(deviceId)) {
            log.info("Thermal stream started: deviceId={}", deviceId)
        }
        log.debug("Thermal frame received: deviceId={}, bytes={}", deviceId, jpeg.size)
        return true
    }

    private fun Device.requireId(): Long = id ?: throw IllegalStateException("Device must have an ID")

    private fun List<SensorReading>.toTelemetry(deviceId: Long, incidentId: Long?): Telemetry {
        val values = associate { (it.sensor to it.metric) to it.value }
        return Telemetry(
            incidentId = incidentId,
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

    companion object {
        private val KNOWN_METRICS = setOf(
            "bmi160" to "ax", "bmi160" to "ay", "bmi160" to "az",
            "bmi160" to "gx", "bmi160" to "gy", "bmi160" to "gz",
            "sgp30" to "eco2", "sgp30" to "tvoc",
            "mics5524" to "voltage",
            "max6675" to "temperature"
        )
    }
}
