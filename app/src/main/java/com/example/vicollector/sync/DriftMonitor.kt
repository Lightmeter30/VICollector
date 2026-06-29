package com.example.vicollector.sync

data class DriftSnapshot(
    val firstTimestampNs: Long,
    val lastTimestampNs: Long,
    val durationSec: Double,
)

class DriftMonitor {
    fun snapshot(timestampsNs: List<Long>): DriftSnapshot {
        if (timestampsNs.isEmpty()) {
            return DriftSnapshot(0L, 0L, 0.0)
        }
        val first = timestampsNs.first()
        val last = timestampsNs.last()
        return DriftSnapshot(first, last, Timebase.nsToSec(last - first))
    }
}
