package com.infraView.video.rest

import com.infraView.video.domain.ManageVideoRecordingUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@Tag(name = "Video API", description = "Zarządzanie nagraniami wideo z akcji")
@RequestMapping("/videos")
@RestController
class VideoRecordingController(
    private val useCase: ManageVideoRecordingUseCase
) {

    @Operation(summary = "Pobierz wszystkie nagrania", description = "Zwraca listę wszystkich nagrań w systemie.")
    @GetMapping
    fun getAll(): List<VideoRecordingDto> {
        return useCase.getAll().map { it.toDto() }
    }

    @Operation(summary = "Pobierz nagranie po ID")
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): VideoRecordingDto {
        return useCase.getById(id)?.toDto()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Nagranie o ID $id nie istnieje")
    }

    @Operation(summary = "Pobierz nagrania dla zdarzenia", description = "Zwraca wszystkie nagrania powiązane z daną akcją.")
    @GetMapping("/incident/{incidentId}")
    fun getByIncidentId(@PathVariable incidentId: Long): List<VideoRecordingDto> {
        return useCase.getByIncidentId(incidentId).map { it.toDto() }
    }

    @Operation(summary = "Rozpocznij nagranie", description = "Rejestruje fakt rozpoczęcia nagrywania")
    @PostMapping
    fun startRecording(@RequestBody dto: VideoRecordingCreateDto): VideoRecordingDto {
        return useCase.startRecording(dto.incidentId, dto.startedAt).toDto()
    }

    @Operation(summary = "Zaktualizuj nagranie (np. zakończ)", description = "Służy do aktualizacji metadanych po zakończeniu nagrywania")
    @PatchMapping("/{id}")
    fun updateRecording(@PathVariable id: Long, @RequestBody dto: VideoRecordingUpdateDto): VideoRecordingDto {
        return useCase.updateRecording(id, dto.endedAt, dto.storageKey, dto.fileSizeBytes, dto.durationSec)?.toDto()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Nagranie o ID $id nie istnieje")
    }

    @Operation(summary = "Usuń nagranie", description = "Usuwa metadane nagrania z bazy.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteRecording(@PathVariable id: Long) {
        useCase.delete(id)
    }
}
