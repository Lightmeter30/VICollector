package com.example.vicollector.storage

import java.io.Closeable
import java.io.File

class ImageWriter(private val imagesDirectory: File) : Closeable {
    init {
        imagesDirectory.mkdirs()
    }

    fun write(fileName: String, bytes: ByteArray): File {
        val file = File(imagesDirectory, fileName)
        file.writeBytes(bytes)
        return file
    }

    fun flush() {
        // File.writeBytes is synchronous; this method keeps the writer contract uniform.
    }

    override fun close() {
        flush()
    }
}
