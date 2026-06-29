package com.example.vicollector.core.error

class ImuException(
    message: String,
    cause: Throwable? = null,
) : CaptureException(message, cause)
