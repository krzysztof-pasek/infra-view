package com.infraView.telemetry.rest

import com.infraView.telemetry.domain.TelemetryUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime

@Tag(name = "Telemetry API", description = "Telemetry data management")
@RequestMapping("/telemetry")
@RestController
class TelemetryController (
    private val useCase: TelemetryUseCase
) {
    private val log = LoggerFactory.getLogger(TelemetryController::class.java)

    @Operation(summary = "Get telemetry by ID", description = "Retrieves details of a single telemetry measurement.")
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): TelemetryDto {
        return useCase.getById(id)?.toDto()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Telemetry measurement with ID $id not found")
    }

    @Operation(summary = "Add new telemetry measurement", description = "Saves fresh sensor data.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun addTelemetry(@RequestBody dto: TelemetryCreateDto): TelemetryDto {
        val saved = useCase.add(dto.toDomain())
        log.info("Telemetry added via REST: telemetryId={}, incidentId={}", saved.id, saved.incidentId)
        return saved.toDto()
    }

    @Operation(
        summary = "Get telemetry by incident ID",
        description = "Without from, returns the latest telemetry measurements of a specific incident (limit, default 600, max 3600). " +
            "Helmets send one measurement per second, so the default limit covers the last 10 minutes. " +
            "With from (ISO-8601), returns all measurements recorded at or after from and, if to is given, before to (half-open range [from, to)); " +
            "limit cannot be combined with from. Measurements are always ordered from oldest to newest."
    )
    @GetMapping("/incident/{incidentId}")
    fun getByIncidentId(
        @PathVariable incidentId: Long,
        @RequestParam(required = false) limit: Int?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: OffsetDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: OffsetDateTime?
    ): List<TelemetryDto> {
        if (from == null) {
            if (to != null) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "to requires from")
            }
            val effectiveLimit = limit ?: DEFAULT_LIMIT
            if (effectiveLimit !in 1..MAX_LIMIT) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and $MAX_LIMIT")
            }
            return useCase.getByIncidentId(incidentId, effectiveLimit).map { it.toDto() }
        }
        if (limit != null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "limit cannot be combined with from")
        }
        if (to != null && !from.isBefore(to)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be before to")
        }
        return useCase.getByIncidentIdBetween(incidentId, from, to).map { it.toDto() }
    }

    @Operation(summary = "Delete telemetry measurement", description = "Removes a single telemetry entry from the database.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTelemetry(@PathVariable id: Long) {
        useCase.delete(id)
    }

    companion object {
        private const val DEFAULT_LIMIT = 600
        private const val MAX_LIMIT = 3600
    }
}
