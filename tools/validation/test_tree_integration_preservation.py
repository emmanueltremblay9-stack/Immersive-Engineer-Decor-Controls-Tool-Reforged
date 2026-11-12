"""Mutation checks for bounded Tree/Freezer/Barrel/Solar integration; no gameplay claims."""
import copy
import hashlib
import json
from pathlib import Path
import unittest
from unittest.mock import patch

from breaker_integration_preservation import BREAKER_FILES, BREAKER_MENU_ADDITIONS
from tree_integration_preservation import (
    BARREL_GUARD, FREEZER_FILES, FREEZER_PREVIEW, HISTORICAL_FREEZER_PREVIEW,
    INTEGRATION_FILES, MACHINE_ENTITY, TREE_ACCESSOR, TREE_FILES,
    assert_menu_tail_preserved, assert_solar_baseline_preserved,
)


ROOT = Path(__file__).resolve().parents[2]
MENU = ROOT / 'src/main/java/com/oblixorprime/engineersdecorreforged/menu/MachineMenu.java'


class IntegratedPreservationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.menu = MENU.read_text()
        cls.manifest = json.loads((ROOT / 'docs/testing/solar-v2-reference/preserved-baseline-sha256.json').read_text())
        cls.files = {name: (ROOT / name).read_bytes() for name in set(cls.manifest['files']) | set(FREEZER_FILES) | set(BREAKER_FILES)}

    def check_files(self, manifest=None, files=None):
        contents = self.files if files is None else files
        with patch.object(Path, 'read_bytes', lambda path: contents[path.relative_to(ROOT).as_posix()]):
            assert_solar_baseline_preserved(ROOT, self.manifest if manifest is None else manifest)

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

    def test_historical_pins_cannot_be_rewritten_or_dropped(self):
        for path in self.manifest['files']:
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

    def test_missing_changed_duplicate_or_historical_freezer_preview_is_rejected(self):
        for replacement in ('', FREEZER_PREVIEW.replace('>= 1000', '>= 1'),
                            FREEZER_PREVIEW * 2, HISTORICAL_FREEZER_PREVIEW):
            with self.subTest(replacement=replacement), self.assertRaises(AssertionError):
                assert_menu_tail_preserved(self.menu.replace(FREEZER_PREVIEW, replacement))

    def test_each_extraction_branch_is_required_in_both_container_methods(self):
        source = self.files[MACHINE_ENTITY].decode()
        for method in ('canTakeItemThroughFace', 'canTakeItem(Container'):
            start = source.index('public boolean ' + method)
            for branch in (
                '      if (this.kind() == MachineKind.SMALL_TREE_CUTTER) return slot == 0;\n',
                '      if (this.kind() == MachineKind.SMALL_FREEZER) return this.canAutomationExtract(slot);\n',
            ):
                for replacement in ('', branch.replace('return ', 'return true || ')):
                    tail = source[start:]
                    self.assertIn(branch, tail)
                    changed = dict(self.files)
                    changed[MACHINE_ENTITY] = (source[:start] + tail.replace(branch, replacement, 1)).encode()
                    with self.subTest(method=method, branch=branch, replacement=replacement), self.assertRaises(AssertionError):
                        self.check_files(files=changed)

    def test_freezer_layer_path_set_is_exact(self):
        for path in (*FREEZER_FILES, 'src/unrelated.java'):
            manifest = copy.deepcopy(self.manifest)
            overrides = manifest['freezer_fluid_automation_integration']['files']
            if path in overrides:
                del overrides[path]
            else:
                overrides[path] = {'integrated_sha256': '0' * 64}
            with self.subTest(path=path), self.assertRaises(AssertionError):
                self.check_files(manifest=manifest)

    def test_freezer_source_provenance_is_independently_pinned(self):
        for key in ('main_commit', 'freezer_commit'):
            manifest = copy.deepcopy(self.manifest)
            manifest['freezer_fluid_automation_integration'][key] = '0' * 40
            with self.subTest(key=key), self.assertRaises(AssertionError):
                self.check_files(manifest=manifest)
        for path in FREEZER_FILES:
            for key in ('main_sha256', 'freezer_sha256', 'integrated_sha256'):
                manifest = copy.deepcopy(self.manifest)
                manifest['freezer_fluid_automation_integration']['files'][path][key] = '0' * 64
                with self.subTest(path=path, key=key), self.assertRaises(AssertionError):
                    self.check_files(manifest=manifest)

    def test_rehash_cannot_admit_changed_freezer_integration_files(self):
        for path in FREEZER_FILES:
            changed = dict(self.files)
            changed[path] += b'\nmutation'
            manifest = copy.deepcopy(self.manifest)
            manifest['freezer_fluid_automation_integration']['files'][path]['integrated_sha256'] = hashlib.sha256(changed[path]).hexdigest()
            with self.subTest(path=path), self.assertRaises(AssertionError):
                self.check_files(manifest=manifest, files=changed)

    def test_tree_history_cannot_be_rewritten(self):
        manifest = copy.deepcopy(self.manifest)
        manifest['tree_cutter_integration']['scope'] = 'Overwritten integration history'
        with self.assertRaisesRegex(AssertionError, 'Historical Tree integration changed'):
            self.check_files(manifest=manifest)



    def test_each_breaker_menu_addition_is_exactly_once(self):
        for addition in BREAKER_MENU_ADDITIONS:
            for replacement in ('', addition + '// mutation\n', addition * 2):
                with self.subTest(addition=addition[:60], replacement=replacement[:60]), self.assertRaises(AssertionError):
                    assert_menu_tail_preserved(self.menu.replace(addition, replacement, 1))

    def test_breaker_layer_path_set_is_exact(self):
        for path in (*BREAKER_FILES, 'src/unrelated.java'):
            manifest = copy.deepcopy(self.manifest)
            overrides = manifest['block_breaker_lubricant_integration']['files']
            if path in overrides:
                del overrides[path]
            else:
                overrides[path] = {'integrated_sha256': '0' * 64}
            with self.subTest(path=path), self.assertRaises(AssertionError):
                self.check_files(manifest=manifest)

    def test_breaker_metadata_and_pins_cannot_be_reassigned(self):
        for key in ('main_commit', 'source_patch_sha256', 'scope'):
            manifest = copy.deepcopy(self.manifest)
            manifest['block_breaker_lubricant_integration'][key] = 'changed'
            with self.subTest(key=key), self.assertRaises(AssertionError):
                self.check_files(manifest=manifest)
        for path in BREAKER_FILES:
            for key in ('main_sha256', 'integrated_sha256'):
                manifest = copy.deepcopy(self.manifest)
                manifest['block_breaker_lubricant_integration']['files'][path][key] = '0' * 64
                with self.subTest(path=path, key=key), self.assertRaises(AssertionError):
                    self.check_files(manifest=manifest)

    def test_rehash_cannot_admit_changed_breaker_files(self):
        for path in BREAKER_FILES:
            changed = dict(self.files)
            changed[path] += b'\nmutation'
            manifest = copy.deepcopy(self.manifest)
            manifest['block_breaker_lubricant_integration']['files'][path]['integrated_sha256'] = hashlib.sha256(changed[path]).hexdigest()
            with self.subTest(path=path), self.assertRaises(AssertionError):
                self.check_files(manifest=manifest, files=changed)

    def test_freezer_history_cannot_be_rewritten(self):
        manifest = copy.deepcopy(self.manifest)
        manifest['freezer_fluid_automation_integration']['scope'] = 'Overwritten integration history'
        with self.assertRaisesRegex(AssertionError, 'Historical Freezer integration changed'):
            self.check_files(manifest=manifest)

    def test_entire_breaker_layer_is_required(self):
        manifest = copy.deepcopy(self.manifest)
        del manifest['block_breaker_lubricant_integration']
        with self.assertRaises((AssertionError, KeyError)):
            self.check_files(manifest=manifest)

    def test_breaker_menu_constant_mutation_is_rejected_by_full_file_pin(self):
        path = 'src/main/java/com/oblixorprime/engineersdecorreforged/menu/MachineMenu.java'
        changed = dict(self.files)
        changed[path] = changed[path].replace(b'BLOCK_BREAKER_FLUID_PRESENT = 1;', b'BLOCK_BREAKER_FLUID_PRESENT = 8;')
        self.assertNotEqual(changed[path], self.files[path])
        with self.assertRaises(AssertionError):
            self.check_files(files=changed)


if __name__ == '__main__':
    unittest.main()
