package com.infraView.alarm.domain

import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class AlarmService(
    private val alarmRepository: AlarmRepository
) : AlarmUseCase {

    override fun getById(id: Long): Alarm? {
        return alarmRepository.getById(id)
    }

    override fun getAll(): List<Alarm> {
        return alarmRepository.getAll()
    }

    override fun triggerAlarm(alarm: Alarm): Alarm {
        return alarmRepository.save(alarm)
    }

    override fun resolveAlarm(id: Long): Alarm? {
        val alarm = alarmRepository.getById(id) ?: return null
        if (alarm.resolvedAt != null) return alarm
        return alarmRepository.save(alarm.copy(resolvedAt = OffsetDateTime.now(ZoneOffset.UTC)))
    }

    override fun delete(id: Long) {
        alarmRepository.delete(id)
    }
}
