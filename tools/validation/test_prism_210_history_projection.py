"""Exact immutable history projection and fail-closed mutation tests for #210."""
import json
import unittest

from prism_210_history_projection import (
    BASE_COMMIT, HEAD_COMMIT, RECORD, ROOT,
    project_current_candidate, sha256,
)


class Prism210HistoryProjectionTests(unittest.TestCase):
    def test_candidate_commit_range_and_every_reversible_layer_are_exact(self):
        data = json.loads(RECORD.read_bytes())
        self.assertEqual(data["base_commit"], BASE_COMMIT)
        self.assertEqual(data["head_commit"], HEAD_COMMIT)
        self.assertGreaterEqual(len(data["files"]), 10)
        for path, record in data["files"].items():
            current = (ROOT / path).read_bytes()
            self.assertEqual(sha256(current), record["states"][-1]["sha256"], path)
            historical = project_current_candidate(path, current, strict=True)
            self.assertEqual(sha256(historical), record["states"][0]["sha256"], path)
            self.assertEqual(sha256(current), record["states"][-1]["sha256"], "projection mutated source " + path)

    def test_any_unrecorded_candidate_mutation_is_rejected(self):
        data = json.loads(RECORD.read_bytes())
        for path, record in data["files"].items():
            current = (ROOT / path).read_bytes()
            changed = current + b"// outside exact #210 hunk\n"
            with self.subTest(path=path), self.assertRaisesRegex(AssertionError, "Unrecognized #210 candidate bytes"):
                project_current_candidate(path, changed, strict=True)

    def test_projection_starts_from_the_candidate_and_returns_exact_historical_bytes(self):
        data = json.loads(RECORD.read_bytes())
        for path, record in data["files"].items():
            candidate = (ROOT / path).read_bytes()
            history = project_current_candidate(path, candidate, strict=True)
            with self.subTest(path=path):
                self.assertEqual(sha256(history), record["states"][0]["sha256"])
                self.assertEqual(sha256(candidate), record["states"][-1]["sha256"])


if __name__ == "__main__":
    unittest.main()
