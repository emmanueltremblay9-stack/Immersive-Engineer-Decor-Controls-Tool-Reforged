"""Fail-closed checks for synthetic canal capture evidence (pixels require review)."""
import copy
import struct
import tempfile
import unittest
from pathlib import Path
from client_gameplay import validate_canal_proof
from client_mixin_smoke import SmokeFailure


class CanalProofTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.directory = Path(self.temp.name)
        images = self.directory / "canal-screenshots"
        images.mkdir()
        self.proof = {"suite": "native-water-canal-diagnostic-v2", "nonce": "current",
                      "status": "captured", "cases": []}
        for name in ("stairs-wet", "stairs-dry-control", "catwalk-wet", "raised-wet",
                     "stairs-railed-wet", "stairs-railed-reversed-wet",
                     "stairs-flow-adjacent-natural", "stairs-flow-adjacent-stored"):
            wet = name not in {"stairs-dry-control", "stairs-flow-adjacent-natural"}
            case = {"case": name, "source": wet, "upper_horizontal_quads": int(wet), "upper_surface_quads": int(name!="stairs-dry-control")}
            case.update(adjacent_water=True, adjacent_source=False, adjacent_fluid_height=0.6,
                        cell_fluid_height=0.5,fluid_state="minecraft:flowing_water")
            for angle in ("oblique", "overhead"):
                filename = f"{name}-{angle}.png"
                case[f"{angle}_screenshot"] = filename
                # Deliberate header fixture: this gate never claims to validate pixels.
                (images / filename).write_bytes(b"\x89PNG\r\n\x1a\n" + b"\0\0\0\rIHDR"
                                               + struct.pack(">II", 1280, 720) + bytes(1024))
            self.proof["cases"].append(case)

    def test_complete_capture_matrix(self):
        self.assertEqual(validate_canal_proof(self.proof, "current", self.directory), self.proof)

    def test_rejects_stale_incomplete_duplicate_and_wrong_source(self):
        for mutation in ("stale", "missing", "duplicate", "source", "surface", "dry_surface"):
            proof = copy.deepcopy(self.proof)
            if mutation == "stale": proof["nonce"] = "old"
            elif mutation == "missing": proof["cases"].pop()
            elif mutation == "duplicate": proof["cases"][-1] = proof["cases"][0]
            elif mutation == "source": proof["cases"][0]["source"] = False
            elif mutation == "surface": proof["cases"][0]["upper_surface_quads"] = 0
            else: proof["cases"][1]["upper_surface_quads"] = 1
            with self.subTest(mutation=mutation), self.assertRaises(SmokeFailure):
                validate_canal_proof(proof, "current", self.directory)

    def test_rejects_wrong_file_identity_and_invalid_image(self):
        proof = copy.deepcopy(self.proof)
        proof["cases"][0]["oblique_screenshot"] = "../other.png"
        with self.assertRaises(SmokeFailure):
            validate_canal_proof(proof, "current", self.directory)
        image = self.directory / "canal-screenshots" / "stairs-wet-oblique.png"
        for data in (b"not an image", b"\x89PNG\r\n\x1a\n" + b"\0\0\0\rIHDR"
                     + struct.pack(">II", 10, 10) + bytes(1024)):
            image.write_bytes(data)
            with self.assertRaises(SmokeFailure):
                validate_canal_proof(self.proof, "current", self.directory)

    def test_requires_actual_flowing_adjacent_water(self):
        for field, value in (("adjacent_water", False), ("adjacent_source", True),
                             ("adjacent_fluid_height", 0), ("adjacent_fluid_height", 1)):
            proof = copy.deepcopy(self.proof)
            proof["cases"][-1][field] = value
            with self.subTest(field=field, value=value), self.assertRaises(SmokeFailure):
                validate_canal_proof(proof, "current", self.directory)
    def test_sloped_top_is_valid_without_horizontal_quads(self):
        self.proof["cases"][-1]["upper_horizontal_quads"] = 0
        self.proof["cases"][-1]["upper_surface_quads"] = 2
        self.assertEqual(validate_canal_proof(self.proof, "current", self.directory), self.proof)
    def test_side_vertices_cannot_replace_an_upper_surface(self):
        self.proof["cases"][-1].update(vertices=16, upper_horizontal_quads=0, upper_surface_quads=0)
        with self.assertRaises(SmokeFailure):
            validate_canal_proof(self.proof, "current", self.directory)
