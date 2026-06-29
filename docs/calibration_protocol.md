# 标定协议

VICollector 当前保存原始图像 timestamp、gyro 和 accel。相机内参、camera-IMU 外参和 time offset 需要离线标定并单独归档。

## Kalibr 序列采集动作

1. 使用 AprilGrid 或 Kalibr 支持的标定板。
2. 保持标定板清晰可见，避免过曝、欠曝和运动模糊。
3. 让标定板覆盖图像不同区域，包含中心、四角和边缘。
4. 改变距离和朝向，覆盖近距离、中距离、俯仰、偏航和横滚。
5. 控制运动速度，保证图像仍可检测角点。

## IMU 激励

采集过程中需要覆盖各轴 IMU 激励：

1. 绕设备 x 轴正反方向旋转。
2. 绕设备 y 轴正反方向旋转。
3. 绕设备 z 轴正反方向旋转。
4. 沿 x、y、z 方向做小幅平移和加减速。
5. 避免长时间静止或只绕单一轴运动。

## 导出 Kalibr 输入

1. 拉取 session 后运行 `verify_dataset.py` 和 `check_timestamp.py`。
2. 确认图像数量、IMU CSV 非空、timestamp 单调。
3. 根据 `tools/calibration/kalibr_template.yaml`、`camchain_template.yaml` 和 `imu_template.yaml` 填写设备参数。
4. 将 VICollector 的 `image_timestamps.csv`、`gyro.csv`、`accel.csv` 转换为 Kalibr 所需 topic 或文件布局。
5. 保留原始 session，不要覆盖 VICollector 采集文件。

## 结果归档

每台设备和每个相机 profile 至少保存：

1. camera intrinsics。
2. camera distortion model 和 distortion coefficients。
3. camera-IMU extrinsics。
4. camera-IMU time offset。
5. Kalibr report、命令行参数、标定板参数和原始标定 session id。

标定结果应与设备型号、Android 版本、相机 ID、分辨率和 FPS 绑定，避免跨设备或跨 profile 误用。
