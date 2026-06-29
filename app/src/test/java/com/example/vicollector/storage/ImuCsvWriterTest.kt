package com.example.vicollector.storage

import com.example.vicollector.core.model.ImuSample
import com.example.vicollector.core.model.SensorType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class ImuCsvWriterTest {
    @Test
    fun writesGyroHeaderAndRows() {
        val file = File.createTempFile("gyro", ".csv")
        ImuCsvWriter(file, SensorType.GYROSCOPE).use { writer ->
            writer.write(ImuSample(10L, SensorType.GYROSCOPE, 1f, 2f, 3f, 3))
        }

        assertEquals(
            listOf("timestamp_ns,gx,gy,gz,accuracy", "10,1.0,2.0,3.0,3"),
            file.readLines()
        )
    }
}
