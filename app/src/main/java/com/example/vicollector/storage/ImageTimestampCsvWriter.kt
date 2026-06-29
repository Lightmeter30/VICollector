package com.example.vicollector.storage

import com.example.vicollector.core.model.CameraFrame
import java.io.BufferedWriter
import java.io.Closeable
import java.io.File
import java.io.FileWriter

class ImageTimestampCsvWriter(file: File) : Closeable {
    private val writer = BufferedWriter(FileWriter(file))

    init {
        writer.write("frame_index,timestamp_ns,file_name,width,height,exposure_time_ns,iso")
        writer.newLine()
    }

    fun write(frame: CameraFrame) {
        writer.write(
            "${frame.frameIndex},${frame.timestampNs},${frame.fileName},${frame.width},${frame.height}," +
                "${frame.exposureTimeNs ?: ""},${frame.sensitivityIso ?: ""}"
        )
        writer.newLine()
    }

    fun flush() = writer.flush()

    override fun close() = writer.close()
}
