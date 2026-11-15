import copy
from pathlib import Path
import unittest
from unittest.mock import patch
from gui_album_three import validate, CASES, SCREENS, SmokeFailure
class ProofTests(unittest.TestCase):
    def setUp(self):
        self.jar=Path('/tmp/album.jar');self.digest='a'*64
        self.proof=dict(suite='gui-album-three-v1',status='observed',nonce='b'*32,executed_source_sha='c'*40,visual_acceptance=False,physical_os_input=False,
            packaged_runtime=dict(jar_path=str(self.jar.resolve()),jar_sha256=self.digest,fixture_free_jar=True,separate_harness_module=True),
            cases=[dict(case=n,screen='com.oblixorprime.engineersdecorreforged.client.'+('MachineScreen$' if i!=4 else '')+SCREENS[i],server_menu_acknowledged=True,screen_class_sha256='d'*64,capture=n+'.png',capture_sha256='e'*64) for i,n in enumerate(CASES)])
    def check(self,p):
        with patch('gui_album_three.png') as png:
            validate(p,'b'*32,'c'*40,self.jar,self.digest,Path('/tmp/run'),1280,720)
            self.assertEqual(png.call_count,6)
    def test_accept_structural_proof(self):self.check(self.proof)
    def test_reject_missing_screen(self):
        self.proof['cases'].pop()
        with self.assertRaises(SmokeFailure):self.check(self.proof)
    def test_reject_bad_hash(self):
        self.proof['packaged_runtime']['jar_sha256']='f'*64
        with self.assertRaises(SmokeFailure):self.check(self.proof)
    def test_reject_wrong_provenance(self):
        self.proof['cases'][0]['screen_class_sha256']=''
        with self.assertRaises(SmokeFailure):self.check(self.proof)
    def test_reject_human_claim(self):
        self.proof['physical_os_input']=True
        with self.assertRaises(SmokeFailure):self.check(self.proof)
    def test_reject_unsynchronized_menu(self):
        self.proof['cases'][2]['server_menu_acknowledged']=False
        with self.assertRaises(SmokeFailure):self.check(self.proof)
