package com.infraView.video.domain

import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class VideoRecordingService(
    private val videoRecordingPort: VideoRecordingPort
) : ManageVideoRecordingUseCase {

    override fun getAll(): List<VideoRecording> {
        return videoRecordingPort.getAll()
    }

    override fun getById(id: Long): VideoRecording? {
        return videoRecordingPort.getById(id)
    }

    override fun getByIncidentId(incidentId: Long): List<VideoRecording> {
        return videoRecordingPort.getByIncidentId(incidentId)
    }

    override fun startRecording(incidentId: Long, startedAt: OffsetDateTime): VideoRecording {
        val video = VideoRecording(
            incidentId = incidentId,
            startedAt = startedAt
        )
        return videoRecordingPort.save(video)
    }

    override fun updateRecording(
        id: Long,
        endedAt: OffsetDateTime?,
        filePath: String?,
        fileSizeBytes: Long?,
        durationSec: Int?
    ): VideoRecording? {
        val existing = videoRecordingPort.getById(id) ?: return null
        val updated = existing.copy(
            endedAt = endedAt ?: existing.endedAt,
            filePath = filePath ?: existing.filePath,
            fileSizeBytes = fileSizeBytes ?: existing.fileSizeBytes,
            durationSec = durationSec ?: existing.durationSec
        )
        return videoRecordingPort.save(updated)
    }

    override fun delete(id: Long) {
        videoRecordingPort.delete(id)
    }
}
