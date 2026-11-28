"""Structural conservation tests; not a native or compilation claim."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]
class ConservationTests(unittest.TestCase):
    def test_both_fixtures_preserved(self):
        for name in ['NativeTfcMilkingRecoveryValidation','NativeMooshroomMilkingValidation']:
            old=(ROOT/f'src/tfcGameTest/java/com/oblixorprime/engineersdecorreforged/client/validation/{name}.java').read_text()
            new=(ROOT/f'src/tfcPackagedValidation/java/com/oblixorprime/iedctrtest/tfc/{name}.java').read_text()
            new=new.replace('package com.oblixorprime.iedctrtest.tfc;','package com.oblixorprime.engineersdecorreforged.client.validation;').replace('modid = TfcValidationMod.MOD_ID','modid = EngineersDecorReforged.MOD_ID')
            new=new.replace('iedctr.tfcPackagedRecoveryNative','iedctr.tfcMilkingRecoveryNative').replace('iedctr.tfcPackagedMooshroomNative','iedctr.mooshroomMilkingNative')
            new=new.replace('   private static JsonObject runtimeBefore;\n','').replace('      runtimeBefore=PackagedRuntimeProof.verify(mc);\n','')
            new=new.replace('         var runtimeAfter=error==null?PackagedRuntimeProof.verify(mc):null;\n','').replace('p.add("packaged_runtime",runtimeBefore);p.add("packaged_runtime_after",runtimeAfter);','')
            self.assertEqual(new,old,name)
    def test_no_compatibility_build_of_normal_artifact(self):
        g=(ROOT/'gradle/tfc-packaged-validation.gradle').read_text()
        self.assertIn("['jar','compileJava','processResources','classes']",g)
        self.assertNotIn("taskBefore(tasks.named('jar'))",g)
        self.assertNotIn("dependsOn(tasks.named('jar')",g)
        runtime=g.split('runtimeClasspath =',1)[1].split('    }',1)[0]
        self.assertIn('configurations.getByName(sourceSets.main.runtimeClasspathConfigurationName)',runtime)
        self.assertNotIn('sourceSets.main.runtimeClasspath - sourceSets.main.output',runtime)
        self.assertIn('actualDependencies != expectedDependencies',g)
        self.assertIn('task.taskDependencies.getDependencies(task).contains(blocked)',g)
        self.assertIn("'include_tfc_milking_runtime','include_tfc_crossover_runtime'",g)
        self.assertIn('harnessPackages.contains',g)
        self.assertIn('b4753e1db0d8e1a6e7eb0051b1f0951dad062f6429c10526820d55ecee335e5e',g)
    def test_dependency_edge_regressions_rejected(self):
        text=(ROOT/'gradle/tfc-packaged-validation.gradle').read_text()
        def check(g):
            runtime=g.split('runtimeClasspath =',1)[1].split('    }',1)[0]
            self.assertIn('configurations.getByName(sourceSets.main.runtimeClasspathConfigurationName)',runtime)
            self.assertNotIn('sourceSets.main.output',runtime)
            self.assertNotIn('sourceSets.main.runtimeClasspath - sourceSets.main.output',runtime)
            self.assertIn('actualDependencies != expectedDependencies',g)
            self.assertIn("['jar','compileJava','processResources','classes']",g)
        check(text)
        for bad in [text.replace('configurations.getByName(sourceSets.main.runtimeClasspathConfigurationName) + files(tfcRuntimeJar)', '(sourceSets.main.runtimeClasspath - sourceSets.main.output) + files(tfcRuntimeJar)'),
                    text.replace('runtimeClasspath = output +', 'runtimeClasspath = sourceSets.main.output + output +'),
                    text.replace('actualDependencies != expectedDependencies','false'),
                    text.replace("['jar','compileJava','processResources','classes']",'[]')]:
            with self.assertRaises(AssertionError):check(bad)
    def test_normal_build_precedes_compatibility_profile(self):
        for kind in ['recovery','mooshroom']:
            t=(ROOT/f'tools/validation/tfc_packaged_{kind}_native.py').read_text()
            self.assertLess(t.index("base+['jar']"),t.index("'-Pinclude_tfc_packaged_validation=true'"))
            self.assertIn('shutil.copyfile(normal,jar)',t)
            self.assertIn("['verifyTfcPackagedRuntime']",t)
            self.assertIn("'-Ptfc_runtime_jar='+str(jar)",t)
            self.assertNotIn('include_tfc_milking_runtime=true',t)
            self.assertNotIn("['runClient']",t)
            self.assertIn(".iedctr-",t)
            if kind=='recovery':self.assertIn("os.environ['DISPLAY']",t)
    def test_old_source_profiles_unchanged(self):
        g=(ROOT/'build.gradle').read_text()
        self.assertIn("if (tfcMilkingRuntime) sourceSets.main.java.srcDir('src/tfcGameTest/java')",g)
        self.assertNotIn("if (tfcPackagedRuntime) sourceSets.main",g)
if __name__=='__main__':unittest.main()
