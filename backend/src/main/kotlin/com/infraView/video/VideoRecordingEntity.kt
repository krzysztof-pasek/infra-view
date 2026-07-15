package com.infraView.video

import com.infraView.incident.IncidentEntity
import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "video_recordings")
class VideoRecordingEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    var incident: IncidentEntity,

    @Column(name = "started_at", nullable = false)
    var startedAt: OffsetDateTime,

    @Column(name = "ended_at")
    var endedAt: OffsetDateTime? = null,

    @Column(name = "storage_key", columnDefinition = "TEXT")
    var storageKey: String? = null,

    @Column(name = "file_size_bytes")
    var fileSizeBytes: Long? = null,

    @Column(name = "duration_sec")
    var durationSec: Int? = null,

    @Column(name = "is_thermal", nullable = false)
    var isThermal: Boolean,

    @Column(name = "status", nullable = false, length = 20)
    var status: String
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null
}
