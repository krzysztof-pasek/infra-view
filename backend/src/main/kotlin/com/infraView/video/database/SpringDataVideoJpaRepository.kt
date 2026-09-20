package com.infraView.video.database
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SpringDataVideoJpaRepository : JpaRepository<VideoRecordingJpaEntity, Long> {
    fun findByIncidentId(incidentId: Long): List<VideoRecordingJpaEntity>
}
