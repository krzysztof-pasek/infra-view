package com.infraView.video.database

import com.infraView.device.database.ThermalFrameJpaEntity
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime

interface VideoFrameStats {
    val startedAt: OffsetDateTime?
    val endedAt: OffsetDateTime?
    val frameCount: Long
}

interface VideoRecordingJpaRepository : Repository<ThermalFrameJpaEntity, Long> {
    @Query(
        "select min(f.capturedAt) as startedAt, max(f.capturedAt) as endedAt, count(f) as frameCount " +
            "from ThermalFrameJpaEntity f where f.incident.id = :incidentId"
    )
    fun getStatsByIncidentId(@Param("incidentId") incidentId: Long): VideoFrameStats

    @Query(
        "select coalesce(sum(octet_length(image)), 0) from video_frames where incident_id = :incidentId",
        nativeQuery = true
    )
    fun getImageBytesByIncidentId(@Param("incidentId") incidentId: Long): Long
}
