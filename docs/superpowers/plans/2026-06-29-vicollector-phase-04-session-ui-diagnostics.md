# VICollector 阶段 4：会话集成、UI 与诊断计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` 或 `superpowers:executing-plans` 逐任务执行。所有步骤使用 checkbox 跟踪。

**目标：** 将 Camera、IMU、Storage、Device 和 Config 模块整合为完整采集 session，并输出实时状态、`session_summary.json` 和 `diagnostics.json`。

**架构：** `CaptureSessionController` 是唯一采集编排入口，按 `IDLE -> PREPARING -> RECORDING -> STOPPING -> FINISHED` 推进。`sync` 模块只做频率、间隔、丢帧、重复 timestamp 和邻近 IMU 数量诊断，不修改原始 timestamp，也不做强制 camera-IMU 对齐。

**技术栈：** Kotlin、Handler/StateFlow 二选一、Camera2、SensorManager、org.json、JUnit、真机采集验证。

---

## 交付物

- `startSession()` 按权限检查、磁盘检查、创建目录、写配置、启动 IMU、启动 Camera、更新 UI 的顺序执行。
- `stopSession()` 按停止 Camera、停止 IMU、flush 队列、关闭 writer、生成 summary、生成 diagnostics、校验完整性的顺序执行。
- UI 显示 Camera FPS、Gyro Hz、Accel Hz、Dropped Frames、Write Queue Size、Disk Free、Session Duration、Current State。
- `session_summary.json` 与 `diagnostics.json` 可被主机脚本解析。

## 文件清单

- Create: `VICollector/app/src/main/java/com/example/vicollector/session/SessionState.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/session/SessionConfig.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/session/CaptureSessionController.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/session/SessionRecorder.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/session/SessionSummaryGenerator.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/session/SessionValidator.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/sync/Timebase.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/sync/TimestampDiagnostics.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/sync/FrameImuAssociator.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/sync/DriftMonitor.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/ui/viewmodel/CaptureViewModel.kt`
- Modify: `VICollector/app/src/main/java/com/example/vicollector/MainActivity.kt`
- Test: `VICollector/app/src/test/java/com/example/vicollector/sync/TimestampDiagnosticsTest.kt`
- Test: `VICollector/app/src/test/java/com/example/vicollector/session/SessionSummaryGeneratorTest.kt`

## Task 1：定义 session 状态机

- [ ] 写入 `SessionState.kt`：

```kotlin
package com.example.vicollector.session

enum class SessionState {
    IDLE,
    PREPARING,
    RECORDING,
    STOPPING,
    FINISHED
}
```

- [ ] 写入 `SessionConfig.kt`：

```kotlin
package com.example.vicollector.session

data class SessionConfig(
    val imageWidth: Int = 640,
    val imageHeight: Int = 480,
    val imageFormat: String = "JPEG",
    val targetFps: Int = 30,
    val enableGyro: Boolean = true,
    val enableAccel: Boolean = true,
    val sensorDelay: String = "FASTEST",
    val imageStorage: String = "JPEG",
    val imuStorage: String = "CSV",
    val enableTimestampDiagnostics: Boolean = true
)
```

- [ ] 验证：

```powershell
cd VICollector
.\gradlew.bat compileDebugKotlin
```

期望：`BUILD SUCCESSFUL`。

## Task 2：实现 timestamp 诊断

- [ ] 写入 `TimestampDiagnosticsTest.kt`：

```kotlin
package com.example.vicollector.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class TimestampDiagnosticsTest {
    @Test
    fun detectsNonMonotonicTimestamps() {
        val result = TimestampDiagnostics().analyze(listOf(10L, 20L, 15L, 30L))
        assertEquals(1, result.nonMonotonicCount)
    }
}
```

- [ ] 写入 `TimestampDiagnostics.kt`：

```kotlin
package com.example.vicollector.sync

data class TimestampDiagnosticsResult(
    val count: Int,
    val duplicateCount: Int,
    val nonMonotonicCount: Int,
    val minIntervalNs: Long,
    val maxIntervalNs: Long
)

class TimestampDiagnostics {
    fun analyze(timestampsNs: List<Long>): TimestampDiagnosticsResult {
        if (timestampsNs.size < 2) {
            return TimestampDiagnosticsResult(timestampsNs.size, 0, 0, 0L, 0L)
        }
        val intervals = timestampsNs.zipWithNext { a, b -> b - a }
        return TimestampDiagnosticsResult(
            count = timestampsNs.size,
            duplicateCount = intervals.count { it == 0L },
            nonMonotonicCount = intervals.count { it < 0L },
            minIntervalNs = intervals.min(),
            maxIntervalNs = intervals.max()
        )
    }
}
```

- [ ] 验证：

```powershell
cd VICollector
.\gradlew.bat testDebugUnitTest --tests "*TimestampDiagnosticsTest"
```

期望：`BUILD SUCCESSFUL`。

## Task 3：生成 session summary

- [ ] 写入 `SessionSummaryGeneratorTest.kt`：

```kotlin
package com.example.vicollector.session

import com.example.vicollector.core.model.CaptureStats
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionSummaryGeneratorTest {
    @Test
    fun writesRatesAndStorageFormat() {
        val json = SessionSummaryGenerator().generate(
            sessionId = "session001",
            durationSec = 30.0,
            numImages = 900,
            numGyro = 12000,
            numAccel = 12000,
            stats = CaptureStats(30.0, 400.0, 400.0, 0, 0, 30.0, 1_000_000L, 0)
        )
        assertEquals("JPEG+CSV", json.getString("storage_format"))
        assertEquals(30.0, json.getDouble("estimated_camera_fps"), 0.01)
    }
}
```

- [ ] 写入 `SessionSummaryGenerator.kt`：

```kotlin
package com.example.vicollector.session

import com.example.vicollector.core.model.CaptureStats
import org.json.JSONObject

class SessionSummaryGenerator {
    fun generate(
        sessionId: String,
        durationSec: Double,
        numImages: Int,
        numGyro: Int,
        numAccel: Int,
        stats: CaptureStats
    ): JSONObject = JSONObject()
        .put("session_id", sessionId)
        .put("duration_sec", durationSec)
        .put("num_images", numImages)
        .put("num_gyro", numGyro)
        .put("num_accel", numAccel)
        .put("estimated_camera_fps", stats.cameraFps)
        .put("estimated_gyro_hz", stats.gyroHz)
        .put("estimated_accel_hz", stats.accelHz)
        .put("dropped_frames", stats.droppedFrames)
        .put("storage_format", "JPEG+CSV")
}
```

- [ ] 验证测试成功。

## Task 4：实现采集编排控制器

- [ ] 写入 `CaptureSessionController.kt`：

```kotlin
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
    private val onStateChanged: (SessionState) -> Unit
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
```

- [ ] 约束检查：`startSession()` 必须先启动 IMU，再启动 Camera。

## Task 5：实现 SessionValidator

- [ ] `SessionValidator` 必须检查：
  - `images/` 存在。
  - `image_timestamps.csv` 存在。
  - `gyro.csv` 存在且非空。
  - `accel.csv` 存在且非空。
  - `device_info.json` 存在。
  - `sensor_info.json` 存在。
  - `session_config.json` 存在。
  - `session_summary.json` 存在。
  - `diagnostics.json` 存在。

- [ ] 校验失败时写入 diagnostics，不删除 session。

## Task 6：UI 绑定实时状态

- [ ] 在 `MainActivity` 中维护可更新的 TextView：
  - `stateText`
  - `cameraFpsText`
  - `gyroHzText`
  - `accelHzText`
  - `droppedFramesText`
  - `queueSizeText`
  - `diskFreeText`
  - `durationText`

- [ ] 每秒刷新一次 `CaptureStats`。

- [ ] Stop 后显示 `FINISHED`，并禁用 Stop 逻辑直到下一次采集准备完成。

## 阶段验收

- [ ] `cd VICollector; .\gradlew.bat testDebugUnitTest assembleDebug` 成功。
- [ ] 真机 30 秒采集后 session 目录包含 `session_summary.json` 和 `diagnostics.json`。
- [ ] `diagnostics.json` 报告重复 timestamp、非单调 timestamp、异常间隔、dropped frames、write queue max size。
- [ ] App 不在内部做 camera-IMU 插值同步，不修改原始 timestamp。
