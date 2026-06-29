package com.example.vicollector.device

import android.hardware.Sensor
import android.hardware.SensorManager
import org.json.JSONArray
import org.json.JSONObject

class DeviceInfoCollector {
    fun collectDeviceInfo(buildInfo: AndroidBuildInfo = AndroidBuildInfo.current()): JSONObject =
        buildInfo.toJson()

    fun collectSensorInfo(sensorManager: SensorManager): JSONObject {
        val sensors = JSONArray()
        sensorManager.getSensorList(Sensor.TYPE_ALL).forEach { sensor ->
            sensors.put(
                SensorDeviceInfo(
                    name = sensor.name,
                    vendor = sensor.vendor,
                    type = sensor.type,
                    version = sensor.version,
                    resolution = sensor.resolution,
                    maximumRange = sensor.maximumRange,
                    minDelayUs = sensor.minDelay,
                ).toJson()
            )
        }
        return JSONObject().put("sensors", sensors)
    }

    fun runtimeInfo(diskFreeBytes: Long, writeQueueSize: Int): JSONObject = JSONObject()
        .put("disk_free_bytes", diskFreeBytes)
        .put("write_queue_size", writeQueueSize)
}
