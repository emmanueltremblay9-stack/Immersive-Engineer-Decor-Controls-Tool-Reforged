#!/usr/bin/env python3
"""Resource/source identity and DECLARED CPU-nearest model, never a Minecraft capture.
No network, dependencies, image redrawing, source mutations or native-JSON regeneration.
"""
import argparse
import hashlib
import json
import math
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parents[2]
ASSET = ROOT / 'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/freezer_reference_v5/background.png'
EXPECTED_BYTES = '68c1dc11e7d0e920addebca18100892861a70c5d68db5547b8014235acee3d46'
EXPECTED_RGBA = '61377093d58be84df2852af2e020ae1df56c36190f9a4536d5e65c4260de0d0c'


def sha(data):
    return hashlib.sha256(data).hexdigest()


def read_png(path):
    raw = path.read_bytes()
    assert raw[:8] == b'\x89PNG\r\n\x1a\n', 'not a PNG'
    offset, compressed, chunks = 8, bytearray(), []
    while offset < len(raw):
        length, kind = struct.unpack_from('>I4s', raw, offset)
        data = raw[offset + 8: offset + 8 + length]
        crc, = struct.unpack_from('>I', raw, offset + 8 + length)
        assert zlib.crc32(kind + data) & 0xffffffff == crc, 'PNG CRC mismatch'
        chunks.append(kind.decode('ascii'))
        if kind == b'IHDR':
            width, height, depth, color, compression, filtering, interlace = struct.unpack('>IIBBBBB', data)
            assert (depth, compression, filtering, interlace) == (8, 0, 0, 0)
            assert color in (2, 6), 'only retained RGB/RGBA8 sources are accepted'
        if kind == b'IDAT':
            compressed.extend(data)
        offset += length + 12
        if kind == b'IEND':
            break
    channels = 3 if color == 2 else 4
    stride = width * channels
    decoded = zlib.decompress(compressed)
    assert len(decoded) == height * (stride + 1)
    rgba, previous = bytearray(), bytearray(stride)
    for y in range(height):
        start = y * (stride + 1)
        filt, row = decoded[start], bytearray(decoded[start + 1:start + 1 + stride])
        assert 0 <= filt <= 4
        for x in range(stride):
            a = row[x - channels] if x >= channels else 0
            b = previous[x]
            c = previous[x - channels] if x >= channels else 0
            if filt == 1:
                predictor = a
            elif filt == 2:
                predictor = b
            elif filt == 3:
                predictor = (a + b) // 2
            elif filt == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                predictor = a if pa <= pb and pa <= pc else b if pb <= pc else c
            else:
                predictor = 0
            row[x] = (row[x] + predictor) & 255
        if channels == 4:
            rgba.extend(row)
        else:
            for x in range(0, stride, 3):
                rgba.extend(row[x:x + 3])
                rgba.append(255)
        previous = row
    return raw, bytes(rgba), {'width': width, 'height': height, 'mode': 'RGB' if channels == 3 else 'RGBA',
                              'chunks': chunks, 'has_color_profile': any(x in chunks for x in ('iCCP', 'sRGB', 'gAMA', 'cHRM'))}


def different_pixels(a, b):
    assert len(a) == len(b)
    return sum(a[i:i+4] != b[i:i+4] for i in range(0, len(a), 4))


def nearest_panel(rgba, target):
    # Pixel centers of a square integer-sized destination, direct UV sampling from1254.
    # A diagnostic CPU model only, not a substitute for framebuffer evidence.
    out = bytearray()
    for y in range(target):
        sy = min(1253, math.floor((y + 0.5) * 1254 / target))
        for x in range(target):
            sx = min(1253, math.floor((x + 0.5) * 1254 / target))
            i = (sy * 1254 + sx) * 4
            out.extend(rgba[i:i + 4])
    return bytes(out)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, help='optional independently retained original PNG')
    parser.add_argument('--proof-render', type=Path, help='optional EXISTING native toolkit render; never regenerated')
    parser.add_argument('--report', type=Path, default=ROOT / 'build/reports/freezer-reference-resource.json')
    args = parser.parse_args()
    raw, rgba, meta = read_png(ASSET)
    assert sha(raw) == EXPECTED_BYTES, 'runtime PNG differs from retained exact bytes'
    assert sha(rgba) == EXPECTED_RGBA, 'runtime decoded RGBA differs from retained exact pixels'
    assert (meta['width'], meta['height'], meta['mode']) == (1254, 1254, 'RGB')
    assert json.loads(Path(str(ASSET) + '.mcmeta').read_text()) == {'texture': {'blur': False, 'clamp': True}}
    source_rgba = rgba
    report = {'status': 'PASS_RESOURCE_IDENTITY_NOT_NATIVE_RUNTIME', 'source_filename': 'freezer-mixed-rp-v5-preview.png',
              'runtime_resource': str(ASSET.relative_to(ROOT)), 'bytes': len(raw), 'byte_sha256': sha(raw),
              'rgba_sha256': sha(rgba), 'metadata': meta, 'native_runtime_capture': 'NOT_RUN',
              'sampling': 'CPU point sampling at integer destination pixel centers, direct1254 UVs, nearest/clamp. Not hardware framebuffer/color-pipeline proof.'}
    if args.source:
        original, source_rgba, original_meta = read_png(args.source)
        assert raw == original, 'runtime PNG file bytes differ from actual original source'
        assert meta == original_meta
        report['original_source_byte_identity'] = True
        report['original_source_rgba_different_pixels'] = different_pixels(rgba, source_rgba)
    if args.proof_render:
        proof_raw, proof_rgba, proof_meta = read_png(args.proof_render)
        assert (proof_meta['width'], proof_meta['height']) == (1254, 1254)
        diff = different_pixels(rgba, proof_rgba)
        assert diff == 0
        report['existing_official_render'] = {'byte_sha256': sha(proof_raw), 'same_file_bytes': raw == proof_raw,
                                            'rgba_different_pixels': diff}
    manifest = json.loads((ROOT / 'docs/testing/freezer-reference-exact/geometry-manifest.json').read_text())
    report['conservative_v5_interior_pixel_checks'] = []
    for anchor in manifest['anchors']:
        x, y, width, height = anchor['source_rect_xywh']
        maximum = max(max(rgba[(sy * 1254 + sx) * 4:(sy * 1254 + sx) * 4 + 3])
                      for sy in range(y, y + height) for sx in range(x, x + width))
        assert maximum <= 60, 'Conservative interior includes bright frame pixels'
        report['conservative_v5_interior_pixel_checks'].append({'menu_index': anchor['menu_index'], 'max_rgb_channel': maximum})
    report['cpu_sampling_checks'] = []
    for side in (314, 228, 168, 108):
        sampled_runtime, sampled_source = nearest_panel(rgba, side), nearest_panel(source_rgba, side)
        diff = different_pixels(sampled_runtime, sampled_source)
        assert diff == 0
        report['cpu_sampling_checks'].append({'display_square': side, 'sampled_rgba_sha256': sha(sampled_runtime),
                                             'different_pixels': diff, 'item_display_pixels': 16 * side / 314})
    # Legacy crop remains available as historical evidence and is explicitly not theV5 source.
    old = ROOT / 'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/small_water_freezer/background.png'
    old_raw, old_rgba, old_meta = read_png(old)
    assert sha(old_raw) != EXPECTED_BYTES
    report['untouched_production_background'] = {'byte_sha256': sha(old_raw), 'metadata': old_meta,
                                               'is_exact_v5_reference': False}
    crop = ROOT / 'docs/gui-art/small-water-freezer/small-water-freezer.reference-crop-422x443.png'
    crop_raw, crop_rgba, crop_meta = read_png(crop)
    assert (crop_meta['width'], crop_meta['height']) == (422, 443)
    actual_crop = b''.join(old_rgba[(y * 941 + 259) * 4:(y * 941 + 681) * 4] for y in range(614, 1057))
    assert actual_crop == crop_rgba, 'historical crop does not match its actual source rectangle'
    rendered_crop = ROOT / 'docs/gui-art/small-water-freezer/small-water-freezer.toolkit-render-crop-422x443.png'
    rendered_crop_raw, rendered_crop_rgba, rendered_crop_meta = read_png(rendered_crop)
    assert rendered_crop_rgba == crop_rgba
    report['historical_crop_identity'] = {
        'rectangle': [259, 614, 681, 1057], 'size': [422, 443],
        'reference_crop_byte_sha256': sha(crop_raw), 'toolkit_crop_byte_sha256': sha(rendered_crop_raw),
        'source_crop_rgba_different_pixels': 0, 'toolkit_crop_rgba_different_pixels': 0,
        'is_exact_v5_reference': False}
    runtime_jsons = list(ASSET.parent.glob('*.json'))
    assert runtime_jsons == [], 'Do not bundle the large native proof JSON in runtime assets'
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps({'status': report['status'], 'original_byte_sha256': sha(raw),
                      'decoded_rgba_differences': 0, 'cpu_samples': len(report['cpu_sampling_checks']),
                      'native_runtime_capture': 'NOT_RUN', 'report': str(args.report)}, indent=2))


if __name__ == '__main__':
    main()
