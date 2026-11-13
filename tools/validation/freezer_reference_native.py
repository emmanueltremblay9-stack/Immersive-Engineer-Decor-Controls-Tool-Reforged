#!/usr/bin/env python3
"""Run the bounded Freezer V5 smoke in a fresh isolated Linux client.

A CPU preview, stale file, title screen, missing case or dirty checkout cannot pass.
This runner does not publish, upload, merge, release, or use an installed client.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import os
from pathlib import Path
import re
import secrets
import shutil
import struct
import subprocess
import sys
import tempfile

from client_mixin_smoke import FATAL_PATTERN, SmokeFailure, run_bounded

SUITE = "native-freezer-reference-v1"
ART_SHA = "68c1dc11e7d0e920addebca18100892861a70c5d68db5547b8014235acee3d46"
SCREEN_CLASS = "com.oblixorprime.engineersdecorreforged.client.FreezerReferenceScreen"
CASES = {
    "pickup_from_hotbar", "right_click_places_one_input", "output_rejects_insertion",
    "return_carried_stack", "full_inventory_preserves_reserved",
    "recovery_preserves_count_and_components", "repeated_recovery_no_duplicate",
    "close_reaches_server", "reopen_preserves_inventory_and_geometry", "real_recipe_consumes_1440_fe", "natural_frost_phases_0_to_3_and_completion_reset",
}
CAPTURES = {"energy-0.png": (0, 0), "energy-8000.png": (8000, 5),
            "energy-16000.png": (16000, 11), "completed-recipe.png": (0, 0)}


def validate_proof(proof: dict, main: dict, nonce: str, source_sha: str, game: Path,
                   logs: str, exit_code: int | None, timed_out: bool,
                   width: int, height: int, language: str) -> dict:
    if language not in ("en_us", "fr_fr") or proof.get("selected_language") != language:
        raise SmokeFailure("Native Freezer selected language differs from the requested locale")
    if timed_out or exit_code != 0 or FATAL_PATTERN.search(logs):
        raise SmokeFailure("Native Freezer client did not finish cleanly")
    if list(game.glob("crash-reports/*")) or list(game.parent.rglob("hs_err_pid*.log")):
        raise SmokeFailure("Native Freezer client wrote a crash report")
    if (main.get("suite") != SUITE or main.get("nonce") != nonce or main.get("status") != "passed"
            or main.get("actual_integrated_world") is not True):
        raise SmokeFailure("Isolated native-world harness did not complete this run")
    if (proof.get("suite") != SUITE or proof.get("nonce") != nonce
            or proof.get("status") != "passed_bounded_smoke"
            or proof.get("executed_source_sha") != source_sha
            or proof.get("source_art_sha256") != ART_SHA
            or proof.get("actual_integrated_world") is not True
            or proof.get("frost_art_static") is not False
            or proof.get("decorative_rim_frost_static") is not True
            or proof.get("phase_source") != "server_menu_phase_natural_recipe"
            or proof.get("phase4_native_observed") is not False
            or proof.get("native_visual_acceptance") is not False
            or type(proof.get("menu_slot_count")) is not int or proof["menu_slot_count"] != 39):
        raise SmokeFailure("Freezer proof identity, bounded scope or slot contract differs")
    cases = proof.get("cases")
    if not isinstance(cases, list) or any(not isinstance(case, str) for case in cases) or len(cases) != len(CASES) or set(cases) != CASES:
        raise SmokeFailure("Incomplete or duplicate Freezer interaction cases")
    captures = proof.get("captures")
    if not isinstance(captures, list) or len(captures) != len(CAPTURES):
        raise SmokeFailure("Incomplete native Freezer capture matrix")
    seen, menu_ids, hashes = set(), {}, {}
    for capture in captures:
        if not isinstance(capture, dict):
            raise SmokeFailure("Invalid Freezer capture")
        name = capture.get("name")
        if name not in CAPTURES or name in seen:
            raise SmokeFailure("Unexpected, duplicate or unsafe capture name")
        seen.add(name)
        energy, cells = CAPTURES[name]
        if (type(capture.get("energy")) is not int or capture["energy"] != energy
                or type(capture.get("lit_cells")) is not int or capture["lit_cells"] != cells
                or capture.get("screen_class") != SCREEN_CLASS
                or type(capture.get("menu_id")) is not int or capture["menu_id"] <= 0
                or type(capture.get("view_scale")) not in (int, float)
                or not 0 < capture["view_scale"] <= 1):
            raise SmokeFailure("Native Freezer capture has incorrect live menu state")
        path = game / "freezer-reference-screenshots" / name
        if not path.is_file() or path.is_symlink():
            raise SmokeFailure("Missing native framebuffer PNG")
        data = path.read_bytes()
        if (len(data) < 33 or data[:16] != b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR"
                or struct.unpack(">II", data[16:24]) != (width, height)
                or type(capture.get("width")) is not int or capture["width"] != width
                or type(capture.get("height")) is not int or capture["height"] != height
                or capture.get("sha256") != hashlib.sha256(data).hexdigest()):
            raise SmokeFailure("Native Freezer PNG bytes or dimensions differ from proof")
        expected_scale = min(1, (min(width, height) - 12) / 314)
        if abs(capture["view_scale"] - expected_scale) > 0.00001:
            raise SmokeFailure("Freezer panel did not use the expected fitted viewport")
        # NativeImage writes deterministic meter crops; require the gauge itself to
        # change, not merely an animated full-frame world backdrop.
        meter_name = name[:-4] + "-meter.png"
        meter_path = game / "freezer-reference-screenshots" / meter_name
        scale = capture["view_scale"]
        ox, oy = (width - 314 * scale) / 2, (height - 314 * scale) / 2
        mw = math.ceil(ox + 790 * 314 / 1254 * scale) - math.floor(ox + 500 * 314 / 1254 * scale)
        mh = math.ceil(oy + 698 * 314 / 1254 * scale) - math.floor(oy + 670 * 314 / 1254 * scale)
        if capture.get("meter_name") != meter_name or not meter_path.is_file() or meter_path.is_symlink():
            raise SmokeFailure("Missing native Freezer meter crop")
        meter_data = meter_path.read_bytes()
        if (len(meter_data) < 33 or meter_data[:16] != b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR"
                or struct.unpack(">II", meter_data[16:24]) != (mw, mh)
                or type(capture.get("meter_width")) is not int or capture["meter_width"] != mw
                or type(capture.get("meter_height")) is not int or capture["meter_height"] != mh
                or capture.get("meter_sha256") != hashlib.sha256(meter_data).hexdigest()):
            raise SmokeFailure("Native meter bytes or geometry differ from proof")
        menu_ids[name], hashes[name] = capture["menu_id"], capture["meter_sha256"]
    if len({menu_ids[name] for name in CAPTURES if name.startswith("energy-")}) != 1:
        raise SmokeFailure("Energy states were captured from different menus")
    if menu_ids["completed-recipe.png"] == menu_ids["energy-0.png"]:
        raise SmokeFailure("Reopened Freezer did not get a new menu identity")
    if len({hashes[name] for name in CAPTURES if name.startswith("energy-")}) != 3:
        raise SmokeFailure("Native meter crops are duplicates, not live gauge changes")
    frost_captures = proof.get("frost_captures")
    if not isinstance(frost_captures, list) or len(frost_captures) != 4:
        raise SmokeFailure("Missing natural frost phase captures")
    seen_phases, frost_hashes = set(), set()
    for capture in frost_captures + captures:
        phase, ticks = capture.get("phase"), capture.get("progress_ticks")
        if type(phase) is not int or type(ticks) is not int:
            raise SmokeFailure("Invalid synchronized frost state")
        natural = capture in frost_captures
        if natural:
            if phase not in range(4) or phase in seen_phases or capture.get("name") != f"frost-phase-{phase}.png":
                raise SmokeFailure("Invalid or duplicate natural phase")
            if not (ticks == 0 if phase == 0 else phase * 45 <= ticks < (phase + 1) * 45):
                raise SmokeFailure("Natural phase/progress mismatch")
            if (capture.get("width") != width or capture.get("height") != height
                    or (phase > 0 and capture.get("energy") != 1440 - 8 * ticks)):
                raise SmokeFailure("Natural frost capture dimensions/real FE differ")
            seen_phases.add(phase)
            if capture.get("screen_class") != SCREEN_CLASS or capture.get("menu_id") != (menu_ids["energy-0.png"] if phase == 0 else menu_ids["completed-recipe.png"]):
                raise SmokeFailure("Frost capture menu identity changed")
            frame_path = game / "freezer-reference-screenshots" / capture["name"]
            if not frame_path.is_file() or frame_path.is_symlink():
                raise SmokeFailure("Missing frost framebuffer")
            data = frame_path.read_bytes()
            if len(data) < 33 or data[:16] != b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR" or hashlib.sha256(data).hexdigest() != capture.get("sha256") or struct.unpack(">II", data[16:24]) != (width, height):
                raise SmokeFailure("Frost framebuffer bytes/dimensions differ")
        elif phase != 0 or ticks != 0:
            raise SmokeFailure("Idle/completed frost must reflect server reset")
        scale = min(1, (min(width, height) - 12) / 314)
        if (type(capture.get("view_scale")) not in (int, float) or not math.isfinite(capture["view_scale"])
                or abs(capture["view_scale"] - scale) > 0.00001):
            raise SmokeFailure("Frost viewport differs")
        ox, oy = (width - 314 * scale) / 2, (height - 314 * scale) / 2
        fw = math.ceil(ox + 795 * 314 / 1254 * scale) - math.floor(ox + 460 * 314 / 1254 * scale)
        fh = math.ceil(oy + 555 * 314 / 1254 * scale) - math.floor(oy + 390 * 314 / 1254 * scale)
        name = capture["name"][:-4] + "-frost.png"
        path = game / "freezer-reference-screenshots" / name
        if capture.get("frost_name") != name or not path.is_file() or path.is_symlink():
            raise SmokeFailure("Missing frost crop")
        data = path.read_bytes()
        if (len(data) < 33 or data[:16] != b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR"
                or struct.unpack(">II", data[16:24]) != (fw, fh)
                or capture.get("frost_width") != fw or capture.get("frost_height") != fh
                or hashlib.sha256(data).hexdigest() != capture.get("frost_sha256")):
            raise SmokeFailure("Frost crop hash or geometry differs")
        if natural:
            frost_hashes.add(capture["frost_sha256"])
    if len(frost_hashes) != 4:
        raise SmokeFailure("Natural frost crops do not show distinct phases")
    zero_hash = next(c["frost_sha256"] for c in frost_captures if c["phase"] == 0)
    if any(c["frost_sha256"] != zero_hash for c in captures):
        raise SmokeFailure("Idle/completed chamber differs from phase zero")
    if f"IEDCTR_FREEZER_REFERENCE PASSED nonce={nonce}" not in logs:
        raise SmokeFailure("Missing current-run Freezer completion marker")
    return proof


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--language", choices=("en_us", "fr_fr"), default="en_us")
    parser.add_argument("--height", type=int, choices=(260, 720), default=720)
    parser.add_argument("--prepare-timeout", type=int, default=600)
    parser.add_argument("--timeout", type=int, default=600)
    args = parser.parse_args()
    if sys.platform != "linux" or min(args.prepare_timeout, args.timeout) <= 0:
        parser.error("Requires Linux and positive bounded timeouts")
    project = args.project.resolve()
    # No uncommitted candidate may borrow the passing status of its base commit.
    clean = subprocess.run(["git", "status", "--porcelain", "--untracked-files=all"], cwd=project,
                           check=True, capture_output=True, text=True)
    if clean.stdout.strip():
        parser.error("Requires a clean committed candidate; base SHA alone cannot identify uncommitted code")
    source_sha = subprocess.run(["git", "rev-parse", "HEAD"], cwd=project, check=True,
                                capture_output=True, text=True).stdout.strip()
    if not re.fullmatch(r"[a-f0-9]{40}", source_sha):
        parser.error("Missing exact source SHA")
    art = project / "src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/freezer_reference_v5/background.png"
    if hashlib.sha256(art.read_bytes()).hexdigest() != ART_SHA:
        parser.error("Retained Freezer V5 art changed")
    missing = [name for name in ("java", "Xvfb", "xvfb-run", "xauth") if not shutil.which(name)]
    if missing:
        parser.error("Missing native prerequisites: " + ", ".join(missing))
    (project / "build").mkdir(exist_ok=True)
    evidence = Path(tempfile.mkdtemp(prefix="freezer-reference-native-", dir=project / "build"))
    game = evidence / "run"
    game.mkdir()
    nonce = secrets.token_hex(16)
    (game / ".iedctr-client-gameplay-nonce").write_text(nonce, encoding="utf-8")
    (game / "options.txt").write_text(
        "onboardAccessibility:false\ntutorialStep:none\nrenderDistance:2\nsimulationDistance:2\n"
        "maxFps:60\npauseOnLostFocus:false\nsoundCategory_master:0.0\nguiScale:1\n"
        f"lang:{args.language}\n", encoding="utf-8")
    init = evidence / "freezer.init.gradle"
    init.write_text("""gradle.projectsEvaluated {
    rootProject.extensions.getByName('neoForge').runs.named('client') {
        gameDirectory.set(rootProject.file(System.getenv('IEDCTR_GAMEPLAY_DIR')))
        systemProperty 'iedctr.clientGameplay', 'true'
        systemProperty 'iedctr.clientGameplayNonce', System.getenv('IEDCTR_GAMEPLAY_NONCE')
        systemProperty 'iedctr.clientGameplayDirectory', System.getenv('IEDCTR_GAMEPLAY_DIR')
        systemProperty 'iedctr.freezerReferenceOnly', 'true'
        systemProperty 'iedctr.dev.freezerV5LiveEnergy', 'true'
        systemProperty 'iedctr.dev.freezerV5ExactReference', 'false'
        systemProperty 'iedctr.executedSourceSha', System.getenv('IEDCTR_EXECUTED_SOURCE_SHA')
        programArgument '--width'
        programArgument '1280'
        programArgument '--height'
        programArgument System.getenv('IEDCTR_SCREEN_HEIGHT')
    }
}
""", encoding="utf-8")
    env = dict(os.environ, IEDCTR_GAMEPLAY_DIR=str(game), IEDCTR_GAMEPLAY_NONCE=nonce,
               IEDCTR_EXECUTED_SOURCE_SHA=source_sha, IEDCTR_SCREEN_HEIGHT=str(args.height),
               LIBGL_ALWAYS_SOFTWARE="1", ALSOFT_DRIVERS="null")
    gradle = ["bash", str(project / "gradlew"), "--no-daemon", "--init-script", str(init), "--stacktrace"]
    print(f"Freezer isolated native evidence: {evidence}", flush=True)
    try:
        code, expired = run_bounded(gradle + ["prepareClientRun"], project, env,
                                    evidence / "prepare.log", args.prepare_timeout)
        if expired or code != 0:
            raise SmokeFailure(f"Freezer client preparation failed (exit={code}, timeout={expired})")
        log = evidence / "client.log"
        code, expired = run_bounded(["xvfb-run", "-a", "-s", f"-screen 0 1280x{args.height}x24"]
                                    + gradle + ["runClient"], project, env, log, args.timeout)
        proof = json.loads((game / "client-freezer-reference-result.json").read_text())
        main_proof = json.loads((game / "client-gameplay-result.json").read_text())
        logs = log.with_suffix(".observed.log").read_text(errors="replace")
        for name in ("latest.log", "debug.log"):
            runtime = game / "logs" / name
            if runtime.is_file():
                logs += "\n" + runtime.read_text(errors="replace")
        final_status = subprocess.run(["git", "status", "--porcelain", "--untracked-files=all"], cwd=project,
                                      check=True, capture_output=True, text=True).stdout.strip()
        final_sha = subprocess.run(["git", "rev-parse", "HEAD"], cwd=project, check=True,
                                   capture_output=True, text=True).stdout.strip()
        if final_status or final_sha != source_sha:
            raise SmokeFailure("Candidate changed during native execution; source identity is not exact")
        validate_proof(proof, main_proof, nonce, source_sha, game, logs, code, expired, 1280, args.height, args.language)
        print("PASS bounded native Freezer smoke. Chamber frost follows server phases; decorative rim remains static; full visual/input acceptance is not claimed.")
        return 0
    except (SmokeFailure, OSError, ValueError) as error:
        print(f"FAIL: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
