package com.infraView.video.domain

import java.time.OffsetDateTime

interface ManageVideoRecordingUseCase {
    fun getAll(): List<VideoRecording>
    fun getById(id: Int): VideoRecording?
    fun getByIncidentId(incidentId: Int): List<VideoRecording>
    fun startRecording(incidentId: Int, startedAt: OffsetDateTime): VideoRecording
    fun updateRecording(id: Int, endedAt: OffsetDateTime?, storageKey: String?, fileSizeBytes: Long?, durationSec: Int?): VideoRecording?
    fun delete(id: Int)
}
