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
        self.proof = {"suite": "native-water-canal-diagnostic-v1", "nonce": "current",
                      "status": "captured", "cases": []}
        for name in ("stairs-wet", "stairs-dry-control", "catwalk-wet", "raised-wet"):
            wet = name != "stairs-dry-control"
            case = {"case": name, "source": wet, "upper_horizontal_quads": int(wet)}
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
            elif mutation == "surface": proof["cases"][0]["upper_horizontal_quads"] = 0
            else: proof["cases"][1]["upper_horizontal_quads"] = 1
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
