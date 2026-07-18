package com.infraView.incident

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@Tag(name = "Incidents API", description = "Incident management")
@RestController
@RequestMapping("/incidents")
class IncidentController(
    private val incidentService: IncidentService,
) {

    @Operation(summary = "Get all incidents", description = "Returns the full history of all incidents in the system.")
    @GetMapping
    fun getIncidents(): List<IncidentDto> {
        return incidentService.getAll()
    }

    @Operation(summary = "Get incident by ID", description = "Retrieves detailed information about a specific incident based on its unique identifier.")
    @GetMapping("/{id}")
    fun getIncident(@PathVariable id: Int): IncidentDto {
        return incidentService.getById(id) 
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found")
    }

    @Operation(summary = "Report a new incident", description = "Creates a new incident in the system.")
    @PostMapping
    fun createIncident(@RequestBody dto: IncidentCreateDto): IncidentDto {
        return incidentService.add(dto)
    }

    @Operation(summary = "End an incident", description = "Changes the incident status to resolved and sets ended_at to the current date.")
    @PutMapping("/{id}/end")
    fun endIncident(@PathVariable id: Int): IncidentDto {
        return incidentService.endIncident(id)
    }

    @Operation(summary = "Delete an incident", description = "Permanently removes an incident from the database.")
    @DeleteMapping("/{id}")
    fun deleteIncident(@PathVariable id: Int) {
        incidentService.delete(id)
    }
}