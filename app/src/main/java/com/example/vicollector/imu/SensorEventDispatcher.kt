package com.example.vicollector.imu

import android.hardware.Sensor
import android.hardware.SensorEvent
import com.example.vicollector.core.model.ImuSample
import com.example.vicollector.core.model.SensorType

class SensorEventDispatcher(private val onSample: (ImuSample) -> Unit) {
    fun dispatch(event: SensorEvent) {
        val type = when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> SensorType.GYROSCOPE
            Sensor.TYPE_ACCELEROMETER -> SensorType.ACCELEROMETER
            else -> return
        }
        if (event.values.size < 3) return
        onSample(
            ImuSample(
                timestampNs = event.timestamp,
                sensorType = type,
                x = event.values[0],
                y = event.values[1],
                z = event.values[2],
                accuracy = event.accuracy,
            )
        )
    }
}
