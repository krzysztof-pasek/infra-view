package com.infraView.alarm.rest

import com.infraView.alarm.domain.AlarmThreshold
import com.infraView.alarm.domain.AlarmType
import com.infraView.alarm.domain.ThresholdMetric

data class AlarmThresholdDto(
    val metric: ThresholdMetric,
    val alarmType: AlarmType,
    val threshold: Double,
    val resetMargin: Double,
    val enabled: Boolean
)

data class AlarmThresholdUpdateDto(
    val threshold: Double,
    val resetMargin: Double,
    val enabled: Boolean
) {
    fun toDomain(metric: ThresholdMetric) = AlarmThreshold(
        metric = metric,
        threshold = this.threshold,
        resetMargin = this.resetMargin,
        enabled = this.enabled
    )
}

fun AlarmThreshold.toDto() = AlarmThresholdDto(
    metric = this.metric,
    alarmType = this.metric.alarmType,
    threshold = this.threshold,
    resetMargin = this.resetMargin,
    enabled = this.enabled
)
