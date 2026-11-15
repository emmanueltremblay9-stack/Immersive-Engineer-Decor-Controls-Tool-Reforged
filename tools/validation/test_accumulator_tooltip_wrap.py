"""Recording tests of the exact production tooltip dispatcher, not native rendering."""
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SCREEN = ROOT / "src/main/java/com/oblixorprime/engineersdecorreforged/client/MachineScreen.java"

FIXTURE = r'''
import java.util.List;
public class TooltipProbe {
    record Component(String text) { Line getVisualOrderText() { return new Line(this); } }
    record Line(Component source) {}
    static final class Font {
        int width;
        Component source;
        int width(Component text) { return 90; }
        List<Line> split(Component text, int width) {
            this.source = text;
            this.width = width;
            return List.of(new Line(text));
        }
    }
    static final class GuiGraphics {
        boolean wrapped;
        Component source;
        int calls, x, y;
        FluidTooltipPositioner positioner;
        void renderTooltip(Font font, Component text, int x, int y) {
            source = text; this.x = x; this.y = y; calls++;
        }
        void renderTooltip(Font font, List<Line> lines, int x, int y) {
            wrapped = true; source = lines.getFirst().source();
            this.x = x; this.y = y; calls++;
        }
        void renderTooltip(Font font, List<Line> lines, FluidTooltipPositioner positioner, int x, int y) {
            this.positioner = positioner; renderTooltip(font, lines, x, y);
        }
        void renderTooltip(Font font, Component text, FluidTooltipPositioner positioner, int x, int y) {
            this.positioner = positioner; renderTooltip(font, text, x, y);
        }
    }
    static final class FluidTooltipPositioner { static final FluidTooltipPositioner INSTANCE = new FluidTooltipPositioner(); }
    static final class FluidMachineRedesignLayout { static final int ACCUMULATOR_COLLECTION = 1; }
    static final class MachineLayout {
        record MachineSlot(int x, int y) {}
        List<MachineSlot> machineSlots() { return List.of(); }
    }
    enum MachineKind { SMALL_FLUID_FUNNEL, PASSIVE_FLUID_ACCUMULATOR, FACTORY_HOPPER, FACTORY_DROPPER, OTHER }
    static final class MachineMenu { MachineKind selected = MachineKind.OTHER; MachineLayout layout() { return new MachineLayout(); } MachineKind kind() { return selected; } }
    static final class Slot { boolean hasItem() { return false; } }
    final Font font = new Font();
    final MachineMenu menu = new MachineMenu();
    Slot hoveredSlot;
    int width, mode;
    boolean collection;
    final Component text = new Component("Styled translated explanation: 80 ticks, 250 mB, 100 mB, 200 mB.");
    Component smallFluidFunnelTooltip(int x, int y) { return mode == 1 ? text : null; }
    Component passiveFluidAccumulatorTooltip(int x, int y) { return mode == 2 ? text : null; }
    Component fluidBarrelTooltip(int x, int y) { return mode == 3 ? text : null; }
    Component smallMilkingMachineTooltip(int x, int y) { return mode == 4 ? text : null; }
    Component solarPanelEnergyTooltip(int x, int y) { return mode == 5 ? text : null; }
    Component blockBreakerTooltip(int x, int y) { return mode == 6 ? text : null; }
    Component originalControlTooltip(int x, int y) { return mode == 7 ? text : null; }
    Component slotTooltip(MachineLayout.MachineSlot slot) { return text; }
    boolean isFluidArtworkHovered(int region, double x, double y) { return collection; }
    boolean isHovering(int x, int y, int w, int h, double mx, double my) { return false; }
    /* METHOD */
    static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
    public static void main(String[] args) {
        for (int width : new int[]{1280, 400, 320, 240, 100, 24, 1}) {
            TooltipProbe probe = new TooltipProbe();
            probe.mode = 2; probe.collection = true; probe.width = width;
            GuiGraphics graphics = new GuiGraphics();
            probe.renderMachineTooltip(graphics, 161, 23);
            check(graphics.wrapped, "Collection must wrap");
            check(graphics.positioner == FluidTooltipPositioner.INSTANCE, "Collection frame positioning");
            check(probe.font.width == Math.max(1, Math.min(240, width - 24)), "Bounded width");
            check(probe.font.source == probe.text && graphics.source == probe.text, "Original Component preserved");
            check(graphics.calls == 1 && graphics.x == 161 && graphics.y == 23, "Native pointer and one tooltip");
        }
        for (int mode = 1; mode <= 7; mode++) {
            TooltipProbe probe = new TooltipProbe(); probe.mode = mode; probe.width = 320;
            // Even at collection coordinates, other machines must retain their paths.
            probe.collection = mode != 2;
            GuiGraphics graphics = new GuiGraphics(); probe.renderMachineTooltip(graphics, 15, 26);
            if (mode == 1 || mode == 2) {
                check(graphics.positioner == FluidTooltipPositioner.INSTANCE, "Fluid frame positioning");
                check(probe.font.source == null, "Short fluid text need not rewrap");
            } else if (mode == 4) {
                check(graphics.wrapped && probe.font.source == probe.text && graphics.positioner == null, "Milking tooltip retains native wrapping");
                check(probe.font.width == Math.max(1, Math.min(240, probe.width - 16)), "Milking width stays bounded");
            } else {
                check(!graphics.wrapped && probe.font.source == null && graphics.positioner == null, "Other tooltips unchanged");
            }
            check(graphics.source == probe.text && graphics.calls == 1, "Other tooltip content");
        }
        for (MachineKind kind : new MachineKind[]{MachineKind.FACTORY_HOPPER, MachineKind.FACTORY_DROPPER}) {
            for (int width : new int[]{1280, 400, 320, 240, 100, 24, 1}) {
                TooltipProbe factory = new TooltipProbe(); factory.mode = 7; factory.width = width;
                factory.menu.selected = kind;
                GuiGraphics draw = new GuiGraphics(); factory.renderMachineTooltip(draw, 148, 66);
                check(draw.wrapped, "Factory controls must wrap");
                check(factory.font.width == Math.max(1, Math.min(240, width - 16)), "Factory bounded width");
                check(draw.source == factory.text && factory.font.source == factory.text, "Factory original styled Component retained");
                check(draw.calls == 1 && draw.x == 148 && draw.y == 66, "Factory one pointer tooltip");
            }
        }
        TooltipProbe probe = new TooltipProbe();
        GuiGraphics graphics = new GuiGraphics(); probe.renderMachineTooltip(graphics, 0, 0);
        check(graphics.calls == 0, "No tooltip outside targets");
        System.out.println("Accumulator tooltip dispatcher recording passed");
    }
}
'''


def run_probe(source):
    start = source.index("   private void renderMachineTooltip(")
    end = source.index("   private Component solarPanelEnergyTooltip(", start)
    with tempfile.TemporaryDirectory(prefix="accumulator-tooltip-") as directory:
        target = Path(directory)
        java = target / "TooltipProbe.java"
        helper_start = source.index("   static int milkingTooltipWidth(")
        helper_end = source.index("\n   }", helper_start) + len("\n   }")
        java.write_text(FIXTURE.replace("    /* METHOD */", source[start:end] + source[helper_start:helper_end]))
        compile_result = subprocess.run(
            ["java", "-m", "jdk.compiler/com.sun.tools.javac.Main", "-source", "21", "-target", "21",
             "-d", str(target), str(java)], capture_output=True, text=True)
        if compile_result.returncode:
            raise AssertionError(compile_result.stderr)
        return subprocess.run(["java", "-cp", str(target), "TooltipProbe"], capture_output=True, text=True)


class AccumulatorTooltipWrapTests(unittest.TestCase):
    def test_exact_dispatcher_preserves_content_routes_and_bounds(self):
        result = run_probe(SCREEN.read_text())
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_unwrapped_collection_regression_is_rejected(self):
        source = SCREEN.read_text().replace("this.font.split(accumulatorTooltip, wrapWidth)", "accumulatorTooltip")
        result = run_probe(source)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Collection must wrap", result.stderr)

    def test_unbounded_width_regression_is_rejected(self):
        source = SCREEN.read_text().replace("Math.max(1, Math.min(240, this.width - 24))", "this.width")
        result = run_probe(source)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Bounded width", result.stderr)


if __name__ == "__main__":
    unittest.main()
