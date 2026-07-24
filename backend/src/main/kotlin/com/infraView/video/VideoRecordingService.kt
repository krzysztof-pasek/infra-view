package com.infraView.video

import com.infraView.incident.IncidentEntity
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class VideoRecordingService(
    private val videoRecordingRepository: VideoRecordingRepository,
    private val entityManager: EntityManager
) {
    fun getAll(): List<VideoRecordingDto> {
        return videoRecordingRepository.findAll().map { it.toDto() }
    }

    fun getById(id: Int): VideoRecordingDto? {
        return videoRecordingRepository.findByIdOrNull(id)?.toDto()
    }

    fun getByIncidentId(incidentId: Int): List<VideoRecordingDto> {
        return videoRecordingRepository.findAllByIncidentId(incidentId).map { it.toDto() }
    }

    fun startRecording(dto: VideoRecordingCreateDto): VideoRecordingDto {
        val incidentRef = entityManager.getReference(IncidentEntity::class.java, dto.incidentId)
        
        val entity = VideoRecordingEntity(
            incident = incidentRef,
            startedAt = dto.startedAt
        )
        return videoRecordingRepository.save(entity).toDto()
    }

    fun updateRecording(id: Int, dto: VideoRecordingUpdateDto): VideoRecordingDto? {
        val existing = videoRecordingRepository.findByIdOrNull(id) ?: return null
        
        dto.endedAt?.let { existing.endedAt = it }
        dto.storageKey?.let { existing.storageKey = it }
        dto.fileSizeBytes?.let { existing.fileSizeBytes = it }
        dto.durationSec?.let { existing.durationSec = it }

        return videoRecordingRepository.save(existing).toDto()
    }

    fun delete(id: Int) {
        videoRecordingRepository.deleteById(id)
    }
}
