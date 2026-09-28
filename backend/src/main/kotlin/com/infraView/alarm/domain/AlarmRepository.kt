package com.infraView.alarm.domain

interface AlarmRepository {
    fun getById(id: Long): Alarm?
    fun getAll(): List<Alarm>
    fun save(alarm: Alarm): Alarm
    fun delete(id: Long)
}
