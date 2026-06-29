package com.example.vicollector.core.error

open class CaptureException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
