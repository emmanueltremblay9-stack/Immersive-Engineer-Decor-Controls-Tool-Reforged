import hashlib
import json
from pathlib import Path
import tempfile
import unittest
import zipfile
from check_freezer_issue153_artifact import verify

class ArtifactScopeTest(unittest.TestCase):
    def test_scope_and_mutations(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp); jar=root/'test.jar'; baseline=root/'baseline.json'
            baseline.write_text(json.dumps(dict(jar_sha256='historical', entries={'Freezer.class':hashlib.sha256(b'original').hexdigest()})))
            for value, passes in [(b'original',True),(b'changed',False)]:
                with zipfile.ZipFile(jar,'w') as archive:
                    archive.writestr('Freezer.class',value); archive.writestr('UnrelatedManual.class',b'independent change')
                if passes: verify(jar,baseline)
                else:
                    with self.assertRaises(ValueError): verify(jar,baseline)
            with zipfile.ZipFile(jar,'w'): pass
            with self.assertRaises(KeyError): verify(jar,baseline)
