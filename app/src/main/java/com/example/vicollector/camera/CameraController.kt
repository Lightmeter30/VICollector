package com.example.vicollector.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import androidx.core.content.ContextCompat
import com.example.vicollector.core.model.CameraFrame

class CameraController(
    private val context: Context,
    private val cameraManager: CameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager,
    private val config: CameraConfig = CameraConfig(),
    private val onImage: (frame: CameraFrame, jpegBytes: ByteArray) -> Unit,
    private val onError: (Throwable) -> Unit = {},
) {
    private val selector = CameraDeviceSelector(cameraManager)
    private val requestFactory = CaptureRequestFactory()
    private val sessionManager = CameraSessionManager()
    private val imageReaderManager = ImageReaderManager(config)
    private val metadataLogger = CameraMetadataLogger(config) { index -> "%06d.jpg".format(index) }

    private var cameraThread: HandlerThread? = null
    private var cameraHandler: Handler? = null
    private var imageReader: ImageReader? = null
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null

    fun startCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            onError(SecurityException("Camera permission is not granted"))
            return
        }

        val thread = HandlerThread("VICollector-Camera").also { it.start() }
        val handler = Handler(thread.looper)
        cameraThread = thread
        cameraHandler = handler

        try {
            val cameraId = selector.selectBackMainCamera(config.cameraId)
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val reader = imageReaderManager.createReader(handler) { timestampNs, bytes ->
                onImage(metadataLogger.nextFrame(timestampNs), bytes)
            }
            imageReader = reader
            cameraManager.openCamera(
                cameraId,
                object : CameraDevice.StateCallback() {
                    override fun onOpened(device: CameraDevice) {
                        cameraDevice = device
                        createCaptureSession(device, characteristics, reader, handler)
                    }

                    override fun onDisconnected(device: CameraDevice) {
                        device.close()
                        if (cameraDevice == device) cameraDevice = null
                    }

                    override fun onError(device: CameraDevice, error: Int) {
                        device.close()
                        if (cameraDevice == device) cameraDevice = null
                        onError(CameraAccessException(error, "CameraDevice error: $error"))
                    }
                },
                handler,
            )
        } catch (error: Throwable) {
            onError(error)
            stopCamera()
        }
    }

    fun stopCamera() {
        try {
            captureSession?.stopRepeating()
        } catch (_: Throwable) {
        }
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
        imageReader?.close()
        imageReader = null
        cameraThread?.quitSafely()
        try {
            cameraThread?.join(1_000)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        cameraThread = null
        cameraHandler = null
    }

    private fun createCaptureSession(
        device: CameraDevice,
        characteristics: CameraCharacteristics,
        reader: ImageReader,
        handler: Handler,
    ) {
        val surface = reader.surface
        sessionManager.createSession(
            cameraDevice = device,
            surfaces = listOf(surface),
            handler = handler,
            onConfigured = { session ->
                captureSession = session
                val request = requestFactory.createRepeatingRequest(
                    cameraDevice = device,
                    characteristics = characteristics,
                    imageSurface = surface,
                    targetFps = config.targetFps,
                )
                sessionManager.startRepeating(session, request, handler)
            },
            onConfigureFailed = { session ->
                onError(CameraAccessException(CameraAccessException.CAMERA_ERROR, "Camera session configure failed"))
                session.close()
            },
        )
    }
}
