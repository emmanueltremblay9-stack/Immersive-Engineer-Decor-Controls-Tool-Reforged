"""Contract/mutation tests, not Minecraft rendering or screenshot acceptance."""
import copy
from pathlib import Path
import unittest
from unittest.mock import patch
import heavy_native as native
from client_mixin_smoke import SmokeFailure
ROOT=Path(__file__).resolve().parents[2]

class HeavyEvidenceTests(unittest.TestCase):
    def proof(self):
        captures=[]
        for i,name in enumerate(native.CASES):
            captures.append(dict(case=name,capture='heavy-captures/'+name+'.png',sha256='a'*64,
                                 baked_quads_sha256=f'{i:064x}',quad_count=20,stable_ticks=40,
                                 rendered_frames=40,client_synced=True))
            captures[-1].update(axis=['down','up','north','south','west','east'][i//4] if i<24 else 'up',
                                roll=i%4 if i<24 else 0,
                                kind='isolated' if i<24 else ('anchor' if i<30 else ['axial','remove','branch','remove'][i-30]),
                                anchor_mask=(1 << (i-24)) if 24<=i<30 else 0,
                                axial_mask=2 if i==30 else 0,junction_mask=32 if i==32 else 0)
        return dict(suite='native-heavy-scenes-v1',status='passed_bounded_capture',nonce='b'*32,
                    executed_source_sha='c'*40,actual_integrated_world=True,visual_acceptance=False,
                    pixel_ctm_verified=False,physical_item_placement=False,mesh_refresh_acceptance=False,
                    width=1280,height=720,selected_language='en_us',scene_setup='server setBlock; no runtime/art edits',captures=captures,
                    packaged_runtime=dict(jar_path=str(Path('/tmp/heavy-test.jar').resolve()),jar_sha256='d'*64,
                        fixture_free_jar=True,separate_harness_module=True,active_models=[{} for _ in range(86)],active_textures=[{} for _ in range(3)],class_origins=[
                            dict({'class':'com.oblixorprime.engineersdecorreforged.'+name}) for name in
                            ['block.HeavyDoubleTSupportBlock','block.HeavyDoubleTTopology','block.HeavyDoubleTOrientation','client.HeavyDoubleTConnectedModels']]))
    def validate(self,p):
        native.validate(p,'b'*32,'c'*40,Path('/tmp/heavy-test.jar'),'d'*64,Path('/tmp/evidence'),1280,720,'en_us')
    @patch.object(native,'png')
    def test_valid_bounded_proof_and_exact_coverage(self,png):
        self.validate(self.proof());self.assertEqual(png.call_count,34)
        self.assertEqual(len(set(native.CASES)),34)
    @patch.object(native,'png')
    def test_identity_and_overclaim_mutations_rejected(self,png):
        for key,value in [('nonce','e'*32),('executed_source_sha','f'*40),('visual_acceptance',True),
                          ('pixel_ctm_verified',True),('mesh_refresh_acceptance',True),('width',640),('actual_integrated_world',False)]:
            p=self.proof();p[key]=value
            with self.subTest(key=key),self.assertRaises(SmokeFailure):self.validate(p)
    @patch.object(native,'png')
    def test_missing_duplicate_and_reordered_cases_rejected(self,png):
        for mode in ['missing','duplicate','reverse']:
            p=self.proof()
            if mode=='missing':p['captures'].pop()
            elif mode=='duplicate':p['captures'][1]=copy.deepcopy(p['captures'][0])
            else:p['captures'].reverse()
            with self.subTest(mode=mode),self.assertRaises(SmokeFailure):self.validate(p)
    @patch.object(native,'png')
    def test_unsettled_client_missing_quads_and_traversal_rejected(self,png):
        for key,value in [('stable_ticks',39),('rendered_frames',39),('client_synced',False),
                          ('quad_count',0),('baked_quads_sha256','bad'),('capture','../outside.png'),('axis','other'),('anchor_mask',2),('roll',True)]:
            p=self.proof();p['captures'][0][key]=value
            with self.subTest(key=key),self.assertRaises(SmokeFailure):self.validate(p)
    @patch.object(native,'png')
    def test_removal_requires_client_baked_geometry_change(self,png):
        for before,after in [(30,31),(32,33)]:
            p=self.proof();p['captures'][after]['baked_quads_sha256']=p['captures'][before]['baked_quads_sha256']
            with self.assertRaises(SmokeFailure):self.validate(p)
    @patch.object(native,'png')
    def test_production_harness_identity_rejected(self,png):
        for key,value in [('fixture_free_jar',False),('separate_harness_module',False),('jar_sha256','e'*64),('class_origins',[])]:
            p=self.proof();p['packaged_runtime'][key]=value
            with self.subTest(key=key),self.assertRaises(SmokeFailure):self.validate(p)
    def test_fixture_isolation_and_native_not_mocked(self):
        gradle=(ROOT/'gradle/heavy-packaged-validation.gradle').read_text()
        for guard in ['sourceSets.main.runtimeClasspath - sourceSets.main.output','fixture_free_jar: true','split_packages: false','sha256(jarFile) != sha256(normalJar)']:
            self.assertIn(guard,gradle)
        java=(ROOT/'src/heavyPackagedValidation/java/com/oblixorprime/iedctrtest/heavy/NativeHeavyValidation.java').read_text()
        for term in ['createFreshLevel','getSingleplayerServer().submit','getBlockModel(blockState)','getModelData(mc.level','RenderLevelStageEvent.Stage.AFTER_LEVEL','Screenshot.takeScreenshot','previousModelHash','clientMatches','"mesh_refresh_acceptance",false']:
            self.assertIn(term,java)
        self.assertNotIn('levelRenderer.allChanged',java)
        self.assertNotIn('requestModelDataUpdate',java)
        self.assertIn('preserve the existing center state',java)

if __name__=='__main__':unittest.main()
