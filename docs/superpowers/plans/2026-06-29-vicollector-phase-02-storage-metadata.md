# VICollector 阶段 2：存储与元数据计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` 或 `superpowers:executing-plans` 逐任务执行。所有步骤使用 checkbox 跟踪。

**目标：** 实现 session 目录、CSV/JSON writer、异步写盘队列和设备/传感器元数据输出。

**架构：** Camera 和 IMU callback 不直接执行重 I/O，只做轻量转换与入队。`storage` 模块集中处理文件创建、批量写入、flush 和 close；`device` 模块在 session 开始前采集设备、相机、传感器和运行时信息，并通过 `MetadataWriter` 写入 JSON。

**技术栈：** Kotlin、`java.io.File`、`org.json`、`ExecutorService`、JUnit 临时目录测试。

---

## 交付物

- 每次 session 创建独立目录：`VICollectorDataset/<yyyy_MM_dd_HH_mm_ss_<model>_sessionNNN>/`。
- 输出 `images/`、`image_timestamps.csv`、`gyro.csv`、`accel.csv`、`device_info.json`、`sensor_info.json`、`camera_info.json`、`runtime_info.json`、`session_config.json`。
- 所有 writer 支持 `flush()` 和 `close()`。
- `AsyncWriteQueue` 可报告当前队列长度。
- 异常时不删除已经写出的 session 数据。

## 文件清单

- Create: `VICollector/app/src/main/java/com/example/vicollector/storage/SessionDirectoryManager.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/storage/FileNameGenerator.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/storage/ImageWriter.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/storage/ImuCsvWriter.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/storage/ImageTimestampCsvWriter.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/storage/MetadataWriter.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/storage/AsyncWriteQueue.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/storage/DiskSpaceMonitor.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/storage/StorageManager.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/device/DeviceInfoCollector.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/device/AndroidBuildInfo.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/device/CameraDeviceInfo.kt`
- Create: `VICollector/app/src/main/java/com/example/vicollector/device/SensorDeviceInfo.kt`
- Create: `VICollector/app/src/test/java/com/example/vicollector/storage/SessionDirectoryManagerTest.kt`
- Create: `VICollector/app/src/test/java/com/example/vicollector/storage/ImuCsvWriterTest.kt`
- Create: `VICollector/app/src/test/java/com/example/vicollector/storage/MetadataWriterTest.kt`

## Task 1：实现 session 目录管理

- [x] 先写失败测试 `SessionDirectoryManagerTest.kt`：

```kotlin
package com.example.vicollector.storage

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SessionDirectoryManagerTest {
    @Test
    fun createSessionDirectoryCreatesImagesDirectory() {
        val root = createTempDir(prefix = "vicollector")
        val dir = SessionDirectoryManager(root).createSessionDirectory("Pixel8", 1)
        assertTrue(File(dir, "images").isDirectory)
        assertTrue(dir.name.endsWith("_Pixel8_session001"))
    }
}
```

- [x] 运行失败测试：

```powershell
cd VICollector
.\gradlew.bat testDebugUnitTest --tests "*SessionDirectoryManagerTest"
```

期望：因 `SessionDirectoryManager` 未定义而失败。

- [x] 写入 `SessionDirectoryManager.kt`：

```kotlin
package com.example.vicollector.storage

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionDirectoryManager(private val datasetRoot: File) {
    fun createSessionDirectory(deviceModel: String, index: Int, now: Date = Date()): File {
        val time = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US).format(now)
        val safeModel = deviceModel.replace(Regex("[^A-Za-z0-9_-]"), "")
        val dir = File(datasetRoot, "${time}_${safeModel}_session%03d".format(index))
        File(dir, "images").mkdirs()
        return dir
    }
}
```

- [x] 写入 `FileNameGenerator.kt`：

```kotlin
package com.example.vicollector.storage

class FileNameGenerator {
    fun imageFileName(frameIndex: Long): String = "%06d.jpg".format(frameIndex)
}
```

- [x] 运行测试，期望成功。

## Task 2：实现 IMU CSV writer

- [x] 写入测试 `ImuCsvWriterTest.kt`：

```kotlin
package com.example.vicollector.storage

import com.example.vicollector.core.model.ImuSample
import com.example.vicollector.core.model.SensorType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class ImuCsvWriterTest {
    @Test
    fun writesGyroHeaderAndRows() {
        val file = File.createTempFile("gyro", ".csv")
        ImuCsvWriter(file, SensorType.GYROSCOPE).use { writer ->
            writer.write(ImuSample(10L, SensorType.GYROSCOPE, 1f, 2f, 3f, 3))
        }
        assertEquals(
            listOf("timestamp_ns,gx,gy,gz,accuracy", "10,1.0,2.0,3.0,3"),
            file.readLines()
        )
    }
}
```

- [x] 写入 `ImuCsvWriter.kt`：

```kotlin
package com.example.vicollector.storage

import com.example.vicollector.core.model.ImuSample
import com.example.vicollector.core.model.SensorType
import java.io.BufferedWriter
import java.io.Closeable
import java.io.File
import java.io.FileWriter

class ImuCsvWriter(file: File, private val type: SensorType) : Closeable {
    private val writer = BufferedWriter(FileWriter(file))

    init {
        writer.write(
            when (type) {
                SensorType.GYROSCOPE -> "timestamp_ns,gx,gy,gz,accuracy"
                SensorType.ACCELEROMETER -> "timestamp_ns,ax,ay,az,accuracy"
            }
        )
        writer.newLine()
    }

    fun write(sample: ImuSample) {
        require(sample.sensorType == type)
        writer.write("${sample.timestampNs},${sample.x},${sample.y},${sample.z},${sample.accuracy}")
        writer.newLine()
    }

    fun flush() = writer.flush()
    override fun close() = writer.close()
}
```

- [x] 验证：

```powershell
cd VICollector
.\gradlew.bat testDebugUnitTest --tests "*ImuCsvWriterTest"
```

期望：`BUILD SUCCESSFUL`。

## Task 3：实现图像 timestamp CSV writer

- [x] 写入 `ImageTimestampCsvWriter.kt`：

```kotlin
package com.example.vicollector.storage

import com.example.vicollector.core.model.CameraFrame
import java.io.BufferedWriter
import java.io.Closeable
import java.io.File
import java.io.FileWriter

class ImageTimestampCsvWriter(file: File) : Closeable {
    private val writer = BufferedWriter(FileWriter(file))

    init {
        writer.write("frame_index,timestamp_ns,file_name,width,height,exposure_time_ns,iso")
        writer.newLine()
    }

    fun write(frame: CameraFrame) {
        writer.write(
            "${frame.frameIndex},${frame.timestampNs},${frame.fileName},${frame.width},${frame.height}," +
                "${frame.exposureTimeNs ?: ""},${frame.sensitivityIso ?: ""}"
        )
        writer.newLine()
    }

    fun flush() = writer.flush()
    override fun close() = writer.close()
}
```

- [x] 编译验证：

```powershell
cd VICollector
.\gradlew.bat compileDebugKotlin
```

期望：`BUILD SUCCESSFUL`。

## Task 4：实现 JSON 元数据 writer

- [x] 写入测试 `MetadataWriterTest.kt`：

```kotlin
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
```

- [x] 写入 `MetadataWriter.kt`：

```kotlin
package com.example.vicollector.storage

import org.json.JSONObject
import java.io.File

class MetadataWriter {
    fun writeJson(file: File, json: JSONObject) {
        file.parentFile?.mkdirs()
        file.writeText(json.toString(2))
    }
}
```

- [x] 验证测试成功。

## Task 5：实现异步写盘队列

- [x] 写入 `AsyncWriteQueue.kt`：

```kotlin
package com.example.vicollector.storage

import java.io.Closeable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class AsyncWriteQueue : Closeable {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val pending = AtomicInteger(0)

    fun enqueue(write: () -> Unit) {
        pending.incrementAndGet()
        executor.execute {
            try {
                write()
            } finally {
                pending.decrementAndGet()
            }
        }
    }

    fun size(): Int = pending.get()

    fun flushAndStop(timeoutSec: Long = 10) {
        executor.shutdown()
        executor.awaitTermination(timeoutSec, TimeUnit.SECONDS)
    }

    override fun close() = flushAndStop()
}
```

- [x] 约束检查：Camera callback 和 Sensor callback 后续只能调用 `enqueue` 或写入内存 buffer，不能直接写图片和 CSV。

## 阶段验收

- [x] `cd VICollector; .\gradlew.bat testDebugUnitTest assembleDebug` 成功。
- [x] session 目录结构与 `VICollectorDataset/...` 约定一致。
- [x] CSV 字段为 `timestamp_ns`，单位为 nanoseconds。
- [x] writer 均可 `flush()` 和 `close()`。
