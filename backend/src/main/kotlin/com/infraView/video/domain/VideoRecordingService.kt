package com.infraView.video.domain

import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class VideoRecordingService(
    private val videoRecordingRepository: VideoRecordingRepository
) : VideoRecordingUseCase {

    override fun getAll(): List<VideoRecording> {
        return videoRecordingRepository.getAll()
    }

    override fun getById(id: Long): VideoRecording? {
        return videoRecordingRepository.getById(id)
    }

    override fun getByIncidentId(incidentId: Long): List<VideoRecording> {
        return videoRecordingRepository.getByIncidentId(incidentId)
    }

    override fun startRecording(incidentId: Long, startedAt: OffsetDateTime): VideoRecording {
        val video = VideoRecording(
            incidentId = incidentId,
            startedAt = startedAt
        )
        return videoRecordingRepository.save(video)
    }

    override fun updateRecording(
        id: Long,
        endedAt: OffsetDateTime?,
        filePath: String?,
        fileSizeBytes: Long?,
        durationSec: Int?
    ): VideoRecording? {
        val existing = videoRecordingRepository.getById(id) ?: return null
        val updated = existing.copy(
            endedAt = endedAt ?: existing.endedAt,
            filePath = filePath ?: existing.filePath,
            fileSizeBytes = fileSizeBytes ?: existing.fileSizeBytes,
            durationSec = durationSec ?: existing.durationSec
        )
        return videoRecordingRepository.save(updated)
    }

    override fun delete(id: Long) {
        videoRecordingRepository.delete(id)
    }
}
