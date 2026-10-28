package com.infraView.incident.domain

import java.time.OffsetDateTime

data class Incident(
    val id: Long? = null,
    val code: String,
    val startedAt: OffsetDateTime,
    val endedAt: OffsetDateTime? = null,
    val status: IncidentStatus,
    val deviceId: Long? = null
)
