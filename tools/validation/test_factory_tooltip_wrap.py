"""Source-dispatch recording/mutation tests, not native screenshot qualification."""
import unittest
from pathlib import Path
from breaker_current_main_integration import project_merge_tree
from test_accumulator_tooltip_wrap import SCREEN, run_probe
from factory_tooltip_integration import assert_projection

ROOT = Path(__file__).resolve().parents[2]

def main_screen_source():
    path = SCREEN.relative_to(ROOT).as_posix()
    return project_merge_tree(path, SCREEN.read_bytes(), 'main').decode('utf-8')

class FactoryTooltipTests(unittest.TestCase):
    def test_exact_additive_projection(self):
        assert_projection(main_screen_source())
    def test_rejects_unwrapped(self):
        result = run_probe(SCREEN.read_text(encoding='utf-8').replace('this.font.split(controlTooltip, wrapWidth)', 'controlTooltip'))
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('Factory controls must wrap', result.stderr)
    def test_rejects_unbounded(self):
        result = run_probe(SCREEN.read_text(encoding='utf-8').replace('int wrapWidth = Math.max(1, Math.min(240, this.width - 16));', 'int wrapWidth = this.width;'))
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('Factory bounded width', result.stderr)
    def test_rejects_global_routing(self):
        result = run_probe(SCREEN.read_text(encoding='utf-8').replace('kind == MachineKind.FACTORY_HOPPER || kind == MachineKind.FACTORY_DROPPER', 'true'))
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('Other tooltips unchanged', result.stderr)

if __name__ == '__main__': unittest.main()
