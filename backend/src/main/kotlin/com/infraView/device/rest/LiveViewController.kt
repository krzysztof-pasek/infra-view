package com.infraView.device.rest

import com.infraView.device.domain.DeviceUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.OutputStream
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Tag(name = "Live view", description = "Live and recorded thermal camera view for the dashboard")
@RestController
class LiveViewController(
    private val useCase: DeviceUseCase
) {
    @Operation(
        summary = "Stream live thermal view",
        description = "Streams thermal frames of the helmet assigned to an in-progress incident as MJPEG. The stream closes after 3 seconds without a new frame or when the helmet starts a new session."
    )
    @GetMapping("/incidents/{incidentId}/live")
    fun live(@PathVariable incidentId: Long): ResponseEntity<StreamingResponseBody> {
        val deviceId = useCase.getLiveDeviceId(incidentId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No live view for incident $incidentId")
        val body = StreamingResponseBody { out ->
            var sequence = 0L
            while (true) {
                val frame = useCase.awaitLiveFrame(deviceId, sequence, FRAME_TIMEOUT) ?: break
                if (frame.incidentId != incidentId) break
                out.writeFrame(frame.image)
                sequence = frame.sequence
            }
        }
        return mjpeg(body)
    }

    @Operation(
        summary = "Stream delayed thermal view",
        description = "Streams thermal frames of the incident as MJPEG with a constant delay behind real time, like rewinding a live stream. Each frame is sent exactly delaySeconds after it was captured, so the view stays behind until the client switches back to /live. Reconnect with a larger delaySeconds to rewind further. The stream closes 3 seconds (in delayed time) after the helmet stops sending frames."
    )
    @GetMapping("/incidents/{incidentId}/replay")
    fun replay(
        @PathVariable incidentId: Long,
        @RequestParam(defaultValue = "10") delaySeconds: Long
    ): ResponseEntity<StreamingResponseBody> {
        if (delaySeconds !in 1..MAX_DELAY_SECONDS) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "delaySeconds must be between 1 and $MAX_DELAY_SECONDS")
        }
        useCase.getLastFrameTime(incidentId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No recorded frames for incident $incidentId")
        val delay = Duration.ofSeconds(delaySeconds)
        val body = StreamingResponseBody { out ->
            out.writeStoredFrames(incidentId, delayedNow(delay), delay)
        }
        return mjpeg(body)
    }

    @Operation(
        summary = "Get recorded thermal frame",
        description = "Returns the JPEG frame of the incident that was on screen at the given time (the last frame captured at or before it, or the first frame when the time is before the recording starts). Use it to preview the recording while scrubbing. The X-Captured-At header holds the frame's capture time."
    )
    @GetMapping("/incidents/{incidentId}/frame")
    fun frame(
        @PathVariable incidentId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) at: OffsetDateTime
    ): ResponseEntity<ByteArray> {
        val frame = useCase.getFrameAt(incidentId, at)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No recorded frames for incident $incidentId")
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .header(CAPTURED_AT_HEADER, frame.capturedAt.toString())
            .body(frame.image)
    }

    @Operation(
        summary = "Play thermal recording",
        description = "Streams the recorded thermal frames of the incident as MJPEG, starting from the frame on screen at the given time (the same frame /frame returns) and keeping the original pace between frames. Reconnect with a different from to seek. For an in-progress incident playback continues behind real time. The stream closes 3 seconds after the last recorded frame."
    )
    @GetMapping("/incidents/{incidentId}/recording")
    fun recording(
        @PathVariable incidentId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: OffsetDateTime
    ): ResponseEntity<StreamingResponseBody> {
        val first = useCase.getFrameAt(incidentId, from)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No recorded frames for incident $incidentId")
        val body = StreamingResponseBody { out ->
            val delay = Duration.between(first.capturedAt, OffsetDateTime.now(ZoneOffset.UTC))
            out.writeFrame(first.image)
            out.writeStoredFrames(incidentId, first.capturedAt, delay)
        }
        return mjpeg(body)
    }

    private fun OutputStream.writeStoredFrames(incidentId: Long, start: OffsetDateTime, delay: Duration) {
        var cursor = start
        while (true) {
            val frames = useCase.getFramesAfter(incidentId, cursor, REPLAY_BATCH_SIZE)
            if (frames.isEmpty()) {
                if (Duration.between(cursor, delayedNow(delay)) > FRAME_TIMEOUT) return
                Thread.sleep(REPLAY_POLL_INTERVAL)
                continue
            }
            for (frame in frames) {
                val wait = Duration.between(delayedNow(delay), frame.capturedAt)
                if (wait.isPositive) Thread.sleep(wait)
                writeFrame(frame.image)
                cursor = frame.capturedAt
            }
        }
    }

    private fun delayedNow(delay: Duration): OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC).minus(delay)

    private fun mjpeg(body: StreamingResponseBody): ResponseEntity<StreamingResponseBody> {
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("multipart/x-mixed-replace;boundary=$BOUNDARY"))
            .header("X-Accel-Buffering", "no")
            .body(body)
    }

    private fun OutputStream.writeFrame(image: ByteArray) {
        write("--$BOUNDARY\r\nContent-Type: image/jpeg\r\nContent-Length: ${image.size}\r\n\r\n".toByteArray())
        write(image)
        write("\r\n".toByteArray())
        flush()
    }

    companion object {
        private const val BOUNDARY = "frame"
        private const val CAPTURED_AT_HEADER = "X-Captured-At"
        private val FRAME_TIMEOUT = Duration.ofSeconds(3)
        private const val MAX_DELAY_SECONDS = 300L
        private const val REPLAY_BATCH_SIZE = 50
        private val REPLAY_POLL_INTERVAL = Duration.ofMillis(200)
    }
}
