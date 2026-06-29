package com.example.vicollector.core.error

class CameraException(
    message: String,
    cause: Throwable? = null,
) : CaptureException(message, cause)
