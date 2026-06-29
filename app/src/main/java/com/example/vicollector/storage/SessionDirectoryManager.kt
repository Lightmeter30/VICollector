package com.example.vicollector.storage

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionDirectoryManager(private val datasetRoot: File) {
    fun createSessionDirectory(deviceModel: String, index: Int, now: Date = Date()): File {
        val time = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US).format(now)
        val safeModel = deviceModel.replace(Regex("[^A-Za-z0-9_-]"), "")
        val dir = File(datasetRoot, "${time}_${safeModel}_session%03d".format(index))
        File(dir, "images").mkdirs()
        return dir
    }
}
