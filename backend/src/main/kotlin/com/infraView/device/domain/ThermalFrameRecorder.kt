package com.infraView.device.domain

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

@Service
class ThermalFrameRecorder(
    private val thermalFrameRepository: ThermalFrameRepository
) : AutoCloseable {

    private val log = LoggerFactory.getLogger(ThermalFrameRecorder::class.java)
    private val queue = ArrayBlockingQueue<ThermalFrame>(QUEUE_CAPACITY)
    private val droppedFrames = AtomicLong()
    @Volatile
    private var running = true
    private val writer = Thread.ofPlatform().name("thermal-frame-writer").daemon(true).start(::writeFrames)

    fun record(frame: ThermalFrame) {
        if (!queue.offer(frame)) {
            log.warn(
                "Thermal frame queue full, frame dropped: deviceId={}, incidentId={}, droppedTotal={}",
                frame.deviceId, frame.incidentId, droppedFrames.incrementAndGet()
            )
        }
    }

    override fun close() {
        running = false
        writer.join(SHUTDOWN_TIMEOUT)
        if (queue.isNotEmpty()) {
            log.warn("Thermal frame writer stopped with {} unsaved frames", queue.size)
        }
    }

    private fun writeFrames() {
        while (running || queue.isNotEmpty()) {
            val frame = queue.poll(POLL_INTERVAL_MS, TimeUnit.MILLISECONDS) ?: continue
            write(frame)
        }
    }

    private fun write(frame: ThermalFrame) {
        val startedAt = System.nanoTime()
        try {
            val saved = thermalFrameRepository.save(frame)
            val saveMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
            if (saveMs >= SLOW_SAVE_MS) {
                log.warn("Slow thermal frame save: deviceId={}, frameId={}, saveMs={}, queued={}", saved.deviceId, saved.id, saveMs, queue.size)
            }
            log.debug("Thermal frame stored: deviceId={}, frameId={}, bytes={}, saveMs={}", saved.deviceId, saved.id, frame.image.size, saveMs)
        } catch (e: Exception) {
            log.error("Failed to store thermal frame: deviceId={}, incidentId={}", frame.deviceId, frame.incidentId, e)
        }
    }

    companion object {
        const val QUEUE_CAPACITY = 256
        private const val POLL_INTERVAL_MS = 200L
        private const val SLOW_SAVE_MS = 200L
        private val SHUTDOWN_TIMEOUT = Duration.ofSeconds(10)
    }
}
