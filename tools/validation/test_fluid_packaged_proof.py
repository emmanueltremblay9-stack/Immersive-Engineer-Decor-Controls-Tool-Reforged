import copy
import hashlib
from pathlib import Path
import tempfile
import unittest
import zipfile
from fluid_gestures_native import SmokeFailure
from fluid_packaged_proof import CLASSES, HARNESS, MOD, TEXTURES, validate_packaged
from test_fluid_gestures_native import valid

ROOT = Path(__file__).resolve().parents[2]


class PackagedProofTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        jar = Path(self.temp.name)/'ordinary.jar'
        with zipfile.ZipFile(jar, 'w') as archive:
            for name in CLASSES:
                archive.writestr(name.replace('.', '/')+'.class', name.encode())
            for name in TEXTURES:
                archive.write(ROOT/'src/main/resources/assets'/MOD/name, 'assets/'+MOD+'/'+name)
        digest = hashlib.sha256(jar.read_bytes()).hexdigest()
        root = 'union:/exact-normal-jar/'
        self.report = dict(jar_path=str(jar), jar_sha256=digest, fixture_free_jar=True, split_packages=False,
            loaded_development_mods=[HARNESS], runtime_classpath=[str(jar), '/isolated/harness'],
            forbidden_main_outputs=['/main/classes', '/main/resources'])
        self.proof = valid()
        self.proof['packaged_runtime'] = dict(jar_path=str(jar), jar_sha256=digest, fixture_free_jar=True,
            separate_harness_module=True, production_selectors_absent=True, harness_mod_id=HARNESS,
            harness_mod_path='/isolated/harness', mod_root_uri=root,
            class_origins=[{'class': n, 'resource_uri': root+n.replace('.', '/')+'.class',
                           'code_source_uri': root, 'sha256': hashlib.sha256(n.encode()).hexdigest()} for n in CLASSES],
            active_textures=[dict(path=n, sha256=h, pack='mod/'+MOD) for n, h in TEXTURES.items()])
        self.proof['native_openings'], self.proof['tank_captures'] = [], []
        for i, machine in enumerate(('funnel', 'accumulator')):
            self.proof['native_openings'].append(dict(machine=machine, menu_id=i+1, hit_block='0,80,0',
                method='XTEST_RMB_BLOCK_HIT', native_right_press_observed=True, native_right_release_observed=True))
            capacity = 3000 if i == 0 else 4000
            for j, kind in enumerate(('empty', 'half', 'full')):
                row = copy.deepcopy(self.proof['hovers'][6*i])
                amount = (0, capacity//2, capacity)[j]
                row.update(name=f'{machine}-tank-{kind}.png', amount=amount, capacity=capacity, setup_seed=True,
                    recipe_toast_absent=True, target_x=520, target_y=240, actual_x=520.0, actual_y=240.0,
                    raw_mouse_x=520.0, raw_mouse_y=240.0)
                state = dict(state=[0,0,1,0,0,0,amount], total=1, menu_id=i+1)
                row.update(client=state, server=copy.deepcopy(state))
                self.proof['tank_captures'].append(row)
        for row in self.proof['hovers']:
            row['recipe_toast_absent'] = True

    def check(self, proof=None, report=None):
        return validate_packaged(self.proof if proof is None else proof, self.report if report is None else report,
                                 1280, 720, 1)

    def test_complete_packaged_proof(self):
        self.check()

    def test_equivalent_java_file_uri_forms(self):
        proof = copy.deepcopy(self.proof)
        java_jar_uri = 'file:'+self.report['jar_path']
        for row in proof['packaged_runtime']['class_origins']:
            row['code_source_uri'] = java_jar_uri
        self.check(proof)
        proof['packaged_runtime']['mod_root_uri'] = 'file:/authoritative-mod-root/'
        for row in proof['packaged_runtime']['class_origins']:
            entry = row['class'].replace('.', '/')+'.class'
            row.update(code_source_uri='file:///authoritative-mod-root/',
                       resource_uri='file:///authoritative-mod-root/'+entry)
        self.check(proof)

    def test_reject_unsafe_or_different_origin_uris(self):
        for field in ('code_source_uri', 'resource_uri'):
            for alteration in ('query', 'fragment', 'different_root', 'unsupported_scheme'):
                proof = copy.deepcopy(self.proof)
                row = proof['packaged_runtime']['class_origins'][0]
                uri = row[field]
                row[field] = {'query': uri+'?source=other', 'fragment': uri+'#other',
                              'different_root': uri.replace('exact-normal-jar', 'other-normal-jar'),
                              'unsupported_scheme': uri.replace('union:', 'https:')}[alteration]
                with self.subTest(field=field, alteration=alteration), self.assertRaises(SmokeFailure):
                    self.check(proof)
        for root in ('union:/exact-normal-jar/?source=other/', 'union:/exact-normal-jar/#other/',
                     'https:/exact-normal-jar/'):
            proof = copy.deepcopy(self.proof)
            proof['packaged_runtime']['mod_root_uri'] = root
            with self.subTest(root=root), self.assertRaises(SmokeFailure):
                self.check(proof)

    def test_reject_duplicate_archive_names_with_matching_digest(self):
        name = CLASSES[0]
        with zipfile.ZipFile(self.report['jar_path'], 'a') as archive:
            with self.assertWarns(UserWarning):
                archive.writestr(name.replace('.', '/')+'.class', name.encode())
        digest = hashlib.sha256(Path(self.report['jar_path']).read_bytes()).hexdigest()
        self.report['jar_sha256'] = digest
        self.proof['packaged_runtime']['jar_sha256'] = digest
        with self.assertRaisesRegex(SmokeFailure, 'Duplicate production archive entries'):
            self.check()

    def test_reject_runtime_mutations(self):
        mutations = [lambda p: p.pop('packaged_runtime'),
            lambda p: p['packaged_runtime'].update(production_selectors_absent=False),
            lambda p: p['packaged_runtime'].update(separate_harness_module=False),
            lambda p: p['packaged_runtime'].update(jar_sha256='0'*64),
            lambda p: p['packaged_runtime'].update(harness_mod_id=MOD),
            lambda p: p['packaged_runtime'].update(harness_mod_path=self.report['jar_path']),
            lambda p: p['packaged_runtime']['class_origins'].pop(),
            lambda p: p['packaged_runtime']['class_origins'][0].update(resource_uri='file:/main/classes/foo.class'),
            lambda p: p['packaged_runtime']['class_origins'][0].update(code_source_uri='file:/source-output/'),
            lambda p: p['packaged_runtime']['class_origins'][0].update(sha256='0'*64),
            lambda p: p['packaged_runtime']['active_textures'][0].update(pack='file/override.zip'),
            lambda p: p['packaged_runtime']['active_textures'][0].update(sha256='0'*64),
            lambda p: p['packaged_runtime']['active_textures'].pop()]
        for change in mutations:
            proof = copy.deepcopy(self.proof)
            change(proof)
            with self.subTest(change=change), self.assertRaises(SmokeFailure):
                self.check(proof)

    def test_reject_registration_mutations(self):
        for key, value in [('loaded_development_mods', [MOD, HARNESS]), ('split_packages', True),
                ('fixture_free_jar', False), ('runtime_classpath', ['/main/classes', self.report['jar_path']]),
                ('runtime_classpath', ['/main/other/../classes', self.report['jar_path']]),
                ('forbidden_main_outputs', [])]:
            report = copy.deepcopy(self.report)
            report[key] = value
            with self.subTest(key=key, value=value), self.assertRaises(SmokeFailure):
                self.check(report=report)

    def test_reject_opening_and_capture_mutations(self):
        mutations = [lambda p: p['native_openings'].pop(),
            lambda p: p['native_openings'][0].update(method='openMenu'),
            lambda p: p['native_openings'][0].update(menu_id=99),
            lambda p: p['native_openings'][0].update(hit_block='1,80,0'),
            lambda p: p['native_openings'][0].update(native_right_press_observed=False),
            lambda p: p['native_openings'][0].update(native_right_release_observed=False),
            lambda p: p['tank_captures'].pop(), lambda p: p['tank_captures'][1].update(amount=1),
            lambda p: p['tank_captures'][1].update(setup_seed=False),
            lambda p: p['tank_captures'][1].update(capacity=True),
            lambda p: p['tank_captures'][0].update(capacity=1),
            lambda p: p['tank_captures'][0].update(capacity=4000),
            lambda p: p['tank_captures'][1]['server']['state'].__setitem__(6, 2),
            lambda p: p['tank_captures'][1].update(menu_id=90),
            lambda p: p['tank_captures'][0].update(width=800),
            lambda p: p['tank_captures'][0].update(gui_scale=True),
            lambda p: p['tank_captures'][0].update(screen_class='FakeScreen'),
            lambda p: p['tank_captures'][0].update(window_width=999),
            lambda p: p['tank_captures'][0].update(raw_mouse_x=0),
            lambda p: p['tank_captures'][0].update(xtest_motion_count=0),
            lambda p: p['tank_captures'][0].update(target_y=250),
            lambda p: p['hovers'][0].update(recipe_toast_absent=False),
            lambda p: p['tank_captures'][0].pop('recipe_toast_absent')]
        for change in mutations:
            proof = copy.deepcopy(self.proof)
            change(proof)
            with self.subTest(change=change), self.assertRaises(SmokeFailure):
                self.check(proof)

    def test_scaled_tank_pointer_mapping(self):
        for width, height, scale in [(800,600,2), (960,810,3), (961,811,3)]:
            proof = copy.deepcopy(self.proof)
            gw, gh = (width+scale-1)//scale, (height+scale-1)//scale
            for row in proof['tank_captures']:
                x, y = (gw-256)//2+8, (gh-256)//2+8
                row.update(width=width, height=height, gui_width=gw, gui_height=gh, gui_scale=scale,
                    window_width=width, window_height=height, target_x=x, target_y=y,
                    actual_x=float(x), actual_y=float(y), raw_mouse_x=x*width/gw, raw_mouse_y=y*height/gh)
            validate_packaged(proof, self.report, width, height, scale)
            proof['tank_captures'][0]['raw_mouse_x'] = 0
            with self.assertRaises(SmokeFailure):
                validate_packaged(proof, self.report, width, height, scale)

    def test_reject_fixture_leaked_even_with_matching_archive_hash(self):
        with zipfile.ZipFile(self.report['jar_path'], 'a') as archive:
            archive.writestr('com/oblixorprime/iedctrtest/fluid/Leak.class', b'leak')
        digest = hashlib.sha256(Path(self.report['jar_path']).read_bytes()).hexdigest()
        self.report['jar_sha256'] = digest
        self.proof['packaged_runtime']['jar_sha256'] = digest
        with self.assertRaises(SmokeFailure):
            self.check()

    def test_runner_workflow_contract(self):
        runner = (ROOT/'tools/validation/fluid_packaged_native.py').read_text()
        workflow = (ROOT/'.github/workflows/fluid-gestures-native.yml').read_text()
        for token in ('supplied_hash', 'Supplied artifact changed during preparation',
                      'Production JAR changed during execution', 'validate_gestures(', 'validate_packaged(',
                      'verifyFluidPackagedRuntime', 'runFluidPackagedDefaultClient', 'final_sha != source_sha'):
            self.assertIn(token, runner)
        self.assertNotIn("systemProperty 'iedctr.clientGameplay', 'false'", runner)
        self.assertNotIn("systemProperty 'iedctr.fluidGestureNative'", runner)
        self.assertIn('fluid_packaged_native.py', workflow)
        self.assertIn('--runtime-jar', workflow)
        self.assertIn('shutil.copyfile(jar, artifact)', workflow)
        self.assertIn("source_sha=source, sha256=digest", workflow)
        self.assertIn('language: [en_us, fr_fr]', workflow)
        self.assertIn('needs: ordinary-artifact', workflow)
        self.assertEqual(workflow.count('matrix:'), 1)
        self.assertIn('actions/download-artifact@9000827ccba6bdab643e8b6fd33ac0654aef8333', workflow)
        self.assertIn("identity['source_sha'] == source", workflow)
        self.assertIn("identity['sha256'] == digest", workflow)
        self.assertIn('test_accumulator_tooltip_wrap.py', workflow)
        for dimensions in ('width: 1280, height: 720, scale: 1', 'width: 800, height: 600, scale: 2',
                           'width: 960, height: 810, scale: 3'):
            self.assertIn(dimensions, workflow)


if __name__ == '__main__':
    unittest.main()
