package com.infraView.video.rest

import com.infraView.video.domain.VideoRecordingUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Video API", description = "Thermal video recordings of incidents")
@RequestMapping("/videos")
@RestController
class VideoRecordingController(
    private val useCase: VideoRecordingUseCase
) {

    @Operation(
        summary = "Get video recordings by incident ID",
        description = "Returns the thermal recording of an incident, built from its stored frames: one entry spanning the first to the last frame, or an empty list when the incident has no frames. endedAt is null while the incident is in progress. Play it with /incidents/{incidentId}/frame and /incidents/{incidentId}/recording."
    )
    @GetMapping("/incident/{incidentId}")
    fun getByIncidentId(@PathVariable incidentId: Long): List<VideoRecordingDto> {
        return useCase.getByIncidentId(incidentId).map { it.toDto() }
    }
}
