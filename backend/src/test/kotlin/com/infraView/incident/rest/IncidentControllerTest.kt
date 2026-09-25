package com.infraView.incident.rest

import com.infraView.incident.domain.Incident
import com.infraView.incident.domain.ManageIncidentUseCase
import com.infraView.incident.domain.StatusType
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.dao.DataIntegrityViolationException
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
    private lateinit var useCase: ManageIncidentUseCase

    private val startedAt = OffsetDateTime.parse("2026-09-24T10:00:00Z")
    private val incident = Incident(
        id = 1L,
        code = "INC-123",
        firefighterName = "Jan Kowalski",
        description = "Pożar mieszkania",
        location = "Warszawa",
        startedAt = startedAt,
        status = StatusType.IN_PROGRESS
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
            .andExpect(jsonPath("$.firefighterName").value("Jan Kowalski"))
            .andExpect(jsonPath("$.description").value("Pożar mieszkania"))
            .andExpect(jsonPath("$.location").value("Warszawa"))
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
                      "firefighterName": "Jan Kowalski",
                      "description": "Pożar mieszkania",
                      "location": "Warszawa",
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
        every { useCase.endIncident(1L) } returns incident.copy(endedAt = endedAt, status = StatusType.RESOLVED)

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
    fun `should delete incident`() {
        every { useCase.delete(1L) } just Runs

        mockMvc.perform(delete("/incidents/1"))
            .andExpect(status().isNoContent)

        verify(exactly = 1) { useCase.delete(1L) }
    }

    @Test
    fun `should return 409 when deleting incident with related records`() {
        every { useCase.delete(1L) } throws DataIntegrityViolationException("fk_alarms_incident")

        mockMvc.perform(delete("/incidents/1"))
            .andExpect(status().isConflict)
    }
}
