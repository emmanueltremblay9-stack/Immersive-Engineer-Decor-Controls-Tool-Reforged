import copy
import hashlib
from pathlib import Path
import struct
import tempfile
import unittest
import zlib

from barrel_reference_native import (ART_SHA, CASES, CAPTURES, SCREEN_CLASS, SUITE,
                                    SmokeFailure, expected_geometry, validate_proof)


def png_bytes(w,h,seed):
    def chunk(name,data):
        return struct.pack('>I',len(data))+name+data+struct.pack('>I',zlib.crc32(name+data))
    return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+chunk(b'IDAT',zlib.compress((b'\0'+bytes([seed,0,0,255])*w)*h))+chunk(b'IEND',b'')


class NativeBarrelProofTest(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup)
        self.game=Path(self.tmp.name)/'run';self.folder=self.game/'barrel-reference-screenshots';self.folder.mkdir(parents=True)
        self.nonce='a'*32;self.sha='b'*40
        self.logs=f'IEDCTR_BARREL_REFERENCE PASSED nonce={self.nonce}'
        self.proof={'suite':SUITE,'nonce':self.nonce,'executed_source_sha':self.sha,'status':'passed_bounded_smoke',
                    'source_art_sha256':ART_SHA,'actual_integrated_world':True,'native_visual_acceptance':False,
                    'selected_language':'en_us','menu_slot_count':38,'slot_routes':list(range(38)),
                    'cases':sorted(CASES),'captures':[]}
        self.set_captures(720)

    def set_captures(self,height,width=1280,gui_scale=1):
        self.width=width;self.height=height;self.gui_scale=gui_scale;self.proof['captures']=[]
        viewport,scale,(tx,ty,tw,th)=expected_geometry(width,height,gui_scale)
        self.proof.update(viewport)
        for i,(name,fraction) in enumerate(CAPTURES.items()):
            data=png_bytes(width,height,i);(self.folder/name).write_bytes(data)
            tn=name[:-4]+'-tank.png';tank=png_bytes(tw,th,i);(self.folder/tn).write_bytes(tank)
            self.proof['captures'].append({'name':name,'sha256':hashlib.sha256(data).hexdigest(),
                **viewport,'width':width,'height':height,'view_scale':scale,'menu_id':1,'screen_class':SCREEN_CLASS,
                'capacity':16000,'amount':16000*fraction//2,'tank_name':tn,'tank_width':tw,'tank_height':th,
                'tank_x':tx,'tank_y':ty,
                'tank_sha256':hashlib.sha256(tank).hexdigest(),'frame_ring_sha256':'f'*64})

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
