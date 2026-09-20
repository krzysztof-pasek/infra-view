package com.infraView.video.domain

import java.time.OffsetDateTime

interface ManageVideoRecordingUseCase {
    fun getAll(): List<VideoRecording>
    fun getById(id: Long): VideoRecording?
    fun getByIncidentId(incidentId: Long): List<VideoRecording>
    fun startRecording(incidentId: Long, startedAt: OffsetDateTime): VideoRecording
    fun updateRecording(id: Long, endedAt: OffsetDateTime?, storageKey: String?, fileSizeBytes: Long?, durationSec: Int?): VideoRecording?
    fun delete(id: Long)
}
