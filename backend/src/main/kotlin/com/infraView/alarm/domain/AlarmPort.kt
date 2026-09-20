package com.infraView.alarm.domain

interface AlarmPort {
    fun getById(id: Int): Alarm?
    fun getAll(): List<Alarm>
    fun save(alarm: Alarm): Alarm
    fun deleteById(id: Int)
}
