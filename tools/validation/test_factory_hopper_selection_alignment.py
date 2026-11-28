"""V5-Corrigee slot/outline source checks (native captures are validated separately)."""
import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
GEOMETRY = ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/utility/FactoryStudyGeometry.java"
RUNTIME = ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/utility/Gui11V5CRuntimeGeometry.java"
MENU = ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/menu/MachineMenu.java"
SCREEN = ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/client/FactoryStudyScreen.java"
EVENTS = ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/client/ClientModEvents.java"
MATRIX = ROOT / "docs/testing/gui11-v5c-runtime-map.json"


def hopper_anchors(runtime_source):
    match = re.search(r'case "01" -> java\.util\.Arrays\.asList\((.*?)\);', runtime_source, re.S)
    if not match:
        raise AssertionError("Missing V5-Corrigee Hopper runtime anchor array")
    return [tuple(map(int, pair)) for pair in re.findall(r"new Point\((\d+),\s*(\d+)\)", match.group(1))]


def assert_v3_alignment(geometry_source, menu_source, screen_source, events_source, runtime_source=None, map_source=None):
    """Keep the historical import name while validating the currently selected V5-Corrigee map."""
    runtime_source = runtime_source if runtime_source is not None else RUNTIME.read_text(encoding="utf-8")
    map_source = map_source if map_source is not None else MATRIX.read_text(encoding="utf-8")
    actual = hopper_anchors(runtime_source)
    contract = json.loads(map_source)["surfaces"]["01"]["menu_slots"]
    if actual != [tuple(point) for point in contract]:
        raise AssertionError("V5-Corrigee Hopper runtime anchors differ from the measured contract")
    if len(actual) != 54 or len(set(actual)) != 54:
        raise AssertionError("Hopper needs 54 unique runtime anchors")
    if "mapped.add(mappedAnchor(kind, i, point))" not in geometry_source:
        raise AssertionError("Vanilla item hitboxes no longer scale from V5-Corrigee canvas coordinates")
    if "FactoryStudyGeometry.anchor(kind, this.slots.size())" not in menu_source:
        raise AssertionError("Machine hitboxes no longer use the V5-Corrigee anchor map")
    if "FactoryStudyGeometry.anchor(kind, this.visibleMachineSlots + row * 9 + column)" not in menu_source:
        raise AssertionError("Player inventory hitboxes no longer use the V5-Corrigee anchor map")
    if "FactoryStudyGeometry.anchor(kind, this.visibleMachineSlots + 27 + column)" not in menu_source:
        raise AssertionError("Hotbar hitboxes no longer use the V5-Corrigee anchor map")
    if "FactoryStudyGeometry.anchor(kind, cursor)" not in screen_source:
        raise AssertionError("Selection outline no longer resolves from the same slot anchors")
    if "graphics.renderOutline(slot.x() - 1, slot.y() - 1, 18, 18" not in screen_source:
        raise AssertionError("Selection outline must keep its one-pixel border around the 16x16 hitbox")
    if "FactoryStudyGeometry.fit(kind, width, height)" not in screen_source:
        raise AssertionError("V5-Corrigee canvas fit transform missing")
    if "view.renderPointerX(mouseX)" not in screen_source or "view.renderPointerY(mouseY)" not in screen_source:
        raise AssertionError("Mouse pointer is not transformed with the V5-Corrigee GUI hitboxes")
    if "FactoryStudyScreen::new" not in events_source:
        raise AssertionError("Factory Study screen is not registered as active GUI")
    return actual


class HopperSelectionAlignment(unittest.TestCase):
    def setUp(self):
        self.geometry = GEOMETRY.read_text(encoding="utf-8")
        self.menu = MENU.read_text(encoding="utf-8")
        self.screen = SCREEN.read_text(encoding="utf-8")
        self.events = EVENTS.read_text(encoding="utf-8")
        self.runtime = RUNTIME.read_text(encoding="utf-8")
        self.contract = json.loads(MATRIX.read_text(encoding="utf-8"))["surfaces"]["01"]["menu_slots"]

    def test_all_54_machine_inventory_and_hotbar_hitboxes_share_v5c_art_anchors(self):
        anchors = assert_v3_alignment(self.geometry, self.menu, self.screen, self.events, self.runtime)
        self.assertEqual(len(anchors[:18]), 18)
        self.assertEqual(len(anchors[18:45]), 27)
        self.assertEqual(len(anchors[45:]), 9)
        self.assertEqual([anchors[i][1] for i in (0, 6, 12)], [67, 91, 115])
        self.assertEqual([anchors[i][0] for i in range(6)], [44, 67, 89, 111, 133, 155])
        self.assertEqual(anchors, [tuple(p) for p in self.contract])

    def test_old_machine_screen_pitch_is_not_the_active_v5c_contract(self):
        self.assertNotIn("drawFactoryHopperWidgets", self.screen)
        self.assertIn('case "01" -> java.util.Arrays.asList', self.runtime)
        self.assertIn("FactoryStudyScreen::new", self.events)

    def test_selection_or_pointer_transform_mutation_is_rejected(self):
        mutations = [
            self.screen.replace("slot.x() - 1", "slot.x() - 2", 1),
            self.screen.replace("view.renderPointerX(mouseX)", "mouseX", 1),
            self.menu.replace("FactoryStudyGeometry.anchor(kind, this.slots.size())", "new MachineLayout.MachineSlot(0,0,0,\"bad\")", 1),
        ]
        for index, mutated in enumerate(mutations):
            with self.subTest(index=index), self.assertRaises(AssertionError):
                assert_v3_alignment(self.geometry, mutated if index == 2 else self.menu,
                                    mutated if index != 2 else self.screen, self.events,
                                    self.runtime, MATRIX.read_text(encoding="utf-8"))

    def test_contract_or_runtime_anchor_drift_is_rejected(self):
        changed = self.runtime.replace("new Point(44, 67)", "new Point(43, 67)", 1)
        with self.assertRaises(AssertionError):
            assert_v3_alignment(self.geometry, self.menu, self.screen, self.events, changed)


if __name__ == "__main__":
    unittest.main()
