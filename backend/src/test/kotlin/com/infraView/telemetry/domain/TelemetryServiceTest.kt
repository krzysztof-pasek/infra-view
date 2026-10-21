package com.infraView.telemetry.domain

import com.infraView.alarm.domain.Alarm
import com.infraView.alarm.domain.AlarmThreshold
import com.infraView.alarm.domain.AlarmThresholdUseCase
import com.infraView.alarm.domain.AlarmType
import com.infraView.alarm.domain.AlarmUseCase
import com.infraView.alarm.domain.ThresholdMetric
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TelemetryServiceTest {

    private val telemetryRepository = mockk<TelemetryRepository>()
    private val alarmUseCase = mockk<AlarmUseCase>()
    private val alarmThresholdUseCase = mockk<AlarmThresholdUseCase>()
    private val service = TelemetryService(telemetryRepository, alarmUseCase, alarmThresholdUseCase)

    private val thresholds = listOf(
        AlarmThreshold(ThresholdMetric.TEMPERATURE, threshold = 80.0, resetMargin = 5.0, enabled = true),
        AlarmThreshold(ThresholdMetric.CO2_PPM, threshold = 5000.0, resetMargin = 500.0, enabled = true),
        AlarmThreshold(ThresholdMetric.TVOC_PPB, threshold = 5500.0, resetMargin = 500.0, enabled = true)
    )

    private val start = OffsetDateTime.parse("2026-09-28T10:00:00Z")

    private fun packet(second: Long, x: Double, y: Double, z: Double, gyro: Double, state: MotionState? = null) = Telemetry(
        deviceId = 1L,
        incidentId = 7L,
        recordedAt = start.plusSeconds(second),
        accelRawX = x,
        accelRawY = y,
        accelRawZ = z,
        gyroRawX = gyro,
        gyroRawY = 0.0,
        gyroRawZ = 0.0,
        motionState = state?.name
    )

    private fun walking(second: Long) = packet(second, 0.1, 0.6, 1.1, 40.0, MotionState.MOVING)
    private fun lying(second: Long, state: MotionState? = null) = packet(second, 1.0, 0.0, 0.05, 0.5, state)

    private val beforeFall = List(5) { walking(it.toLong()) } +
        lying(5, MotionState.MOVING) +
        List(9) { lying(6L + it, MotionState.STILL) }

    @BeforeEach
    fun stubThresholds() {
        every { alarmThresholdUseCase.getAll() } returns thresholds
    }

    private fun hot(second: Long, temperature: Double) =
        Telemetry(deviceId = 1L, incidentId = 7L, recordedAt = start.plusSeconds(second), temperature = temperature, co2Ppm = 400.0)

    private fun stubSave(): CapturingSlot<Telemetry> {
        val saved = slot<Telemetry>()
        every { telemetryRepository.save(capture(saved)) } answers { saved.captured.copy(id = 100L) }
        return saved
    }

    @Test
    fun `should save detected motion state and trigger fall alarm when entering fallen state`() {
        val saved = stubSave()
        val alarm = slot<Alarm>()
        every { telemetryRepository.getRecentByDeviceId(1L, MotionDetector.HISTORY_SIZE) } returns beforeFall
        every { alarmUseCase.triggerAlarm(capture(alarm)) } answers { alarm.captured.copy(id = 5L) }

        val result = service.add(lying(15))

        assertEquals(MotionState.FALLEN.name, saved.captured.motionState)
        assertEquals(100L, result.id)
        assertEquals(Alarm(alarmType = AlarmType.FALL, triggeredAt = alarm.captured.triggeredAt, incidentId = 7L), alarm.captured)
        assertEquals(ZoneOffset.UTC, alarm.captured.triggeredAt.offset)
    }

    @Test
    fun `should trigger no motion alarm when entering no motion state`() {
        stubSave()
        val alarm = slot<Alarm>()
        every { telemetryRepository.getRecentByDeviceId(1L, MotionDetector.HISTORY_SIZE) } returns
            List(29) { packet(it.toLong(), 0.0, 0.5, 0.866, 0.5, MotionState.STILL) }
        every { alarmUseCase.triggerAlarm(capture(alarm)) } answers { alarm.captured.copy(id = 6L) }

        service.add(packet(29, 0.0, 0.5, 0.866, 0.5))

        assertEquals(Alarm(alarmType = AlarmType.NO_MOTION, triggeredAt = alarm.captured.triggeredAt, incidentId = 7L), alarm.captured)
    }

    @Test
    fun `should not trigger alarm again while still in fallen state`() {
        val saved = stubSave()
        every { telemetryRepository.getRecentByDeviceId(1L, MotionDetector.HISTORY_SIZE) } returns
            beforeFall + lying(15, MotionState.FALLEN)

        service.add(lying(16))

        assertEquals(MotionState.FALLEN.name, saved.captured.motionState)
        verify(exactly = 0) { alarmUseCase.triggerAlarm(any()) }
    }

    @Test
    fun `should ignore history of previous session of the same device`() {
        val saved = stubSave()
        every { telemetryRepository.getRecentByDeviceId(1L, MotionDetector.HISTORY_SIZE) } returns
            beforeFall.map { it.copy(incidentId = 6L) }

        service.add(lying(15))

        assertEquals(MotionState.STILL.name, saved.captured.motionState)
        verify(exactly = 0) { alarmUseCase.triggerAlarm(any()) }
    }

    @Test
    fun `should save telemetry without device as is`() {
        val saved = stubSave()
        val manual = Telemetry(incidentId = 7L, recordedAt = start, motionState = "MOVING")

        service.add(manual)

        assertEquals(manual, saved.captured)
        verify(exactly = 0) { telemetryRepository.getRecentByDeviceId(any(), any()) }
    }

    @Test
    fun `should store null motion state when imu reading is missing`() {
        val saved = stubSave()
        every { telemetryRepository.getRecentByDeviceId(1L, MotionDetector.HISTORY_SIZE) } returns beforeFall

        service.add(Telemetry(deviceId = 1L, incidentId = 7L, recordedAt = start.plusSeconds(15), temperature = 25.0))

        assertNull(saved.captured.motionState)
        verify(exactly = 0) { alarmUseCase.triggerAlarm(any()) }
    }

    @Test
    fun `should trigger high temperature alarm when threshold is exceeded for debounce samples`() {
        stubSave()
        val alarm = slot<Alarm>()
        every { telemetryRepository.getRecentByDeviceId(1L, MotionDetector.HISTORY_SIZE) } returns
            listOf(hot(0, 25.0), hot(1, 85.0), hot(2, 90.0))
        every { alarmUseCase.getUnresolvedByIncidentId(7L) } returns emptyList()
        every { alarmUseCase.triggerAlarm(capture(alarm)) } answers { alarm.captured.copy(id = 8L) }

        service.add(hot(3, 95.0))

        assertEquals(Alarm(alarmType = AlarmType.HIGH_TEMPERATURE, triggeredAt = alarm.captured.triggeredAt, incidentId = 7L), alarm.captured)
    }

    @Test
    fun `should not trigger threshold alarm when the same alarm type is still unresolved`() {
        stubSave()
        every { telemetryRepository.getRecentByDeviceId(1L, MotionDetector.HISTORY_SIZE) } returns
            listOf(hot(0, 25.0), hot(1, 85.0), hot(2, 90.0))
        every { alarmUseCase.getUnresolvedByIncidentId(7L) } returns
            listOf(Alarm(id = 3L, alarmType = AlarmType.HIGH_TEMPERATURE, triggeredAt = start, incidentId = 7L))

        service.add(hot(3, 95.0))

        verify(exactly = 0) { alarmUseCase.triggerAlarm(any()) }
    }

    @Test
    fun `should trigger separate co2 and tvoc alarms when both exceed thresholds at once`() {
        stubSave()
        val gas = { second: Long, value: Double ->
            Telemetry(deviceId = 1L, incidentId = 7L, recordedAt = start.plusSeconds(second), co2Ppm = value, tvocPpb = value + 1000.0)
        }
        every { telemetryRepository.getRecentByDeviceId(1L, MotionDetector.HISTORY_SIZE) } returns
            listOf(gas(0, 400.0), gas(1, 6000.0), gas(2, 6000.0))
        every { alarmUseCase.getUnresolvedByIncidentId(7L) } returns emptyList()
        every { alarmUseCase.triggerAlarm(any()) } answers { firstArg<Alarm>().copy(id = 9L) }

        service.add(gas(3, 6000.0))

        verify(exactly = 1) { alarmUseCase.triggerAlarm(match { it.alarmType == AlarmType.HIGH_CO2 && it.incidentId == 7L }) }
        verify(exactly = 1) { alarmUseCase.triggerAlarm(match { it.alarmType == AlarmType.HIGH_TVOC && it.incidentId == 7L }) }
    }

    @Test
    fun `should not look up unresolved alarms when no threshold is crossed`() {
        stubSave()
        every { telemetryRepository.getRecentByDeviceId(1L, MotionDetector.HISTORY_SIZE) } returns
            listOf(hot(0, 85.0), hot(1, 90.0), hot(2, 90.0))

        service.add(hot(3, 95.0))

        verify(exactly = 0) { alarmUseCase.getUnresolvedByIncidentId(any()) }
        verify(exactly = 0) { alarmUseCase.triggerAlarm(any()) }
    }
}
