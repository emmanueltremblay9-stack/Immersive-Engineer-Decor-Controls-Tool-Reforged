#!/usr/bin/env python3
"""Compare native RGBA pixels and report hashes for a reference/render pair."""
from __future__ import annotations

import argparse
import hashlib
from pathlib import Path

from PIL import Image


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest().upper()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("reference", type=Path)
    parser.add_argument("render", type=Path)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()

    reference = Image.open(args.reference).convert("RGBA")
    rendered = Image.open(args.render).convert("RGBA")
    if reference.size != rendered.size:
        raise SystemExit(f"dimension mismatch: {reference.size} != {rendered.size}")
    a, b = reference.tobytes(), rendered.tobytes()
    mismatches = sum(a[i : i + 4] != b[i : i + 4] for i in range(0, len(a), 4))
    report = (
        f"dimensions: {reference.width}x{reference.height}\n"
        f"reference_sha256: {sha256(args.reference)}\n"
        f"render_sha256: {sha256(args.render)}\n"
        f"rgba_pixel_mismatches: {mismatches}\n"
    )
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(report, encoding="utf-8", newline="\n")
    print(report, end="")
    return 0 if mismatches == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())

