"""Source contracts only; these tests do not execute Minecraft."""
from pathlib import Path
import unittest
ROOT = Path(__file__).resolve().parents[2]

class DriverTests(unittest.TestCase):
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

    def test_no_stale_static_chamber_scope(self):
        source=(ROOT/'src/main/java/com/oblixorprime/engineersdecorreforged/client/validation/NativeClientMiningValidation.java').read_text()
        self.assertIn('chamber frost from server phases; static decorative rim',source)
        self.assertNotIn('inventory/energy smoke; static frost;',source)

if __name__=='__main__': unittest.main()
