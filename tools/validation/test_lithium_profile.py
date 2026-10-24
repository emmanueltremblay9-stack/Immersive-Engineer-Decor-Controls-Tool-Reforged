import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from client_mixin_smoke import SmokeFailure
from lithium_profile import prepare_lithium_dependency, validate_lithium_runtime


class LithiumProfileTests(unittest.TestCase):
    def test_runtime_proof_requires_version_enabled_bfs_and_session(self):
        marker = "IEDCTR_LITHIUM_COMPAT version=0.15.4+mc1.21.1 bfs=true nonce=abc"
        validate_lithium_runtime(marker, "abc")
        for logs in ("", marker.replace("0.15.4", "0.15.3"), marker.replace("bfs=true", "bfs=false")):
            with self.assertRaises(SmokeFailure):
                validate_lithium_runtime(logs, "abc")
        with self.assertRaises(SmokeFailure):
            validate_lithium_runtime(marker, "another-session")

    def test_corrupt_cached_dependency_fails_without_network_or_overwrite(self):
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            path = directory / "lithium.jar"
            path.write_bytes(b"not the pinned official jar")
            with patch("lithium_profile.urllib.request.urlopen") as network:
                with self.assertRaises(SmokeFailure):
                    prepare_lithium_dependency(directory)
                network.assert_not_called()
            self.assertEqual(path.read_bytes(), b"not the pinned official jar")
            self.assertFalse((directory / "identity.json").exists())
