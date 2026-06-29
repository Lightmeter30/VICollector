from __future__ import annotations

import argparse
import json
from pathlib import Path

from dataset_manifest import existing_metadata_paths, missing_required_items


def build_manifest(session: Path) -> dict[str, object]:
    missing = missing_required_items(session)
    if missing:
        raise FileNotFoundError(f"Missing required VICollector items: {', '.join(missing)}")
    return {
        "format": "rosbag",
        "status": "manifest_only",
        "timestamp_unit": "nanoseconds",
        "topics": {
            "camera": "/camera/image_raw",
            "gyro": "/imu/gyro",
            "accel": "/imu/accel",
        },
        "sources": {
            "images": str(session / "images"),
            "image_timestamps_csv": str(session / "image_timestamps.csv"),
            "gyro_csv": str(session / "gyro.csv"),
            "accel_csv": str(session / "accel.csv"),
        },
        "metadata": existing_metadata_paths(session),
        "next_step": "Write camera and IMU messages with ROS timestamps derived from timestamp_ns.",
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Create a manifest for future ROS bag export.")
    parser.add_argument("session", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    manifest = build_manifest(args.session)
    args.output.mkdir(parents=True, exist_ok=True)
    (args.output / "export_manifest_rosbag.json").write_text(
        json.dumps(manifest, indent=2) + "\n",
        encoding="utf-8",
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
