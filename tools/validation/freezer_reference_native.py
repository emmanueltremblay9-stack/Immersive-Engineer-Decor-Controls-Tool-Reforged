#!/usr/bin/env python3
"""Run the bounded Thermal Vertical Freezer smoke in a fresh isolated Linux client.

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
ART_SHA = "29ea3ff661c6591a3b89ee97d3a33d8817f437745926eb566338cbb22bcd8dc6"
RETAINED_ARTS = {
    "neutral": "48d9d8a9eea6ffb477453bc7b84516b9f03be20132bff3b873d4d7b66e68b806",
    "effect": "046988bf2783a5396c43c6cbe949a32b3088a4c6cc9f59be0347fdd9611af957",
    "energy": "73403246b8e9389955a9cb5ff23ce8f478cbdb06af633c0feda7445dac355638",
}
SCREEN_CLASS = "com.oblixorprime.engineersdecorreforged.client.ThermalVerticalScreen"
CANVAS_WIDTH, CANVAS_HEIGHT, MAX_VIEW_SCALE, VIEW_MARGIN = 281, 313, 0.78, 6
ENERGY_CROP = (59, 88, 71, 140)
EFFECT_CROP = (102, 92, 177, 125)
ENERGY_CLIP_TOP = {0: 136, 8000: 114, 16000: 92}
CASES = {
    "pickup_from_hotbar", "right_click_places_one_input", "output_rejects_insertion",
    "return_carried_stack", "full_inventory_preserves_reserved",
    "recovery_preserves_count_and_components", "repeated_recovery_no_duplicate",
    "close_reaches_server", "reopen_preserves_inventory_and_geometry", "real_recipe_consumes_1440_fe", "natural_frost_phases_0_to_3_and_completion_reset",
}
CAPTURES = {"energy-0.png": 0, "energy-8000.png": 8000,
            "energy-16000.png": 16000, "completed-recipe.png": 0}


def _expected_scale(width: int, height: int) -> float:
    short_edge = min(width, height)
    margin = min(VIEW_MARGIN, (short_edge - 1) / 2)
    return min(1.0, MAX_VIEW_SCALE, (width - 2 * margin) / CANVAS_WIDTH,
               (height - 2 * margin) / CANVAS_HEIGHT)


def _crop_dimensions(width: int, height: int, scale: float, rect: tuple[float, float, float, float]) -> tuple[int, int]:
    left, top, right, bottom = rect
    origin_x = (width - CANVAS_WIDTH * scale) / 2
    origin_y = (height - CANVAS_HEIGHT * scale) / 2
    x0, y0 = math.floor(origin_x + left * scale), math.floor(origin_y + top * scale)
    x1, y1 = math.ceil(origin_x + right * scale), math.ceil(origin_y + bottom * scale)
    return x1 - x0, y1 - y0


def _validate_crop(capture: dict, suffix: str, rect: tuple[float, float, float, float],
                   game: Path, width: int, height: int, scale: float) -> str:
    name = capture.get("name", "")[:-4] + "-" + suffix
    path = game / "freezer-reference-screenshots" / name
    expected_width, expected_height = _crop_dimensions(width, height, scale, rect)
    if capture.get(suffix.removesuffix(".png") + "_crop_name") != name or not path.is_file() or path.is_symlink():
        raise SmokeFailure(f"Missing native Freezer {suffix.removesuffix('.png')} crop")
    data = path.read_bytes()
    if (len(data) < 33 or data[:16] != b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR"
            or struct.unpack(">II", data[16:24]) != (expected_width, expected_height)
            or capture.get(suffix.removesuffix(".png") + "_crop_width") != expected_width
            or capture.get(suffix.removesuffix(".png") + "_crop_height") != expected_height
            or capture.get(suffix.removesuffix(".png") + "_crop_sha256") != hashlib.sha256(data).hexdigest()):
        raise SmokeFailure(f"Native Freezer {suffix.removesuffix('.png')} crop bytes/geometry differ")
    return hashlib.sha256(data).hexdigest()


def load_run_proofs(game: Path) -> tuple[dict, dict]:
    """Read gameplay status before requiring the success-only Freezer proof."""
    main_path = game / "client-gameplay-result.json"
    if not main_path.is_file():
        raise SmokeFailure("Native client gameplay result is missing; cannot identify failed stage")
    try:
        main = json.loads(main_path.read_text())
    except (OSError, json.JSONDecodeError) as error:
        raise SmokeFailure(f"Native client gameplay result is unreadable: {error}") from error
    if not isinstance(main, dict):
        raise SmokeFailure("Native client gameplay result is not a JSON object")
    if main.get("status") != "passed":
        phase = main.get("phase") or "UNKNOWN"
        detail = main.get("error") or "no failure detail recorded"
        raise SmokeFailure(f"Native client gameplay failed at {phase}: {detail}")

    proof_path = game / "client-freezer-reference-result.json"
    if not proof_path.is_file():
        raise SmokeFailure("Freezer success proof is missing after gameplay status passed")
    try:
        proof = json.loads(proof_path.read_text())
    except (OSError, json.JSONDecodeError) as error:
        raise SmokeFailure(f"Freezer success proof is unreadable after gameplay status passed: {error}") from error
    if not isinstance(proof, dict):
        raise SmokeFailure("Freezer success proof is not a JSON object")
    return main, proof


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
            or proof.get("screen_class") != SCREEN_CLASS
            or proof.get("source_art_sha256") != ART_SHA
            or "effect_art_sha256" in proof or "energy_art_sha256" in proof
            or type(proof.get("canvas_width")) is not int or proof["canvas_width"] != CANVAS_WIDTH
            or type(proof.get("canvas_height")) is not int or proof["canvas_height"] != CANVAS_HEIGHT
            or type(proof.get("visible_machine_slots")) is not int or proof["visible_machine_slots"] != 2
            or type(proof.get("visible_slot_count")) is not int or proof["visible_slot_count"] != 38
            or type(proof.get("reserved_menu_index")) is not int or proof["reserved_menu_index"] != 1
            or type(proof.get("menu_slot_count")) is not int or proof["menu_slot_count"] != 39
            or proof.get("actual_integrated_world") is not True
            or proof.get("presentation_mode") != "normal-dynamic"
            or proof.get("dev_selectors_absent") is not True
            or proof.get("frost_art_static") is not False
            or proof.get("effect_visibility_rule") != "server_phase_greater_than_zero_v5c_chamber_pulse"
            or proof.get("phase_source") != "server_menu_phase_natural_recipe"
            or proof.get("phase4_native_observed") is not False
            or proof.get("native_visual_acceptance") is not False):
        raise SmokeFailure("Freezer proof identity, ThermalVerticalScreen scope, art or slot contract differs")
    cases = proof.get("cases")
    if not isinstance(cases, list) or any(not isinstance(case, str) for case in cases) or len(cases) != len(CASES) or set(cases) != CASES:
        raise SmokeFailure("Incomplete or duplicate Freezer interaction cases")
    captures = proof.get("captures")
    if not isinstance(captures, list) or len(captures) != len(CAPTURES):
        raise SmokeFailure("Incomplete native Freezer capture matrix")
    expected_scale = _expected_scale(width, height)
    seen, menu_ids, energy_hashes = set(), {}, {}
    for capture in captures:
        if not isinstance(capture, dict):
            raise SmokeFailure("Invalid Freezer capture")
        name = capture.get("name")
        if name not in CAPTURES or name in seen:
            raise SmokeFailure("Unexpected, duplicate or unsafe capture name")
        seen.add(name)
        energy = CAPTURES[name]
        clip_top = ENERGY_CLIP_TOP[energy]
        if (type(capture.get("energy")) is not int or capture["energy"] != energy
                or type(capture.get("energy_capacity")) is not int or capture["energy_capacity"] != 16000
                or type(capture.get("energy_clip_top")) is not int or capture["energy_clip_top"] != clip_top
                or capture.get("screen_class") != SCREEN_CLASS
                or type(capture.get("menu_id")) is not int or capture["menu_id"] <= 0
                or type(capture.get("view_scale")) not in (int, float)
                or not math.isfinite(capture["view_scale"])
                or abs(capture["view_scale"] - expected_scale) > 0.00001):
            raise SmokeFailure("Native Freezer capture has incorrect live menu/canvas state")
        path = game / "freezer-reference-screenshots" / name
        if not path.is_file() or path.is_symlink():
            raise SmokeFailure("Missing native framebuffer PNG")
        data = path.read_bytes()
        if (len(data) < 33 or data[:16] != b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR"
                or struct.unpack(">II", data[16:24]) != (width, height)
                or capture.get("width") != width or capture.get("height") != height
                or capture.get("sha256") != hashlib.sha256(data).hexdigest()):
            raise SmokeFailure("Native Freezer PNG bytes or dimensions differ from proof")
        energy_hashes[name] = _validate_crop(capture, "energy.png", ENERGY_CROP, game, width, height, expected_scale)
        _validate_crop(capture, "effect.png", EFFECT_CROP, game, width, height, expected_scale)
        menu_ids[name] = capture["menu_id"]
    if len({menu_ids[name] for name in CAPTURES if name.startswith("energy-")}) != 1:
        raise SmokeFailure("Energy states were captured from different menus")
    if menu_ids["completed-recipe.png"] == menu_ids["energy-0.png"]:
        raise SmokeFailure("Reopened Freezer did not get a new menu identity")
    if len({energy_hashes[name] for name in ("energy-0.png", "energy-8000.png", "energy-16000.png")}) != 3:
        raise SmokeFailure("Native FE crop did not change across 0, half and full capacity")
    if energy_hashes["completed-recipe.png"] != energy_hashes["energy-0.png"]:
        raise SmokeFailure("FE crop did not return to zero after recipe completion")

    frost_captures = proof.get("frost_captures")
    if not isinstance(frost_captures, list) or len(frost_captures) != 4:
        raise SmokeFailure("Missing natural frost phase captures")
    seen_phases, effect_hashes = set(), {}
    for capture in frost_captures + captures:
        phase, ticks = capture.get("phase"), capture.get("progress_ticks")
        if type(phase) is not int or type(ticks) is not int:
            raise SmokeFailure("Invalid synchronized frost state")
        natural = capture in frost_captures
        if natural:
            if (type(capture.get("rendered_frames")) is not int or capture["rendered_frames"] < 3
                    or capture.get("capture_event") != "ScreenEvent.Render.Post"):
                raise SmokeFailure("Frost capture lacks coherent rendered frames")
            if phase not in range(4) or phase in seen_phases or capture.get("name") != f"frost-phase-{phase}.png":
                raise SmokeFailure("Invalid or duplicate natural phase")
            if not (ticks == 0 if phase == 0 else phase * 45 <= ticks < (phase + 1) * 45):
                raise SmokeFailure("Natural phase/progress mismatch")
            if capture.get("width") != width or capture.get("height") != height:
                raise SmokeFailure(
                    f"Natural frost frame dimensions differ at phase={phase}, progress={ticks}: "
                    f"expected {width}x{height}, got {capture.get('width')}x{capture.get('height')}"
                )
            if type(capture.get("energy")) is not int:
                raise SmokeFailure(f"Natural frost FE is not an integer at phase={phase}, progress={ticks}")
            expected_energy = 0 if phase == 0 else 1440 - 8 * ticks
            if capture["energy"] != expected_energy:
                raise SmokeFailure(
                    f"Natural frost FE mismatch at phase={phase}, progress={ticks}: "
                    f"expected {expected_energy} FE from the 1440 FE recipe, got {capture['energy']} FE"
                )
            natural_energy = capture["energy"]
            natural_clip_top = 136 - (44 * min(16000, max(0, natural_energy))) // 16000
            if capture.get("energy_capacity") != 16000 or capture.get("energy_clip_top") != natural_clip_top:
                raise SmokeFailure("Natural frost frame energy scissor differs from synchronized FE")
            seen_phases.add(phase)
            expected_menu_id = menu_ids["energy-0.png"] if phase == 0 else menu_ids["completed-recipe.png"]
            if capture.get("screen_class") != SCREEN_CLASS or capture.get("menu_id") != expected_menu_id:
                raise SmokeFailure("Frost capture menu identity changed")
            frame_path = game / "freezer-reference-screenshots" / capture["name"]
            if not frame_path.is_file() or frame_path.is_symlink():
                raise SmokeFailure("Missing frost framebuffer")
            data = frame_path.read_bytes()
            if (len(data) < 33 or data[:16] != b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR"
                    or hashlib.sha256(data).hexdigest() != capture.get("sha256")
                    or struct.unpack(">II", data[16:24]) != (width, height)):
                raise SmokeFailure("Frost framebuffer bytes/dimensions differ")
        elif phase != 0 or ticks != 0:
            raise SmokeFailure("Idle/completed frost must reflect server reset")
        scale = capture.get("view_scale")
        if (type(scale) not in (int, float) or not math.isfinite(scale)
                or abs(scale - expected_scale) > 0.00001):
            raise SmokeFailure("Frost viewport differs")
        effect_hashes[capture["name"]] = _validate_crop(capture, "effect.png", EFFECT_CROP, game, width, height, expected_scale)
        _validate_crop(capture, "energy.png", ENERGY_CROP, game, width, height, expected_scale)
    if {c.get("phase") for c in frost_captures} != {0, 1, 2, 3}:
        raise SmokeFailure("Natural frost capture set is incomplete")
    active_hashes = {effect_hashes[f"frost-phase-{phase}.png"] for phase in (1, 2, 3)}
    zero_hash = effect_hashes["frost-phase-0.png"]
    # The selected chamber pulse can vary between frames; every synchronized
    # active phase must still visibly differ from the same idle chamber crop.
    if zero_hash in active_hashes:
        raise SmokeFailure("An active server phase did not reveal the frost effect")
    if any(effect_hashes[capture["name"]] != zero_hash for capture in captures):
        raise SmokeFailure("Idle/completed Freezer effect did not return to phase zero")
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
    art_root = project / "src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui"
    retained = {"thermal_vertical/freezer/" + name + ".png": digest for name, digest in RETAINED_ARTS.items()}
    for name, expected in {**retained, "gui11_v5c/05.png": ART_SHA}.items():
        path = art_root / name
        if hashlib.sha256(path.read_bytes()).hexdigest() != expected:
            parser.error(f"Active Thermal Freezer {name} art changed")
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
        main_proof, proof = load_run_proofs(game)
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
        print("PASS bounded native Thermal Vertical Freezer smoke. Frost overlay follows synchronized server phases; reserved slot recovery and exact FE states were exercised; full visual/input acceptance is not claimed.")
        return 0
    except (SmokeFailure, OSError, ValueError) as error:
        print(f"FAIL: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
