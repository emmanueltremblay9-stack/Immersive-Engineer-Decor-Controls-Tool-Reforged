"""V8b exact-source static gate. Additive to the unmodified historical V7 suite.

No owner-originals, Pillow, NumPy, image generation or runtime claims.
The frozen V7 PNGs are read from immutable commit cca76b0 and checked by SHA.
The outside-mask hashes were recorded from the SHA-pinned V8 local candidate.
Run only on a clean checkout that includes the two exact V8b PNGs.
"""
from __future__ import annotations

import hashlib
import io
import subprocess
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools" / "validation"))
from factory_placer_v2_reference_native import decode_png  # pylint: disable=wrong-import-position
import factory_owner_20261119 as owner_guard  # pylint: disable=wrong-import-position
import build_slot_relief as art  # pylint: disable=wrong-import-position

FROZEN_COMMIT = "cca76b0019d9a241e1f92461dccfccba728e3ec1"
PIXEL_SCALE = 6  # source texels per logical GUI unit, NOT Minecraft GUI Scale
ASSETS = "src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/factory_steel_v2"
FOOTPRINT_SOURCE_SHA256 = "8d22a29df59ab2a9ee9eca7aa612c495d38b35de82f03a24e5852a5c7bfb16cc"
EXPECTED = {
    "hopper": {
        "size": (1152, 1092),
        "slots": 54,
        "sha_v7": "da41853c9b1c01582900bdc5c1dbbe5d240d8dcf48923c2e010b73f7522e6f0e",
        "sha_v8b": "deb156c121171ae199488080879219ad41c3758326290746c6c40a68f460fc17",
        "outside_v8_sha": "bdde9026c540f9d7f126952c997f1e1737832ffef20705b70d4ae3b25c62e17d",
        "inside_v7_sha": "1eaaa26476f9ef9d6886fec0d5762354344f21e15c191bd47c6349a9ec51951c",
        "widgets": ((139,29,38,7),(138,42,39,7),(140,55,37,7),
                    (140,64,11,11),(152,64,11,11),(166,64,11,11),(156,38,3,3)),
    },
    "dropper": {
        "size": (1368, 1500),
        "slots": 51,
        "sha_v7": "363a10f20398be9efdb45ee48501e64c1c86ed9c94292c0064bd4c8bfea72813",
        "sha_v8b": "01ae08557fd2f7fd66e4573284527c8ca39a557201157b9a46e324f87cab6d3c",
        "outside_v8_sha": "4d307a7e70b1e0ebce73009a705dbaba5efddbd2dbdfc77c4aeb8cf66135bc6e",
        "inside_v7_sha": "7d53f805a8f239ec15f83146c19de4fef49a856a1b262940c3b7f90585064f9b",
        "widgets": ((152,31,9,31),(169,32,30,29),(155,65,45,9),(155,77,45,9),
                    (142,89,20,20),(165,89,20,20),(188,89,20,20),(126,96,5,5),
                    (202,79,4,4),(37,105,4,4),(70,105,4,4),(103,105,4,4)),
    },
}

class MemoryPNG(io.BytesIO):
    def read_bytes(self):
        return self.getvalue()


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def v7_bytes(kind):
    owner_guard.ensure_base_object(anchor=FROZEN_COMMIT)
    rel = f"{ASSETS}/{kind}.png"
    return subprocess.check_output(["git", "show", FROZEN_COMMIT + ":" + rel], cwd=ROOT)


def guard_rects(kind, width, height):
    config = EXPECTED[kind]
    slots = [(x, y, 16, 16) for x, y, _ in art.footprint(kind)]
    if len(slots) != config["slots"]:
        raise AssertionError("Registered footprint slot count changed")
    rects = slots + list(config["widgets"])
    protected = bytearray(width * height)
    for x, y, w, h in rects:
        x0, y0, x1, y1 = x*PIXEL_SCALE, y*PIXEL_SCALE, (x+w)*PIXEL_SCALE, (y+h)*PIXEL_SCALE
        if not (0 <= x0 < x1 <= width and 0 <= y0 < y1 <= height):
            raise AssertionError("Rectangle exceeds atlas dimensions")
        for row in range(y0, y1):
            a, b = row * width + x0, row * width + x1
            protected[a:b] = b"\x01" * (b-a)
    return protected, slots


def split_sha(rgba, mask):
    """Hash RGBA in row-major mask order, without any third-party package."""
    assert len(rgba) == len(mask) * 4
    inside, outside = hashlib.sha256(), hashlib.sha256()
    # Coalesce adjacent same-mask pixels to avoid millions of tiny hash updates.
    i = 0
    while i < len(mask):
        mark = mask[i]
        j = i + 1
        while j < len(mask) and mask[j] == mark:
            j += 1
        (inside if mark else outside).update(rgba[4*i:4*j])
        i = j
    return inside.hexdigest(), outside.hexdigest()


def inspect(kind, *, local_path=None):
    spec = EXPECTED[kind]
    art_path = local_path or ROOT / ASSETS / (kind + ".png")
    candidate = art_path.read_bytes()
    if sha(candidate) != spec["sha_v8b"]:
        raise AssertionError("Wrong V8b PNG SHA256: " + kind)
    original = v7_bytes(kind)
    if sha(original) != spec["sha_v7"]:
        raise AssertionError("Frozen V7 Git evidence differs: " + kind)
    width, height, new_rgba = decode_png(MemoryPNG(candidate))
    ow, oh, old_rgba = decode_png(MemoryPNG(original))
    if (width, height) != spec["size"] or (ow, oh) != (width, height):
        raise AssertionError("V8b dimensions differ")
    protected, slots = guard_rects(kind, width, height)
    for idx, (x, y, w, h) in enumerate(slots):
        for row in range(y*PIXEL_SCALE, (y+h)*PIXEL_SCALE):
            start = (row*width + x*PIXEL_SCALE) * 4
            end = start + w*PIXEL_SCALE*4
            if old_rgba[start:end] != new_rgba[start:end]:
                raise AssertionError(f"Slot footprint differs: {kind} #{idx}")
    old_in, _ = split_sha(old_rgba, protected)
    new_in, new_out = split_sha(new_rgba, protected)
    if old_in != spec["inside_v7_sha"] or new_in != old_in:
        raise AssertionError("V7 slot/control protected RGBA differs: " + kind)
    if new_out != spec["outside_v8_sha"]:
        raise AssertionError("V8 nonprotected pixels changed: " + kind)
    if old_rgba[3::4] != new_rgba[3::4]:
        raise AssertionError("V7 alpha changed: " + kind)
    return {"kind": kind, "sha256": sha(candidate), "slots": len(slots),
            "widgets": len(spec["widgets"]), "outside_v8_sha": new_out}


class V8bQualification(unittest.TestCase):
    def test_registered_geometry_and_pinned_tool(self):
        path = ROOT / "tools/approved_gui_20261119/build_slot_relief.py"
        self.assertEqual(sha(path.read_bytes()), FOOTPRINT_SOURCE_SHA256)
        self.assertEqual((len(art.footprint("hopper")), len(art.footprint("dropper"))), (54, 51))
        # First/last and nonuniform Dropper anchors are independently pinned.
        self.assertEqual([p[:2] for p in art.footprint("hopper")][:3], [(19,25),(37,25),(55,25)])
        self.assertEqual([p[:2] for p in art.footprint("hopper")][-9:], [(16+18*c,145) for c in range(9)])
        self.assertEqual([p[:2] for p in art.footprint("dropper")][:6], [(21,38),(40,38),(59,38),(78,38),(97,38),(115,38)])
        self.assertEqual([p[:2] for p in art.footprint("dropper")][-9:], [(31,197),(50,197),(69,197),(87,197),(106,197),(125,197),(143,197),(162,197),(180,197)])

    def test_exact_binary_and_complete_protected_pixels(self):
        for kind in EXPECTED:
            with self.subTest(kind=kind):
                self.assertEqual(inspect(kind)["slots"], EXPECTED[kind]["slots"])

    def test_reject_wrong_candidate_and_metadata_tampering(self):
        for kind in EXPECTED:
            with self.subTest(kind=kind):
                with self.assertRaisesRegex(AssertionError, "Wrong V8b"):
                    inspect(kind, local_path=ROOT / ASSETS / "owner_control_well.png")
        with self.subTest("mutated_expected_v8"):
            original = EXPECTED["hopper"]["outside_v8_sha"]
            try:
                EXPECTED["hopper"]["outside_v8_sha"] = "0"*64
                with self.assertRaisesRegex(AssertionError, "nonprotected"):
                    inspect("hopper")
            finally:
                EXPECTED["hopper"]["outside_v8_sha"] = original


    def test_mutated_png_rejected_without_touching_checked_in_assets(self):
        import tempfile
        for kind in EXPECTED:
            original = (ROOT / ASSETS / (kind + ".png")).read_bytes()
            with tempfile.TemporaryDirectory() as td:
                for offset in (0, len(original) // 2, len(original) - 1):
                    changed = bytearray(original)
                    changed[offset] ^= 1
                    path = Path(td) / (kind + "-damaged.png")
                    path.write_bytes(changed)
                    with self.subTest(machine=kind, offset=offset), self.assertRaisesRegex(
                            AssertionError, "Wrong V8b PNG SHA256"):
                        inspect(kind, local_path=path)


if __name__ == "__main__":
    unittest.main()
