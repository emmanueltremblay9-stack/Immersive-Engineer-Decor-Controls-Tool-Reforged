"""Reject malformed native proof records; fixtures are NOT runtime qualification."""
import copy
import re
from pathlib import Path
import unittest
from unittest.mock import patch
from factory_control_tooltip_native import validate, validate_hopper_icon_pixels, NAMES, SmokeFailure
from factory_placer_v2_reference_native import decode_png

class ProofTests(unittest.TestCase):
    def test_dropper_v3_controls_share_live_tooltip_geometry(self):
        root = Path(__file__).resolve().parents[2]
        geometry = (root/'src/main/java/com/oblixorprime/engineersdecorreforged/utility/FactoryStudyGeometry.java').read_text()
        screen = (root/'src/main/java/com/oblixorprime/engineersdecorreforged/client/FactoryStudyScreen.java').read_text()
        events = (root/'src/main/java/com/oblixorprime/engineersdecorreforged/client/ClientModEvents.java').read_text()
        dropper = geometry.split('if (kind == MachineKind.FACTORY_DROPPER) return switch (control) {', 1)[1].split('\n      };', 1)[0]
        zones = {name: tuple(map(int, rect)) for name, *rect in re.findall(
            r'case (VELOCITY|DIRECTION|COUNT|PERIOD|MANUAL|FILTER_GATE|REDSTONE|TRIGGER) -> new Rect\((\d+), (\d+), (\d+), (\d+)\)', dropper)}
        points = {'VELOCITY': (195, 52), 'FILTER_GATE': (221, 139), 'REDSTONE': (250, 139), 'TRIGGER': (280, 139)}
        self.assertEqual(set(zones), {'VELOCITY','DIRECTION','COUNT','PERIOD','MANUAL','FILTER_GATE','REDSTONE','TRIGGER'})
        for name, point in points.items():
            x,y,w,h = zones[name]
            self.assertTrue(x <= point[0] < x+w and y <= point[1] < y+h, (name,point,zones[name]))
        self.assertIn('FactoryStudyGeometry.controlAt(kind, localPointerX, localPointerY)', screen)
        self.assertIn('tooltip = controls.get(hovered).description()', screen)
        self.assertIn('font.split(tooltip, FactoryStudyGeometry.tooltipWidth(width))', screen)
        self.assertIn('event.register((MenuType)ModMenus.FACTORY_DROPPER.get(), FactoryStudyScreen::new)', events)

    def make(self, machine):
        p=dict(suite='native-factory-control-tooltip-v2',status='passed',nonce='a'*32,
            executed_source_sha='b'*40,width=320,height=260,selected_language='fr_fr',machine=machine,
            presentation_selector_absent=True,opened_via_native_block_interaction=1,native_visual_acceptance=False,
            state_change_server_acks=3,
            trigger_setup_before=6,trigger_setup_after=2,
            packaged_runtime=dict(jar_path=str(Path('artifact.jar').resolve()),jar_sha256='c'*64,
                fixture_free_jar=True,separate_harness_module=True),captures=[])
        states={'inversion':3,'inversion_changed':6,'inversion_normal':2,'triggermode':2,'triggermode_changed':0}
        for name in NAMES[machine]:
            phrases={'inversion':'État : inversé — activé sans signal (par défaut).',
                     'inversion_changed':'État : redstone ignorée — toujours activé.',
                     'inversion_normal':'État : normal — activé par un signal alimenté.'}
            text='Complete translated tooltip\n'+phrases.get(name,'with dynamic state '+name)
            p['captures'].append(dict(name=name,file='factory-control-'+name+'.png',sha256='d'*64,
                render_frames=3,tooltip=dict(in_bounds=True,width=230,height=40,gray_characters=10,
                    text=text,expected_text=text,synced_logic=states.get(name,0))))
        if machine=='hopper':
            p['hopper_icons']=[dict(name='icon-'+name,file='factory-control-icon-'+name+'.png',
                sha256='e'*64,client_logic=states[name],server_logic=states[name],render_frames=3,
                tooltip_absent=True,gui_scale=1) for name in ('inversion','inversion_changed','inversion_normal')]
        if machine=='dropper':
            states={'filtergate':2,'filtergate_changed':3,'externgate':3,'externgate_changed':1,'triggermode':1,'triggermode_changed':17}
            for c in p['captures']:c['tooltip']['synced_logic']=states.get(c['name'],2)
            p['dropper_transitions']=[dict(control=n,before=states[n],client_after=states[n+'_changed'],server_after=states[n+'_changed'],expected_after=states[n+'_changed']) for n in ('filtergate','externgate','triggermode')]
        return p
    def check(self,p):
        with patch('factory_control_tooltip_native.png'), patch('factory_control_tooltip_native.validate_hopper_icon_pixels'):
            validate(p,'a'*32,'b'*40,Path('artifact.jar'),'c'*64,Path('game'),320,260,'fr_fr',p['machine'])
    def test_valid_records(self):
        for machine in NAMES: self.check(self.make(machine))
    def test_reject_identity_isolation_and_coverage(self):
        p=self.make('hopper')
        for key,value in [('nonce','x'),('executed_source_sha','x'),('state_change_server_acks',0),('captures',[]),('native_visual_acceptance',True)]:
            q=copy.deepcopy(p);q[key]=value
            with self.subTest(key=key),self.assertRaises(SmokeFailure):self.check(q)
        for key,value in [('jar_sha256','x'),('fixture_free_jar',False),('separate_harness_module',False)]:
            q=copy.deepcopy(p);q['packaged_runtime'][key]=value
            with self.subTest(key=key),self.assertRaises(SmokeFailure):self.check(q)
    def test_reject_unchanged_server_state(self):
        p=self.make('hopper')
        p['captures'][3]['tooltip']['synced_logic']=p['captures'][2]['tooltip']['synced_logic']
        with self.assertRaises(SmokeFailure):self.check(p)
    def test_reject_invisible_trigger_transition(self):
        p=self.make('hopper')
        for key in ('text','expected_text'):
            p['captures'][6]['tooltip'][key]=p['captures'][5]['tooltip'][key]
        with self.assertRaises(SmokeFailure):self.check(p)
    def test_reject_ignored_trigger_mode(self):
        p=self.make('hopper');p['captures'][5]['tooltip']['synced_logic']=4
        p['trigger_setup_after']=4
        with self.assertRaises(SmokeFailure):self.check(p)
    def test_reject_render_semantic_mismatch(self):
        p=self.make('dropper');p['captures'][1]['tooltip']['expected_text']='Different complete text'
        with self.assertRaises(SmokeFailure):self.check(p)
    def test_reject_tooltip_damage(self):
        for key,value in [('in_bounds',False),('width',241),('height',10),('gray_characters',0),('text','short'),('synced_logic',None)]:
            q=self.make('hopper');q['captures'][2]['tooltip'][key]=value
            with self.subTest(key=key),self.assertRaises(SmokeFailure):self.check(q)

    def test_reject_dropper_missing_or_wrong_ack(self):
        for key,value in [('before',3),('client_after',2),('server_after',2),('expected_after',2),('control','velocity')]:
            p=self.make('dropper');p['dropper_transitions'][0][key]=value
            with self.subTest(key=key),self.assertRaises(SmokeFailure):self.check(p)
        p=self.make('dropper');p['dropper_transitions']=[]
        with self.assertRaises(SmokeFailure):self.check(p)
        p=self.make('dropper');p['captures'][3]['tooltip']['synced_logic']=2
        with self.assertRaises(SmokeFailure):self.check(p)
    def test_reject_missing_normal_tooltip_wrong_semantics_and_icon_proof(self):
        p=self.make('hopper');p['captures'].pop(4)
        with self.assertRaises(SmokeFailure):self.check(p)
        p=self.make('hopper')
        for k in ('text','expected_text'):p['captures'][4]['tooltip'][k]=p['captures'][2]['tooltip'][k]
        with self.assertRaises(SmokeFailure):self.check(p)
        for k,v in [('server_logic',6),('client_logic',6),('render_frames',0),('tooltip_absent',False),('gui_scale',2)]:
            p=self.make('hopper');p['hopper_icons'][2][k]=v
            with self.subTest(k=k),self.assertRaises(SmokeFailure):self.check(p)
        p=self.make('hopper');p['hopper_icons']=[]
        with self.assertRaises(SmokeFailure):self.check(p)

    def test_icon_pixels_reject_permuted_duplicate_and_damaged_sprites(self):
        root=Path(__file__).resolve().parents[2]
        aw,ah,atlas=decode_png(root/'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/factory_hopper_gui.png')
        width,height=320,260;left=(width-176)//2;top=(height-166)//2
        images=[]
        for u,v in ((188,49),(177,59),(177,49)):
            pixels=bytearray(width*height*4)
            for y in range(9):
                for x in range(9):
                    a=((v+y)*aw+u+x)*4
                    target=((top+49+y)*width+left+145+x)*4
                    for k in range(3):pixels[target+k]=atlas[a+k]
            images.append((width,height,bytes(pixels)))
        records=self.make('hopper')['hopper_icons']
        def check(frames):
            with patch('factory_control_tooltip_native.zipfile.ZipFile'), patch('factory_control_tooltip_native.zipfile.Path'), patch('factory_control_tooltip_native.decode_png',side_effect=[(aw,ah,atlas)]+frames):
                validate_hopper_icon_pixels(records,Path('game'),width,height,Path('artifact.jar'))
        check(images)
        for frames in ([images[1],images[0],images[2]],[images[0]]*3,[images[2],images[1],images[0]]):
            with self.assertRaises(SmokeFailure):check(frames)
        damaged=bytearray(images[2][2]);damaged[((top+49)*width+left+145)*4]^=1
        with self.assertRaises(SmokeFailure):check(images[:2]+[(width,height,bytes(damaged))])

    def test_hopper_points_and_exact_modes_remain_native(self):
        root=Path(__file__).resolve().parents[2]
        source=(root/'src/factoryPlacerNativeValidation/java/com/oblixorprime/iedctrtest/factoryplacer/FactoryControlTooltipValidation.java').read_text()
        self.assertIn('int[] expectedStates={3,6,2}',source)
        self.assertIn('serverMenu.containerId==menu.containerId && serverMenu.field(2)==iconLogic',source)
        self.assertIn('Tooltip covers icon evidence',source)
        self.assertIn('ticks<12 || iconFrames<3',source)
        self.assertNotIn('setSuppressTooltips',source)
        self.assertNotIn('dataAccessForTests().set',source)
        self.assertIn('nativeClick(1)',source)

if __name__=='__main__':unittest.main()
