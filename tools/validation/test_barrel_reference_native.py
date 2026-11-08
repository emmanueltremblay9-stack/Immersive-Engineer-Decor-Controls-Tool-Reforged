import copy
import hashlib
from pathlib import Path
import struct
import tempfile
import unittest
import zlib

from barrel_reference_native import (ART_SHA, CASES, CAPTURES, SCREEN_CLASS, SUITE,
                                    SmokeFailure, expected_geometry, expected_presentation_bounds, validate_proof,
                                    capture_state, image_evidence, decode_png)


def png_rows(w,h,rows):
    def chunk(name,data):
        return struct.pack('>I',len(data))+name+data+struct.pack('>I',zlib.crc32(name+data))
    return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(b''.join(b'\0'+row for row in rows)))+chunk(b'IEND',b'')


def png_bytes(w,h,seed):
    return png_rows(w,h,[bytes([seed,0,0,255])*w]*h)


class NativeBarrelProofTest(unittest.TestCase):
    CACHE={}
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup)
        self.game=Path(self.tmp.name)/'run';self.folder=self.game/'barrel-reference-screenshots';self.folder.mkdir(parents=True)
        self.nonce='a'*32;self.sha='b'*40
        self.logs=f'IEDCTR_BARREL_REFERENCE PASSED nonce={self.nonce}'
        self.proof={'suite':SUITE,'nonce':self.nonce,'executed_source_sha':self.sha,'status':'passed_bounded_smoke',
                    'source_art_sha256':ART_SHA,'actual_integrated_world':True,'native_visual_acceptance':False,
                    'selected_language':'en_us','menu_slot_count':38,'slot_routes':list(range(38)),
                    'cases':sorted(CASES),'captures':[]}
        state={'menu_id':1,'fluid_id':'minecraft:milk','amount':16000,'capacity':16000,
               'slot_count':38,'total_items':0,'inventory_items':0,'dimension':'minecraft:overworld',
               'player_uuid':'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee'}
        self.proof['post_reload_slot_routes']=list(range(38))
        self.proof['resource_reload']={'method':'Minecraft.reloadResourcePacks','requested_count':1,'completed_count':1,
            'overlay_dismissed':True,'same_world_and_server':True,'same_menu_and_machine':True,
            'same_player_and_screen':True,'state_preserved':True,'fixture_drain_confirmed':True,
            'settle_ticks':12,'fixture_drain_amount':16000,
            'before':{'client':copy.deepcopy(state),'server':copy.deepcopy(state)},
            'after':{'client':copy.deepcopy(state),'server':copy.deepcopy(state)}}
        self.set_captures(720)

    def set_captures(self,height,width=1280,gui_scale=1):
        self.width=width;self.height=height;self.gui_scale=gui_scale;self.proof['captures']=[]
        key=(width,height,gui_scale)
        if key in self.CACHE:
            metadata,files=self.CACHE[key];self.proof.update(copy.deepcopy(metadata))
            for name,data in files.items():(self.folder/name).write_bytes(data)
            return
        viewport,scale,(tx,ty,tw,th)=expected_geometry(width,height,gui_scale)
        self.proof.update(viewport)
        for name,fraction in CAPTURES.items():
            phase,fluid_id=capture_state(name)
            seed={'minecraft:empty':0,'minecraft:water':60,'minecraft:lava':120,'minecraft:milk':180}[fluid_id]
            if fraction and phase!='baseline':seed+=1  # Animation phase may change on reload.
            rows=[]
            for y in range(height):
                row=bytearray(bytes([30,30,30,255])*width)
                if ty<=y<ty+th:
                    color=bytes([seed,0,0,255]) if y>=ty+th-th*fraction//2 else bytes([0,0,0,255])
                    row[tx*4:(tx+tw)*4]=color*tw
                rows.append(bytes(row))
            data=png_rows(width,height,rows);(self.folder/name).write_bytes(data)
            tn=name[:-4]+'-tank.png';tank=png_rows(tw,th,[row[tx*4:(tx+tw)*4] for row in rows[ty:ty+th]])
            (self.folder/tn).write_bytes(tank)
            ring=image_evidence(self.folder/name,self.folder/tn,width,height,gui_scale)
            self.proof['captures'].append({'name':name,'sha256':hashlib.sha256(data).hexdigest(),
                **viewport,**expected_presentation_bounds(width,height,gui_scale),
                'width':width,'height':height,'view_scale':scale,'menu_id':1,'screen_class':SCREEN_CLASS,
                'capacity':16000,'amount':16000*fraction//2,'tank_name':tn,'tank_width':tw,'tank_height':th,
                'tank_x':tx,'tank_y':ty,'phase':phase,'fluid_id':fluid_id,
                'tank_sha256':hashlib.sha256(tank).hexdigest(),'frame_ring_sha256':ring})
        self.CACHE[key]=({k:copy.deepcopy(self.proof[k]) for k in (*viewport,'captures')},
                         {f.name:f.read_bytes() for f in self.folder.glob('*.png')})

    def validate(self,proof=None,**kwargs):
        opts=dict(logs=self.logs,exit_code=0,timed_out=False,width=self.width,height=self.height,
                  language=self.proof['selected_language'],gui_scale=self.gui_scale);opts.update(kwargs)
        return validate_proof(self.proof if proof is None else proof,self.nonce,self.sha,self.game,**opts)

    def test_complete_bounded_en720(self):self.validate()
    def test_complete_bounded_fr260(self):
        self.set_captures(260);self.proof['selected_language']='fr_fr';self.validate()
    def test_complete_bounded_hd_scales_1_2_3(self):
        for gui_scale in (1,2,3):
            with self.subTest(gui_scale=gui_scale):
                self.set_captures(1080,1920,gui_scale);self.validate()
    def test_expected_framebuffer_geometry(self):
        # Fixed independent coordinates catch pre-rounding, double-vs-float and
        # framebuffer/scaled-width ratio errors. Odd sizes must use ceil GUI sizes.
        profiles=[(1280,720,1,1280,720,1.0,(621,245,40,77)),
                  (1280,260,1,1280,260,0.537960946559906,(630,68,21,41)),
                  (1920,1080,1,1920,1080,1.0,(941,425,40,77)),
                  (1920,1080,2,960,540,1.0,(922,310,80,154)),
                  (1920,1080,3,640,360,0.7548806667327881,(918,279,90,174)),
                  (1921,1081,3,641,361,0.7570498585700989,(918,282,90,174))]
        for w,h,s,gw,gh,expected_scale,expected_crop in profiles:
            with self.subTest(width=w,height=h,gui_scale=s):
                viewport,scale,crop=expected_geometry(w,h,s)
                self.assertEqual((viewport['gui_width'],viewport['gui_height']),(gw,gh))
                self.assertEqual(scale,expected_scale)
                self.assertEqual(crop,expected_crop)
    def test_odd_framebuffer_scale_3(self):
        self.set_captures(1081,1921,3);self.validate()
    def test_impossible_or_invalid_viewport(self):
        for width,height,scale in [(1280,719,3),(1280,260,2),(319,240,1),(320,239,1),
                                   (1920,1080,0),(1920,1080,4),(1920,1080,True),
                                   (1920.0,1080,1),(1920,1080.0,1)]:
            with self.subTest(width=width,height=height,gui_scale=scale):
                with self.assertRaises(SmokeFailure):expected_geometry(width,height,scale)
    def test_scale_1_fallback_cannot_pass_scale_3_request(self):
        self.set_captures(1080,1920,1)
        with self.assertRaises(SmokeFailure):self.validate(gui_scale=3)
        # Changing only the requested scale cannot disguise actual scale 1.
        self.proof['requested_gui_scale']=3
        with self.assertRaises(SmokeFailure):self.validate(gui_scale=3)
    def test_viewport_metadata_fail_closed(self):
        self.set_captures(1080,1920,3)
        for capture in (False,True):
            for key,value in [('requested_gui_scale',1),('requested_gui_scale',3.0),
                              ('actual_gui_scale',1),('actual_gui_scale',float('nan')),
                              ('actual_gui_scale',float('inf')),('actual_gui_scale',True),
                              ('gui_width',1920),('gui_height',1080),('gui_width',640.0),
                              ('framebuffer_width',640),('framebuffer_height',360)]:
                with self.subTest(capture=capture,key=key,value=value):
                    p=copy.deepcopy(self.proof);target=p['captures'][0] if capture else p;target[key]=value
                    with self.assertRaises(SmokeFailure):self.validate(p)
            for key in ('requested_gui_scale','actual_gui_scale','gui_width','gui_height',
                        'framebuffer_width','framebuffer_height'):
                with self.subTest(capture=capture,missing=key):
                    p=copy.deepcopy(self.proof);target=p['captures'][0] if capture else p;del target[key]
                    with self.assertRaises(SmokeFailure):self.validate(p)
    def test_unscaled_tank_crop_cannot_pass(self):
        self.set_captures(1080,1920,3)
        p=copy.deepcopy(self.proof);c=p['captures'][0]
        c['tank_width']=30;c['tank_height']=58
        data=png_bytes(30,58,0);(self.folder/c['tank_name']).write_bytes(data)
        c['tank_sha256']=hashlib.sha256(data).hexdigest()
        with self.assertRaises(SmokeFailure):self.validate(p)
        # Even correct metadata and hash must not disguise wrong PNG dimensions.
        p['captures'][0]['tank_width']=90;p['captures'][0]['tank_height']=174
        with self.assertRaises(SmokeFailure):self.validate(p)
    def test_public_bounds_fixed_profiles(self):
        for w,h,scale,bounds in [(1280,720,1,(450,129,380,462)),(1280,260,1,(538,6,204,248)),
                                  (1920,1080,1,(770,309,380,462)),(1920,1080,2,(290,39,380,462)),
                                  (1920,1080,3,(176,6,288,348)),(1921,1081,3,(177,6,287,349))]:
            with self.subTest(width=w,height=h,scale=scale):
                self.assertEqual(tuple(expected_presentation_bounds(w,h,scale).values()),bounds)
    def test_public_bounds_fail_closed(self):
        for key,value in expected_presentation_bounds(1280,720,1).items():
            for bad in (value+1,float(value),True,None):
                p=copy.deepcopy(self.proof);p['captures'][0][key]=bad
                with self.assertRaisesRegex(SmokeFailure,'Public presentation bounds'):self.validate(p)
        p=copy.deepcopy(self.proof)
        p['captures'][0].update(presentation_gui_left=0,presentation_gui_top=0,presentation_gui_width=379,presentation_gui_height=461)
        with self.assertRaises(SmokeFailure):self.validate(p)
    def test_reload_completion_and_preservation_fail_closed(self):
        mutations=[('method','simulated'),('requested_count',0),('requested_count',True),('completed_count',0),
                   ('completed_count',2),('completed_count',1.0),('settle_ticks',0),('settle_ticks',11),
                   ('settle_ticks',1800),('settle_ticks',12.0),('fixture_drain_amount',15999)]
        mutations += [(key,False) for key in ('overlay_dismissed','same_world_and_server','same_menu_and_machine',
                      'same_player_and_screen','state_preserved','fixture_drain_confirmed')]
        for key,value in mutations:
            with self.subTest(key=key,value=value):
                p=copy.deepcopy(self.proof);p['resource_reload'][key]=value
                with self.assertRaises(SmokeFailure):self.validate(p)
        for key in self.proof['resource_reload']:
            p=copy.deepcopy(self.proof);del p['resource_reload'][key]
            with self.assertRaises(SmokeFailure):self.validate(p)
        p=copy.deepcopy(self.proof);del p['resource_reload']
        with self.assertRaises(SmokeFailure):self.validate(p)
    def test_reload_client_server_snapshots_fail_closed(self):
        for phase in ('before','after'):
            for side in ('client','server'):
                for key,value in [('menu_id',2),('fluid_id','minecraft:water'),('amount',15000),('capacity',32000),
                                  ('slot_count',37),('total_items',1),('inventory_items',1),('inventory_items',False),
                                  ('dimension','minecraft:the_nether'),('player_uuid','bbbbbbbb-bbbb-cccc-dddd-eeeeeeeeeeee')]:
                    with self.subTest(phase=phase,side=side,key=key):
                        p=copy.deepcopy(self.proof);p['resource_reload'][phase][side][key]=value
                        with self.assertRaises(SmokeFailure):self.validate(p)
                p=copy.deepcopy(self.proof);del p['resource_reload'][phase][side]
                with self.assertRaises(SmokeFailure):self.validate(p)
    def test_post_reload_routes_fail_closed(self):
        for routes in [[],list(range(37)),list(range(37))+[36],[False]+list(range(1,38)),list(reversed(range(38)))]:
            p=copy.deepcopy(self.proof);p['post_reload_slot_routes']=routes
            with self.assertRaises(SmokeFailure):self.validate(p)
    def test_capture_phase_and_registered_fluid_fail_closed(self):
        for index in (0,7,8,14):
            for key,value in [('phase','baseline' if index else 'post_reload'),('fluid_id','neoforge:milk')]:
                p=copy.deepcopy(self.proof);p['captures'][index][key]=value
                with self.assertRaises(SmokeFailure):self.validate(p)
    def test_corrupt_png_crc_rejected_even_with_updated_file_hash(self):
        p=copy.deepcopy(self.proof);capture=p['captures'][0];path=self.folder/capture['name']
        data=bytearray(path.read_bytes());data[-1]^=1;path.write_bytes(data);capture['sha256']=hashlib.sha256(data).hexdigest()
        with self.assertRaisesRegex(SmokeFailure,'PNG CRC'):self.validate(p)
    def test_crop_must_match_framebuffer_pixels(self):
        p=copy.deepcopy(self.proof);capture=p['captures'][0];path=self.folder/capture['tank_name']
        data=png_bytes(capture['tank_width'],capture['tank_height'],99);path.write_bytes(data)
        capture['tank_sha256']=hashlib.sha256(data).hexdigest()
        with self.assertRaisesRegex(SmokeFailure,'Tank crop is not'):self.validate(p)
    def test_fabricated_ring_digest_rejected(self):
        p=copy.deepcopy(self.proof)
        for capture in p['captures']:capture['frame_ring_sha256']='f'*64
        with self.assertRaisesRegex(SmokeFailure,'digest does not match'):self.validate(p)
    def test_changed_ring_pixels_rejected_even_with_updated_hashes(self):
        p=copy.deepcopy(self.proof);capture=p['captures'][7];path=self.folder/capture['name']
        w,h,pixels=decode_png(path);rows=[bytearray(pixels[y*w*4:(y+1)*w*4]) for y in range(h)]
        # At scale1/1280x720 this is inside the audited artwork ring, outside tank.
        rows[236][611*4]^=1
        data=png_rows(w,h,rows);path.write_bytes(data);capture['sha256']=hashlib.sha256(data).hexdigest()
        capture['frame_ring_sha256']=image_evidence(path,self.folder/capture['tank_name'],w,h,1)
        with self.assertRaisesRegex(SmokeFailure,'surrounding artwork'):self.validate(p)
    def test_different_png_encodings_cannot_disguise_identical_empty_pixels(self):
        p=copy.deepcopy(self.proof)
        empty=(self.folder/'empty.png').read_bytes();tank=(self.folder/'empty-tank.png').read_bytes()
        for i,capture in enumerate(p['captures']):
            # Unique legal ancillary text changes the PNG hash without changing pixels.
            kind=b'tEXt';payload=b'Comment\0'+str(i).encode()
            chunk=struct.pack('>I',len(payload))+kind+payload+struct.pack('>I',zlib.crc32(kind+payload))
            encoded=tank[:-12]+chunk+tank[-12:]
            (self.folder/capture['name']).write_bytes(empty);capture['sha256']=hashlib.sha256(empty).hexdigest()
            (self.folder/capture['tank_name']).write_bytes(encoded);capture['tank_sha256']=hashlib.sha256(encoded).hexdigest()
        self.assertEqual(len({c['tank_sha256'] for c in p['captures']}),len(CAPTURES))
        with self.assertRaisesRegex(SmokeFailure,'distinguish empty/half/full'):self.validate(p)
    def test_animated_fluid_pixels_may_change_across_reload(self):
        # Generated phases differ inside the tank only. Both remain independently valid.
        by_name={c['name']:c for c in self.proof['captures']}
        self.assertNotEqual(by_name['milk-full.png']['tank_sha256'],by_name['reload-preserved-milk-full.png']['tank_sha256'])
        self.validate()
    def test_proof_errors_fail_closed(self):
        with self.assertRaises(SmokeFailure):self.validate([])
        p=copy.deepcopy(self.proof);p['error']='failed reload'
        with self.assertRaises(SmokeFailure):self.validate(p)
    def test_identity_and_scope_fail_closed(self):
        for key,value in [('suite','other'),('nonce','stale'),('executed_source_sha','0'*40),('source_art_sha256','0'*64),
                          ('status','passed'),('actual_integrated_world',False),('native_visual_acceptance',True),
                          ('menu_slot_count',39),('selected_language','de_de')]:
            with self.subTest(key=key):
                p=copy.deepcopy(self.proof);p[key]=value
                with self.assertRaises(SmokeFailure):self.validate(p)
    def test_missing_duplicate_or_bool_slot_routes(self):
        for value in [list(range(37)),list(range(37))+[36],[False]+list(range(1,38))]:
            p=copy.deepcopy(self.proof);p['slot_routes']=value
            with self.assertRaises(SmokeFailure):self.validate(p)
    def test_cases_must_be_complete(self):
        for value in [[],sorted(CASES)+[sorted(CASES)[0]]]:
            p=copy.deepcopy(self.proof);p['cases']=value
            with self.assertRaises(SmokeFailure):self.validate(p)
    def test_capture_metadata_rejected(self):
        for key,value in [('name','../escape.png'),('width',1279),('view_scale',float('nan')),('view_scale',.1),
                          ('capacity',0),('amount',1),('screen_class','MachineScreen'),('menu_id',0),
                          ('tank_x',620),('tank_y',244),('tank_width',999),('tank_height',999),
                          ('tank_name','../evil.png'),('frame_ring_sha256','0'*64)]:
            with self.subTest(key=key):
                p=copy.deepcopy(self.proof);p['captures'][0][key]=value
                with self.assertRaises(SmokeFailure):self.validate(p)
    def test_capture_missing_or_duplicate(self):
        p=copy.deepcopy(self.proof);p['captures'].pop()
        with self.assertRaises(SmokeFailure):self.validate(p)
        p=copy.deepcopy(self.proof);p['captures'][1]=p['captures'][0]
        with self.assertRaises(SmokeFailure):self.validate(p)
    def test_wrong_menu_or_capacity(self):
        p=copy.deepcopy(self.proof);p['captures'][0]['menu_id']=2
        with self.assertRaises(SmokeFailure):self.validate(p)
        p=copy.deepcopy(self.proof);p['captures'][0]['capacity']=32000
        with self.assertRaises(SmokeFailure):self.validate(p)
    def test_tampered_native_image(self):
        (self.folder/'empty.png').write_bytes(b'not a PNG')
        with self.assertRaises(SmokeFailure):self.validate()
    def test_missing_native_image(self):
        (self.folder/'empty-tank.png').unlink()
        with self.assertRaises(SmokeFailure):self.validate()
    def test_symlink_image(self):
        path=self.folder/'empty.png';target=self.folder/'actual.png';path.rename(target);path.symlink_to(target)
        with self.assertRaises(SmokeFailure):self.validate()
    def test_duplicate_fluid_crops(self):
        src=self.folder/'empty-tank.png';data=src.read_bytes()
        p=copy.deepcopy(self.proof)
        for c in p['captures']:
            (self.folder/c['tank_name']).write_bytes(data);c['tank_sha256']=hashlib.sha256(data).hexdigest()
        with self.assertRaises(SmokeFailure):self.validate(p)
    def test_unclean_finish(self):
        for kwargs in [dict(exit_code=1),dict(timed_out=True),dict(logs='no marker')]:
            with self.assertRaises(SmokeFailure):self.validate(**kwargs)
    def test_crash_report(self):
        d=self.game/'crash-reports';d.mkdir();(d/'crash.txt').write_text('crash')
        with self.assertRaises(SmokeFailure):self.validate()

if __name__=='__main__':unittest.main()
