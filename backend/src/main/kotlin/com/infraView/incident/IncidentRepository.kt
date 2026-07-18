package com.infraView.incident

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository


@Repository
interface IncidentRepository: JpaRepository<IncidentEntity, Int> {
}