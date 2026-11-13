"""Pin the merged 1.1.67 identity without running Minecraft or authorizing publication."""
import hashlib
from pathlib import Path
import unittest

import qualify_1_1_65 as q


ROOT = Path(__file__).resolve().parent


class MergedVersion167CandidateTests(unittest.TestCase):
    def test_version167_identity_is_pinned_to_its_main_push_build(self):
        self.assertEqual(q.load_spec(ROOT / "iedctr-1.1.67-main-b9fc2b44.json"), {
            "schemaVersion": 1,
            "repository": "emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged",
            "sourceCommit": "b9fc2b44d41eab7cb0513775b27ac7c231aa6e20",
            "sourceTree": "fbb0ea029eaee8e52e179d55d79de9531ee0abb2",
            "buildRunId": 43089042054,
            "buildRunAttempt": 1,
            "modId": "immersive_engineer_decor_controls_tool_reforged",
            "version": "1.1.67-reconstructed",
            "ciReportedJarSha256": "757c9f894aad532e53214a83a69642d41742ac16aec232d541f8a8ace9dc03c5",
        })

    def test_prior_candidate_records_are_unchanged(self):
        expected = {
            "iedctr-1.1.66.json": "284eedd8d2d292caceecc74912c028fe0a6cc4e6f8e0b4b302481e6fca0a0fdb",
            "iedctr-1.1.66-main-890958ff.json": "9006e0417b43afdc8448b2168795ba16da14a3a80cd336822e16493c78c7a65e",
            "iedctr-1.1.66-main-509913bd.json": "f0defc3641c31c7b9ead816cea11cfd1a771a53d72109911f1840e16d9626808",
        }
        for name, digest in expected.items():
            with self.subTest(candidate=name):
                self.assertEqual(hashlib.sha256((ROOT / name).read_bytes()).hexdigest(), digest)


if __name__ == "__main__":
    unittest.main()
