package com.infraView.video.database

import com.infraView.video.domain.VideoFileStoragePort
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

@Component
class LocalVideoFileStorageAdapter(
    @Value("\${video.storage.path:./data/videos}") private val baseStoragePath: String
) : VideoFileStoragePort {

    init {
        File(baseStoragePath).mkdirs()
    }

    override fun saveFrame(incidentId: Long, startedAt: java.time.OffsetDateTime, frameBytes: ByteArray): String {
        val dateFolder = startedAt.toLocalDate().toString()
        val incidentDir = File("$baseStoragePath/$dateFolder/incident_$incidentId")

        if (!incidentDir.exists()) {
            incidentDir.mkdirs()
        }

        val timeString = startedAt.toLocalTime().withNano(0).toString().replace(":", "-")
        val fileName = "video_$timeString.mjpeg"
        val videoFile = File(incidentDir, fileName)
        
        Files.write(
            Paths.get(videoFile.toURI()),
            frameBytes,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        )

        return "$dateFolder/incident_$incidentId/$fileName"
    }

    override fun getRecordingPath(incidentId: Long, startedAt: java.time.OffsetDateTime): String? {
        val dateFolder = startedAt.toLocalDate().toString()
        val timeString = startedAt.toLocalTime().withNano(0).toString().replace(":", "-")
        val fileName = "video_$timeString.mjpeg"
        
        val videoFile = File("$baseStoragePath/$dateFolder/incident_$incidentId/$fileName")
        return if (videoFile.exists()) videoFile.absolutePath else null
    }
}
