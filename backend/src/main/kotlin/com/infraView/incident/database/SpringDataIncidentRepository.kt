package com.infraView.incident.database

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository


@Repository
interface SpringDataIncidentRepository : JpaRepository<IncidentJpaEntity, Long> {
}