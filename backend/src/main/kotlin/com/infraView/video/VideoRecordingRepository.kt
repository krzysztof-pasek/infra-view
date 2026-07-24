package com.infraView.video

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VideoRecordingRepository: JpaRepository<VideoRecordingEntity, Int> {
    fun findAllByIncidentId(incidentId: Int): List<VideoRecordingEntity>
}
