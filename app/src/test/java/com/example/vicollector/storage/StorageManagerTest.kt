package com.example.vicollector.storage

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StorageManagerTest {
    @Test
    fun createsSessionFilesWhenDirectoryDoesNotExistYet() {
        val root = createTempDir(prefix = "vicollector")
        val sessionDirectory = File(root, "2026_06_29_120000_Pixel8_session001")

        StorageManager(sessionDirectory).use { manager ->
            manager.flush()
        }

        assertTrue(File(sessionDirectory, "images").isDirectory)
        assertTrue(File(sessionDirectory, "image_timestamps.csv").isFile)
        assertTrue(File(sessionDirectory, "gyro.csv").isFile)
        assertTrue(File(sessionDirectory, "accel.csv").isFile)
    }
}
