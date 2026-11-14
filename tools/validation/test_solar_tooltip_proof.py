import copy
import hashlib
from pathlib import Path
import tempfile
import unittest
from solar_tooltip_proof import validate_tooltip
from client_mixin_smoke import SmokeFailure

class SolarTooltipProofTest(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup);self.game=Path(self.tmp.name)
        (self.game/'solar-fe-tooltip.png').write_bytes(b'fixture')
        self.p=dict(nonce='n',source_sha='s',language='fr_fr',frames=3,lines=['Stockage : 100 FE'],expected_text='Stockage : 100 FE',styles=['{}'],x=8,y=8,tooltip_width=200,tooltip_height=8,viewport_width=320,viewport_height=260,in_bounds=True,capture_sha256=hashlib.sha256(b'fixture').hexdigest())
    def test_complete(self):self.assertTrue(validate_tooltip(self.p,self.game,'n','s','fr_fr'))
    def test_mutations(self):
        for key,value in [('frames',0),('nonce','stale'),('styles',[]),('lines',['missing']),('tooltip_width',400),('x',-2),('in_bounds',False),('capture_sha256','bad')]:
            with self.subTest(key=key):
                p=copy.deepcopy(self.p);p[key]=value
                with self.assertRaises(SmokeFailure):validate_tooltip(p,self.game,'n','s','fr_fr')
