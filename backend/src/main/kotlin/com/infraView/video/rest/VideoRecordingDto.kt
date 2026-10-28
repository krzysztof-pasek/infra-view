package com.infraView.video.rest

import com.infraView.video.domain.VideoRecording
import java.time.OffsetDateTime

data class VideoRecordingDto(
    val id: Long,
    val incidentId: Long,
    val startedAt: OffsetDateTime,
    val endedAt: OffsetDateTime?,
    val frameCount: Long,
    val fileSizeBytes: Long,
    val durationSec: Int
)

fun VideoRecording.toDto() = VideoRecordingDto(
    id = this.id,
    incidentId = this.incidentId,
    startedAt = this.startedAt,
    endedAt = this.endedAt,
    frameCount = this.frameCount,
    fileSizeBytes = this.fileSizeBytes,
    durationSec = this.durationSec
)
