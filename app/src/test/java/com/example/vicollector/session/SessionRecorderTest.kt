package com.example.vicollector.session

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionRecorderTest {
    @Test
    fun reportsDroppedFramesAndCameraTimestampDiagnostics() {
        val recorder = SessionRecorder(targetFps = 10, startedAtMs = 0L)

        recorder.recordImage(0L, 1)
        recorder.recordImage(100_000_000L, 2)
        recorder.recordImage(300_000_000L, 1)
        val stats = recorder.stats(nowMs = 1_000L, diskFreeBytes = 123L, currentWriteQueueSize = 0)
        val diagnostics = recorder.diagnostics(stats)

        assertEquals(1L, stats.droppedFrames)
        assertEquals(2, diagnostics.getInt("write_queue_max_size"))
        assertEquals(
            1,
            diagnostics.getJSONObject("camera_timestamps").getInt("abnormal_interval_count"),
        )
    }
}
