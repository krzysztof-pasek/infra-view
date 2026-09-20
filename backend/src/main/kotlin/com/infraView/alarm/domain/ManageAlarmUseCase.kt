package com.infraView.alarm.domain

interface ManageAlarmUseCase {
    fun getById(id: Int): Alarm?
    fun getAll(): List<Alarm>
    fun triggerAlarm(alarm: Alarm): Alarm
    fun resolveAlarm(id: Int): Alarm?
    fun delete(id: Int)
}
