package com.infraView.video.domain

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VideoRecordingServiceTest {

    private val videoRecordingPort = mockk<VideoRecordingPort>()
    private val service = VideoRecordingService(videoRecordingPort)

    private val startedAt = OffsetDateTime.parse("2026-09-24T10:00:00Z")
    private val video = VideoRecording(
        id = 1L,
        incidentId = 100L,
        startedAt = startedAt,
        filePath = "2026-09-24/incident_100/video_10-00-00.mjpeg",
        fileSizeBytes = 2048L
    )

    @Test
    fun `should create recording with only incident and start time`() {
        val saved = slot<VideoRecording>()
        every { videoRecordingPort.save(capture(saved)) } answers { saved.captured.copy(id = 1L) }

        val result = service.startRecording(100L, startedAt)

        assertEquals(VideoRecording(incidentId = 100L, startedAt = startedAt), saved.captured)
        assertEquals(1L, result.id)
    }

    @Test
    fun `should update only fields that were provided`() {
        val saved = slot<VideoRecording>()
        val endedAt = startedAt.plusMinutes(10)
        every { videoRecordingPort.getById(1L) } returns video
        every { videoRecordingPort.save(capture(saved)) } answers { saved.captured }

        service.updateRecording(1L, endedAt = endedAt, filePath = null, fileSizeBytes = null, durationSec = 600)

        assertEquals(video.copy(endedAt = endedAt, durationSec = 600), saved.captured)
    }

    @Test
    fun `should return null when updating non-existent recording`() {
        every { videoRecordingPort.getById(99L) } returns null

        assertNull(service.updateRecording(99L, null, null, null, null))
        verify(exactly = 0) { videoRecordingPort.save(any()) }
    }
}
