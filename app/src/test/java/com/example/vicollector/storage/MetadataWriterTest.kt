package com.example.vicollector.storage

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class MetadataWriterTest {
    @Test
    fun writesJsonObject() {
        val file = File.createTempFile("device_info", ".json")
        MetadataWriter().writeJson(file, JSONObject().put("model", "Pixel8"))

        assertEquals("Pixel8", JSONObject(file.readText()).getString("model"))
    }
}
