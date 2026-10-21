package com.infraView.alarm.database

import com.infraView.alarm.domain.ThresholdMetric
import jakarta.persistence.*

@Entity
@Table(name = "alarm_thresholds")
class AlarmThresholdJpaEntity(
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "metric", length = 30)
    var metric: ThresholdMetric,

    @Column(name = "threshold", nullable = false)
    var threshold: Double,

    @Column(name = "reset_margin", nullable = false)
    var resetMargin: Double,

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean
)
