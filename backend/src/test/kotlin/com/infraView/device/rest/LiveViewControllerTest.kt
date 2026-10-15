package com.infraView.device.rest

import com.infraView.device.domain.DeviceUseCase
import com.infraView.device.domain.LiveFrame
import com.infraView.device.domain.ThermalFrame
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@WebMvcTest(LiveViewController::class)
class LiveViewControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var useCase: DeviceUseCase

    private val timeout = Duration.ofSeconds(3)
    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())

    @Test
    fun `should stream frames as mjpeg until helmet stops sending`() {
        every { useCase.getLiveDeviceId(7L) } returns 1L
        every { useCase.awaitLiveFrame(1L, 0L, timeout) } returns LiveFrame(sequence = 5L, incidentId = 7L, image = jpeg)
        every { useCase.awaitLiveFrame(1L, 5L, timeout) } returns null

        val started = mockMvc.perform(get("/incidents/7/live"))
            .andExpect(request().asyncStarted())
            .andReturn()

        val result = mockMvc.perform(asyncDispatch(started))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "multipart/x-mixed-replace;boundary=frame"))
            .andExpect(header().string("X-Accel-Buffering", "no"))
            .andReturn()

        val expected = "--frame\r\nContent-Type: image/jpeg\r\nContent-Length: 4\r\n\r\n".toByteArray() + jpeg + "\r\n".toByteArray()
        assertEquals(expected.toList(), result.response.contentAsByteArray.toList())
    }

    @Test
    fun `should end stream when helmet starts a new session`() {
        every { useCase.getLiveDeviceId(7L) } returns 1L
        every { useCase.awaitLiveFrame(1L, 0L, timeout) } returns LiveFrame(sequence = 5L, incidentId = 8L, image = jpeg)

        val started = mockMvc.perform(get("/incidents/7/live"))
            .andExpect(request().asyncStarted())
            .andReturn()

        val result = mockMvc.perform(asyncDispatch(started))
            .andExpect(status().isOk)
            .andReturn()

        assertEquals(0, result.response.contentAsByteArray.size)
    }

    @Test
    fun `should return 404 when incident has no live view`() {
        every { useCase.getLiveDeviceId(7L) } returns null

        mockMvc.perform(get("/incidents/7/live"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should stream stored frames with requested delay until helmet stops sending`() {
        val second = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 2, 0xFF.toByte(), 0xD9.toByte())
        val afters = mutableListOf<OffsetDateTime>()
        every { useCase.getLastFrameTime(7L) } returns OffsetDateTime.parse("2026-09-26T10:05:00Z")
        every { useCase.getFramesAfter(7L, capture(afters), 50) } returnsMany listOf(
            listOf(
                ThermalFrame(id = 1L, deviceId = 1L, incidentId = 7L, capturedAt = OffsetDateTime.parse("2026-09-26T10:04:59.900Z"), image = jpeg),
                ThermalFrame(id = 2L, deviceId = 1L, incidentId = 7L, capturedAt = OffsetDateTime.parse("2026-09-26T10:05:00Z"), image = second)
            ),
            emptyList()
        )

        val requestedAt = OffsetDateTime.now(ZoneOffset.UTC)
        val started = mockMvc.perform(get("/incidents/7/replay").param("delaySeconds", "20"))
            .andExpect(request().asyncStarted())
            .andReturn()

        val result = mockMvc.perform(asyncDispatch(started))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "multipart/x-mixed-replace;boundary=frame"))
            .andExpect(header().string("X-Accel-Buffering", "no"))
            .andReturn()

        val expected = "--frame\r\nContent-Type: image/jpeg\r\nContent-Length: 4\r\n\r\n".toByteArray() + jpeg + "\r\n".toByteArray() +
            "--frame\r\nContent-Type: image/jpeg\r\nContent-Length: 5\r\n\r\n".toByteArray() + second + "\r\n".toByteArray()
        assertEquals(expected.toList(), result.response.contentAsByteArray.toList())
        val startDelay = Duration.between(afters[0], requestedAt)
        assertTrue(startDelay > Duration.ofSeconds(19) && startDelay < Duration.ofSeconds(21))
        assertEquals(OffsetDateTime.parse("2026-09-26T10:05:00Z"), afters[1])
    }

    @Test
    fun `should return 404 when incident has no recorded frames to replay`() {
        every { useCase.getLastFrameTime(7L) } returns null

        mockMvc.perform(get("/incidents/7/replay"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 400 when replay delay is out of range`() {
        mockMvc.perform(get("/incidents/7/replay").param("delaySeconds", "0"))
            .andExpect(status().isBadRequest)
        mockMvc.perform(get("/incidents/7/replay").param("delaySeconds", "301"))
            .andExpect(status().isBadRequest)
    }
}
