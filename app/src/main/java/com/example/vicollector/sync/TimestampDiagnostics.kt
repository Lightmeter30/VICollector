package com.example.vicollector.sync

data class TimestampDiagnosticsResult(
    val count: Int,
    val duplicateCount: Int,
    val nonMonotonicCount: Int,
    val minIntervalNs: Long,
    val maxIntervalNs: Long,
)

class TimestampDiagnostics {
    fun analyze(timestampsNs: List<Long>): TimestampDiagnosticsResult {
        if (timestampsNs.size < 2) {
            return TimestampDiagnosticsResult(
                count = timestampsNs.size,
                duplicateCount = 0,
                nonMonotonicCount = 0,
                minIntervalNs = 0L,
                maxIntervalNs = 0L,
            )
        }
        val intervals = timestampsNs.zipWithNext { previous, current -> current - previous }
        return TimestampDiagnosticsResult(
            count = timestampsNs.size,
            duplicateCount = intervals.count { it == 0L },
            nonMonotonicCount = intervals.count { it < 0L },
            minIntervalNs = intervals.min(),
            maxIntervalNs = intervals.max(),
        )
    }
}
