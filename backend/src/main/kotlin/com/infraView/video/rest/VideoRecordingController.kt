package com.infraView.video.rest

import com.infraView.video.domain.ManageVideoRecordingUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@Tag(name = "Video API", description = "Video recordings management")
@RequestMapping("/videos")
@RestController
class VideoRecordingController(
    private val useCase: ManageVideoRecordingUseCase
) {

    @Operation(summary = "Get all video recordings", description = "Returns a list of all video recordings in the system.")
    @GetMapping
    fun getAll(): List<VideoRecordingDto> {
        return useCase.getAll().map { it.toDto() }
    }

    @Operation(summary = "Get video recording by ID", description = "Retrieves detailed information about a specific video recording based on its unique identifier.")
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): VideoRecordingDto {
        return useCase.getById(id)?.toDto()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Video recording with ID $id not found")
    }

    @Operation(summary = "Get video recordings by incident ID", description = "Returns all video recordings associated with a specific incident.")
    @GetMapping("/incident/{incidentId}")
    fun getByIncidentId(@PathVariable incidentId: Long): List<VideoRecordingDto> {
        return useCase.getByIncidentId(incidentId).map { it.toDto() }
    }

    @Operation(summary = "Start video recording", description = "Registers the start of a new video recording session.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun startRecording(@RequestBody dto: VideoRecordingCreateDto): VideoRecordingDto {
        return useCase.startRecording(dto.incidentId, dto.startedAt).toDto()
    }

    @Operation(summary = "Update video recording metadata", description = "Updates metadata like end time and file path for a recording.")
    @PatchMapping("/{id}")
    fun updateRecording(@PathVariable id: Long, @RequestBody dto: VideoRecordingUpdateDto): VideoRecordingDto {
        return useCase.updateRecording(id, dto.endedAt, dto.filePath, dto.fileSizeBytes, dto.durationSec)?.toDto()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Video recording with ID $id not found")
    }

    @Operation(summary = "Delete video recording", description = "Removes video recording metadata from the database.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteRecording(@PathVariable id: Long) {
        useCase.delete(id)
    }
}
