package com.infraView.incident.rest

import com.infraView.incident.domain.IncidentStatus
import com.infraView.incident.domain.Incident
import java.time.OffsetDateTime

data class IncidentDto(
    val id: Long?,
    val code: String,
    val startedAt: OffsetDateTime,
    val endedAt: OffsetDateTime?,
    val status: IncidentStatus,
    val deviceId: Long?
)

data class IncidentCreateDto(
    val code: String,
    val startedAt: OffsetDateTime,
    val endedAt: OffsetDateTime?,
    val status: IncidentStatus
) {
    fun toDomain() = Incident(
        code = this.code,
        startedAt = this.startedAt,
        endedAt = this.endedAt,
        status = this.status
    )
}

data class IncidentUpdateDto(
    val code: String
)

fun Incident.toDto() = IncidentDto(
    id = this.id,
    code = this.code,
    startedAt = this.startedAt,
    endedAt = this.endedAt,
    status = this.status,
    deviceId = this.deviceId
)
