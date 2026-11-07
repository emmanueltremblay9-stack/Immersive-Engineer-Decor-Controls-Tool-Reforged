import copy
from pathlib import Path
import unittest
import os
import subprocess
import sys
import tempfile
import hashlib
import struct
import zlib
from fluid_gestures_native import ACTIONS, ROLES, STATES, SUITE, XVFB_BIND, SmokeFailure, validate_evidence, validate_proof

NONCE='a'*32
SHA='b'*40

def valid():
    p=dict(suite=SUITE,nonce=NONCE,executed_source_sha=SHA,status='passed_bounded_native',
           actual_integrated_world=True,input_backend='XTEST',invalid_input_evidence='native-attempt-and-state-conservation-only',cleanup_released=True,selected_language='en_us',
           hovers=[],gestures=[],process_seeds=[],slot_contracts=[])
    for i,machine in enumerate(['funnel','accumulator']):
        p['slot_contracts'].append(dict(machine=machine,menu_id=i+1,
            server_menu_class='com.oblixorprime.engineersdecorreforged.menu.MachineMenu',
            server_accepts_empty_bucket=True,server_rejects_water_bucket=True,backing_accepts_empty_bucket=True,
            backing_rejects_water_bucket=True,server_output_rejects_insertion=True,
            client_accepts_empty_bucket=True,client_output_rejects_insertion=True))
        seed=dict(state=[0,0,1,0,0,0,1000],total=1,menu_id=i+1)
        p['process_seeds'].append(dict(client=seed,server=copy.deepcopy(seed)))
        def row(name,x,y):
            return dict(machine=machine,name=machine+'-'+name+'.png',sha256='c'*64,width=1280,height=720,gui_scale=1,gui_width=1280,gui_height=720,window_width=1280,window_height=720,raw_mouse_x=x+512.0,raw_mouse_y=y+232.0,
                        target_x=x+512,target_y=y+232,actual_x=x+512.0,actual_y=y+232.0,xtest_motion_count=1,menu_id=i+1,
                        screen_class='com.oblixorprime.engineersdecorreforged.client.MachineScreen$'+('SmallFluidFunnelScreen' if i==0 else 'PassiveFluidAccumulatorScreen'))
        for j,role in enumerate(ROLES):
            x,y=[(128,14),(128,102) if i==0 else (128,79),(48,78) if i==0 else (48,108),
                 (208,78) if i==0 else (208,108),(126,123) if i==0 else (128,35),(48,78) if i==0 else (48,108)][j]
            r=row('hover-'+str(j),x,y);r.update(role=role,tooltip_render_events=5,tooltip_event_class="RenderTooltipEvent.Color+ScreenEvent.Render.Post");state=dict(state=[1,0,0,0,0,0,0] if j==5 else [0,0,1,0,0,0,0],total=1,menu_id=i+1);r.update(client=state,server=copy.deepcopy(state));p['hovers'].append(r)
        for j,action in enumerate(ACTIONS):
            slot=[2,0,0,2,2,0,37,1,2,2,0,2][j]
            x,y=(48 if slot==0 else 208,78 if i==0 else 108) if slot<2 else (52,146) if slot==2 else (204,214)
            r=row(action,x,y)
            state=dict(state=copy.deepcopy(STATES[j]),total=1,menu_id=i+1)
            r.update(native_press_observed=True,native_release_observed=True,native_shift_observed=j in (4,5,6),action=action,menu_slot=slot,client=state,server=copy.deepcopy(state),released=True,
                     button_events=(i*len(ACTIONS)+j+1)*2,key_events=i*6+(0 if j<4 else 2 if j==4 else 4 if j==5 else 6))
            p['gestures'].append(r)
    return p

class ProofTests(unittest.TestCase):
    def check(self,p): return validate_evidence(p,NONCE,SHA,1280,720,'en_us')
    def test_complete_proof(self): self.check(valid())
    def test_post_attempt_state_requires_independent_conservation_oracle(self):
        for state in ([0,0,0,1,0,0,0],[0,0,0,0,0,0,0],[1,0,0,0,0,0,0]):
            p=valid()
            for side in ('client','server'):
                p['gestures'][10][side]['state']=list(state)
            with self.assertRaises(SmokeFailure):self.check(p)

    def test_scaled_viewports(self):
        for width,height,scale in [(800,600,2),(960,810,3),(961,811,3)]:
            with self.subTest(width=width,height=height,scale=scale):
                p=valid();gw=(width+scale-1)//scale;gh=(height+scale-1)//scale
                for row in p['hovers']+p['gestures']:
                    row.update(width=width,height=height,gui_scale=scale,gui_width=gw,gui_height=gh,window_width=width,window_height=height)
                    row['target_x']+=(gw-256)//2-512;row['actual_x']=float(row['target_x'])
                    row['target_y']+=(gh-256)//2-232;row['actual_y']=float(row['target_y'])
                    row['raw_mouse_x']=row['actual_x']*width/gw;row['raw_mouse_y']=row['actual_y']*height/gh
                validate_evidence(p,NONCE,SHA,width,height,'en_us',scale)
                for field,value in [('gui_scale',1),('gui_width',width),('gui_height',height),('actual_x',width/2),('window_width',width+1),('raw_mouse_x',0)]:
                    altered=copy.deepcopy(p);altered['hovers'][0][field]=value
                    with self.assertRaises(SmokeFailure):validate_evidence(altered,NONCE,SHA,width,height,'en_us',scale)

    def test_reject_mutations(self):
        mutations=[
            lambda p:p.pop('invalid_input_evidence'),
            lambda p:p.update(invalid_input_evidence='verified-server-rejection'),
            lambda p:p['gestures'][10]['client']['state'].__setitem__(5,0),
            lambda p:p['gestures'][10]['server']['state'].__setitem__(5,0),
            lambda p:p['gestures'][10].update(menu_slot=1),
            lambda p:p['hovers'][0].pop('gui_scale'),
            lambda p:p['hovers'][0].update(gui_width=True),
            lambda p:p.pop('slot_contracts'),lambda p:p['slot_contracts'].pop(),
            lambda p:p['slot_contracts'][0].update(menu_id=999),
            lambda p:p['slot_contracts'][0].update(machine='accumulator'),
            lambda p:p['slot_contracts'][0].update(server_menu_class='SimpleContainer'),
            *[lambda p,key=key:p['slot_contracts'][0].pop(key) for key in
              ('server_accepts_empty_bucket','server_rejects_water_bucket','backing_accepts_empty_bucket',
               'backing_rejects_water_bucket','server_output_rejects_insertion','client_accepts_empty_bucket','client_output_rejects_insertion')],
            *[lambda p,key=key:p['slot_contracts'][0].update({key:False}) for key in
              ('server_accepts_empty_bucket','server_rejects_water_bucket','backing_accepts_empty_bucket',
               'backing_rejects_water_bucket','server_output_rejects_insertion','client_accepts_empty_bucket','client_output_rejects_insertion')],
            lambda p:p.update(status='passed'),lambda p:p.update(executed_source_sha='d'*40),
            lambda p:p.update(nonce='e'*32),lambda p:p.update(input_backend='GLFW_WARP'),
            lambda p:p.update(cleanup_released=False),lambda p:p.update(actual_integrated_world=False),
            lambda p:p['hovers'][0].update(menu_id=99),lambda p:p['process_seeds'][0]['client'].update(menu_id=99),
            lambda p:p['hovers'].pop(),lambda p:p['gestures'].pop(),
            lambda p:p['hovers'][0].update(tooltip_render_events=0),
            lambda p:p['hovers'][0].update(tooltip_event_class='RenderTooltipEvent.Pre'),
            lambda p:p['hovers'][0].update(actual_x=float('nan')),
            lambda p:p['hovers'][0].update(actual_x=0),
            lambda p:p['hovers'][0].update(actual_x=641,target_x=641),
            lambda p:p['gestures'][0].update(button_events=0),
            lambda p:p['gestures'][0].update(native_press_observed=False),
            lambda p:p['gestures'][0].update(native_release_observed=False),
            lambda p:p['gestures'][4].update(native_shift_observed=False),
            lambda p:p['gestures'][4].update(key_events=0),
            lambda p:p['gestures'][0].update(xtest_motion_count=0),
            lambda p:p['gestures'][0].pop('server'),
            lambda p:p['gestures'][0]['server'].update(menu_id=40),
            lambda p:p['gestures'][0]['server'].update(total=2),
            lambda p:p['gestures'][0]['client']['state'].__setitem__(4,0),
            lambda p:p['gestures'][6]['server']['state'].__setitem__(6,1000),
            lambda p:p['process_seeds'][0]['server']['state'].__setitem__(6,0),
            lambda p:p['gestures'][0].update(menu_slot=0),
            lambda p:p['gestures'][0].update(released=False),
            lambda p:p['gestures'][0].update(name='../escape.png'),
        ]
        for i,mutate in enumerate(mutations):
            with self.subTest(mutation=i):
                p=valid();mutate(p)
                with self.assertRaises(SmokeFailure): self.check(p)
    def test_runner_binds_nonce_to_display_exclusively(self):
        with tempfile.TemporaryDirectory() as directory:
            env=dict(os.environ,IEDCTR_GAMEPLAY_DIR=directory,IEDCTR_GAMEPLAY_NONCE=NONCE,DISPLAY=':171')
            command=[sys.executable,'-c',XVFB_BIND,sys.executable,'-c','pass']
            self.assertEqual(subprocess.run(command,env=env,capture_output=True).returncode,0)
            self.assertEqual((Path(directory)/'.iedctr-fluid-xvfb').read_text(),NONCE+'\n:171')
            self.assertNotEqual(subprocess.run(command,env=env,capture_output=True).returncode,0)

    def test_failed_proof_reports_sanitized_fixture_error(self):
        p=valid();p.update(status='failed',error='Input contract changed\n\x00'+('x'*2000))
        for code in (0,1):
            with self.subTest(code=code), self.assertRaises(SmokeFailure) as failure:
                validate_proof(p,NONCE,SHA,Path('/unused'),'BUILD FAILED' if code else '',code,False,1280,720,'en_us')
            message=str(failure.exception)
            self.assertIn('Input contract changed',message)
            self.assertNotIn('\n',message);self.assertNotIn('\x00',message)
            self.assertLess(len(message),1100)

    def test_run_and_png_boundaries(self):
        def chunk(kind,data):
            return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data))
        data=(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',1280,720,8,6,0,0,0))
              +chunk(b'IDAT',zlib.compress((b'\0'+b'\0'*(1280*4))*720))+chunk(b'IEND',b''))
        with tempfile.TemporaryDirectory() as directory:
            game=Path(directory)/'run';folder=game/'fluid-gesture-screenshots';folder.mkdir(parents=True)
            p=valid()
            for row in p['hovers']+p['gestures']:
                row['sha256']=hashlib.sha256(data).hexdigest();(folder/row['name']).write_bytes(data)
            marker='IEDCTR_FLUID_GESTURES PASSED nonce='+NONCE
            def check(proof=p,logs=marker,code=0,timeout=False):
                return validate_proof(proof,NONCE,SHA,game,logs,code,timeout,1280,720,'en_us')
            check()
            for changes in [dict(timeout=True),dict(code=1),dict(logs=marker+' FATAL'),dict(logs='')]:
                with self.subTest(changes=changes),self.assertRaises(SmokeFailure):check(**changes)
            crash=game/'crash-reports'/'crash.txt';crash.parent.mkdir();crash.write_text('failure')
            with self.assertRaises(SmokeFailure):check()
            crash.unlink()
            target=folder/p['hovers'][0]['name'];target.unlink()
            with self.assertRaises(SmokeFailure):check()
            target.symlink_to(folder/p['hovers'][1]['name'])
            with self.assertRaises(SmokeFailure):check()
            target.unlink();target.write_bytes(data+b'tamper')
            with self.assertRaises(SmokeFailure):check()
            target.write_bytes(data)
            altered=copy.deepcopy(p);altered['hovers'][0]['width']=1279
            with self.assertRaises(SmokeFailure):check(proof=altered)

    def test_fixture_is_opt_in_and_has_no_synthetic_clicks(self):
        root=Path(__file__).resolve().parents[2]
        java=(root/'src/fluidGestureNativeValidation/java/com/oblixorprime/engineersdecorreforged/client/validation/NativeFluidGestureValidation.java').read_text()
        for forbidden in ['.clicked(','.quickMoveStack(','.handleInventoryMouseClick(','.mouseClicked(','.mouseReleased(','.onPress(']:
            self.assertNotIn(forbidden,java)
        for required in ['XTestFakeMotionEvent','XTestFakeButtonEvent','XTestFakeKeyEvent','mc.mouseHandler.xpos()',
                         'RenderTooltipEvent.Color','serverSnapshot','finally','XCloseDisplay']:
            self.assertIn(required,java)
        build=(root/'build.gradle').read_text()
        self.assertIn("sourceSets.create('fluidGestureNativeValidation')",build)
        self.assertNotIn("sourceSets.main.java.srcDir('src/fluidGestureNativeValidation",build)

if __name__=='__main__': unittest.main()
