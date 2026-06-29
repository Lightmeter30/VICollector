from __future__ import annotations

from pathlib import Path


REQUIRED_FILES = [
    "image_timestamps.csv",
    "gyro.csv",
    "accel.csv",
    "device_info.json",
    "sensor_info.json",
    "session_config.json",
    "session_summary.json",
    "diagnostics.json",
]

METADATA_FILES = [
    "device_info.json",
    "sensor_info.json",
    "camera_info.json",
    "runtime_info.json",
    "session_config.json",
    "session_summary.json",
    "diagnostics.json",
]


def required_paths(session: Path) -> dict[str, Path]:
    paths = {"images": session / "images"}
    paths.update({name: session / name for name in REQUIRED_FILES})
    return paths


def missing_required_items(session: Path) -> list[str]:
    missing = []
    for name, path in required_paths(session).items():
        if not path.exists():
            missing.append(name)
    return missing


def existing_metadata_paths(session: Path) -> dict[str, str]:
    return {
        name.replace(".", "_"): str(session / name)
        for name in METADATA_FILES
        if (session / name).exists()
    }
