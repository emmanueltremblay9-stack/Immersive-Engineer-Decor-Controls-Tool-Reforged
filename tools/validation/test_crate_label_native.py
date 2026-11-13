"""Synthetic validator guards; these tests do not run Minecraft."""
import copy
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import crate_label_native as native


class CrateNativeProofTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.game = Path(self.temp.name)
        self.jar = self.game/'mod.jar'
        self.proof = dict(suite='native-crate-label-resize-v1', status='passed', nonce='a'*32,
            executed_source_sha='b'*40, physical_os_input=False, visual_acceptance=False,
            resize_method='Screen.resize with real Minecraft and EditBox; no GLFW resize',
            width=1280, height=720, selected_language='en_us', cases=native.CASES[:], capture_sha256='d'*64,
            packaged_runtime=dict(jar_path=str(self.jar.resolve()), jar_sha256='c'*64,
                fixture_free_jar=True, separate_harness_module=True,
                class_origins=[dict(**{'class':'com.oblixorprime.engineersdecorreforged.client.LabeledCrateEditScreen'})]))

    def validate(self, proof):
        with patch.object(native, 'png') as png:
            native.validate(proof,'a'*32,'b'*40,self.jar,'c'*64,self.game,1280,720,'en_us')
            png.assert_called_once_with(self.game/'crate-label-resized.png',1280,720,'d'*64)

    def test_complete_bounded_proof(self):
        self.validate(self.proof)

    def test_identity_and_evidence_boundaries_fail_closed(self):
        for key, value in dict(nonce='x', executed_source_sha='x', status='failed',
                              physical_os_input=True, visual_acceptance=True, width=True,
                              resize_method='mock resize', selected_language='fr_fr').items():
            with self.subTest(key=key):
                proof=copy.deepcopy(self.proof);proof[key]=value
                with self.assertRaises(native.SmokeFailure):self.validate(proof)

    def test_missing_each_case_rejected(self):
        for case in native.CASES:
            proof=copy.deepcopy(self.proof);proof['cases'].remove(case)
            with self.assertRaises(native.SmokeFailure):self.validate(proof)

    def test_missing_native_screen_provenance_rejected(self):
        proof=copy.deepcopy(self.proof);proof['packaged_runtime']['class_origins']=[]
        with self.assertRaises(native.SmokeFailure):self.validate(proof)

    def test_source_runtime_or_wrong_jar_rejected(self):
        for key,value in dict(jar_path='classes',jar_sha256='x',fixture_free_jar=False,separate_harness_module=False).items():
            proof=copy.deepcopy(self.proof);proof['packaged_runtime'][key]=value
            with self.assertRaises(native.SmokeFailure):self.validate(proof)

    def test_native_code_is_test_only_and_contains_real_routes(self):
        root=Path(__file__).resolve().parents[2]
        src=(root/'src/factoryPlacerNativeValidation/java/com/oblixorprime/iedctrtest/factoryplacer/CrateLabelValidation.java').read_text()
        for token in ['screen.resize(mc,width,height)', 'EditBox.class::isInstance', 'screen.keyPressed(257, 0, 0)',
                      'screen.mouseClicked(', 'mc.getSingleplayerServer().submit(', 'machine.labelLines()',
                      'Existing saves forbidden', 'packaged = PackagedRuntimeProof.verify(mc)']:
            self.assertIn(token,src)
        self.assertFalse((root/'src/main/java/com/oblixorprime/iedctrtest/factoryplacer/CrateLabelValidation.java').exists())
        self.assertIn("systemProperty 'iedctr.crateLabelPackagedNative', 'true'",(root/'build.gradle').read_text())


    def test_historical_provenance_contract_is_preserved(self):
        import re
        root=Path(__file__).resolve().parents[2]
        source=(root/'src/factoryPlacerNativeValidation/java/com/oblixorprime/iedctrtest/factoryplacer/PackagedRuntimeProof.java').read_text()
        baseline=re.search(r'productionTypes = .*?java\.util\.List\.of\((.*?)\)\);',source,re.S)
        self.assertIsNotNone(baseline)
        self.assertEqual(len(re.findall(r'\.class',baseline.group(1))),6)
        self.assertNotIn('LabeledCrateEditScreen',baseline.group(1))
        self.assertRegex(source,r'if \(Boolean.getBoolean\("iedctr.crateLabelPackagedNative"\)\) \{\s*productionTypes.add\(com.oblixorprime.engineersdecorreforged.client.LabeledCrateEditScreen.class\);\s*\}')

    def test_final_commit_identity_checked(self):
        source=(Path(__file__).with_name('crate_label_native.py')).read_text()
        self.assertIn("final_sha = subprocess.check_output(['git','rev-parse','HEAD']",source)
        self.assertIn('clean() and final_sha == sha and hashlib.sha256(jar.read_bytes()).hexdigest()==jar_sha',source)


if __name__=='__main__':unittest.main()
