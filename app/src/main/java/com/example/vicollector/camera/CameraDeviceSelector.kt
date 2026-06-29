package com.example.vicollector.camera

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

class CameraDeviceSelector(private val cameraManager: CameraManager) {
    fun selectBackMainCamera(preferredId: String? = null): String {
        preferredId?.let { return it }
        return cameraManager.cameraIdList.first { id ->
            val chars = cameraManager.getCameraCharacteristics(id)
            chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
    }
}
