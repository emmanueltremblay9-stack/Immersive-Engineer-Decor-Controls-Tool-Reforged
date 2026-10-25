import itertools
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import unittest

from ci_required import REQUIRED_JOBS, evaluate_needs, summarize


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "tools/validation/ci_required.py"
HEAD = "a" * 40
MERGE = "b" * 40


def successful_needs():
    return {job: {"result": "success", "outputs": {}} for job in REQUIRED_JOBS}


def environment(event="pull_request", sha=MERGE):
    return {"GITHUB_EVENT_NAME": event, "GITHUB_SHA": sha,
            "CI_PR_HEAD_SHA": HEAD, "GITHUB_SERVER_URL": "https://github.com",
            "GITHUB_REPOSITORY": "owner/repo", "GITHUB_RUN_ID": "123",
            "GITHUB_RUN_ATTEMPT": "2"}


def mapping_block(text, key, indent=0):
    """Read a literal mapping block for static invariants, not a YAML interpreter."""
    matches = list(re.finditer(rf"^{' ' * indent}{re.escape(key)}: *\n", text, re.M))
    if len(matches) != 1:
        raise AssertionError(f"Expected exactly one literal {key} mapping")
    lines = []
    for line in text[matches[0].end():].splitlines():
        if line.strip() and not line.lstrip().startswith("#") and len(line) - len(line.lstrip()) <= indent:
            break
        lines.append(line)
    return "\n".join(lines)


def meaningful(text):
    return "\n".join(line.rstrip() for line in text.splitlines()
                     if line.strip() and not line.lstrip().startswith("#"))


class RequiredGateTests(unittest.TestCase):
    def test_exact_four_successful_results_pass(self):
        self.assertEqual([], evaluate_needs(json.dumps(successful_needs()))[1])

    def test_all_256_documented_result_combinations_fail_except_all_success(self):
        for states in itertools.product(("success", "failure", "cancelled", "skipped"), repeat=4):
            with self.subTest(states=states):
                proof = {job: {"result": state} for job, state in zip(REQUIRED_JOBS, states)}
                results, errors = evaluate_needs(json.dumps(proof))
                self.assertEqual(states, tuple(results.values()))
                self.assertEqual(all(state == "success" for state in states), not errors)

    def test_missing_dependency_and_extra_dependency_fail(self):
        for job in REQUIRED_JOBS:
            proof = successful_needs()
            del proof[job]
            with self.subTest(job=job):
                self.assertTrue(evaluate_needs(json.dumps(proof))[1])
        proof = successful_needs()
        proof["unexpected"] = {"result": "success"}
        self.assertTrue(evaluate_needs(json.dumps(proof))[1])

    def test_missing_unknown_and_wrong_type_results_fail_for_every_job(self):
        for job in REQUIRED_JOBS:
            for value in ("unknown", "neutral", "timed_out", "Success", "success ", "", None,
                          True, 1, [], {}, ["success"]):
                proof = successful_needs()
                proof[job]["result"] = value
                with self.subTest(job=job, value=value):
                    self.assertTrue(evaluate_needs(json.dumps(proof))[1])
            for entry in ({}, {"outputs": {"result": "success"}}, "success", None, []):
                proof = successful_needs()
                proof[job] = entry
                with self.subTest(job=job, entry=entry):
                    self.assertTrue(evaluate_needs(json.dumps(proof))[1])

    def test_malformed_non_object_ambiguous_or_non_json_evidence_fails(self):
        raw = json.dumps(successful_needs())
        cases = ("", "{", "{}", "null", "[]", '"success"', "true", "12",
                 raw + "{}", raw.replace('"result": "success"',
                 '"result": "failure", "result": "success"', 1),
                 raw[:-1] + ', "build": {"result": "success"}}',
                 raw.replace('"outputs": {}', '"outputs": NaN', 1))
        for value in cases:
            with self.subTest(value=value):
                self.assertTrue(evaluate_needs(value)[1])

    def test_outputs_are_not_used_as_status_or_printed(self):
        proof = successful_needs()
        proof["build"]["outputs"] = {"untrusted": "::error::do not print this"}
        passed, summary = summarize(json.dumps(proof), environment(), MERGE)
        self.assertTrue(passed)
        self.assertNotIn("do not print", summary)

    def test_pr_summary_separates_head_and_tested_merge_and_links_evidence(self):
        passed, summary = summarize(json.dumps(successful_needs()), environment(), MERGE)
        self.assertTrue(passed)
        self.assertIn(f"PR head: [`{HEAD}`]", summary)
        self.assertIn(f"Actually tested PR merge checkout: [`{MERGE}`]", summary)
        self.assertIn("https://github.com/owner/repo/actions/runs/123/attempts/2", summary)
        self.assertIn("https://github.com/owner/repo/actions/runs/123#artifacts", summary)
        self.assertIn("informational until separately configured", summary)
        self.assertIn("does not authorize a merge or establish release readiness", summary)

    def test_push_and_dispatch_summaries_do_not_claim_a_pr_merge(self):
        for event in ("push", "workflow_dispatch"):
            env = environment(event)
            del env["CI_PR_HEAD_SHA"]
            passed, summary = summarize(json.dumps(successful_needs()), env, MERGE)
            self.assertTrue(passed)
            self.assertIn("PR head: not applicable", summary)
            self.assertNotIn("tested PR merge", summary)

    def test_missing_wrong_and_unsafe_identity_fail(self):
        for key in environment():
            env = environment()
            del env[key]
            with self.subTest(missing=key):
                self.assertFalse(summarize(json.dumps(successful_needs()), env, MERGE)[0])
        for key, value in (("GITHUB_SHA", HEAD), ("GITHUB_EVENT_NAME", "pull_request_target"),
                           ("GITHUB_RUN_ID", "0"), ("GITHUB_RUN_ATTEMPT", "-1"),
                           ("GITHUB_SERVER_URL", "https://example.invalid"),
                           ("GITHUB_REPOSITORY", "owner/repo\n::error::injected"),
                           ("CI_PR_HEAD_SHA", "not a sha")):
            env = environment()
            env[key] = value
            with self.subTest(key=key, value=value):
                self.assertFalse(summarize(json.dumps(successful_needs()), env, MERGE)[0])
        self.assertFalse(summarize(json.dumps(successful_needs()), environment(), "")[0])

    def test_failure_summary_names_failed_family(self):
        proof = successful_needs()
        proof["renderer-matrix"]["result"] = "cancelled"
        passed, summary = summarize(json.dumps(proof), environment(), MERGE)
        self.assertFalse(passed)
        self.assertIn("FAIL", summary)
        self.assertIn("renderer-matrix: cancelled (required: success)", summary)

    def run_cli(self, raw, temp, summary=True):
        actual_sha = subprocess.run(["git", "rev-parse", "HEAD"], cwd=ROOT, check=True,
                                    capture_output=True, text=True).stdout.strip()
        env = os.environ.copy()
        env.update(environment(sha=actual_sha))
        env["CI_NEEDS_JSON"] = raw
        env.pop("GITHUB_STEP_SUMMARY", None)
        path = Path(temp) / "summary.md"
        if summary:
            env["GITHUB_STEP_SUMMARY"] = str(path)
        result = subprocess.run([sys.executable, str(SCRIPT)], cwd=ROOT, env=env,
                                capture_output=True, text=True)
        return result, path

    def test_cli_success_records_summary_and_returns_zero(self):
        with tempfile.TemporaryDirectory() as temp:
            result, path = self.run_cli(json.dumps(successful_needs()), temp)
            self.assertEqual(0, result.returncode, result.stderr)
            self.assertIn("PASS", path.read_text())

    def test_cli_failure_and_missing_summary_return_nonzero(self):
        with tempfile.TemporaryDirectory() as temp:
            result, path = self.run_cli("{}", temp)
            self.assertEqual(1, result.returncode)
            self.assertIn("FAIL", path.read_text())
            result, _ = self.run_cli(json.dumps(successful_needs()), temp, summary=False)
            self.assertEqual(1, result.returncode)

    def test_cli_treats_shell_payload_as_data_and_never_echoes_it(self):
        with tempfile.TemporaryDirectory() as temp:
            marker = Path(temp) / "should-not-exist"
            proof = successful_needs()
            proof["build"]["result"] = f"$(touch {marker})`touch {marker}`\n::error::injected"
            result, path = self.run_cli(json.dumps(proof), temp)
            self.assertEqual(1, result.returncode)
            self.assertFalse(marker.exists())
            self.assertNotIn("touch", result.stdout + path.read_text())


class BuildWorkflowContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.workflow = (ROOT / ".github/workflows/build.yml").read_text()
        cls.jobs = mapping_block(cls.workflow, "jobs")

    def test_events_keep_main_all_tags_prs_and_explicit_dispatch_only(self):
        # Literal contract: no feature push, path filters, schedules or privileged PR event.
        self.assertEqual("  push:\n    branches: [main]\n    tags: ['**']\n"
                         "  pull_request:\n  workflow_dispatch:",
                         meaningful(mapping_block(self.workflow, "on")))
        self.assertIn("name: Build\n", self.workflow)

    def test_concurrency_keys_same_pr_by_workflow_and_all_other_runs_by_unique_id(self):
        # Statically check the actual Actions expressions, not a Python imitation.
        # PR: workflow+PR number; push/main, push/tag, dispatch: workflow+unique run ID.
        self.assertEqual(
            "  group: ${{ github.workflow }}-${{ github.event_name == 'pull_request' && "
            "format('pr-{0}', github.event.pull_request.number) || format('run-{0}', github.run_id) }}\n"
            "  cancel-in-progress: ${{ github.event_name == 'pull_request' }}",
            meaningful(mapping_block(self.workflow, "concurrency")))
        self.assertEqual(1, self.workflow.count("concurrency:"))
        self.assertNotIn("github.head_ref", mapping_block(self.workflow, "concurrency"))

    def test_gate_always_evaluates_exact_dependencies_without_failure_override(self):
        gate = mapping_block(self.jobs, "ci-required", indent=2)
        self.assertIn("    name: IEDCTR CI required\n", gate)
        self.assertIn("    if: ${{ always() }}\n", gate)
        self.assertIn("    needs: [build, lithium-compatibility, renderer-matrix, lava-surfaces]\n", gate)
        self.assertEqual(1, gate.count("if:"))
        self.assertNotIn("continue-on-error", self.workflow)
        self.assertIn("    timeout-minutes: 5\n", gate)

    def test_needs_is_env_json_and_command_never_interpolates_it(self):
        gate = mapping_block(self.jobs, "ci-required", indent=2)
        self.assertIn("          CI_NEEDS_JSON: ${{ toJSON(needs) }}\n", gate)
        self.assertIn("          CI_PR_HEAD_SHA: ${{ github.event.pull_request.head.sha }}\n", gate)
        self.assertEqual(["python tools/validation/ci_required.py"],
                         re.findall(r"^        run: (.+)$", gate, re.M))
        self.assertNotIn("ref:", gate)  # Default checkout must remain the tested PR merge ref.

    def test_exact_job_families_and_all_eight_matrix_entries_are_preserved(self):
        self.assertEqual([*REQUIRED_JOBS, "ci-required"], re.findall(r"^  ([\w-]+):$", self.jobs, re.M))
        for job, profile in (("renderer-matrix", "native, embeddium, modernfix, both"),
                             ("lava-surfaces", "native, embeddium")):
            block = mapping_block(self.jobs, job, indent=2)
            self.assertIn(f"        profile: [{profile}]\n", block)
            self.assertIn("      fail-fast: false\n", block)
        for job in REQUIRED_JOBS:
            block = mapping_block(self.jobs, job, indent=2)
            self.assertNotRegex(block, r"(?m)^    if:")
        build = mapping_block(self.jobs, "build", indent=2)
        for command in ("./gradlew compileJava processResources --stacktrace", "./gradlew test --stacktrace",
                        "./gradlew runGameTestServer --stacktrace", "./gradlew build --stacktrace",
                        "python -W error::ResourceWarning -m unittest discover -s tools/validation -p 'test_*.py' -v"):
            self.assertIn(command, build)

    def test_read_only_permissions_and_pinned_actions_are_preserved(self):
        self.assertEqual("  contents: read", meaningful(mapping_block(self.workflow, "permissions")))
        self.assertEqual(1, self.workflow.count("permissions:"))
        pins = {"actions/checkout": ("3d3c42e5aac5ba805825da76410c181273ba90b1", 5),
                "actions/setup-java": ("b6effb05e454b25005698d916606bdc6ffcbf961", 4),
                "actions/setup-python": ("5fda3b95a4ea91299a34e894583c3862153e4b97", 5),
                "gradle/actions/setup-gradle": ("9c971963bec38e04b3d30dcc455b5382be2fdbfb", 4),
                "actions/upload-artifact": ("043fb46d1a93c77aae656e7c1c64a875d1fc6a0a", 4)}
        actions = re.findall(r"uses: ([^@\s]+)@([^\s]+)", self.workflow)
        self.assertEqual(set(pins), {action for action, _ in actions})
        for action, (sha, count) in pins.items():
            self.assertEqual([sha] * count, [version for name, version in actions if name == action])

    def test_existing_diagnostic_artifact_retention_and_missing_file_failure(self):
        for job in REQUIRED_JOBS:
            block = mapping_block(self.jobs, job, indent=2)
            self.assertEqual(1, block.count("uses: actions/upload-artifact@"))
            self.assertEqual(1, block.count("          retention-days: 14"))
            self.assertEqual(1, block.count("          if-no-files-found: error"))
        self.assertNotIn("uses: actions/upload-artifact@", mapping_block(self.jobs, "ci-required", indent=2))


if __name__ == "__main__":
    unittest.main()
