package com.infraView.incident.domain

import java.time.OffsetDateTime

data class Incident(
    val id: Long? = null,
    val code: String,
    val firefighterName: String? = null,
    val description: String? = null,
    val location: String? = null,
    val startedAt: OffsetDateTime,
    val endedAt: OffsetDateTime? = null,
    val status: StatusType
)
