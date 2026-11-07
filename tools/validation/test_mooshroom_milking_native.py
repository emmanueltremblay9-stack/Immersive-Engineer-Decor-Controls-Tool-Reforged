"""Synthetic rejection tests only; these never establish gameplay or visual acceptance."""
import copy
import hashlib
from pathlib import Path
import tempfile
import unittest
from test_milking_qualification_native import png
from mooshroom_milking_native import *

class MooshroomEvidenceTest(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup)
        self.game=Path(self.temp.name)/'run';folder=self.game/'mooshroom-milking-screenshots';folder.mkdir(parents=True)
        self.nonce='a'*32;self.sha='b'*40;self.logs=f'IEDCTR_MOOSHROOM_MILKING PASSED nonce={self.nonce}'
        g=expected_geometry(1280,720,2)
        geometry={k:v for k,v in g.items() if not k.startswith(('tank_','presentation_'))}
        self.p=dict(suite=SUITE,nonce=self.nonce,executed_source_sha=self.sha,status='passed_bounded_smoke',source_background_sha256=BACKGROUND_SHA,
            source_milk_sha256=MILK_SHA,actual_integrated_world=True,native_visual_acceptance=False,requested_language='fr_fr',selected_language='fr_fr',
            screen_class=SCREEN_CLASS,tfc_version='4.2.12',native_tick_production=True,captures=[],production_checks=[],baby_checks=[],**geometry)
        for i,phase in enumerate(PHASES):
            variant='RED' if 'red' in phase else 'BROWN';baby=phase.startswith('baby-');hover=phase.endswith('-identity-tooltip')
            entity=(4 if variant=='RED' else 5) if baby else (2 if variant=='RED' else 3)
            uid=str(uuid.UUID(int=entity))
            s=dict(source='minecraft:mooshroom',variant=variant,server_source='NONE' if baby else 'VANILLA',client_source='NONE' if baby else 'VANILLA',
                server_uuid=uid,client_uuid=uid,server_entity_id=entity,client_entity_id=entity,server_name='Native '+variant+' Mooshroom',client_name='Native '+variant+' Mooshroom',
                amount=0 if baby else 1000,client_amount=0 if baby else 1000,remaining_ticks=0,client_remaining_ticks=0,total_ticks=0,client_total_ticks=0,
                client_state_code=0 if baby else 1,phase=phase,name=phase+'.png',portrait_name=phase+'-portrait.png',portrait_x=(g['presentation_gui_left']+27)*2,
                portrait_y=(g['presentation_gui_top']+34)*2,portrait_width=94,portrait_height=82,menu_id=entity,server_menu_id=entity,screen_class=SCREEN_CLASS,**geometry)
            s.update(server_selected_entity_id=entity,client_selected_entity_id=entity,server_selected_uuid=uid,client_selected_uuid=uid)
            if baby:s.update(baby_rejected=True,server_selected_entity_id=0,client_selected_entity_id=0,server_selected_uuid=None,client_selected_uuid=None)
            hx=g['presentation_gui_left']+50 if hover else 638;hy=g['presentation_gui_top']+45 if hover else 358
            s.update(tooltip_hovered=hover,hover_x=hx,hover_y=hy,native_input=dict(glfw_focused=True,glfw_visible=True,
                glfw_cursor_mode=212993,glfw_cursor_x=hx*2,glfw_cursor_y=hy*2,minecraft_cursor_x=hx*2,minecraft_cursor_y=hy*2,
                window_width=1280,window_height=720,settled_hover_ticks=8 if hover else 0))
            pixels=bytearray(bytes(range(256))*(1280*720*4//256));pixels[0]=i
            pixels[(s['portrait_y']*1280+s['portrait_x'])*4]=entity
            crop=crop_pixels(pixels,1280,s['portrait_x'],s['portrait_y'],94,82)
            for name,data,key in [(s['name'],png(1280,720,bytes(pixels)),'sha256'),(s['portrait_name'],png(94,82,crop),'portrait_sha256')]:
                (folder/name).write_bytes(data);s[key]=hashlib.sha256(data).hexdigest()
            self.p['captures'].append(s)
        for v in ('RED','BROWN'):
            self.p['production_checks'].append(dict(variant=v,bucket=True,tank_amount=1000,cadence_ticks=120,first_tick=120,second_tick=240,no_early_repeat=True))
            self.p['baby_checks'].append(dict(variant=v,baby=True,observed_ticks=240,amount=0,bucket_input=1,bucket_output=0,source='NONE'))

    def validate(self):return validate_proof(self.p,self.nonce,self.sha,self.game,self.logs,0,False)
    def reject_changes(self,obj,changes):
        for k,v in changes.items():
            with self.subTest(key=k,value=v):
                old=obj[k];obj[k]=v
                with self.assertRaises(SmokeFailure):self.validate()
                obj[k]=old
    def test_synthetic_valid(self):self.validate()
    def test_reject_wrong_run(self):
        self.reject_changes(self.p,dict(nonce='c'*32,executed_source_sha='c'*40,tfc_version='4.2.11',native_tick_production=False,
            native_visual_acceptance=True,actual_integrated_world=False,source_background_sha256='c'*64))
    def test_reject_wrong_animal(self):
        self.reject_changes(self.p['captures'][0],dict(source='minecraft:cow',variant='BROWN',client_uuid=str(uuid.UUID(int=9)),client_entity_id=9,
            client_name='Cow',server_selected_entity_id=9,client_selected_uuid=str(uuid.UUID(int=9)),server_source='TFC',remaining_ticks=120,total_ticks=120,client_state_code=2,amount=2000))
    def test_reject_baby_selected(self):
        self.reject_changes(self.p['captures'][4],dict(baby_rejected=False,server_selected_entity_id=9,client_selected_uuid=str(uuid.UUID(int=9)),amount=1000,client_state_code=1))
    def test_reject_missing_explicit_null(self):
        del self.p['captures'][4]['server_selected_uuid']
        with self.assertRaises(SmokeFailure):self.validate()
    def test_reject_changed_pair_menu(self):
        self.p['captures'][1].update(menu_id=9,server_menu_id=9)
        with self.assertRaises(SmokeFailure):self.validate()
    def test_reject_reused_scenario_menu(self):
        for c in self.p['captures'][2:4]:c.update(menu_id=2,server_menu_id=2)
        with self.assertRaises(SmokeFailure):self.validate()
    def test_reject_wrong_production(self):
        self.reject_changes(self.p['production_checks'][0],dict(bucket=False,tank_amount=999,cadence_ticks=119,second_tick=239,no_early_repeat=False,first_tick=True))
    def test_reject_wrong_baby_observation(self):
        self.reject_changes(self.p['baby_checks'][0],dict(baby=False,observed_ticks=239,amount=1000,bucket_input=0,bucket_output=1,source='VANILLA'))
    def test_reject_missing_capture(self):
        self.p['captures'].pop()
        with self.assertRaises(SmokeFailure):self.validate()
    def test_reject_duplicate_variant(self):
        self.p['production_checks'][1]['variant']='RED'
        with self.assertRaises(SmokeFailure):self.validate()
    def test_reject_wrong_hover(self):
        self.reject_changes(self.p['captures'][1],dict(tooltip_hovered=False,hover_x=320,hover_y=180))
    def test_reject_bad_native_input(self):
        self.reject_changes(self.p['captures'][1]['native_input'],dict(glfw_focused=False,glfw_visible=False,glfw_cursor_mode=212995,
            settled_hover_ticks=7,minecraft_cursor_x=640,glfw_cursor_x=640,window_width=640))
    def test_reject_nonfinite_cursor(self):
        self.reject_changes(self.p['captures'][1]['native_input'],dict(glfw_cursor_x=float('nan'),minecraft_cursor_y=float('inf')))
    def test_reject_unhovered_tooltip_claim(self):
        self.p['captures'][0]['tooltip_hovered']=True
        with self.assertRaises(SmokeFailure):self.validate()
    def test_reject_missing_file(self):
        (self.game/'mooshroom-milking-screenshots/red-portrait.png').unlink()
        with self.assertRaises(SmokeFailure):self.validate()
    def test_reject_adult_using_baby_background(self):
        adult=self.p['captures'][0];baby=self.p['captures'][4]
        folder=self.game/'mooshroom-milking-screenshots'
        w,h,pixels=decode_png(folder/adult['name']);pixels=bytearray(pixels)
        _,_,crop=decode_png(folder/baby['portrait_name'])
        for y in range(adult['portrait_height']):
            start=((adult['portrait_y']+y)*w+adult['portrait_x'])*4
            size=adult['portrait_width']*4
            pixels[start:start+size]=crop[y*size:(y+1)*size]
        for name,data,key in ((adult['name'],png(w,h,bytes(pixels)),'sha256'),
                (adult['portrait_name'],png(adult['portrait_width'],adult['portrait_height'],crop),'portrait_sha256')):
            (folder/name).write_bytes(data);adult[key]=hashlib.sha256(data).hexdigest()
        with self.assertRaises(SmokeFailure):self.validate()
    def test_reject_wrong_crop_hash(self):
        self.p['captures'][0]['portrait_sha256']='c'*64
        with self.assertRaises(SmokeFailure):self.validate()
    def test_reject_exit_and_marker(self):
        for code,timeout,logs in ((1,False,self.logs),(0,True,self.logs),(0,False,'')):
            with self.subTest(code=code,timeout=timeout,logs=logs):
                with self.assertRaises(SmokeFailure):validate_proof(self.p,self.nonce,self.sha,self.game,logs,code,timeout)

if __name__=='__main__':unittest.main()
