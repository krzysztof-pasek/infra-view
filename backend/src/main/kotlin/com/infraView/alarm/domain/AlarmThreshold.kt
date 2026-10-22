package com.infraView.alarm.domain

data class AlarmThreshold(
    val metric: ThresholdMetric,
    val threshold: Double,
    val resetMargin: Double,
    val enabled: Boolean
)
