package com.example.vicollector.imu

class ImuRateEstimator(private val maxSamples: Int = 256) {
    private val timestamps = ArrayDeque<Long>()

    fun addTimestampNs(timestampNs: Long) {
        timestamps.addLast(timestampNs)
        while (timestamps.size > maxSamples) timestamps.removeFirst()
    }

    fun estimatedHz(): Double {
        if (timestamps.size < 2) return 0.0
        val durationNs = timestamps.last() - timestamps.first()
        if (durationNs <= 0L) return 0.0
        return (timestamps.size - 1) * 1_000_000_000.0 / durationNs
    }
}
