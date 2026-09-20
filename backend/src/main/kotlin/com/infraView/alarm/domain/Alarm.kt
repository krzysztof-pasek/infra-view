package com.infraView.alarm.domain

import java.time.OffsetDateTime

data class Alarm(
    val id: Int? = null,
    val alarmType: AlarmType,
    val triggeredAt: OffsetDateTime,
    var resolvedAt: OffsetDateTime? = null,
    val incidentId: Int
)
