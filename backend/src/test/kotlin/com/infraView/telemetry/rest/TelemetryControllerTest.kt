package com.infraView.telemetry.rest

import com.infraView.telemetry.domain.ManageTelemetryUseCase
import com.infraView.telemetry.domain.Telemetry
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

@WebMvcTest(TelemetryController::class)
class TelemetryControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var useCase: ManageTelemetryUseCase

    private val recordedAt = OffsetDateTime.parse("2026-09-24T10:00:00Z")
    private val telemetry = Telemetry(
        id = 1L,
        incidentId = 100L,
        recordedAt = recordedAt,
        accelRawX = -0.037,
        accelRawY = 0.509,
        accelRawZ = 0.904,
        temperature = 24,
        gasPpm = 0.267,
        co2Ppm = 400.0
    )

    @Test
    fun `should return telemetry by id`() {
        every { useCase.getById(1L) } returns telemetry

        mockMvc.perform(get("/telemetry/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.incidentId").value(100))
            .andExpect(jsonPath("$.recordedAt").value("2026-09-24T10:00:00Z"))
            .andExpect(jsonPath("$.accelRawX").value(-0.037))
            .andExpect(jsonPath("$.accelRawZ").value(0.904))
            .andExpect(jsonPath("$.temperature").value(24))
            .andExpect(jsonPath("$.co2Ppm").value(400.0))
            .andExpect(jsonPath("$.gyroRawX").doesNotExist())
    }

    @Test
    fun `should return 404 when telemetry not found`() {
        every { useCase.getById(99L) } returns null

        mockMvc.perform(get("/telemetry/99"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return telemetry by incident id`() {
        every { useCase.getByIncidentId(100L) } returns listOf(telemetry, telemetry.copy(id = 2L))

        mockMvc.perform(get("/telemetry/incident/100"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[1].id").value(2))
    }

    @Test
    fun `should add telemetry`() {
        val expectedDomain = telemetry.copy(id = null)
        every { useCase.add(expectedDomain) } returns telemetry

        mockMvc.perform(
            post("/telemetry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "incidentId": 100,
                      "recordedAt": "2026-09-24T10:00:00Z",
                      "accelRawX": -0.037,
                      "accelRawY": 0.509,
                      "accelRawZ": 0.904,
                      "temperature": 24,
                      "gasPpm": 0.267,
                      "co2Ppm": 400.0
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(1))

        verify(exactly = 1) { useCase.add(expectedDomain) }
    }

    @Test
    fun `should return 400 when incident id is missing`() {
        mockMvc.perform(
            post("/telemetry")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"recordedAt": "2026-09-24T10:00:00Z"}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should delete telemetry`() {
        every { useCase.delete(1L) } just Runs

        mockMvc.perform(delete("/telemetry/1"))
            .andExpect(status().isNoContent)

        verify(exactly = 1) { useCase.delete(1L) }
    }
}
