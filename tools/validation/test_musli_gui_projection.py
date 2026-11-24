"""Fail-closed source checks for the exact one-line Musli client registration."""
import hashlib
from pathlib import Path
import unittest
from musli_gui_projection import PATH, ADDED, BASE_SHA256, CURRENT_SHA256, current_hash, project_history

ROOT = Path(__file__).resolve().parents[2]


class MusliGuiProjectionTests(unittest.TestCase):
    def test_exact_registration_reconstructs_historical_client_bytes(self):
        current = (ROOT / PATH).read_bytes()
        self.assertEqual(hashlib.sha256(current).hexdigest(), CURRENT_SHA256)
        self.assertEqual(hashlib.sha256(project_history(PATH, current)).hexdigest(), BASE_SHA256)
        self.assertEqual(current_hash(PATH, BASE_SHA256), CURRENT_SHA256)

    def test_missing_duplicate_and_mutated_registration_rejected(self):
        source = (ROOT / PATH).read_bytes()
        historic = source.replace(ADDED, b"")
        self.assertEqual(hashlib.sha256(historic).hexdigest(), BASE_SHA256)
        self.assertNotEqual(hashlib.sha256(historic).hexdigest(), CURRENT_SHA256)
        for mutated in (
            source.replace(ADDED, ADDED + ADDED),
            source.replace(b"ModMenus.MUSLI_BAR_PRESS", b"ModMenus.SMALL_FREEZER"),
            source + b"// mutation\n",
        ):
            with self.subTest(mutated_sha=hashlib.sha256(mutated).hexdigest()), self.assertRaises(AssertionError):
                project_history(PATH, mutated)

    def test_non_musli_paths_and_foreign_baseline(self):
        self.assertEqual(project_history("src/other.java", b"bytes"), b"bytes")
        self.assertEqual(current_hash("src/other.java", "historic"), "historic")
        with self.assertRaises(AssertionError):
            current_hash(PATH, "0" * 64)
