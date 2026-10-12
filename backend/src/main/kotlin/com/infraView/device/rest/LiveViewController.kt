package com.infraView.device.rest

import com.infraView.device.domain.DeviceUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.time.Duration

@Tag(name = "Live view", description = "Live thermal camera stream for the dashboard")
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
                out.write("--$BOUNDARY\r\nContent-Type: image/jpeg\r\nContent-Length: ${frame.image.size}\r\n\r\n".toByteArray())
                out.write(frame.image)
                out.write("\r\n".toByteArray())
                out.flush()
                sequence = frame.sequence
            }
        }
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("multipart/x-mixed-replace;boundary=$BOUNDARY"))
            .header("X-Accel-Buffering", "no")
            .body(body)
    }

    companion object {
        private const val BOUNDARY = "frame"
        private val FRAME_TIMEOUT = Duration.ofSeconds(3)
    }
}
