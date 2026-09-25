package com.infraView.incident.database

import com.infraView.alarm.database.AlarmJpaEntity
import com.infraView.incident.domain.StatusType
import com.infraView.telemetry.database.TelemetryJpaEntity
import com.infraView.video.database.VideoRecordingJpaEntity
import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "incidents")
class IncidentJpaEntity(
    @Column(name = "code", nullable = false, length = 20)
    var code: String,

    @Column(name = "firefighter_name", length = 100)
    var firefighterName: String? = null,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "location", length = 200)
    var location: String? = null,

    @Column(name = "started_at", nullable = false)
    var startedAt: OffsetDateTime,

    @Column(name = "ended_at")
    var endedAt: OffsetDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: StatusType
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @OneToMany(mappedBy = "incident", fetch = FetchType.LAZY)
    var videoRecordings: MutableList<VideoRecordingJpaEntity> = mutableListOf()

    @OneToMany(mappedBy = "incident", fetch = FetchType.LAZY)
    var telemetryEntries: MutableList<TelemetryJpaEntity> = mutableListOf()

    @OneToMany(mappedBy = "incident", fetch = FetchType.LAZY)
    var alarms: MutableList<AlarmJpaEntity> = mutableListOf()
}
