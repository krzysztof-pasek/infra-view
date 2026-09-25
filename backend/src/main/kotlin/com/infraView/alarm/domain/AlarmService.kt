package com.infraView.alarm.domain

import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneOffset

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
        val alarm = alarmPort.getById(id) ?: return null
        if (alarm.resolvedAt != null) return alarm
        return alarmPort.save(alarm.copy(resolvedAt = OffsetDateTime.now(ZoneOffset.UTC)))
    }

    override fun delete(id: Long) {
        alarmPort.delete(id)
    }
}
