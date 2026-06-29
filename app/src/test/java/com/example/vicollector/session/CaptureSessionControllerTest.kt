package com.example.vicollector.session

import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureSessionControllerTest {
    @Test
    fun startsImuBeforeCameraAndReportsRecording() {
        val events = mutableListOf<String>()
        val controller = controller(events)

        controller.startSession()

        assertEquals(
            listOf(
                "state:PREPARING",
                "checkPermissions",
                "checkDiskSpace",
                "createSessionDirectory",
                "writeInitialMetadata",
                "startImu",
                "startCamera",
                "state:RECORDING",
            ),
            events,
        )
        assertEquals(SessionState.RECORDING, controller.state)
    }

    @Test
    fun stopsCameraBeforeImuAndReportsFinished() {
        val events = mutableListOf<String>()
        val controller = controller(events)

        controller.stopSession()

        assertEquals(
            listOf(
                "state:STOPPING",
                "stopCamera",
                "stopImu",
                "flushStorage",
                "writeFinalMetadata",
                "validateSession",
                "state:FINISHED",
            ),
            events,
        )
        assertEquals(SessionState.FINISHED, controller.state)
    }

    private fun controller(events: MutableList<String>) = CaptureSessionController(
        checkPermissions = { events += "checkPermissions" },
        checkDiskSpace = { events += "checkDiskSpace" },
        createSessionDirectory = { events += "createSessionDirectory" },
        writeInitialMetadata = { events += "writeInitialMetadata" },
        startImu = { events += "startImu" },
        startCamera = { events += "startCamera" },
        stopCamera = { events += "stopCamera" },
        stopImu = { events += "stopImu" },
        flushStorage = { events += "flushStorage" },
        writeFinalMetadata = { events += "writeFinalMetadata" },
        validateSession = { events += "validateSession" },
        onStateChanged = { events += "state:$it" },
    )
}
