package com.example.vicollector.config

data class CaptureProfile(
    val imageWidth: Int,
    val imageHeight: Int,
    val imageFormat: String,
    val targetFps: Int,
    val enableGyro: Boolean,
    val enableAccel: Boolean,
    val sensorDelay: String,
    val imageStorage: String,
    val imuStorage: String,
    val writeMetadata: Boolean,
    val enableTimestampDiagnostics: Boolean,
)
