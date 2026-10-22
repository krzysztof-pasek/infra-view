package com.infraView.alarm.domain

interface AlarmUseCase {
    fun getById(id: Long): Alarm?
    fun getAll(): List<Alarm>
    fun getActive(): List<Alarm>
    fun getByIncidentId(incidentId: Long): List<Alarm>
    fun triggerAlarm(alarm: Alarm): Alarm
    fun resolveAlarm(id: Long): Alarm?
    fun delete(id: Long)
}
