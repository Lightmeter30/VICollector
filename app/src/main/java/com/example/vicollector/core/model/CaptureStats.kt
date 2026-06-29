package com.example.vicollector.core.model

data class CaptureStats(
    val cameraFps: Double,
    val gyroHz: Double,
    val accelHz: Double,
    val droppedFrames: Long,
    val writeQueueSize: Int,
    val sessionDurationSec: Double,
    val diskFreeBytes: Long,
    val errorCount: Int,
)
