package com.infraView.telemetry.domain

import java.time.Duration
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * Man-down detection from the 1 Hz IMU snapshots the helmet sends (strategy A, see FALL_DETECTION.md).
 *
 * A single snapshot per second cannot catch the impact itself, so this only detects the state after a fall:
 * - FALLEN: the firefighter was moving, then the helmet orientation changed by at least [FALL_TILT_DEG]
 *   and has not moved for [FALL_STILL_SAMPLES] packets,
 * - NO_MOTION: not moving for [NO_MOTION_STILL_SAMPLES] packets, whatever the orientation.
 *
 * Stateless: everything is derived from the recent telemetry of the session. The `motionState` saved on the
 * previous row carries FALLEN/NO_MOTION forward once the moving samples have left the window.
 */
object MotionDetector {

    const val STILL_GYRO_DPS = 10.0
    const val STILL_ACCEL_TOLERANCE_G = 0.15
    const val STILL_ACCEL_DELTA_G = 0.1
    const val FALL_TILT_DEG = 60.0
    const val FALL_STILL_SAMPLES = 10
    const val NO_MOTION_STILL_SAMPLES = 30
    const val REFERENCE_SAMPLES = 5
    val MAX_GAP: Duration = Duration.ofSeconds(3)

    /** Must stay larger than NO_MOTION_STILL_SAMPLES + REFERENCE_SAMPLES so the carry-over works. */
    const val HISTORY_SIZE = 40

    private val ARMED_STATES = setOf(MotionState.MOVING, MotionState.STILL, MotionState.FALLEN, MotionState.NO_MOTION)

    /**
     * @param history earlier telemetry of the same session, oldest first, without [current]
     * @return the state for [current], or null when its IMU reading is incomplete
     */
    fun evaluate(history: List<Telemetry>, current: Telemetry): MotionState? {
        val currentSample = current.toSample() ?: return null
        val segment = contiguousSegment(history, current, currentSample)
        val stillRun = segment.indices.reversed()
            .takeWhile { segment.isStill(it) }
            .count()
        if (stillRun == 0) return MotionState.MOVING

        val previous = history.lastOrNull()
            ?.takeIf { segment.size > 1 }
            ?.motionState
            ?.let { state -> MotionState.entries.find { it.name == state } }
        if (previous == MotionState.FALLEN || previous == MotionState.NO_MOTION) return previous

        // Across a data gap there is no reference orientation, but a helmet that was already worn stays armed for NO_MOTION.
        val moving = segment.dropLast(stillRun).takeLast(REFERENCE_SAMPLES)
        val armed = moving.isNotEmpty() || history.any { row -> ARMED_STATES.any { it.name == row.motionState } }
        if (!armed) return MotionState.IDLE

        val still = segment.takeLast(stillRun)
        return when {
            moving.isNotEmpty() && stillRun >= FALL_STILL_SAMPLES &&
                angleDeg(moving.meanAccel(), still.meanAccel()) >= FALL_TILT_DEG -> MotionState.FALLEN
            stillRun >= NO_MOTION_STILL_SAMPLES -> MotionState.NO_MOTION
            else -> MotionState.STILL
        }
    }

    /** Trailing samples ending at [current] with complete IMU data and no gap longer than [MAX_GAP]. */
    private fun contiguousSegment(history: List<Telemetry>, current: Telemetry, currentSample: Sample): List<Sample> {
        val segment = mutableListOf(currentSample)
        var next = current
        for (row in history.asReversed()) {
            val sample = row.toSample() ?: break
            if (Duration.between(row.recordedAt, next.recordedAt) > MAX_GAP) break
            segment.add(sample)
            next = row
        }
        return segment.asReversed()
    }

    private fun List<Sample>.isStill(index: Int): Boolean {
        val sample = this[index]
        if (sample.gyro.norm() >= STILL_GYRO_DPS) return false
        if (abs(sample.accel.norm() - 1.0) >= STILL_ACCEL_TOLERANCE_G) return false
        val previous = getOrNull(index - 1) ?: return true
        return (sample.accel - previous.accel).norm() < STILL_ACCEL_DELTA_G
    }

    private fun List<Sample>.meanAccel(): Vector = map { it.accel }
        .reduce { sum, v -> sum + v }
        .let { Vector(it.x / size, it.y / size, it.z / size) }

    private fun angleDeg(a: Vector, b: Vector): Double {
        val cos = (a.x * b.x + a.y * b.y + a.z * b.z) / (a.norm() * b.norm())
        return Math.toDegrees(acos(cos.coerceIn(-1.0, 1.0)))
    }

    private fun Telemetry.toSample(): Sample? {
        return Sample(
            accel = Vector(accelRawX ?: return null, accelRawY ?: return null, accelRawZ ?: return null),
            gyro = Vector(gyroRawX ?: return null, gyroRawY ?: return null, gyroRawZ ?: return null)
        )
    }

    private data class Sample(val accel: Vector, val gyro: Vector)

    private data class Vector(val x: Double, val y: Double, val z: Double) {
        operator fun plus(o: Vector) = Vector(x + o.x, y + o.y, z + o.z)
        operator fun minus(o: Vector) = Vector(x - o.x, y - o.y, z - o.z)
        fun norm() = sqrt(x * x + y * y + z * z)
    }
}
