package com.example.vicollector.imu

import com.example.vicollector.core.model.ImuSample
import com.example.vicollector.core.model.SensorType

class ImuBuffer(private val maxSamples: Int = 2048) {
    private val samples = ArrayDeque<ImuSample>()

    fun add(sample: ImuSample) {
        samples.addLast(sample)
        while (samples.size > maxSamples) samples.removeFirst()
    }

    fun snapshot(): List<ImuSample> = samples.toList()

    fun count(type: SensorType): Int = samples.count { it.sensorType == type }

    fun clear() {
        samples.clear()
    }
}
