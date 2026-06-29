package com.example.vicollector.session

import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class SessionValidatorTest {
    @Test
    fun writesDiagnosticsWhenRequiredFilesAreMissing() {
        val sessionDir = createTempDirectory("vicollector-session").toFile()

        val result = SessionValidator().validate(sessionDir)

        assertFalse(result.isValid)
        assertTrue(File(sessionDir, "diagnostics.json").exists())
        val diagnostics = JSONObject(File(sessionDir, "diagnostics.json").readText())
        assertFalse(diagnostics.getBoolean("is_valid"))
        assertTrue(diagnostics.getJSONArray("missing_items").length() > 0)
    }
}
