package com.infraView.alarm.rest

import com.infraView.alarm.domain.ManageAlarmUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@Tag(name = "Alarms API", description = "Alarm management")
@RestController
@RequestMapping("/alarms")
class AlarmController(
    private val useCase: ManageAlarmUseCase
) {
    @Operation(summary = "Get all alarms", description = "Returns the full history of all alarms across all incidents in the system.")
    @GetMapping
    fun all(): List<AlarmDto> {
        return useCase.getAll().map { it.toDto() }
    }

    @Operation(summary = "Get alarm by ID", description = "Retrieves detailed information about a specific alarm based on its unique identifier.")
    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): AlarmDto {
        return useCase.getById(id)?.toDto()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Alarm with ID $id not found")
    }

    @Operation(summary = "Report a new alarm", description = "Used by sensors to report a new event within an existing incident.")
    @PostMapping
    fun add(@RequestBody alarm: AlarmCreateDto): AlarmDto {
        return useCase.triggerAlarm(alarm.toDomain()).toDto()
    }

    @Operation(summary = "Resolve an alarm", description = "Changes the alarm status to resolved and sets the resolution timestamp.")
    @PutMapping("/{id}/resolve")
    fun resolve(@PathVariable id: Long): AlarmDto {
        return useCase.resolveAlarm(id)?.toDto()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Alarm with ID $id not found")
    }

    @Operation(summary = "Delete an alarm", description = "Removes an alarm from the database.")
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long) {
        useCase.delete(id)
    }
}