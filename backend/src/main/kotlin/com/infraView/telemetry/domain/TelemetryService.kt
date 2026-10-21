package com.infraView.telemetry.domain

import com.infraView.alarm.domain.Alarm
import com.infraView.alarm.domain.AlarmThresholdUseCase
import com.infraView.alarm.domain.AlarmType
import com.infraView.alarm.domain.AlarmUseCase
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneOffset


@Service
class TelemetryService(
    private val telemetryRepository: TelemetryRepository,
    private val alarmUseCase: AlarmUseCase,
    private val alarmThresholdUseCase: AlarmThresholdUseCase
) : TelemetryUseCase {

    private val log = LoggerFactory.getLogger(TelemetryService::class.java)

    override fun getById(id: Long): Telemetry? {
        return telemetryRepository.getById(id)
    }

    override fun getByIncidentId(incidentId: Long): List<Telemetry> {
        return telemetryRepository.getByIncidentId(incidentId)
    }

    override fun add(telemetry: Telemetry): Telemetry {
        val deviceId = telemetry.deviceId ?: return telemetryRepository.save(telemetry)
        val history = telemetryRepository.getRecentByDeviceId(deviceId, MotionDetector.HISTORY_SIZE)
            .filter { it.incidentId == telemetry.incidentId }
        val state = MotionDetector.evaluate(history, telemetry)
        val saved = telemetryRepository.save(telemetry.copy(motionState = state?.name))
        val incidentId = saved.incidentId ?: return saved

        val motionAlarmType = when (state) {
            MotionState.FALLEN -> AlarmType.FALL
            MotionState.NO_MOTION -> AlarmType.NO_MOTION
            else -> null
        }
        if (motionAlarmType != null && history.lastOrNull()?.motionState != state?.name) {
            trigger(motionAlarmType, deviceId, incidentId)
        }

        val thresholdAlarmTypes = alarmThresholdUseCase.getAll()
            .filter { ThresholdDetector.shouldTrigger(history, telemetry, it) }
            .map { it.metric.alarmType }
            .toSet()
        if (thresholdAlarmTypes.isNotEmpty()) {
            val activeAlarmTypes = alarmUseCase.getUnresolvedByIncidentId(incidentId).map { it.alarmType }.toSet()
            (thresholdAlarmTypes - activeAlarmTypes).forEach { trigger(it, deviceId, incidentId) }
        }
        return saved
    }

    override fun delete(id: Long) {
        telemetryRepository.delete(id)
    }

    private fun trigger(alarmType: AlarmType, deviceId: Long, incidentId: Long) {
        val alarm = alarmUseCase.triggerAlarm(
            Alarm(alarmType = alarmType, triggeredAt = OffsetDateTime.now(ZoneOffset.UTC), incidentId = incidentId)
        )
        log.warn("Alarm triggered: type={}, deviceId={}, incidentId={}, alarmId={}", alarmType, deviceId, incidentId, alarm.id)
    }

}
