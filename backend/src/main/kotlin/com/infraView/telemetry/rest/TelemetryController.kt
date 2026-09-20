package com.infraView.telemetry.rest

import com.infraView.telemetry.domain.ManageTelemetryUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@Tag(name = "Telemetry API", description = "Odbiór i przeglądanie danych z czujników strażaka")
@RequestMapping("/telemetry")
@RestController
class TelemetryController (
    private val useCase: ManageTelemetryUseCase
) {

    @Operation(summary = "Pobierz pomiar po ID", description = "Zwraca szczegóły pojedynczego pomiaru z czujników.")
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): TelemetryDto {
        return useCase.getById(id)?.toDto()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Pomiar o ID $id nie istnieje")
    }

    @Operation(summary = "Dodaj nowy pomiar", description = "Oczekiwane wywołanie z Raspberry Pi co jakść część sekundy. Zapisuje najświeższe dane z czujników.")
    @PostMapping
    fun addTelemetry(@RequestBody dto: TelemetryCreateDto): TelemetryDto {
        return useCase.add(dto.toDomain()).toDto()
    }

    @Operation(summary = "Pobierz telemetrię dla zdarzenia", description = "Zwraca wszystkie pomiary przypisane do konkretnego zdarzenia (incidentId).")
    @GetMapping("/incident/{incidentId}")
    fun getByIncidentId(@PathVariable incidentId: Int): List<TelemetryDto> {
        return useCase.getByIncidentId(incidentId).map { it.toDto() }
    }

    @Operation(summary = "Usuń pomiar", description = "Usuwa pojedynczy wpis telemetryczny (np. błędny lub testowy z bazy).")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTelemetry(@PathVariable id: Long) {
        useCase.delete(id)
    }
}