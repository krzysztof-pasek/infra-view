package com.infraView.video.domain

import java.time.OffsetDateTime

data class VideoRecording(
    val id: Long? = null,
    val incidentId: Long,
    val startedAt: OffsetDateTime,
    var endedAt: OffsetDateTime? = null,
    var filePath: String? = null,
    val fileSizeBytes: Long? = null,
    val durationSec: Int? = null
)
