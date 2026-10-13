#!/usr/bin/env python3
"""Run isolated, bounded native-client lamp mining and prediction/ACK regressions.

Unlike client_mixin_smoke.py, a title screen or a timed-out client cannot pass.
No external world, user options or installed Minecraft directory is used.
"""

from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import secrets
import shutil
import subprocess
import sys
import tempfile

from client_mixin_smoke import FATAL_PATTERN, SmokeFailure, run_bounded


SUITE = "native-client-lamp-mining-v1"
APPROACHES = ("down", "north", "east", "south", "west")
EXPECTED_CASES = {f"{mode}-{side}" for mode in ("creative", "survival") for side in APPROACHES}


def validate_proof(proof: dict, nonce: str, logs: str, exit_code: int | None,
                   timed_out: bool, crash_files: list[Path]) -> dict:
    """Fail closed on missing/stale/incomplete/replayed or loading-only evidence."""
    if timed_out or exit_code != 0:
        raise SmokeFailure(f"Native gameplay client did not finish cleanly (exit={exit_code}, deadline={timed_out})")
    if crash_files:
        raise SmokeFailure("Native gameplay client wrote a crash report")
    fatal = FATAL_PATTERN.search(logs)
    if fatal:
        raise SmokeFailure(f"Fatal client diagnostic: {fatal.group(0)}")
    if proof.get("suite") != SUITE or proof.get("nonce") != nonce or proof.get("status") != "passed":
        raise SmokeFailure("Missing matching successful gameplay proof")
    if proof.get("actual_integrated_world") is not True or proof.get("expected_cases") != len(EXPECTED_CASES):
        raise SmokeFailure("Gameplay proof lacks its real integrated-world scope")
    cases = proof.get("cases")
    if not isinstance(cases, list) or len(cases) != len(EXPECTED_CASES):
        raise SmokeFailure("Incomplete native gameplay scenario matrix")
    if any(not isinstance(case, dict) for case in cases):
        raise SmokeFailure("Invalid native gameplay case")
    names = [case.get("case") for case in cases]
    if len(set(names)) != len(names) or set(names) != EXPECTED_CASES:
        raise SmokeFailure("Duplicate or missing native gameplay scenario")
    for case in cases:
        label = case["case"]
        if label != f"{case.get('mode')}-{case.get('approach')}":
            raise SmokeFailure(f"Inconsistent scenario identity: {label}")
        for flag in ("immediate_client_prediction_lamp_only", "authoritative_lamp_only",
                     "release_allows_native_deck_mining", "immediate_client_prediction_deck_removed",
                     "authoritative_deck_removed"):
            if case.get(flag) is not True:
                raise SmokeFailure(f"{label} lacks {flag}")
        for stage in ("lamp", "deck"):
            sequence, ack = case.get(f"{stage}_sequence"), case.get(f"{stage}_ack_applied")
            if type(sequence) is not int or type(ack) is not int or sequence < 0 or ack < sequence:
                raise SmokeFailure(f"{label} lacks processed native {stage} ACK evidence")
        if case["deck_sequence"] <= case["lamp_sequence"]:
            raise SmokeFailure(f"{label} reused an old mining sequence")
        if type(case.get("held_ticks")) is not int or case["held_ticks"] < 12:
            raise SmokeFailure(f"{label} lacks a sustained held-input observation")
        if type(case.get("held_attack_cancellations")) is not int or case["held_attack_cancellations"] < case["held_ticks"]:
            raise SmokeFailure(f"{label} did not suppress every continued attack")
        if type(case.get("held_extra_mining_packets")) is not int or case["held_extra_mining_packets"] != 0:
            raise SmokeFailure(f"{label} leaked native mining packets after lamp removal")
        expected = 1 if case["mode"] == "survival" else 0
        if case.get("lamp_drops_or_inventory") != expected or case.get("lamp_tool_damage") != expected:
            raise SmokeFailure(f"{label} has incorrect authoritative lamp loot/tool wear")
        if type(case.get("native_mining_ticks")) is not int or case["native_mining_ticks"] < expected:
            raise SmokeFailure(f"{label} did not exercise native mining duration")
    marker = f"IEDCTR_CLIENT_GAMEPLAY PASSED nonce={nonce} cases={len(EXPECTED_CASES)}"
    if marker not in logs:
        raise SmokeFailure("Missing current-run successful gameplay completion marker")
    return proof


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--prepare-timeout", type=int, default=600)
    parser.add_argument("--timeout", type=int, default=420)
    args = parser.parse_args()
    if sys.platform != "linux":
        parser.error("The native gameplay runner supports isolated Linux CI only")
    if min(args.prepare_timeout, args.timeout) <= 0:
        parser.error("Timeouts must be positive")
    missing = [name for name in ("java", "Xvfb", "xvfb-run", "xauth") if not shutil.which(name)]
    if missing:
        print("Native gameplay prerequisites missing: " + ", ".join(missing), file=sys.stderr)
        return 1
    project = args.project.resolve()
    if not (project / "gradlew").is_file():
        parser.error("Project must contain the checked-in Gradle wrapper")
    (project / "build").mkdir(exist_ok=True)
    evidence = Path(tempfile.mkdtemp(prefix="client-gameplay-", dir=project / "build"))
    game = evidence / "run"
    game.mkdir()
    nonce = secrets.token_hex(16)
    (game / ".iedctr-client-gameplay-nonce").write_text(nonce, encoding="utf-8")
    # These options apply only to this empty fixture directory. No user data or
    # resource packs are imported; onboarding cannot hide the title screen.
    (game / "options.txt").write_text(
        "onboardAccessibility:false\ntutorialStep:none\nrenderDistance:2\nsimulationDistance:2\n"
        "maxFps:60\npauseOnLostFocus:false\nautoJump:true\nsoundCategory_master:0.0\n", encoding="utf-8")
    init = evidence / "gameplay.init.gradle"
    init.write_text("""// Isolated, explicit, opt-in native gameplay fixture.
gradle.projectsEvaluated {
    rootProject.extensions.getByName('neoForge').runs.named('client') {
        gameDirectory.set(rootProject.file(System.getenv('IEDCTR_GAMEPLAY_DIR')))
        systemProperty 'iedctr.clientGameplay', 'true'
        systemProperty 'iedctr.clientGameplayNonce', System.getenv('IEDCTR_GAMEPLAY_NONCE')
        systemProperty 'iedctr.clientGameplayDirectory', System.getenv('IEDCTR_GAMEPLAY_DIR')
    }
}
""", encoding="utf-8")
    env = dict(os.environ, IEDCTR_GAMEPLAY_DIR=str(game), IEDCTR_GAMEPLAY_NONCE=nonce,
               LIBGL_ALWAYS_SOFTWARE="1", ALSOFT_DRIVERS="null")
    gradle = [str(project / "gradlew"), "--no-daemon", "--init-script", str(init), "--stacktrace"]
    prepare_log, client_log = evidence / "prepare.log", evidence / "client.log"
    print(f"Native client gameplay evidence: {evidence}", flush=True)
    try:
        code, expired = run_bounded(gradle + ["prepareClientRun"], project, env, prepare_log, args.prepare_timeout)
        if expired or code != 0:
            raise SmokeFailure(f"Native client preparation failed (exit={code}, deadline={expired})")
        code, expired = run_bounded(["xvfb-run", "-a", "-s", "-screen 0 1280x720x24"] + gradle + ["runClient"],
                                    project, env, client_log, args.timeout)
        path = game / "client-gameplay-result.json"
        if not path.is_file():
            raise SmokeFailure("No native gameplay proof was produced; loading alone cannot pass")
        proof = json.loads(path.read_text(encoding="utf-8"))
        logs = client_log.with_suffix(".observed.log").read_text(encoding="utf-8", errors="replace")
        for name in ("latest.log", "debug.log"):
            runtime = game / "logs" / name
            if runtime.is_file():
                logs += "\n" + runtime.read_text(encoding="utf-8", errors="replace")
        crashes = list(game.glob("crash-reports/*")) + list(evidence.rglob("hs_err_pid*.log"))
        validate_proof(proof, nonce, logs, code, expired, crashes)
        print(json.dumps(proof, indent=2), flush=True)
        return 0
    except (SmokeFailure, OSError, ValueError, TypeError, subprocess.SubprocessError) as error:
        print(f"Native client gameplay FAILED: {error}\nEvidence: {evidence}", file=sys.stderr)
        for diagnostic in (prepare_log, client_log.with_suffix(".observed.log"),
                           game / "logs/latest.log", game / "client-gameplay-result.json"):
            if diagnostic.is_file():
                print(f"--- {diagnostic.name}: last 100 lines ---", file=sys.stderr)
                print("\n".join(diagnostic.read_text(encoding="utf-8", errors="replace").splitlines()[-100:]), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
