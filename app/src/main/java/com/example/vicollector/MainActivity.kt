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
import com.example.vicollector.core.model.CaptureStats
import com.example.vicollector.core.model.SensorType
import com.example.vicollector.device.DeviceInfoCollector
import com.example.vicollector.imu.ImuConfig
import com.example.vicollector.imu.ImuController
import com.example.vicollector.session.CaptureSessionController
import com.example.vicollector.session.SessionConfig
import com.example.vicollector.session.SessionRecorder
import com.example.vicollector.session.SessionState
import com.example.vicollector.session.SessionSummaryGenerator
import com.example.vicollector.session.SessionValidator
import com.example.vicollector.storage.AsyncWriteQueue
import com.example.vicollector.storage.MetadataWriter
import com.example.vicollector.storage.SessionDirectoryManager
import com.example.vicollector.storage.StorageManager
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var stateText: TextView
    private lateinit var cameraFpsText: TextView
    private lateinit var gyroHzText: TextView
    private lateinit var accelHzText: TextView
    private lateinit var droppedFramesText: TextView
    private lateinit var queueSizeText: TextView
    private lateinit var diskFreeText: TextView
    private lateinit var durationText: TextView
    private lateinit var actionButton: Button

    private var storageManager: StorageManager? = null
    private var writeQueue: AsyncWriteQueue? = null
    private var imuController: ImuController? = null
    private var cameraController: CameraController? = null
    private var sessionController: CaptureSessionController? = null
    private var sessionRecorder: SessionRecorder? = null
    private var lastFinishedStats: CaptureStats? = null
    private var sessionDirectory: File? = null
    private var sessionConfig: SessionConfig = SessionConfig()
    private var recording = false

    private val uiHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val statsRefresh = object : Runnable {
        override fun run() {
            updateStats()
            if (recording) uiHandler.postDelayed(this, 1_000L)
        }
    }

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
        droppedFramesText = TextView(this).apply { text = "Dropped Frames: 0" }
        queueSizeText = TextView(this).apply { text = "Write Queue Size: 0" }
        diskFreeText = TextView(this).apply { text = "Disk Free: ${filesDir.freeSpace}" }
        durationText = TextView(this).apply { text = "Session Duration: 0.0 s" }
        listOf(
            stateText,
            cameraFpsText,
            gyroHzText,
            accelHzText,
            droppedFramesText,
            queueSizeText,
            diskFreeText,
            durationText,
        ).forEach(layout::addView)
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
        actionButton.isEnabled = false
        recordingStartedAtMs = SystemClock.elapsedRealtime()

        val profile = DefaultProfiles.default640x48030Fps
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val deviceInfoCollector = DeviceInfoCollector()
        sessionConfig = SessionConfig(
            imageWidth = profile.imageWidth,
            imageHeight = profile.imageHeight,
            imageFormat = profile.imageFormat,
            targetFps = profile.targetFps,
            enableGyro = profile.enableGyro,
            enableAccel = profile.enableAccel,
            sensorDelay = profile.sensorDelay,
            imageStorage = profile.imageStorage,
            imuStorage = profile.imuStorage,
            enableTimestampDiagnostics = profile.enableTimestampDiagnostics,
        )
        sessionRecorder = SessionRecorder(profile.targetFps, recordingStartedAtMs)
        lastFinishedStats = null

        sessionController = CaptureSessionController(
            checkPermissions = {
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    throw SecurityException("Camera permission is not granted")
                }
            },
            checkDiskSpace = {
                if (datasetRoot().usableSpace <= 0L) error("No disk space available")
            },
            createSessionDirectory = {
                sessionDirectory = SessionDirectoryManager(datasetRoot()).createSessionDirectory(
                    deviceModel = android.os.Build.MODEL,
                    index = nextSessionIndex(),
                )
                storageManager = StorageManager(requireNotNull(sessionDirectory))
                writeQueue = AsyncWriteQueue()
            },
            writeInitialMetadata = {
                val storage = requireNotNull(storageManager)
                storage.writeSessionConfig(profile)
                storage.writeDeviceInfo(deviceInfoCollector.collectDeviceInfo())
                storage.writeSensorInfo(deviceInfoCollector.collectSensorInfo(sensorManager))
                val selectedCameraId = CameraDeviceSelector(cameraManager).selectBackMainCamera()
                storage.writeCameraInfo(
                    CameraCapabilityReader(cameraManager)
                        .read(selectedCameraId, CameraConfig().imageFormat)
                        .toJson(),
                )
                buildControllers(sensorManager, cameraManager, selectedCameraId)
            },
            startImu = { imuController?.startImu() },
            startCamera = { cameraController?.startCamera() },
            stopCamera = { cameraController?.stopCamera() },
            stopImu = { imuController?.stopImu() },
            flushStorage = {
                writeQueue?.flushAndStop()
                storageManager?.close()
            },
            writeFinalMetadata = { writeFinalMetadata() },
            validateSession = { sessionDirectory?.let { SessionValidator().validate(it) } },
            onStateChanged = { state -> runOnUiThread { updateState(state) } },
        )

        try {
            sessionController?.startSession()
            recording = true
            actionButton.isEnabled = true
            actionButton.text = "Stop Recording"
            uiHandler.removeCallbacks(statsRefresh)
            uiHandler.post(statsRefresh)
        } catch (error: Throwable) {
            showError(error.message ?: error.javaClass.simpleName)
            abortStartFailure()
        }
    }

    private fun stopRecording() {
        actionButton.isEnabled = false
        uiHandler.removeCallbacks(statsRefresh)
        sessionController?.stopSession()
        lastFinishedStats = sessionRecorder?.stats(
            nowMs = SystemClock.elapsedRealtime(),
            diskFreeBytes = sessionDirectory?.freeSpace ?: datasetRoot().freeSpace,
            currentWriteQueueSize = writeQueue?.size() ?: 0,
        )
        clearSessionObjects()
        recording = false
        actionButton.text = "Start Recording"
        actionButton.isEnabled = true
        updateStats()
    }

    private fun abortStartFailure() {
        uiHandler.removeCallbacks(statsRefresh)
        cameraController?.stopCamera()
        imuController?.stopImu()
        writeQueue?.flushAndStop()
        storageManager?.close()
        clearSessionObjects()
        recording = false
        actionButton.text = "Start Recording"
        actionButton.isEnabled = true
    }

    private fun clearSessionObjects() {
        cameraController = null
        imuController = null
        writeQueue = null
        storageManager = null
        sessionController = null
        sessionRecorder = null
        sessionDirectory = null
    }

    private fun updateStats() {
        val queueSize = writeQueue?.size() ?: 0
        val stats = sessionRecorder?.stats(
            nowMs = SystemClock.elapsedRealtime(),
            diskFreeBytes = sessionDirectory?.freeSpace ?: datasetRoot().freeSpace,
            currentWriteQueueSize = queueSize,
        ) ?: lastFinishedStats
        if (stats == null) {
            queueSizeText.text = "Write Queue Size: 0"
            diskFreeText.text = "Disk Free: ${datasetRoot().freeSpace}"
            return
        }
        cameraFpsText.text = "Camera FPS: %.1f".format(stats.cameraFps)
        gyroHzText.text = "Gyro Hz: %.1f".format(stats.gyroHz)
        accelHzText.text = "Accel Hz: %.1f".format(stats.accelHz)
        droppedFramesText.text = "Dropped Frames: ${stats.droppedFrames}"
        queueSizeText.text = "Write Queue Size: ${stats.writeQueueSize}"
        diskFreeText.text = "Disk Free: ${stats.diskFreeBytes}"
        durationText.text = "Session Duration: %.1f s".format(stats.sessionDurationSec)
    }

    private fun buildControllers(
        sensorManager: SensorManager,
        cameraManager: CameraManager,
        selectedCameraId: String,
    ) {
        val storage = requireNotNull(storageManager)
        val queue = requireNotNull(writeQueue)
        val recorder = requireNotNull(sessionRecorder)
        val profile = DefaultProfiles.default640x48030Fps
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
                        recorder.recordGyro(sample.timestampNs, queue.size())
                    }
                    SensorType.ACCELEROMETER -> {
                        storage.accelWriter.write(sample)
                        recorder.recordAccel(sample.timestampNs, queue.size())
                    }
                }
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
                    recorder.recordImage(frame.timestampNs, queue.size())
                }
            },
            onError = { error ->
                recorder.recordError()
                runOnUiThread { showError(error.message ?: error.javaClass.simpleName) }
            },
        )
    }

    private fun writeFinalMetadata() {
        val directory = requireNotNull(sessionDirectory)
        val recorder = requireNotNull(sessionRecorder)
        val stats = recorder.stats(
            nowMs = SystemClock.elapsedRealtime(),
            diskFreeBytes = directory.freeSpace,
            currentWriteQueueSize = writeQueue?.size() ?: 0,
        )
        val summary = SessionSummaryGenerator().generate(
            sessionId = directory.name,
            durationSec = stats.sessionDurationSec,
            numImages = recorder.numImages,
            numGyro = recorder.numGyro,
            numAccel = recorder.numAccel,
            stats = stats,
            config = sessionConfig,
        )
        val metadataWriter = MetadataWriter()
        metadataWriter.writeJson(File(directory, "session_summary.json"), summary)
        metadataWriter.writeJson(File(directory, "diagnostics.json"), recorder.diagnostics(stats))
    }

    private fun updateState(state: SessionState) {
        stateText.text = "State: $state"
    }

    private fun datasetRoot(): File =
        File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: filesDir, "VICollector")

    private fun nextSessionIndex(): Int = datasetRoot().listFiles()?.size?.plus(1) ?: 1

    private fun showError(message: String) {
        stateText.text = "State: ERROR"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
