package com.infraView.alarm.domain

import org.springframework.stereotype.Service

@Service
class AlarmThresholdService(
    private val alarmThresholdRepository: AlarmThresholdRepository
) : AlarmThresholdUseCase {

    override fun getAll(): List<AlarmThreshold> {
        return alarmThresholdRepository.getAll()
    }

    override fun getByMetric(metric: ThresholdMetric): AlarmThreshold? {
        return alarmThresholdRepository.getByMetric(metric)
    }

    override fun update(threshold: AlarmThreshold): AlarmThreshold? {
        alarmThresholdRepository.getByMetric(threshold.metric) ?: return null
        return alarmThresholdRepository.save(threshold)
    }
}
