package com.example.vicollector.storage

import com.example.vicollector.core.model.ImuSample
import com.example.vicollector.core.model.SensorType
import java.io.BufferedWriter
import java.io.Closeable
import java.io.File
import java.io.FileWriter

class ImuCsvWriter(file: File, private val type: SensorType) : Closeable {
    private val writer = BufferedWriter(FileWriter(file))

    init {
        writer.write(
            when (type) {
                SensorType.GYROSCOPE -> "timestamp_ns,gx,gy,gz,accuracy"
                SensorType.ACCELEROMETER -> "timestamp_ns,ax,ay,az,accuracy"
            }
        )
        writer.newLine()
    }

    fun write(sample: ImuSample) {
        require(sample.sensorType == type)
        writer.write("${sample.timestampNs},${sample.x},${sample.y},${sample.z},${sample.accuracy}")
        writer.newLine()
    }

    fun flush() = writer.flush()

    override fun close() = writer.close()
}
