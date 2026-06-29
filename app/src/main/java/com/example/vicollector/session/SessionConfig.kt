package com.example.vicollector.session

data class SessionConfig(
    val imageWidth: Int = 640,
    val imageHeight: Int = 480,
    val imageFormat: String = "JPEG",
    val targetFps: Int = 30,
    val enableGyro: Boolean = true,
    val enableAccel: Boolean = true,
    val sensorDelay: String = "FASTEST",
    val imageStorage: String = "JPEG",
    val imuStorage: String = "CSV",
    val enableTimestampDiagnostics: Boolean = true,
)
