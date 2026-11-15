"""Source-dispatch recording/mutation tests, not native screenshot qualification."""
import unittest
from test_accumulator_tooltip_wrap import SCREEN, run_probe
from factory_tooltip_integration import assert_projection

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

if __name__ == '__main__': unittest.main()
