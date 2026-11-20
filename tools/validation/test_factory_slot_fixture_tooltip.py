"""Proof-validator contract only. Pixel doubles are not native game evidence."""
import copy
from pathlib import Path
import unittest
from unittest.mock import patch

from factory_control_tooltip_native import fixture_coordinates, validate_fixture, SmokeFailure


class SlotFixtureProof(unittest.TestCase):
    def sample(self,machine,scale):
        width,height=1280,720
        j=Path('fixture.jar').resolve();g=Path('fixture-run')
        p=dict(suite='native-factory-control-tooltip-v2',status='passed',nonce='a'*32,executed_source_sha='b'*40,
               width=width,height=height,selected_language='en_us',machine=machine,presentation_selector_absent=True,
               opened_via_native_block_interaction=1,native_visual_acceptance=False,state_change_server_acks=0,captures=[],
               packaged_runtime=dict(jar_path=str(j),jar_sha256='c'*64,fixture_free_jar=True,separate_harness_module=True),fixture_captures=[])
        for name in ['empty','occupied']:
            slots=[]
            for i,(x,y) in enumerate(fixture_coordinates(machine)):
                if name=='empty':item,n='air',0
                elif machine=='dropper' and 12<=i<15:item,n=['redstone','diamond','emerald'][i-12],1
                else:item,n=['stone','gold_ingot','iron_ingot','bread'][i%4],2+i%61
                slots.append(dict(x=x,y=y,item='minecraft:'+item,count=n))
            w,h=(176,166) if machine=='hopper' else (228,250)
            snapshot=dict(logic=2,container_id=1,slots=slots,filters=([0 if name=='empty' else 1]*3 if machine=='dropper' else []))
            p['fixture_captures'].append(dict(name=name,file='factory-fixture-'+name+'.png',sha256='d'*64,
                gui_scale=scale,gui_width=width//scale,gui_height=height//scale,render_frames=3,tooltip_absent=True,
                panel_x=(width//scale-w)//2,panel_y=(height//scale-h)//2,client=snapshot,server=copy.deepcopy(snapshot)))
        return p,('a'*32,'b'*40,j,'c'*64,g,width,height,'en_us',machine,scale)

    def checked(self,p,args,visible=True):
        width,height=args[5:7]; n=width*height*4
        if visible=='digits':
            scale=args[-1];e=p['fixture_captures'][1];out=bytearray(n)
            for x,y in fixture_coordinates(args[-2]):
                for yy in range((e['panel_y']+y+11)*scale,(e['panel_y']+y+16)*scale):
                    start=(yy*width+(e['panel_x']+x+6)*scale)*4
                    out[start:start+10*scale*4]=bytes([1])*10*scale*4
        else:out=bytes([int(visible)])*n
        with patch('factory_control_tooltip_native.png'),patch('factory_control_tooltip_native.decode_png',side_effect=[(0,0,bytes(n)),(0,0,out)]):
            validate_fixture(p,*args)

    def test_each_machine_has_two_scale_profiles(self):
        for m in ('hopper','dropper'):
            for s in (1,2):
                with self.subTest(machine=m,scale=s):self.checked(*self.sample(m,s))

    def test_missing_render_witness_rejected(self):
        p,args=self.sample('dropper',2)
        with self.assertRaisesRegex(SmokeFailure,'No rendered item witness'):self.checked(p,args,False)
        with self.assertRaisesRegex(SmokeFailure,'No rendered item witness'):self.checked(p,args,'digits')

    def test_reject_wrong_observed_scale_and_unsynced_menu(self):
        p,args=self.sample('hopper',2)
        for key,value in [('gui_scale',1),('server',{}),('panel_x',1)]:
            q=copy.deepcopy(p);q['fixture_captures'][1][key]=value
            with self.subTest(key=key),self.assertRaises(SmokeFailure):self.checked(q,args)

    def test_reject_bad_filter_and_shifted_hitbox(self):
        p,args=self.sample('dropper',1)
        for key,value in [('item','minecraft:stone'),('y',83)]:
            q=copy.deepcopy(p)
            for side in ('client','server'):q['fixture_captures'][1][side]['slots'][12][key]=value
            with self.subTest(key=key),self.assertRaises(SmokeFailure):self.checked(q,args)
