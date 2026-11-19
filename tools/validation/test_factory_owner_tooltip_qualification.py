"""Owner-19 public preflight. Static qualification only, never native evidence."""
import importlib.util
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch, Mock
import factory_owner_20261119 as gate

class OwnerQualification(unittest.TestCase):
    def setUp(self):gate.ensure_baseline()
    def test_live_candidate_and_exact_baseline(self):
        data = gate.validate_candidate()
        self.assertEqual(len(data['source_attestations']), 2)
        self.assertEqual(data['contract']['hopper_input_half_open'], [-8,-16,184,166])
        self.assertEqual(data['contract']['native_carried_stack'], 'NOT_RUN')
        for path in data['inverse']:
            self.assertEqual(gate.project_source(path,(gate.ROOT/path).read_text()).encode(), (gate.BASELINE/path).read_bytes())

    def mutated_root(self, path, replacement):
        """Private filesystem copy: tests never mutate the actual worktree."""
        td=tempfile.TemporaryDirectory(); self.addCleanup(td.cleanup)
        root=Path(td.name)
        data=gate.recipe()
        for name in set(data['baseline']) | set(data['candidate']) | set(gate.ADAPTER_HASHES) | gate.NEW_GATE_PATHS:
            dest=root/name;dest.parent.mkdir(parents=True,exist_ok=True)
            dest.symlink_to(gate.ROOT/name)
        dest=root/path;dest.unlink();dest.write_bytes(replacement)
        return root

    def test_reject_old_r04_png(self):
        path='src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/factory_steel_v2/hopper.png'
        root=self.mutated_root(path,(gate.BASELINE/path).read_bytes())
        with self.assertRaisesRegex(AssertionError,'Wrong owner asset'):gate.validate_candidate(root)

    def test_reject_previous_ef40_png_as_new_candidate(self):
        gate.ensure_base_object(anchor=gate.PREVIOUS_COMMIT)
        for machine in ('hopper', 'dropper'):
            path='src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/factory_steel_v2/'+machine+'.png'
            previous=subprocess.check_output(['git','show',gate.PREVIOUS_COMMIT+':'+path],cwd=gate.ROOT)
            root=self.mutated_root(path,previous)
            with self.subTest(machine=machine), self.assertRaisesRegex(AssertionError,'Wrong owner asset'):
                gate.validate_candidate(root)

    def test_reject_historical_v3_recipe_mutation(self):
        with tempfile.TemporaryDirectory() as td:
            p=Path(td)/'v3.json';p.write_bytes(gate.PREVIOUS_RECIPE.read_bytes()+b' ')
            with patch.object(gate,'PREVIOUS_RECIPE',p), self.assertRaisesRegex(AssertionError,'Historical v3 recipe changed'):
                gate.validate_candidate()

    def test_reject_unapproved_history_anchor(self):
        with self.assertRaisesRegex(AssertionError,'Unapproved historical Git anchor'):
            gate.ensure_base_object(anchor='0'*40)

    def test_reject_v4_assets_as_metal_candidate(self):
        gate.ensure_base_object(anchor=gate.V4_COMMIT)
        for machine in ('hopper','dropper','owner_control_well'):
            path='src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/factory_steel_v2/'+machine+'.png'
            previous=subprocess.check_output(['git','show',gate.V4_COMMIT+':'+path],cwd=gate.ROOT)
            root=self.mutated_root(path,previous)
            with self.subTest(asset=machine), self.assertRaisesRegex(AssertionError,'Wrong owner asset'):
                gate.validate_candidate(root)

    def test_reject_historical_v4_recipe_mutation(self):
        with tempfile.TemporaryDirectory() as td:
            p=Path(td)/'v4.json';p.write_bytes(gate.V4_RECIPE.read_bytes()+b' ')
            with patch.object(gate,'V4_RECIPE',p), self.assertRaisesRegex(AssertionError,'Historical v4 recipe changed'):
                gate.validate_candidate()

    def test_historical_v4_view_is_not_current_assets(self):
        root=gate.historical_v4_root()
        self.assertNotEqual(root,gate.ROOT)
        path='src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/factory_steel_v2/hopper.png'
        self.assertNotEqual((root/path).read_bytes(),(gate.ROOT/path).read_bytes())
        self.assertEqual(gate.digest((root/path).read_bytes()),gate.historical_v4_recipe()['candidate'][path])

    def test_historical_suite_wrapper_is_not_flattened(self):
        suite=load_tests(unittest.defaultTestLoader,unittest.TestSuite(),None)
        self.assertTrue(any(isinstance(item,HistoricalV4Assets) for item in suite))

    def test_reject_guard_mutation(self):
        for path in ['tools/validation/test_factory_tooltip_wrap.py','tools/validation/factory_r04_inverse_projection.py']:
            root=self.mutated_root(path,(gate.ROOT/path).read_bytes()+b'\n# altered\n')
            with self.subTest(path=path),self.assertRaisesRegex(AssertionError,'guard'):gate.validate_candidate(root)

    def test_reject_third_party_java_mutation(self):
        path='src/main/java/com/oblixorprime/engineersdecorreforged/client/MachineScreen.java'
        content=(gate.ROOT/path).read_bytes().replace(b'RedesignedMachineArtwork.milking(',b'RedesignedMachineArtwork.otherMilking(',1)
        self.assertNotEqual(content,(gate.ROOT/path).read_bytes())
        root=self.mutated_root(path,content)
        with self.assertRaises(AssertionError):gate.validate_candidate(root)

    def test_reject_historical_fixture_mutation(self):
        with tempfile.TemporaryDirectory() as td:
            root=Path(td)
            for path in gate.recipe()['baseline']:
                dest=root/path;dest.parent.mkdir(parents=True,exist_ok=True);dest.symlink_to(gate.BASELINE/path)
            path='tools/validation/test_factory_r04_scoped.py'
            dest=root/path;original=dest.read_bytes();dest.unlink();dest.write_bytes(original+b'\n# changed\n')
            with patch.object(gate,'BASELINE',root),self.assertRaisesRegex(AssertionError,'Historical baseline altered'):gate.validate_candidate()

    def test_reject_added_third_party_file(self):
        path='tools/validation/test_factory_tooltip_wrap.py'
        root=self.mutated_root(path,(gate.ROOT/path).read_bytes())
        (root/'tools/validation/unapproved_guard.py').write_text('# unapproved')
        with self.assertRaisesRegex(AssertionError,'inventory changed'):gate.validate_candidate(root)

    def test_reject_java_hidden_in_cache_named_directory(self):
        path='tools/validation/test_factory_tooltip_wrap.py'
        root=self.mutated_root(path,(gate.ROOT/path).read_bytes())
        extra=root/'src/main/java/com/oblixorprime/engineersdecorreforged/client/__pycache__/Unexpected.java'
        extra.parent.mkdir();extra.write_text('class Unexpected {}')
        with self.assertRaisesRegex(AssertionError,'inventory changed'):gate.validate_candidate(root)

    def test_reject_optimized_python(self):
        result=subprocess.run([sys.executable,'-O','-c','import factory_owner_20261119 as g; g.validate_candidate()'],cwd=gate.ROOT/'tools/validation',capture_output=True,text=True)
        self.assertNotEqual(result.returncode,0)
        self.assertIn('optimized mode refused',result.stderr)

    def test_reject_recipe_change(self):
        with tempfile.TemporaryDirectory() as td:
            p=Path(td)/'recipe.json';p.write_bytes(gate.RECIPE.read_bytes()+b' ')
            with patch.object(gate,'RECIPE',p),self.assertRaisesRegex(AssertionError,'recipe changed'):gate.validate_candidate()

    def test_reject_private_sources_added_to_public_tree(self):
        path='tools/validation/test_factory_tooltip_wrap.py'
        root=self.mutated_root(path,(gate.ROOT/path).read_bytes())
        private=root/'tools/approved_gui_20261119/sources';private.mkdir()
        (private/'private.png').write_bytes(b'private')
        with self.assertRaisesRegex(AssertionError,'Private or duplicated'):gate.validate_candidate(root)

    def test_ci_fetch_is_only_the_exact_anchored_commit(self):
        # Simulated subprocess protocol only; no network request in this test.
        with patch.dict('os.environ',{'GITHUB_ACTIONS':'true','CI':'true'}), \
             patch.object(gate.subprocess,'run',side_effect=[Mock(returncode=1),Mock(returncode=0)]) as run, \
             patch.object(gate.subprocess,'check_output',return_value=gate.BASE+'\n'):
            gate.ensure_base_object()
            self.assertEqual(run.call_args_list[1].args[0],['git','fetch','--no-tags','--depth=1','origin',gate.BASE])
        with patch.object(gate.subprocess,'run',return_value=Mock(returncode=0)), \
             patch.object(gate.subprocess,'check_output',return_value='0'*40+'\n'), \
             self.assertRaisesRegex(AssertionError,'Historical Git commit changed'):
            gate.ensure_base_object()

    def test_missing_git_object_fails_without_network_locally(self):
        with tempfile.TemporaryDirectory() as td:
            subprocess.run(['git','init','-q',td],check=True)
            with patch.dict('os.environ',{'GITHUB_ACTIONS':'false','CI':'false'}):
                with self.assertRaisesRegex(AssertionError,'Missing exact historical Git object'):gate.ensure_base_object(Path(td))



class HistoricalV4Assets(unittest.TestSuite):
    """Unmodified v4 art tests run on exact v4 Git bytes, explicitly historical."""
    def __init__(self, tests, module):
        super().__init__(tests);self.module=module
    def run(self,result,debug=False):
        root=gate.historical_v4_root()
        data=gate.historical_v4_recipe()
        b=self.module.b
        out=root/self.module.b.OUT.relative_to(gate.ROOT)
        with patch.object(b,'ROOT',root), patch.object(b,'OUT',out), patch.object(gate,'recipe',return_value=data):
            return super().run(result,debug)


def load_tests(loader, tests, pattern):
    # Native workflow preflight always runs the NEW metal gate and tests.
    # Old v4 art assertions stay byte-identical and run separately on v4 Git.
    pkg=gate.PACKAGE
    sys.path.insert(0,str(pkg))
    try:
        for name in ['test_assets','test_click_bounds','test_metal_assets']:
            spec=importlib.util.spec_from_file_location('owner_'+name,pkg/(name+'.py'))
            module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
            suite=loader.loadTestsFromModule(module)
            if name=='test_assets':tests.addTest(HistoricalV4Assets(suite,module))
            else:tests.addTests(suite)
    finally:sys.path.remove(str(pkg))
    return tests
