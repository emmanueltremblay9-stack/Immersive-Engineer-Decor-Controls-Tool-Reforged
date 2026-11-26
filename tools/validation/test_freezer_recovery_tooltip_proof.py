import copy
import hashlib
import struct
from pathlib import Path
import tempfile
import unittest
from freezer_recovery_tooltip_proof import validate_tooltip, EXPECTED
from client_mixin_smoke import SmokeFailure

class FreezerRecoveryTooltipProofTest(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup);self.game=Path(self.tmp.name)
        data=b'\x89PNG\r\n\x1a\n'+b'\0'*8+struct.pack('>II',320,260)
        (self.game/'freezer-recovery-tooltip.png').write_bytes(data)
        self.p=dict(nonce='n',source_sha='s',language='fr_fr',frames=3,lines=[EXPECTED['fr_fr']],expected_text=EXPECTED['fr_fr'],styles=['{}'],x=8,y=8,tooltip_width=200,tooltip_height=8,viewport_width=320,viewport_height=260,in_bounds=True,capture_sha256=hashlib.sha256(data).hexdigest(),translation_key='gui.immersive_engineer_decor_controls_tool_reforged.freezer_reference.recover_help',hovered_control='freezer_recovery_button',button_canvas_x=238,button_canvas_y=199,button_canvas_width=16,button_canvas_height=16,pointer_canvas_x=246,pointer_canvas_y=207)
    def test_complete(self):self.assertTrue(validate_tooltip(self.p,self.game,'n','s','fr_fr'))
    def test_mutations(self):
        for key,value in [('frames',0),('nonce','stale'),('styles',[]),('lines',['missing']),('tooltip_width',241),('x',-2),('in_bounds',False),('capture_sha256','bad'),('viewport_width',640),('expected_text',''),('tooltip_height',True),('translation_key','wrong'),('hovered_control','slot'),('button_canvas_x',True),('pointer_canvas_x',0)]:
            with self.subTest(key=key):
                p=copy.deepcopy(self.p);p[key]=value
                with self.assertRaises(SmokeFailure):validate_tooltip(p,self.game,'n','s','fr_fr')
