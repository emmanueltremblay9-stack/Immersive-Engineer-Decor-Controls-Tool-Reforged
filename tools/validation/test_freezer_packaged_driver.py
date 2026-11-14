"""Source contracts only; these tests do not execute Minecraft."""
from pathlib import Path
import unittest
import tomllib
from client_mixin_smoke import SmokeFailure
from freezer_packaged_native import require_client_success
ROOT = Path(__file__).resolve().parents[2]

class DriverTests(unittest.TestCase):
    def test_harness_metadata_is_required_and_matches_registration(self):
        path=ROOT/'src/freezerPackagedValidation/resources/META-INF/neoforge.mods.toml'
        self.assertTrue(path.is_file(), 'Separate harness must have NeoForge metadata')
        data=tomllib.loads(path.read_text())
        self.assertEqual(data['modLoader'],'javafml')
        self.assertEqual([m['modId'] for m in data['mods']],['iedctr_freezer_validation'])
        dependency=data['dependencies']['iedctr_freezer_validation'][0]
        self.assertEqual(dependency['modId'],'immersive_engineer_decor_controls_tool_reforged')
        self.assertEqual((dependency['type'],dependency['ordering'],dependency['side']),('required','AFTER','CLIENT'))
        self.assertFalse((ROOT/'src/main/resources/META-INF/neoforge.mods.toml').exists() and 'iedctr_freezer_validation' in (ROOT/'src/main/resources/META-INF/neoforge.mods.toml').read_text())

    def test_client_failure_precedes_evidence_reads(self):
        require_client_success(0,False,Path('/missing/log'))
        for code,expired in [(1,False),(0,True),(None,True),(False,False)]:
            with self.subTest(code=code,expired=expired),self.assertRaisesRegex(SmokeFailure,'Freezer packaged client failed'):
                require_client_success(code,expired,Path('/missing/log'))
        source=(ROOT/'tools/validation/freezer_packaged_native.py').read_text()
        self.assertLess(source.index('        require_client_success(code, expired, log)'),source.index('        proof = json.loads'))
        self.assertLess(source.index('        if FATAL_PATTERN.search(logs)'),source.index('        proof = json.loads'))

    def test_isolation_and_optin(self):
        gradle=(ROOT/'gradle/freezer-packaged-validation.gradle').read_text()
        for needle in ['sourceSets.main.runtimeClasspath - sourceSets.main.output', 'sha256(jarFile) != sha256(normalJar)', 'classpath.any { mainOutputs.contains(it) }', 'harnessPackages.contains(', 'fixture_free_jar: false', 'separate_provenance_harness: true']:
            self.assertIn(needle,gradle)
        observer=(ROOT/'src/freezerPackagedValidation/java/com/oblixorprime/iedctrtest/freezer/FreezerProvenanceObserver.java').read_text()
        for needle in ['EventPriority.HIGHEST','EventPriority.LOWEST','PackagedRuntimeProof.verify(mc)','client-freezer-reference-result.json']:
            self.assertIn(needle,observer)
        self.assertNotIn('setBlock',observer)

    def test_runner_retains_native_gate_and_identity(self):
        runner=(ROOT/'tools/validation/freezer_packaged_native.py').read_text()
        for needle in ['prepareFreezerPackagedOptInClientRun','verifyFreezerPackagedRuntime','runFreezerPackagedOptInClient',"for stage in ('before', 'after')",'validate_runtime(origin, report)','validate_proof(proof, main_proof', 'final_sha != source_sha', 'Production JAR changed during execution',"evidence/'tested-production.jar'"]:
            self.assertIn(needle,runner)
        workflow=(ROOT/'.github/workflows/freezer-packaged-native.yml').read_text()
        for needle in ['ordinary-artifact','--runtime-jar','language: en_us, height: 720','language: fr_fr, height: 260','github.event.pull_request.head.sha || github.sha']:
            self.assertIn(needle,workflow)
        self.assertNotIn('matrix.fluid',workflow)

    def test_normal_selection_without_development_properties(self):
        runner=(ROOT/'tools/validation/freezer_packaged_native.py').read_text()
        self.assertNotIn("systemProperty 'iedctr.dev.",runner)
        proof=(ROOT/'src/freezerPackagedValidation/java/com/oblixorprime/iedctrtest/freezer/PackagedRuntimeProof.java').read_text()
        self.assertIn('key.startsWith("iedctr.dev.")',proof)
        allowed=proof.split('java.util.Set<String> allowed = ')[1].split(';')[0]
        self.assertNotIn('iedctr.dev.',allowed)
        menu=(ROOT/'src/main/java/com/oblixorprime/engineersdecorreforged/menu/MachineMenu.java').read_text()
        self.assertIn('kind == MachineKind.SMALL_FREEZER,',menu)
        self.assertIn('this(kind, id, inventory, container, data, false, false, false, false);',menu)

    def test_no_stale_static_chamber_scope(self):
        source=(ROOT/'src/main/java/com/oblixorprime/engineersdecorreforged/client/validation/NativeClientMiningValidation.java').read_text()
        self.assertIn('chamber frost from server phases; static decorative rim',source)
        self.assertNotIn('inventory/energy smoke; static frost;',source)

if __name__=='__main__': unittest.main()
