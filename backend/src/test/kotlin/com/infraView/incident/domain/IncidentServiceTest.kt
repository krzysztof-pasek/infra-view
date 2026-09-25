package com.infraView.incident.domain

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IncidentServiceTest {

    private val incidentPort = mockk<IncidentPort>()
    private val service = IncidentService(incidentPort)

    private val incident = Incident(
        id = 1L,
        code = "INC-123",
        startedAt = OffsetDateTime.parse("2026-09-24T10:00:00Z"),
        status = StatusType.IN_PROGRESS
    )

    @Test
    fun `should resolve incident and set endedAt when ending it`() {
        val saved = slot<Incident>()
        every { incidentPort.getById(1L) } returns incident
        every { incidentPort.save(capture(saved)) } answers { saved.captured }
        val before = OffsetDateTime.now()

        val result = service.endIncident(1L)

        assertNotNull(result)
        assertEquals(StatusType.RESOLVED, result.status)
        val endedAt = assertNotNull(result.endedAt)
        assertTrue(!endedAt.isBefore(before))
        assertEquals(incident.copy(status = StatusType.RESOLVED, endedAt = endedAt), saved.captured)
    }

    @Test
    fun `should not overwrite endedAt when incident is already resolved`() {
        val resolved = incident.copy(status = StatusType.RESOLVED, endedAt = incident.startedAt.plusHours(2))
        every { incidentPort.getById(1L) } returns resolved

        assertEquals(resolved, service.endIncident(1L))
        verify(exactly = 0) { incidentPort.save(any()) }
    }

    @Test
    fun `should return null when ending non-existent incident`() {
        every { incidentPort.getById(99L) } returns null

        assertNull(service.endIncident(99L))
        verify(exactly = 0) { incidentPort.save(any()) }
    }
}
