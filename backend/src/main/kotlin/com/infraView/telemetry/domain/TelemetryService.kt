package com.infraView.telemetry.domain

import com.infraView.alarm.domain.Alarm
import com.infraView.alarm.domain.AlarmType
import com.infraView.alarm.domain.ManageAlarmUseCase
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service


/**
 * Serwis domenowy telemetrii — przyjmuje pomiary z czujników kasku,
 * zapisuje je i uruchamia algorytm detekcji upadku.
 */
@Service
class TelemetryService(
    private val telemetryPort: TelemetryPort,
    private val alarmUseCase: ManageAlarmUseCase
) : ManageTelemetryUseCase {

    private val log = LoggerFactory.getLogger(TelemetryService::class.java)
    private val fallDetectionEngine = FallDetectionEngine()

    override fun getById(id: Long): Telemetry? {
        return telemetryPort.getById(id)
    }

    override fun getByIncidentId(incidentId: Long): List<Telemetry> {
        return telemetryPort.getAllByIncidentId(incidentId)
    }

    override fun add(telemetry: Telemetry): Telemetry {
        val saved = telemetryPort.save(telemetry)

        val result = fallDetectionEngine.evaluate(saved)
        if (result == FallDetectionResult.FALL_DETECTED) {
            log.warn(
                "Wykryto upadek strażaka! incidentId={}, recordedAt={}",
                saved.incidentId,
                saved.recordedAt
            )
            alarmUseCase.triggerAlarm(
                Alarm(
                    alarmType = AlarmType.FALL,
                    triggeredAt = saved.recordedAt,
                    incidentId = saved.incidentId!!
                )
            )
        }

        return saved
    }

    override fun delete(id: Long) {
        telemetryPort.deleteById(id)
    }
}