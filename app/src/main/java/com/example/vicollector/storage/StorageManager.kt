package com.example.vicollector.storage

import com.example.vicollector.config.CaptureProfile
import com.example.vicollector.core.model.SensorType
import org.json.JSONObject
import java.io.Closeable
import java.io.File

class StorageManager(
    private val sessionDirectory: File,
    private val metadataWriter: MetadataWriter = MetadataWriter(),
) : Closeable {
    val imagesDirectory: File = File(sessionDirectory, "images")
    val imageWriter: ImageWriter = ImageWriter(imagesDirectory)
    val imageTimestampWriter: ImageTimestampCsvWriter =
        ImageTimestampCsvWriter(File(sessionDirectory, "image_timestamps.csv"))
    val gyroWriter: ImuCsvWriter = ImuCsvWriter(File(sessionDirectory, "gyro.csv"), SensorType.GYROSCOPE)
    val accelWriter: ImuCsvWriter = ImuCsvWriter(File(sessionDirectory, "accel.csv"), SensorType.ACCELEROMETER)

    init {
        sessionDirectory.mkdirs()
        imagesDirectory.mkdirs()
    }

    fun writeSessionConfig(profile: CaptureProfile) {
        metadataWriter.writeJson(File(sessionDirectory, "session_config.json"), profile.toJson())
    }

    fun writeDeviceInfo(json: JSONObject) {
        writeMetadata("device_info.json", json)
    }

    fun writeSensorInfo(json: JSONObject) {
        writeMetadata("sensor_info.json", json)
    }

    fun writeCameraInfo(json: JSONObject) {
        writeMetadata("camera_info.json", json)
    }

    fun writeRuntimeInfo(json: JSONObject) {
        writeMetadata("runtime_info.json", json)
    }

    fun writeMetadata(fileName: String, json: JSONObject) {
        metadataWriter.writeJson(File(sessionDirectory, fileName), json)
    }

    fun flush() {
        imageWriter.flush()
        imageTimestampWriter.flush()
        gyroWriter.flush()
        accelWriter.flush()
    }

    override fun close() {
        flush()
        imageWriter.close()
        imageTimestampWriter.close()
        gyroWriter.close()
        accelWriter.close()
    }
}
