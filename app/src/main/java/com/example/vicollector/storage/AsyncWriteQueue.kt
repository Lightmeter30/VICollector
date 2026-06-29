package com.example.vicollector.storage

import java.io.Closeable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class AsyncWriteQueue : Closeable {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val pending = AtomicInteger(0)

    fun enqueue(write: () -> Unit) {
        pending.incrementAndGet()
        executor.execute {
            try {
                write()
            } finally {
                pending.decrementAndGet()
            }
        }
    }

    fun size(): Int = pending.get()

    fun flushAndStop(timeoutSec: Long = 10) {
        executor.shutdown()
        executor.awaitTermination(timeoutSec, TimeUnit.SECONDS)
    }

    override fun close() = flushAndStop()
}
