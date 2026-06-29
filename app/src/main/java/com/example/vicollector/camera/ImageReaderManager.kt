package com.example.vicollector.camera

import android.media.ImageReader
import android.os.Handler

class ImageReaderManager(private val config: CameraConfig) {
    fun createReader(handler: Handler, onImage: (timestampNs: Long, bytes: ByteArray) -> Unit): ImageReader {
        val reader = ImageReader.newInstance(config.width, config.height, config.imageFormat, 4)
        reader.setOnImageAvailableListener({ imageReader ->
            val image = imageReader.acquireNextImage() ?: return@setOnImageAvailableListener
            image.use {
                val buffer = it.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                onImage(it.timestamp, bytes)
            }
        }, handler)
        return reader
    }
}
