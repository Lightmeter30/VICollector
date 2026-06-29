# VICollector

VICollector 是一个 Android 视觉惯性数据采集 App。当前版本采集 JPEG 图像、相机 timestamp、gyroscope、accelerometer 和设备 metadata，用于科研数据集构建、标定和离线验证。

## 构建

在仓库根目录运行：

```powershell
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat clean assembleDebug
```

非 Windows shell 使用 `./gradlew` 和相同 Gradle task。

## 安装

Windows：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\adb\install_debug.ps1
```

Ubuntu 或其他 Bash 环境：

```bash
bash tools/adb/install_debug.sh
```

安装前先运行 `adb devices`，确认设备已授权并显示为 `device`。

## 采集

1. 按 `docs/device_protocol.md` 完成设备准备。
2. 打开 VICollector 并授权 Camera。
3. 确认 gyroscope 和 accelerometer 可用。
4. 选择采集 profile，开始采集。
5. 采集结束后记录 session id。

App 将数据写入设备端：

```text
/sdcard/Android/data/com.example.vicollector/files/VICollectorDataset
```

Session 格式、CSV 字段、单位和 JSON 含义见 `docs/data_format.md`。

## 拉取数据

Windows：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\adb\pull_dataset.ps1 .\data
```

Ubuntu 或其他 Bash 环境：

```bash
bash tools/adb/pull_dataset.sh ./data
```

清理设备端数据前先确认本地已有备份：

```bash
bash tools/adb/clear_device_dataset.sh
```

## 验证

使用可用的 Python 3 解释器运行：

```powershell
python tools\dataset_converter\verify_dataset.py data\VICollectorDataset\<session_id>
python tools\dataset_converter\check_timestamp.py data\VICollectorDataset\<session_id>
```

或：

```bash
python3 tools/dataset_converter/verify_dataset.py data/VICollectorDataset/<session_id>
python3 tools/dataset_converter/check_timestamp.py data/VICollectorDataset/<session_id>
```

`verify_dataset.py` 检查必需文件、图像数量、CSV 非空、timestamp 单调和基础频率。`check_timestamp.py` 输出 camera、gyro、accel 的 interval min/max/mean/std、重复和非单调计数。

## 标定与导出

Kalibr 标定流程见 `docs/calibration_protocol.md`。模板位于 `tools/calibration/`。

当前 EuRoC 和 ROS bag 工具是扩展入口，会检查输入 session 并写出 manifest：

```bash
python3 tools/dataset_converter/convert_to_euroc.py data/VICollectorDataset/<session_id> exports/euroc
python3 tools/dataset_converter/convert_to_rosbag.py data/VICollectorDataset/<session_id> exports/rosbag
```

## 已知问题

v0.2 和 v1.0 扩展列表见 `docs/known_issues.md`。当前 App 不在采集端做重力移除、bias 去除、滤波、跨传感器重采样或强制同步；不要把图像写盘时间当作采集时间，后处理应使用 CSV 中的 `timestamp_ns`。
