import copy
import hashlib
import math
from pathlib import Path
import struct
import tempfile
import unittest
import zlib

from solar_v2_reference_native import (ART_SHA, CASES, CAPTURES, SCREEN_CLASS, SUITE,
                                    SmokeFailure, expected_geometry, validate_proof, image_evidence, decode_png)


def png_bytes(w,h,seed):
    def chunk(name,data):
        return struct.pack('>I',len(data))+name+data+struct.pack('>I',zlib.crc32(name+data))
    return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+chunk(b'IDAT',zlib.compress((b'\0'+bytes([seed,0,0,255])*w)*h))+chunk(b'IEND',b'')


def png_rows(w,h,rows):
    def chunk(name,data):
        return struct.pack('>I',len(data))+name+data+struct.pack('>I',zlib.crc32(name+data))
    return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(b''.join(b'\0'+r for r in rows)))+chunk(b'IEND',b'')


class NativeSolarV2ProofTest(unittest.TestCase):
    CACHE={}
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup)
        self.game=Path(self.tmp.name)/'run';self.folder=self.game/'solar-v2-reference-screenshots';self.folder.mkdir(parents=True)
        self.nonce='a'*32;self.sha='b'*40
        self.logs=f'IEDCTR_SOLAR_V2_REFERENCE PASSED nonce={self.nonce}'
        self.proof={'suite':SUITE,'nonce':self.nonce,'executed_source_sha':self.sha,'status':'passed_bounded_smoke',
                    'source_art_sha256':ART_SHA,'actual_integrated_world':True,'native_visual_acceptance':False,
                    'selected_language':'en_us','menu_slot_count':37,'slot_routes':list(range(37)),
                    'cases':sorted(CASES),'captures':[], 'cell_item':'immersiveengineering:capacitor_lv',
                    'cell_initial_buffer_fe':1000,'cell_initial_item_fe':0,'cell_charged_fe':256,'cell_remaining_buffer_fe':744}
        self.set_captures(720)

    def set_captures(self,height,width=1280,gui_scale=1):
        self.width=width;self.height=height;self.gui_scale=gui_scale;self.proof['captures']=[]
        key=(width,height,gui_scale)
        if key in self.CACHE:
            metadata,files=self.CACHE[key]
            self.proof.update(copy.deepcopy(metadata))
            for name,data in files.items():(self.folder/name).write_bytes(data)
            return
        viewport,scale,(tx,ty,tw,th)=expected_geometry(width,height,gui_scale)
        self.proof.update(viewport)
        for i,(name,fraction) in enumerate(CAPTURES.items()):
            rows=[]
            for y in range(height):
                row=bytearray(bytes([30,30,30,255])*width)
                if ty<=y<ty+th:
                    row[tx*4:(tx+tw)*4]=bytes([0,0,0,255])*tw
                    fill=tw*fraction//2
                    row[tx*4:(tx+fill)*4]=bytes([242,164,50,255])*fill
                rows.append(bytes(row))
            data=png_rows(width,height,rows);(self.folder/name).write_bytes(data)
            tn=name[:-4]+'-energy.png';tank=png_rows(tw,th,[row[tx*4:(tx+tw)*4] for row in rows[ty:ty+th]]);(self.folder/tn).write_bytes(tank)
            ring=image_evidence(self.folder/name,self.folder/tn,width,height,gui_scale)
            self.proof['captures'].append({'name':name,'sha256':hashlib.sha256(data).hexdigest(),
                **viewport,'width':width,'height':height,'view_scale':scale,'menu_id':1,'screen_class':SCREEN_CLASS,
                'capacity':64000,'amount':64000*fraction//2,'comparator':math.ceil(fraction*7.5),'sync_granularity_fe':100,'energy_name':tn,'energy_width':tw,'energy_height':th,
                'energy_x':tx,'energy_y':ty,
                'energy_sha256':hashlib.sha256(tank).hexdigest(),'frame_exterior_sha256':ring})
        self.CACHE[key]=({k:copy.deepcopy(self.proof[k]) for k in (*viewport,'captures')},
                         {f.name:f.read_bytes() for f in self.folder.glob('*.png')})

    def validate(self,proof=None,**kwargs):
        opts=dict(logs=self.logs,exit_code=0,timed_out=False,width=self.width,height=self.height,
                  language=self.proof['selected_language'],gui_scale=self.gui_scale);opts.update(kwargs)
        return validate_proof(proof or self.proof,self.nonce,self.sha,self.game,**opts)

    def test_complete_bounded_en720(self):self.validate()
    def test_complete_bounded_fr260(self):
        self.set_captures(260);self.proof['selected_language']='fr_fr';self.validate()
    def test_complete_bounded_hd_scales_1_2_3(self):
        for gui_scale in (1,2,3):
            with self.subTest(gui_scale=gui_scale):
                self.set_captures(1080,1920,gui_scale);self.validate()
    def test_expected_framebuffer_geometry(self):
        profiles=[(1280,720,1,(480,347,283,12)),(1280,260,1,(546,123,166,6)),
                  (1920,1080,1,(800,527,283,12)),(1920,1080,2,(640,514,566,24)),
                  (1920,1080,3,(561,510,705,27))]
        for w,h,s,crop in profiles:
            self.assertEqual(expected_geometry(w,h,s)[2],crop)
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
    def test_unscaled_energy_crop_cannot_pass(self):
        self.set_captures(1080,1920,3)
        p=copy.deepcopy(self.proof);c=p['captures'][0]
        c['energy_width']=30;c['energy_height']=58
        data=png_bytes(30,58,0);(self.folder/c['energy_name']).write_bytes(data)
        c['energy_sha256']=hashlib.sha256(data).hexdigest()
        with self.assertRaises(SmokeFailure):self.validate(p)
        # Even correct metadata and hash must not disguise wrong PNG dimensions.
        p['captures'][0]['energy_width']=90;p['captures'][0]['energy_height']=174
        with self.assertRaises(SmokeFailure):self.validate(p)
    def test_identity_and_scope_fail_closed(self):
        for key,value in [('suite','other'),('nonce','stale'),('executed_source_sha','0'*40),('source_art_sha256','0'*64),
                          ('status','passed'),('actual_integrated_world',False),('native_visual_acceptance',True),
                          ('menu_slot_count',39),('selected_language','de_de')]:
            with self.subTest(key=key):
                p=copy.deepcopy(self.proof);p[key]=value
                with self.assertRaises(SmokeFailure):self.validate(p)
    def test_missing_duplicate_or_bool_slot_routes(self):
        for value in [list(range(36)),list(range(36))+[35],[False]+list(range(1,37))]:
            p=copy.deepcopy(self.proof);p['slot_routes']=value
            with self.assertRaises(SmokeFailure):self.validate(p)
    def test_cases_must_be_complete(self):
        for value in [[],sorted(CASES)+[sorted(CASES)[0]]]:
            p=copy.deepcopy(self.proof);p['cases']=value
            with self.assertRaises(SmokeFailure):self.validate(p)
    def test_capture_metadata_rejected(self):
        for key,value in [('name','../escape.png'),('width',1279),('view_scale',float('nan')),('view_scale',.1),
                          ('capacity',0),('amount',1),('comparator',9),('sync_granularity_fe',1),('screen_class','MachineScreen'),('menu_id',0),
                          ('energy_x',620),('energy_y',244),('energy_width',999),('energy_height',999),
                          ('energy_name','../evil.png'),('frame_exterior_sha256','0'*64)]:
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
        (self.folder/'empty-energy.png').unlink()
        with self.assertRaises(SmokeFailure):self.validate()
    def test_symlink_image(self):
        path=self.folder/'empty.png';target=self.folder/'actual.png';path.rename(target);path.symlink_to(target)
        with self.assertRaises(SmokeFailure):self.validate()
    def test_duplicate_energy_crops(self):
        src=self.folder/'empty-energy.png';data=src.read_bytes()
        p=copy.deepcopy(self.proof)
        for c in p['captures']:
            (self.folder/c['energy_name']).write_bytes(data);c['energy_sha256']=hashlib.sha256(data).hexdigest()
        with self.assertRaises(SmokeFailure):self.validate(p)
    def test_cell_charge_evidence_required(self):
        for key,value in [('cell_item','minecraft:stone'),('cell_charged_fe',0),('cell_charged_fe',True),
                          ('cell_remaining_buffer_fe',1000),('cell_initial_item_fe',1)]:
            p=copy.deepcopy(self.proof);p[key]=value
            with self.assertRaises(SmokeFailure):self.validate(p)
    def test_forged_exterior_digest_rejected(self):
        p=copy.deepcopy(self.proof)
        for c in p['captures']:c['frame_exterior_sha256']='0'*64
        with self.assertRaises(SmokeFailure):self.validate(p)
    def test_crop_must_match_screenshot(self):
        p=copy.deepcopy(self.proof);c=p['captures'][0]
        data=png_bytes(c['energy_width'],c['energy_height'],99)
        (self.folder/c['energy_name']).write_bytes(data);c['energy_sha256']=hashlib.sha256(data).hexdigest()
        with self.assertRaises(SmokeFailure):self.validate(p)
    def test_png_crc_rejected_even_with_matching_file_hash(self):
        p=copy.deepcopy(self.proof);c=p['captures'][0];path=self.folder/c['name']
        data=bytearray(path.read_bytes());data[-1]^=1;path.write_bytes(data);c['sha256']=hashlib.sha256(data).hexdigest()
        with self.assertRaises(SmokeFailure):self.validate(p)
    def test_unclean_finish(self):
        for kwargs in [dict(exit_code=1),dict(timed_out=True),dict(logs='no marker')]:
            with self.assertRaises(SmokeFailure):self.validate(**kwargs)
    def test_crash_report(self):
        d=self.game/'crash-reports';d.mkdir();(d/'crash.txt').write_text('crash')
        with self.assertRaises(SmokeFailure):self.validate()

if __name__=='__main__':unittest.main()
