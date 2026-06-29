# VICollector 阶段 1：工程基线计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` 或 `superpowers:executing-plans` 逐任务执行。所有步骤使用 checkbox 跟踪。

**目标：** 建立可构建、可安装、权限完整的 VICollector Android 工程基线。

**架构：** 本阶段只完成 Gradle 工程、Manifest、基础 UI、核心模型、默认配置和公共工具，不接入真实 Camera2 或 SensorManager。后续阶段必须复用本阶段定义的 `core/model`、`config` 和 `utils` 契约。

**技术栈：** Kotlin、Gradle Kotlin DSL、Android SDK 26+、AndroidX Activity、JUnit。

---

## 交付物

- `VICollector/` Android 工程骨架。
- `app/src/main/AndroidManifest.xml` 包含 Camera、Wake Lock、Foreground Service 权限。
- 主界面显示 `State`、`Camera FPS`、`Gyro Hz`、`Accel Hz`、`Disk Free` 和采集按钮。
- 核心模型包含 `CameraFrame`、`ImuSample`、`CaptureStats`、`DeviceInfo`、`SessionInfo`。
- 默认采集配置为 640x480、JPEG、30 FPS、gyro/accel enabled、timestamp diagnostics enabled。

## 文件清单

- Create: `VICollector/settings.gradle.kts`
- Create: `VICollector/build.gradle.kts`
- Create: `VICollector/gradle.properties`
- Create: `VICollector/app/build.gradle.kts`
- Create: `VICollector/app/src/main/AndroidManifest.xml`
- Create: `VICollector/app/src/main/java/com/example/vicollector/VICollectorApp.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/MainActivity.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/core/model/CameraFrame.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/core/model/ImuSample.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/core/model/CaptureStats.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/core/model/DeviceInfo.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/core/model/SessionInfo.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/core/constant/CaptureConstants.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/core/constant/FileConstants.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/core/error/CaptureException.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/core/error/CameraException.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/core/error/ImuException.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/core/error/StorageException.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/config/CaptureProfile.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/config/DefaultProfiles.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/utils/PermissionUtils.kt`
- Create: `VICollector/app/src/test/java/com/example/vicollector/core/model/CoreModelTest.kt`

## Task 1：初始化 Gradle Android 工程

- [x] 创建 `VICollector/`、`VICollector/app/`、`VICollector/app/src/main/` 和 `VICollector/app/src/test/` 目录。

- [x] 写入 `VICollector/settings.gradle.kts`：

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "VICollector"
include(":app")
```

- [x] 写入根 `VICollector/build.gradle.kts`：

```kotlin
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
}
```

- [x] 写入 `VICollector/gradle.properties`：

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
```

- [x] 写入 `VICollector/app/build.gradle.kts`：

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.vicollector"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.vicollector"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    testImplementation("junit:junit:4.13.2")
}
```

- [x] 验证：

```powershell
cd VICollector
.\gradlew.bat tasks
```

期望：输出包含 `assembleDebug`。

## Task 2：配置 Manifest 和入口 Activity

- [x] 写入 `AndroidManifest.xml`，必须包含：

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-feature android:name="android.hardware.camera" android:required="true" />
<uses-feature android:name="android.hardware.sensor.gyroscope" android:required="true" />
<uses-feature android:name="android.hardware.sensor.accelerometer" android:required="true" />
```

- [x] 写入 `VICollectorApp.kt`：

```kotlin
package com.example.vicollector

import android.app.Application

class VICollectorApp : Application()
```

- [x] 写入 `MainActivity.kt` 的最小 UI：

```kotlin
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
            "Disk Free: unknown"
        ).forEach { layout.addView(TextView(this).apply { text = it }) }
        layout.addView(Button(this).apply { text = "Start Recording" })
        setContentView(layout)
    }
}
```

- [x] 验证：

```powershell
cd VICollector
.\gradlew.bat :app:processDebugMainManifest
```

期望：`BUILD SUCCESSFUL`。

## Task 3：定义核心模型

- [x] 写入 `CameraFrame.kt`：

```kotlin
package com.example.vicollector.core.model

data class CameraFrame(
    val frameIndex: Long,
    val timestampNs: Long,
    val width: Int,
    val height: Int,
    val format: Int,
    val exposureTimeNs: Long?,
    val sensitivityIso: Int?,
    val fileName: String
)
```

- [x] 写入 `ImuSample.kt`：

```kotlin
package com.example.vicollector.core.model

data class ImuSample(
    val timestampNs: Long,
    val sensorType: SensorType,
    val x: Float,
    val y: Float,
    val z: Float,
    val accuracy: Int
)

enum class SensorType {
    GYROSCOPE,
    ACCELEROMETER
}
```

- [x] 写入 `CaptureStats.kt`：

```kotlin
package com.example.vicollector.core.model

data class CaptureStats(
    val cameraFps: Double,
    val gyroHz: Double,
    val accelHz: Double,
    val droppedFrames: Long,
    val writeQueueSize: Int,
    val sessionDurationSec: Double,
    val diskFreeBytes: Long,
    val errorCount: Int
)
```

- [x] 写入单元测试 `CoreModelTest.kt`：

```kotlin
package com.example.vicollector.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CoreModelTest {
    @Test
    fun imuSampleKeepsRawTimestamp() {
        val sample = ImuSample(123L, SensorType.GYROSCOPE, 0.1f, 0.2f, 0.3f, 3)
        assertEquals(123L, sample.timestampNs)
        assertEquals(SensorType.GYROSCOPE, sample.sensorType)
    }
}
```

- [x] 验证：

```powershell
cd VICollector
.\gradlew.bat testDebugUnitTest
```

期望：`BUILD SUCCESSFUL`。

## Task 4：定义默认配置

- [x] 写入 `CaptureProfile.kt`：

```kotlin
package com.example.vicollector.config

data class CaptureProfile(
    val imageWidth: Int,
    val imageHeight: Int,
    val imageFormat: String,
    val targetFps: Int,
    val enableGyro: Boolean,
    val enableAccel: Boolean,
    val sensorDelay: String,
    val imageStorage: String,
    val imuStorage: String,
    val writeMetadata: Boolean,
    val enableTimestampDiagnostics: Boolean
)
```

- [x] 写入 `DefaultProfiles.kt`：

```kotlin
package com.example.vicollector.config

object DefaultProfiles {
    val default640x48030Fps = CaptureProfile(
        imageWidth = 640,
        imageHeight = 480,
        imageFormat = "JPEG",
        targetFps = 30,
        enableGyro = true,
        enableAccel = true,
        sensorDelay = "FASTEST",
        imageStorage = "JPEG",
        imuStorage = "CSV",
        writeMetadata = true,
        enableTimestampDiagnostics = true
    )
}
```

## 阶段验收

- [x] `cd VICollector; .\gradlew.bat clean assembleDebug testDebugUnitTest` 成功。
- [x] Manifest 未使用 `WRITE_EXTERNAL_STORAGE`。
- [x] 基础 UI 可启动，显示采集状态与关键指标。
- [x] 模型字段命名使用 `timestampNs`，后续 CSV 字段统一写为 `timestamp_ns`。
