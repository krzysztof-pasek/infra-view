package com.infraView.alarm

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@Tag(name = "Alarms API", description = "Alarm management")
@RestController
@RequestMapping("/alarms")
class AlarmController(
    private val alarmService: AlarmService
) {
    @Operation(summary = "Get all alarms", description = "Returns the full history of all alarms across all incidents in the system.")
    @GetMapping
    fun all() : List<AlarmDto>{
        return alarmService.findAll()
    }

    @Operation(summary = "Get alarm by ID", description = "Retrieves detailed information about a specific alarm based on its unique identifier.")
    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): AlarmDto {
        return alarmService.findById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Alarm with ID $id not found")
    }

    @Operation(summary = "Report a new alarm", description = "Used by sensors to report a new event within an existing incident.")
    @PostMapping
    fun add(@RequestBody alarm: AlarmCreateDto) : AlarmDto{
        return alarmService.add(alarm)
    }

    @Operation(summary = "Resolve an alarm", description = "Changes the alarm status to resolved (sets resolved_at to the current date).")
    @PutMapping("/{id}/resolve")
    fun resolve(@PathVariable id: Long): AlarmDto {
        return alarmService.resolve(id)
    }

    @Operation(summary = "Delete an alarm", description = "Permanently removes an alarm from the database.")
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long) {
        alarmService.delete(id)
    }

}