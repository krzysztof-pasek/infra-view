package com.infraView.alarm.domain

interface AlarmThresholdUseCase {
    fun getAll(): List<AlarmThreshold>
    fun getByMetric(metric: ThresholdMetric): AlarmThreshold?
    fun update(threshold: AlarmThreshold): AlarmThreshold?
}
