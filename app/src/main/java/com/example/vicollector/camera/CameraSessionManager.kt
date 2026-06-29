package com.example.vicollector.camera

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.os.Handler
import android.view.Surface

class CameraSessionManager {
    fun createSession(
        cameraDevice: CameraDevice,
        surfaces: List<Surface>,
        handler: Handler,
        onConfigured: (CameraCaptureSession) -> Unit,
        onConfigureFailed: (CameraCaptureSession) -> Unit,
    ) {
        cameraDevice.createCaptureSession(
            surfaces,
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    onConfigured(session)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    onConfigureFailed(session)
                }
            },
            handler,
        )
    }

    fun startRepeating(session: CameraCaptureSession, request: CaptureRequest, handler: Handler) {
        session.setRepeatingRequest(request, null, handler)
    }
}
