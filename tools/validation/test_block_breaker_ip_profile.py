"""Fail-closed wiring of the separate real-IP compatibility job."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]


class RequiredIpProfileTests(unittest.TestCase):
    def test_real_dependency_is_pinned_and_isolated(self):
        workflow = (ROOT / '.github/workflows/block-breaker-lubricant.yml').read_text()
        self.assertIn('github.event.pull_request.head.sha || github.sha', workflow)
        self.assertIn('build/isolated-ip-gametest/mods/', workflow)
        self.assertIn('https://cursemaven.com/curse/maven/immersive-petroleum-268250/8420142/immersive-petroleum-268250-8420142.jar', workflow)
        self.assertIn('c1bfef3290dd8c2c68519155e395059766f5d258a52d5e08faf63eb627312e8b', workflow)
        self.assertIn('sha256sum --check', workflow)
        self.assertIn('--init-script tools/validation/fixtures/block_breaker/ip-gametest.init.gradle runGameTestServer', workflow)

    def test_init_script_requires_registry_in_real_server(self):
        script = (ROOT / 'tools/validation/fixtures/block_breaker/ip-gametest.init.gradle').read_text()
        self.assertIn('gameTestRun.gameDirectory = root.file("build/isolated-ip-gametest")', script)
        self.assertIn('gameTestRun.systemProperty "iedctr.requireLubricantProfile", "true"', script)
        source = (ROOT / 'src/main/java/com/oblixorprime/engineersdecorreforged/gametest/MachineGuiGameTests.java').read_text()
        start = source.index('public static void small_block_breaker_required_lubricant_profile_is_present(')
        method = source[start:source.index('\n   @GameTest', start)]
        self.assertIn('Boolean.getBoolean("iedctr.requireLubricantProfile")', method)
        self.assertIn('ResourceLocation.fromNamespaceAndPath("immersivepetroleum", "lubricant")', method)
        self.assertIn('helper.assertTrue(lubricant != Fluids.EMPTY,', method)
        self.assertNotIn('return;', method)


if __name__ == '__main__':
    unittest.main()
