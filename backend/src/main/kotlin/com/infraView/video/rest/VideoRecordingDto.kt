package com.infraView.video.rest

import com.infraView.video.domain.VideoRecording
import java.time.OffsetDateTime

data class VideoRecordingDto(
    val id: Int?,
    val incidentId: Int,
    val startedAt: OffsetDateTime,
    val endedAt: OffsetDateTime?,
    val storageKey: String?,
    val fileSizeBytes: Long?,
    val durationSec: Int?
)

data class VideoRecordingCreateDto(
    val incidentId: Int,
    val startedAt: OffsetDateTime
)

data class VideoRecordingUpdateDto(
    val endedAt: OffsetDateTime?,
    val storageKey: String?,
    val fileSizeBytes: Long?,
    val durationSec: Int?
)

fun VideoRecording.toDto() = VideoRecordingDto(
    id = this.id,
    incidentId = this.incidentId,
    startedAt = this.startedAt,
    endedAt = this.endedAt,
    storageKey = this.storageKey,
    fileSizeBytes = this.fileSizeBytes,
    durationSec = this.durationSec
)
