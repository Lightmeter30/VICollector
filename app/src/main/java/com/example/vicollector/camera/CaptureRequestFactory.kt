package com.example.vicollector.camera

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.util.Range
import android.view.Surface

class CaptureRequestFactory {
    fun createRepeatingRequest(
        cameraDevice: CameraDevice,
        characteristics: CameraCharacteristics,
        imageSurface: Surface,
        targetFps: Int,
    ): CaptureRequest {
        val builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        builder.addTarget(imageSurface)
        builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
        chooseFpsRange(
            characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                ?.toList()
                .orEmpty(),
            targetFps,
        )?.let { range ->
            builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, range)
        }
        return builder.build()
    }

    private fun chooseFpsRange(ranges: Iterable<Range<Int>>, targetFps: Int): Range<Int>? =
        ranges.minWithOrNull(
            compareBy<Range<Int>>(
                { kotlin.math.abs(it.upper - targetFps) },
                { kotlin.math.abs(it.lower - targetFps) },
                { it.upper - it.lower },
            )
        )
}
