package com.infraView.device.domain

import com.infraView.incident.domain.Incident
import com.infraView.incident.domain.IncidentStatus
import com.infraView.incident.domain.IncidentUseCase
import com.infraView.telemetry.domain.TelemetryUseCase
import com.infraView.telemetry.domain.Telemetry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Service
class DeviceService(
    private val deviceRepository: DeviceRepository,
    private val thermalFrameRepository: ThermalFrameRepository,
    private val telemetryUseCase: TelemetryUseCase,
    private val incidentUseCase: IncidentUseCase
) : DeviceUseCase {

    private val log = LoggerFactory.getLogger(DeviceService::class.java)
    private val streamingDevices = ConcurrentHashMap.newKeySet<Long>()
    private val liveLock = ReentrantLock()
    private val newLiveFrame = liveLock.newCondition()
    private val liveFrames = HashMap<Long, LiveFrame>()
    private var liveSequence = 0L

    override fun register(mac: String): Device {
        deviceRepository.getByMac(mac)?.let {
            log.info("Helmet reconnected: mac={}, deviceId={}", it.mac, it.id)
            return it
        }
        val device = deviceRepository.save(
            Device(
                mac = mac,
                uuid = UUID.randomUUID().toString(),
                registeredAt = OffsetDateTime.now(ZoneOffset.UTC)
            )
        )
        log.info("New helmet registered: mac={}, deviceId={}", device.mac, device.id)
        return device
    }

    override fun ingestReadings(uuid: String, readings: Map<String, Double?>): Telemetry? {
        val device = deviceRepository.getByUuid(uuid)
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
        val failed = readings.filterValues { it == null }.keys
        if (failed.isNotEmpty()) {
            log.warn("Failed sensor readings: deviceId={}, metrics={}", device.id, failed)
        }
        val unknown = readings.keys - KNOWN_METRICS
        if (unknown.isNotEmpty()) {
            log.warn("Ignored unknown sensor metrics: deviceId={}, metrics={}", device.id, unknown)
        }
        return saved
    }

    override fun ingestFrame(uuid: String, jpeg: ByteArray): Boolean {
        val device = deviceRepository.getByUuid(uuid)
        if (device == null) {
            log.warn("Rejected thermal frame from unknown device")
            return false
        }
        val deviceId = device.requireId()
        val session = incidentUseCase.getOrStartDeviceSession(deviceId)
        publishLiveFrame(deviceId, session.requireId(), jpeg)
        val saved = thermalFrameRepository.save(
            ThermalFrame(
                deviceId = deviceId,
                incidentId = session.requireId(),
                capturedAt = OffsetDateTime.now(ZoneOffset.UTC),
                image = jpeg
            )
        )
        if (streamingDevices.add(deviceId)) {
            log.info("Thermal stream started: deviceId={}, incidentId={}", deviceId, saved.incidentId)
        }
        log.debug("Thermal frame received: deviceId={}, frameId={}, bytes={}", deviceId, saved.id, jpeg.size)
        return true
    }

    override fun getLiveDeviceId(incidentId: Long): Long? {
        val incident = incidentUseCase.getById(incidentId) ?: return null
        if (incident.status != IncidentStatus.IN_PROGRESS) return null
        return incident.deviceId
    }

    override fun awaitLiveFrame(deviceId: Long, afterSequence: Long, timeout: Duration): LiveFrame? {
        var remainingNanos = timeout.toNanos()
        liveLock.withLock {
            while (true) {
                val frame = liveFrames[deviceId]
                if (frame != null && frame.sequence > afterSequence) return frame
                if (remainingNanos <= 0) return null
                remainingNanos = newLiveFrame.awaitNanos(remainingNanos)
            }
        }
    }

    override fun getLastFrameTime(incidentId: Long): OffsetDateTime? {
        return thermalFrameRepository.getLastByIncidentId(incidentId)?.capturedAt
    }

    override fun getFramesAfter(incidentId: Long, after: OffsetDateTime, limit: Int): List<ThermalFrame> {
        return thermalFrameRepository.getByIncidentIdAfter(incidentId, after, limit)
    }

    private fun publishLiveFrame(deviceId: Long, incidentId: Long, jpeg: ByteArray) {
        liveLock.withLock {
            liveFrames[deviceId] = LiveFrame(sequence = ++liveSequence, incidentId = incidentId, image = jpeg)
            newLiveFrame.signalAll()
        }
    }

    private fun Device.requireId(): Long = id ?: throw IllegalStateException("Device must have an ID")

    private fun Incident.requireId(): Long = id ?: throw IllegalStateException("Incident must have an ID")

    private fun Map<String, Double?>.toTelemetry(deviceId: Long, incidentId: Long?): Telemetry {
        return Telemetry(
            incidentId = incidentId,
            deviceId = deviceId,
            recordedAt = OffsetDateTime.now(ZoneOffset.UTC),
            accelRawX = this["ax"],
            accelRawY = this["ay"],
            accelRawZ = this["az"],
            gyroRawX = this["gx"],
            gyroRawY = this["gy"],
            gyroRawZ = this["gz"],
            co2Ppm = this["eco2"],
            tvocPpb = this["tvoc"],
            gasVoltage = this["voltage"],
            temperature = this["temperature"]
        )
    }

    companion object {
        private val KNOWN_METRICS = setOf(
            "ax", "ay", "az",
            "gx", "gy", "gz",
            "eco2", "tvoc",
            "voltage",
            "temperature"
        )
    }
}
