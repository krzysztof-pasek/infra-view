package com.infraView.video.rest

import com.infraView.video.domain.ManageVideoRecordingUseCase
import com.infraView.video.domain.VideoRecording
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.OffsetDateTime

@WebMvcTest(VideoRecordingController::class)
class VideoRecordingControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var useCase: ManageVideoRecordingUseCase

    private val startedAt = OffsetDateTime.parse("2026-09-24T10:00:00Z")
    private val video = VideoRecording(
        id = 1L,
        incidentId = 100L,
        startedAt = startedAt
    )

    @Test
    fun `should return all videos`() {
        every { useCase.getAll() } returns listOf(video)

        mockMvc.perform(get("/videos"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].incidentId").value(100))
    }

    @Test
    fun `should return video by id`() {
        every { useCase.getById(1L) } returns video

        mockMvc.perform(get("/videos/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.incidentId").value(100))
            .andExpect(jsonPath("$.startedAt").value("2026-09-24T10:00:00Z"))
            .andExpect(jsonPath("$.endedAt").doesNotExist())
            .andExpect(jsonPath("$.filePath").doesNotExist())
    }

    @Test
    fun `should return 404 when video not found`() {
        every { useCase.getById(99L) } returns null

        mockMvc.perform(get("/videos/99"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return videos by incident id`() {
        every { useCase.getByIncidentId(100L) } returns listOf(video, video.copy(id = 2L))

        mockMvc.perform(get("/videos/incident/100"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[1].id").value(2))
    }

    @Test
    fun `should start video recording`() {
        every { useCase.startRecording(100L, startedAt) } returns video

        mockMvc.perform(
            post("/videos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"incidentId": 100, "startedAt": "2026-09-24T10:00:00Z"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.startedAt").value("2026-09-24T10:00:00Z"))

        verify(exactly = 1) { useCase.startRecording(100L, startedAt) }
    }

    @Test
    fun `should update video recording`() {
        val endedAt = startedAt.plusMinutes(10)
        val filePath = "2026-09-24/incident_100/video_10-00-00.mjpeg"
        every { useCase.updateRecording(1L, endedAt, filePath, 1_048_576L, 600) } returns
            video.copy(endedAt = endedAt, filePath = filePath, fileSizeBytes = 1_048_576L, durationSec = 600)

        mockMvc.perform(
            patch("/videos/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "endedAt": "2026-09-24T10:10:00Z",
                      "filePath": "$filePath",
                      "fileSizeBytes": 1048576,
                      "durationSec": 600
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.endedAt").value("2026-09-24T10:10:00Z"))
            .andExpect(jsonPath("$.filePath").value(filePath))
            .andExpect(jsonPath("$.fileSizeBytes").value(1048576))
            .andExpect(jsonPath("$.durationSec").value(600))
    }

    @Test
    fun `should pass nulls for fields omitted in partial update`() {
        val endedAt = startedAt.plusMinutes(10)
        every { useCase.updateRecording(1L, endedAt, null, null, null) } returns video.copy(endedAt = endedAt)

        mockMvc.perform(
            patch("/videos/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"endedAt": "2026-09-24T10:10:00Z"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.endedAt").value("2026-09-24T10:10:00Z"))

        verify(exactly = 1) { useCase.updateRecording(1L, endedAt, null, null, null) }
    }

    @Test
    fun `should return 404 when updating non-existent video`() {
        every { useCase.updateRecording(99L, any(), any(), any(), any()) } returns null

        mockMvc.perform(
            patch("/videos/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"durationSec": 600}""")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should delete video recording`() {
        every { useCase.delete(1L) } just Runs

        mockMvc.perform(delete("/videos/1"))
            .andExpect(status().isNoContent)

        verify(exactly = 1) { useCase.delete(1L) }
    }
}
