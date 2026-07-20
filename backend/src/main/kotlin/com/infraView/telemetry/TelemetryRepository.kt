package com.infraView.telemetry

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository


@Repository
interface TelemetryRepository: JpaRepository<TelemetryEntity, Long> {

}