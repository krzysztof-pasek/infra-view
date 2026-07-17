package com.infraView.alarm

import java.time.OffsetDateTime

data class AlarmDto(
    val id: Int?,
    val alarmType: AlarmType,
    val triggeredAt: OffsetDateTime,
    val resolvedAt: OffsetDateTime?,
    val incidentId: Int?
)

data class AlarmCreateDto(
    val alarmType: AlarmType,
    val triggeredAt: OffsetDateTime,
    val incidentId: Int
)

fun AlarmEntity.toDto() = AlarmDto(
    id = this.id,
    alarmType = this.alarmType,
    triggeredAt = this.triggeredAt,
    resolvedAt = this.resolvedAt,
    incidentId = this.incident.id
)
