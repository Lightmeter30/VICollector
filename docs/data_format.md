# VICollector 数据格式

VICollector 每次采集生成一个 session 目录。主机工具只读取 session 内容，不修改原始采集文件。

## 目录结构

```text
VICollectorDataset/
  2026_06_29_213000_DeviceModel_session001/
    images/
      000000000000.jpg
      000000000001.jpg
    image_timestamps.csv
    gyro.csv
    accel.csv
    device_info.json
    sensor_info.json
    camera_info.json
    runtime_info.json
    session_config.json
    session_summary.json
    diagnostics.json
```

`camera_info.json` 和 `runtime_info.json` 是推荐保留的附加 metadata；验证脚本的必需项是 `images/`、三个 CSV、`device_info.json`、`sensor_info.json`、`session_config.json`、`session_summary.json` 和 `diagnostics.json`。

## image_timestamps.csv

字段：

| 字段 | 含义 |
| --- | --- |
| `frame_index` | App 内递增帧序号。 |
| `timestamp_ns` | 相机帧采集 timestamp，单位为 nanoseconds。 |
| `file_name` | 对应 `images/` 下的 JPEG 文件名。 |
| `width` | 图像宽度，单位为 pixel。 |
| `height` | 图像高度，单位为 pixel。 |
| `exposure_time_ns` | Android Camera2 暴光时间；设备未提供时为空。 |
| `iso` | Android Camera2 sensitivity ISO；设备未提供时为空。 |

`timestamp_ns` 不是 JPEG 写盘完成时间。图像文件写盘可能晚于采集事件。

## gyro.csv

字段：

| 字段 | 含义 |
| --- | --- |
| `timestamp_ns` | 陀螺仪事件 timestamp，单位为 nanoseconds。 |
| `gx` | 绕 x 轴角速度，单位为 rad/s。 |
| `gy` | 绕 y 轴角速度，单位为 rad/s。 |
| `gz` | 绕 z 轴角速度，单位为 rad/s。 |
| `accuracy` | Android SensorEvent accuracy。 |

## accel.csv

字段：

| 字段 | 含义 |
| --- | --- |
| `timestamp_ns` | 加速度计事件 timestamp，单位为 nanoseconds。 |
| `ax` | x 轴加速度，单位为 m/s^2。 |
| `ay` | y 轴加速度，单位为 m/s^2。 |
| `az` | z 轴加速度，单位为 m/s^2。 |
| `accuracy` | Android SensorEvent accuracy。 |

## JSON 文件

| 文件 | 含义 |
| --- | --- |
| `device_info.json` | 设备型号、Android 版本、硬件与构建信息。 |
| `sensor_info.json` | Gyroscope 和 accelerometer 的传感器名称、厂商、版本、量程、分辨率和功耗等信息。 |
| `camera_info.json` | 相机 ID、输出尺寸、硬件等级和 Camera2 能力信息。 |
| `runtime_info.json` | App 运行时信息，例如版本、权限或运行环境。 |
| `session_config.json` | 本次采集使用的 profile、目标 FPS、IMU 采样配置和诊断开关。 |
| `session_summary.json` | 采集结束后的帧数、IMU 数量、时长、估计频率和错误计数。 |
| `diagnostics.json` | timestamp 单调性、重复、异常间隔、丢帧估计和写队列诊断。 |

## 坐标与同步边界

Camera、gyroscope 和 accelerometer 使用 Android 设备报告的原始坐标约定。App 不做相机-IMU 外参求解，不做重力移除，不做 bias 去除，不做滤波，不做数据重采样，也不强制同步不同传感器流。跨传感器对齐、bias 建模、重力处理和外参标定应在离线处理或 Kalibr 等标定工具中完成。
