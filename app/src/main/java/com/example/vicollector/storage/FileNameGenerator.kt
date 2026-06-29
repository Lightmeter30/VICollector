package com.example.vicollector.storage

class FileNameGenerator {
    fun imageFileName(frameIndex: Long): String = "%06d.jpg".format(frameIndex)
}
