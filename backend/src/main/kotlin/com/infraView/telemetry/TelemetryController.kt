package com.infraView.telemetry

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@Tag(name = "Telemetry API", description = "Odbiór i przeglądanie danych z czujników strażaka")
@RequestMapping("/telemetry")
@RestController
class TelemetryController (
    private val telemetryService: TelemetryService
) {

    @Operation(summary = "Pobierz całą telemetrię", description = "Zwraca wszystkie zarejestrowane pomiary ze wszystkich zdarzeń.")
    @GetMapping
    fun getAll() : List<TelemetryDto>{
        return telemetryService.getAll()
    }

    @Operation(summary = "Pobierz pomiar po ID", description = "Zwraca szczegóły pojedynczego pomiaru z czujników.")
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): TelemetryDto {
        return telemetryService.getById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Pomiar o ID $id nie istnieje")
    }

    @Operation(summary = "Dodaj nowy pomiar", description = "Oczekiwane wywołanie z Raspberry Pi co 0.5 sekundy. Zapisuje najświeższe dane z czujników.")
    @PostMapping
    fun addTelemetry(@RequestBody dto: TelemetryCreateDto): TelemetryDto {
        return telemetryService.add(dto)
    }

    @Operation(summary = "Pobierz telemetrię dla zdarzenia", description = "Zwraca wszystkie pomiary przypisane do konkretnego zdarzenia (incidentId).")
    @GetMapping("/incident/{incidentId}")
    fun getByIncidentId(@PathVariable incidentId: Int): List<TelemetryDto> {
        return telemetryService.getByIncidentId(incidentId)
    }


    @Operation(summary = "Usuń pomiar", description = "Usuwa pojedynczy wpis telemetryczny (np. błędny lub testowy z bazy).")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTelemetry(@PathVariable id: Long) {
        telemetryService.delete(id)
    }
}