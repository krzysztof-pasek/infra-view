package com.infraView.alarm.rest

import com.infraView.alarm.domain.AlarmThresholdUseCase
import com.infraView.alarm.domain.ThresholdMetric
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@Tag(name = "Alarm Thresholds API", description = "Sensor thresholds that trigger HIGH_TEMPERATURE, HIGH_CO2 and HIGH_TVOC alarms")
@RestController
@RequestMapping("/alarm-thresholds")
class AlarmThresholdController(
    private val useCase: AlarmThresholdUseCase
) {
    @Operation(summary = "Get all alarm thresholds", description = "Returns the threshold, reset margin and enabled flag of every monitored sensor metric.")
    @GetMapping
    fun all(): List<AlarmThresholdDto> {
        return useCase.getAll().map { it.toDto() }
    }

    @Operation(summary = "Get alarm threshold by metric", description = "Returns the threshold configuration of a single sensor metric.")
    @GetMapping("/{metric}")
    fun get(@PathVariable metric: ThresholdMetric): AlarmThresholdDto {
        return useCase.getByMetric(metric)?.toDto()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Alarm threshold for metric $metric not found")
    }

    @Operation(
        summary = "Update alarm threshold",
        description = "Sets the value at which the alarm is triggered, the reset margin the value must drop below the threshold by before the alarm can be triggered again, and whether the threshold is active."
    )
    @PutMapping("/{metric}")
    fun update(@PathVariable metric: ThresholdMetric, @RequestBody threshold: AlarmThresholdUpdateDto): AlarmThresholdDto {
        if (!threshold.threshold.isFinite() || !threshold.resetMargin.isFinite() || threshold.resetMargin < 0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Threshold must be a finite number and reset margin must be a finite non-negative number")
        }
        return useCase.update(threshold.toDomain(metric))?.toDto()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Alarm threshold for metric $metric not found")
    }
}
