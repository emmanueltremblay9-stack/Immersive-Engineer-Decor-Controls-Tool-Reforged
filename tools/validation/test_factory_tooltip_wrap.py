"""Source-dispatch recording/mutation tests, not native screenshot qualification."""
import unittest
from test_accumulator_tooltip_wrap import SCREEN, run_probe
from factory_tooltip_integration import assert_projection
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
V3_SCREEN = ROOT / 'src/main/java/com/oblixorprime/engineersdecorreforged/client/FactoryStudyScreen.java'
V3_GEOMETRY = ROOT / 'src/main/java/com/oblixorprime/engineersdecorreforged/utility/FactoryStudyGeometry.java'
V3_EVENTS = ROOT / 'src/main/java/com/oblixorprime/engineersdecorreforged/client/ClientModEvents.java'
V5C_GEOMETRY = ROOT / 'src/main/java/com/oblixorprime/engineersdecorreforged/utility/Gui11V5CRuntimeGeometry.java'

def assert_v3_tooltip_routing(screen, geometry, events):
    required_screen = (
        'FactoryStudyGeometry.controlAt(kind, localPointerX, localPointerY)',
        'tooltip = controls.get(hovered).description()',
        'if (tooltip == null && focusedControl() != null && minecraft.getLastInputType().isKeyboard())',
        'font.split(tooltip, FactoryStudyGeometry.tooltipWidth(width))',
        'if (action == null || !menu.getCarried().isEmpty()) return;',
    )
    required_geometry = (
        'case VELOCITY -> new Rect(184, 43, 28, 80)',
        'case FILTER_GATE -> new Rect(209, 126, 24, 28)',
        'for (var control : controls(kind)) if (controlBounds(kind, control).contains(x, y)) return control;',
    )
    required_events = (
        'event.register((MenuType)ModMenus.FACTORY_HOPPER.get(), FactoryStudyScreen::new)',
        'event.register((MenuType)ModMenus.FACTORY_DROPPER.get(), FactoryStudyScreen::new)',
    )
    for fragment in required_screen: assert fragment in screen, f'Current V3 tooltip path missing: {fragment}'
    for fragment in required_geometry: assert fragment in geometry, f'Current V3 hit geometry missing: {fragment}'
    for fragment in required_events: assert fragment in events, f'Current V3 screen route missing: {fragment}'

class FactoryTooltipTests(unittest.TestCase):
    def test_exact_additive_projection(self):
        assert_projection(SCREEN.read_text())
    def test_rejects_unwrapped(self):
        result = run_probe(SCREEN.read_text().replace('this.font.split(controlTooltip, wrapWidth)', 'controlTooltip'))
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('Factory controls must wrap', result.stderr)
    def test_rejects_unbounded(self):
        result = run_probe(SCREEN.read_text().replace('int wrapWidth = Math.max(1, Math.min(240, this.width - 16));', 'int wrapWidth = this.width;'))
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('Factory bounded width', result.stderr)
    def test_rejects_global_routing(self):
        result = run_probe(SCREEN.read_text().replace('kind == MachineKind.FACTORY_HOPPER || kind == MachineKind.FACTORY_DROPPER', 'true'))
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('Other tooltips unchanged', result.stderr)

    def test_v5c_tooltip_route_uses_menu_state_and_geometry(self):
        screen = SCREEN.read_text()
        geometry = V5C_GEOMETRY.read_text()
        start = screen.index('   private void renderMachineTooltip(')
        end = screen.index('   private Component solarPanelEnergyTooltip(', start)
        route = screen[start:end]
        self.assertTrue(route.startswith('   private void renderMachineTooltip(GuiGraphics graphics, int mouseX, int mouseY) {\n'
                                          '      if (((MachineMenu)this.menu).gui11V5CPresentation()) {'))
        for token in ('Gui11V5CRuntimeGeometry.energy(this.menu.kind())',
                      'Gui11V5CRuntimeGeometry.fluidDisplay(this.menu.kind())',
                      'this.menu.energyStored(), this.menu.energyCapacity()',
                      'this.menu.smallFluidFunnelFluidCode()',
                      'this.menu.kind() == MachineKind.SMALL_ELECTRICAL_FURNACE',
                      'this.menu.field(4)', 'this.menu.field(7)',
                      'if (hasVisibleTooltipText(tooltip)) graphics.renderTooltip',
                      '         return;'):
            self.assertIn(token, route)
        self.assertIn('case "07" -> switch (key)', geometry)
        self.assertIn('case "13" -> new Rect(110, 103, 88, 20)', geometry)
        self.assertIn('case "14" -> new Rect(137, 65, 29, 52)', geometry)

    def test_current_v3_control_tooltips_follow_hit_test_and_live_state(self):
        screen = V3_SCREEN.read_text()
        geometry = V3_GEOMETRY.read_text()
        events = V3_EVENTS.read_text()
        assert_v3_tooltip_routing(screen, geometry, events)
        self.assertIn('return result.append("\\n").append(text(!menu.getCarried().isEmpty() ? "carried_blocked"', screen)

    def test_current_v3_tooltip_mutations_are_detected(self):
        screen = V3_SCREEN.read_text()
        geometry = V3_GEOMETRY.read_text()
        events = V3_EVENTS.read_text()
        expected = (
            ('screen', screen, 'FactoryStudyGeometry.controlAt(kind, localPointerX, localPointerY)', 'null'),
            ('screen', screen, 'font.split(tooltip, FactoryStudyGeometry.tooltipWidth(width))', 'tooltip'),
            ('screen', screen, 'tooltip = controls.get(hovered).description()', 'tooltip = null'),
        )
        for name, source, before, after in expected:
            with self.subTest(before=before):
                self.assertIn(before, source)
                with self.assertRaises(AssertionError):
                    assert_v3_tooltip_routing(source.replace(before, after, 1), geometry, events)

if __name__ == '__main__': unittest.main()
