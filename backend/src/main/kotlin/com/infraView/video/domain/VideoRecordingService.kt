package com.infraView.video.domain

import com.infraView.incident.domain.IncidentStatus
import com.infraView.incident.domain.IncidentUseCase
import org.springframework.stereotype.Service

@Service
class VideoRecordingService(
    private val videoRecordingRepository: VideoRecordingRepository,
    private val incidentUseCase: IncidentUseCase
) : VideoRecordingUseCase {

    override fun getByIncidentId(incidentId: Long): List<VideoRecording> {
        val recording = videoRecordingRepository.getByIncidentId(incidentId) ?: return emptyList()
        if (incidentUseCase.getById(incidentId)?.status == IncidentStatus.IN_PROGRESS) {
            return listOf(recording.copy(endedAt = null))
        }
        return listOf(recording)
    }
}
