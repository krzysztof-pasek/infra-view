package com.infraView.alarm.rest

import com.infraView.alarm.domain.Alarm
import com.infraView.alarm.domain.AlarmType
import java.time.OffsetDateTime

data class AlarmDto(
    val id: Int?,
    val alarmType: AlarmType,
    val triggeredAt: OffsetDateTime,
    val resolvedAt: OffsetDateTime?,
    val incidentId: Int
)

data class AlarmCreateDto(
    val alarmType: AlarmType,
    val triggeredAt: OffsetDateTime,
    val incidentId: Int
) {
    fun toDomain() = Alarm(
        alarmType = this.alarmType,
        triggeredAt = this.triggeredAt,
        incidentId = this.incidentId
    )
}

fun Alarm.toDto() = AlarmDto(
    id = this.id,
    alarmType = this.alarmType,
    triggeredAt = this.triggeredAt,
    resolvedAt = this.resolvedAt,
    incidentId = this.incidentId
)
