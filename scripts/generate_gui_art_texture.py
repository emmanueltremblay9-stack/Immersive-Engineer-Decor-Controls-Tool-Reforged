#!/usr/bin/env python3
"""Create a native-size runtime GUI crop from an exact RGBA reference PNG.

This script uses only the Python standard library. It never edits the input.
The resulting crop preserves every RGBA pixel inside the reference alpha bounds.
"""

from __future__ import annotations

import argparse
import struct
import zlib
from pathlib import Path


PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"


def read_chunks(data: bytes) -> list[tuple[bytes, bytes]]:
    if not data.startswith(PNG_SIGNATURE):
        raise ValueError("Input is not a PNG")
    chunks = []
    offset = len(PNG_SIGNATURE)
    while offset < len(data):
        length = struct.unpack_from(">I", data, offset)[0]
        kind = data[offset + 4 : offset + 8]
        start = offset + 8
        payload = data[start : start + length]
        expected_crc = struct.unpack_from(">I", data, start + length)[0]
        actual_crc = zlib.crc32(kind + payload) & 0xFFFFFFFF
        if actual_crc != expected_crc:
            raise ValueError(f"PNG CRC mismatch in {kind.decode('ascii', 'replace')}")
        chunks.append((kind, payload))
        offset = start + length + 4
        if kind == b"IEND":
            break
    return chunks


def paeth(a: int, b: int, c: int) -> int:
    estimate = a + b - c
    da, db, dc = abs(estimate - a), abs(estimate - b), abs(estimate - c)
    if da <= db and da <= dc:
        return a
    return b if db <= dc else c


def decode_rgba(chunks: list[tuple[bytes, bytes]]) -> tuple[int, int, list[bytes]]:
    header = next(payload for kind, payload in chunks if kind == b"IHDR")
    width, height, bit_depth, color_type, compression, filtering, interlace = struct.unpack(">IIBBBBB", header)
    if (bit_depth, color_type, compression, filtering, interlace) != (8, 6, 0, 0, 0):
        raise ValueError("Only non-interlaced 8-bit RGBA PNG references are supported")
    compressed = b"".join(payload for kind, payload in chunks if kind == b"IDAT")
    packed = zlib.decompress(compressed)
    stride = width * 4
    if len(packed) != height * (stride + 1):
        raise ValueError("Unexpected decompressed PNG data size")

    rows: list[bytes] = []
    previous = bytes(stride)
    offset = 0
    for _ in range(height):
        filter_type = packed[offset]
        source = packed[offset + 1 : offset + stride + 1]
        offset += stride + 1
        row = bytearray(stride)
        for index, value in enumerate(source):
            left = row[index - 4] if index >= 4 else 0
            above = previous[index]
            upper_left = previous[index - 4] if index >= 4 else 0
            if filter_type == 0:
                predictor = 0
            elif filter_type == 1:
                predictor = left
            elif filter_type == 2:
                predictor = above
            elif filter_type == 3:
                predictor = (left + above) // 2
            elif filter_type == 4:
                predictor = paeth(left, above, upper_left)
            else:
                raise ValueError(f"Unsupported PNG filter {filter_type}")
            row[index] = (value + predictor) & 0xFF
        rows.append(bytes(row))
        previous = row
    return width, height, rows


def chunk(kind: bytes, payload: bytes) -> bytes:
    return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload) & 0xFFFFFFFF)


def crop_reference(reference: Path, output: Path) -> tuple[int, int, tuple[int, int, int, int]]:
    chunks = read_chunks(reference.read_bytes())
    width, height, rows = decode_rgba(chunks)
    left, top, right, bottom = width, height, -1, -1
    for y, row in enumerate(rows):
        for x in range(width):
            if row[x * 4 + 3]:
                left, top = min(left, x), min(top, y)
                right, bottom = max(right, x), max(bottom, y)
    if right < left or bottom < top:
        raise ValueError("Reference has no visible pixels")
    right += 1
    bottom += 1

    cropped_width, cropped_height = right - left, bottom - top
    filtered = bytearray()
    for row in rows[top:bottom]:
        filtered.append(0)
        filtered.extend(row[left * 4 : right * 4])
    header = struct.pack(">IIBBBBB", cropped_width, cropped_height, 8, 6, 0, 0, 0)
    encoded = PNG_SIGNATURE + chunk(b"IHDR", header) + chunk(b"IDAT", zlib.compress(filtered, 9)) + chunk(b"IEND", b"")
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(encoded)
    return cropped_width, cropped_height, (left, top, right, bottom)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reference", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    width, height, bounds = crop_reference(args.reference, args.output)
    print(f"Wrote {args.output}: crop {bounds}, {width}x{height} RGBA")


if __name__ == "__main__":
    main()
