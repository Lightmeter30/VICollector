package com.example.vicollector.core.error

class StorageException(
    message: String,
    cause: Throwable? = null,
) : CaptureException(message, cause)
