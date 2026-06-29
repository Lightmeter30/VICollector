package com.example.vicollector.device

import org.json.JSONObject

data class SensorDeviceInfo(
    val name: String,
    val vendor: String,
    val type: Int,
    val version: Int,
    val resolution: Float,
    val maximumRange: Float,
    val minDelayUs: Int,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("name", name)
        .put("vendor", vendor)
        .put("type", type)
        .put("version", version)
        .put("resolution", resolution)
        .put("maximum_range", maximumRange)
        .put("min_delay_us", minDelayUs)
}
