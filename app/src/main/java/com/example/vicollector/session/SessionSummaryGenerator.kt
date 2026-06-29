package com.example.vicollector.session

import com.example.vicollector.core.model.CaptureStats
import org.json.JSONObject

class SessionSummaryGenerator {
    fun generate(
        sessionId: String,
        durationSec: Double,
        numImages: Long,
        numGyro: Long,
        numAccel: Long,
        stats: CaptureStats,
        config: SessionConfig = SessionConfig(),
    ): JSONObject = JSONObject()
        .put("session_id", sessionId)
        .put("duration_sec", durationSec)
        .put("num_images", numImages)
        .put("num_gyro", numGyro)
        .put("num_accel", numAccel)
        .put("estimated_camera_fps", stats.cameraFps)
        .put("estimated_gyro_hz", stats.gyroHz)
        .put("estimated_accel_hz", stats.accelHz)
        .put("dropped_frames", stats.droppedFrames)
        .put("write_queue_size", stats.writeQueueSize)
        .put("disk_free_bytes", stats.diskFreeBytes)
        .put("error_count", stats.errorCount)
        .put("storage_format", "${config.imageStorage}+${config.imuStorage}")
}
