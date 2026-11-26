import copy
from pathlib import Path
import unittest
from unittest.mock import patch
from gui_album_three import validate, validate_selection_pixels, v3_outline, CASES, SCREENS, V3_LAYOUTS, V3_OCCUPIED_SLOTS, SmokeFailure
from test_factory_hopper_selection_alignment import assert_v3_alignment
class ProofTests(unittest.TestCase):
    def setUp(self):
        self.jar=Path('/tmp/album.jar');self.digest='a'*64
        self.proof=dict(suite='gui-album-three-v2',status='observed',nonce='b'*32,executed_source_sha='c'*40,visual_acceptance=False,physical_os_input=True,human_input=False,
            input_method="synthetic XTest pointer positioning only; no clicks",synthetic_button_events=0,
            packaged_runtime=dict(jar_path=str(self.jar.resolve()),jar_sha256=self.digest,fixture_free_jar=True,separate_harness_module=True),
            cases=[dict(case=n,screen=SCREENS[i],server_menu_acknowledged=True,gui_scale=2,visible_nonempty_slots=3,visible_machine_slots=0,total_menu_slots=0,screen_class_sha256='d'*64,capture=n+'.png',capture_sha256='e'*64) for i,n in enumerate(CASES)])
        self.proof['cases'][0].update(visible_nonempty_slots=54,server_selection=0,client_selection=0,ring_x=10,ring_y=8,
            selection_source='server-seeded ContainerData; not user selection',selection_render_frames=3)
        self.proof['cases'][0].update(visible_machine_slots=18,total_menu_slots=54,ring_x=33,ring_y=67)
        self.proof['cases'][0]['visible_nonempty_machine_slots']=18
        self.proof['cases'][5].update(visible_nonempty_slots=51,visible_machine_slots=15,total_menu_slots=51,
                                      visible_nonempty_machine_slots=15)
        self.proof['cases'][5].update(dropper_filter_gate=2,dropper_external_powered=False)
        self.proof['cases'][6].update(visible_nonempty_slots=41,visible_machine_slots=11,total_menu_slots=47,
                                      visible_nonempty_machine_slots=5)
        self.proof['hopper_selections']=[dict(self.proof['cases'][0],server_selection=row*6,client_selection=row*6,ring_y=8+18*row,
            capture='factory-hopper'+('' if row==0 else '-selection-row-'+str(row))+'.png') for row in range(3)]
        for row,selection in enumerate(self.proof['hopper_selections']):
            selection['ring_y']=67+25*row
    def check(self,p):
        with patch('gui_album_three.png') as png, patch('gui_album_three.validate_selection_pixels'):
            validate(p,'b'*32,'c'*40,self.jar,self.digest,Path('/tmp/run'),1280,720)
            self.assertEqual(png.call_count,10)
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
        self.proof['human_input']=True
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
        for index,key,value in ((0,'visible_machine_slots',17),(5,'total_menu_slots',50),
                                (6,'visible_nonempty_slots',40),(6,'visible_nonempty_machine_slots',4)):
            p=copy.deepcopy(self.proof);p['cases'][index][key]=value
            with self.subTest(index=index,key=key),self.assertRaises(SmokeFailure):self.check(p)

    def test_rejects_dropper_capture_fixture_that_can_transfer_items(self):
        for key,value in [('dropper_filter_gate',0),('dropper_external_powered',True)]:
            p=copy.deepcopy(self.proof);p['cases'][5][key]=value
            with self.subTest(key=key),self.assertRaises(SmokeFailure):self.check(p)

    def test_v3_screens_are_exact_factory_study_screens_with_18_15_and_11_machine_slots(self):
        self.assertEqual(V3_LAYOUTS,{0:(18,54),5:(15,51),6:(11,47)})
        self.assertEqual(V3_OCCUPIED_SLOTS,{0:(18,54),5:(15,51),6:(5,41)})
        self.assertEqual([SCREENS[i] for i in (0,5,6)],[
            'com.oblixorprime.engineersdecorreforged.client.FactoryStudyScreen']*3)
        root=Path(__file__).resolve().parents[2]
        source=(root/'src/guiAlbumThree/java/com/oblixorprime/iedctrtest/factoryplacer/GuiAlbumThree.java').read_text()
        for token in ('Factory Study V3 menu slot layout mismatch','activeMenu.visibleMachineSlots()==EXPECTED_MACHINE_SLOTS[index]',
                      'activeMenu.slots.size()==EXPECTED_TOTAL_SLOTS[index]','EXPECTED_OCCUPIED_SLOTS',
                      'EXPECTED_OCCUPIED_MACHINE_SLOTS','visible_nonempty_machine_slots','FactoryStudyScreen'):
            self.assertIn(token,source)
        self.assertIn('if(index==5){',source)
        self.assertNotIn('if(caseIndex==5){',source)

    def test_current_hopper_screen_uses_v3_menu_and_selection_anchors(self):
        root=Path(__file__).resolve().parents[2]
        geometry=(root/'src/main/java/com/oblixorprime/engineersdecorreforged/utility/FactoryStudyGeometry.java').read_text()
        menu=(root/'src/main/java/com/oblixorprime/engineersdecorreforged/menu/MachineMenu.java').read_text()
        screen=(root/'src/main/java/com/oblixorprime/engineersdecorreforged/client/FactoryStudyScreen.java').read_text()
        events=(root/'src/main/java/com/oblixorprime/engineersdecorreforged/client/ClientModEvents.java').read_text()
        anchors=assert_v3_alignment(geometry,menu,screen,events)
        self.assertEqual(len(anchors),54)
        self.assertIn('FactoryStudyScreen::new',events)

    def test_selection_pixel_guard_rejects_old_pitch_and_missing_ring(self):
        width,height,scale=1280,720,2
        def pixels(pitch,missing=False):
            outputs=[]
            for row in range(3):
                data=bytearray(width*height*4)
                if not missing:
                    left,top,right,bottom=v3_outline(width,height,scale,row)
                    # Relocate each row by the candidate pitch and keep the same 18x18 border.
                    delta=(pitch-25)*row*scale
                    top+=delta;bottom+=delta
                    border={(x,top) for x in range(left,right+1)} | {(x,bottom) for x in range(left,right+1)}
                    border|={(left,y) for y in range(top,bottom+1)} | {(right,y) for y in range(top,bottom+1)}
                    for px,py in border:
                        for y in range(py,py+scale):
                            for x in range(px,px+scale):
                                data[(y*width+x)*4:(y*width+x)*4+3]=b'\xff\xff\xff'
                outputs.append((width,height,bytes(data)))
            return outputs
        records=[dict(capture=str(i)+'.png') for i in range(3)]
        with patch('gui_album_three.decode_png',side_effect=pixels(25)):
            validate_selection_pixels(records,Path('/tmp'),width,height,scale)
        for images in (pixels(17),pixels(18),pixels(25,True)):
            with patch('gui_album_three.decode_png',side_effect=images),self.assertRaises(SmokeFailure):
                validate_selection_pixels(records,Path('/tmp'),width,height,scale)

    def test_pointer_target_is_interior_and_observed_not_bypassed(self):
        root=Path(__file__).resolve().parents[2]
        source=(root/'src/guiAlbumThree/java/com/oblixorprime/iedctrtest/factoryplacer/GuiAlbumThree.java').read_text()
        self.assertIn('POINTER_TARGET=8',source)
        self.assertNotIn('getScreenWidth()-2',source)
        self.assertNotIn('getScreenHeight()-2',source)
        self.assertIn('GLFW.glfwFocusWindow(window)',source)
        self.assertIn('XTestFakeMotionEvent',source)
        self.assertNotIn('XTestFakeButtonEvent',source)
        self.assertNotIn('glfwSetCursorPos',source)
        self.assertIn('Math.abs(mc.mouseHandler.xpos()-POINTER_TARGET)>1',source)
        self.assertIn('Math.abs(mc.mouseHandler.ypos()-POINTER_TARGET)>1',source)
        self.assertIn('Pointer target must remain outside panel',source)
        self.assertIn('++ticks<1800',source)
        self.assertIn('selectionRenderFrames<3',source)

    def test_wait_failure_preserves_state_diagnostics(self):
        root=Path(__file__).resolve().parents[2]
        source=(root/'src/guiAlbumThree/java/com/oblixorprime/iedctrtest/factoryplacer/GuiAlbumThree.java').read_text()
        for field in ('wait_diagnostics','terminal_state','mouse_x','mouse_y','screen_width','screen_height',
                      'window_x','window_y','glfw_cursor_x','glfw_cursor_y','focused','client_selection','selection_render_frames'):
            self.assertIn('"'+field+'"',source)
        for reason in ('screen not opened','window not focused','pointer callback not at target',
                       'client selection not synchronized','selection render frames missing','server inspection pending'):
            self.assertIn('waiting(mc,"'+reason+'")',source)

    def test_reject_album_clicks_or_ambiguous_input_provenance(self):
        for key,value in [('physical_os_input',False),('human_input',True),('synthetic_button_events',1),
                          ('synthetic_button_events',False),('input_method','XTest clicks')]:
            p=copy.deepcopy(self.proof);p[key]=value
            with self.subTest(key=key),self.assertRaises(SmokeFailure):self.check(p)

    def test_pointer_primes_after_open_on_separate_ticks(self):
        root=Path(__file__).resolve().parents[2]
        source=(root/'src/guiAlbumThree/java/com/oblixorprime/iedctrtest/factoryplacer/GuiAlbumThree.java').read_text()
        opening=source.split('case OPEN -> {',1)[1].split('case WAIT -> {',1)[0]
        self.assertNotIn('positionPointer',opening)
        self.assertIn('POINTER_PRIME=12',source)
        self.assertIn('if(!pointerReady(mc))',source)
        self.assertIn('positionPointer(mc,POINTER_PRIME)',source)
        self.assertIn('if(ticks<=lastMotionTick)return false;',source)
        self.assertIn('Math.abs(x[0]-POINTER_PRIME)>1',source)
        self.assertIn('positionPointer(mc,POINTER_TARGET)',source)
        self.assertIn('pointerPhase=2;lastMotionTick=ticks;return false;',source)
        self.assertIn('pointer callback not at target");pointerPhase=0;return false;',source)
        self.assertIn('pointerPhase=0;lastMotionTick=-1;',source)
        self.assertNotIn('mouseHandler.onMove',source)
        self.assertNotIn('mouseHandler.set',source)
