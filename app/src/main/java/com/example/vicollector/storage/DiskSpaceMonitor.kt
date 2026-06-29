package com.example.vicollector.storage

import java.io.File

class DiskSpaceMonitor(private val root: File) {
    fun freeBytes(): Long = root.usableSpace
}
