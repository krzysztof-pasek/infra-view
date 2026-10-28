package com.infraView.incident.rest

import com.infraView.incident.domain.Incident
import com.infraView.incident.domain.IncidentUseCase
import com.infraView.incident.domain.IncidentStatus
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

@WebMvcTest(IncidentController::class)
class IncidentControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var useCase: IncidentUseCase

    private val startedAt = OffsetDateTime.parse("2026-09-24T10:00:00Z")
    private val incident = Incident(
        id = 1L,
        code = "INC-123",
        startedAt = startedAt,
        status = IncidentStatus.IN_PROGRESS
    )

    @Test
    fun `should return all incidents`() {
        every { useCase.getAll() } returns listOf(incident)

        mockMvc.perform(get("/incidents"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].code").value("INC-123"))
    }

    @Test
    fun `should return incident by id`() {
        every { useCase.getById(1L) } returns incident

        mockMvc.perform(get("/incidents/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.code").value("INC-123"))
            .andExpect(jsonPath("$.startedAt").value("2026-09-24T10:00:00Z"))
            .andExpect(jsonPath("$.endedAt").doesNotExist())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
    }

    @Test
    fun `should return 404 when incident not found`() {
        every { useCase.getById(99L) } returns null

        mockMvc.perform(get("/incidents/99"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should create new incident`() {
        val expectedDomain = incident.copy(id = null)
        every { useCase.save(expectedDomain) } returns incident

        mockMvc.perform(
            post("/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "code": "INC-123",
                      "startedAt": "2026-09-24T10:00:00Z",
                      "status": "IN_PROGRESS"
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.code").value("INC-123"))

        verify(exactly = 1) { useCase.save(expectedDomain) }
    }

    @Test
    fun `should return 400 when required field is missing`() {
        mockMvc.perform(
            post("/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"startedAt": "2026-09-24T10:00:00Z", "status": "IN_PROGRESS"}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should end incident`() {
        val endedAt = startedAt.plusHours(1)
        every { useCase.endIncident(1L) } returns incident.copy(endedAt = endedAt, status = IncidentStatus.RESOLVED)

        mockMvc.perform(put("/incidents/1/end"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("RESOLVED"))
            .andExpect(jsonPath("$.endedAt").value("2026-09-24T11:00:00Z"))
    }

    @Test
    fun `should return 404 when ending non-existent incident`() {
        every { useCase.endIncident(99L) } returns null

        mockMvc.perform(put("/incidents/99/end"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should rename incident with trimmed code`() {
        every { useCase.rename(1L, "Jan Kowalski") } returns incident.copy(code = "Jan Kowalski")

        mockMvc.perform(
            put("/incidents/1/code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"code": "  Jan Kowalski  "}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.code").value("Jan Kowalski"))

        verify(exactly = 1) { useCase.rename(1L, "Jan Kowalski") }
    }

    @Test
    fun `should return 400 when new code is blank`() {
        mockMvc.perform(
            put("/incidents/1/code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"code": "   "}""")
        )
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { useCase.rename(any(), any()) }
    }

    @Test
    fun `should return 400 when new code is too long`() {
        mockMvc.perform(
            put("/incidents/1/code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"code": "${"a".repeat(21)}"}""")
        )
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { useCase.rename(any(), any()) }
    }

    @Test
    fun `should return 404 when renaming non-existent incident`() {
        every { useCase.rename(99L, "Jan Kowalski") } returns null

        mockMvc.perform(
            put("/incidents/99/code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"code": "Jan Kowalski"}""")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should delete incident`() {
        every { useCase.delete(1L) } just Runs

        mockMvc.perform(delete("/incidents/1"))
            .andExpect(status().isNoContent)

        verify(exactly = 1) { useCase.delete(1L) }
    }
}
