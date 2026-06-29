package com.example.vicollector.imu

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.vicollector.core.model.ImuSample

class ImuController(
    private val sensorManager: SensorManager,
    private val config: ImuConfig = ImuConfig(),
    onSample: (ImuSample) -> Unit,
) : SensorEventListener {
    private val selector = SensorSelector(sensorManager)
    private val dispatcher = SensorEventDispatcher(onSample)

    fun startImu() {
        if (config.enableGyro) {
            selector.gyroscope()?.let { sensor ->
                sensorManager.registerListener(this, sensor, config.samplingPeriodUs)
            }
        }
        if (config.enableAccel) {
            selector.accelerometer()?.let { sensor ->
                sensorManager.registerListener(this, sensor, config.samplingPeriodUs)
            }
        }
    }

    fun stopImu() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        dispatcher.dispatch(event)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
