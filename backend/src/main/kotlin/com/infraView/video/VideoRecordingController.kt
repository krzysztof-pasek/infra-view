package com.infraView.video

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@Tag(name = "Video API", description = "Zarządzanie nagraniami wideo z akcji")
@RequestMapping("/videos")
@RestController
class VideoRecordingController(
    private val videoRecordingService: VideoRecordingService
) {

    @Operation(summary = "Pobierz wszystkie nagrania", description = "Zwraca listę wszystkich nagrań w systemie.")
    @GetMapping
    fun getAll(): List<VideoRecordingDto> {
        return videoRecordingService.getAll()
    }

    @Operation(summary = "Pobierz nagranie po ID")
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Int): VideoRecordingDto {
        return videoRecordingService.getById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Nagranie o ID $id nie istnieje")
    }

    @Operation(summary = "Pobierz nagrania dla zdarzenia", description = "Zwraca wszystkie nagrania powiązane z daną akcją.")
    @GetMapping("/incident/{incidentId}")
    fun getByIncidentId(@PathVariable incidentId: Int): List<VideoRecordingDto> {
        return videoRecordingService.getByIncidentId(incidentId)
    }

    @Operation(summary = "Rozpocznij nagranie", description = "Rejestruje fakt rozpoczęcia nagrywania")
    @PostMapping
    fun startRecording(@RequestBody dto: VideoRecordingCreateDto): VideoRecordingDto {
        return videoRecordingService.startRecording(dto)
    }

    @Operation(summary = "Zaktualizuj nagranie (np. zakończ)", description = "Służy do aktualizacji metadanych po zakończeniu nagrywania")
    @PatchMapping("/{id}")
    fun updateRecording(@PathVariable id: Int, @RequestBody dto: VideoRecordingUpdateDto): VideoRecordingDto {
        return videoRecordingService.updateRecording(id, dto)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Nagranie o ID $id nie istnieje")
    }

    @Operation(summary = "Usuń nagranie", description = "Usuwa metadane nagrania z bazy.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteRecording(@PathVariable id: Int) {
        videoRecordingService.delete(id)
    }
}
