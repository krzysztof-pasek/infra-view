package com.infraView.alarm.domain

interface AlarmPort {
    fun getById(id: Long): Alarm?
    fun getAll(): List<Alarm>
    fun save(alarm: Alarm): Alarm
    fun deleteById(id: Long)
}
