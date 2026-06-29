# 设备准备协议

## 基本准备

1. 打开 Android 开发者选项。
2. 启用 USB debugging。
3. 使用 USB 连接主机后，在手机端授权 ADB 调试。
4. 在主机运行 `adb devices`，确认设备状态为 `device`。
5. 安装并启动 VICollector，按系统弹窗授权 Camera。

## 采集前设备状态

1. 关闭无关后台应用，减少 CPU、内存和 I/O 抢占。
2. 保证电量充足；长序列建议连接稳定电源。
3. 保证存储空间充足，避免 JPEG 或 CSV 写入失败。
4. 关闭可能打断采集的通知、来电和省电限制。
5. 固定设备时间设置，避免跨时区或系统时间调整影响记录说明。

## 传感器确认

1. 在 App 设备信息或 `sensor_info.json` 中确认 gyroscope 存在。
2. 在 App 设备信息或 `sensor_info.json` 中确认 accelerometer 存在。
3. 如果任一 IMU 传感器缺失，不要将该设备用于视觉惯性数据集采集。
4. 记录设备型号、Android 版本和 Camera2 能力，便于后续兼容性分析。

## ADB 命令

```powershell
adb devices
powershell -NoProfile -ExecutionPolicy Bypass -File tools\adb\install_debug.ps1
```

```bash
adb devices
bash tools/adb/install_debug.sh
```
