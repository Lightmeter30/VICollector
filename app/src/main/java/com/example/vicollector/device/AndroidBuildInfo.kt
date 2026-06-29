package com.example.vicollector.device

import android.os.Build
import org.json.JSONObject

data class AndroidBuildInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdkInt: Int,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("manufacturer", manufacturer)
        .put("model", model)
        .put("android_version", androidVersion)
        .put("sdk_int", sdkInt)

    companion object {
        fun current(): AndroidBuildInfo = AndroidBuildInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
        )
    }
}
