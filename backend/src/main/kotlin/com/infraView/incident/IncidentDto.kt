package com.infraView.incident

import java.time.OffsetDateTime

data class IncidentDto(
    val id: Int?,
    val code: String,
    val firefighterName: String?,
    val description: String?,
    val location: String?,
    val startedAt: OffsetDateTime,
    val endedAt: OffsetDateTime?,
    val status: StatusType
)

data class IncidentCreateDto(
    val code: String,
    val firefighterName: String?,
    val description: String?,
    val location: String?,
    val startedAt: OffsetDateTime,
    val endedAt: OffsetDateTime?,
    val status: StatusType
)

fun IncidentEntity.toDto() = IncidentDto(
    id = this.id,
    code = this.code,
    firefighterName = this.firefighterName,
    description = this.description,
    location = this.location,
    startedAt = this.startedAt,
    endedAt = this.endedAt,
    status = this.status
)
