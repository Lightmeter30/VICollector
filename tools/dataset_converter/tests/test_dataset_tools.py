import csv
import json
import subprocess
import sys
from pathlib import Path


TOOLS_DIR = Path(__file__).resolve().parents[1]


def write_csv(path: Path, fieldnames: list[str], rows: list[dict[str, object]]) -> None:
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)


def create_session(root: Path) -> Path:
    session = root / "session_001"
    images = session / "images"
    images.mkdir(parents=True)
    for name in ["0000000001000000000.jpg", "0000000001033333333.jpg"]:
        (images / name).write_bytes(b"jpg")

    write_csv(
        session / "image_timestamps.csv",
        ["frame_index", "timestamp_ns", "file_name", "width", "height", "exposure_time_ns", "iso"],
        [
            {
                "frame_index": 0,
                "timestamp_ns": 1_000_000_000,
                "file_name": "0000000001000000000.jpg",
                "width": 1920,
                "height": 1080,
                "exposure_time_ns": "",
                "iso": "",
            },
            {
                "frame_index": 1,
                "timestamp_ns": 1_033_333_333,
                "file_name": "0000000001033333333.jpg",
                "width": 1920,
                "height": 1080,
                "exposure_time_ns": "",
                "iso": "",
            },
        ],
    )
    write_csv(
        session / "gyro.csv",
        ["timestamp_ns", "gx", "gy", "gz", "accuracy"],
        [
            {"timestamp_ns": 1_000_000_000, "gx": 0.1, "gy": 0.2, "gz": 0.3, "accuracy": 3},
            {"timestamp_ns": 1_005_000_000, "gx": 0.2, "gy": 0.3, "gz": 0.4, "accuracy": 3},
        ],
    )
    write_csv(
        session / "accel.csv",
        ["timestamp_ns", "ax", "ay", "az", "accuracy"],
        [
            {"timestamp_ns": 1_000_000_000, "ax": 0.1, "ay": 0.2, "az": 9.8, "accuracy": 3},
            {"timestamp_ns": 1_005_000_000, "ax": 0.2, "ay": 0.3, "az": 9.7, "accuracy": 3},
        ],
    )
    for name in [
        "device_info.json",
        "sensor_info.json",
        "session_config.json",
        "session_summary.json",
        "diagnostics.json",
    ]:
        (session / name).write_text("{}", encoding="utf-8")
    return session


def run_tool(script: str, *args: Path) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [sys.executable, str(TOOLS_DIR / script), *[str(arg) for arg in args]],
        text=True,
        capture_output=True,
        check=False,
    )


def test_verify_dataset_accepts_complete_monotonic_session(tmp_path: Path) -> None:
    session = create_session(tmp_path)

    result = run_tool("verify_dataset.py", session)

    assert result.returncode == 0, result.stderr
    payload = json.loads(result.stdout)
    assert payload["missing"] == []
    assert payload["image_file_count"] == 2
    assert payload["image_count_matches"] is True
    assert payload["gyro_non_empty"] is True
    assert payload["accel_non_empty"] is True
    assert payload["image_timestamp_monotonic"] is True
    assert payload["gyro_hz"] > 0


def test_verify_dataset_rejects_non_monotonic_gyro(tmp_path: Path) -> None:
    session = create_session(tmp_path)
    write_csv(
        session / "gyro.csv",
        ["timestamp_ns", "gx", "gy", "gz", "accuracy"],
        [
            {"timestamp_ns": 2_000_000_000, "gx": 0.1, "gy": 0.2, "gz": 0.3, "accuracy": 3},
            {"timestamp_ns": 1_000_000_000, "gx": 0.2, "gy": 0.3, "gz": 0.4, "accuracy": 3},
        ],
    )

    result = run_tool("verify_dataset.py", session)

    assert result.returncode == 1
    payload = json.loads(result.stdout)
    assert payload["gyro_timestamp_monotonic"] is False


def test_check_timestamp_prints_all_stream_statistics(tmp_path: Path) -> None:
    session = create_session(tmp_path)

    result = run_tool("check_timestamp.py", session)

    assert result.returncode == 0, result.stderr
    assert "camera: count=2" in result.stdout
    assert "gyro: count=2" in result.stdout
    assert "accel: count=2" in result.stdout
    assert "mean_ns=" in result.stdout


def test_euroc_converter_writes_source_manifest(tmp_path: Path) -> None:
    session = create_session(tmp_path)
    output = tmp_path / "euroc"

    result = run_tool("convert_to_euroc.py", session, output)

    assert result.returncode == 0, result.stderr
    manifest = json.loads((output / "export_manifest_euroc.json").read_text(encoding="utf-8"))
    assert manifest["format"] == "euroc"
    assert manifest["sources"]["images"].endswith("images")
    assert manifest["sources"]["image_timestamps_csv"].endswith("image_timestamps.csv")
    assert "device_info_json" in manifest["metadata"]


def test_rosbag_converter_writes_topic_manifest(tmp_path: Path) -> None:
    session = create_session(tmp_path)
    output = tmp_path / "rosbag"

    result = run_tool("convert_to_rosbag.py", session, output)

    assert result.returncode == 0, result.stderr
    manifest = json.loads((output / "export_manifest_rosbag.json").read_text(encoding="utf-8"))
    assert manifest["format"] == "rosbag"
    assert manifest["topics"]["camera"] == "/camera/image_raw"
    assert manifest["topics"]["gyro"] == "/imu/gyro"
    assert manifest["topics"]["accel"] == "/imu/accel"
    assert manifest["timestamp_unit"] == "nanoseconds"
