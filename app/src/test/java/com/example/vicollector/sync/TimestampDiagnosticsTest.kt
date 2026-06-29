package com.example.vicollector.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class TimestampDiagnosticsTest {
    @Test
    fun detectsNonMonotonicTimestamps() {
        val result = TimestampDiagnostics().analyze(listOf(10L, 20L, 15L, 30L))

        assertEquals(1, result.nonMonotonicCount)
    }

    @Test
    fun detectsDuplicateTimestampsAndIntervals() {
        val result = TimestampDiagnostics().analyze(listOf(10L, 10L, 30L, 90L))

        assertEquals(4, result.count)
        assertEquals(1, result.duplicateCount)
        assertEquals(0L, result.minIntervalNs)
        assertEquals(60L, result.maxIntervalNs)
    }
}
