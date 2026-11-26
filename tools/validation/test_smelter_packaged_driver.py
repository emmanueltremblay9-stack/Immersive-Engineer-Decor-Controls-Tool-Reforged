"""Structural preservation tests, not native execution or a Java compilation claim."""
from pathlib import Path
import unittest
import tempfile
from unittest.mock import patch
ROOT = Path(__file__).resolve().parents[2]
class DriverTests(unittest.TestCase):
    def test_fixture_preserves_every_original_gate(self):
        old=(ROOT/'src/main/java/com/oblixorprime/engineersdecorreforged/client/validation/NativeSmelterGuiValidation.java').read_text()
        new=(ROOT/'src/smelterPackagedValidation/java/com/oblixorprime/iedctrtest/smelter/NativeSmelterGuiValidation.java').read_text()
        self.assertEqual(new.replace('package com.oblixorprime.iedctrtest.smelter;', 'package com.oblixorprime.engineersdecorreforged.client.validation;'),old)
    def test_support_is_server_prepared_before_original_fixture(self):
        text=(ROOT/'src/smelterPackagedValidation/java/com/oblixorprime/iedctrtest/smelter/SmelterPackagedDriver.java').read_text()
        self.assertIn('new BlockPos(x, 80, z), Blocks.STONE.defaultBlockState()',text)
        self.assertIn('x = -1; x <= 1',text);self.assertIn('z = 2; z <= 4',text)
        self.assertLess(text.index('supportReady.join()'),text.index('NativeSmelterGuiValidation.begin'))
        self.assertIn('smelter-failure-diagnostic.json',text)
        self.assertIn('.get(3, TimeUnit.SECONDS)',text)
    def test_smelter_fixture_uses_rear_public_fluid_capability_and_rejects_wrong_faces(self):
        source=(ROOT/'src/smelterPackagedValidation/java/com/oblixorprime/iedctrtest/smelter/NativeSmelterGuiValidation.java').read_text()
        self.assertIn('.setValue(MachineBlocks.HORIZONTAL_FACING, Direction.NORTH)',source)
        self.assertIn('Capabilities.FluidHandler.BLOCK, POS, Direction.SOUTH',source)
        self.assertIn('rear.fill(new FluidStack(Fluids.LAVA, 2500), FluidAction.EXECUTE) == 2500',source)
        for face in ('UP','NORTH','EAST','WEST'):
            self.assertIn('Capabilities.FluidHandler.BLOCK, POS, Direction.'+face,source)
        self.assertIn('bottom.fill(new FluidStack(Fluids.LAVA, 2500), FluidAction.EXECUTE) == 0',source)
        self.assertNotIn('machine.fluidHandler(Direction.UP)',source)
    def test_failed_isolation_never_stops_user_client(self):
        text=(ROOT/'src/smelterPackagedValidation/java/com/oblixorprime/iedctrtest/smelter/SmelterPackagedDriver.java').read_text()
        catch=text.split('} catch (Throwable error) {',1)[1]
        self.assertIn('if (isolatedDirectoryValidated) mc.stop();',catch)
        self.assertEqual(catch.count('mc.stop()'),1)
        self.assertLess(text.index('Existing saves forbidden'),text.index('isolatedDirectoryValidated = true;'))
        self.assertLess(text.index('Isolated directory/nonce mismatch'),text.index('isolatedDirectoryValidated = true;'))
    def test_packaged_run_no_source_fallback_and_exact_hash(self):
        driver=(ROOT/'tools/validation/smelter_packaged_native.py').read_text()
        for token in ['verifySmelterPackagedRuntime','prepareSmelterPackagedDefaultClientRun','runSmelterPackagedDefaultClient',"==args.expected_jar_sha256",'current_candidate(project)==source','hashlib.sha256(jar.read_bytes()).hexdigest()==jar_sha']:
            self.assertIn(token,driver)
        self.assertNotIn("['runClient']",driver)
        self.assertNotIn("systemProperty 'iedctr.smelterQualificationNative'",driver)
        self.assertNotIn("systemProperty 'iedctr.clientGameplay'",driver)
    def test_gradle_guard_scope_and_namespace(self):
        g=(ROOT/'gradle/smelter-packaged-validation.gradle').read_text()
        for token in ['sourceSets.main.runtimeClasspath - sourceSets.main.output', 'mainOutputs.contains(it)', 'harnessPackages.contains', 'Duplicate entries', 'sha256(normalJar)', 'loadedMods =', "['iedctr_smelter_validation']"]: self.assertIn(token,g)
        self.assertNotIn('src/main/java',g)
    def test_original_validator_called_not_reimplemented(self):
        from smelter_packaged_proof import validate_proof
        with tempfile.TemporaryDirectory() as temporary, patch('smelter_packaged_proof.validate_runtime'), patch('client_gameplay.validate_smelter_gui_proof',return_value='called') as original:
            game=Path(temporary)/'run';game.mkdir()
            runtime={};proof={'packaged_runtime':runtime,'packaged_runtime_after':{}}
            self.assertEqual(validate_proof(proof,'nonce','sha',game,'logs',0,False,1280,260,'fr_fr',{},1),'called')
            original.assert_called_once_with(proof,'nonce','sha','sha',game,1280,260,'fr_fr','logs')
if __name__=='__main__':unittest.main()
