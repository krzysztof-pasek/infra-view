package com.infraView.device.domain

import com.infraView.incident.domain.Incident
import com.infraView.incident.domain.ManageIncidentUseCase
import com.infraView.incident.domain.StatusType
import com.infraView.telemetry.domain.ManageTelemetryUseCase
import com.infraView.telemetry.domain.Telemetry
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HelmetServiceTest {

    private val devicePort = mockk<DevicePort>()
    private val thermalFramePort = mockk<ThermalFramePort>()
    private val telemetryUseCase = mockk<ManageTelemetryUseCase>()
    private val incidentUseCase = mockk<ManageIncidentUseCase>()
    private val service = HelmetService(devicePort, thermalFramePort, telemetryUseCase, incidentUseCase)

    private val mac = "d8:3a:dd:ed:3f:42"
    private val uuid = "689d8e89-2503-4739-9dd8-c93bc9d7c14d"
    private val device = Device(id = 1L, mac = mac, uuid = uuid, registeredAt = OffsetDateTime.parse("2026-09-26T10:00:00Z"))
    private val session = Incident(
        id = 7L,
        code = "HELMET-1-0926100000",
        startedAt = OffsetDateTime.parse("2026-09-26T10:00:00Z"),
        status = StatusType.IN_PROGRESS,
        deviceId = 1L
    )

    private val fullPacket = listOf(
        SensorReading("bmi160", "ax", -0.037),
        SensorReading("bmi160", "ay", 0.509),
        SensorReading("bmi160", "az", 0.904),
        SensorReading("bmi160", "gx", -0.899),
        SensorReading("bmi160", "gy", 0.655),
        SensorReading("bmi160", "gz", -0.107),
        SensorReading("sgp30", "eco2", 400.0),
        SensorReading("sgp30", "tvoc", 0.0),
        SensorReading("mics5524", "voltage", 0.267),
        SensorReading("max6675", "temperature", 23.75)
    )

    @Test
    fun `should return existing device for known mac`() {
        every { devicePort.getByMac(mac) } returns device

        assertEquals(device, service.register(mac))
        verify(exactly = 0) { devicePort.save(any()) }
    }

    @Test
    fun `should create device with new uuid for unknown mac`() {
        val saved = slot<Device>()
        every { devicePort.getByMac(mac) } returns null
        every { devicePort.save(capture(saved)) } answers { saved.captured.copy(id = 2L) }

        val result = service.register(mac)

        assertEquals(2L, result.id)
        assertEquals(mac, saved.captured.mac)
        UUID.fromString(saved.captured.uuid)
        assertEquals(ZoneOffset.UTC, saved.captured.registeredAt.offset)
    }

    @Test
    fun `should map every sensor reading to its telemetry field`() {
        val saved = slot<Telemetry>()
        every { devicePort.getByUuid(uuid) } returns device
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
        assertNull(telemetry.gasPpm)
        assertEquals(ZoneOffset.UTC, telemetry.recordedAt.offset)
    }

    @Test
    fun `should keep failed and missing sensors as null and ignore unknown ones`() {
        val saved = slot<Telemetry>()
        every { devicePort.getByUuid(uuid) } returns device
        every { incidentUseCase.getOrStartDeviceSession(1L) } returns session
        every { telemetryUseCase.add(capture(saved)) } answers { saved.captured }

        service.ingestReadings(
            uuid,
            listOf(
                SensorReading("bmi160", "ax", 0.1),
                SensorReading("max6675", "temperature", null),
                SensorReading("unknown", "metric", 42.0)
            )
        )

        assertEquals(Telemetry(incidentId = 7L, deviceId = 1L, recordedAt = saved.captured.recordedAt, accelRawX = 0.1), saved.captured)
    }

    @Test
    fun `should not store readings from unknown device`() {
        every { devicePort.getByUuid("unknown") } returns null

        assertNull(service.ingestReadings("unknown", fullPacket))
        verify(exactly = 0) { telemetryUseCase.add(any()) }
        verify(exactly = 0) { incidentUseCase.getOrStartDeviceSession(any()) }
    }

    @Test
    fun `should store frame of known device`() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
        every { devicePort.getByUuid(uuid) } returns device
        every { thermalFramePort.saveLatest(1L, jpeg) } just runs

        assertTrue(service.ingestFrame(uuid, jpeg))
        verify(exactly = 1) { thermalFramePort.saveLatest(1L, jpeg) }
    }

    @Test
    fun `should not store frame from unknown device`() {
        every { devicePort.getByUuid("unknown") } returns null

        assertFalse(service.ingestFrame("unknown", byteArrayOf(0xFF.toByte(), 0xD8.toByte())))
        verify(exactly = 0) { thermalFramePort.saveLatest(any(), any()) }
    }
}
