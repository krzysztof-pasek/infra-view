package com.infraView.video.domain

import java.time.OffsetDateTime

data class VideoRecording(
    val id: Int? = null,
    val incidentId: Int,
    val startedAt: OffsetDateTime,
    val endedAt: OffsetDateTime? = null,
    val storageKey: String? = null,
    val fileSizeBytes: Long? = null,
    val durationSec: Int? = null
)
