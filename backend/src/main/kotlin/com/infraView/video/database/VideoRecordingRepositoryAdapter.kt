package com.infraView.video.database

import com.infraView.video.domain.VideoRecording
import com.infraView.video.domain.VideoRecordingRepository
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class VideoRecordingRepositoryAdapter(
    private val jpaRepository: VideoRecordingJpaRepository
) : VideoRecordingRepository {

    override fun getByIncidentId(incidentId: Long): VideoRecording? {
        val stats = jpaRepository.getStatsByIncidentId(incidentId)
        val startedAt = stats.startedAt ?: return null
        val endedAt = stats.endedAt ?: return null
        return VideoRecording(
            id = incidentId,
            incidentId = incidentId,
            startedAt = startedAt,
            endedAt = endedAt,
            frameCount = stats.frameCount,
            fileSizeBytes = jpaRepository.getImageBytesByIncidentId(incidentId),
            durationSec = Duration.between(startedAt, endedAt).toSeconds().toInt()
        )
    }
}
