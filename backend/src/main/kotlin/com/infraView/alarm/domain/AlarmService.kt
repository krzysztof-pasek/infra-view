package com.infraView.alarm.domain

import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class AlarmService(
    private val alarmPort: AlarmPort
) : ManageAlarmUseCase {

    override fun getById(id: Long): Alarm? {
        return alarmPort.getById(id)
    }

    override fun getAll(): List<Alarm> {
        return alarmPort.getAll()
    }

    override fun triggerAlarm(alarm: Alarm): Alarm {
        return alarmPort.save(alarm)
    }

    override fun resolveAlarm(id: Long): Alarm? {
        val alarm = alarmPort.getById(id) ?: throw RuntimeException("Alarm not found")
        alarm.resolvedAt = OffsetDateTime.now()
        return alarmPort.save(alarm)
    }

    override fun delete(id: Long) {
        alarmPort.deleteById(id)
    }
}
