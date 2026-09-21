package com.infraView.incident.rest

import com.infraView.incident.domain.ManageIncidentUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@Tag(name = "Incidents API", description = "Incident management")
@RestController
@RequestMapping("/incidents")
class IncidentController(
    private val useCase: ManageIncidentUseCase,
) {

    @Operation(summary = "Get all incidents", description = "Returns the full history of all incidents in the system.")
    @GetMapping
    fun getIncidents(): List<IncidentDto> {
        return useCase.getAll().map { it.toDto() }
    }

    @Operation(summary = "Get incident by ID", description = "Retrieves detailed information about a specific incident based on its unique identifier.")
    @GetMapping("/{id}")
    fun getIncident(@PathVariable id: Long): IncidentDto {
        return useCase.getById(id)?.toDto() 
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found")
    }

    @Operation(summary = "Report a new incident", description = "Creates a new incident in the system.")
    @PostMapping
    fun createIncident(@RequestBody dto: IncidentCreateDto): IncidentDto {
        return useCase.save(dto.toDomain()).toDto()
    }

    @Operation(summary = "End an incident", description = "Changes the incident status to resolved and sets ended_at to the current date.")
    @PutMapping("/{id}/end")
    fun endIncident(@PathVariable id: Long): IncidentDto {
        return useCase.endIncident(id)?.toDto()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found")
    }

    @Operation(summary = "Delete an incident", description = "Removes an incident from the database.")
    @DeleteMapping("/{id}")
    fun deleteIncident(@PathVariable id: Long) {
        useCase.delete(id)
    }
}