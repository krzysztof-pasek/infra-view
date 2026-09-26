package com.infraView.device.database

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class LocalThermalFrameStorageAdapterTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `should keep only the latest frame per device`() {
        val adapter = LocalThermalFrameStorageAdapter(tempDir.resolve("thermal").toString())
        val first = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1)
        val second = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 2)

        adapter.saveLatest(1L, first)
        adapter.saveLatest(1L, second)
        adapter.saveLatest(2L, first)

        val dir = tempDir.resolve("thermal")
        assertEquals(listOf("device_1.jpg", "device_2.jpg"), dir.listDirectoryEntries().map { it.name }.sorted())
        assertContentEquals(second, Files.readAllBytes(dir.resolve("device_1.jpg")))
        assertContentEquals(first, Files.readAllBytes(dir.resolve("device_2.jpg")))
    }
}
