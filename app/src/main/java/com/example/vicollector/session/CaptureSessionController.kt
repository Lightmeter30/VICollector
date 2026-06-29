package com.example.vicollector.session

class CaptureSessionController(
    private val checkPermissions: () -> Unit,
    private val checkDiskSpace: () -> Unit,
    private val createSessionDirectory: () -> Unit,
    private val writeInitialMetadata: () -> Unit,
    private val startImu: () -> Unit,
    private val startCamera: () -> Unit,
    private val stopCamera: () -> Unit,
    private val stopImu: () -> Unit,
    private val flushStorage: () -> Unit,
    private val writeFinalMetadata: () -> Unit,
    private val validateSession: () -> Unit,
    private val onStateChanged: (SessionState) -> Unit,
) {
    var state: SessionState = SessionState.IDLE
        private set

    fun startSession() {
        transition(SessionState.PREPARING)
        checkPermissions()
        checkDiskSpace()
        createSessionDirectory()
        writeInitialMetadata()
        startImu()
        startCamera()
        transition(SessionState.RECORDING)
    }

    fun stopSession() {
        transition(SessionState.STOPPING)
        stopCamera()
        stopImu()
        flushStorage()
        writeFinalMetadata()
        validateSession()
        transition(SessionState.FINISHED)
    }

    private fun transition(next: SessionState) {
        state = next
        onStateChanged(next)
    }
}
