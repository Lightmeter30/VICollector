package com.example.vicollector.camera

import android.hardware.camera2.CaptureResult
import com.example.vicollector.core.model.CameraFrame
import java.util.concurrent.atomic.AtomicLong

class CameraMetadataLogger(
    private val config: CameraConfig,
    private val fileNameForIndex: (Long) -> String,
) {
    private val frameIndex = AtomicLong(0)

    fun nextFrame(timestampNs: Long, result: CaptureResult? = null): CameraFrame {
        val index = frameIndex.getAndIncrement()
        return CameraFrame(
            frameIndex = index,
            timestampNs = timestampNs,
            width = config.width,
            height = config.height,
            format = config.imageFormat,
            exposureTimeNs = result?.get(CaptureResult.SENSOR_EXPOSURE_TIME),
            sensitivityIso = result?.get(CaptureResult.SENSOR_SENSITIVITY),
            fileName = fileNameForIndex(index),
        )
    }
}
