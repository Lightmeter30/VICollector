package com.example.vicollector.storage

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SessionDirectoryManagerTest {
    @Test
    fun createSessionDirectoryCreatesImagesDirectory() {
        val root = createTempDir(prefix = "vicollector")
        val dir = SessionDirectoryManager(root).createSessionDirectory("Pixel8", 1)

        assertTrue(File(dir, "images").isDirectory)
        assertTrue(dir.name.endsWith("_Pixel8_session001"))
    }
}
