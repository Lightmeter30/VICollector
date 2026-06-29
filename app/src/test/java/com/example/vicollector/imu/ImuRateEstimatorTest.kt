package com.example.vicollector.imu

import org.junit.Assert.assertEquals
import org.junit.Test

class ImuRateEstimatorTest {
    @Test
    fun estimatesHzFromNanosecondIntervals() {
        val estimator = ImuRateEstimator()
        listOf(0L, 10_000_000L, 20_000_000L, 30_000_000L).forEach(estimator::addTimestampNs)
        assertEquals(100.0, estimator.estimatedHz(), 0.01)
    }
}
