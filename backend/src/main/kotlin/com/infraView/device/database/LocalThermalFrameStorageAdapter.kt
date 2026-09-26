package com.infraView.device.database

import com.infraView.device.domain.ThermalFramePort
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@Component
class LocalThermalFrameStorageAdapter(
    @Value("\${thermal.storage.path:./data/thermal}") baseStoragePath: String
) : ThermalFramePort {

    private val baseDir: Path = Files.createDirectories(Path.of(baseStoragePath))

    override fun saveLatest(deviceId: Long, jpeg: ByteArray) {
        val target = baseDir.resolve("device_$deviceId.jpg")
        val tmp = Files.createTempFile(baseDir, "device_$deviceId", ".tmp")
        Files.write(tmp, jpeg)
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }
}
