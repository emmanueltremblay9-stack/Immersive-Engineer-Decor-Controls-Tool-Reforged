"""Synthetic rejection tests; no Minecraft execution is implied."""
import copy
import hashlib
from pathlib import Path
import tempfile
import unittest
import warnings
import zipfile
from client_mixin_smoke import SmokeFailure
from freezer_packaged_proof import CLASSES, HARNESS, MOD, TEXTURES, SELECTORS, validate_runtime

ROOT = Path(__file__).resolve().parents[2]


class RuntimeTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        jar = Path(self.temp.name)/'normal.jar'
        with zipfile.ZipFile(jar, 'w') as archive:
            for name in CLASSES:
                archive.writestr(name.replace('.', '/')+'.class', name.encode())
            for name in TEXTURES:
                archive.write(ROOT/'src/main/resources/assets'/MOD/name, 'assets/'+MOD+'/'+name)
        digest = hashlib.sha256(jar.read_bytes()).hexdigest()
        root = 'union:/exact-normal-jar/'
        self.report = dict(jar_path=str(jar), jar_sha256=digest, fixture_free_jar=False, separate_provenance_harness=True,
            split_packages=False, loaded_development_mods=[HARNESS],
            runtime_classpath=[str(jar), '/isolated/harness'], forbidden_main_outputs=['/main/classes', '/main/resources'])
        self.proof = dict(packaged_runtime=dict(jar_path=str(jar), jar_sha256=digest,
            fixture_free_jar=False, separate_provenance_harness=True, separate_harness_module=True, production_selectors_absent=False, production_selectors=SELECTORS, opt_in_selector="iedctr.dev.freezerV5LiveEnergy",
            harness_mod_id=HARNESS, harness_mod_path='/isolated/harness', mod_root_uri=root,
            class_origins=[{'class': n, 'resource_uri': root+n.replace('.', '/')+'.class',
                'code_source_uri': root, 'sha256': hashlib.sha256(n.encode()).hexdigest()} for n in CLASSES],
            active_textures=[dict(path=n, sha256=h, pack='mod/'+MOD) for n, h in TEXTURES.items()]))

    def check(self, proof=None, report=None):
        return validate_runtime(self.proof if proof is None else proof, self.report if report is None else report)

    def test_valid_runtime(self):
        self.check()


    def test_explicit_optin_is_mandatory(self):
        for field,value in [('production_selectors',None),('production_selectors',['iedctr.dev.freezerV2Reference=true']),('opt_in_selector',None),('opt_in_selector','iedctr.dev.freezerV2Reference=false'),('production_selectors_absent',True)]:
            p=copy.deepcopy(self.proof);p['packaged_runtime'][field]=value
            with self.subTest(field=field,value=value),self.assertRaises(SmokeFailure):self.check(p)

    def test_java_equivalent_uri(self):
        for row in self.proof['packaged_runtime']['class_origins']:
            row['code_source_uri'] = 'file:'+self.report['jar_path']
        self.check()

    def test_reject_runtime_corruption(self):
        mutations = [lambda p: p.pop('packaged_runtime'),
            lambda p: p['packaged_runtime'].update(production_selectors_absent=True),
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
            with self.subTest(change=change), self.assertRaises(SmokeFailure): self.check(proof)

    def test_reject_unsafe_uris(self):
        for field in ('code_source_uri', 'resource_uri'):
            for suffix in ('?other=true', '#other'):
                proof = copy.deepcopy(self.proof)
                proof['packaged_runtime']['class_origins'][0][field] += suffix
                with self.subTest(field=field, suffix=suffix), self.assertRaises(SmokeFailure): self.check(proof)

    def test_reject_main_output_and_wrong_registration(self):
        for field, value in [('loaded_development_mods', [MOD, HARNESS]), ('split_packages', True),
            ('fixture_free_jar', True), ('runtime_classpath', [self.report['jar_path'], '/main/else/../classes']),
            ('runtime_classpath', ['/other.jar']), ('forbidden_main_outputs', [])]:
            report = copy.deepcopy(self.report)
            report[field] = value
            with self.subTest(field=field), self.assertRaises(SmokeFailure): self.check(report=report)

    def test_reject_leaked_harness_and_duplicate_entries(self):
        for name in ('com/oblixorprime/iedctrtest/freezer/Leak.class', CLASSES[0].replace('.', '/')+'.class'):
            with zipfile.ZipFile(self.report['jar_path'], 'a') as archive:
                with warnings.catch_warnings():
                    warnings.simplefilter('ignore', UserWarning)
                    archive.writestr(name, b'bad')
            digest = hashlib.sha256(Path(self.report['jar_path']).read_bytes()).hexdigest()
            self.report['jar_sha256'] = self.proof['packaged_runtime']['jar_sha256'] = digest
            with self.subTest(name=name), self.assertRaises(SmokeFailure): self.check()


    def test_strict_boolean_flags(self):
        for target, fields in [('report', ('fixture_free_jar', 'split_packages')),
                ('runtime', ('fixture_free_jar', 'separate_harness_module', 'production_selectors_absent'))]:
            for field in fields:
                for value in (0, 1, 'true', 'false', None):
                    proof, report = copy.deepcopy(self.proof), copy.deepcopy(self.report)
                    (report if target == 'report' else proof['packaged_runtime'])[field] = value
                    with self.subTest(target=target, field=field, value=value), self.assertRaises(SmokeFailure):
                        self.check(proof, report)

    def test_jar_bytes_not_just_reported_digest(self):
        with open(self.report['jar_path'], 'ab') as stream:
            stream.write(b'changed archive bytes')
        with self.assertRaises(SmokeFailure): self.check()

    def test_substring_module_and_resource_origins_rejected(self):
        for field in ('code_source_uri', 'resource_uri'):
            for prefix, suffix in (('union:/unrelated/', ''), ('', '-other'), ('', '/../other/')):
                proof = copy.deepcopy(self.proof)
                row = proof['packaged_runtime']['class_origins'][0]
                row[field] = prefix+row[field]+suffix
                with self.subTest(field=field, prefix=prefix, suffix=suffix), self.assertRaises(SmokeFailure):
                    self.check(proof)

    def test_root_query_fragment_and_missing_slash_rejected(self):
        for root in ('union:/exact-normal-jar', 'union:/exact-normal-jar/?x=1/',
                     'union:/exact-normal-jar/#other/', None):
            proof = copy.deepcopy(self.proof)
            proof['packaged_runtime']['mod_root_uri'] = root
            with self.subTest(root=root), self.assertRaises(SmokeFailure): self.check(proof)

    def test_malformed_paths_rejected(self):
        for target, field in (('report', 'jar_path'), ('runtime', 'jar_path'), ('runtime', 'harness_mod_path')):
            for value in (None, False, 1, [], ''):
                proof, report = copy.deepcopy(self.proof), copy.deepcopy(self.report)
                (report if target == 'report' else proof['packaged_runtime'])[field] = value
                with self.subTest(target=target, field=field, value=value), self.assertRaises(SmokeFailure):
                    self.check(proof, report)

    def test_provenance_driver_in_production_rejected(self):
        name = 'com/oblixorprime/engineersdecorreforged/client/validation/PackagedRuntimeProof.class'
        with zipfile.ZipFile(self.report['jar_path'], 'a') as archive:
            archive.writestr(name, b'legacy test driver')
        digest = hashlib.sha256(Path(self.report['jar_path']).read_bytes()).hexdigest()
        self.report['jar_sha256'] = self.proof['packaged_runtime']['jar_sha256'] = digest
        with self.assertRaisesRegex(SmokeFailure, 'Driver leaked'): self.check()

    def test_classpath_symlink_cannot_hide_main_outputs(self):
        main = Path(self.temp.name)/'main'
        main.mkdir()
        alias = Path(self.temp.name)/'alias'
        alias.symlink_to(main, target_is_directory=True)
        self.report['forbidden_main_outputs'] = [str(main)]
        self.report['runtime_classpath'].append(str(alias))
        with self.assertRaises(SmokeFailure): self.check()

    def test_reordered_or_duplicate_classes_rejected(self):
        for reverse in (False, True):
            proof = copy.deepcopy(self.proof)
            rows = proof['packaged_runtime']['class_origins']
            if reverse: rows.reverse()
            else: rows[-1] = copy.deepcopy(rows[0])
            with self.subTest(reverse=reverse), self.assertRaises(SmokeFailure): self.check(proof)


    def test_rehashed_class_and_artwork_bytes_rejected(self):
        jar = Path(self.report['jar_path'])
        original = jar.read_bytes()
        for entry in (CLASSES[0].replace('.', '/')+'.class', 'assets/'+MOD+'/'+next(iter(TEXTURES))):
            with zipfile.ZipFile(jar) as archive:
                entries = {name: archive.read(name) for name in archive.namelist()}
            entries[entry] = b'changed bytes'
            with zipfile.ZipFile(jar, 'w') as archive:
                for name, data in entries.items(): archive.writestr(name, data)
            digest = hashlib.sha256(jar.read_bytes()).hexdigest()
            self.report['jar_sha256'] = self.proof['packaged_runtime']['jar_sha256'] = digest
            with self.subTest(entry=entry), self.assertRaises(SmokeFailure): self.check()
            jar.write_bytes(original)

    def test_missing_or_unreadable_archive_rejected(self):
        jar = Path(self.report['jar_path'])
        jar.write_bytes(b'not a ZIP archive')
        digest = hashlib.sha256(jar.read_bytes()).hexdigest()
        self.report['jar_sha256'] = self.proof['packaged_runtime']['jar_sha256'] = digest
        with self.assertRaises(SmokeFailure): self.check()
        jar.unlink()
        with self.assertRaises(SmokeFailure): self.check()

    def test_other_test_namespace_leak_rejected(self):
        with zipfile.ZipFile(self.report['jar_path'], 'a') as archive:
            archive.writestr('com/oblixorprime/iedctrtest/unrelated/Leak.class', b'test driver')
        digest = hashlib.sha256(Path(self.report['jar_path']).read_bytes()).hexdigest()
        self.report['jar_sha256'] = self.proof['packaged_runtime']['jar_sha256'] = digest
        with self.assertRaisesRegex(SmokeFailure, 'Driver leaked'): self.check()


    def relocated_evidence(self):
        archive = Path(self.temp.name)/'tested-production.jar'
        Path(self.report['jar_path']).rename(archive)
        original_path = '/unavailable-ci-run/worker/build/libs/normal.jar'
        self.report['jar_path'] = original_path
        self.report['runtime_classpath'][0] = original_path
        self.proof['packaged_runtime']['jar_path'] = original_path
        for row in self.proof['packaged_runtime']['class_origins']:
            row['code_source_uri'] = 'file:'+original_path
        return archive

    def test_relocated_archive_preserves_evidence_and_original_uri(self):
        archive = self.relocated_evidence()
        before = copy.deepcopy((self.proof, self.report))
        self.assertIs(validate_runtime(self.proof, self.report, archive_path=archive), self.proof)
        self.assertEqual(before, (self.proof, self.report))
        with self.assertRaises(SmokeFailure): self.check()

    def test_relocated_changed_archive_rejected(self):
        archive = self.relocated_evidence()
        archive.write_bytes(archive.read_bytes()+b'changed')
        with self.assertRaises(SmokeFailure):
            validate_runtime(self.proof, self.report, archive_path=archive)

    def test_relocated_origin_must_not_be_substituted(self):
        archive = self.relocated_evidence()
        for field, value in [('code_source_uri', archive.as_uri()),
                             ('resource_uri', archive.as_uri()+'/Other.class'),
                             ('code_source_uri', 'file:/other-ci-run/normal.jar')]:
            proof = copy.deepcopy(self.proof)
            proof['packaged_runtime']['class_origins'][0][field] = value
            with self.subTest(field=field, value=value), self.assertRaises(SmokeFailure):
                validate_runtime(proof, self.report, archive_path=archive)

    def test_relocated_paths_must_be_absolute_and_originals_must_match(self):
        archive = self.relocated_evidence()
        for target, field, value in [('report', 'jar_path', 'normal.jar'),
                ('runtime', 'jar_path', 'normal.jar'), ('runtime', 'jar_path', '/other/normal.jar'),
                ('runtime', 'harness_mod_path', 'harness'),
                ('report', 'runtime_classpath', ['normal.jar']),
                ('report', 'runtime_classpath', [str(archive)]),
                ('report', 'forbidden_main_outputs', ['main/classes'])]:
            proof, report = copy.deepcopy(self.proof), copy.deepcopy(self.report)
            (report if target == 'report' else proof['packaged_runtime'])[field] = value
            with self.subTest(target=target, field=field, value=value), self.assertRaises(SmokeFailure):
                validate_runtime(proof, report, archive_path=archive)



if __name__ == "__main__": unittest.main()
