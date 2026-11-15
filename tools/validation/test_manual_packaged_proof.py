import copy
import hashlib
import tempfile
import struct
import unittest
from pathlib import Path
from zipfile import ZipFile
from manual_packaged_proof import validate_proof
from client_mixin_smoke import SmokeFailure


class NativeManualProofTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.game = Path(self.temp.name)
        jar = self.game / 'normal.jar'
        names = ['manual/en_us/small_milking_machine.txt', 'manual/en_us/workshop_machines.txt', 'manual/small_milking_machine.json', 'manual/workshop_machines.json']
        with ZipFile(jar, 'w') as z:
            z.writestr('com/oblixorprime/engineersdecorreforged/client/ManualViewportFit.class', b'fit')
            for name in names:
                z.writestr('assets/immersive_engineer_decor_controls_tool_reforged/' + name, b'manual')
        digest = hashlib.sha256(jar.read_bytes()).hexdigest()
        self.report = dict(jar_path=str(jar), jar_sha256=digest)
        self.proof = dict(suite='native-manual-pagination-v1', status='PASS', source_sha='a'*40, nonce='b'*32,
                          language='en_us', width=1280, height=720, gui_scale=2,
                          runtime=dict(jar_sha256=digest, fixture_free_jar=True, separate_harness_module=True,
                                       class_origins=[dict(**{'class':'com.oblixorprime.engineersdecorreforged.client.ManualViewportFit'},sha256=hashlib.sha256(b'fit').hexdigest())],
                                       active_resources=[dict(path=n, sha256=hashlib.sha256(b'manual').hexdigest()) for n in names]),
                          groups=[], pages=[], captures=[], transitions=[])
        for entry in ['small_milking_machine', 'workshop_machines']:
            self.proof['transitions'].append(dict(entry='immersive_engineer_decor_controls_tool_reforged:'+entry,
                other_entry='immersiveengineering:control', roundtrip=True, reopen=True, resize_reinit=True,
                config_unchanged=True, physical_resize_restored=True, resized_width=960, resized_height=540))
            self.proof['groups'].append(dict(entry='mod:'+entry, start_page=0, end_page_exclusive=2, captured_pages=2,
                body_complete=True, recipe_present=True, navigation_forward=1, navigation_backward=1))
            for page in range(2):
                path = f'{entry}-{page}.png'
                raw = b'\x89PNG\r\n\x1a\n' + struct.pack('>I', 13) + b'IHDR' + struct.pack('>II', 1280, 720) + b'x'*1024
                (self.game/path).write_bytes(raw)
                self.proof['captures'].append(dict(path=path, sha256=hashlib.sha256(raw).hexdigest(), width=1280, height=720, entry='mod:'+entry, page=page))
                self.proof['pages'].append(dict(entry='mod:'+entry, page=page, lines=['abc'], line_widths=[15],
                    page_width=120, page_height=148, text_height=10, special_height=0, bounds_ok=True, capture=copy.deepcopy(self.proof['captures'][-1])))

    def validate(self):
        validate_proof(self.proof,'b'*32,'a'*40,self.game,'MANUAL_PAGINATION_PASS:'+'b'*32,0,False,1280,720,'en_us',2,self.report)
    def test_valid(self): self.validate()
    def test_wrong_commit(self):
        self.proof['source_sha']='c'*40
        with self.assertRaises(SmokeFailure): self.validate()
    def test_missing_last_page(self):
        self.proof['pages'].pop(); self.proof['captures'].pop()
        with self.assertRaises(SmokeFailure): self.validate()
    def test_horizontal_overflow(self):
        self.proof['pages'][0]['line_widths']=[121]
        with self.assertRaises(SmokeFailure): self.validate()
    def test_vertical_overflow(self):
        self.proof['pages'][0]['special_height']=150
        with self.assertRaises(SmokeFailure): self.validate()
    def test_missing_recipe(self):
        self.proof['groups'][0]['recipe_present']=False
        with self.assertRaises(SmokeFailure): self.validate()
    def test_missing_text(self):
        self.proof['groups'][0]['body_complete']=False
        with self.assertRaises(SmokeFailure): self.validate()
    def test_changed_capture(self):
        (self.game/self.proof['captures'][0]['path']).write_bytes(b'bad')
        with self.assertRaises(SmokeFailure): self.validate()
    def test_changed_resource(self):
        self.proof['runtime']['active_resources'][0]['sha256']='0'*64
        with self.assertRaises(SmokeFailure): self.validate()
    def test_capture_wrong_page(self):
        self.proof['captures'][0]['page']=77
        with self.assertRaises(SmokeFailure): self.validate()
    def test_wrong_scale(self):
        self.proof['gui_scale']=1
        with self.assertRaises(SmokeFailure): self.validate()
    def test_missing_isolation(self):
        self.proof['runtime']['fixture_free_jar']=False
        with self.assertRaises(SmokeFailure): self.validate()
    def test_duplicate_capture(self):
        self.proof['captures'][1]=copy.deepcopy(self.proof['captures'][0])
        with self.assertRaises(SmokeFailure): self.validate()
    def test_missing_transitions(self):
        self.proof['transitions']=[]
        with self.assertRaises(SmokeFailure): self.validate()
    def test_config_changed(self):
        self.proof['transitions'][0]['config_unchanged']=False
        with self.assertRaises(SmokeFailure): self.validate()
    def test_missing_resize(self):
        self.proof['transitions'][0]['physical_resize_restored']=False
        with self.assertRaises(SmokeFailure): self.validate()
    def test_wrong_fit_class(self):
        self.proof['runtime']['class_origins'][0]['sha256']='0'*64
        with self.assertRaises(SmokeFailure): self.validate()

if __name__ == '__main__': unittest.main()
