package com.example.vicollector.storage

import java.io.File
import java.io.Closeable

class ImageWriter(private val imagesDirectory: File? = null) : Closeable {
    init {
        imagesDirectory?.mkdirs()
    }

    fun write(fileName: String, bytes: ByteArray): File {
        val directory = requireNotNull(imagesDirectory) { "imagesDirectory is required for named image writes" }
        val file = File(directory, fileName)
        writeJpeg(file, bytes)
        return file
    }

    fun writeJpeg(file: File, jpegBytes: ByteArray) {
        file.parentFile?.mkdirs()
        file.writeBytes(jpegBytes)
    }

    fun flush() {
        // File.writeBytes is synchronous; this method keeps the writer contract uniform.
    }

    override fun close() {
        flush()
    }
}
