package com.infraView.incident

import com.infraView.alarm.AlarmEntity
import com.infraView.telemetry.TelemetryEntity
import com.infraView.video.VideoRecordingEntity
import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "incidents")
class IncidentEntity(
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

    @Column(name = "status", nullable = false, length = 20)
    var status: StatusType
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null

    @OneToMany(mappedBy = "incident", fetch = FetchType.LAZY)
    var videoRecordings: MutableList<VideoRecordingEntity> = mutableListOf()

    @OneToMany(mappedBy = "incident", fetch = FetchType.LAZY)
    var telemetryEntries: MutableList<TelemetryEntity> = mutableListOf()

    @OneToMany(mappedBy = "incident", fetch = FetchType.LAZY)
    var alarms: MutableList<AlarmEntity> = mutableListOf()
}
