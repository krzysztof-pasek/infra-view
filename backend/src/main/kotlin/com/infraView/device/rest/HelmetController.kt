package com.infraView.device.rest

import com.infraView.device.domain.ManageDeviceUseCase
import com.infraView.telemetry.domain.ManageTelemetryUseCase
import com.infraView.telemetry.domain.Telemetry
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.OffsetDateTime

@Tag(name = "Helmet API", description = "Komunikacja kask (RPI) → serwer zgodnie z API.md")
@RestController
class HelmetController(
    private val deviceUseCase: ManageDeviceUseCase,
    private val telemetryUseCase: ManageTelemetryUseCase,
    @Value("\${infraview.thermal.storage-dir:./thermal}")
    private val thermalStorageDir: String
) {
    private val log = LoggerFactory.getLogger(HelmetController::class.java)

    @Operation(
        summary = "Rejestracja kasku",
        description = "Rejestruje urządzenie po adresie MAC i zwraca UUID. " +
                "Ten sam MAC zawsze dostaje ten sam UUID, żeby restart kasku nie rozbijał historii."
    )
    @PostMapping("/auth")
    fun auth(@RequestBody body: AuthRequestDto): AuthResponseDto {
        val device = deviceUseCase.registerOrFind(body.mac)
        log.info("Kask zarejestrowany: MAC={}, UUID={}", device.mac, device.uuid)
        return AuthResponseDto(uuid = device.uuid)
    }

    @Operation(
        summary = "Paczka pomiarów",
        description = "Przyjmuje tablicę odczytów z czujników kasku (1 raz na sekundę). " +
                "Mapuje readings na telemetrię i zapisuje do bazy."
    )
    @PostMapping("/{uuid}/data")
    fun data(@PathVariable uuid: String, @RequestBody packet: DataPacketDto): Map<String, Boolean> {
        val device = deviceUseCase.getByUuid(uuid)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Nieznany UUID: $uuid")

        val telemetry = mapReadingsToTelemetry(device.id!!, packet.readings)
        telemetryUseCase.add(telemetry)

        return mapOf("ok" to true)
    }

    @Operation(
        summary = "Obraz z kamery termowizyjnej",
        description = "Przyjmuje surowy JPEG z kamery FLIR Lepton (~8-9 razy na sekundę). " +
                "Treścią są surowe bajty pliku JPEG (Content-Type: image/jpeg)."
    )
    @PostMapping("/{uuid}/view", consumes = [MediaType.IMAGE_JPEG_VALUE])
    fun view(@PathVariable uuid: String, @RequestBody jpeg: ByteArray): Map<String, Boolean> {
        val device = deviceUseCase.getByUuid(uuid)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Nieznany UUID: $uuid")

        val dir = Path.of(thermalStorageDir)
        Files.createDirectories(dir)
        val file = dir.resolve("${device.uuid}.jpg")
        Files.write(file, jpeg, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

        return mapOf("ok" to true)
    }

    /**
     * Mapuje tablicę odczytów z kasku na domenowy obiekt Telemetry.
     *
     * Mapowanie czujników:
     * - bmi160 ax/ay/az → accelRawX/Y/Z
     * - bmi160 gx/gy/gz → gyroRawX/Y/Z
     * - sgp30 eco2      → co2Ppm
     * - sgp30 tvoc       → gasPpm
     * - max6675 temperature → temperature
     * - mics5524 voltage → pomijany (czujnik nie jest wykalibrowany)
     */
    private fun mapReadingsToTelemetry(deviceId: Long, readings: List<ReadingDto>): Telemetry {
        var accelRawX: Double? = null
        var accelRawY: Double? = null
        var accelRawZ: Double? = null
        var gyroRawX: Double? = null
        var gyroRawY: Double? = null
        var gyroRawZ: Double? = null
        var co2Ppm: Double? = null
        var gasPpm: Double? = null
        var temperature: Double? = null

        for (r in readings) {
            when {
                r.sensor == "bmi160" && r.metric == "ax" -> accelRawX = r.value
                r.sensor == "bmi160" && r.metric == "ay" -> accelRawY = r.value
                r.sensor == "bmi160" && r.metric == "az" -> accelRawZ = r.value
                r.sensor == "bmi160" && r.metric == "gx" -> gyroRawX = r.value
                r.sensor == "bmi160" && r.metric == "gy" -> gyroRawY = r.value
                r.sensor == "bmi160" && r.metric == "gz" -> gyroRawZ = r.value
                r.sensor == "sgp30" && r.metric == "eco2" -> co2Ppm = r.value
                r.sensor == "sgp30" && r.metric == "tvoc" -> gasPpm = r.value
                r.sensor == "max6675" && r.metric == "temperature" -> temperature = r.value
                r.sensor == "mics5524" && r.metric == "voltage" -> { /* mics5524 nie jest wykalibrowany — pomijany */ }
            }
        }

        return Telemetry(
            deviceId = deviceId,
            recordedAt = OffsetDateTime.now(),
            accelRawX = accelRawX,
            accelRawY = accelRawY,
            accelRawZ = accelRawZ,
            gyroRawX = gyroRawX,
            gyroRawY = gyroRawY,
            gyroRawZ = gyroRawZ,
            co2Ppm = co2Ppm,
            gasPpm = gasPpm,
            temperature = temperature
        )
    }
}
