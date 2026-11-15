"""Diagnostic-only structural regression guards; native CI still required."""
from pathlib import Path
import hashlib
import unittest

SOURCE = Path(__file__).resolve().parents[2] / 'src/main/java/com/oblixorprime/engineersdecorreforged/client/validation/ExternalFluidClientValidation.java'
BASELINE_TICK_SHA256 = "b6f5df5f9262d13d6dbb014b56ed675f23717bd394932106d7610b3a1722de64"

def original_tick(source):
    tick = source[source.index('   static boolean tick('):source.index('   // Diagnostics only:')]
    tick = tick.replace('      if (!(++wait < 400)) check(false, "external sync stalled " + stalledDiagnostic(mc));', '      check(++wait < 400, "external sync stalled");')
    tick = tick.replace('            lastServerObservation = observeServer(level, "setup-complete");\n', '')
    tick = tick.replace('            lastServerObservation = observeServer(level, "drain-submitted");\n', '')
    return hashlib.sha256(tick.strip().encode()).hexdigest()

class ExternalDiagnosticTest(unittest.TestCase):
    def test_original_tick_order_and_gates_unchanged(self):
        self.assertEqual(original_tick(SOURCE.read_text()), BASELINE_TICK_SHA256)

    def test_guard_mutations_detected(self):
        source = SOURCE.read_text()
        for old, new in [('++wait < 400', '++wait < 800'),
                         ('pending.join();', ''),
                         ('if(!mc.level.getFluidState(POS).isEmpty())return false;', '')]:
            self.assertNotEqual(original_tick(source.replace(old, new)), BASELINE_TICK_SHA256)

    def test_diagnostics_do_not_join_or_read_server_from_client(self):
        source = SOURCE.read_text()
        client = source[source.index('private static String stalledDiagnostic'):source.index('private static void check')]
        self.assertNotIn('getSingleplayerServer', client)
        self.assertNotIn('.join(', client)
        self.assertIn('lastServerObserved=', client)
        self.assertIn('private static volatile String lastServerObservation', source)
        server = source[source.index('private static String observeServer'):source.index('private static String stalledDiagnostic')]
        self.assertIn('gameTime=', server)
        self.assertIn('case=', server)
        self.assertEqual(source.count('observeServer(level,'), 2)

if __name__ == '__main__': unittest.main()
