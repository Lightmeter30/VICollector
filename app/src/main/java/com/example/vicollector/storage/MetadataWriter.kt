package com.example.vicollector.storage

import org.json.JSONObject
import java.io.File

class MetadataWriter {
    fun writeJson(file: File, json: JSONObject) {
        file.parentFile?.mkdirs()
        file.writeText(json.toString(2))
    }
}
