package com.infraView.alarm.rest

import com.infraView.alarm.domain.Alarm
import com.infraView.alarm.domain.AlarmType
import com.infraView.alarm.domain.ManageAlarmUseCase
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

@WebMvcTest(AlarmController::class)
class AlarmControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var useCase: ManageAlarmUseCase

    private val triggeredAt = OffsetDateTime.parse("2026-09-24T10:00:00Z")
    private val alarm = Alarm(
        id = 1L,
        incidentId = 100L,
        alarmType = AlarmType.FALL,
        triggeredAt = triggeredAt
    )

    @Test
    fun `should return all alarms`() {
        every { useCase.getAll() } returns listOf(alarm, alarm.copy(id = 2L, alarmType = AlarmType.HIGH_GAS))

        mockMvc.perform(get("/alarms"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].alarmType").value("FALL"))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].alarmType").value("HIGH_GAS"))
    }

    @Test
    fun `should return empty list when there are no alarms`() {
        every { useCase.getAll() } returns emptyList()

        mockMvc.perform(get("/alarms"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `should return alarm by id`() {
        every { useCase.getById(1L) } returns alarm

        mockMvc.perform(get("/alarms/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.incidentId").value(100))
            .andExpect(jsonPath("$.alarmType").value("FALL"))
            .andExpect(jsonPath("$.triggeredAt").value("2026-09-24T10:00:00Z"))
            .andExpect(jsonPath("$.resolvedAt").doesNotExist())
    }

    @Test
    fun `should return 404 when alarm not found`() {
        every { useCase.getById(99L) } returns null

        mockMvc.perform(get("/alarms/99"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should trigger new alarm`() {
        val expectedDomain = Alarm(incidentId = 100L, alarmType = AlarmType.FALL, triggeredAt = triggeredAt)
        every { useCase.triggerAlarm(expectedDomain) } returns alarm

        mockMvc.perform(
            post("/alarms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"incidentId": 100, "alarmType": "FALL", "triggeredAt": "2026-09-24T10:00:00Z"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.alarmType").value("FALL"))

        verify(exactly = 1) { useCase.triggerAlarm(expectedDomain) }
    }

    @Test
    fun `should return 400 for unknown alarm type`() {
        mockMvc.perform(
            post("/alarms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"incidentId": 100, "alarmType": "EXPLOSION", "triggeredAt": "2026-09-24T10:00:00Z"}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 409 when incident does not exist`() {
        every { useCase.triggerAlarm(any()) } throws DataIntegrityViolationException("fk_alarms_incident")

        mockMvc.perform(
            post("/alarms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"incidentId": 999, "alarmType": "FALL", "triggeredAt": "2026-09-24T10:00:00Z"}""")
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `should resolve alarm`() {
        val resolvedAt = triggeredAt.plusMinutes(5)
        every { useCase.resolveAlarm(1L) } returns alarm.copy(resolvedAt = resolvedAt)

        mockMvc.perform(put("/alarms/1/resolve"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.resolvedAt").value("2026-09-24T10:05:00Z"))
    }

    @Test
    fun `should return 404 when resolving non-existent alarm`() {
        every { useCase.resolveAlarm(99L) } returns null

        mockMvc.perform(put("/alarms/99/resolve"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should delete alarm`() {
        every { useCase.delete(1L) } just Runs

        mockMvc.perform(delete("/alarms/1"))
            .andExpect(status().isNoContent)

        verify(exactly = 1) { useCase.delete(1L) }
    }
}
