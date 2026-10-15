package com.infraView.telemetry.domain

import java.time.Duration
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

object MotionDetector {

    const val STILL_GYRO_DPS = 10.0
    const val STILL_ACCEL_TOLERANCE_G = 0.15
    const val STILL_ACCEL_DELTA_G = 0.1
    const val FALL_TILT_DEG = 60.0
    const val FALL_STILL_SAMPLES = 10
    const val NO_MOTION_STILL_SAMPLES = 30
    const val REFERENCE_SAMPLES = 5
    val MAX_GAP: Duration = Duration.ofSeconds(3)

    const val HISTORY_SIZE = 40

    fun evaluate(history: List<Telemetry>, current: Telemetry): MotionState? {
        val currentSample = current.toSample() ?: return null
        val segment = contiguousSegment(history, current, currentSample)
        val stillRun = segment.indices.reversed()
            .takeWhile { segment.isStill(it) }
            .count()
        if (stillRun == 0) return MotionState.MOVING

        val previous = history.lastOrNull()?.takeIf { segment.size > 1 }?.motionState
        if (previous == MotionState.FALLEN.name) return MotionState.FALLEN
        if (stillRun >= NO_MOTION_STILL_SAMPLES) return MotionState.NO_MOTION

        val moving = segment.dropLast(stillRun).takeLast(REFERENCE_SAMPLES)
        val still = segment.takeLast(stillRun)
        val postureChanged = moving.isNotEmpty() && stillRun >= FALL_STILL_SAMPLES &&
            angleDeg(moving.meanAccel(), still.meanAccel()) >= FALL_TILT_DEG
        return if (postureChanged) MotionState.FALLEN else MotionState.STILL
    }

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
