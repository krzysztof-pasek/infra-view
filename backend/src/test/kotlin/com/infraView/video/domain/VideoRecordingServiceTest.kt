package com.infraView.video.domain

import com.infraView.incident.domain.Incident
import com.infraView.incident.domain.IncidentStatus
import com.infraView.incident.domain.IncidentUseCase
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VideoRecordingServiceTest {

    private val videoRecordingRepository = mockk<VideoRecordingRepository>()
    private val incidentUseCase = mockk<IncidentUseCase>()
    private val service = VideoRecordingService(videoRecordingRepository, incidentUseCase)

    private val recording = VideoRecording(
        id = 7L,
        incidentId = 7L,
        startedAt = OffsetDateTime.parse("2026-09-26T10:00:01Z"),
        endedAt = OffsetDateTime.parse("2026-09-26T10:05:00Z"),
        frameCount = 2700L,
        fileSizeBytes = 13_500_000L,
        durationSec = 299
    )
    private val incident = Incident(
        id = 7L,
        code = "HELMET-1-0926100000",
        startedAt = OffsetDateTime.parse("2026-09-26T10:00:00Z"),
        endedAt = OffsetDateTime.parse("2026-09-26T10:06:00Z"),
        status = IncidentStatus.RESOLVED,
        deviceId = 1L
    )

    @Test
    fun `should return recording of resolved incident`() {
        every { videoRecordingRepository.getByIncidentId(7L) } returns recording
        every { incidentUseCase.getById(7L) } returns incident

        assertEquals(listOf(recording), service.getByIncidentId(7L))
    }

    @Test
    fun `should leave end of recording open while incident is in progress`() {
        every { videoRecordingRepository.getByIncidentId(7L) } returns recording
        every { incidentUseCase.getById(7L) } returns incident.copy(endedAt = null, status = IncidentStatus.IN_PROGRESS)

        assertEquals(listOf(recording.copy(endedAt = null)), service.getByIncidentId(7L))
    }

    @Test
    fun `should return no recordings when incident has no frames`() {
        every { videoRecordingRepository.getByIncidentId(8L) } returns null

        assertTrue(service.getByIncidentId(8L).isEmpty())
    }
}
