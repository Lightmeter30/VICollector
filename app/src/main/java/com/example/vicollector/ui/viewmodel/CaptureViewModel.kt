package com.example.vicollector.ui.viewmodel

import com.example.vicollector.core.model.CaptureStats
import com.example.vicollector.session.SessionState

data class CaptureViewModel(
    val state: SessionState,
    val stats: CaptureStats,
)
