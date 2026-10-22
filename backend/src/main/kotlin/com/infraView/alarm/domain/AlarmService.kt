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

    override fun getActive(): List<Alarm> {
        return alarmRepository.getActive()
    }

    override fun getByIncidentId(incidentId: Long): List<Alarm> {
        return alarmRepository.getByIncidentId(incidentId)
    }

    override fun triggerAlarm(alarm: Alarm): Alarm {
        return alarmRepository.save(alarm)
    }

    override fun resolveAlarm(id: Long): Alarm? {
        val alarm = alarmRepository.getById(id) ?: return null
        if (alarm.resolvedAt != null) return alarm
        return alarmRepository.save(alarm.copy(resolvedAt = OffsetDateTime.now(ZoneOffset.UTC)))
    }

    override fun resolveByIncidentId(incidentId: Long): List<Alarm> {
        val resolvedAt = OffsetDateTime.now(ZoneOffset.UTC)
        return alarmRepository.getByIncidentId(incidentId)
            .filter { it.resolvedAt == null }
            .map { alarmRepository.save(it.copy(resolvedAt = resolvedAt)) }
    }

    override fun delete(id: Long) {
        alarmRepository.delete(id)
    }
}
