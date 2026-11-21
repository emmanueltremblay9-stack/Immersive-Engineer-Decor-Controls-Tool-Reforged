"""Structural regression guard, not a native runtime verdict."""
from pathlib import Path
import unittest

class CurrentJarBindingTests(unittest.TestCase):
    def test_workflow_binds_own_source_and_jar_before_runtime(self):
        root=Path(__file__).resolve().parents[2]
        s=(root/'.github/workflows/modernui-native-diagnostic.yml').read_text()
        self.assertIn('test "$(git rev-parse HEAD)" = "$EXPECTED_SOURCE"',s)
        self.assertIn('sha256sum "$jar"',s)
        self.assertIn("--expected-jar-sha256 '${{ steps.jar.outputs.sha }}'",s)
        self.assertLess(s.index('bash gradlew jar'),s.index('python tools/validation/modernui_native_diagnostic.py'))
        self.assertIn('build/modernui-candidate-identity/',s)
        self.assertIn('build/libs/*.jar',s)
        self.assertNotRegex(s,r'--expected-jar-sha256 [0-9a-f]{64}')
