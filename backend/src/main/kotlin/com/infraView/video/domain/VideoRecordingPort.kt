package com.infraView.video.domain

interface VideoRecordingPort {
    fun getById(id: Long): VideoRecording?
    fun getAll(): List<VideoRecording>
    fun getByIncidentId(incidentId: Long): List<VideoRecording>
    fun save(video: VideoRecording): VideoRecording
    fun delete(id: Long)
}
