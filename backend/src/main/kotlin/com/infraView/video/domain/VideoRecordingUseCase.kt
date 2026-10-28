package com.infraView.video.domain

interface VideoRecordingUseCase {
    fun getByIncidentId(incidentId: Long): List<VideoRecording>
}
