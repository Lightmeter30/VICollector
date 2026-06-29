# VICollector 阶段 5：主机工具、文档与扩展计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` 或 `superpowers:executing-plans` 逐任务执行。所有步骤使用 checkbox 跟踪。

**目标：** 完成 ADB 拉取、数据集验证、timestamp 检查、文档和 v0.2/v1.0 扩展入口，使 VICollector 从可采集 App 变成可交付的科研数据采集工程。

**架构：** 主机工具只读取 App 输出的 session 目录，不修改原始采集文件。文档固定数据格式、设备准备、采集流程和 Kalibr 标定流程；扩展计划保留 YUV raw、binary logger、Kalibr export、EuRoC export、ROS bag export 与批量采集协议。

**技术栈：** PowerShell、Bash、Python 3、csv、json、pathlib、Android Debug Bridge、Markdown。

---

## 交付物

- Windows 与 Ubuntu 均可拉取设备中的 `VICollectorDataset`。
- `verify_dataset.py` 可检查必需文件、图像数量、CSV 非空、timestamp 单调和基础频率。
- `check_timestamp.py` 可输出 camera、gyro、accel 的 interval min/max/mean/std。
- 文档说明 session 结构、字段、单位、坐标约定、设备准备、采集命名和质量检查。
- v0.2/v1.0 扩展入口以脚本和文档形式固化。

## 文件清单

- Create: `VICollector/tools/adb/pull_dataset.sh`
- Create: `VICollector/tools/adb/pull_dataset.ps1`
- Create: `VICollector/tools/adb/install_debug.sh`
- Create: `VICollector/tools/adb/install_debug.ps1`
- Create: `VICollector/tools/adb/clear_device_dataset.sh`
- Create: `VICollector/tools/adb/batch_collect_helper.sh`
- Create: `VICollector/tools/dataset_converter/verify_dataset.py`
- Create: `VICollector/tools/dataset_converter/check_timestamp.py`
- Create: `VICollector/tools/dataset_converter/plot_sensor_rate.py`
- Create: `VICollector/tools/dataset_converter/convert_to_euroc.py`
- Create: `VICollector/tools/dataset_converter/convert_to_rosbag.py`
- Create: `VICollector/tools/calibration/kalibr_template.yaml`
- Create: `VICollector/tools/calibration/camchain_template.yaml`
- Create: `VICollector/tools/calibration/imu_template.yaml`
- Create: `VICollector/docs/data_format.md`
- Create: `VICollector/docs/device_protocol.md`
- Create: `VICollector/docs/calibration_protocol.md`
- Create: `VICollector/docs/collection_protocol.md`
- Create: `VICollector/docs/known_issues.md`
- Create: `VICollector/README.md`

## Task 1：实现 ADB 拉取与安装脚本

- [ ] 写入 `tools/adb/pull_dataset.sh`：

```bash
#!/usr/bin/env bash
set -euo pipefail

DEVICE_PATH="/sdcard/Android/data/com.example.vicollector/files/VICollectorDataset"
LOCAL_PATH="${1:-./data}"

adb devices
mkdir -p "${LOCAL_PATH}"
adb pull "${DEVICE_PATH}" "${LOCAL_PATH}/"
```

- [ ] 写入 `tools/adb/pull_dataset.ps1`：

```powershell
$DevicePath = "/sdcard/Android/data/com.example.vicollector/files/VICollectorDataset"
$LocalPath = if ($args.Count -ge 1) { $args[0] } else { "./data" }

adb devices
New-Item -ItemType Directory -Force -Path $LocalPath | Out-Null
adb pull $DevicePath $LocalPath
```

- [ ] 写入 `tools/adb/install_debug.sh`：

```bash
#!/usr/bin/env bash
set -euo pipefail

./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

- [ ] 写入 `tools/adb/install_debug.ps1`：

```powershell
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

- [ ] 验证 PowerShell 脚本：

```powershell
cd VICollector
powershell -NoProfile -ExecutionPolicy Bypass -File tools\adb\install_debug.ps1
```

期望：连接设备时 `adb install` 输出 `Success`。

## Task 2：实现数据集验证脚本

- [ ] 写入 `tools/dataset_converter/verify_dataset.py`：

```python
from __future__ import annotations

import argparse
import csv
import json
from pathlib import Path

REQUIRED = [
    "images",
    "image_timestamps.csv",
    "gyro.csv",
    "accel.csv",
    "device_info.json",
    "sensor_info.json",
    "session_config.json",
    "session_summary.json",
    "diagnostics.json",
]


def read_csv(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle))


def monotonic(rows: list[dict[str, str]]) -> bool:
    values = [int(row["timestamp_ns"]) for row in rows]
    return all(b > a for a, b in zip(values, values[1:]))


def rate_hz(rows: list[dict[str, str]]) -> float:
    if len(rows) < 2:
        return 0.0
    first = int(rows[0]["timestamp_ns"])
    last = int(rows[-1]["timestamp_ns"])
    if last <= first:
        return 0.0
    return (len(rows) - 1) * 1_000_000_000.0 / (last - first)


def verify(session: Path) -> dict[str, object]:
    missing = [name for name in REQUIRED if not (session / name).exists()]
    image_rows = read_csv(session / "image_timestamps.csv")
    gyro_rows = read_csv(session / "gyro.csv")
    accel_rows = read_csv(session / "accel.csv")
    image_count = len(list((session / "images").glob("*.jpg")))
    return {
        "missing": missing,
        "image_file_count": image_count,
        "image_timestamp_rows": len(image_rows),
        "image_count_matches": image_count == len(image_rows),
        "gyro_non_empty": len(gyro_rows) > 0,
        "accel_non_empty": len(accel_rows) > 0,
        "image_timestamp_monotonic": monotonic(image_rows),
        "gyro_timestamp_monotonic": monotonic(gyro_rows),
        "accel_timestamp_monotonic": monotonic(accel_rows),
        "camera_fps": rate_hz(image_rows),
        "gyro_hz": rate_hz(gyro_rows),
        "accel_hz": rate_hz(accel_rows),
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("session", type=Path)
    args = parser.parse_args()
    result = verify(args.session)
    print(json.dumps(result, indent=2))
    failed = bool(result["missing"]) or not result["image_count_matches"]
    failed = failed or not result["gyro_non_empty"] or not result["accel_non_empty"]
    failed = failed or not result["image_timestamp_monotonic"]
    failed = failed or not result["gyro_timestamp_monotonic"]
    failed = failed or not result["accel_timestamp_monotonic"]
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
```

- [ ] 使用真实采集数据验证：

```powershell
cd VICollector
python tools\dataset_converter\verify_dataset.py data\VICollectorDataset\<session_id>
```

期望：exit code 0，`image_count_matches`、`gyro_non_empty`、`accel_non_empty` 均为 `true`。

## Task 3：实现 timestamp 检查脚本

- [ ] 写入 `tools/dataset_converter/check_timestamp.py`：

```python
from __future__ import annotations

import argparse
import csv
import statistics
from pathlib import Path


def read_timestamps(path: Path) -> list[int]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        return [int(row["timestamp_ns"]) for row in csv.DictReader(handle)]


def describe(name: str, timestamps: list[int]) -> None:
    intervals = [b - a for a, b in zip(timestamps, timestamps[1:])]
    if not intervals:
        print(f"{name}: count={len(timestamps)} intervals=0")
        return
    print(
        f"{name}: count={len(timestamps)} "
        f"min_ns={min(intervals)} max_ns={max(intervals)} "
        f"mean_ns={statistics.mean(intervals):.2f} "
        f"std_ns={statistics.pstdev(intervals):.2f} "
        f"duplicates={sum(1 for value in intervals if value == 0)} "
        f"non_monotonic={sum(1 for value in intervals if value < 0)}"
    )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("session", type=Path)
    args = parser.parse_args()
    describe("camera", read_timestamps(args.session / "image_timestamps.csv"))
    describe("gyro", read_timestamps(args.session / "gyro.csv"))
    describe("accel", read_timestamps(args.session / "accel.csv"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
```

- [ ] 使用真实采集数据验证：

```powershell
cd VICollector
python tools\dataset_converter\check_timestamp.py data\VICollectorDataset\<session_id>
```

期望：输出包含 `camera:`、`gyro:`、`accel:` 三行统计。

## Task 4：编写数据格式与采集协议文档

- [ ] `docs/data_format.md` 必须包含：
  - session 目录结构。
  - `image_timestamps.csv` 字段。
  - `gyro.csv` 字段。
  - `accel.csv` 字段。
  - JSON 文件含义。
  - timestamp 单位为 nanoseconds。
  - Gyroscope 单位为 rad/s。
  - Accelerometer 单位为 m/s²。
  - App 不做重力移除、不做 bias 去除、不做强制同步。

- [ ] `docs/device_protocol.md` 必须包含：
  - 打开开发者选项。
  - 授权 Camera。
  - 连接 ADB。
  - 关闭后台应用。
  - 保证电量。
  - 保证存储空间。
  - 确认 gyroscope 和 accelerometer 存在。

- [ ] `docs/collection_protocol.md` 必须包含：
  - 采集命名规范。
  - 采集时长建议。
  - 采集前检查。
  - 采集后拉取数据。
  - 运行 `verify_dataset.py`。
  - 运行 `check_timestamp.py`。

- [ ] `docs/calibration_protocol.md` 必须包含：
  - Kalibr 序列采集动作。
  - 各轴 IMU 激励。
  - 导出 Kalibr 输入。
  - 保存 camera intrinsics、camera-IMU extrinsics 和 time offset。

## Task 5：固化 v0.2 和 v1.0 扩展入口

- [ ] 在 `docs/known_issues.md` 中记录 v0.2 必须补齐：
  - YUV raw support。
  - exposure metadata。
  - camera FPS diagnostics。
  - IMU jitter diagnostics。
  - dropped frame detection。
  - dataset verification script。

- [ ] 在 `docs/known_issues.md` 中记录 v1.0 必须补齐：
  - multi-profile capture。
  - calibration mode。
  - binary logger。
  - Kalibr export。
  - EuRoC export。
  - ROS bag export。
  - batch device collection protocol。
  - device compatibility report。

- [ ] `convert_to_euroc.py` 初版必须完成输入目录检查，并生成 `export_manifest_euroc.json`，记录将被转换的 `images/`、`image_timestamps.csv`、`gyro.csv`、`accel.csv` 和 metadata 文件路径。

- [ ] `convert_to_rosbag.py` 初版必须完成输入目录检查，并生成 `export_manifest_rosbag.json`，记录将被写入 ROS bag 的 camera topic、gyro topic、accel topic、源文件路径和 timestamp 单位。

## 阶段验收

- [ ] `cd VICollector; .\gradlew.bat clean assembleDebug` 成功。
- [ ] `python tools\dataset_converter\verify_dataset.py data\VICollectorDataset\<session_id>` exit code 0。
- [ ] `python tools\dataset_converter\check_timestamp.py data\VICollectorDataset\<session_id>` 输出 camera、gyro、accel interval 统计。
- [ ] README 明确说明构建、安装、采集、拉取、验证和已知问题。
- [ ] 文档没有把写盘时间当作采集时间，没有要求 App 端滤波、去重力、去 bias 或强制同步。

