import copy
from pathlib import Path
import unittest
from unittest.mock import patch
from modernui_native_diagnostic import validate,MODERNUI_SHA512,SmokeFailure
ROOT=Path(__file__).resolve().parents[2]
class ModernUIProofTests(unittest.TestCase):
    def proof(self,on):
        names=['editor','machine-container','vanilla-chest']
        screens=['com.oblixorprime.engineersdecorreforged.client.LabeledCrateEditScreen','com.oblixorprime.engineersdecorreforged.client.MachineScreen$SmallElectricalFurnaceScreen','net.minecraft.client.gui.screens.inventory.ContainerScreen']
        return dict(suite='modernui-native-diagnostic-v1',status='observed',nonce='a'*32,executed_source_sha='b'*40,visual_acceptance=False,physical_os_input=False,
            packaged_runtime=dict(jar_path=str(Path('test.jar').resolve()),jar_sha256='c'*64,fixture_free_jar=True,separate_harness_module=True),
            cases=[dict(case=name,screen=screens[i],screen_class_sha256='d'*64,capture=name+'.png',capture_sha256='e'*64,
                modernui_jar_sha512=MODERNUI_SHA512,additional_blur_config=on,additional_blur_effective=on,
                server_acknowledged_interaction=True,blur_process_call_instrumented=False,mBlurring=on,mBlurEffect=True) for i,name in enumerate(names)])
    def check(self,p,on):
        with patch('modernui_native_diagnostic.png') as png:
            validate(p,'a'*32,'b'*40,Path('test.jar'),'c'*64,Path('run'),1280,720,on)
            self.assertEqual(png.call_count,3)
    def test_valid_both_config_profiles(self):
        for on in (False,True):self.check(self.proof(on),on)
    def test_wrong_settings_shader_and_interactions_rejected(self):
        for field,value in [('additional_blur_config',False),('additional_blur_effective',False),('mBlurring',False),('mBlurEffect',False),('server_acknowledged_interaction',False),('modernui_jar_sha512','x'),('screen','fake'),('capture','../fake.png'),('screen_class_sha256','')]:
            p=self.proof(True);p['cases'][0][field]=value
            with self.subTest(field=field),self.assertRaises(SmokeFailure):self.check(p,True)
    def test_missing_screen_and_wrong_source_rejected(self):
        p=self.proof(False);p['cases'].pop()
        with self.assertRaises(SmokeFailure):self.check(p,False)
        p=self.proof(False);p['executed_source_sha']='0'*40
        with self.assertRaises(SmokeFailure):self.check(p,False)
    def test_source_is_separate_and_observer_does_not_force_blur(self):
        driver=(ROOT/'tools/validation/modernui_native_diagnostic.py').read_text()
        java=(ROOT/'src/modernuiDiagnostic/java/com/oblixorprime/iedctrtest/factoryplacer/ModernUIDiagnostic.java').read_text()
        self.assertIn('config/ModernUI/client.toml',driver)
        self.assertIn("systemProperty 'iedctr.crateLabelPackagedNative', 'false'",driver)
        self.assertIn("'factoryPlacerNativeValidationRuntimeOnly'",driver)
        self.assertIn('PackagedRuntimeProof.verify(mc)',java)
        self.assertNotIn('.setBoolean(',java)
        self.assertNotIn('sBlurForVanillaScreens =',java)
if __name__=='__main__':unittest.main()
