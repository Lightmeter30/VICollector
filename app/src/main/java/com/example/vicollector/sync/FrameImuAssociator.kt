package com.example.vicollector.sync

class FrameImuAssociator {
    fun countNearbySamples(
        frameTimestampNs: Long,
        imuTimestampsNs: List<Long>,
        windowNs: Long,
    ): Int = imuTimestampsNs.count { timestampNs ->
        kotlin.math.abs(timestampNs - frameTimestampNs) <= windowNs
    }
}
