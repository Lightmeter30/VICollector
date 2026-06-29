package com.example.vicollector.camera

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Range
import android.util.Size
import org.json.JSONArray
import org.json.JSONObject

data class CameraCapabilities(
    val cameraId: String,
    val outputSizes: List<Size>,
    val fpsRanges: List<Range<Int>>,
    val sensorPhysicalSize: android.util.SizeF?,
    val focalLengths: List<Float>,
    val exposureTimeRangeNs: Range<Long>?,
)

class CameraCapabilityReader(private val cameraManager: CameraManager) {
    fun read(cameraId: String, imageFormat: Int): CameraCapabilities {
        val chars = cameraManager.getCameraCharacteristics(cameraId)
        val streamMap = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        return CameraCapabilities(
            cameraId = cameraId,
            outputSizes = streamMap.outputSizesFor(imageFormat),
            fpsRanges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                ?.toList()
                .orEmpty(),
            sensorPhysicalSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE),
            focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                ?.toList()
                .orEmpty(),
            exposureTimeRangeNs = chars.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE),
        )
    }
}

fun CameraCapabilities.toJson(): JSONObject =
    JSONObject()
        .put("camera_id", cameraId)
        .put(
            "output_sizes",
            JSONArray(outputSizes.map { size ->
                JSONObject()
                    .put("width", size.width)
                    .put("height", size.height)
            })
        )
        .put(
            "fps_ranges",
            JSONArray(fpsRanges.map { range ->
                JSONObject()
                    .put("lower", range.lower)
                    .put("upper", range.upper)
            })
        )
        .put(
            "sensor_physical_size",
            sensorPhysicalSize?.let { size ->
                JSONObject()
                    .put("width_mm", size.width)
                    .put("height_mm", size.height)
            }
        )
        .put("focal_lengths_mm", JSONArray(focalLengths))
        .put(
            "exposure_time_range_ns",
            exposureTimeRangeNs?.let { range ->
                JSONObject()
                    .put("lower", range.lower)
                    .put("upper", range.upper)
            }
        )

private fun StreamConfigurationMap?.outputSizesFor(imageFormat: Int): List<Size> =
    this?.getOutputSizes(imageFormat)?.toList().orEmpty()
