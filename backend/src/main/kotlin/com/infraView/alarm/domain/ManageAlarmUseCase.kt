package com.infraView.alarm.domain

interface ManageAlarmUseCase {
    fun getById(id: Long): Alarm?
    fun getAll(): List<Alarm>
    fun triggerAlarm(alarm: Alarm): Alarm
    fun resolveAlarm(id: Long): Alarm?
    fun delete(id: Long)
}
