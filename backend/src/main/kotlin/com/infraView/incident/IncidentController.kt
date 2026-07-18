package com.infraView.incident

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/incidents")
class IncidentController(
    private val incidentService: IncidentService,
) {

    @GetMapping
    fun getIncidents(): List<IncidentDto> {
        return incidentService.getAll()
    }

    @GetMapping("/{id}")
    fun getIncident(@PathVariable id: Int): IncidentDto {
        return incidentService.getById(id) 
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found")
    }

    @PostMapping
    fun createIncident(@RequestBody dto: IncidentCreateDto): IncidentDto {
        return incidentService.add(dto)
    }

    @PutMapping("/{id}/end")
    fun endIncident(@PathVariable id: Int): IncidentDto {
        return incidentService.endIncident(id)
    }

    @DeleteMapping("/{id}")
    fun deleteIncident(@PathVariable id: Int) {
        incidentService.delete(id)
    }
}