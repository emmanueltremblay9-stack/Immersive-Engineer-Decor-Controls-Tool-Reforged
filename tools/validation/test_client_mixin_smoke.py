import subprocess
from pathlib import Path
import tempfile
import unittest
import zipfile

import client_mixin_smoke as smoke


HANDLER = "handler$abc000$engineersdecorreforged$pickUpperStairRail"
CLASS_LOG = "[3.410s][info][class,load] net.minecraft.client.renderer.GameRenderer source: __JVM_DefineClass__\n"
BASELINE = f"""public class net.minecraft.client.renderer.GameRenderer {{
  private net.minecraft.world.phys.HitResult pick(net.minecraft.world.entity.Entity, double, double, float);
    descriptor: {smoke.PICK_DESCRIPTOR}
    Code:
       0: areturn
       1: areturn
}}
"""
DISASSEMBLY = f"""public class net.minecraft.client.renderer.GameRenderer {{
  private net.minecraft.world.phys.HitResult pick(net.minecraft.world.entity.Entity, double, double, float);
    descriptor: {smoke.PICK_DESCRIPTOR}
    Code:
       0: invokespecial #900 // Method {HANDLER}:{smoke.HANDLER_DESCRIPTOR}
       3: areturn
       4: invokespecial #900 // Method {HANDLER}:{smoke.HANDLER_DESCRIPTOR}
       7: areturn
  private void {HANDLER}(net.minecraft.world.entity.Entity, double, double, float, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable);
    descriptor: {smoke.HANDLER_DESCRIPTOR}
    Code:
       0: invokestatic #901 // Method {smoke.HELPER}:{smoke.HELPER_DESCRIPTOR}
       3: return
}}
"""


class EvidenceTests(unittest.TestCase):
    def validate(self, **changes):
        args = dict(disassembly=DISASSEMBLY, class_log=CLASS_LOG, client_log="",
                    return_code=-15, timed_out=True, crash_files=[], baseline=BASELINE)
        args.update(changes)
        return smoke.validate_evidence(**args)

    def test_actual_both_return_injections_and_class_definition_pass(self):
        proof = self.validate()
        self.assertEqual(2, proof["injected_return_calls"])
        self.assertTrue(proof["jvm_class_defined"])
        self.assertIn("gameplay is untested", proof["scope"])

    def test_clean_early_exit_can_pass(self):
        self.validate(return_code=0, timed_out=False)

    def test_unsuccessful_early_exit_fails_even_with_export(self):
        with self.assertRaises(smoke.SmokeFailure):
            self.validate(return_code=1, timed_out=False)

    def test_export_without_actual_class_definition_fails(self):
        for log in ("", CLASS_LOG.replace("GameRenderer", "GameRenderer$Nested"),
                    "Loading mixin into net.minecraft.client.renderer.GameRenderer"):
            with self.subTest(log=log), self.assertRaises(smoke.SmokeFailure):
                self.validate(class_log=log)

    def test_handler_present_without_native_pick_call_fails(self):
        with self.assertRaises(smoke.SmokeFailure):
            self.validate(disassembly=DISASSEMBLY.replace("invokespecial", "nop"))

    def test_only_one_return_injected_fails(self):
        with self.assertRaises(smoke.SmokeFailure):
            self.validate(disassembly=DISASSEMBLY.replace("invokespecial", "nop", 1))

    def test_one_shared_baseline_return_requires_one_injection(self):
        proof = self.validate(disassembly=DISASSEMBLY.replace("invokespecial", "nop", 1),
                              baseline=BASELINE.replace("       1: areturn\n", ""))
        self.assertEqual(1, proof["baseline_areturn_count"])
        self.assertEqual(1, proof["injected_return_calls"])

    def test_extra_injection_does_not_match_one_return_baseline(self):
        with self.assertRaisesRegex(smoke.SmokeFailure, "matched 2 calls; baseline 1"):
            self.validate(baseline=BASELINE.replace("       1: areturn\n", ""))

    def test_missing_or_already_transformed_baseline_fails(self):
        for baseline in ("", BASELINE.replace("areturn", "return"), DISASSEMBLY):
            with self.subTest(baseline=baseline), self.assertRaises(smoke.SmokeFailure):
                self.validate(baseline=baseline)

    def test_wrong_pick_descriptor_fails(self):
        with self.assertRaises(smoke.SmokeFailure):
            self.validate(disassembly=DISASSEMBLY.replace(smoke.PICK_DESCRIPTOR, "(F)V"))

    def test_unrelated_method_helper_call_does_not_validate_handler(self):
        changed = DISASSEMBLY.replace("       0: invokestatic", "  private void unrelated();\n    descriptor: ()V\n    Code:\n       0: invokestatic")
        with self.assertRaises(smoke.SmokeFailure):
            self.validate(disassembly=changed)

    def test_wrong_helper_descriptor_fails(self):
        with self.assertRaises(smoke.SmokeFailure):
            self.validate(disassembly=DISASSEMBLY.replace(smoke.HELPER_DESCRIPTOR, "()V"))

    def test_crash_report_and_injection_failures_are_rejected(self):
        with self.assertRaises(smoke.SmokeFailure):
            self.validate(crash_files=[Path("crash.txt")])
        for message in ("MixinApplyError", "InvalidInjectionException", "VerifyError", "BUILD FAILED", "Exception in thread render"):
            with self.subTest(message=message), self.assertRaises(smoke.SmokeFailure):
                self.validate(client_log=message)

    def test_nonfatal_headless_audio_or_service_log_is_not_a_crash(self):
        self.validate(client_log="[ERROR] Couldn't initialize OpenAL\n[ERROR] Failed to fetch user properties")

    def test_diagnostics_report_exact_target_calls_and_helper_link(self):
        report = smoke.evidence_diagnostics(DISASSEMBLY, BASELINE)
        self.assertIn('"exact_pick_found": true', report)
        self.assertIn('"baseline_areturn_count": 2', report)
        self.assertIn('"pick_areturn_count": 2', report)
        self.assertIn(HANDLER, report)
        self.assertIn(smoke.HELPER, report)
        self.assertIn("Exact native pick disassembly", report)

    def test_diagnostics_expose_renamed_handler_without_loosening_gate(self):
        changed = DISASSEMBLY.replace(HANDLER, HANDLER + "$unexpected")
        report = smoke.evidence_diagnostics(changed)
        self.assertIn(HANDLER + "$unexpected", report)
        with self.assertRaisesRegex(smoke.SmokeFailure, "matched 0 calls"):
            self.validate(disassembly=changed)

    def test_diagnostics_line_budget_is_bounded(self):
        report = smoke.evidence_diagnostics(DISASSEMBLY, max_lines=8)
        self.assertEqual(8, len(report.splitlines()))
        self.assertTrue(report.endswith("... linkage diagnostics truncated ..."))

    def test_baseline_artifact_requires_one_unique_exact_class(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            target = smoke.TARGET.replace(".", "/") + ".class"
            with zipfile.ZipFile(root / "client.jar", "w") as archive:
                archive.writestr(target, b"compiled class")
            with zipfile.ZipFile(root / "sources.jar", "w") as archive:
                archive.writestr(target.replace(".class", ".java"), "source only")
            proof = smoke.extract_baseline(root, root / "baseline.class")
            self.assertEqual(b"compiled class", (root / "baseline.class").read_bytes())
            self.assertEqual(1, len(proof["baseline_jars"]))
            with zipfile.ZipFile(root / "combined.jar", "w") as archive:
                archive.writestr(target, b"compiled class")
            self.assertEqual(2, len(smoke.extract_baseline(root, root / "baseline.class")["baseline_jars"]))
            with zipfile.ZipFile(root / "other-runtime.jar", "w") as archive:
                archive.writestr(target, b"different class")
            with self.assertRaisesRegex(smoke.SmokeFailure, "one unique GameRenderer baseline"):
                smoke.extract_baseline(root, root / "baseline.class")

    def test_baseline_artifact_without_exact_class_fails(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            with zipfile.ZipFile(root / "client.jar", "w") as archive:
                archive.writestr("other/GameRenderer.class", b"wrong class")
            with self.assertRaises(smoke.SmokeFailure):
                smoke.extract_baseline(root, root / "baseline.class")

    def test_timeout_stops_spawned_child_process(self):
        self.assert_child_stopped("sleep 60")

    def test_timeout_also_stops_a_detached_gradle_style_child(self):
        self.assert_child_stopped("setsid sleep 60")

    def assert_child_stopped(self, child_command):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            # A child holding stdout must not survive the process-group cleanup.
            command = ["bash", "-c", child_command + " & child=$!; echo $child > child.pid; wait"]
            code, expired = smoke.run_bounded(command, root, {}, root / "log.txt", 1)
            self.assertTrue(expired)
            self.assertNotEqual(0, code)
            pid = (root / "child.pid").read_text().strip()
            status = subprocess.run(["ps", "-o", "stat=", "-p", pid], capture_output=True, text=True)
            self.assertTrue(status.returncode != 0 or status.stdout.strip().startswith("Z"), status.stdout)


if __name__ == "__main__":
    unittest.main()
