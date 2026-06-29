# 采集协议

## 命名规范

Session 目录由 App 自动生成，格式为：

```text
yyyy_MM_dd_HH_mm_ss_DeviceModel_sessionNNN
```

外部实验记录建议使用同样的 session id，并补充场景名、操作者、轨迹类型和备注，例如：

```text
2026_06_29_213000_Pixel8_session001_indoor_loop_operatorA
```

## 采集时长建议

1. 功能冒烟测试：10 到 20 秒。
2. 单段普通视觉惯性序列：60 到 180 秒。
3. 标定序列：60 到 120 秒，动作应覆盖不同方向和角速度。
4. 长序列采集前先完成短序列验证，确认图像、gyro 和 accel 都在写入。

## 采集前检查

1. 设备已授权 Camera 和 ADB。
2. gyroscope 与 accelerometer 均存在。
3. 存储空间充足。
4. 后台应用已关闭。
5. 目标 profile、分辨率、FPS 和 IMU 设置符合实验要求。
6. 镜头干净，场景有足够纹理和光照。

## 采集后拉取数据

Windows：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\adb\pull_dataset.ps1 .\data
```

Ubuntu 或其他 Bash 环境：

```bash
bash tools/adb/pull_dataset.sh ./data
```

脚本会从设备路径 `/sdcard/Android/data/com.example.vicollector/files/VICollectorDataset` 拉取完整数据集。

## 质量检查

对每个 session 运行：

```powershell
python tools\dataset_converter\verify_dataset.py data\VICollectorDataset\<session_id>
python tools\dataset_converter\check_timestamp.py data\VICollectorDataset\<session_id>
```

也可以使用系统 Python：

```bash
python3 tools/dataset_converter/verify_dataset.py data/VICollectorDataset/<session_id>
python3 tools/dataset_converter/check_timestamp.py data/VICollectorDataset/<session_id>
```

`verify_dataset.py` 应返回 exit code 0，并显示 `image_count_matches`、`gyro_non_empty`、`accel_non_empty` 和三个 timestamp monotonic 字段为 `true`。`check_timestamp.py` 应输出 `camera:`、`gyro:`、`accel:` 三行 interval 统计。

## 注意事项

不要把图像写盘时间当作采集时间。后处理应以 CSV 中的 `timestamp_ns` 为准。App 不在采集端做滤波、去重力、去 bias、跨传感器重采样或强制同步。
