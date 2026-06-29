package com.example.vicollector.session

import com.example.vicollector.core.model.CaptureStats
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionSummaryGeneratorTest {
    @Test
    fun writesRatesAndStorageFormat() {
        val json = SessionSummaryGenerator().generate(
            sessionId = "session001",
            durationSec = 30.0,
            numImages = 900,
            numGyro = 12_000,
            numAccel = 12_000,
            stats = CaptureStats(30.0, 400.0, 400.0, 0, 0, 30.0, 1_000_000L, 0),
        )

        assertEquals("JPEG+CSV", json.getString("storage_format"))
        assertEquals(30.0, json.getDouble("estimated_camera_fps"), 0.01)
    }
}
