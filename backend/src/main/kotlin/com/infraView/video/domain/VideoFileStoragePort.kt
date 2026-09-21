package com.infraView.video.domain

interface VideoFileStoragePort {
    fun saveFrame(incidentId: Long, startedAt: java.time.OffsetDateTime, frameBytes: ByteArray): String
    fun getRecordingPath(incidentId: Long, startedAt: java.time.OffsetDateTime): String?
}
