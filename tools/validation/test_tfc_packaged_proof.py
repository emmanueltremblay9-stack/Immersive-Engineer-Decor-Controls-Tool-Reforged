"""Synthetic rejection tests; no Minecraft execution is implied."""
import copy
import hashlib
from pathlib import Path
import tempfile
import unittest
import warnings
import zipfile
from client_mixin_smoke import SmokeFailure
from tfc_packaged_proof import CLASSES, HARNESS, MOD, TEXTURES, validate_runtime

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
        self.report = dict(jar_path=str(jar), jar_sha256=digest, fixture_free_jar=True,
            split_packages=False, loaded_development_mods=[HARNESS],
            runtime_classpath=[str(jar), '/isolated/harness'], forbidden_main_outputs=['/main/classes', '/main/resources'])
        self.proof = dict(packaged_runtime=dict(jar_path=str(jar), jar_sha256=digest,
            fixture_free_jar=True, separate_harness_module=True, production_selectors_absent=True, production_selectors=[],
            harness_mod_id=HARNESS, harness_mod_path='/isolated/harness', mod_root_uri=root,
            class_origins=[{'class': n, 'resource_uri': root+n.replace('.', '/')+'.class',
                'code_source_uri': root, 'sha256': hashlib.sha256(n.encode()).hexdigest()} for n in CLASSES],
            active_textures=[dict(path=n, sha256=h, pack='mod/'+MOD) for n, h in TEXTURES.items()]))

    def check(self, proof=None, report=None):
        return validate_runtime(self.proof if proof is None else proof, self.report if report is None else report)

    def test_valid_runtime(self):
        self.check()


    def test_selector_absence_is_mandatory(self):
        for field,value in [('production_selectors',None),('production_selectors',['iedctr.dev.milkingV2Reference=true']),('opt_in_selector',None),('opt_in_selector','iedctr.dev.milkingV2Reference=false'),('production_selectors_absent',False)]:
            p=copy.deepcopy(self.proof);p['packaged_runtime'][field]=value
            with self.subTest(field=field,value=value),self.assertRaises(SmokeFailure):self.check(p)

    def test_java_equivalent_uri(self):
        for row in self.proof['packaged_runtime']['class_origins']:
            row['code_source_uri'] = 'file:'+self.report['jar_path']
        self.check()

    def test_reject_runtime_corruption(self):
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
            with self.subTest(change=change), self.assertRaises(SmokeFailure): self.check(proof)

    def test_reject_unsafe_uris(self):
        for field in ('code_source_uri', 'resource_uri'):
            for suffix in ('?other=true', '#other'):
                proof = copy.deepcopy(self.proof)
                proof['packaged_runtime']['class_origins'][0][field] += suffix
                with self.subTest(field=field, suffix=suffix), self.assertRaises(SmokeFailure): self.check(proof)

    def test_reject_main_output_and_wrong_registration(self):
        for field, value in [('loaded_development_mods', [MOD, HARNESS]), ('split_packages', True),
            ('fixture_free_jar', False), ('runtime_classpath', [self.report['jar_path'], '/main/else/../classes']),
            ('runtime_classpath', ['/other.jar']), ('forbidden_main_outputs', [])]:
            report = copy.deepcopy(self.report)
            report[field] = value
            with self.subTest(field=field), self.assertRaises(SmokeFailure): self.check(report=report)

    def test_reject_leaked_harness_and_duplicate_entries(self):
        for name in ('com/oblixorprime/iedctrtest/milking/Leak.class', CLASSES[0].replace('.', '/')+'.class'):
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

    def test_old_in_production_driver_rejected(self):
        name = 'com/oblixorprime/engineersdecorreforged/client/validation/NativeMilkingPackagedValidation.class'
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


class CompatibilityTests(unittest.TestCase):
    def setUp(self):
        from unittest.mock import patch
        from tfc_packaged_proof import DEPENDENCIES
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup)
        self.rows=[];pins={}
        for name,(version,_) in DEPENDENCIES.items():
            jar=Path(self.temp.name)/(name+'.jar')
            entry='net/dries007/tfc/common/entities/livestock/DairyAnimal.class'
            with zipfile.ZipFile(jar,'w') as z:z.writestr(entry,b'synthetic-not-a-real-runtime')
            digest=hashlib.sha256(jar.read_bytes()).hexdigest();pins[name]=(version,digest)
            root='union:/'+name+'/'
            row=dict(mod_id=name,version=version,jar_path=str(jar),sha256=digest,mod_root_uri=root)
            if name=='tfc':row.update({'class':'net.dries007.tfc.common.entities.livestock.DairyAnimal',
                'resource_uri':root+entry,'code_source_uri':root,'class_sha256':hashlib.sha256(b'synthetic-not-a-real-runtime').hexdigest()})
            self.rows.append(row)
        self.runtime=dict(neoforge_version='21.1.234',compatibility_dependencies=self.rows)
        self.patcher=patch('tfc_packaged_proof.DEPENDENCIES',pins);self.patcher.start();self.addCleanup(self.patcher.stop)
    def check(self,p=None):
        from tfc_packaged_proof import validate_dependencies
        return validate_dependencies(self.runtime if p is None else p)
    def relocated(self):
        archives={r['mod_id']:Path(r['jar_path']) for r in self.rows}
        p=copy.deepcopy(self.runtime)
        for row in p['compatibility_dependencies']:
            row['jar_path']='/ci/absent/dependencies/'+row['mod_id']+'.jar'
            if row['mod_id']=='tfc':row['code_source_uri']='file:'+row['jar_path']
        return p,archives
    def test_offline_keeps_original_paths_and_origins(self):
        from tfc_packaged_proof import validate_dependencies
        p,archives=self.relocated();before=copy.deepcopy(p)
        validate_dependencies(p,dependency_archives=archives)
        self.assertEqual(p,before)
        with self.assertRaises(SmokeFailure):validate_dependencies(p)
    def test_offline_mapping_and_bytes_rejected(self):
        from tfc_packaged_proof import validate_dependencies
        p,archives=self.relocated()
        bad=Path(self.temp.name)/'bad.jar';bad.write_bytes(b'bad')
        for mapping in [{}, {'tfc':archives['tfc']}, {**archives,'extra':bad}, {**archives,'tfc':bad},
                        {**archives,'patchouli':'/missing.jar'}, {**archives,'tfc':None}]:
            with self.subTest(mapping=mapping),self.assertRaises(SmokeFailure):validate_dependencies(p,dependency_archives=mapping)
    def test_offline_does_not_waive_original_identity(self):
        from tfc_packaged_proof import validate_dependencies
        p,archives=self.relocated()
        for field,value in [('jar_path','relative.jar'),('code_source_uri','file:/wrong.jar'),('resource_uri','file:/wrong.class')]:
            changed=copy.deepcopy(p);changed['compatibility_dependencies'][0][field]=value
            with self.subTest(field=field),self.assertRaises(SmokeFailure):validate_dependencies(changed,dependency_archives=archives)
    def test_synthetic_dependencies(self):self.check()
    def test_mutations(self):
        changes=[lambda p:p.update(neoforge_version='21.1.230'),lambda p:p['compatibility_dependencies'].pop(),
            lambda p:p['compatibility_dependencies'][0].update(sha256='0'*64),
            lambda p:p['compatibility_dependencies'][0].update(version='4.2.13'),
            lambda p:p['compatibility_dependencies'][0].update(resource_uri='file:/source/DairyAnimal.class'),
            lambda p:p['compatibility_dependencies'][0].update(class_sha256='0'*64),
            lambda p:p['compatibility_dependencies'][1].update(jar_path='/missing.jar')]
        for mutate in changes:
            p=copy.deepcopy(self.runtime);mutate(p)
            with self.subTest(mutate=mutate),self.assertRaises(SmokeFailure):self.check(p)

class ComposedTests(unittest.TestCase):
    def test_both_wrappers_offline_without_rewriting_evidence(self):
        from unittest.mock import patch
        from tfc_packaged_proof import validate_recovery,validate_mooshroom
        runtime=RuntimeTests();runtime.setUp();self.addCleanup(runtime.doCleanups)
        jar=runtime.relocated_evidence()
        compatibility=CompatibilityTests();compatibility.setUp();self.addCleanup(compatibility.doCleanups)
        dependencies,archives=compatibility.relocated()
        runtime.proof['packaged_runtime'].update(dependencies)
        runtime.proof['packaged_runtime_after']=copy.deepcopy(runtime.proof['packaged_runtime'])
        before=copy.deepcopy(runtime.proof)
        for kind,validator in [('tfc_milking_recovery_native',validate_recovery),('mooshroom_milking_native',validate_mooshroom)]:
            with patch(kind+'.validate_proof',return_value='called') as original:
                args=(runtime.proof,'nonce','sha',Path('/unused'),'logs',0,False,1280,720,'fr_fr',2)
                self.assertEqual(validator(*args,report=runtime.report,archive_path=jar,dependency_archives=archives),'called')
                original.assert_called_once_with(*args)
                bad=Path(runtime.temp.name)/'bad.jar';bad.write_bytes(b'bad')
                with self.assertRaises(SmokeFailure):validator(*args,report=runtime.report,archive_path=bad,dependency_archives=archives)
        self.assertEqual(runtime.proof,before)

    def test_each_original_gate_called(self):
        from unittest.mock import patch
        from tfc_packaged_proof import validate_recovery, validate_mooshroom
        for kind,validator in [('tfc_milking_recovery_native',validate_recovery),('mooshroom_milking_native',validate_mooshroom)]:
            with patch('tfc_packaged_proof.validate_runtime'),patch('tfc_packaged_proof.validate_dependencies'),patch(kind+'.validate_proof',return_value='called') as original:
                p=dict(packaged_runtime={},packaged_runtime_after={})
                args=(p,'nonce','sha',Path('/unused'),'logs',0,False,1280,720,'fr_fr',2)
                self.assertEqual(validator(*args,report={}), 'called');original.assert_called_once_with(*args)
    def test_before_after_mismatch_rejected(self):
        from unittest.mock import patch
        from tfc_packaged_proof import validate_recovery
        with patch('tfc_packaged_proof.validate_runtime'):
            with self.assertRaises(SmokeFailure):validate_recovery(dict(packaged_runtime={},packaged_runtime_after={'changed':1}), 'n','s',Path('/unused'),'l',0,False,1280,720,'fr_fr',2,report={})
if __name__=='__main__':unittest.main()
