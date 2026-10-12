package com.infraView.device.rest

import com.infraView.device.domain.DeviceUseCase
import com.infraView.device.domain.LiveFrame
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
import kotlin.test.assertEquals

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
}
