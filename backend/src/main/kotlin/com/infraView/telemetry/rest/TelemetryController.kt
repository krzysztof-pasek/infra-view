package com.infraView.telemetry.rest

import com.infraView.telemetry.domain.ManageTelemetryUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@Tag(name = "Telemetry API", description = "Telemetry data management")
@RequestMapping("/telemetry")
@RestController
class TelemetryController (
    private val useCase: ManageTelemetryUseCase
) {

    @Operation(summary = "Get telemetry by ID", description = "Retrieves details of a single telemetry measurement.")
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): TelemetryDto {
        return useCase.getById(id)?.toDto()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Pomiar o ID $id nie istnieje")
    }

    @Operation(summary = "Add new telemetry measurement", description = "Saves fresh sensor data.")
    @PostMapping
    fun addTelemetry(@RequestBody dto: TelemetryCreateDto): TelemetryDto {
        return useCase.add(dto.toDomain()).toDto()
    }

    @Operation(summary = "Get telemetry by incident ID", description = "Returns all telemetry measurements assigned to a specific incident.")
    @GetMapping("/incident/{incidentId}")
    fun getByIncidentId(@PathVariable incidentId: Long): List<TelemetryDto> {
        return useCase.getByIncidentId(incidentId).map { it.toDto() }
    }

    @Operation(summary = "Delete telemetry measurement", description = "Removes a single telemetry entry from the database.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTelemetry(@PathVariable id: Long) {
        useCase.delete(id)
    }
}