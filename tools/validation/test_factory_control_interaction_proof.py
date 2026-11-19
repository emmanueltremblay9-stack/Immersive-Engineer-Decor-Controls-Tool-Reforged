"""Synthetic rejection tests, not native execution evidence."""
import copy
import json
import unittest
from unittest.mock import patch
from pathlib import Path
from factory_control_interaction_proof import validate_interaction, POINTS
from client_mixin_smoke import SmokeFailure
class InteractionTests(unittest.TestCase):
    def fixture(self,machine='hopper'):
        def state(carried=64,drops=0):return dict(carried=carried,slots=0,drops=drops,total=64,fields=[0]*16,container_id=1,slot_counts=[0]*(54 if machine=='hopper' else 51),player_inventory=[0]*41,entity_count=drops)
        points=[(x,y,b) for x,y in POINTS[machine] for b in (1,3)]+[(-8,83 if machine=='hopper' else 125,3)]
        panel=(176,166) if machine=='hopper' else (228,250)
        left,top=(1280-panel[0])//2,(720-panel[1])//2
        cases=[]
        for i,(x,y,b) in enumerate(points):
            after=state(63,1) if i==16 else state()
            cases.append(dict(x=x,y=y,button=b,mc_input_action_index=i,mc_button=0 if b==1 else 1,consumed_press_count=1,consumed_release_count=1,render_frames_before=3,render_frames_after=3,server_ack=True,pointer_ack=True,xtest_press_release_ack=True,settle_ticks=12,pointer_x=left+x,pointer_y=top+y,before_file=f'factory-interaction-before-{i}.png',before_sha256='b'*64,before=state(),after=after,client_after=copy.deepcopy(after),file=f'factory-interaction-{i}.png',sha256='a'*64))
        pickup_before=state(0);pickup_before['slots']=64;pickup_before['player_inventory'][9]=64
        pickup=dict(mc_input_action_index=-1,mc_button=0,consumed_press_count=1,consumed_release_count=1,render_frames_before=3,render_frames_after=3,server_ack=True,pointer_ack=True,xtest_press_release_ack=True,button=1,before=pickup_before,after=state(),file='factory-interaction-pickup.png',before_file='factory-interaction-before-pickup.png',sha256='a'*64,before_sha256='b'*64)
        return dict(interaction=dict(pickup=pickup,entity_mutation=False,panel_width=panel[0],panel_height=panel[1],panel_left=left,panel_top=top,input_method='XTest',fixture_item='minecraft:stone',initial_total=64,gui_scale=1,cases=cases))
    def check(self,p,machine='hopper'):
        with patch('factory_control_interaction_proof.png'):
            validate_interaction(p,Path('run'),1280,720,machine)
    def test_points_do_not_touch_item_envelopes(self):
        root=Path(__file__).resolve().parents[2]
        for machine,points in POINTS.items():
            mapping=json.loads((root/'docs/qualification/factory-steel-v2'/f'{machine}-mapping.json').read_text())
            slots=sum((mapping[k] for k in ('machine_slots','player_slots','hotbar_slots')),[])
            for x,y in points:
                self.assertFalse(any(s['x']-1<=x<s['x']+17 and s['y']-1<=y<s['y']+17 for s in slots))
    def test_points_avoid_control_envelopes(self):
        controls={'hopper':[(126,1,49,60)],'dropper':[(151,31,11,31),(168,31,32,31),(154,65,47,10),(154,76,47,10),(122,92,13,13),(143,99,18,9),(166,99,18,9),(189,99,18,9)]}
        for machine,points in POINTS.items():
            for x,y in points:
                self.assertFalse(any(a<=x<a+w and b<=y<b+h for a,b,w,h in controls[machine]))
    def test_valid(self):
        for m in POINTS:self.check(self.fixture(m),m)
    def test_missing_coverage_and_wrong_identity(self):
        for k,v in [('input_method','Screen.mouseClicked'),('initial_total',True),('gui_scale',2),('fixture_item','minecraft:dirt'),('cases',[])]:
            p=self.fixture();p['interaction'][k]=v
            with self.subTest(k=k),self.assertRaises(SmokeFailure):self.check(p)
    def test_drop_loss_duplication_and_config_mutations_rejected(self):
        for k,v in [('carried',63),('drops',1),('slots',1),('total',65),('fields',[1]*16)]:
            p=self.fixture();p['interaction']['cases'][0]['after'][k]=v
            with self.subTest(k=k),self.assertRaises(SmokeFailure):self.check(p)
    def test_wrong_target_button_or_ack_rejected(self):
        for k,v in [('x',-1),('y',100),('button',2),('server_ack',False),('file','../wrong.png')]:
            p=self.fixture();p['interaction']['cases'][0][k]=v
            with self.subTest(k=k),self.assertRaises(SmokeFailure):self.check(p)
    def test_no_outside_drop_rejected(self):
        p=self.fixture();p['interaction']['cases'][-1]['after']=p['interaction']['cases'][-1]['before'].copy()
        with self.assertRaises(SmokeFailure):self.check(p)
    def test_missing_observed_input_or_frames_rejected(self):
        for k in ('consumed_press_count','consumed_release_count','render_frames_before','render_frames_after'):
            for target in ('pickup','cases'):
                p=self.fixture();r=p['interaction'][target] if target=='pickup' else p['interaction'][target][0];r[k]=0
                with self.subTest(k=k,target=target),self.assertRaises(SmokeFailure):self.check(p)
    def test_inventory_duplication_rejected(self):
        p=self.fixture();p['interaction']['cases'][0]['after']['player_inventory'][40]=64
        with self.assertRaises(SmokeFailure):self.check(p)
    def test_client_desync_rejected(self):
        p=self.fixture();p['interaction']['cases'][0]['client_after']['carried']=63
        with self.assertRaises(SmokeFailure):self.check(p)
if __name__=='__main__':unittest.main()
