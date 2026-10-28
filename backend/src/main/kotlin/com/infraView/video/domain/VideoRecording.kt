package com.infraView.video.domain

import java.time.OffsetDateTime

data class VideoRecording(
    val id: Long,
    val incidentId: Long,
    val startedAt: OffsetDateTime,
    val endedAt: OffsetDateTime?,
    val frameCount: Long,
    val fileSizeBytes: Long,
    val durationSec: Int
)
