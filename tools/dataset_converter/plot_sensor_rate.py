from __future__ import annotations

import argparse
import csv
import json
from pathlib import Path


def read_timestamps(path: Path) -> list[int]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        return [int(row["timestamp_ns"]) for row in csv.DictReader(handle)]


def interval_summary(path: Path) -> dict[str, float | int]:
    timestamps = read_timestamps(path)
    intervals = [b - a for a, b in zip(timestamps, timestamps[1:])]
    if not intervals:
        return {"count": len(timestamps), "interval_count": 0, "mean_hz": 0.0}
    duration_ns = timestamps[-1] - timestamps[0]
    mean_hz = ((len(timestamps) - 1) * 1_000_000_000.0 / duration_ns) if duration_ns > 0 else 0.0
    return {
        "count": len(timestamps),
        "interval_count": len(intervals),
        "min_interval_ns": min(intervals),
        "max_interval_ns": max(intervals),
        "mean_hz": mean_hz,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Export sensor rate summaries for plotting.")
    parser.add_argument("session", type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    summary = {
        "camera": interval_summary(args.session / "image_timestamps.csv"),
        "gyro": interval_summary(args.session / "gyro.csv"),
        "accel": interval_summary(args.session / "accel.csv"),
    }
    payload = json.dumps(summary, indent=2)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(payload + "\n", encoding="utf-8")
    else:
        print(payload)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
