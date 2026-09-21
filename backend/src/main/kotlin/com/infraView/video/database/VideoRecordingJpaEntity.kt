package com.infraView.video.database

import com.infraView.incident.database.IncidentJpaEntity
import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "video_recordings")
class VideoRecordingJpaEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    var incident: IncidentJpaEntity,

    @Column(name = "started_at", nullable = false)
    var startedAt: OffsetDateTime,

    @Column(name = "ended_at")
    var endedAt: OffsetDateTime? = null,

    @Column(name = "file_path")
    var filePath: String? = null,

    @Column(name = "file_size_bytes")
    var fileSizeBytes: Long? = null,

    @Column(name = "duration_sec")
    var durationSec: Int? = null
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
