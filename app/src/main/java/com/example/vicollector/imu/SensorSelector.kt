package com.example.vicollector.imu

import android.hardware.Sensor
import android.hardware.SensorManager

class SensorSelector(private val sensorManager: SensorManager) {
    fun gyroscope(): Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    fun accelerometer(): Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
}
