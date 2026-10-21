package com.infraView.alarm.database

import com.infraView.alarm.domain.ThresholdMetric
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AlarmThresholdJpaRepository : JpaRepository<AlarmThresholdJpaEntity, ThresholdMetric>
