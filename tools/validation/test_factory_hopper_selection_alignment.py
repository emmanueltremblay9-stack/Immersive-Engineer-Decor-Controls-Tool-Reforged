"""Active V3 slot/outline/pointer geometry source checks (not native captures)."""
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
GEOMETRY = ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/utility/FactoryStudyGeometry.java"
MENU = ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/menu/MachineMenu.java"
SCREEN = ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/client/FactoryStudyScreen.java"
EVENTS = ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/client/ClientModEvents.java"
KIND = "MachineKind.FACTORY_HOPPER"


def hopper_anchors(geometry_source):
    body = geometry_source.split("FACTORY_HOPPER_ANCHORS = List.of(", 1)[1].split("\n   );", 1)[0]
    rows = [tuple(map(int, row)) for row in re.findall(
        r"new SlotAnchor\((\d+),\s*(\d+),\s*(\d+),\s*(\d+),\s*(\d+),\s*(\d+),\s*(\d+)\)", body)]
    if len(rows) != 54:
        raise AssertionError(f"Expected 54 active Hopper anchors, got {len(rows)}")
    return rows


def assert_v3_alignment(geometry_source, menu_source, screen_source, events_source):
    anchors = hopper_anchors(geometry_source)
    if [row[0] for row in anchors] != list(range(54)):
        raise AssertionError("Hopper slot IDs must remain contiguous 0..53")
    if len({(row[5], row[6]) for row in anchors}) != 54:
        raise AssertionError("Hopper anchors must be unique")
    machine = menu_source.split("if (factoryStudyPresentation)", 1)[1].split("\n      }", 1)[0]
    if "FactoryStudyGeometry.anchor(kind, this.slots.size())" not in machine:
        raise AssertionError("Machine slot hitboxes no longer use the measured artwork anchors")
    if "FactoryStudyGeometry.anchor(kind, this.visibleMachineSlots + row * 9 + column)" not in menu_source:
        raise AssertionError("Player inventory slot hitboxes no longer use V3 anchors")
    if "FactoryStudyGeometry.anchor(kind, this.visibleMachineSlots + 27 + column)" not in menu_source:
        raise AssertionError("Hotbar hitboxes no longer use V3 anchors")
    if "FactoryStudyGeometry.anchor(kind, cursor)" not in screen_source:
        raise AssertionError("V3 outline no longer resolves from the same slot anchors")
    if "graphics.renderOutline(slot.x() - 1, slot.y() - 1, 18, 18" not in screen_source:
        raise AssertionError("Selection outline must preserve the one-pixel border around the actual 16x16 slot")
    if "FactoryStudyGeometry.fit(width, height)" not in screen_source:
        raise AssertionError("V3 screen fit transform missing")
    if "view.renderPointerX(mouseX)" not in screen_source or "view.renderPointerY(mouseY)" not in screen_source:
        raise AssertionError("Mouse pointer is not transformed with the GUI hitboxes")
    if "FactoryStudyScreen::new" not in events_source:
        raise AssertionError("V3 screen is not registered as the active GUI")
    return anchors


class HopperSelectionAlignment(unittest.TestCase):
    def setUp(self):
        self.geometry = GEOMETRY.read_text(encoding="utf-8")
        self.menu = MENU.read_text(encoding="utf-8")
        self.screen = SCREEN.read_text(encoding="utf-8")
        self.events = EVENTS.read_text(encoding="utf-8")

    def test_all_54_machine_inventory_and_hotbar_hitboxes_share_v3_art_anchors(self):
        anchors = assert_v3_alignment(self.geometry, self.menu, self.screen, self.events)
        self.assertEqual(len(anchors[:18]), 18)
        self.assertEqual(len(anchors[18:45]), 27)
        self.assertEqual(len(anchors[45:]), 9)
        # The three machine rows are measured in the artwork coordinate plane,
        # and each full row has distinct 16x16 logical hitboxes.
        self.assertEqual([anchors[i][6] for i in (0, 6, 12)], [68, 93, 118])
        self.assertEqual([anchors[i][5] for i in (0, 1, 2, 3, 4, 5)], [34, 57, 81, 105, 128, 152])

    def test_old_machine_screen_pitch_is_not_the_active_v3_contract(self):
        self.assertNotIn("drawFactoryHopperWidgets", self.screen)
        self.assertIn("FACTORY_HOPPER_ANCHORS", self.geometry)
        self.assertIn("FactoryStudyScreen::new", self.events)

    def test_selection_or_pointer_transform_mutation_is_rejected(self):
        mutations = [
            self.screen.replace("slot.x() - 1", "slot.x() - 2", 1),
            self.screen.replace("view.renderPointerX(mouseX)", "mouseX", 1),
            self.menu.replace("FactoryStudyGeometry.anchor(kind, this.slots.size())", "new MachineLayout.MachineSlot(0,0,0,\"bad\")", 1),
        ]
        for mutated in mutations:
            with self.subTest(mutated=mutated[:50]), self.assertRaises(AssertionError):
                assert_v3_alignment(self.geometry, self.menu, mutated if mutated != mutations[2] else self.screen,
                                    self.events) if mutated != mutations[2] else assert_v3_alignment(
                                        self.geometry, mutated, self.screen, self.events)


if __name__ == "__main__":
    unittest.main()
