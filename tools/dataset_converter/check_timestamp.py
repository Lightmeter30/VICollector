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
    parser = argparse.ArgumentParser(description="Print interval statistics for VICollector timestamps.")
    parser.add_argument("session", type=Path)
    args = parser.parse_args()
    describe("camera", read_timestamps(args.session / "image_timestamps.csv"))
    describe("gyro", read_timestamps(args.session / "gyro.csv"))
    describe("accel", read_timestamps(args.session / "accel.csv"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
