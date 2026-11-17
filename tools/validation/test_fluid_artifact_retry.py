"""Offline workflow contract: partial retries reuse exact producer artifacts."""
from pathlib import Path
import subprocess
import unittest

WORKFLOW = Path(__file__).resolve().parents[2] / '.github/workflows/fluid-gestures-native.yml'

class FluidArtifactRetryTest(unittest.TestCase):
    def setUp(self):
        self.text = WORKFLOW.read_text()
        self.producer, self.consumer = self.text.split('  native-packaged-default:', 1)

    def test_exact_id_is_exported_and_consumed(self):
        self.assertIn('artifact_id: ${{ steps.share.outputs.artifact-id }}', self.producer)
        self.assertIn('id: share', self.producer)
        self.assertIn('artifact-ids: ${{ needs.ordinary-artifact.outputs.artifact_id }}', self.consumer)
        download = self.consumer.split('      - name: Download the shared ordinary artifact', 1)[1].split('      - name:', 1)[0]
        self.assertNotIn('github.run_attempt', download)
        self.assertNotIn('          name:', download)
        self.assertNotIn('pattern:', download)

    def test_missing_id_fails_closed_before_download(self):
        guard = self.consumer.split('        run: |\n', 1)[1].split('      - name: Download', 1)[0].strip()
        for value in ['', '0', 'abc', '12,13', '12 13', '12\n13']:
            result = subprocess.run(['bash', '-c', guard], env={'PRODUCER_ARTIFACT_ID': value}, capture_output=True)
            self.assertNotEqual(result.returncode, 0, value)
        self.assertEqual(subprocess.run(['bash', '-c', guard], env={'PRODUCER_ARTIFACT_ID': '14180362487'}, capture_output=True).returncode, 0)

    def test_evidence_artifacts_are_attempt_unique(self):
        for text in [self.producer, self.consumer]:
            for block in text.split('uses: actions/upload-artifact@')[1:]:
                name = block.split('          name: ', 1)[1].splitlines()[0]
                self.assertIn('${{ github.run_id }}', name)
                self.assertIn('${{ github.run_attempt }}', name)

    def test_source_and_digest_guards_remain(self):
        for guard in ["identity['source_sha'] == source", "identity['sha256'] == digest", "identity['artifact'] == 'normal.jar'", "(target/'normal.jar.sha256').read_text() == digest+'  normal.jar\\n'"]:
            self.assertIn(guard, self.consumer)
        self.assertIn('needs: ordinary-artifact', self.consumer)

if __name__ == '__main__':
    unittest.main()
