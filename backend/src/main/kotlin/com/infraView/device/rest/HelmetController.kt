package com.infraView.device.rest

import com.infraView.device.domain.ManageHelmetUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@Tag(name = "Helmet API", description = "Endpoints called by the helmet (Raspberry Pi), see API.md")
@RestController
class HelmetController(
    private val useCase: ManageHelmetUseCase
) {
    @Operation(summary = "Register helmet", description = "Registers a helmet by its MAC address and returns its UUID. The same MAC always gets the same UUID.")
    @PostMapping("/auth", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun auth(@RequestBody body: AuthRequestDto): AuthResponseDto {
        val mac = body.mac.trim().lowercase()
        if (!MAC_PATTERN.matches(mac)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid MAC address")
        }
        return AuthResponseDto(uuid = useCase.register(mac).uuid)
    }

    @Operation(summary = "Receive sensor readings", description = "Receives one packet of sensor readings (sent once per second) and stores it as telemetry of the helmet.")
    @PostMapping("/{uuid}/data", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun data(@PathVariable uuid: String, @RequestBody packet: DataPacketDto) {
        useCase.ingestReadings(uuid, packet.readings.map { it.toDomain() })
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown device")
    }

    @Operation(summary = "Receive thermal camera frame", description = "Receives a raw JPEG frame from the thermal camera (about 8-9 per second) and keeps it as the latest frame of the helmet.")
    @PostMapping("/{uuid}/view", consumes = [MediaType.IMAGE_JPEG_VALUE])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun view(@PathVariable uuid: String, @RequestBody jpeg: ByteArray) {
        if (jpeg.size < 2 || jpeg[0] != 0xFF.toByte() || jpeg[1] != 0xD8.toByte()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Body is not a JPEG image")
        }
        if (!useCase.ingestFrame(uuid, jpeg)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown device")
        }
    }

    companion object {
        private val MAC_PATTERN = Regex("^([0-9a-f]{2}:){5}[0-9a-f]{2}$")
    }
}
