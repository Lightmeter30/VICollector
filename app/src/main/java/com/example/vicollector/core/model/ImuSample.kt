package com.example.vicollector.core.model

data class ImuSample(
    val timestampNs: Long,
    val sensorType: SensorType,
    val x: Float,
    val y: Float,
    val z: Float,
    val accuracy: Int,
)

enum class SensorType {
    GYROSCOPE,
    ACCELEROMETER,
}
