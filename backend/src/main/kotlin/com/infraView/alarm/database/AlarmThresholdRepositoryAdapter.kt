package com.infraView.alarm.database

import com.infraView.alarm.domain.AlarmThreshold
import com.infraView.alarm.domain.AlarmThresholdRepository
import com.infraView.alarm.domain.ThresholdMetric
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class AlarmThresholdRepositoryAdapter(
    private val jpaRepository: AlarmThresholdJpaRepository
) : AlarmThresholdRepository {

    override fun getAll(): List<AlarmThreshold> {
        return jpaRepository.findAll().map { it.toDomain() }.sortedBy { it.metric }
    }

    override fun getByMetric(metric: ThresholdMetric): AlarmThreshold? {
        return jpaRepository.findByIdOrNull(metric)?.toDomain()
    }

    override fun save(threshold: AlarmThreshold): AlarmThreshold {
        return jpaRepository.save(threshold.toJpaEntity()).toDomain()
    }
}

private fun AlarmThreshold.toJpaEntity(): AlarmThresholdJpaEntity {
    return AlarmThresholdJpaEntity(
        metric = this.metric,
        threshold = this.threshold,
        resetMargin = this.resetMargin,
        enabled = this.enabled
    )
}

private fun AlarmThresholdJpaEntity.toDomain(): AlarmThreshold {
    return AlarmThreshold(
        metric = this.metric,
        threshold = this.threshold,
        resetMargin = this.resetMargin,
        enabled = this.enabled
    )
}
