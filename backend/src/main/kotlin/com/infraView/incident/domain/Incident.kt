package com.infraView.incident.domain

import com.infraView.incident.StatusType
import java.time.OffsetDateTime

data class Incident (
    val id: Long? = null,
    val code: String,
    val firefighterName: String? = null,
    val description: String? = null,
    val location: String? = null,
    val startedAt: OffsetDateTime,
    var endedAt: OffsetDateTime? = null,
    var status: StatusType
)
