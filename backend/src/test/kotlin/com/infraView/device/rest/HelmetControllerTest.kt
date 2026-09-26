package com.infraView.device.rest

import com.infraView.device.domain.Device
import com.infraView.device.domain.ManageHelmetUseCase
import com.infraView.device.domain.SensorReading
import com.infraView.telemetry.domain.Telemetry
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.OffsetDateTime

@WebMvcTest(HelmetController::class)
class HelmetControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var useCase: ManageHelmetUseCase

    private val uuid = "689d8e89-2503-4739-9dd8-c93bc9d7c14d"
    private val device = Device(
        id = 1L,
        mac = "d8:3a:dd:ed:3f:42",
        uuid = uuid,
        registeredAt = OffsetDateTime.parse("2026-09-26T10:00:00Z")
    )
    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00, 0x10)

    @Test
    fun `should return uuid for registered helmet`() {
        every { useCase.register("d8:3a:dd:ed:3f:42") } returns device

        mockMvc.perform(
            post("/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"mac": "d8:3a:dd:ed:3f:42"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.uuid").value(uuid))
    }

    @Test
    fun `should normalize mac address before registering`() {
        every { useCase.register("d8:3a:dd:ed:3f:42") } returns device

        mockMvc.perform(
            post("/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"mac": " D8:3A:DD:ED:3F:42 "}""")
        )
            .andExpect(status().isOk)

        verify(exactly = 1) { useCase.register("d8:3a:dd:ed:3f:42") }
    }

    @Test
    fun `should return 400 for invalid mac address`() {
        mockMvc.perform(
            post("/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"mac": "not-a-mac"}""")
        )
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { useCase.register(any()) }
    }

    @Test
    fun `should return 400 when mac is missing`() {
        mockMvc.perform(
            post("/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should pass readings to use case`() {
        val expectedReadings = listOf(
            SensorReading("bmi160", "ax", -0.037),
            SensorReading("max6675", "temperature", null)
        )
        every { useCase.ingestReadings(uuid, expectedReadings) } returns
            Telemetry(id = 1L, deviceId = 1L, recordedAt = OffsetDateTime.parse("2026-09-26T10:00:01Z"))

        mockMvc.perform(
            post("/$uuid/data")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"readings":[{"metric":"ax","sensor":"bmi160","unit":"g","value":-0.037},{"metric":"temperature","sensor":"max6675","unit":"C","value":null}]}""")
        )
            .andExpect(status().isNoContent)

        verify(exactly = 1) { useCase.ingestReadings(uuid, expectedReadings) }
    }

    @Test
    fun `should return 404 for readings from unknown device`() {
        every { useCase.ingestReadings("unknown", any()) } returns null

        mockMvc.perform(
            post("/unknown/data")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"readings":[]}""")
        )
            .andExpect(status().isNotFound)
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("unknown"))))
    }

    @Test
    fun `should return 400 when readings are missing`() {
        mockMvc.perform(
            post("/$uuid/data")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should accept thermal frame`() {
        every { useCase.ingestFrame(uuid, jpeg) } returns true

        mockMvc.perform(
            post("/$uuid/view")
                .contentType(MediaType.IMAGE_JPEG)
                .content(jpeg)
        )
            .andExpect(status().isNoContent)

        verify(exactly = 1) { useCase.ingestFrame(uuid, jpeg) }
    }

    @Test
    fun `should return 404 for frame from unknown device`() {
        every { useCase.ingestFrame("unknown", jpeg) } returns false

        mockMvc.perform(
            post("/unknown/view")
                .contentType(MediaType.IMAGE_JPEG)
                .content(jpeg)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 400 when frame is not a jpeg`() {
        mockMvc.perform(
            post("/$uuid/view")
                .contentType(MediaType.IMAGE_JPEG)
                .content(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47))
        )
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { useCase.ingestFrame(any(), any()) }
    }

    @Test
    fun `should return 415 when frame is sent with wrong content type`() {
        mockMvc.perform(
            post("/$uuid/view")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(jpeg)
        )
            .andExpect(status().isUnsupportedMediaType)
    }
}
