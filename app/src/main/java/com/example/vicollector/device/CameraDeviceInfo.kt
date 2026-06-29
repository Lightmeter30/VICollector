package com.example.vicollector.device

import org.json.JSONArray
import org.json.JSONObject

data class CameraDeviceInfo(
    val cameraId: String,
    val facing: String,
    val supportedOutputFormats: List<String>,
    val supportedResolutions: List<String>,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("camera_id", cameraId)
        .put("facing", facing)
        .put("supported_output_formats", JSONArray(supportedOutputFormats))
        .put("supported_resolutions", JSONArray(supportedResolutions))
}
