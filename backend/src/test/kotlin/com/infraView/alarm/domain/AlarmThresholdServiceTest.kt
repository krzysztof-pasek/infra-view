package com.infraView.alarm.domain

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AlarmThresholdServiceTest {

    private val alarmThresholdRepository = mockk<AlarmThresholdRepository>()
    private val service = AlarmThresholdService(alarmThresholdRepository)

    private val threshold = AlarmThreshold(ThresholdMetric.TEMPERATURE, threshold = 80.0, resetMargin = 5.0, enabled = true)

    @Test
    fun `should save updated threshold`() {
        val saved = slot<AlarmThreshold>()
        val updated = threshold.copy(threshold = 70.0, enabled = false)
        every { alarmThresholdRepository.getByMetric(ThresholdMetric.TEMPERATURE) } returns threshold
        every { alarmThresholdRepository.save(capture(saved)) } answers { saved.captured }

        val result = service.update(updated)

        assertEquals(updated, saved.captured)
        assertEquals(updated, result)
    }

    @Test
    fun `should return null and not save when threshold does not exist`() {
        every { alarmThresholdRepository.getByMetric(ThresholdMetric.TEMPERATURE) } returns null

        assertNull(service.update(threshold))
        verify(exactly = 0) { alarmThresholdRepository.save(any()) }
    }
}
