package com.example.vicollector.storage

import com.example.vicollector.config.CaptureProfile
import org.json.JSONObject

fun CaptureProfile.toJson(): JSONObject = JSONObject()
    .put("image_width", imageWidth)
    .put("image_height", imageHeight)
    .put("image_format", imageFormat)
    .put("target_fps", targetFps)
    .put("enable_gyro", enableGyro)
    .put("enable_accel", enableAccel)
    .put("sensor_delay", sensorDelay)
    .put("image_storage", imageStorage)
    .put("imu_storage", imuStorage)
    .put("write_metadata", writeMetadata)
    .put("enable_timestamp_diagnostics", enableTimestampDiagnostics)
