package com.infraView.telemetry.domain

import com.infraView.telemetry.domain.MotionState.FALLEN
import com.infraView.telemetry.domain.MotionState.IDLE
import com.infraView.telemetry.domain.MotionState.MOVING
import com.infraView.telemetry.domain.MotionState.NO_MOTION
import com.infraView.telemetry.domain.MotionState.STILL
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MotionDetectorTest {

    private val start = OffsetDateTime.parse("2026-09-28T10:00:00Z")

    private data class Imu(val ax: Double, val ay: Double, val az: Double, val gyro: Double = 0.5)

    // The real helmet is tilted ~30 degrees when worn upright (sample packet from API.md).
    private val upright = Imu(0.0, 0.5, 0.866)
    private val lying = Imu(1.0, 0.0, 0.05)
    private val walking = listOf(Imu(0.1, 0.6, 1.1, gyro = 40.0), Imu(-0.1, 0.4, 0.7, gyro = 35.0))

    private fun walk(count: Int) = List(count) { walking[it % 2] }

    /** Feeds packets one per second like the helmet does, returning the state of each packet. */
    private fun run(packets: List<Imu?>, gapBefore: Set<Int> = emptySet()): List<MotionState?> {
        val stored = mutableListOf<Telemetry>()
        var time = start
        return packets.mapIndexed { index, imu ->
            time = time.plusSeconds(if (index in gapBefore) 6 else 1)
            val telemetry = Telemetry(
                deviceId = 1L,
                incidentId = 7L,
                recordedAt = time,
                accelRawX = imu?.ax,
                accelRawY = imu?.ay,
                accelRawZ = imu?.az,
                gyroRawX = imu?.gyro,
                gyroRawY = imu?.let { 0.0 },
                gyroRawZ = imu?.let { 0.0 }
            )
            val state = MotionDetector.evaluate(stored.takeLast(MotionDetector.HISTORY_SIZE), telemetry)
            stored += telemetry.copy(motionState = state?.name)
            state
        }
    }

    @Test
    fun `should report moving while walking`() {
        assertEquals(List(10) { MOVING }, run(walk(10)))
    }

    @Test
    fun `should stay idle when helmet never moved`() {
        assertTrue(run(List(60) { upright }).all { it == IDLE })
    }

    @Test
    fun `should detect fall after posture change and 10 still packets`() {
        val states = run(walk(5) + List(60) { lying })

        // Packet 5 is the transition, the still run starts at packet 6 and reaches 10 packets at 15.
        assertEquals(MOVING, states[5])
        assertEquals(STILL, states[14])
        assertEquals(FALLEN, states[15])
        assertTrue(states.drop(15).all { it == FALLEN })
    }

    @Test
    fun `should raise no motion instead of fall when pausing in the same posture`() {
        val states = run(walk(5) + List(80) { upright })

        assertEquals(STILL, states[34])
        assertEquals(NO_MOTION, states[35])
        assertTrue(states.drop(35).all { it == NO_MOTION })
    }

    @Test
    fun `should not report fall when getting up before 10 still packets`() {
        val states = run(walk(5) + List(8) { lying } + walk(5) + List(20) { upright })

        assertTrue(FALLEN !in states)
        assertEquals(MOVING, states[13])
    }

    @Test
    fun `should go back to moving after fall and detect the next one`() {
        val states = run(walk(5) + List(15) { lying } + walk(5) + List(15) { lying })

        assertEquals(FALLEN, states[19])
        assertEquals(MOVING, states[20])
        assertEquals(FALLEN, states[39])
    }

    @Test
    fun `should not count missing data as stillness`() {
        val states = run(walk(5) + List(9) { lying } + listOf(null) + List(9) { lying })

        assertNull(states[14])
        assertTrue(FALLEN !in states)
    }

    @Test
    fun `should restart still run after gap but stay armed for no motion`() {
        val states = run(walk(5) + List(40) { lying }, gapBefore = setOf(5))

        // No moving packet right before the gap, so the still run starts at packet 5 and no fall can be confirmed.
        assertTrue(FALLEN !in states)
        assertEquals(STILL, states[33])
        assertEquals(NO_MOTION, states[34])
    }

    @Test
    fun `should not treat strong acceleration as still`() {
        val states = run(walk(5) + List(20) { Imu(0.0, 0.5, 1.3) })

        assertTrue(states.all { it == MOVING })
    }
}
