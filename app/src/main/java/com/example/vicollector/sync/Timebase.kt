package com.example.vicollector.sync

object Timebase {
    fun nsToSec(timestampNs: Long): Double = timestampNs / 1_000_000_000.0
}
