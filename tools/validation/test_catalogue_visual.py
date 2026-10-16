import copy,json,struct,tempfile,unittest,zlib
from pathlib import Path
from catalogue_visual import BLOCKS,MODES,validate_catalogue
from client_mixin_smoke import SmokeFailure
class CatalogueVisualTests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup);self.path=Path(self.tmp.name)
        (self.path/'catalogue-screenshots').mkdir();cases=[]
        def chunk(kind,payload):return struct.pack('>I',len(payload))+kind+payload+struct.pack('>I',zlib.crc32(kind+payload))
        png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',1280,720,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(bytes(720*(1+1280*3))))+chunk(b'IEND',b'')
        for b in BLOCKS:
            for m in MODES:
                c=dict(block='immersive_engineer_decor_controls_tool_reforged:'+b,mode=m,source_before=m!='natural-water',amount_before=7 if m=='natural-water' else 8,fluid_vertices=16,server_client_agree=True,geometry_and_paid_state_preserved=True,after_empty=True)
                for phase in ('before','after'):
                    name=f'{b}-{m}-{phase}.png';c[phase]=name
                    (self.path/'catalogue-screenshots'/name).write_bytes(png)
                cases.append(c)
        self.proof=dict(suite='native-catalogue-visual-v1',nonce='current',status='passed',cases=cases)
    def test_complete(self):self.assertEqual(validate_catalogue(self.proof,'current',self.path),self.proof)
    def test_wrong_namespace(self):
        p=copy.deepcopy(self.proof);p['cases'][0]['block']='minecraft:iron_bulb_light'
        with self.assertRaises(SmokeFailure):validate_catalogue(p,'current',self.path)
    def test_wrong_nonce(self):
        with self.assertRaises(SmokeFailure):validate_catalogue(self.proof,'old',self.path)
    def test_missing_or_duplicate(self):
        for duplicate in (False,True):
            p=copy.deepcopy(self.proof)
            if duplicate:p['cases'][-1]=p['cases'][0]
            else:p['cases'].pop()
            with self.assertRaises(SmokeFailure):validate_catalogue(p,'current',self.path)
    def test_promotion_recession_or_mesh(self):
        for field,value,index in [('source_before',True,3),('after_empty',False,0),('fluid_vertices',0,0),('geometry_and_paid_state_preserved',False,0),('server_client_agree',False,0)]:
            p=copy.deepcopy(self.proof);p['cases'][index][field]=value
            with self.assertRaises(SmokeFailure):validate_catalogue(p,'current',self.path)
    def test_path_and_small_image(self):
        p=copy.deepcopy(self.proof);p['cases'][0]['before']='../other.png'
        with self.assertRaises(SmokeFailure):validate_catalogue(p,'current',self.path)
        path=self.path/'catalogue-screenshots'/self.proof['cases'][0]['before'];path.write_bytes(b'bad')
        with self.assertRaises(SmokeFailure):validate_catalogue(self.proof,'current',self.path)
