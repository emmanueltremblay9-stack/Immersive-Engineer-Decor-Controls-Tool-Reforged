"""Separate raw-candidate contracts from the historical-only thermal projection."""
from pathlib import Path
import unittest

from thermal_single_stage_projection import (
    FILES, load_hunks, project_historical_files, reverse_hunks, sha256,
)

ROOT = Path(__file__).resolve().parents[2]
ENTITY = "src/main/java/com/oblixorprime/engineersdecorreforged/utility/MachineBlockEntity.java"
MENU = "src/main/java/com/oblixorprime/engineersdecorreforged/menu/MachineMenu.java"
EVENTS = "src/main/java/com/oblixorprime/engineersdecorreforged/client/ClientModEvents.java"
SCREEN = "src/main/java/com/oblixorprime/engineersdecorreforged/client/ThermalVerticalScreen.java"
GEOMETRY = "src/main/java/com/oblixorprime/engineersdecorreforged/utility/ThermalVerticalGeometry.java"
LAYOUT = "src/main/java/com/oblixorprime/engineersdecorreforged/utility/MachineLayout.java"
NATIVE_VALIDATOR = "src/main/java/com/oblixorprime/engineersdecorreforged/client/validation/NativeCorrectedMachineGuiValidation.java"


def read(path):
    return (ROOT / path).read_text(encoding="utf-8")


class ThermalProjectionTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.candidate = {path: (ROOT / path).read_bytes() for path in FILES}

    def test_exact_candidate_projects_to_the_pinned_main_bytes_without_mutation(self):
        before = dict(self.candidate)
        projected = project_historical_files(self.candidate)
        self.assertEqual(self.candidate, before)
        for path, record in FILES.items():
            self.assertEqual(sha256(self.candidate[path]), record["candidate_sha256"], path)
            self.assertEqual(sha256(projected[path]), record["historical_sha256"], path)
        self.assertEqual({path: len(hunks) for path, hunks in load_hunks().items()},
                         {path: record["hunks"] for path, record in FILES.items()})

    def test_a_mutation_inside_each_hunk_is_rejected(self):
        hunks_by_path = load_hunks()
        for path, source in self.candidate.items():
            hunk = hunks_by_path[path][0]
            changed_line = next(line for line in hunk.new_lines if line not in hunk.old_lines)
            lines = source.decode("utf-8").splitlines(keepends=True)
            position = hunk.new_start - 1 + hunk.new_lines.index(changed_line)
            lines[position] = "X" + lines[position][1:]
            with self.subTest(path=path), self.assertRaisesRegex(AssertionError, "hunk mismatch"):
                reverse_hunks(path, "".join(lines).encode("utf-8"), hunks_by_path[path])

    def test_a_mutation_outside_all_hunks_fails_the_historical_byte_comparison(self):
        hunks_by_path = load_hunks()
        for path, source in self.candidate.items():
            changed = source + b"// unauthorized byte outside all declared hunks\n"
            with self.subTest(path=path), self.assertRaisesRegex(AssertionError, "historical bytes changed outside declared hunks"):
                reverse_hunks(path, changed, hunks_by_path[path])

    def test_no_projection_path_is_a_resource_or_texture(self):
        self.assertEqual(set(FILES), {ENTITY, MENU, EVENTS})
        self.assertFalse(any("resources" in path or path.endswith(".png") for path in FILES))


class ThermalCandidateContractTests(unittest.TestCase):
    """These checks read the real, unprojected PR candidate."""

    def test_freezer_keeps_reserved_slot_and_39_slot_menu_wire(self):
        menu, layout, geometry, screen, native = (
            read(MENU), read(LAYOUT), read(GEOMETRY), read(SCREEN), read(NATIVE_VALIDATOR)
        )
        freezer_layout = layout.split("private static MachineLayout freezer()", 1)[1].split("\n   }", 1)[0]
        self.assertEqual(freezer_layout.count("new MachineLayout.MachineSlot("), 3)
        for index in (0, 1, 2):
            self.assertIn(f"MachineLayout.MachineSlot({index},", freezer_layout)
        self.assertIn("this.visibleMachineSlots = this.layout.machineSlots().size();", menu)
        self.assertIn("for (MachineLayout.MachineSlot slot : this.layout.machineSlots())", menu)
        self.assertIn("this.addSlot(this.createMachineSlot(presentationSlot));", menu)
        self.assertIn("for (int row = 0; row < 3; row++)", menu)
        self.assertIn("for (int column = 0; column < 9; column++)", menu)
        self.assertIn("menu.slots.size() != menu.visibleMachineSlots() + 36", native)
        self.assertEqual(3 + 27 + 9, 39)
        self.assertIn("public static final int VISIBLE_SLOTS = 38;", geometry)
        self.assertIn("if (menuIndex == 1 || menuIndex < 0 || menuIndex > 38)", geometry)
        self.assertIn("if (menuIndex == 0) return new Point(145, 50);", geometry)
        self.assertIn("if (menuIndex == 2) return new Point(145, 160);", geometry)
        self.assertIn("return !MachineMenu.this.freezerReferencePresentation\n                  && !MachineMenu.this.thermalVerticalPresentation;", menu)
        self.assertIn("recoveryButton.visible = menu.getSlot(1).hasItem();", screen)
        self.assertIn("slotClicked(menu.getSlot(1), 1, 0, ClickType.QUICK_MOVE);", screen)
        self.assertIn("menu.getSlot(1).mayPlace(new ItemStack(Items.STONE))", native)

    def test_freezer_bottom_extraction_is_output_two_and_empty_bucket_zero_only(self):
        entity = read(ENTITY)
        bottom = entity.split("private final IItemHandler freezerBottomItems", 1)[1].split("private ItemStack extractMineralOutput", 1)[0]
        self.assertIn("MachineBlockEntity.this.items.get(2).copy()", bottom)
        self.assertIn("MachineBlockEntity.this.items.get(0).is(Items.BUCKET)", bottom)
        self.assertIn("itemHandler.extractItem(2, amount, simulate)", bottom)
        self.assertIn("itemHandler.extractItem(0, amount, simulate)", bottom)
        self.assertIn("return side == Direction.UP ? new int[]{0} : side == Direction.DOWN ? new int[]{2, 0} : new int[0];", entity)
        self.assertIn("return side == Direction.DOWN && (slot == 2 || slot == 0 && stack.is(Items.BUCKET));", entity)
        self.assertIn("destination.getBlockPos().equals(this.worldPosition.below())", entity)
        self.assertIn("private final IItemHandler freezerTopItems", entity)
        self.assertIn("slot == 0 ? MachineBlockEntity.this.itemHandler.insertItem(0, stack, simulate)", entity)

    def test_tree_extraction_contract_and_registration_are_unchanged(self):
        entity, menu, events = read(ENTITY), read(MENU), read(EVENTS)
        self.assertEqual(entity.count("if (this.kind() == MachineKind.SMALL_TREE_CUTTER) return slot == 0;"), 2)
        self.assertEqual(menu.count("public boolean treeCutterV3ReferencePresentation()"), 1)
        self.assertIn("event.register((MenuType)ModMenus.SMALL_MINERAL_SMELTER.get(), ThermalVerticalScreen::new);", events)
        self.assertIn("event.register((MenuType)ModMenus.SMALL_FREEZER.get(), ThermalVerticalScreen::new);", events)
        self.assertIn("new MachineScreen.FluidBarrelScreen(menu, inventory, title)", events)
        self.assertIn("MachineScreen.SmallMilkingMachineScreen::new", events)
        self.assertIn("event.register((MenuType)ModMenus.SMALL_TREE_CUTTER.get(), TreeCutterV3ReferenceScreen::new);", events)
        self.assertNotIn("SmallMineralSmelterHybridScreen::new", events)
        self.assertNotIn("MachineScreen.SmallFreezerScreen::new", events)


if __name__ == "__main__":
    unittest.main()
