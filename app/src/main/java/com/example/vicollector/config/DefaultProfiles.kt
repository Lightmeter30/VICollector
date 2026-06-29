package com.example.vicollector.config

object DefaultProfiles {
    val default640x48030Fps = CaptureProfile(
        imageWidth = 640,
        imageHeight = 480,
        imageFormat = "JPEG",
        targetFps = 30,
        enableGyro = true,
        enableAccel = true,
        sensorDelay = "FASTEST",
        imageStorage = "JPEG",
        imuStorage = "CSV",
        writeMetadata = true,
        enableTimestampDiagnostics = true,
    )
}
