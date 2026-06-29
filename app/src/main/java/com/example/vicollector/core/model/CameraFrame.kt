package com.example.vicollector.core.model

data class CameraFrame(
    val frameIndex: Long,
    val timestampNs: Long,
    val width: Int,
    val height: Int,
    val format: Int,
    val exposureTimeNs: Long?,
    val sensitivityIso: Int?,
    val fileName: String,
)
