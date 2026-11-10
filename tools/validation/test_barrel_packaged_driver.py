"""Source isolation guards, not a mapped compilation or native success claim."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[2]
BASE=ROOT/'src/barrelPackagedValidation/java/com/oblixorprime/iedctrtest/barrel'

class BarrelDriverGuards(unittest.TestCase):
    def test_native_input_has_no_direct_dispatch_shortcuts(self):
        source=(BASE/'NativeBarrelValidation.java').read_text()
        for forbidden in ('.openMenu(', '.handleInventoryMouseClick(', '.clicked(', '.quickMoveStack(',
                          '.setScreen(', '.mouseClicked(', '.mouseReleased('):
            self.assertNotIn(forbidden,source)
        for required in ('XTestFakeMotionEvent', 'XTestFakeButtonEvent', 'XTEST_RMB_BLOCK_HIT',
                         'hit.getBlockPos().equals(POS)', 'mc.player.getMainHandItem().isEmpty()',
                         'native_press_observed', 'native_release_observed', '.iedctr-barrel-xvfb'):
            self.assertIn(required,source)

    def test_renderer_end_owns_capture(self):
        source=(BASE/'NativeBarrelValidation.java').read_text()
        screen_post=source.split('void afterRender(',1)[1].split('void afterFrame(',1)[0]
        frame_post=source.split('void afterFrame(',1)[1].split('void tick(',1)[0]
        self.assertNotIn('capture(mc,screen)',screen_post)
        self.assertNotIn('captureIndex++',screen_post)
        self.assertIn('capture(mc,screen)',frame_post)
        for token in ('mc.screen==screen', 'screen.getMenu()==pendingMenu', 'mc.player.containerMenu==pendingMenu',
                      '!mc.getDebugOverlay().showProfilerChart()', 'expected(serverSnapshot)',
                      '"RenderFrameEvent.Post:LOWEST"', 'clearFrame()'):
            self.assertIn(token,frame_post)
        self.assertIn('@SubscribeEvent(priority=EventPriority.LOWEST)\n   public static void afterFrame',source)
        self.assertIn('component.getWidth(event.getFont())',source)
        self.assertIn('height+=component.getHeight()',source)
        self.assertIn('"barrel_fluid_pixels"',source)
        self.assertIn('colors.contains(0xFF282014)',source)
        self.assertIn('sprite.contents().name().equals(still)',source)

    def test_ordinary_jar_isolation_and_six_profiles(self):
        gradle=(ROOT/'gradle/barrel-packaged-validation.gradle').read_text()
        self.assertIn('sourceSets.main.runtimeClasspath - sourceSets.main.output',gradle)
        self.assertNotIn('compileClasspath += sourceSets.main.output',gradle)
        self.assertIn('Supplied runtime JAR differs',gradle)
        self.assertIn('Harness and production JAR contain a split package',gradle)
        self.assertIn("loadedMods = [neoForge.mods.getByName('iedctr_barrel_validation')]",gradle)
        workflow=(ROOT/'.github/workflows/barrel-packaged-native.yml').read_text()
        for token in ('needs: ordinary-artifact', 'fluid: [water, lava, milk]',
                      '{language: en_us, height: 720}', '{language: fr_fr, height: 260}',
                      'shutil.copyfile(jar, artifact)', "identity['source_sha'] == source", "identity['sha256'] == digest",
                      '--runtime-jar', 'if: always()'):
            self.assertIn(token,workflow)
        runner=(ROOT/'tools/validation/barrel_packaged_native.py').read_text()
        for token in ('final_sha != source_sha', 'Production JAR changed during execution',
                      '-Pbarrel_runtime_jar=', 'Supplied artifact changed during preparation'):
            self.assertIn(token,runner)
        self.assertNotIn("systemProperty 'iedctr.clientGameplay'",runner)
        cumulative=(ROOT/'.github/workflows/barrel-fluid-validation.yml').read_text()
        self.assertIn('--corrected-machine-gui-only',cumulative)
        self.assertIn('test_client_gameplay.py',cumulative)

    def test_exact_legacy_selector_is_isolated_and_observed(self):
        for path in ('tools/validation/barrel_packaged_native.py', 'gradle/barrel-packaged-validation.gradle'):
            source = (ROOT/path).read_text()
            self.assertIn("systemProperty 'iedctr.dev.barrelV5Reference', 'false'", source)
        source = (BASE/'PackagedRuntimeProof.java').read_text()
        self.assertIn('require("false".equals(System.getProperty("iedctr.dev.barrelV5Reference"))', source)
        self.assertIn('if (key.equals("iedctr.dev.barrelV5Reference")) continue;', source)
        self.assertIn('"legacy-diagnostic-only"', source)
        self.assertIn('"other_production_selectors_absent"', source)
        runner = (ROOT/'tools/validation/client_gameplay.py').read_text()
        self.assertIn("if (System.getenv('IEDCTR_CORRECTED_MACHINE_GUI_ONLY') == 'true') {\n            systemProperty 'iedctr.dev.barrelV5Reference', 'false'", runner)

    def test_corrected_gui_solar_legacy_selector_is_scoped_and_observed(self):
        runner = (ROOT/'tools/validation/client_gameplay.py').read_text()
        selector = "systemProperty 'iedctr.dev.solarV2Reference', 'false'"
        self.assertEqual(1, runner.count(selector))
        self.assertIn("if (System.getenv('IEDCTR_CORRECTED_MACHINE_GUI_ONLY') == 'true') {\n"
                      "            systemProperty 'iedctr.dev.barrelV5Reference', 'false'\n"
                      "            " + selector + "\n        }", runner)
        source = (ROOT/'src/main/java/com/oblixorprime/engineersdecorreforged/client/validation/NativeCorrectedMachineGuiValidation.java').read_text()
        self.assertEqual(2, source.count('require("false".equals(System.getProperty("iedctr.dev.solarV2Reference"))'))
        self.assertIn('proof.addProperty("solar_legacy_selector", "iedctr.dev.solarV2Reference=" + System.getProperty("iedctr.dev.solarV2Reference"))', source)
        self.assertIn('proof.addProperty("solar_qualification", "legacy-diagnostic-only")', source)

    def test_runtime_provenance_and_legacy_selectors(self):
        source=(BASE/'PackagedRuntimeProof.java').read_text()
        for token in ('url.toURI().equals(mod.findResource(entry).toUri())',
                      'type.getModule() != BarrelValidationMod.class.getModule()',
                      'resourceHash.equals(hash(archive.readAllBytes()))', 'active.sourcePackId().equals(',
                      'key.startsWith("iedctr.dev.")', 'key.equals("iedctr.clientGameplay")',
                      'key.equals("iedctr.correctedMachineGuiOnly")', 'fixture_free_scope'):
            self.assertIn(token,source)

if __name__=='__main__':unittest.main()
