"""Fail-closed checks for the additive PR164/current-main integration record."""
import copy
import hashlib
import json
from pathlib import Path
import unittest
from unittest.mock import patch
from freezer_v5_reconciliation import ACTIVATION_PATH, ACTIVATION_SHA256, FILES, LAYER, assert_freezer_v5_reconciliation, current_hash

ROOT = Path(__file__).resolve().parents[2]
MANIFESTS = (
    'docs/testing/solar-v2-reference/preserved-baseline-sha256.json',
    'docs/testing/milking-qualification/production-pins.json',
)

class FreezerV5ReconciliationTests(unittest.TestCase):
    def setUp(self):
        self.manifests = [json.loads((ROOT / name).read_text()) for name in MANIFESTS]
        self.bytes = {name: (ROOT / name).read_bytes() for name in FILES}

    def check(self, manifest, content=None):
        content = self.bytes if content is None else content
        with patch.object(Path, 'read_bytes', lambda path: content[path.relative_to(ROOT).as_posix()]):
            assert_freezer_v5_reconciliation(ROOT, manifest)

    def test_both_guards_use_identical_record_and_exact_files(self):
        self.assertEqual(self.manifests[0][LAYER], self.manifests[1][LAYER])
        for manifest in self.manifests:
            self.check(manifest)
        for path, hashes in FILES.items():
            self.assertEqual(ACTIVATION_SHA256 if path == ACTIVATION_PATH else hashes['integrated_sha256'], current_hash(path, hashes['main_sha256']))
            with self.assertRaises(AssertionError):
                current_hash(path, '0' * 64)

    def test_activation_record_cannot_be_missing_or_rewritten(self):
        for original in self.manifests:
            for key in (None, *original['freezer_v5_normal_activation']):
                changed=copy.deepcopy(original)
                if key is None: del changed['freezer_v5_normal_activation']
                else: changed['freezer_v5_normal_activation'][key]='tampered'
                with self.subTest(key=key),self.assertRaises(AssertionError):self.check(changed)

    def test_record_cannot_be_missing_reassigned_or_expanded(self):
        for original in self.manifests:
            for key in (None, *original[LAYER]):
                changed = copy.deepcopy(original)
                if key is None:
                    del changed[LAYER]
                else:
                    changed[LAYER][key] = 'changed'
                with self.subTest(key=key), self.assertRaises(AssertionError):
                    self.check(changed)
            for path in (*FILES, 'src/unrelated.java'):
                changed = copy.deepcopy(original)
                records = changed[LAYER]['files']
                if path in records:
                    del records[path]
                else:
                    records[path] = {'integrated_sha256': '0' * 64}
                with self.subTest(path=path), self.assertRaises(AssertionError):
                    self.check(changed)

    def test_all_current_bytes_required_even_with_coordinated_rehash(self):
        for path in FILES:
            content = dict(self.bytes)
            content[path] += b'\nmutation'
            for original in self.manifests:
                with self.subTest(path=path), self.assertRaises(AssertionError):
                    self.check(original, content)
                changed = copy.deepcopy(original)
                changed[LAYER]['files'][path]['integrated_sha256'] = hashlib.sha256(content[path]).hexdigest()
                with self.assertRaises(AssertionError):
                    self.check(changed, content)

    def test_each_parent_hash_is_immutable(self):
        for original in self.manifests:
            for path in FILES:
                for key in FILES[path]:
                    changed = copy.deepcopy(original)
                    changed[LAYER]['files'][path][key] = '0' * 64
                    with self.subTest(path=path, key=key), self.assertRaises(AssertionError):
                        self.check(changed)

    def test_menu_tail_requires_exact_freezer_accessor_and_reserved_slot(self):
        from tree_integration_preservation import assert_menu_tail_preserved, V5_ACCESSOR, V5_RESERVED_SLOT
        source = (ROOT / 'src/main/java/com/oblixorprime/engineersdecorreforged/menu/MachineMenu.java').read_text()
        assert_menu_tail_preserved(source)
        for fragment in (V5_ACCESSOR, V5_RESERVED_SLOT):
            for replacement in ('', fragment * 2, fragment.replace('freezerReferencePresentation', 'true')):
                with self.subTest(fragment=fragment, replacement=replacement), self.assertRaises(AssertionError):
                    assert_menu_tail_preserved(source.replace(fragment, replacement))

if __name__ == '__main__':
    unittest.main()
