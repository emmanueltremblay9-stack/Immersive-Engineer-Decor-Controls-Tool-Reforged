#!/usr/bin/env python3
"""Convert an RGBA PNG to an exact Pixel Art Toolkit pixels operation."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from PIL import Image


def encode_image(source: Path) -> dict[str, object]:
    image = Image.open(source).convert("RGBA")
    width, height = image.size
    raw = image.tobytes()
    colors = sorted(
        {
            tuple(raw[offset : offset + 4])
            for offset in range(0, len(raw), 4)
            if raw[offset + 3] != 0
        }
    )
    color_names = {color: f"c{index:05d}" for index, color in enumerate(colors)}
    palette = {
        color_names[color]: "#" + "".join(f"{channel:02x}" for channel in color)
        for color in colors
    }
    points = []
    for y in range(height):
        row_offset = y * width * 4
        for x in range(width):
            offset = row_offset + x * 4
            color = tuple(raw[offset : offset + 4])
            if color[3] != 0:
                points.append([x, y, color_names[color]])

    return {
        "version": "0.1",
        "canvas": {"width": width, "height": height, "background": None},
        "palette": palette,
        "layers": [
            {
                "name": "exact-reference",
                "operations": [{"op": "pixels", "points": points}],
            }
        ],
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("reference", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    document = encode_image(args.reference)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(document, separators=(",", ":"), ensure_ascii=False, allow_nan=False) + "\n",
        encoding="utf-8",
        newline="\n",
    )
    print(
        f"wrote {args.output}: {document['canvas']['width']}x{document['canvas']['height']}, "
        f"{len(document['palette'])} colors, "
        f"{len(document['layers'][0]['operations'][0]['points'])} source pixels"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

