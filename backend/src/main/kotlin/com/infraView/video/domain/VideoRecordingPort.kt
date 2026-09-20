package com.infraView.video.domain

interface VideoRecordingPort {
    fun getById(id: Int): VideoRecording?
    fun getAll(): List<VideoRecording>
    fun getByIncidentId(incidentId: Int): List<VideoRecording>
    fun save(video: VideoRecording): VideoRecording
    fun delete(id: Int)
}
