package com.example.vicollector

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.example.vicollector.camera.CameraConfig
import com.example.vicollector.camera.CameraController
import com.example.vicollector.camera.CameraCapabilityReader
import com.example.vicollector.camera.CameraDeviceSelector
import com.example.vicollector.camera.toJson
import com.example.vicollector.config.DefaultProfiles
import com.example.vicollector.core.model.SensorType
import com.example.vicollector.device.DeviceInfoCollector
import com.example.vicollector.imu.ImuConfig
import com.example.vicollector.imu.ImuController
import com.example.vicollector.storage.AsyncWriteQueue
import com.example.vicollector.storage.SessionDirectoryManager
import com.example.vicollector.storage.StorageManager
import java.io.File
import java.util.concurrent.atomic.AtomicLong

class MainActivity : ComponentActivity() {
    private lateinit var stateText: TextView
    private lateinit var cameraFpsText: TextView
    private lateinit var gyroHzText: TextView
    private lateinit var accelHzText: TextView
    private lateinit var diskFreeText: TextView
    private lateinit var actionButton: Button

    private var storageManager: StorageManager? = null
    private var writeQueue: AsyncWriteQueue? = null
    private var imuController: ImuController? = null
    private var cameraController: CameraController? = null
    private var recording = false

    private val imageCount = AtomicLong(0)
    private val gyroCount = AtomicLong(0)
    private val accelCount = AtomicLong(0)
    private val lastUiUpdateMs = AtomicLong(0)
    private var recordingStartedAtMs: Long = 0L

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startRecording()
        } else {
            showError("Camera permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        stateText = TextView(this).apply { text = "State: IDLE" }
        cameraFpsText = TextView(this).apply { text = "Camera FPS: 0.0" }
        gyroHzText = TextView(this).apply { text = "Gyro Hz: 0.0" }
        accelHzText = TextView(this).apply { text = "Accel Hz: 0.0" }
        diskFreeText = TextView(this).apply { text = "Disk Free: ${filesDir.freeSpace}" }
        listOf(stateText, cameraFpsText, gyroHzText, accelHzText, diskFreeText).forEach(layout::addView)
        actionButton = Button(this).apply {
            text = "Start Recording"
            setOnClickListener {
                if (recording) {
                    stopRecording()
                } else {
                    ensureCameraPermissionThenStart()
                }
            }
        }
        layout.addView(actionButton)
        setContentView(layout)
    }

    override fun onDestroy() {
        if (recording) stopRecording()
        super.onDestroy()
    }

    private fun ensureCameraPermissionThenStart() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startRecording()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startRecording() {
        if (recording) return
        stateText.text = "State: PREPARING"
        imageCount.set(0)
        gyroCount.set(0)
        accelCount.set(0)
        lastUiUpdateMs.set(0)
        recordingStartedAtMs = SystemClock.elapsedRealtime()

        val profile = DefaultProfiles.default640x48030Fps
        val sessionDirectory = SessionDirectoryManager(datasetRoot()).createSessionDirectory(
            deviceModel = android.os.Build.MODEL,
            index = nextSessionIndex(),
        )
        val storage = StorageManager(sessionDirectory)
        val queue = AsyncWriteQueue()
        storageManager = storage
        writeQueue = queue
        storage.writeSessionConfig(profile)

        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val deviceInfoCollector = DeviceInfoCollector()
        storage.writeDeviceInfo(deviceInfoCollector.collectDeviceInfo())
        storage.writeSensorInfo(deviceInfoCollector.collectSensorInfo(sensorManager))
        val selectedCameraId = CameraDeviceSelector(cameraManager).selectBackMainCamera()
        storage.writeCameraInfo(
            CameraCapabilityReader(cameraManager)
                .read(selectedCameraId, CameraConfig().imageFormat)
                .toJson()
        )

        imuController = ImuController(
            sensorManager = sensorManager,
            config = ImuConfig(
                enableGyro = profile.enableGyro,
                enableAccel = profile.enableAccel,
            ),
        ) { sample ->
            queue.enqueue {
                when (sample.sensorType) {
                    SensorType.GYROSCOPE -> {
                        storage.gyroWriter.write(sample)
                        gyroCount.incrementAndGet()
                    }
                    SensorType.ACCELEROMETER -> {
                        storage.accelWriter.write(sample)
                        accelCount.incrementAndGet()
                    }
                }
                requestStatsUpdate()
            }
        }

        cameraController = CameraController(
            context = this,
            cameraManager = cameraManager,
            config = CameraConfig(
                width = profile.imageWidth,
                height = profile.imageHeight,
                targetFps = profile.targetFps,
                cameraId = selectedCameraId,
            ),
            onImage = { frame, jpegBytes ->
                queue.enqueue {
                    storage.imageWriter.write(frame.fileName, jpegBytes)
                    storage.imageTimestampWriter.write(frame)
                    imageCount.incrementAndGet()
                    requestStatsUpdate()
                }
            },
            onError = { error -> runOnUiThread { showError(error.message ?: error.javaClass.simpleName) } },
        )

        try {
            imuController?.startImu()
            cameraController?.startCamera()
            recording = true
            actionButton.text = "Stop Recording"
            stateText.text = "State: RECORDING"
            diskFreeText.text = "Disk Free: ${sessionDirectory.freeSpace}"
        } catch (error: Throwable) {
            showError(error.message ?: error.javaClass.simpleName)
            stopRecording()
        }
    }

    private fun stopRecording() {
        stateText.text = "State: STOPPING"
        cameraController?.stopCamera()
        imuController?.stopImu()
        writeQueue?.flushAndStop()
        storageManager?.close()
        cameraController = null
        imuController = null
        writeQueue = null
        storageManager = null
        recording = false
        actionButton.text = "Start Recording"
        stateText.text = "State: FINISHED"
        updateStats()
    }

    private fun requestStatsUpdate() {
        val nowMs = SystemClock.elapsedRealtime()
        val lastMs = lastUiUpdateMs.get()
        if (nowMs - lastMs < 250L) return
        if (lastUiUpdateMs.compareAndSet(lastMs, nowMs)) {
            runOnUiThread { updateStats() }
        }
    }

    private fun updateStats() {
        val elapsedSec = ((SystemClock.elapsedRealtime() - recordingStartedAtMs).coerceAtLeast(1L)) / 1000.0
        cameraFpsText.text = "Camera FPS: %.1f (%d images)".format(imageCount.get() / elapsedSec, imageCount.get())
        gyroHzText.text = "Gyro Hz: %.1f (%d rows)".format(gyroCount.get() / elapsedSec, gyroCount.get())
        accelHzText.text = "Accel Hz: %.1f (%d rows)".format(accelCount.get() / elapsedSec, accelCount.get())
        diskFreeText.text = "Disk Free: ${datasetRoot().freeSpace}"
    }

    private fun datasetRoot(): File =
        File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: filesDir, "VICollector")

    private fun nextSessionIndex(): Int = datasetRoot().listFiles()?.size?.plus(1) ?: 1

    private fun showError(message: String) {
        stateText.text = "State: ERROR"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
