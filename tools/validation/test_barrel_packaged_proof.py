"""Synthetic rejection tests; no Minecraft execution is implied."""
import copy
import hashlib
from pathlib import Path
import tempfile
import unittest
import warnings
import zipfile
from client_mixin_smoke import SmokeFailure
from barrel_packaged_proof import CLASSES, HARNESS, MOD, TEXTURES, validate_runtime

ROOT = Path(__file__).resolve().parents[2]


class RuntimeTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        jar = Path(self.temp.name)/'normal.jar'
        with zipfile.ZipFile(jar, 'w') as archive:
            for name in CLASSES:
                archive.writestr(name.replace('.', '/')+'.class', name.encode())
            for name in TEXTURES:
                archive.write(ROOT/'src/main/resources/assets'/MOD/name, 'assets/'+MOD+'/'+name)
        digest = hashlib.sha256(jar.read_bytes()).hexdigest()
        root = 'union:/exact-normal-jar/'
        self.report = dict(jar_path=str(jar), jar_sha256=digest, fixture_free_jar=True,
            split_packages=False, loaded_development_mods=[HARNESS],
            runtime_classpath=[str(jar), '/isolated/harness'], forbidden_main_outputs=['/main/classes', '/main/resources'])
        self.proof = dict(packaged_runtime=dict(jar_path=str(jar), jar_sha256=digest,
            fixture_free_jar=True, separate_harness_module=True, production_selectors_absent=True,
            harness_mod_id=HARNESS, harness_mod_path='/isolated/harness', mod_root_uri=root,
            class_origins=[{'class': n, 'resource_uri': root+n.replace('.', '/')+'.class',
                'code_source_uri': root, 'sha256': hashlib.sha256(n.encode()).hexdigest()} for n in CLASSES],
            active_textures=[dict(path=n, sha256=h, pack='mod/'+MOD) for n, h in TEXTURES.items()]))

    def check(self, proof=None, report=None):
        return validate_runtime(self.proof if proof is None else proof, self.report if report is None else report)

    def test_valid_runtime(self):
        self.check()

    def test_java_equivalent_uri(self):
        for row in self.proof['packaged_runtime']['class_origins']:
            row['code_source_uri'] = 'file:'+self.report['jar_path']
        self.check()

    def test_reject_runtime_corruption(self):
        mutations = [lambda p: p.pop('packaged_runtime'),
            lambda p: p['packaged_runtime'].update(production_selectors_absent=False),
            lambda p: p['packaged_runtime'].update(separate_harness_module=False),
            lambda p: p['packaged_runtime'].update(jar_sha256='0'*64),
            lambda p: p['packaged_runtime'].update(harness_mod_id=MOD),
            lambda p: p['packaged_runtime'].update(harness_mod_path=self.report['jar_path']),
            lambda p: p['packaged_runtime']['class_origins'].pop(),
            lambda p: p['packaged_runtime']['class_origins'][0].update(resource_uri='file:/main/classes/foo.class'),
            lambda p: p['packaged_runtime']['class_origins'][0].update(code_source_uri='file:/source-output/'),
            lambda p: p['packaged_runtime']['class_origins'][0].update(sha256='0'*64),
            lambda p: p['packaged_runtime']['active_textures'][0].update(pack='file/override.zip'),
            lambda p: p['packaged_runtime']['active_textures'][0].update(sha256='0'*64),
            lambda p: p['packaged_runtime']['active_textures'].pop()]
        for change in mutations:
            proof = copy.deepcopy(self.proof)
            change(proof)
            with self.subTest(change=change), self.assertRaises(SmokeFailure): self.check(proof)

    def test_reject_unsafe_uris(self):
        for field in ('code_source_uri', 'resource_uri'):
            for suffix in ('?other=true', '#other'):
                proof = copy.deepcopy(self.proof)
                proof['packaged_runtime']['class_origins'][0][field] += suffix
                with self.subTest(field=field, suffix=suffix), self.assertRaises(SmokeFailure): self.check(proof)

    def test_reject_main_output_and_wrong_registration(self):
        for field, value in [('loaded_development_mods', [MOD, HARNESS]), ('split_packages', True),
            ('fixture_free_jar', False), ('runtime_classpath', [self.report['jar_path'], '/main/else/../classes']),
            ('runtime_classpath', ['/other.jar']), ('forbidden_main_outputs', [])]:
            report = copy.deepcopy(self.report)
            report[field] = value
            with self.subTest(field=field), self.assertRaises(SmokeFailure): self.check(report=report)

    def test_reject_leaked_harness_and_duplicate_entries(self):
        for name in ('com/oblixorprime/iedctrtest/barrel/Leak.class', CLASSES[0].replace('.', '/')+'.class'):
            with zipfile.ZipFile(self.report['jar_path'], 'a') as archive:
                with warnings.catch_warnings():
                    warnings.simplefilter('ignore', UserWarning)
                    archive.writestr(name, b'bad')
            digest = hashlib.sha256(Path(self.report['jar_path']).read_bytes()).hexdigest()
            self.report['jar_sha256'] = self.proof['packaged_runtime']['jar_sha256'] = digest
            with self.subTest(name=name), self.assertRaises(SmokeFailure): self.check()



from barrel_packaged_proof import FLUID_IDS, STILL_TEXTURES, STATES, PREFIX, validate_evidence


def valid_evidence(fluid='water', language='en_us', height=720):
    width=1280
    left, top=(width-201)//2, (height-226)//2
    proof=dict(suite='barrel-packaged-native-v1', nonce='a'*32, executed_source_sha='b'*40,
        status='passed_bounded_native', selected_fluid=fluid, selected_language=language,
        actual_integrated_world=True, input_backend='XTEST', cleanup_released=True,
        rendered_sprite_identity_verified=False, natural_collection_verified=False, visual_acceptance=False,
        opening=dict(method='XTEST_RMB_BLOCK_HIT', hit_block='0,80,0', native_right_press_observed=True,
                     native_right_release_observed=True, menu_id=1), captures=[], gestures=[])
    for i, state in enumerate(STATES):
        carried=int(i==4)
        snapshot=dict(menu_id=1, fluid_capacity=32000, fluid_amount=(0,16000,32000,32000,32000,32000)[i],
            fluid_id='minecraft:empty' if i==0 else FLUID_IDS[fluid], total_items=1,
            player_buckets=1-carried, player_slot_2_buckets=1-carried, carried_buckets=carried,
            machine_input_empty=True, machine_output_empty=True)
        x,y=left+(105 if i==3 else 8),top+(65 if i==3 else 8)
        geometry=dict(outer_frame_id=100+i, render_id=50+i, screen_id=2, menu_id=1, state_name=state,
            pointer_x=float(x), pointer_y=float(y), gui_width=width, gui_height=height,
            gui_left=left, gui_top=top, image_width=201, image_height=226,
            tank_x=left+92, tank_y=top+35, tank_width=25, tank_height=74)
        if i==3:
            geometry['tooltip']=dict(text_x=x+12,text_y=y-12,text_width=120,text_height=10,
                frame_left=x+8,frame_top=y-16,frame_right=x+136,frame_bottom=y+2)
        row=dict(name='barrel-'+state+'.png', state_name=state, machine='barrel', selected_fluid=fluid,
            screen_class=PREFIX+'client.MachineScreen$FluidBarrelScreen',setup_seed=True,menu_id=1,
            width=width,height=height,window_width=width,window_height=height,gui_width=width,gui_height=height,
            gui_scale=1.0,recipe_toast_absent=True,profiler_chart_disabled=True,
            capture_phase='RenderFrameEvent.Post:LOWEST',xtest_motion_count=i+1,sha256='c'*64,
            capture_outer_frame_id=100+i,capture_render_id=50+i,capture_screen_id=2,
            target_x=x,target_y=y,geometry=geometry,client=snapshot,server=copy.deepcopy(snapshot))
        if i<3:
            sample_top=(38,73,38)[i]
            sample=dict(x=left+99,y=top+sample_top,width=16,height=107-sample_top,
                distinct_rgba_colors=1 if i==0 else 10,fluid_id=snapshot['fluid_id'])
            if i:
                namespace,path=STILL_TEXTURES[fluid].split(':')
                sample.update(still_texture=STILL_TEXTURES[fluid],tint_argb='ffffffff',atlas_sprite=STILL_TEXTURES[fluid],
                    active_texture=namespace+':textures/'+path+'.png',
                    active_texture_pack='mod/neoforge' if fluid=='milk' else 'vanilla',active_texture_sha256='d'*64)
            row['barrel_fluid_pixels']=sample
        proof['captures'].append(row)
        if i>=4:
            proof['gestures'].append(dict(action=state,menu_slot=2,native_press_observed=True,native_release_observed=True,
                button_events=2*(i-3),client=copy.deepcopy(snapshot),server=copy.deepcopy(snapshot)))
    return proof


class BarrelEvidenceTests(unittest.TestCase):
    def check(self, p=None, fluid='water', language='en_us', height=720):
        return validate_evidence(valid_evidence(fluid,language,height) if p is None else p,'a'*32,'b'*40,1280,height,language,fluid)

    def test_six_profiles(self):
        for fluid in FLUID_IDS:
            for language,height in [('en_us',720),('fr_fr',260)]:
                with self.subTest(fluid=fluid,language=language): self.check(fluid=fluid,language=language,height=height)

    def test_wrong_identity_or_profile(self):
        for field,value in [('nonce','0'*32),('executed_source_sha','0'*40),('status','passed'),
            ('selected_fluid','milk'),('selected_language','fr_fr'),('actual_integrated_world',False),
            ('input_backend','direct'),('cleanup_released',False),('rendered_sprite_identity_verified',True),
            ('natural_collection_verified',True),('visual_acceptance',True)]:
            p=valid_evidence();p[field]=value
            with self.subTest(field=field),self.assertRaises(SmokeFailure):self.check(p)

    def test_opening_and_matrix_required(self):
        for field,value in [('method','openMenu'),('hit_block','1,80,0'),('menu_id',True),
            ('native_right_press_observed',False),('native_right_release_observed',False)]:
            p=valid_evidence();p['opening'][field]=value
            with self.subTest(field=field),self.assertRaises(SmokeFailure):self.check(p)
        for field in ('captures','gestures'):
            p=valid_evidence();p[field].pop()
            with self.subTest(field=field),self.assertRaises(SmokeFailure):self.check(p)

    def test_every_capture_identity_and_geometry(self):
        for i in range(6):
            for field,value in [('name','../other.png'),('state_name','other'),('machine','funnel'),
                ('selected_fluid','lava'),('screen_class','OtherScreen'),('setup_seed',False),('menu_id',99),
                ('width',999),('height',999),('window_height',999),('gui_width',999),('gui_scale',True),
                ('recipe_toast_absent',False),('profiler_chart_disabled',False),('capture_phase','ScreenEvent.Render.Post'),
                ('xtest_motion_count',0),('sha256','wrong'),('capture_outer_frame_id',0),
                ('capture_render_id',0),('capture_screen_id',99),('target_x',0)]:
                p=valid_evidence();p['captures'][i][field]=value
                with self.subTest(i=i,field=field),self.assertRaises(SmokeFailure):self.check(p)
            for field in ('outer_frame_id','render_id','screen_id','menu_id','gui_width','gui_height','gui_left','gui_top',
                          'image_width','image_height','tank_x','tank_y','tank_width','tank_height','pointer_x','pointer_y'):
                p=valid_evidence();p['captures'][i]['geometry'][field]=True
                with self.subTest(i=i,field=field),self.assertRaises(SmokeFailure):self.check(p)

    def test_conservation_every_side_and_state(self):
        for i in range(6):
            for side in ('client','server'):
                for field in valid_evidence()['captures'][i][side]:
                    p=valid_evidence();p['captures'][i][side][field]=None
                    with self.subTest(i=i,side=side,field=field),self.assertRaises(SmokeFailure):self.check(p)
        for i in range(2):
            for field,value in [('action','other'),('menu_slot',3),('native_press_observed',False),
                ('native_release_observed',False),('button_events',0)]:
                p=valid_evidence();p['gestures'][i][field]=value
                with self.subTest(i=i,field=field),self.assertRaises(SmokeFailure):self.check(p)

    def test_wrong_native_fluid_pixels(self):
        for i in range(3):
            for field in valid_evidence()['captures'][i]['barrel_fluid_pixels']:
                p=valid_evidence();p['captures'][i]['barrel_fluid_pixels'][field]=None
                with self.subTest(i=i,field=field),self.assertRaises(SmokeFailure):self.check(p)
        p=valid_evidence();p['captures'][1]['barrel_fluid_pixels']['distinct_rgba_colors']=1
        with self.assertRaises(SmokeFailure):self.check(p)
        p=valid_evidence();p['captures'][2]['barrel_fluid_pixels']['active_texture_pack']='file/custom.zip'
        with self.assertRaises(SmokeFailure):self.check(p)

    def test_tooltip_frame_and_stale_screen(self):
        for field in valid_evidence()['captures'][3]['geometry']['tooltip']:
            p=valid_evidence();p['captures'][3]['geometry']['tooltip'][field]=None
            with self.subTest(field=field),self.assertRaises(SmokeFailure):self.check(p)
        for x,y in [(3,100),(1157,100),(100,3),(100,707)]:
            p=valid_evidence();p['captures'][3]['geometry']['tooltip'].update(text_x=x,text_y=y,
                frame_left=x-4,frame_top=y-4,frame_right=x+124,frame_bottom=y+14)
            with self.subTest(x=x,y=y),self.assertRaises(SmokeFailure):self.check(p)
        p=valid_evidence();p['captures'][1]['capture_outer_frame_id']=100;p['captures'][1]['geometry']['outer_frame_id']=100
        with self.assertRaises(SmokeFailure):self.check(p)
        p=valid_evidence();p['captures'][1]['capture_screen_id']=3;p['captures'][1]['geometry']['screen_id']=3
        with self.assertRaises(SmokeFailure):self.check(p)
        p=valid_evidence();p['captures'][0]['geometry']['tooltip']={}
        with self.assertRaises(SmokeFailure):self.check(p)

import struct
import zlib
from barrel_packaged_proof import validate_proof


def write_png(path, width, height, pixels):
    def chunk(kind, data):
        return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data))
    raw=b''.join(b'\0'+pixels[y*width*4:(y+1)*width*4] for y in range(height))
    path.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',width,height,8,6,0,0,0))
                    +chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b''))


class FullProofTests(RuntimeTests):
    def setUp(self):
        super().setUp()
        runtime=self.proof['packaged_runtime']
        self.proof=valid_evidence(language='fr_fr',height=260)
        self.proof['packaged_runtime']=runtime
        self.game=Path(self.temp.name)/'run'
        self.folder=self.game/'barrel-packaged-screenshots';self.folder.mkdir(parents=True)
        for i,row in enumerate(self.proof['captures']):
            pixels=bytearray(bytes.fromhex('142028ff')*(1280*260))
            if i in (1,2):
                sample=row['barrel_fluid_pixels'];sample['distinct_rgba_colors']=2
                for y in range(sample['y'],sample['y']+sample['height']):
                    for x in range(sample['x'],sample['x']+sample['width']):
                        offset=4*(y*1280+x)
                        pixels[offset:offset+4]=bytes.fromhex('1050b0ff' if x%2 else '2060c0ff')
            path=self.folder/row['name'];write_png(path,1280,260,pixels)
            row['sha256']=hashlib.sha256(path.read_bytes()).hexdigest()
        self.logs='IEDCTR_BARREL_PACKAGED PASSED nonce='+'a'*32+' source='+'b'*40

    def full_check(self,**kwargs):
        params=dict(proof=self.proof,nonce='a'*32,source_sha='b'*40,game=self.game,logs=self.logs,
            exit_code=0,timed_out=False,width=1280,height=260,language='fr_fr',fluid='water',report=self.report)
        params.update(kwargs)
        return validate_proof(**params)

    def test_full_gate_with_real_png_bytes(self):self.full_check()

    def test_reject_native_failure_missing_marker_and_crash(self):
        for params in (dict(exit_code=1),dict(timed_out=True),dict(logs=''),dict(logs='IEDCTR_BARREL_PACKAGED PASSED nonce='+'a'*32)):
            with self.subTest(params=params),self.assertRaises(SmokeFailure):self.full_check(**params)
        (self.game/'crash-reports').mkdir();(self.game/'crash-reports/failure.txt').write_text('failure')
        with self.assertRaises(SmokeFailure):self.full_check()

    def test_reject_sample_count_forgery(self):
        self.proof['captures'][1]['barrel_fluid_pixels']['distinct_rgba_colors']=3
        with self.assertRaisesRegex(SmokeFailure,'actual PNG pixels'):self.full_check()

    def test_reject_full_render_at_half_and_wrong_empty_backing(self):
        from factory_placer_v2_reference_native import decode_png
        for i in (0,1):
            row=self.proof['captures'][i];path=self.folder/row['name']
            original=path.read_bytes();original_hash=row['sha256']
            width,height,raw=decode_png(path);pixels=bytearray(raw)
            x=row['geometry']['gui_left']+100;y=row['geometry']['gui_top']+40
            pixels[4*(y*width+x):4*(y*width+x)+4]=bytes.fromhex('1050b0ff')
            write_png(path,width,height,pixels);row['sha256']=hashlib.sha256(path.read_bytes()).hexdigest()
            with self.subTest(i=i),self.assertRaises(SmokeFailure):self.full_check()
            path.write_bytes(original);row['sha256']=original_hash

    def test_reject_rehashed_flat_occupied_tank(self):
        row=self.proof['captures'][2];path=self.folder/row['name']
        write_png(path,1280,260,bytes.fromhex('142028ff')*(1280*260))
        row['sha256']=hashlib.sha256(path.read_bytes()).hexdigest()
        with self.assertRaises(SmokeFailure):self.full_check()

    def test_reject_png_corruption_and_symlink(self):
        row=self.proof['captures'][0];path=self.folder/row['name'];original=path.read_bytes()
        path.write_bytes(b'bad png');row['sha256']=hashlib.sha256(path.read_bytes()).hexdigest()
        with self.assertRaises(SmokeFailure):self.full_check()
        path.unlink();target=self.game/'else.png';target.write_bytes(original);path.symlink_to(target)
        row['sha256']=hashlib.sha256(original).hexdigest()
        with self.assertRaises(SmokeFailure):self.full_check()


if __name__=='__main__':unittest.main()
