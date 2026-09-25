package com.infraView.video.database

import com.infraView.incident.database.IncidentJpaEntity
import com.infraView.video.domain.VideoRecordingPort
import com.infraView.video.domain.VideoRecording
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class VideoPersistenceAdapter(
    private val springDataRepository: SpringDataVideoJpaRepository,
    private val entityManager: EntityManager
) : VideoRecordingPort {

    override fun save(video: VideoRecording): VideoRecording {
        val incidentRef = entityManager.getReference(IncidentJpaEntity::class.java, video.incidentId)
        
        val entity = if (video.id == null) {
            VideoRecordingJpaEntity(
                incident = incidentRef,
                startedAt = video.startedAt,
                endedAt = video.endedAt,
                filePath = video.filePath,
                fileSizeBytes = video.fileSizeBytes,
                durationSec = video.durationSec
            )
        } else {
            springDataRepository.findByIdOrNull(video.id)?.apply {
                this.endedAt = video.endedAt
                this.filePath = video.filePath
                this.fileSizeBytes = video.fileSizeBytes
                this.durationSec = video.durationSec
            } ?: throw IllegalStateException("Video recording with ID ${video.id} does not exist")
        }
        
        val savedEntity = springDataRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun getById(id: Long): VideoRecording? {
        return springDataRepository.findByIdOrNull(id)?.toDomain()
    }

    override fun getAll(): List<VideoRecording> {
        return springDataRepository.findAll().map { it.toDomain() }
    }

    override fun getByIncidentId(incidentId: Long): List<VideoRecording> {
        return springDataRepository.findByIncidentId(incidentId).map { it.toDomain() }
    }

    override fun delete(id: Long) {
        springDataRepository.deleteById(id)
    }

    private fun VideoRecordingJpaEntity.toDomain() = VideoRecording(
        id = this.id,
        incidentId = this.incident.id ?: throw IllegalStateException("Incident must have an ID"),
        startedAt = this.startedAt,
        endedAt = this.endedAt,
        filePath = this.filePath,
        fileSizeBytes = this.fileSizeBytes,
        durationSec = this.durationSec
    )
}
