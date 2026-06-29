# VICollector 阶段 3：IMU 与 Camera2 采集计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` 或 `superpowers:executing-plans` 逐任务执行。所有步骤使用 checkbox 跟踪。

**目标：** 接入真实 SensorManager 和 Camera2，完成 v0.1 的 Gyroscope、Accelerometer 和 640x480 JPEG 图像采集链路。

**架构：** `imu` 模块将 `SensorEvent.timestamp` 原样转为 `ImuSample`；`camera` 模块用 Camera2、ImageReader 和 CaptureResult 生成 `CameraFrame` 与 JPEG 数据。采集 callback 只做轻量复制、元数据提取和入队，所有文件 I/O 进入 `AsyncWriteQueue`。

**技术栈：** Kotlin、Camera2 API、SensorManager、SensorEventListener、ImageReader、HandlerThread、JUnit、Android 真机验证。

---

## 交付物

- 真机可采集 `gyro.csv` 和 `accel.csv`，timestamp 来自 `SensorEvent.timestamp`。
- 真机可采集 `images/*.jpg` 和 `image_timestamps.csv`。
- 默认 Camera 配置为后置主摄、640x480、JPEG、目标 30 FPS。
- Camera callback 和 Sensor callback 中不直接执行大文件 I/O。
- 如果无法稳定 30 FPS，记录实际 FPS。

## 文件清单

- Create: `VICollector/app/src/main/java/com/example/vicollector/imu/ImuConfig.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/imu/SensorSelector.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/imu/SensorEventDispatcher.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/imu/ImuRateEstimator.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/imu/ImuBuffer.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/imu/ImuController.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/camera/CameraConfig.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/camera/CameraDeviceSelector.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/camera/CameraCapabilityReader.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/camera/ImageReaderManager.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/camera/CaptureRequestFactory.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/camera/CameraSessionManager.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/camera/CameraController.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/camera/CameraMetadataLogger.kt`
- Test: `VICollector/app/src/test/java/com/example/vicollector/imu/ImuRateEstimatorTest.kt`

## Task 1：实现 IMU 频率估计

- [ ] 写入 `ImuRateEstimatorTest.kt`：

```kotlin
package com.example.vicollector.imu

import org.junit.Assert.assertEquals
import org.junit.Test

class ImuRateEstimatorTest {
    @Test
    fun estimatesHzFromNanosecondIntervals() {
        val estimator = ImuRateEstimator()
        listOf(0L, 10_000_000L, 20_000_000L, 30_000_000L).forEach(estimator::addTimestampNs)
        assertEquals(100.0, estimator.estimatedHz(), 0.01)
    }
}
```

- [ ] 写入 `ImuRateEstimator.kt`：

```kotlin
package com.example.vicollector.imu

class ImuRateEstimator(private val maxSamples: Int = 256) {
    private val timestamps = ArrayDeque<Long>()

    fun addTimestampNs(timestampNs: Long) {
        timestamps.addLast(timestampNs)
        while (timestamps.size > maxSamples) timestamps.removeFirst()
    }

    fun estimatedHz(): Double {
        if (timestamps.size < 2) return 0.0
        val durationNs = timestamps.last() - timestamps.first()
        if (durationNs <= 0L) return 0.0
        return (timestamps.size - 1) * 1_000_000_000.0 / durationNs
    }
}
```

- [ ] 验证：

```powershell
cd VICollector
.\gradlew.bat testDebugUnitTest --tests "*ImuRateEstimatorTest"
```

期望：`BUILD SUCCESSFUL`。

## Task 2：实现 IMU Controller

- [ ] 写入 `ImuConfig.kt`：

```kotlin
package com.example.vicollector.imu

data class ImuConfig(
    val enableGyro: Boolean = true,
    val enableAccel: Boolean = true,
    val samplingPeriodUs: Int = 2_500
)
```

- [ ] 写入 `SensorSelector.kt`：

```kotlin
package com.example.vicollector.imu

import android.hardware.Sensor
import android.hardware.SensorManager

class SensorSelector(private val sensorManager: SensorManager) {
    fun gyroscope(): Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    fun accelerometer(): Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
}
```

- [ ] 写入 `ImuController.kt`：

```kotlin
package com.example.vicollector.imu

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.vicollector.core.model.ImuSample
import com.example.vicollector.core.model.SensorType

class ImuController(
    private val sensorManager: SensorManager,
    private val config: ImuConfig,
    private val onSample: (ImuSample) -> Unit
) : SensorEventListener {
    private val selector = SensorSelector(sensorManager)

    fun startImu() {
        if (config.enableGyro) selector.gyroscope()?.let {
            sensorManager.registerListener(this, it, config.samplingPeriodUs)
        }
        if (config.enableAccel) selector.accelerometer()?.let {
            sensorManager.registerListener(this, it, config.samplingPeriodUs)
        }
    }

    fun stopImu() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val type = when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> SensorType.GYROSCOPE
            Sensor.TYPE_ACCELEROMETER -> SensorType.ACCELEROMETER
            else -> return
        }
        onSample(ImuSample(event.timestamp, type, event.values[0], event.values[1], event.values[2], event.accuracy))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
```

- [ ] 验证：

```powershell
cd VICollector
.\gradlew.bat compileDebugKotlin
```

期望：`BUILD SUCCESSFUL`。

## Task 3：实现 Camera2 基础配置与后置主摄选择

- [ ] 写入 `CameraConfig.kt`：

```kotlin
package com.example.vicollector.camera

import android.graphics.ImageFormat

data class CameraConfig(
    val width: Int = 640,
    val height: Int = 480,
    val imageFormat: Int = ImageFormat.JPEG,
    val targetFps: Int = 30,
    val cameraId: String? = null
)
```

- [ ] 写入 `CameraDeviceSelector.kt`：

```kotlin
package com.example.vicollector.camera

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

class CameraDeviceSelector(private val cameraManager: CameraManager) {
    fun selectBackMainCamera(preferredId: String? = null): String {
        preferredId?.let { return it }
        return cameraManager.cameraIdList.first { id ->
            val chars = cameraManager.getCameraCharacteristics(id)
            chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
    }
}
```

- [ ] 写入 `CameraCapabilityReader.kt`，至少读取支持分辨率、FPS range、sensor size、focal length、exposure range。

- [ ] 验证编译成功。

## Task 4：实现 ImageReader 和 JPEG 入队

- [ ] 写入 `ImageWriter.kt`：

```kotlin
package com.example.vicollector.storage

import java.io.File

class ImageWriter {
    fun writeJpeg(file: File, jpegBytes: ByteArray) {
        file.parentFile?.mkdirs()
        file.writeBytes(jpegBytes)
    }
}
```

- [ ] 写入 `ImageReaderManager.kt`：

```kotlin
package com.example.vicollector.camera

import android.media.ImageReader
import android.os.Handler

class ImageReaderManager(private val config: CameraConfig) {
    fun createReader(handler: Handler, onImage: (timestampNs: Long, bytes: ByteArray) -> Unit): ImageReader {
        val reader = ImageReader.newInstance(config.width, config.height, config.imageFormat, 4)
        reader.setOnImageAvailableListener({ imageReader ->
            val image = imageReader.acquireNextImage() ?: return@setOnImageAvailableListener
            image.use {
                val buffer = it.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                onImage(it.timestamp, bytes)
            }
        }, handler)
        return reader
    }
}
```

- [ ] 约束检查：`onImage` 中只允许入队与记录元数据，不允许直接写文件。

## Task 5：实现 CameraController 最小闭环

- [ ] `CameraController.startCamera()` 必须完成：
  - 检查 Camera 权限。
  - 启动 `HandlerThread`。
  - 选择后置主摄。
  - 创建 `ImageReader`。
  - 打开 `CameraDevice`。
  - 创建 `CameraCaptureSession`。
  - 创建 repeating request。

- [ ] `CameraController.stopCamera()` 必须完成：
  - 停止 repeating。
  - 关闭 session。
  - 关闭 camera device。
  - 关闭 image reader。
  - 停止 handler thread。

- [ ] `CaptureRequestFactory` 必须设置：
  - `CONTROL_MODE_AUTO`
  - `CONTROL_AE_TARGET_FPS_RANGE` 尽量靠近 30 FPS
  - JPEG 输出 surface

## Task 6：真机 30 秒采集验证

- [ ] 构建：

```powershell
cd VICollector
.\gradlew.bat assembleDebug
```

- [ ] 安装：

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

- [ ] 手工执行 30 秒采集，停止后拉取数据目录。

- [ ] 检查输出：

```text
images/
image_timestamps.csv
gyro.csv
accel.csv
```

## 阶段验收

- [ ] `gyro.csv` 第一行为 `timestamp_ns,gx,gy,gz,accuracy`。
- [ ] `accel.csv` 第一行为 `timestamp_ns,ax,ay,az,accuracy`。
- [ ] `image_timestamps.csv` 的 `timestamp_ns` 来自 Image timestamp 或 CaptureResult `SENSOR_TIMESTAMP`，不是写盘时间。
- [ ] 采集期间 UI 无明显卡死。
- [ ] 连续 30 秒采集后图像数、gyro 行数、accel 行数均大于 0。
