import copy
import hashlib
import math
from pathlib import Path
import struct
import tempfile
import unittest
import zlib

from barrel_reference_native import ART_SHA, CASES, CAPTURES, SCREEN_CLASS, SUITE, SmokeFailure, validate_proof


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

    def set_captures(self,height):
        self.height=height;self.proof['captures']=[]
        scale=min(1,(1280-12)/379,(height-12)/461);ox=(1280-379*scale)/2;oy=(height-461*scale)/2
        tw=math.floor(ox+211*scale)-math.ceil(ox+170*scale)
        th=math.floor(oy+193*scale)-math.ceil(oy+115*scale)
        for i,(name,fraction) in enumerate(CAPTURES.items()):
            data=png_bytes(1280,height,i);(self.folder/name).write_bytes(data)
            tn=name[:-4]+'-tank.png';tank=png_bytes(tw,th,i);(self.folder/tn).write_bytes(tank)
            self.proof['captures'].append({'name':name,'sha256':hashlib.sha256(data).hexdigest(),
                'width':1280,'height':height,'view_scale':scale,'menu_id':1,'screen_class':SCREEN_CLASS,
                'capacity':16000,'amount':16000*fraction//2,'tank_name':tn,'tank_width':tw,'tank_height':th,
                'tank_sha256':hashlib.sha256(tank).hexdigest(),'frame_ring_sha256':'f'*64})

    def validate(self,proof=None,**kwargs):
        opts=dict(logs=self.logs,exit_code=0,timed_out=False,width=1280,height=self.height,language=self.proof['selected_language']);opts.update(kwargs)
        return validate_proof(proof or self.proof,self.nonce,self.sha,self.game,**opts)

    def test_complete_bounded_en720(self):self.validate()
    def test_complete_bounded_fr260(self):
        self.set_captures(260);self.proof['selected_language']='fr_fr';self.validate()
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
                          ('tank_width',999),('tank_name','../evil.png'),('frame_ring_sha256','0'*64)]:
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
