package com.infraView.video.domain

interface VideoRecordingRepository {
    fun getByIncidentId(incidentId: Long): VideoRecording?
}
