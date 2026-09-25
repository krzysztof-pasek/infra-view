package com.infraView.alarm.domain

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AlarmServiceTest {

    private val alarmPort = mockk<AlarmPort>()
    private val service = AlarmService(alarmPort)

    private val triggeredAt = OffsetDateTime.parse("2026-09-24T10:00:00Z")
    private val alarm = Alarm(id = 1L, incidentId = 100L, alarmType = AlarmType.FALL, triggeredAt = triggeredAt)

    @Test
    fun `should set resolvedAt when resolving an active alarm`() {
        val saved = slot<Alarm>()
        every { alarmPort.getById(1L) } returns alarm
        every { alarmPort.save(capture(saved)) } answers { saved.captured }
        val before = OffsetDateTime.now()

        val result = service.resolveAlarm(1L)

        assertNotNull(result)
        val resolvedAt = assertNotNull(result.resolvedAt)
        assertTrue(!resolvedAt.isBefore(before))
        assertEquals(alarm.copy(resolvedAt = resolvedAt), saved.captured)
    }

    @Test
    fun `should keep original resolvedAt when alarm is already resolved`() {
        val resolved = alarm.copy(resolvedAt = triggeredAt.plusMinutes(5))
        every { alarmPort.getById(1L) } returns resolved

        val result = service.resolveAlarm(1L)

        assertEquals(resolved, result)
        verify(exactly = 0) { alarmPort.save(any()) }
    }

    @Test
    fun `should return null when resolving non-existent alarm`() {
        every { alarmPort.getById(99L) } returns null

        assertNull(service.resolveAlarm(99L))
        verify(exactly = 0) { alarmPort.save(any()) }
    }
}
