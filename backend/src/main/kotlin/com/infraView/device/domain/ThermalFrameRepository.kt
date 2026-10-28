package com.infraView.device.domain

import java.time.OffsetDateTime

interface ThermalFrameRepository {
    fun save(frame: ThermalFrame): ThermalFrame
    fun getLastByIncidentId(incidentId: Long): ThermalFrame?
    fun getFirstByIncidentId(incidentId: Long): ThermalFrame?
    fun getLastByIncidentIdAtOrBefore(incidentId: Long, at: OffsetDateTime): ThermalFrame?
    fun getByIncidentIdAfter(incidentId: Long, after: OffsetDateTime, limit: Int): List<ThermalFrame>
}
