"""Execute the isolated harness wait/timeout expressions, not Minecraft gameplay."""
from pathlib import Path
import re
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'src/factoryPlacerNativeValidation/java/com/oblixorprime/iedctrtest/factoryplacer/PackagedDefaultScreenValidation.java'


class DefaultScreenWaitTest(unittest.TestCase):
    def run_probe(self, screen_block):
        source = SOURCE.read_text()
        timeout = re.findall(r'require\(System.nanoTime\(\) < deadline && \+\+ticks < 1800, "Default packaged smoke timeout at " \+ stage\);', source)
        self.assertEqual(len(timeout), 1)
        self.assertLess(source.index(timeout[0]), source.index('if (mc.getOverlay() != null) return;'))
        with tempfile.TemporaryDirectory() as directory:
            probe = Path(directory) / 'DefaultScreenProbe.java'
            probe.write_text('''
public class DefaultScreenProbe {
    static final class FactoryPlacerV2ReferenceScreen {}
    static final class UnexpectedScreen {}
    static final class Client { Object screen; boolean accepted; }
    static long deadline;
    static int ticks;
    static String stage = "OPEN";
    static void require(boolean valid, String message) {
        if (!valid) throw new IllegalStateException(message);
    }
    static void tick(Client mc) {
''' + timeout[0] + '\n' + screen_block + '''
        mc.accepted = true;
    }
    static void reset() { ticks = 0; deadline = System.nanoTime() + 900_000_000_000L; }
    static void rejects(Client mc, String prefix) {
        try { tick(mc); throw new AssertionError("Invalid state was accepted"); }
        catch (IllegalStateException expected) {
            require(expected.getMessage().startsWith(prefix), "Wrong failure: " + expected);
        }
    }
    public static void main(String[] ignored) {
        reset();
        Client mc = new Client();
        tick(mc); tick(mc);
        require(!mc.accepted && ticks == 2, "Null must wait, still advancing timeout");
        mc.screen = new FactoryPlacerV2ReferenceScreen(); tick(mc);
        require(mc.accepted, "Expected screen must proceed after delayed arrival");
        reset(); mc = new Client(); mc.screen = new UnexpectedScreen();
        rejects(mc, "Unexpected default screen: DefaultScreenProbe$UnexpectedScreen");
        require(!mc.accepted, "Wrong screen must not proceed");
        reset(); mc = new Client(); deadline = System.nanoTime() - 1;
        rejects(mc, "Default packaged smoke timeout at OPEN");
        reset(); mc = new Client(); ticks = 1799;
        rejects(mc, "Default packaged smoke timeout at OPEN");
    }
}
''')
            return subprocess.run(['java', str(probe)], capture_output=True, text=True, timeout=30)

    def test_actual_wait_handles_delay_wrong_screen_and_timeouts(self):
        source = SOURCE.read_text()
        block = source.split('if (mc.getOverlay() != null) return;', 1)[1].split(
            'require(mc.player.containerMenu instanceof MachineMenu', 1)[0]
        result = self.run_probe(block)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_original_eager_error_message_is_a_failing_negative_control(self):
        result = self.run_probe('''
            if (!(mc.screen instanceof FactoryPlacerV2ReferenceScreen)) {
                require(mc.screen == null, "Unexpected default screen: " + mc.screen.getClass().getName()); return;
            }
''')
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('NullPointerException', result.stderr)


if __name__ == '__main__':
    unittest.main()
