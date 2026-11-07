"""Synthetic adversarial checks, never claims of real cow rendering/gameplay."""
import copy
import hashlib
from pathlib import Path
import tempfile
import unittest
from test_milking_qualification_native import png
from tfc_milking_recovery_native import *

class NativeRecoveryEvidenceTest(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup)
        self.game=Path(self.temp.name)/'run';folder=self.game/'tfc-milking-recovery-screenshots';folder.mkdir(parents=True)
        self.nonce='a'*32;self.sha='b'*40;self.logs=f'IEDCTR_TFC_MILKING_RECOVERY PASSED nonce={self.nonce}'
        g=expected_geometry(1280,720,2)
        self.p=dict(suite=SUITE,nonce=self.nonce,executed_source_sha=self.sha,status='passed_bounded_smoke',source_background_sha256=BACKGROUND_SHA,
            source_milk_sha256=MILK_SHA,actual_integrated_world=True,native_visual_acceptance=False,requested_language='fr_fr',selected_language='fr_fr',
            screen_class=SCREEN_CLASS,tfc_version='4.2.12',config_restored=True,native_tick_production=True,produced_tick_mutated_after_setup=False,captures=[])
        self.p.update({k:v for k,v in g.items() if not k.startswith(('tank_','presentation_'))})
        def snap(amount,uses,produced,calendar):
            r=max(0,produced+400-calendar)
            return dict(source='tfc:cow',client_source='TFC',server_source='TFC',client_name='Native TFC Cow',server_name='Native TFC Cow',
                client_uuid='12345678-1234-1234-1234-123456789abc',server_uuid='12345678-1234-1234-1234-123456789abc',client_entity_id=2,server_entity_id=2,
                amount=amount,client_amount=amount,uses=uses,produced_tick=produced,calendar_tick=calendar,remaining_ticks=r,client_remaining_ticks=r,
                total_ticks=400,client_total_ticks=400,ready=r==0,server_state='READY' if not r else 'RECOVERING',client_state='READY' if not r else 'RECOVERING')
        for i,(phase,args) in enumerate(zip(PHASES,[(0,0,0,500),(1000,1,520,540),(1000,1,520,620),(2000,2,920,940)])):
            s=snap(*args);s.update({k:v for k,v in g.items() if not k.startswith(('tank_','presentation_'))});s.update(menu_id=1,server_menu_id=1,screen_class=SCREEN_CLASS,phase=phase,name=phase+'.png',portrait_name=phase+'-portrait.png',portrait_x=(g['presentation_gui_left']+27)*2,
                portrait_y=(g['presentation_gui_top']+34)*2,portrait_width=94,portrait_height=82)
            pixels=bytes(range(256))*(1280*720*4//256)
            pixels=bytes([i])+pixels[1:]
            crop=crop_pixels(pixels,1280,s['portrait_x'],s['portrait_y'],94,82)
            for name,data,key in [(s['name'],png(1280,720,pixels),'sha256'),(s['portrait_name'],png(94,82,crop),'portrait_sha256')]:
                (folder/name).write_bytes(data);s[key]=hashlib.sha256(data).hexdigest()
            if phase=='recovering-tooltip':s.update(tooltip_hovered=True,hover_x=g['presentation_gui_left']+30,hover_y=g['presentation_gui_top']+118)
            self.p['captures'].append(s)
        self.p['no_repeat']=dict(first=snap(1000,1,520,540),later=snap(1000,1,520,620))

    def validate(self):return validate_proof(self.p,self.nonce,self.sha,self.game,self.logs,0,False)
    def test_synthetic_valid(self):self.validate()
    def test_reject_wrong_top_level(self):
        for key,value in dict(nonce='c'*32,executed_source_sha='c'*40,tfc_version='4.2.11',config_restored=False,native_tick_production=False,
                produced_tick_mutated_after_setup=True,native_visual_acceptance=True,source_background_sha256='c'*64).items():
            with self.subTest(key=key):
                old=self.p[key];self.p[key]=value
                with self.assertRaises(SmokeFailure):self.validate()
                self.p[key]=old
    def test_reject_source_and_timer_fabrication(self):
        c=self.p['captures'][1]
        for key,value in dict(client_uuid='12345678-1234-1234-1234-123456789abd',client_entity_id=3,source='minecraft:cow',client_source='VANILLA',
                server_name='Other cow',server_state='READY',client_state='READY',remaining_ticks=100,total_ticks=120,client_remaining_ticks=0,uses=True,amount=2000,produced_tick=0).items():
            with self.subTest(key=key):
                old=c[key];c[key]=value
                with self.assertRaises(SmokeFailure):self.validate()
                c[key]=old
    def test_reject_missing_capture(self):
        self.p['captures'].pop()
        with self.assertRaises(SmokeFailure):self.validate()
    def test_reject_no_second_production(self):
        self.p['captures'][-1]['uses']=1
        with self.assertRaises(SmokeFailure):self.validate()
    def test_reject_missing_native_time(self):
        self.p['no_repeat']['later']=copy.deepcopy(self.p['no_repeat']['first'])
        with self.assertRaises(SmokeFailure):self.validate()
    def test_reject_wrong_hover(self):
        self.p['captures'][2]['hover_x']=0
        with self.assertRaises(SmokeFailure):self.validate()
    def test_reject_missing_capture_bytes(self):
        (self.game/'tfc-milking-recovery-screenshots/ready.png').unlink()
        with self.assertRaises(SmokeFailure):self.validate()
    def test_reject_unclean_exit(self):
        with self.assertRaises(SmokeFailure):validate_proof(self.p,self.nonce,self.sha,self.game,self.logs,1,False)
    def test_reject_stale_log(self):
        self.logs=''
        with self.assertRaises(SmokeFailure):self.validate()
    def test_runner_requires_native_profile(self):
        code=Path(__file__).with_name('tfc_milking_recovery_native.py').read_text()
        self.assertIn("'-Pinclude_tfc_milking_runtime=true'",code)
        self.assertIn('source=current_candidate(project)',code)
        self.assertIn('current_candidate(project)==source',code)

if __name__=='__main__':unittest.main()
