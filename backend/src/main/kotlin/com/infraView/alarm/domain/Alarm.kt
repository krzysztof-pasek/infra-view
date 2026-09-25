package com.infraView.alarm.domain

import java.time.OffsetDateTime

data class Alarm(
    val id: Long? = null,
    val alarmType: AlarmType,
    val triggeredAt: OffsetDateTime,
    val resolvedAt: OffsetDateTime? = null,
    val incidentId: Long
)
