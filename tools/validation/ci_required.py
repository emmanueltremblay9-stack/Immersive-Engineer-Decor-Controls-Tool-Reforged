"""Fail-closed Build aggregate; consumes data via environment, never shell code."""

from __future__ import annotations

import json
import os
from pathlib import Path
import re
import subprocess
from typing import Mapping


REQUIRED_JOBS = ("build", "lithium-compatibility", "renderer-matrix", "lava-surfaces")
OPTIONAL_JOB = "native-smelter-gui"
ALL_JOBS = REQUIRED_JOBS + (OPTIONAL_JOB,)
RESULTS = {"success", "failure", "cancelled", "skipped"}
SHA = re.compile(r"[0-9a-f]{40}")


def unique_object(pairs: list[tuple[str, object]]) -> dict[str, object]:
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("Duplicate JSON key")
        result[key] = value
    return result


def invalid_constant(value: str) -> None:
    raise ValueError("Non-JSON numeric constant")


def evaluate_needs(raw: str, require_native_gui: bool = False) -> tuple[dict[str, str], list[str]]:
    """Require the four base families and the targeted GUI job only on PR #133."""
    results = dict.fromkeys(ALL_JOBS, "missing")
    try:
        needs = json.loads(raw, object_pairs_hook=unique_object, parse_constant=invalid_constant)
    except (ValueError, TypeError):
        return results, ["Dependency evidence is not valid, unambiguous JSON."]
    if not isinstance(needs, dict):
        return results, ["Dependency evidence must be an object."]

    errors = []
    if set(needs) != set(ALL_JOBS):
        errors.append("Dependency evidence must contain exactly the four required families and the conditional native GUI job.")
    for job in ALL_JOBS:
        if job in needs:
            entry = needs[job]
            value = entry.get("result") if isinstance(entry, dict) else None
            results[job] = value if isinstance(value, str) and value in RESULTS else "unknown"
    for job in REQUIRED_JOBS:
        if results[job] != "success":
            errors.append(f"{job}: {results[job]} (required: success).")

    native_gui = results[OPTIONAL_JOB]
    accepted_native_gui_results = {"success"} if require_native_gui else {"success", "skipped"}
    if native_gui not in accepted_native_gui_results:
        requirement = "success" if require_native_gui else "success or skipped"
        errors.append(f"{OPTIONAL_JOB}: {native_gui} (required: {requirement}).")
    return results, errors

def identity_lines(env: Mapping[str, str], checkout_sha: str) -> list[str]:
    """Validate identity before emitting links; PR head is not the merge checkout."""
    event = env.get("GITHUB_EVENT_NAME", "")
    if event not in {"push", "pull_request", "workflow_dispatch"}:
        raise ValueError("Unsupported or missing workflow event.")
    if not SHA.fullmatch(checkout_sha) or checkout_sha != env.get("GITHUB_SHA"):
        raise ValueError("Actual checkout SHA must match the workflow's tested SHA.")
    repository = env.get("GITHUB_REPOSITORY", "")
    if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repository):
        raise ValueError("Missing or invalid repository identity.")
    if env.get("GITHUB_SERVER_URL") != "https://github.com":
        raise ValueError("Missing or unexpected GitHub server URL.")
    run_id = env.get("GITHUB_RUN_ID", "")
    attempt = env.get("GITHUB_RUN_ATTEMPT", "")
    if not re.fullmatch(r"[1-9][0-9]*", run_id) or not re.fullmatch(r"[1-9][0-9]*", attempt):
        raise ValueError("Missing or invalid run identity.")
    base = f"https://github.com/{repository}"
    run = f"{base}/actions/runs/{run_id}"
    lines = [f"Event: `{event}`."]
    if event == "pull_request":
        number = env.get("CI_PR_NUMBER", "")
        if not re.fullmatch(r"[1-9][0-9]*", number):
            raise ValueError("Missing or invalid PR number.")
        head = env.get("CI_PR_HEAD_SHA", "")
        if not SHA.fullmatch(head):
            raise ValueError("Missing or invalid PR head SHA.")
        lines += [
            f"PR head: [`{head}`]({base}/commit/{head}).",
            f"Actually tested PR merge checkout: [`{checkout_sha}`]({base}/commit/{checkout_sha}).",
        ]
    else:
        lines += ["PR head: not applicable.",
                  f"Actually tested checkout: [`{checkout_sha}`]({base}/commit/{checkout_sha})."]
    lines += [f"[Run {run_id}, attempt {attempt}]({run}/attempts/{attempt}) · "
              f"[Diagnostic artifacts]({run}#artifacts)."]
    return lines


def summarize(raw: str, env: Mapping[str, str], checkout_sha: str) -> tuple[bool, str]:
    require_native_gui = (
        env.get("GITHUB_EVENT_NAME") == "pull_request"
        and env.get("CI_PR_NUMBER") == "133"
    )
    results, errors = evaluate_needs(raw, require_native_gui=require_native_gui)
    try:
        identity = identity_lines(env, checkout_sha)
    except ValueError as error:
        identity = []
        errors.append(str(error))
    passed = not errors
    lines = ["## IEDCTR CI required", "", "PASS" if passed else "FAIL", ""]
    lines += ["| Validation family | Result |", "| --- | --- |"]
    lines += [f"| `{job}` | {results[job]} |" for job in REQUIRED_JOBS]
    expectation = "required" if require_native_gui else "conditional; skip allowed when not applicable"
    lines.append(f"| `{OPTIONAL_JOB}` | {results[OPTIONAL_JOB]} ({expectation}) |")
    lines += ["", *identity, ""]
    if errors:
        lines += ["Validation errors:", "", *(f"- {error}" for error in errors), ""]
    lines += [
        "The four required families cover eight validation jobs, including both matrices. "
        "Native Smelter GUI capture is required only for PR #133; the workflow may skip it on other PRs or events, but a failure never passes.",
        "Diagnostic artifacts retain the existing 14-day lifetime; this summary does not upload a runtime JAR.",
        "This aggregate is informational until separately configured in branch protection. "
        "It does not authorize a merge or establish release readiness.",
        "",
    ]
    return passed, "\n".join(lines)


def main() -> int:
    try:
        checkout_sha = subprocess.run(
            ["git", "rev-parse", "HEAD"], check=True, capture_output=True, text=True
        ).stdout.strip()
    except (OSError, subprocess.CalledProcessError):
        checkout_sha = ""
    passed, summary = summarize(os.environ.get("CI_NEEDS_JSON", ""), os.environ, checkout_sha)
    print(summary)
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if not summary_path:
        print("Missing GITHUB_STEP_SUMMARY; aggregate cannot record its evidence.")
        return 1
    try:
        with Path(summary_path).open("a", encoding="utf-8") as target:
            target.write(summary)
    except OSError as error:
        print(f"Could not record aggregate evidence: {error.__class__.__name__}.")
        return 1
    return 0 if passed else 1


if __name__ == "__main__":
    raise SystemExit(main())
