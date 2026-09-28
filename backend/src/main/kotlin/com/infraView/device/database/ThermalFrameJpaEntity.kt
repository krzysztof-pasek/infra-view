package com.infraView.device.database

import com.infraView.incident.database.IncidentJpaEntity
import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "video_frames")
class ThermalFrameJpaEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    var device: DeviceJpaEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    var incident: IncidentJpaEntity,

    @Column(name = "captured_at", nullable = false)
    var capturedAt: OffsetDateTime,

    @Column(name = "image", nullable = false)
    var image: ByteArray
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
