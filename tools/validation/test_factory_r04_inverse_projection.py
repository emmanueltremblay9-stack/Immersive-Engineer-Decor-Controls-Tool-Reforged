"""Negative regression tests for the exact, limited Factory R04 projection."""
import unittest
from pathlib import Path
from factory_r04_inverse_projection import (
    SCREEN_PATH, GAME_TEST_PATH, RECIPE_PATH, MAIN_SCREEN_BLOB, MAIN_GAME_TEST_BLOB,
    FactoryProjectionError, git_blob_sha, project_screen_to_main, project_preserved_bytes,
    assert_exact_dropper_game_test,
)
from factory_tooltip_integration import assert_projection, BASE_SHA256, CURRENT_SHA256

ROOT = Path(__file__).resolve().parents[2]
SCREEN = ROOT / SCREEN_PATH
GAME_TEST = ROOT / GAME_TEST_PATH


class FactoryInverseProjectionTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = SCREEN.read_text(encoding='utf-8')
        cls.game_test = GAME_TEST.read_text(encoding='utf-8')

    def test_exact_inverse_and_retained_historical_factory_tooltip_pins(self):
        projected = project_screen_to_main(self.source)
        self.assertEqual(git_blob_sha(projected), MAIN_SCREEN_BLOB)
        assert_projection(self.source)
        self.assertEqual(BASE_SHA256, '2b0f8cc8bfeac9f421b6948833e4d88b7c993c5c633b6f26feebc6f96f2a0e6f')
        self.assertEqual(CURRENT_SHA256, '92fc98c4a92e236c6f8237109d85eed485beda0abeb2d0060ba495622397fabf')

    def test_only_named_source_path_is_projected(self):
        source_bytes = self.source.encode()
        self.assertEqual(project_preserved_bytes(SCREEN_PATH, source_bytes).decode(), project_screen_to_main(self.source))
        self.assertEqual(project_preserved_bytes('unrelated.txt', source_bytes), source_bytes)

    def test_milking_or_freezer_mutations_rejected(self):
        for needle, replacement in (
            ('RedesignedMachineArtwork.milking(', 'RedesignedMachineArtwork.differentMilking('),
            ('MachineKind.SMALL_FREEZER', 'MachineKind.SMALL_SOLAR_PANEL'),
        ):
            self.assertIn(needle, self.source)
            with self.subTest(needle=needle), self.assertRaises(FactoryProjectionError):
                project_screen_to_main(self.source.replace(needle, replacement, 1))

    def test_factory_control_and_selection_mutations_rejected(self):
        for needle, replacement in (
            ('y0 + 8 + slotIndex / 6 * 18', 'y0 + 8 + slotIndex / 6 * 17'),
            ('this.font.split(controlTooltip, wrapWidth)', 'controlTooltip'),
            ('this.sendAction(10,', 'this.sendAction(11,'),
        ):
            self.assertIn(needle, self.source)
            with self.subTest(needle=needle), self.assertRaises(FactoryProjectionError):
                project_screen_to_main(self.source.replace(needle, replacement, 1))

    def test_added_method_and_recipe_tampering_rejected(self):
        with self.assertRaises(FactoryProjectionError):
            project_screen_to_main(self.source.replace('private void drawRetainedDropper(', 'private void drawUnapprovedDropper(', 1))
        recipe = RECIPE_PATH.read_bytes()
        with self.assertRaises(FactoryProjectionError):
            project_screen_to_main(self.source, recipe_bytes=recipe.replace(b'Factory', b'FActory', 1))

    def test_dropper_game_test_exact_single_line_projection(self):
        original = assert_exact_dropper_game_test(self.game_test)
        self.assertEqual(git_blob_sha(original), MAIN_GAME_TEST_BLOB)
        self.assertIn('retained_dropper/background', self.game_test)

    def test_game_test_foreign_mutation_rejected(self):
        self.assertIn('ModBlocks.FACTORY_HOPPER', self.game_test)
        with self.assertRaises(FactoryProjectionError):
            assert_exact_dropper_game_test(self.game_test.replace('ModBlocks.FACTORY_HOPPER','ModBlocks.FACTORY_PLACER',1))
        with self.assertRaises(FactoryProjectionError):
            assert_exact_dropper_game_test(self.game_test.replace('retained_dropper/background','factory_dropper_gui',1))


if __name__ == '__main__':
    unittest.main()
