"""Structural safeguards for ordinary-JAR default Solar; no Minecraft runtime claim."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]

class SolarPackagedDriverTest(unittest.TestCase):
    def test_fixture_is_separate(self):
        self.assertFalse((ROOT/'src/main/java/com/oblixorprime/engineersdecorreforged/client/validation/NativeSolarV2ReferenceValidation.java').exists())
        source=(ROOT/'src/solarPackagedValidation/java/com/oblixorprime/iedctrtest/solar/NativeSolarV2ReferenceValidation.java').read_text()
        self.assertIn('modid = SolarValidationMod.MOD_ID',source)
        self.assertIn('System.getProperty(MachineMenu.DEV_SOLAR_V2_REFERENCE_PROPERTY)==null',source)
        self.assertEqual(source.count('PackagedRuntimeProof.verify(mc)'),2)
        self.assertIn('p.add("packaged_runtime",packagedRuntime)',source)
    def test_classpath_and_archive_guards(self):
        source=(ROOT/'gradle/solar-packaged-validation.gradle').read_text()
        for text in ['sourceSets.main.runtimeClasspath - sourceSets.main.output',
                     "loadedMods = [neoForge.mods.getByName('iedctr_solar_validation')]",
                     'sha256(jarFile) != sha256(normalJar)', 'classpath.any { mainOutputs.contains(it) }',
                     "entry.name.startsWith('com/oblixorprime/iedctrtest/')",
                     "entry.name.contains('client/validation/NativeSolarV2ReferenceValidation')",
                     'harnessPackages.contains(', "!entryNames.add(entry.name)",
                     'run.sourceSet.get() != harness', 'taskBefore(verifyPackagedRuntime)']:
            self.assertIn(text,source)
        self.assertNotIn('systemProperty \'iedctr.dev.',source)
    def test_default_driver_requires_artifact_proof(self):
        source=(ROOT/'tools/validation/solar_v2_reference_native.py').read_text()
        self.assertNotIn("systemProperty 'iedctr.dev.",source)
        for text in ['prepareSolarPackagedDefaultClientRun','verifySolarPackagedRuntime',
                     'runSolarPackagedDefaultClient', 'validate_runtime(proof, report)',
                     "shutil.copyfile(jar, evidence/'tested-production.jar')",
                     'Production JAR changed during execution']:
            self.assertIn(text,source)
        workflow=(ROOT/'.github/workflows/solar-v2-reference-validation.yml').read_text()
        for text in ['en-1280x720-scale1','fr-1280x260-scale1','en-1920x1080-scale1',
                     'en-1920x1080-scale2','en-1920x1080-scale3','packaged-runtime.json',
                     'artifact-identity.json','tested-production.jar']:
            self.assertIn(text,workflow)

if __name__=='__main__':unittest.main()
