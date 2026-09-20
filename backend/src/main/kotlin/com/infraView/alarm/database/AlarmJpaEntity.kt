package com.infraView.alarm.database

import com.infraView.alarm.domain.AlarmType
import com.infraView.incident.database.IncidentJpaEntity
import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "alarms")
class AlarmJpaEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    var incident: IncidentJpaEntity,

    @Enumerated(EnumType.STRING)
    @Column(name = "alarm_type", nullable = false, length = 30)
    var alarmType: AlarmType,

    @Column(name = "triggered_at", nullable = false)
    var triggeredAt: OffsetDateTime,

    @Column(name = "resolved_at")
    var resolvedAt: OffsetDateTime? = null
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null
}
