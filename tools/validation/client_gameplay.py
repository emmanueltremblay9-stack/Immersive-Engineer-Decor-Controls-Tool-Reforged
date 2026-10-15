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
    if proof.get("fixture_attack_context_verified") is not True:
        raise SmokeFailure("Gameplay fixture did not verify an active attack binding under its isolation screen")
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
                     "authoritative_deck_removed", "active_attack_binding", "synchronous_lamp_completion_event",
                     "equal_id_cross_side_players_verified"):
            if case.get(flag) is not True:
                raise SmokeFailure(f"{label} lacks {flag}")
        notifications = case.get("client_lamp_removal_notifications")
        if type(notifications) is not int or notifications != 1:
            raise SmokeFailure(f"{label} lacks exactly one synchronous native client lamp completion")
        for stage in ("lamp", "deck"):
            action = "START_DESTROY_BLOCK" if case["mode"] == "creative" else "STOP_DESTROY_BLOCK"
            if case.get(f"{stage}_completion_action") != action:
                raise SmokeFailure(f"{label} lacks the exact native {stage} completion action")
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
        checks = case.get("survival_lamp_hold_checks")
        if type(checks) is not int or (checks != case["native_mining_ticks"] if expected else checks != 0):
            raise SmokeFailure(f"{label} did not preserve its own intent on every survival lamp HOLD")
    marker = f"IEDCTR_CLIENT_GAMEPLAY PASSED nonce={nonce} cases={len(EXPECTED_CASES)}"
    if marker not in logs:
        raise SmokeFailure("Missing current-run successful gameplay completion marker")
    return proof


def validate_fluidlogging_proof(proof: dict, nonce: str) -> dict:
    blocks = {f"immersive_engineer_decor_controls_tool_reforged:{name}"
              for name in ("steel_catwalk", "steel_catwalk_stairs", "steel_catwalk_ta", "steel_railing")}
    fluids = {"minecraft:water", "minecraft:lava", "immersiveengineering:creosote"}
    expected = {(block, fluid, sync) for block in blocks for fluid in fluids for sync in ("update", "chunk")}
    if (proof.get("suite") != "native-fluidlogging-client-v1" or proof.get("nonce") != nonce
            or proof.get("status") != "passed" or proof.get("actual_integrated_world") is not True):
        raise SmokeFailure("Missing matching native fluidlogging client proof")
    cases = proof.get("cases")
    if not isinstance(cases, list) or len(cases) != len(expected):
        raise SmokeFailure("Incomplete native fluidlogging scenario matrix")
    seen = set()
    for case in cases:
        if not isinstance(case, dict):
            raise SmokeFailure("Invalid fluidlogging case")
        identity = (case.get("block"), case.get("fluid"), case.get("sync"))
        if identity not in expected or identity in seen:
            raise SmokeFailure("Duplicate or unexpected fluidlogging case")
        seen.add(identity)
        vertices = case.get("native_textured_vertices")
        if (case.get("client_world_and_chunk_source") is not True or case.get("client_drain_cleared") is not True
                or type(vertices) is not int or vertices <= 0 or vertices % 4 != 0):
            raise SmokeFailure(f"Fluidlogging case lacks synchronization or textured geometry: {identity}")
    return proof



def validate_canal_proof(proof: dict, nonce: str, directory: Path) -> dict:
    import struct
    expected = {"stairs-wet", "stairs-dry-control", "catwalk-wet", "raised-wet",
                "stairs-railed-wet", "stairs-railed-reversed-wet"}
    if (proof.get("suite") != "native-water-canal-diagnostic-v1"
            or proof.get("nonce") != nonce or proof.get("status") != "captured"):
        raise SmokeFailure("Missing current canal scene proof")
    cases = proof.get("cases")
    if not isinstance(cases, list) or len(cases) != len(expected):
        raise SmokeFailure("Incomplete canal scene matrix")
    seen = set()
    for case in cases:
        name = case.get("case")
        if name not in expected or name in seen:
            raise SmokeFailure("Duplicate or unexpected canal scene")
        seen.add(name)
        wet = name != "stairs-dry-control"
        if case.get("source") is not wet:
            raise SmokeFailure(f"Canal source state mismatch: {name}")
        quads = case.get("upper_horizontal_quads")
        if type(quads) is not int or (quads < 1 if wet else quads != 0):
            raise SmokeFailure(f"Canal lacks the expected exposed upper fluid surface: {name}")
        for angle in ("oblique", "overhead"):
            filename = f"{name}-{angle}.png"
            if case.get(f"{angle}_screenshot") != filename:
                raise SmokeFailure("Unexpected canal screenshot identity")
            image = directory / "canal-screenshots" / filename
            data = image.read_bytes()
            if len(data) < 1000 or data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR":
                raise SmokeFailure("Canal screenshot is absent or invalid")
            width, height = struct.unpack(">II", data[16:24])
            if width < 600 or height < 400:
                raise SmokeFailure("Canal screenshot is too small to inspect")
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
        fluid_proof = json.loads((game / "client-fluidlogging-result.json").read_text(encoding="utf-8"))
        validate_fluidlogging_proof(fluid_proof, nonce)
        canal_proof = json.loads((game / "client-water-canal-result.json").read_text(encoding="utf-8"))
        validate_canal_proof(canal_proof, nonce, game)
        print(json.dumps(canal_proof, indent=2), flush=True)
        print(json.dumps(fluid_proof, indent=2), flush=True)
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
