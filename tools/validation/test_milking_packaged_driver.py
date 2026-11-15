"""Structural preservation tests, not native execution or a Java compilation claim."""
from pathlib import Path
import unittest
from unittest.mock import patch
ROOT = Path(__file__).resolve().parents[2]
class DriverTests(unittest.TestCase):
    def test_fixture_preserves_every_original_gate(self):
        old=(ROOT/'src/main/java/com/oblixorprime/engineersdecorreforged/client/validation/NativeMilkingQualificationValidation.java').read_text()
        new=(ROOT/'src/milkingPackagedValidation/java/com/oblixorprime/iedctrtest/milking/NativeMilkingPackagedValidation.java').read_text()
        new=new.replace('package com.oblixorprime.iedctrtest.milking;','package com.oblixorprime.engineersdecorreforged.client.validation;').replace('NativeMilkingPackagedValidation','NativeMilkingQualificationValidation').replace('modid = MilkingValidationMod.MOD_ID','modid = EngineersDecorReforged.MOD_ID').replace('iedctr.milkingPackagedNative','iedctr.milkingQualificationNative')
        new=new.replace('\n   private static JsonObject runtimeBefore;','').replace('\n      runtimeBefore=PackagedRuntimeProof.verify(mc);','')
        new=new.replace('         var runtimeAfter=error==null?PackagedRuntimeProof.verify(mc):null;\n','').replace('p.add("packaged_runtime",runtimeBefore);p.add("packaged_runtime_after",runtimeAfter);','')
        self.assertEqual(new,old, 'Packaged fixture must not silently change original gameplay/input/capture gates')
    def test_packaged_run_no_source_fallback_and_exact_hash(self):
        driver=(ROOT/'tools/validation/milking_packaged_native.py').read_text()
        for token in ['verifyMilkingPackagedRuntime','prepareMilkingPackagedDefaultClientRun','runMilkingPackagedDefaultClient',"==args.expected_jar_sha256",'current_candidate(project)==source','hashlib.sha256(jar.read_bytes()).hexdigest()==jar_sha']:
            self.assertIn(token,driver)
        self.assertNotIn("['runClient']",driver)
        self.assertNotIn("systemProperty 'iedctr.milkingQualificationNative'",driver)
        self.assertNotIn("systemProperty 'iedctr.clientGameplay'",driver)
    def test_gradle_guard_scope_and_namespace(self):
        g=(ROOT/'gradle/milking-packaged-validation.gradle').read_text()
        for token in ['sourceSets.main.runtimeClasspath - sourceSets.main.output', 'mainOutputs.contains(it)', 'harnessPackages.contains', 'Duplicate entries', 'sha256(normalJar)', 'loadedMods =', "['iedctr_milking_validation']"]: self.assertIn(token,g)
        self.assertNotIn('src/main/java',g)
    def test_original_validator_called_not_reimplemented(self):
        from milking_packaged_proof import validate_proof
        with patch('milking_packaged_proof.validate_runtime'), patch('milking_qualification_native.validate_proof',return_value='called') as original:
            runtime={};proof={'packaged_runtime':runtime,'packaged_runtime_after':{}}
            self.assertEqual(validate_proof(proof,'nonce','sha',Path('/x'),'logs',0,False,1280,260,'fr_fr',{},1),'called')
            original.assert_called_once_with(proof,'nonce','sha',Path('/x'),'logs',0,False,1280,260,'fr_fr',1)
if __name__=='__main__':unittest.main()
