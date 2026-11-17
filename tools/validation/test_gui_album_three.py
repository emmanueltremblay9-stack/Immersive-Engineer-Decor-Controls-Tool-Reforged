import copy
from pathlib import Path
import unittest
from unittest.mock import patch
from gui_album_three import validate, validate_selection_pixels, CASES, SCREENS, SmokeFailure
class ProofTests(unittest.TestCase):
    def setUp(self):
        self.jar=Path('/tmp/album.jar');self.digest='a'*64
        self.proof=dict(suite='gui-album-three-v1',status='observed',nonce='b'*32,executed_source_sha='c'*40,visual_acceptance=False,physical_os_input=False,
            packaged_runtime=dict(jar_path=str(self.jar.resolve()),jar_sha256=self.digest,fixture_free_jar=True,separate_harness_module=True),
            cases=[dict(case=n,screen='com.oblixorprime.engineersdecorreforged.client.'+('MachineScreen$' if i!=4 else '')+SCREENS[i],server_menu_acknowledged=True,gui_scale=2,visible_nonempty_slots=3,screen_class_sha256='d'*64,capture=n+'.png',capture_sha256='e'*64) for i,n in enumerate(CASES)])
        self.proof['cases'][0].update(visible_nonempty_slots=54,server_selection=0,client_selection=0,ring_x=10,ring_y=8,
            selection_source='server-seeded ContainerData; not user selection',selection_render_frames=3)
        self.proof['cases'][5]['visible_nonempty_slots']=48
        self.proof['hopper_selections']=[dict(self.proof['cases'][0],server_selection=row*6,client_selection=row*6,ring_y=8+18*row,
            capture='factory-hopper'+('' if row==0 else '-selection-row-'+str(row))+'.png') for row in range(3)]
    def check(self,p):
        with patch('gui_album_three.png') as png, patch('gui_album_three.validate_selection_pixels'):
            validate(p,'b'*32,'c'*40,self.jar,self.digest,Path('/tmp/run'),1280,720)
            self.assertEqual(png.call_count,9)
    def test_accept_structural_proof(self):self.check(self.proof)
    def test_reject_missing_screen(self):
        self.proof['cases'].pop()
        with self.assertRaises(SmokeFailure):self.check(self.proof)
    def test_reject_bad_hash(self):
        self.proof['packaged_runtime']['jar_sha256']='f'*64
        with self.assertRaises(SmokeFailure):self.check(self.proof)
    def test_reject_wrong_provenance(self):
        self.proof['cases'][0]['screen_class_sha256']=''
        with self.assertRaises(SmokeFailure):self.check(self.proof)
    def test_reject_human_claim(self):
        self.proof['physical_os_input']=True
        with self.assertRaises(SmokeFailure):self.check(self.proof)
    def test_reject_unsynchronized_menu(self):
        self.proof['cases'][2]['server_menu_acknowledged']=False
        with self.assertRaises(SmokeFailure):self.check(self.proof)

    def test_reject_wrong_scale_or_empty_visual_evidence(self):
        for key,value in [('gui_scale',1),('visible_nonempty_slots',0)]:
            p=copy.deepcopy(self.proof);p['cases'][0][key]=value
            with self.subTest(key=key),self.assertRaises(SmokeFailure):self.check(p)

    def test_reject_selection_drift_and_partial_inventory(self):
        for key,value in [('server_selection',0),('client_selection',0),('ring_y',25),('ring_x',11),('selection_render_frames',0),('visible_nonempty_slots',53),('selection_source','client fake')]:
            p=copy.deepcopy(self.proof);p['hopper_selections'][1][key]=value
            with self.subTest(key=key),self.assertRaises(SmokeFailure):self.check(p)
        p=copy.deepcopy(self.proof);p['cases'][5]['visible_nonempty_slots']=47
        with self.assertRaises(SmokeFailure):self.check(p)

    def test_production_hopper_ring_uses_real_slot_pitch(self):
        root=Path(__file__).resolve().parents[2]
        source=(root/'src/main/java/com/oblixorprime/engineersdecorreforged/client/MachineScreen.java').read_text()
        method=source.split('private void drawFactoryHopperWidgets(',1)[1].split('private ',1)[0]
        self.assertIn('x0 + 10 + slotIndex % 6 * 18, y0 + 8 + slotIndex / 6 * 18',method)
        self.assertNotIn('slotIndex / 6 * 17',method)

    def test_selection_pixel_guard_rejects_old_pitch_and_missing_ring(self):
        width,height,scale=640,480,2
        left=(width//scale-176)//2;top=(height//scale-166)//2
        def pixels(pitch,missing=False):
            outputs=[]
            for row in range(3):
                data=bytearray(width*height*4)
                if not missing:
                    for edge in (8+pitch*row,25+pitch*row):
                        for y in range((top+edge)*scale,(top+edge+1)*scale):
                            for x in [v for column in ((0,1,2,16,17) if edge==8+pitch*row else (0,1,16,17)) for v in range((left+10+column)*scale,(left+11+column)*scale)]:
                                data[(y*width+x)*4:(y*width+x)*4+3]=b'\xff\xff\xff'
                outputs.append((width,height,bytes(data)))
            return outputs
        records=[dict(capture=str(i)+'.png') for i in range(3)]
        with patch('gui_album_three.decode_png',side_effect=pixels(18)):
            validate_selection_pixels(records,Path('/tmp'),width,height,scale)
        for images in (pixels(17),pixels(18,True)):
            with patch('gui_album_three.decode_png',side_effect=images),self.assertRaises(SmokeFailure):
                validate_selection_pixels(records,Path('/tmp'),width,height,scale)
