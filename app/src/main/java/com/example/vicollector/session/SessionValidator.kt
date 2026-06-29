package com.example.vicollector.session

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class SessionValidationResult(
    val isValid: Boolean,
    val missingItems: List<String>,
    val emptyItems: List<String>,
)

class SessionValidator {
    fun validate(sessionDirectory: File): SessionValidationResult {
        val missing = mutableListOf<String>()
        val empty = mutableListOf<String>()

        requireDirectory(sessionDirectory, "images", missing)
        requireFile(sessionDirectory, "image_timestamps.csv", missing)
        requireNonEmptyFile(sessionDirectory, "gyro.csv", missing, empty)
        requireNonEmptyFile(sessionDirectory, "accel.csv", missing, empty)
        requireFile(sessionDirectory, "device_info.json", missing)
        requireFile(sessionDirectory, "sensor_info.json", missing)
        requireFile(sessionDirectory, "session_config.json", missing)
        requireFile(sessionDirectory, "session_summary.json", missing)
        requireFile(sessionDirectory, "diagnostics.json", missing)

        val result = SessionValidationResult(
            isValid = missing.isEmpty() && empty.isEmpty(),
            missingItems = missing,
            emptyItems = empty,
        )
        writeDiagnostics(sessionDirectory, result)
        return result
    }

    private fun requireDirectory(root: File, name: String, missing: MutableList<String>) {
        if (!File(root, name).isDirectory) missing += name
    }

    private fun requireFile(root: File, name: String, missing: MutableList<String>) {
        if (!File(root, name).isFile) missing += name
    }

    private fun requireNonEmptyFile(
        root: File,
        name: String,
        missing: MutableList<String>,
        empty: MutableList<String>,
    ) {
        val file = File(root, name)
        when {
            !file.isFile -> missing += name
            file.length() == 0L -> empty += name
        }
    }

    private fun writeDiagnostics(sessionDirectory: File, result: SessionValidationResult) {
        val diagnosticsFile = File(sessionDirectory, "diagnostics.json")
        val diagnostics = if (diagnosticsFile.isFile) {
            JSONObject(diagnosticsFile.readText())
        } else {
            JSONObject()
        }
        diagnostics
            .put("is_valid", result.isValid)
            .put("missing_items", JSONArray(result.missingItems))
            .put("empty_items", JSONArray(result.emptyItems))
        diagnosticsFile.writeText(diagnostics.toString(2))
    }
}
