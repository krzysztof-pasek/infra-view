package com.infraView.alarm.domain

interface AlarmThresholdRepository {
    fun getAll(): List<AlarmThreshold>
    fun getByMetric(metric: ThresholdMetric): AlarmThreshold?
    fun save(threshold: AlarmThreshold): AlarmThreshold
}
