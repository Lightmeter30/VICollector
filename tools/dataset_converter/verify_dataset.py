from __future__ import annotations

import argparse
import csv
import json
from pathlib import Path

from dataset_manifest import REQUIRED_FILES, missing_required_items


REQUIRED = ["images", *REQUIRED_FILES]


def read_csv(path: Path) -> list[dict[str, str]]:
    if not path.exists():
        return []
    with path.open("r", encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle))


def monotonic(rows: list[dict[str, str]]) -> bool:
    if not rows:
        return False
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
    missing = missing_required_items(session)
    image_rows = read_csv(session / "image_timestamps.csv")
    gyro_rows = read_csv(session / "gyro.csv")
    accel_rows = read_csv(session / "accel.csv")
    image_count = len(list((session / "images").glob("*.jpg"))) if (session / "images").exists() else 0
    return {
        "session": str(session),
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
    parser = argparse.ArgumentParser(description="Verify one VICollector session directory.")
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
