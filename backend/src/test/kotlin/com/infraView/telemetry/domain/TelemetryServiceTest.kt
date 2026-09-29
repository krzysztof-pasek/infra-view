package com.infraView.telemetry.domain

import com.infraView.alarm.domain.Alarm
import com.infraView.alarm.domain.AlarmType
import com.infraView.alarm.domain.AlarmUseCase
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TelemetryServiceTest {

    private val telemetryRepository = mockk<TelemetryRepository>()
    private val alarmUseCase = mockk<AlarmUseCase>()
    private val service = TelemetryService(telemetryRepository, alarmUseCase)

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

    /** 5 walking packets, a transition packet and 9 still lying packets: the next lying packet confirms the fall. */
    private val beforeFall = List(5) { walking(it.toLong()) } +
        lying(5, MotionState.MOVING) +
        List(9) { lying(6L + it, MotionState.STILL) }

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

        assertEquals(MotionState.IDLE.name, saved.captured.motionState)
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
}
