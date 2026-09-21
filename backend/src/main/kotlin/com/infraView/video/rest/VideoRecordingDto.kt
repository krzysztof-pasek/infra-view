package com.infraView.video.rest

import com.infraView.video.domain.VideoRecording
import java.time.OffsetDateTime

data class VideoRecordingDto(
    val id: Long?,
    val incidentId: Long,
    val startedAt: OffsetDateTime,
    val endedAt: OffsetDateTime?,
    val filePath: String?,
    val fileSizeBytes: Long?,
    val durationSec: Int?
)

data class VideoRecordingCreateDto(
    val incidentId: Long,
    val startedAt: OffsetDateTime
)

data class VideoRecordingUpdateDto(
    val endedAt: OffsetDateTime?,
    val filePath: String?,
    val fileSizeBytes: Long?,
    val durationSec: Int?
)

fun VideoRecording.toDto() = VideoRecordingDto(
    id = this.id,
    incidentId = this.incidentId,
    startedAt = this.startedAt,
    endedAt = this.endedAt,
    filePath = this.filePath,
    fileSizeBytes = this.fileSizeBytes,
    durationSec = this.durationSec
)
