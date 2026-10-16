import copy,struct,tempfile,unittest,zlib
from pathlib import Path
from fluid_extensions import FLUIDS,MODES,validate_generic,validate_submerged
from client_mixin_smoke import SmokeFailure
class FluidExtensionProofTests(unittest.TestCase):
    def setUp(self):
        self.generic=dict(suite='generic-fluid-client-v1',nonce='current',status='passed',cases=[dict(fluid=f,state=k,sync=s,server_client_region_agree=True,drain_cleared=True) for f in FLUIDS for k in range(5) for s in ('update','initial')])
        self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup);self.root=Path(self.tmp.name);(self.root/'submerged-catwalk-screenshots').mkdir()
        def chunk(k,p):return struct.pack('>I',len(p))+k+p+struct.pack('>I',zlib.crc32(k+p))
        image=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',1280,720,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(bytes(720*(1+1280*3))))+chunk(b'IEND',b'')
        cases=[]
        for v in range(6):
            for m in MODES:
                name=f'variant-{v}-{m}.png';(self.root/'submerged-catwalk-screenshots'/name).write_bytes(image)
                cases.append(dict(variant=v,mode=m,server_client_agree=True,paid_state_preserved=True,immersed_camera=m!='dry',source=m=='source-submerged',amount=0 if m=='dry' else 8 if m=='source-submerged' else 7,screenshot=name))
        self.submerged=dict(suite='submerged-catwalk-v1',nonce='current',status='passed',cases=cases)
    def test_complete(self):
        validate_generic(self.generic,'current');validate_submerged(self.submerged,'current',self.root)
    def test_stale_or_incomplete(self):
        for proof,call in [(self.generic,lambda p,n:validate_generic(p,n)),(self.submerged,lambda p,n:validate_submerged(p,n,self.root))]:
            with self.assertRaises(SmokeFailure):call(proof,'stale')
            for duplicate in (False,True):
                p=copy.deepcopy(proof)
                if duplicate:p['cases'][-1]=p['cases'][0]
                else:p['cases'].pop()
                with self.assertRaises(SmokeFailure):call(p,'current')
    def test_missing_sync_or_drain(self):
        for field in ('server_client_region_agree','drain_cleared'):
            p=copy.deepcopy(self.generic);p['cases'][0][field]=False
            with self.assertRaises(SmokeFailure):validate_generic(p,'current')
    def test_camera_paid_and_source_identity(self):
        for field,value in [('immersed_camera',False),('paid_state_preserved',False),('source',False),('amount',0)]:
            p=copy.deepcopy(self.submerged);p['cases'][1][field]=value
            with self.assertRaises(SmokeFailure):validate_submerged(p,'current',self.root)
    def test_image_missing_or_wrong_path(self):
        p=copy.deepcopy(self.submerged);p['cases'][0]['screenshot']='../other.png'
        with self.assertRaises(SmokeFailure):validate_submerged(p,'current',self.root)
        (self.root/'submerged-catwalk-screenshots'/self.submerged['cases'][0]['screenshot']).write_bytes(b'bad')
        with self.assertRaises(SmokeFailure):validate_submerged(self.submerged,'current',self.root)
