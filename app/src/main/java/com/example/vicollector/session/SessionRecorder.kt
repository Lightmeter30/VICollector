package com.example.vicollector.session

import com.example.vicollector.core.model.CaptureStats
import com.example.vicollector.sync.TimestampDiagnostics
import org.json.JSONObject
import kotlin.math.roundToLong

class SessionRecorder(
    private val targetFps: Int,
    private val startedAtMs: Long,
) {
    private val lock = Any()
    private val cameraExpectedIntervalNs: Long =
        if (targetFps > 0) (1_000_000_000L / targetFps) else 0L
    private val imageTimestampsNs = mutableListOf<Long>()
    private val gyroTimestampsNs = mutableListOf<Long>()
    private val accelTimestampsNs = mutableListOf<Long>()
    private var maxWriteQueueSize = 0
    private var errorCount = 0

    val numImages: Long
        get() = synchronized(lock) { imageTimestampsNs.size.toLong() }

    val numGyro: Long
        get() = synchronized(lock) { gyroTimestampsNs.size.toLong() }

    val numAccel: Long
        get() = synchronized(lock) { accelTimestampsNs.size.toLong() }

    fun recordImage(timestampNs: Long, writeQueueSize: Int) {
        synchronized(lock) {
            imageTimestampsNs += timestampNs
            recordWriteQueueSize(writeQueueSize)
        }
    }

    fun recordGyro(timestampNs: Long, writeQueueSize: Int) {
        synchronized(lock) {
            gyroTimestampsNs += timestampNs
            recordWriteQueueSize(writeQueueSize)
        }
    }

    fun recordAccel(timestampNs: Long, writeQueueSize: Int) {
        synchronized(lock) {
            accelTimestampsNs += timestampNs
            recordWriteQueueSize(writeQueueSize)
        }
    }

    fun recordError() {
        synchronized(lock) {
            errorCount += 1
        }
    }

    fun stats(nowMs: Long, diskFreeBytes: Long, currentWriteQueueSize: Int): CaptureStats {
        synchronized(lock) {
            recordWriteQueueSize(currentWriteQueueSize)
            val durationSec = durationSec(nowMs)
            return CaptureStats(
                cameraFps = rate(imageTimestampsNs.size.toLong(), durationSec),
                gyroHz = rate(gyroTimestampsNs.size.toLong(), durationSec),
                accelHz = rate(accelTimestampsNs.size.toLong(), durationSec),
                droppedFrames = estimateDroppedFrames(imageTimestampsNs),
                writeQueueSize = currentWriteQueueSize,
                sessionDurationSec = durationSec,
                diskFreeBytes = diskFreeBytes,
                errorCount = errorCount,
            )
        }
    }

    fun diagnostics(stats: CaptureStats): JSONObject {
        val timestampDiagnostics = TimestampDiagnostics()
        val imageTimestamps: List<Long>
        val gyroTimestamps: List<Long>
        val accelTimestamps: List<Long>
        val queueMaxSize: Int
        synchronized(lock) {
            imageTimestamps = imageTimestampsNs.toList()
            gyroTimestamps = gyroTimestampsNs.toList()
            accelTimestamps = accelTimestampsNs.toList()
            queueMaxSize = maxWriteQueueSize
        }
        return JSONObject()
            .put(
                "camera_timestamps",
                timestampDiagnostics.analyze(imageTimestamps)
                    .toJson(abnormalIntervalCount(imageTimestamps, cameraExpectedIntervalNs)),
            )
            .put("gyro_timestamps", timestampDiagnostics.analyze(gyroTimestamps).toJson())
            .put("accel_timestamps", timestampDiagnostics.analyze(accelTimestamps).toJson())
            .put("dropped_frames", stats.droppedFrames)
            .put("write_queue_max_size", queueMaxSize)
            .put("write_queue_size", stats.writeQueueSize)
            .put("error_count", stats.errorCount)
    }

    private fun durationSec(nowMs: Long): Double =
        ((nowMs - startedAtMs).coerceAtLeast(1L)) / 1000.0

    private fun rate(count: Long, durationSec: Double): Double = count / durationSec

    private fun recordWriteQueueSize(size: Int) {
        if (size > maxWriteQueueSize) maxWriteQueueSize = size
    }

    private fun estimateDroppedFrames(imageTimestamps: List<Long>): Long {
        if (cameraExpectedIntervalNs <= 0L || imageTimestamps.size < 2) return 0L
        return imageTimestamps
            .zipWithNext { previous, current -> current - previous }
            .filter { it > 0L }
            .sumOf { intervalNs ->
                ((intervalNs / cameraExpectedIntervalNs.toDouble()).roundToLong() - 1L).coerceAtLeast(0L)
            }
    }

    private fun abnormalIntervalCount(timestampsNs: List<Long>, expectedIntervalNs: Long): Int {
        if (expectedIntervalNs <= 0L || timestampsNs.size < 2) return 0
        val lowerBound = expectedIntervalNs / 2
        val upperBound = expectedIntervalNs * 3 / 2
        return timestampsNs
            .zipWithNext { previous, current -> current - previous }
            .count { it > 0L && (it < lowerBound || it > upperBound) }
    }
}

private fun com.example.vicollector.sync.TimestampDiagnosticsResult.toJson(
    abnormalIntervalCount: Int = 0,
): JSONObject =
    JSONObject()
        .put("count", count)
        .put("duplicate_count", duplicateCount)
        .put("non_monotonic_count", nonMonotonicCount)
        .put("min_interval_ns", minIntervalNs)
        .put("max_interval_ns", maxIntervalNs)
        .put("abnormal_interval_count", abnormalIntervalCount)
