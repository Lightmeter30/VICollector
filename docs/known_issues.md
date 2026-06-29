# 已知问题与扩展入口

## v0.2 必须补齐

1. YUV raw support：当前保存 JPEG，后续需要支持 YUV raw 以降低编码损失和压缩不确定性。
2. exposure metadata：当前尽力记录 `exposure_time_ns` 和 `iso`，后续需要更完整的曝光、增益、AE 状态和 rolling shutter 相关 metadata。
3. camera FPS diagnostics：当前有基础 timestamp 诊断，后续需要更细的 FPS 稳定性、异常间隔分桶和采集期趋势。
4. IMU jitter diagnostics：当前可统计 min/max/mean/std，后续需要 jitter 分布、窗口化统计和阈值告警。
5. dropped frame detection：当前有基于目标间隔的估计，后续需要结合 frame index、Camera2 metadata 和实际文件落盘检查。
6. dataset verification script：当前已有初版 `verify_dataset.py`，后续需要覆盖更多 schema、metadata 合法性和跨文件一致性。

## v1.0 必须补齐

1. multi-profile capture：支持多分辨率、多 FPS 和多 IMU rate 的标准 profile 切换。
2. calibration mode：提供专用标定采集模式和标定质量提示。
3. binary logger：用二进制日志降低 CSV 写入开销并提升长序列可靠性。
4. Kalibr export：直接生成 Kalibr 可消费的输入布局和配置草稿。
5. EuRoC export：将 session 转换为 EuRoC 风格目录、CSV 和图像命名。
6. ROS bag export：将 camera、gyro 和 accel 写入 ROS bag。
7. batch device collection protocol：固化多设备、多轮采集、命名和质检流程。
8. device compatibility report：记录不同设备的相机能力、IMU rate、timestamp 稳定性和失败模式。

## 当前边界

App 端不做滤波、去重力、去 bias 或强制同步；这些处理属于离线标定、估计或数据清洗阶段。文档和工具应始终把原始采集数据与派生数据分开。
