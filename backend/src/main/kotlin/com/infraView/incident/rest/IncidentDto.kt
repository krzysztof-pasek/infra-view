package com.infraView.incident.rest

import com.infraView.incident.domain.StatusType
import com.infraView.incident.domain.Incident
import java.time.OffsetDateTime

data class IncidentDto(
    val id: Long?,
    val code: String,
    val firefighterName: String?,
    val description: String?,
    val location: String?,
    val startedAt: OffsetDateTime,
    val endedAt: OffsetDateTime?,
    val status: StatusType,
    val deviceId: Long?
)

data class IncidentCreateDto(
    val code: String,
    val firefighterName: String?,
    val description: String?,
    val location: String?,
    val startedAt: OffsetDateTime,
    val endedAt: OffsetDateTime?,
    val status: StatusType
) {
    fun toDomain() = Incident(
        code = this.code,
        firefighterName = this.firefighterName,
        description = this.description,
        location = this.location,
        startedAt = this.startedAt,
        endedAt = this.endedAt,
        status = this.status
    )
}

fun Incident.toDto() = IncidentDto(
    id = this.id,
    code = this.code,
    firefighterName = this.firefighterName,
    description = this.description,
    location = this.location,
    startedAt = this.startedAt,
    endedAt = this.endedAt,
    status = this.status,
    deviceId = this.deviceId
)
