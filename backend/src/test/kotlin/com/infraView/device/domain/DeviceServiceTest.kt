package com.infraView.device.domain

import com.infraView.alarm.domain.Alarm
import com.infraView.alarm.domain.AlarmType
import com.infraView.alarm.domain.AlarmUseCase
import com.infraView.incident.domain.Incident
import com.infraView.incident.domain.IncidentUseCase
import com.infraView.incident.domain.IncidentStatus
import com.infraView.telemetry.domain.TelemetryUseCase
import com.infraView.telemetry.domain.Telemetry
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeviceServiceTest {

    private val deviceRepository = mockk<DeviceRepository>()
    private val thermalFrameRepository = mockk<ThermalFrameRepository>()
    private val telemetryUseCase = mockk<TelemetryUseCase>()
    private val incidentUseCase = mockk<IncidentUseCase>()
    private val alarmUseCase = mockk<AlarmUseCase>()
    private val service = DeviceService(deviceRepository, thermalFrameRepository, telemetryUseCase, incidentUseCase, alarmUseCase)

    private val mac = "d8:3a:dd:ed:3f:42"
    private val uuid = "689d8e89-2503-4739-9dd8-c93bc9d7c14d"
    private val device = Device(id = 1L, mac = mac, uuid = uuid, registeredAt = OffsetDateTime.parse("2026-09-26T10:00:00Z"))
    private val session = Incident(
        id = 7L,
        code = "HELMET-1-0926100000",
        startedAt = OffsetDateTime.parse("2026-09-26T10:00:00Z"),
        status = IncidentStatus.IN_PROGRESS,
        deviceId = 1L
    )

    private val fullPacket = mapOf(
        "ax" to -0.037,
        "ay" to 0.509,
        "az" to 0.904,
        "eco2" to 400.0,
        "gx" to -0.899,
        "gy" to 0.655,
        "gz" to -0.107,
        "temperature" to 23.75,
        "tvoc" to 0.0,
        "voltage" to 0.267
    )

    @Test
    fun `should return existing device for known mac`() {
        every { deviceRepository.getByMac(mac) } returns device

        assertEquals(device, service.register(mac))
        verify(exactly = 0) { deviceRepository.save(any()) }
    }

    @Test
    fun `should create device with new uuid for unknown mac`() {
        val saved = slot<Device>()
        every { deviceRepository.getByMac(mac) } returns null
        every { deviceRepository.save(capture(saved)) } answers { saved.captured.copy(id = 2L) }

        val result = service.register(mac)

        assertEquals(2L, result.id)
        assertEquals(mac, saved.captured.mac)
        UUID.fromString(saved.captured.uuid)
        assertEquals(ZoneOffset.UTC, saved.captured.registeredAt.offset)
    }

    @Test
    fun `should map every sensor reading to its telemetry field`() {
        val saved = slot<Telemetry>()
        every { deviceRepository.getByUuid(uuid) } returns device
        every { incidentUseCase.getOrStartDeviceSession(1L) } returns session
        every { telemetryUseCase.add(capture(saved)) } answers { saved.captured.copy(id = 10L) }

        val result = service.ingestReadings(uuid, fullPacket)

        assertEquals(10L, result?.id)
        val telemetry = saved.captured
        assertEquals(
            Telemetry(
                incidentId = 7L,
                deviceId = 1L,
                recordedAt = telemetry.recordedAt,
                accelRawX = -0.037,
                accelRawY = 0.509,
                accelRawZ = 0.904,
                gyroRawX = -0.899,
                gyroRawY = 0.655,
                gyroRawZ = -0.107,
                co2Ppm = 400.0,
                tvocPpb = 0.0,
                gasVoltage = 0.267,
                temperature = 23.75
            ),
            telemetry
        )
        assertEquals(ZoneOffset.UTC, telemetry.recordedAt.offset)
    }

    @Test
    fun `should keep failed and missing sensors as null and ignore unknown ones`() {
        val saved = slot<Telemetry>()
        every { deviceRepository.getByUuid(uuid) } returns device
        every { incidentUseCase.getOrStartDeviceSession(1L) } returns session
        every { telemetryUseCase.add(capture(saved)) } answers { saved.captured }

        service.ingestReadings(
            uuid,
            mapOf(
                "ax" to 0.1,
                "temperature" to null,
                "unknown" to 42.0
            )
        )

        assertEquals(Telemetry(incidentId = 7L, deviceId = 1L, recordedAt = saved.captured.recordedAt, accelRawX = 0.1), saved.captured)
    }

    @Test
    fun `should not store readings from unknown device`() {
        every { deviceRepository.getByUuid("unknown") } returns null

        assertNull(service.ingestReadings("unknown", fullPacket))
        verify(exactly = 0) { telemetryUseCase.add(any()) }
        verify(exactly = 0) { incidentUseCase.getOrStartDeviceSession(any()) }
    }

    @Test
    fun `should store frame of known device in its session`() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
        val saved = slot<ThermalFrame>()
        every { deviceRepository.getByUuid(uuid) } returns device
        every { incidentUseCase.getOrStartDeviceSession(1L) } returns session
        every { thermalFrameRepository.save(capture(saved)) } answers { saved.captured.copy(id = 20L) }

        assertTrue(service.ingestFrame(uuid, jpeg))
        assertEquals(
            ThermalFrame(deviceId = 1L, incidentId = 7L, capturedAt = saved.captured.capturedAt, image = jpeg),
            saved.captured
        )
        assertEquals(ZoneOffset.UTC, saved.captured.capturedAt.offset)
    }

    @Test
    fun `should not store frame from unknown device`() {
        every { deviceRepository.getByUuid("unknown") } returns null

        assertFalse(service.ingestFrame("unknown", byteArrayOf(0xFF.toByte(), 0xD8.toByte())))
        verify(exactly = 0) { thermalFrameRepository.save(any()) }
        verify(exactly = 0) { incidentUseCase.getOrStartDeviceSession(any()) }
    }

    @Test
    fun `should publish stored frame as live frame of the session`() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
        every { deviceRepository.getByUuid(uuid) } returns device
        every { incidentUseCase.getOrStartDeviceSession(1L) } returns session
        every { thermalFrameRepository.save(any()) } answers { firstArg() }

        service.ingestFrame(uuid, jpeg)

        val frame = assertNotNull(service.awaitLiveFrame(1L, 0L, Duration.ZERO))
        assertEquals(7L, frame.incidentId)
        assertTrue(jpeg.contentEquals(frame.image))
    }

    @Test
    fun `should return only frames newer than given sequence`() {
        val first = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1)
        val second = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 2)
        every { deviceRepository.getByUuid(uuid) } returns device
        every { incidentUseCase.getOrStartDeviceSession(1L) } returns session
        every { thermalFrameRepository.save(any()) } answers { firstArg() }

        service.ingestFrame(uuid, first)
        val firstFrame = assertNotNull(service.awaitLiveFrame(1L, 0L, Duration.ZERO))
        assertNull(service.awaitLiveFrame(1L, firstFrame.sequence, Duration.ZERO))

        service.ingestFrame(uuid, second)
        val secondFrame = assertNotNull(service.awaitLiveFrame(1L, firstFrame.sequence, Duration.ZERO))
        assertTrue(second.contentEquals(secondFrame.image))
    }

    @Test
    fun `should wake up waiting viewer when new frame arrives`() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
        every { deviceRepository.getByUuid(uuid) } returns device
        every { incidentUseCase.getOrStartDeviceSession(1L) } returns session
        every { thermalFrameRepository.save(any()) } answers { firstArg() }

        val publisher = Thread.ofVirtual().start {
            Thread.sleep(100)
            service.ingestFrame(uuid, jpeg)
        }

        assertNotNull(service.awaitLiveFrame(1L, 0L, Duration.ofSeconds(5)))
        publisher.join()
    }

    @Test
    fun `should time out when device sends no frames`() {
        assertNull(service.awaitLiveFrame(1L, 0L, Duration.ofMillis(50)))
    }

    @Test
    fun `should return device of in-progress incident for live view`() {
        every { incidentUseCase.getById(7L) } returns session

        assertEquals(1L, service.getLiveDeviceId(7L))
    }

    @Test
    fun `should not return live device for resolved or unknown incident`() {
        every { incidentUseCase.getById(7L) } returns session.copy(status = IncidentStatus.RESOLVED)
        every { incidentUseCase.getById(8L) } returns null

        assertNull(service.getLiveDeviceId(7L))
        assertNull(service.getLiveDeviceId(8L))
    }

    @Test
    fun `should return capture time of last stored frame of incident`() {
        val last = ThermalFrame(id = 30L, deviceId = 1L, incidentId = 7L, capturedAt = OffsetDateTime.parse("2026-09-26T10:05:00Z"), image = byteArrayOf(3))
        every { thermalFrameRepository.getLastByIncidentId(7L) } returns last
        every { thermalFrameRepository.getLastByIncidentId(8L) } returns null

        assertEquals(OffsetDateTime.parse("2026-09-26T10:05:00Z"), service.getLastFrameTime(7L))
        assertNull(service.getLastFrameTime(8L))
    }

    @Test
    fun `should return stored frames captured after given time`() {
        val after = OffsetDateTime.parse("2026-09-26T10:04:50Z")
        val frame = ThermalFrame(id = 29L, deviceId = 1L, incidentId = 7L, capturedAt = OffsetDateTime.parse("2026-09-26T10:04:55Z"), image = byteArrayOf(2))
        every { thermalFrameRepository.getByIncidentIdAfter(7L, after, 50) } returns listOf(frame)

        assertEquals(listOf(frame), service.getFramesAfter(7L, after, 50))
    }

    @Test
    fun `should trigger sos alarm in current session`() {
        val saved = slot<Alarm>()
        every { deviceRepository.getByUuid(uuid) } returns device
        every { incidentUseCase.getOrStartDeviceSession(1L) } returns session
        every { alarmUseCase.triggerAlarm(capture(saved)) } answers { saved.captured.copy(id = 3L) }

        val result = service.triggerSos(uuid)

        assertEquals(3L, result?.id)
        assertEquals(AlarmType.SOS, saved.captured.alarmType)
        assertEquals(7L, saved.captured.incidentId)
        assertNull(saved.captured.resolvedAt)
        assertEquals(ZoneOffset.UTC, saved.captured.triggeredAt.offset)
    }

    @Test
    fun `should not trigger sos for unknown device`() {
        every { deviceRepository.getByUuid("unknown") } returns null

        assertNull(service.triggerSos("unknown"))
        verify(exactly = 0) { incidentUseCase.getOrStartDeviceSession(any()) }
        verify(exactly = 0) { alarmUseCase.triggerAlarm(any()) }
    }
}
