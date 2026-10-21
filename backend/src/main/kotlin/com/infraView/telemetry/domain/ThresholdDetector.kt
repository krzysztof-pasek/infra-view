package com.infraView.telemetry.domain

import com.infraView.alarm.domain.AlarmThreshold
import com.infraView.alarm.domain.ThresholdMetric

object ThresholdDetector {

    const val DEBOUNCE_SAMPLES = 3

    fun shouldTrigger(history: List<Telemetry>, current: Telemetry, threshold: AlarmThreshold): Boolean {
        if (!threshold.enabled || current.valueOf(threshold.metric) == null) return false
        val resetLevel = threshold.threshold - threshold.resetMargin
        val excursion = (history + current)
            .mapNotNull { it.valueOf(threshold.metric) }
            .takeLastWhile { it >= resetLevel }
        var run = 0
        var triggers = 0
        for (value in excursion) {
            run = if (value >= threshold.threshold) run + 1 else 0
            if (run == DEBOUNCE_SAMPLES) triggers++
        }
        return run == DEBOUNCE_SAMPLES && triggers == 1
    }

    private fun Telemetry.valueOf(metric: ThresholdMetric): Double? = when (metric) {
        ThresholdMetric.TEMPERATURE -> temperature
        ThresholdMetric.CO2_PPM -> co2Ppm
        ThresholdMetric.TVOC_PPB -> tvocPpb
    }
}
