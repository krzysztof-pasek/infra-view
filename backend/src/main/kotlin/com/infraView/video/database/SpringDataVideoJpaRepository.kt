package com.infraView.video.database
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SpringDataVideoJpaRepository : JpaRepository<VideoRecordingJpaEntity, Int> {
    fun findAllByIncidentId(incidentId: Int): List<VideoRecordingJpaEntity>
}
