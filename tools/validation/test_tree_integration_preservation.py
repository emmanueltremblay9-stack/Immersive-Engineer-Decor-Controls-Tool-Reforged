"""Mutation checks for bounded Tree/Freezer/Barrel/Solar integration; no gameplay claims."""
import copy
import hashlib
import json
from pathlib import Path
import unittest
from unittest.mock import patch

from tree_integration_preservation import (
    BARREL_GUARD, COMBINED_FILES, COMBINED_LAYER, FREEZER_FILES, FREEZER_PREVIEW,
    HISTORICAL_FREEZER_PREVIEW, INTEGRATION_FILES, MACHINE_ENTITY, MACHINE_MENU,
    MILKING_FILES, TREE_ACCESSOR, TREE_FILES, assert_milking_integration_preserved,
    assert_menu_tail_preserved, assert_solar_baseline_preserved,
)


ROOT = Path(__file__).resolve().parents[2]
MENU = ROOT / 'src/main/java/com/oblixorprime/engineersdecorreforged/menu/MachineMenu.java'


class IntegratedPreservationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.menu = MENU.read_text()
        cls.manifest = json.loads((ROOT / 'docs/testing/solar-v2-reference/preserved-baseline-sha256.json').read_text())
        cls.files = {name: (ROOT / name).read_bytes() for name in set(cls.manifest['files']) | set(FREEZER_FILES)}
        cls.pins = json.loads((ROOT / 'docs/testing/milking-qualification/production-pins.json').read_text())
        cls.milking_hashes = {name: cls.pins['sha256'][name] for name in MILKING_FILES}

    def check_files(self, manifest=None, files=None, milking_current_hashes=None):
        contents = self.files if files is None else files
        with patch.object(Path, 'read_bytes', lambda path: contents[path.relative_to(ROOT).as_posix()]):
            assert_solar_baseline_preserved(
                ROOT,
                self.manifest if manifest is None else manifest,
                self.milking_hashes if milking_current_hashes is None else milking_current_hashes,
            )

    def check_milking(self, pins=None, files=None):
        contents = self.files if files is None else files
        with patch.object(Path, 'read_bytes', lambda path: contents[path.relative_to(ROOT).as_posix()]):
            assert_milking_integration_preserved(ROOT, self.pins if pins is None else pins)

    def test_combined_layer_is_required_by_both_guards(self):
        self.check_milking()
        for original, check in ((self.manifest, self.check_files), (self.pins, self.check_milking)):
            changed = copy.deepcopy(original)
            del changed[COMBINED_LAYER]
            with self.subTest(guard=check.__name__), self.assertRaisesRegex(AssertionError, 'Combined Milking/Freezer'):
                check(changed)

    def test_combined_provenance_and_path_set_cannot_change_or_disappear(self):
        for original, check in ((self.manifest, self.check_files), (self.pins, self.check_milking)):
            for key in ('main_commit', 'milking_commit', 'merge_base', 'scope', 'files'):
                for drop in (False, True):
                    changed = copy.deepcopy(original)
                    if drop:
                        del changed[COMBINED_LAYER][key]
                    else:
                        changed[COMBINED_LAYER][key] = 'changed'
                    with self.subTest(guard=check.__name__, key=key, drop=drop), self.assertRaises(AssertionError):
                        check(changed)
            for path in (*COMBINED_FILES, 'src/unrelated.java'):
                changed = copy.deepcopy(original)
                records = changed[COMBINED_LAYER]['files']
                if path in records:
                    del records[path]
                else:
                    records[path] = {'integrated_sha256': '0' * 64}
                with self.subTest(guard=check.__name__, path=path), self.assertRaises(AssertionError):
                    check(changed)

    def test_each_combined_record_field_is_independently_pinned(self):
        for original, check in ((self.manifest, self.check_files), (self.pins, self.check_milking)):
            for path in COMBINED_FILES:
                for key in ('main_sha256', 'milking_sha256', 'integrated_sha256', 'reason'):
                    for drop in (False, True):
                        changed = copy.deepcopy(original)
                        record = changed[COMBINED_LAYER]['files'][path]
                        if drop:
                            del record[key]
                        else:
                            record[key] = 'changed'
                        with self.subTest(guard=check.__name__, path=path, key=key, drop=drop), self.assertRaises(AssertionError):
                            check(changed)

    def test_all_milking_current_pins_remain_exact_and_required(self):
        for path in self.pins['sha256']:
            for drop in (False, True):
                pins = copy.deepcopy(self.pins)
                if drop:
                    del pins['sha256'][path]
                else:
                    pins['sha256'][path] = '0' * 64
                with self.subTest(path=path, drop=drop), self.assertRaises(AssertionError):
                    self.check_milking(pins)
        for path, record in COMBINED_FILES.items():
            for parent in ('main_sha256', 'milking_sha256'):
                pins = copy.deepcopy(self.pins)
                pins['sha256'][path] = record[parent]
                with self.subTest(path=path, parent=parent), self.assertRaises(AssertionError):
                    self.check_milking(pins)

    def test_milking_history_cannot_be_rewritten_or_dropped(self):
        for key in self.pins.keys() - {'sha256', COMBINED_LAYER}:
            for drop in (False, True):
                pins = copy.deepcopy(self.pins)
                if drop:
                    del pins[key]
                else:
                    pins[key] = 'changed'
                with self.subTest(key=key, drop=drop), self.assertRaisesRegex(AssertionError, 'Historical Milking'):
                    self.check_milking(pins)

    def test_freezer_historical_scope_and_reasons_are_preserved(self):
        manifest = copy.deepcopy(self.manifest)
        history = manifest['freezer_fluid_automation_integration']
        history['scope'] = 'Overwritten history'
        with self.assertRaisesRegex(AssertionError, 'Historical Freezer'):
            self.check_files(manifest)
        for path in FREEZER_FILES:
            manifest = copy.deepcopy(self.manifest)
            manifest['freezer_fluid_automation_integration']['files'][path]['reason'] = 'Changed history'
            with self.subTest(path=path), self.assertRaisesRegex(AssertionError, 'Historical Freezer'):
                self.check_files(manifest)

    def test_each_machine_logic_is_required_even_with_coordinated_rehash(self):
        mutations = (
            (MACHINE_MENU, '      if (kind == MachineKind.SMALL_MILKING_MACHINE) this.addDataSlots(this.milkingData);\n'),
            (MACHINE_MENU, FREEZER_PREVIEW),
            (MACHINE_ENTITY, '               machine.tickFreezer(powered);\n'),
            (MACHINE_ENTITY, '                  machine.tickMilkingMachine(powered);\n'),
            (MACHINE_ENTITY, '            selected = TfcMilkingCompat.inspect(serverLevel, box, selected.animalId());\n'),
        )
        for path, fragment in mutations:
            self.assertEqual(self.files[path].count(fragment.encode()), 1)
            files = dict(self.files)
            files[path] = files[path].replace(fragment.encode(), b'', 1)
            with self.subTest(path=path, fragment=fragment):
                with self.assertRaises(AssertionError):
                    self.check_files(files=files)
                with self.assertRaises(AssertionError):
                    self.check_milking(files=files)
                digest = hashlib.sha256(files[path]).hexdigest()
                manifest = copy.deepcopy(self.manifest)
                pins = copy.deepcopy(self.pins)
                manifest[COMBINED_LAYER]['files'][path]['integrated_sha256'] = digest
                pins[COMBINED_LAYER]['files'][path]['integrated_sha256'] = digest
                pins['sha256'][path] = digest
                current = {name: pins['sha256'][name] for name in MILKING_FILES}
                with self.assertRaises(AssertionError):
                    self.check_files(manifest=manifest, files=files, milking_current_hashes=current)
                with self.assertRaises(AssertionError):
                    self.check_milking(pins=pins, files=files)

    def test_screen_rehash_cannot_expand_milking_exception(self):
        path = next(name for name in MILKING_FILES if name != MACHINE_ENTITY)
        files = dict(self.files)
        files[path] += b'\nmutation'
        current = dict(self.milking_hashes)
        current[path] = hashlib.sha256(files[path]).hexdigest()
        with self.assertRaisesRegex(AssertionError, 'Milking current hashes changed'):
            self.check_files(files=files, milking_current_hashes=current)
        with self.assertRaisesRegex(AssertionError, 'Unexpected Milking current-hash allowlist'):
            self.check_files(milking_current_hashes={})

    def test_guards_do_not_rewrite_records_while_checking(self):
        manifest, pins = copy.deepcopy(self.manifest), copy.deepcopy(self.pins)
        self.check_files(manifest)
        self.check_milking(pins)
        self.assertEqual(manifest, self.manifest)
        self.assertEqual(pins, self.pins)

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


if __name__ == '__main__':
    unittest.main()
