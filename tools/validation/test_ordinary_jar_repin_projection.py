"""An observed JAR may replace only the nine obsolete hash literals in seven gates."""
import subprocess
import unittest
from unittest.mock import patch

import prism_210_history_projection as projection


class OrdinaryJarRepinProjectionTests(unittest.TestCase):
    BASE = "f3189eb153a4c1ac31f9072be49a2c8872c0f1e6"
    OLD = "c7da0d9e5cfb26d47100483b5a1955a7e7e2784c7225fe0913a2760d9aa2c3fe"
    NEW = "2734a258fd3b8c5d037ee82e34c2a38df8529e1055b5720581e7e1f059106ccc"
    PATHS = {
        ".github/workflows/milking-packaged-native.yml": 2,
        ".github/workflows/smelter-packaged-native.yml": 2,
        ".github/workflows/tfc-packaged-native.yml": 1,
        "gradle/tfc-packaged-validation.gradle": 1,
        "tools/validation/tfc_packaged_recovery_native.py": 1,
        "tools/validation/tfc_packaged_mooshroom_native.py": 1,
        "tools/validation/test_tfc_packaged_driver.py": 1,
    }

    def test_only_nine_observed_jar_hash_literals_change(self):
        record = projection._ordinary_jar_repin_record()
        latest = projection._ordinary_jar_followup_repin_record()
        v5c = projection._ordinary_jar_v5c_repin_record()
        self.assertEqual(record["base_commit"], self.BASE)
        self.assertEqual(set(record["files"]), set(self.PATHS))
        self.assertEqual(record["artifact"], {
            "source_sha": self.BASE, "sha256": self.NEW, "size_bytes": 86514465,
            "previous_sha256": self.OLD, "run_id": 45901991310, "artifact_id": 15155764054,
        })
        self.assertEqual(latest["base_commit"], "0c9299718701e9374b102617631f3b319d224bf3")
        self.assertEqual(latest["artifact"]["sha256"], "b7131f56ccc56b291421da9a0f462a36b03b478599aeeff9e0e60c373c59c01e")
        self.assertEqual(latest["artifact"]["previous_sha256"], self.NEW)
        self.assertEqual(latest["artifact"]["build_run_id"], 46095637365)
        self.assertEqual(set(latest["files"]), set(self.PATHS))
        self.assertEqual(v5c["base_commit"], "372ad8d434977423182515a3bff2fbb2d7ae8f24")
        self.assertEqual(v5c["artifact"]["source_sha"], v5c["base_commit"])
        self.assertEqual(v5c["artifact"]["sha256"], "78f7ff78bb81f04bc8b4e5ea55193b774f060dc72227f1222c4a7660ad3e26ab")
        self.assertEqual(v5c["artifact"]["previous_sha256"], latest["artifact"]["sha256"])
        self.assertEqual(v5c["artifact"]["size_bytes"], 122050890)
        self.assertEqual(set(v5c["files"]), set(self.PATHS))
        self.assertEqual(sum(self.PATHS.values()), 9)
        for path, count in self.PATHS.items():
            with self.subTest(path=path):
                parent = subprocess.check_output(
                    ["git", "show", self.BASE + ":" + path], cwd=projection.ROOT
                )
                current = (projection.ROOT / path).read_bytes()
                v5c_layer = v5c["files"][path]
                self.assertEqual(v5c_layer["replacement_count"], count)
                self.assertEqual(v5c_layer["previous_source_commit"], v5c["base_commit"])
                previous_candidate = subprocess.check_output(
                    ["git", "show", v5c["base_commit"] + ":" + path], cwd=projection.ROOT
                )
                self.assertEqual(projection._reverse_layer(path, current, v5c_layer), previous_candidate)
                self.assertEqual(current, previous_candidate.replace(
                    v5c["artifact"]["previous_sha256"].encode(), v5c["artifact"]["sha256"].encode()
                ))
                self.assertEqual(projection.expected_current_sha256(path), v5c_layer["new_sha256"])
                latest_layer = latest["files"][path]
                self.assertEqual(latest_layer["previous_source_commit"], latest["base_commit"])
                self.assertEqual(latest_layer["replacement_count"], count)
                self.assertEqual(previous_candidate.count(latest["artifact"]["sha256"].encode()), count)
                prior_current = projection._reverse_layer(path, previous_candidate, latest_layer)
                self.assertEqual(projection.sha256(prior_current), latest_layer["old_sha256"])
                self.assertEqual(prior_current.count(self.NEW.encode()), count)
                layer = record["files"][path]
                self.assertEqual(layer["previous_source_commit"], self.BASE)
                self.assertEqual(layer["replacement_count"], count)
                self.assertEqual(parent.count(self.OLD.encode()), count)
                self.assertEqual(prior_current, parent.replace(self.OLD.encode(), self.NEW.encode()))
                self.assertEqual(projection.sha256(prior_current), layer["new_sha256"])
                self.assertEqual(projection.sha256(parent), layer["old_sha256"])
                self.assertEqual(projection._reverse_layer(path, prior_current, layer), parent)
                self.assertEqual(
                    projection.project_current_candidate(path, current, strict=True),
                    projection.project_current_candidate(path, parent),
                )

    def test_wrong_digest_or_any_other_gate_edit_is_rejected(self):
        latest_sha = projection._ordinary_jar_v5c_repin_record()["artifact"]["sha256"].encode()
        for path in self.PATHS:
            current = (projection.ROOT / path).read_bytes()
            for mutated in (current.replace(latest_sha, b"0" * 64, 1),
                            current + b"unreviewed gate change\n"):
                with self.subTest(path=path), self.assertRaisesRegex(AssertionError, "Unrecognized #210"):
                    projection.project_current_candidate(path, mutated, strict=True)
        with patch.object(projection, "ORDINARY_JAR_REPIN_RECORD_BYTES",
                          projection.ORDINARY_JAR_REPIN_RECORD_BYTES + b" "):
            with self.assertRaisesRegex(AssertionError, "ordinary JAR repin projection record changed"):
                projection._ordinary_jar_repin_record()

    def test_previous_eight_receipts_and_barrel_workflow_are_unchanged(self):
        baseline_paths = subprocess.check_output(
            ["git", "ls-tree", "-r", "--name-only", self.BASE, "tools/validation"],
            cwd=projection.ROOT, text=True,
        ).splitlines()
        paths = [path for path in baseline_paths
                 if path.startswith("tools/validation/prism_210") and path.endswith("projection.json")]
        self.assertEqual(len(paths), 8)
        paths.append(".github/workflows/barrel-v5-packaged-native.yml")
        for path in paths:
            with self.subTest(path=path):
                parent = subprocess.check_output(
                    ["git", "show", self.BASE + ":" + path], cwd=projection.ROOT
                )
                self.assertEqual((projection.ROOT / path).read_bytes(), parent)


if __name__ == "__main__":
    unittest.main()
