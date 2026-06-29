package com.example.vicollector.camera

import android.graphics.ImageFormat

data class CameraConfig(
    val width: Int = 640,
    val height: Int = 480,
    val imageFormat: Int = ImageFormat.JPEG,
    val targetFps: Int = 30,
    val cameraId: String? = null,
)
