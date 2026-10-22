package com.infraView.telemetry.domain

import com.infraView.alarm.domain.AlarmThreshold
import com.infraView.alarm.domain.ThresholdMetric
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThresholdDetectorTest {

    private val start = OffsetDateTime.parse("2026-10-21T10:00:00Z")
    private val threshold = AlarmThreshold(ThresholdMetric.TEMPERATURE, threshold = 80.0, resetMargin = 5.0, enabled = true)

    private fun samples(vararg temperatures: Double?) = temperatures.mapIndexed { i, t ->
        Telemetry(deviceId = 1L, incidentId = 7L, recordedAt = start.plusSeconds(i.toLong()), temperature = t)
    }

    private fun triggersAt(vararg temperatures: Double?): List<Int> {
        val all = samples(*temperatures)
        return all.indices.filter { ThresholdDetector.shouldTrigger(all.take(it), all[it], threshold) }
    }

    @Test
    fun `should trigger once when value stays above threshold for debounce samples`() {
        assertEquals(listOf(4), triggersAt(25.0, 30.0, 81.0, 85.0, 90.0, 95.0, 99.0, 100.0))
    }

    @Test
    fun `should not trigger on a short spike`() {
        assertEquals(emptyList(), triggersAt(25.0, 120.0, 120.0, 25.0, 25.0))
    }

    @Test
    fun `should not trigger again while value oscillates between reset level and threshold`() {
        assertEquals(listOf(2), triggersAt(81.0, 81.0, 81.0, 78.0, 81.0, 81.0, 81.0, 76.0, 82.0, 82.0, 82.0))
    }

    @Test
    fun `should trigger again after value drops below clear level`() {
        assertEquals(listOf(2, 6), triggersAt(81.0, 81.0, 81.0, 74.0, 81.0, 81.0, 81.0))
    }

    @Test
    fun `should trigger when excursion reaches debounce samples after a dip that stays above reset level`() {
        assertEquals(listOf(6), triggersAt(25.0, 81.0, 81.0, 78.0, 81.0, 81.0, 81.0))
    }

    @Test
    fun `should skip missing readings`() {
        assertEquals(listOf(4), triggersAt(25.0, 81.0, null, 81.0, 81.0, 81.0))
    }

    @Test
    fun `should not trigger when current reading is missing`() {
        val all = samples(81.0, 81.0, null)
        assertFalse(ThresholdDetector.shouldTrigger(all.take(2), all[2], threshold))
    }

    @Test
    fun `should not trigger when threshold is disabled`() {
        val all = samples(81.0, 81.0, 81.0)
        assertTrue(ThresholdDetector.shouldTrigger(all.take(2), all[2], threshold))
        assertFalse(ThresholdDetector.shouldTrigger(all.take(2), all[2], threshold.copy(enabled = false)))
    }

    @Test
    fun `should trigger with zero reset margin only after value falls below threshold`() {
        val zero = threshold.copy(resetMargin = 0.0)
        val all = samples(81.0, 81.0, 81.0, 79.9, 81.0, 81.0, 81.0)
        val triggers = all.indices.filter { ThresholdDetector.shouldTrigger(all.take(it), all[it], zero) }
        assertEquals(listOf(2, 6), triggers)
    }
}
