"""Mutation checks for bounded Tree/Barrel/Solar integration; no gameplay claims."""
import copy
import hashlib
import json
from pathlib import Path
import unittest
from unittest.mock import patch

from tree_integration_preservation import (
    BARREL_GUARD, INTEGRATION_FILES, MILKING_FILES, TREE_ACCESSOR, TREE_FILES,
    assert_menu_tail_preserved, assert_solar_baseline_preserved,
)


ROOT = Path(__file__).resolve().parents[2]
MENU = ROOT / 'src/main/java/com/oblixorprime/engineersdecorreforged/menu/MachineMenu.java'


class IntegratedPreservationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.menu = MENU.read_text()
        cls.manifest = json.loads((ROOT / 'docs/testing/solar-v2-reference/preserved-baseline-sha256.json').read_text())
        cls.files = {name: (ROOT / name).read_bytes() for name in cls.manifest['files']}
        pins = json.loads((ROOT / 'docs/testing/milking-qualification/production-pins.json').read_text())
        cls.milking_hashes = {name: pins['sha256'][name] for name in MILKING_FILES}

    def check_files(self, manifest=None, files=None, milking_current_hashes=None):
        contents = self.files if files is None else files
        with patch.object(Path, 'read_bytes', lambda path: contents[path.relative_to(ROOT).as_posix()]):
            assert_solar_baseline_preserved(
                ROOT,
                self.manifest if manifest is None else manifest,
                self.milking_hashes if milking_current_hashes is None else milking_current_hashes,
            )

    def test_exact_integrated_menu_and_files_pass(self):
        assert_menu_tail_preserved(self.menu)
        self.check_files()

    def test_missing_changed_or_duplicate_accessor_is_rejected(self):
        for replacement in ('', TREE_ACCESSOR.replace('this.treeCutterV3ReferencePresentation', 'true'), TREE_ACCESSOR * 2):
            with self.subTest(replacement=replacement), self.assertRaises(AssertionError):
                assert_menu_tail_preserved(self.menu.replace(TREE_ACCESSOR, replacement))

    def test_unrelated_menu_change_is_rejected(self):
        with self.assertRaisesRegex(AssertionError, 'Menu transfer/data/validation changed'):
            assert_menu_tail_preserved(self.menu + '\n// unrelated change\n')

    def test_changed_boundary_is_rejected(self):
        with self.assertRaises(AssertionError):
            assert_menu_tail_preserved(self.menu.replace('   private static MenuType<?> resolveMenuType', '   private static MenuType<?> changed'))

    def test_each_preserved_file_mutation_is_rejected(self):
        for path in self.files:
            changed = dict(self.files)
            changed[path] += b'\nmutation'
            with self.subTest(path=path), self.assertRaises(AssertionError):
                self.check_files(files=changed)

    def test_milking_current_hash_override_is_exact_and_pinned(self):
        first = next(iter(MILKING_FILES))
        with self.assertRaisesRegex(AssertionError, 'Unexpected Milking current-hash allowlist'):
            self.check_files(milking_current_hashes={first: self.milking_hashes[first]})
        changed = dict(self.milking_hashes)
        changed[first] = '0' * 64
        with self.assertRaises(AssertionError):
            self.check_files(milking_current_hashes=changed)

    def test_historical_pins_cannot_be_rewritten_or_dropped(self):
        for path in self.files:
            for drop in (False, True):
                manifest = copy.deepcopy(self.manifest)
                if drop:
                    del manifest['files'][path]
                else:
                    manifest['files'][path] = '0' * 64
                with self.subTest(path=path, drop=drop), self.assertRaises(AssertionError):
                    self.check_files(manifest=manifest)

    def test_extra_or_missing_exception_is_rejected(self):
        for path in (BARREL_GUARD, *TREE_FILES, 'src/unrelated.java'):
            manifest = copy.deepcopy(self.manifest)
            overrides = manifest['tree_cutter_integration']['files']
            if path in overrides:
                del overrides[path]
            else:
                overrides[path] = {'integrated_sha256': '0' * 64}
            with self.subTest(path=path), self.assertRaises(AssertionError):
                self.check_files(manifest=manifest)

    def test_rehash_cannot_admit_changed_integration_files(self):
        for path in INTEGRATION_FILES:
            changed = dict(self.files)
            changed[path] += b'\nmutation'
            manifest = copy.deepcopy(self.manifest)
            manifest['tree_cutter_integration']['files'][path]['integrated_sha256'] = hashlib.sha256(changed[path]).hexdigest()
            with self.subTest(path=path), self.assertRaises(AssertionError):
                self.check_files(manifest=manifest, files=changed)

    def test_exception_must_identify_original_pin(self):
        for path in self.manifest['tree_cutter_integration']['files']:
            manifest = copy.deepcopy(self.manifest)
            manifest['tree_cutter_integration']['files'][path]['baseline_sha256'] = '0' * 64
            with self.subTest(path=path), self.assertRaises(AssertionError):
                self.check_files(manifest=manifest)

    def test_source_provenance_cannot_be_reassigned(self):
        for section, key in ((None, 'remote_baseline_commit'), (None, 'baseline_tree'),
                             ('tree_cutter_integration', 'main_commit'), ('tree_cutter_integration', 'tree_commit')):
            manifest = copy.deepcopy(self.manifest)
            (manifest if section is None else manifest[section])[key] = '0' * 40
            with self.subTest(key=key), self.assertRaises(AssertionError):
                self.check_files(manifest=manifest)


if __name__ == '__main__':
    unittest.main()
