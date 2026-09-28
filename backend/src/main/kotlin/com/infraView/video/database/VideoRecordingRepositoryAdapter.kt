package com.infraView.video.database

import com.infraView.incident.database.IncidentJpaEntity
import com.infraView.video.domain.VideoRecordingRepository
import com.infraView.video.domain.VideoRecording
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class VideoRecordingRepositoryAdapter(
    private val jpaRepository: VideoRecordingJpaRepository,
    private val entityManager: EntityManager
) : VideoRecordingRepository {

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
            jpaRepository.findByIdOrNull(video.id)?.apply {
                this.endedAt = video.endedAt
                this.filePath = video.filePath
                this.fileSizeBytes = video.fileSizeBytes
                this.durationSec = video.durationSec
            } ?: throw IllegalStateException("Video recording with ID ${video.id} does not exist")
        }
        
        val savedEntity = jpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    override fun getById(id: Long): VideoRecording? {
        return jpaRepository.findByIdOrNull(id)?.toDomain()
    }

    override fun getAll(): List<VideoRecording> {
        return jpaRepository.findAll().map { it.toDomain() }
    }

    override fun getByIncidentId(incidentId: Long): List<VideoRecording> {
        return jpaRepository.findByIncidentId(incidentId).map { it.toDomain() }
    }

    override fun delete(id: Long) {
        jpaRepository.deleteById(id)
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
