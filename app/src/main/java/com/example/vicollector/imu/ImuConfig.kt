package com.example.vicollector.imu

data class ImuConfig(
    val enableGyro: Boolean = true,
    val enableAccel: Boolean = true,
    val samplingPeriodUs: Int = 2_500,
)
