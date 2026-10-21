package com.infraView.alarm.rest

import com.infraView.alarm.domain.AlarmThreshold
import com.infraView.alarm.domain.AlarmThresholdUseCase
import com.infraView.alarm.domain.ThresholdMetric
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(AlarmThresholdController::class)
class AlarmThresholdControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var useCase: AlarmThresholdUseCase

    private val temperature = AlarmThreshold(ThresholdMetric.TEMPERATURE, threshold = 80.0, resetMargin = 5.0, enabled = true)

    @Test
    fun `should return all thresholds`() {
        every { useCase.getAll() } returns listOf(
            temperature,
            AlarmThreshold(ThresholdMetric.CO2_PPM, threshold = 5000.0, resetMargin = 500.0, enabled = false)
        )

        mockMvc.perform(get("/alarm-thresholds"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].metric").value("TEMPERATURE"))
            .andExpect(jsonPath("$[0].alarmType").value("HIGH_TEMPERATURE"))
            .andExpect(jsonPath("$[0].threshold").value(80.0))
            .andExpect(jsonPath("$[0].resetMargin").value(5.0))
            .andExpect(jsonPath("$[0].enabled").value(true))
            .andExpect(jsonPath("$[1].metric").value("CO2_PPM"))
            .andExpect(jsonPath("$[1].alarmType").value("HIGH_CO2"))
            .andExpect(jsonPath("$[1].enabled").value(false))
    }

    @Test
    fun `should return threshold by metric`() {
        every { useCase.getByMetric(ThresholdMetric.TEMPERATURE) } returns temperature

        mockMvc.perform(get("/alarm-thresholds/TEMPERATURE"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.metric").value("TEMPERATURE"))
            .andExpect(jsonPath("$.threshold").value(80.0))
    }

    @Test
    fun `should return 404 when threshold not found`() {
        every { useCase.getByMetric(ThresholdMetric.TVOC_PPB) } returns null

        mockMvc.perform(get("/alarm-thresholds/TVOC_PPB"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 400 for unknown metric`() {
        mockMvc.perform(get("/alarm-thresholds/HUMIDITY"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should update threshold`() {
        val updated = AlarmThreshold(ThresholdMetric.TEMPERATURE, threshold = 70.0, resetMargin = 3.5, enabled = false)
        every { useCase.update(updated) } returns updated

        mockMvc.perform(
            put("/alarm-thresholds/TEMPERATURE")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"threshold": 70.0, "resetMargin": 3.5, "enabled": false}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.metric").value("TEMPERATURE"))
            .andExpect(jsonPath("$.threshold").value(70.0))
            .andExpect(jsonPath("$.resetMargin").value(3.5))
            .andExpect(jsonPath("$.enabled").value(false))
    }

    @Test
    fun `should return 404 when updating missing threshold`() {
        val updated = AlarmThreshold(ThresholdMetric.TVOC_PPB, threshold = 3000.0, resetMargin = 100.0, enabled = true)
        every { useCase.update(updated) } returns null

        mockMvc.perform(
            put("/alarm-thresholds/TVOC_PPB")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"threshold": 3000.0, "resetMargin": 100.0, "enabled": true}""")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 400 when reset margin is negative`() {
        mockMvc.perform(
            put("/alarm-thresholds/TEMPERATURE")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"threshold": 80.0, "resetMargin": -1.0, "enabled": true}""")
        )
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { useCase.update(any()) }
    }

    @Test
    fun `should return 400 when required field is missing`() {
        mockMvc.perform(
            put("/alarm-thresholds/TEMPERATURE")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"threshold": 80.0, "enabled": true}""")
        )
            .andExpect(status().isBadRequest)
    }
}
