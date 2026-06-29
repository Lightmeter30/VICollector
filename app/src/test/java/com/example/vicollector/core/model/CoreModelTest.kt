package com.example.vicollector.core.model

import com.example.vicollector.config.DefaultProfiles
import com.example.vicollector.core.constant.CaptureConstants
import com.example.vicollector.core.constant.FileConstants
import com.example.vicollector.core.error.CameraException
import com.example.vicollector.core.error.CaptureException
import com.example.vicollector.core.error.ImuException
import com.example.vicollector.core.error.StorageException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreModelTest {
    @Test
    fun imuSampleKeepsRawTimestamp() {
        val sample = ImuSample(123L, SensorType.GYROSCOPE, 0.1f, 0.2f, 0.3f, 3)
        assertEquals(123L, sample.timestampNs)
        assertEquals(SensorType.GYROSCOPE, sample.sensorType)
    }

    @Test
    fun cameraFrameKeepsCaptureMetadata() {
        val frame = CameraFrame(
            frameIndex = 7L,
            timestampNs = 456L,
            width = 640,
            height = 480,
            format = 256,
            exposureTimeNs = 1_000_000L,
            sensitivityIso = 400,
            fileName = "frame_000007.jpg",
        )

        assertEquals(7L, frame.frameIndex)
        assertEquals(456L, frame.timestampNs)
        assertEquals(640, frame.width)
        assertEquals(480, frame.height)
        assertEquals("frame_000007.jpg", frame.fileName)
    }

    @Test
    fun captureStatsKeepsRealtimeMetrics() {
        val stats = CaptureStats(
            cameraFps = 29.8,
            gyroHz = 199.5,
            accelHz = 198.7,
            droppedFrames = 2L,
            writeQueueSize = 3,
            sessionDurationSec = 12.5,
            diskFreeBytes = 1024L,
            errorCount = 1,
        )

        assertEquals(29.8, stats.cameraFps, 0.001)
        assertEquals(199.5, stats.gyroHz, 0.001)
        assertEquals(198.7, stats.accelHz, 0.001)
        assertEquals(2L, stats.droppedFrames)
    }

    @Test
    fun sessionInfoUsesTimestampNsAndDirectoryContract() {
        val deviceInfo = DeviceInfo(
            manufacturer = "Google",
            model = "Pixel",
            androidVersion = "15",
            sdkInt = 35,
        )
        val session = SessionInfo(
            sessionId = "20260629_120000",
            startedAtTimestampNs = 1_000L,
            outputDirectoryName = "20260629_120000",
            captureProfileName = "default640x48030Fps",
            deviceInfo = deviceInfo,
        )

        assertEquals("Pixel", session.deviceInfo.model)
        assertEquals(1_000L, session.startedAtTimestampNs)
        assertEquals("20260629_120000", session.outputDirectoryName)
    }

    @Test
    fun defaultProfileMatchesPhaseOneContract() {
        val profile = DefaultProfiles.default640x48030Fps

        assertEquals(640, profile.imageWidth)
        assertEquals(480, profile.imageHeight)
        assertEquals("JPEG", profile.imageFormat)
        assertEquals(30, profile.targetFps)
        assertTrue(profile.enableGyro)
        assertTrue(profile.enableAccel)
        assertTrue(profile.enableTimestampDiagnostics)
    }

    @Test
    fun constantsExposeStorageAndSamplingDefaults() {
        assertEquals(640, CaptureConstants.DEFAULT_IMAGE_WIDTH)
        assertEquals(480, CaptureConstants.DEFAULT_IMAGE_HEIGHT)
        assertEquals(30, CaptureConstants.DEFAULT_TARGET_FPS)
        assertEquals("metadata.json", FileConstants.METADATA_FILE_NAME)
        assertEquals("imu.csv", FileConstants.IMU_FILE_NAME)
    }

    @Test
    fun specificExceptionsInheritCaptureException() {
        val errors: List<CaptureException> = listOf(
            CameraException("camera"),
            ImuException("imu"),
            StorageException("storage"),
        )

        assertEquals(listOf("camera", "imu", "storage"), errors.map { it.message })
    }
}
