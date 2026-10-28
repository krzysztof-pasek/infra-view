package com.infraView.video.rest

import com.infraView.video.domain.VideoRecording
import com.infraView.video.domain.VideoRecordingUseCase
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.OffsetDateTime

@WebMvcTest(VideoRecordingController::class)
class VideoRecordingControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var useCase: VideoRecordingUseCase

    @Test
    fun `should return recordings of incident`() {
        every { useCase.getByIncidentId(7L) } returns listOf(
            VideoRecording(
                id = 7L,
                incidentId = 7L,
                startedAt = OffsetDateTime.parse("2026-09-26T10:00:01Z"),
                endedAt = OffsetDateTime.parse("2026-09-26T10:05:00Z"),
                frameCount = 2700L,
                fileSizeBytes = 13_500_000L,
                durationSec = 299
            )
        )

        mockMvc.perform(get("/videos/incident/7"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(7))
            .andExpect(jsonPath("$[0].incidentId").value(7))
            .andExpect(jsonPath("$[0].startedAt").value("2026-09-26T10:00:01Z"))
            .andExpect(jsonPath("$[0].endedAt").value("2026-09-26T10:05:00Z"))
            .andExpect(jsonPath("$[0].frameCount").value(2700))
            .andExpect(jsonPath("$[0].fileSizeBytes").value(13500000))
            .andExpect(jsonPath("$[0].durationSec").value(299))
    }

    @Test
    fun `should return empty list when incident has no recording`() {
        every { useCase.getByIncidentId(8L) } returns emptyList()

        mockMvc.perform(get("/videos/incident/8"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }
}
