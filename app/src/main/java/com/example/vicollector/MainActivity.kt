package com.example.vicollector

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        listOf(
            "State: IDLE",
            "Camera FPS: 0.0",
            "Gyro Hz: 0.0",
            "Accel Hz: 0.0",
            "Disk Free: unknown",
        ).forEach { layout.addView(TextView(this).apply { text = it }) }
        layout.addView(Button(this).apply { text = "Start Recording" })
        setContentView(layout)
    }
}
