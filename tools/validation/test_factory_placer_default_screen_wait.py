"""Execute the fixture's actual screen guard with Java doubles, not Minecraft."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
FIXTURE = ROOT / 'src/factoryPlacerNativeValidation/java/com/oblixorprime/iedctrtest/factoryplacer/PackagedDefaultScreenValidation.java'


class DefaultScreenWaitTest(unittest.TestCase):
    def test_null_wait_correct_screen_and_wrong_screen(self):
        self.assertIsNotNone(shutil.which('java'), 'Java 21 source launcher required for fixture guard regression')
        source = FIXTURE.read_text()
        # Extract executable guard unchanged, so restoring the eager null dereference fails.
        guard = source.split('if (mc.getOverlay() != null) return;', 1)[1].split(
            'require(mc.player.containerMenu instanceof MachineMenu', 1)[0]
        java = '''public class ScreenGuardCheck {
            static class FactoryPlacerV2ReferenceScreen {}
            static class WrongScreen {}
            static class Client { Object screen; }
            static int advances;
            static void require(boolean condition, String message) {
                if (!condition) throw new IllegalStateException(message);
            }
            static void tick(Client mc) {
        ''' + guard + '''
                advances++;
            }
            public static void main(String[] args) {
                Client mc = new Client();
                for (int i = 0; i < 50; i++) tick(mc);
                require(advances == 0, "Null screen must wait without progressing");
                mc.screen = new FactoryPlacerV2ReferenceScreen(); tick(mc);
                require(advances == 1, "Expected screen must progress after wait");
                mc.screen = new WrongScreen();
                try { tick(mc); throw new AssertionError("Wrong screen accepted"); }
                catch (IllegalStateException expected) {
                    require(expected.getMessage().equals("Unexpected default screen: " + WrongScreen.class.getName()),
                        "Keep diagnostic for wrong non-null screen");
                }
                require(advances == 1, "Wrong screen must not progress");
            }
        }'''
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / 'ScreenGuardCheck.java'
            path.write_text(java)
            subprocess.run(['java', str(path)], check=True, capture_output=True, text=True)


if __name__ == '__main__':
    unittest.main()
