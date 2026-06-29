package com.example.vicollector.core.model

data class SessionInfo(
    val sessionId: String,
    val startedAtTimestampNs: Long,
    val outputDirectoryName: String,
    val captureProfileName: String,
    val deviceInfo: DeviceInfo,
)
