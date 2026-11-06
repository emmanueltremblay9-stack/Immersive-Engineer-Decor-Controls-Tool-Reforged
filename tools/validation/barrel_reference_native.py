#!/usr/bin/env python3
"""Bounded native Barrel V5 validation in a fresh nonce-owned integrated world."""
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

SUITE = "native-barrel-reference-v1"
ART_SHA = "df89575afd98ccee396b38cee34f230ef03f94528d5a4ad5bc3425658ef54ca9"
SCREEN_CLASS = "com.oblixorprime.engineersdecorreforged.client.BarrelReferenceScreen"
CASES = {"all_38_pickup_return_server_confirmed", "native_empty_water_lava_milk_half_full",
         "close_reaches_server", "reopen_preserves_tank_and_geometry"}
CAPTURES = {"empty.png": 0, **{f"{fluid}-{state}.png": fraction for fluid in ("water", "lava", "milk")
                             for state, fraction in (("half", 1), ("full", 2))}}


def png(path: Path, width: int, height: int, expected_hash: str) -> None:
    if not path.is_file() or path.is_symlink():
        raise SmokeFailure("Missing native PNG or unsafe symlink")
    data = path.read_bytes()
    if (len(data) < 33 or data[:16] != b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR"
            or struct.unpack(">II", data[16:24]) != (width, height)
            or hashlib.sha256(data).hexdigest() != expected_hash):
        raise SmokeFailure("Native PNG hash/dimensions differ from evidence")


def validate_proof(proof: dict, nonce: str, source_sha: str, game: Path,
                   logs: str, exit_code: int | None, timed_out: bool,
                   width: int, height: int, language: str) -> dict:
    if timed_out or exit_code != 0 or FATAL_PATTERN.search(logs):
        raise SmokeFailure("Native client did not finish cleanly")
    if list(game.glob("crash-reports/*")) or list(game.parent.rglob("hs_err_pid*.log")):
        raise SmokeFailure("Native client wrote a crash report")
    if (proof.get("suite") != SUITE or proof.get("nonce") != nonce
            or proof.get("status") != "passed_bounded_smoke"
            or proof.get("executed_source_sha") != source_sha
            or not re.fullmatch(r"[a-f0-9]{40}", source_sha)
            or proof.get("source_art_sha256") != ART_SHA
            or proof.get("actual_integrated_world") is not True
            or proof.get("native_visual_acceptance") is not False
            or type(proof.get("menu_slot_count")) is not int or proof["menu_slot_count"] != 38
            or language not in ("en_us", "fr_fr") or proof.get("selected_language") != language):
        raise SmokeFailure("Barrel proof identity, locale, bounded scope or slot contract differs")
    routes = proof.get("slot_routes")
    if not isinstance(routes, list) or any(type(i) is not int for i in routes) or routes != list(range(38)):
        raise SmokeFailure("All 38 real slot routes must pass exactly once in order")
    cases = proof.get("cases")
    if not isinstance(cases, list) or any(not isinstance(c, str) for c in cases) or len(cases) != len(CASES) or set(cases) != CASES:
        raise SmokeFailure("Missing or duplicate native cases")
    captures = proof.get("captures")
    if not isinstance(captures, list) or len(captures) != len(CAPTURES):
        raise SmokeFailure("Incomplete native capture matrix")
    seen, menu_ids, hashes, ring_hashes, capacities = set(), set(), {}, set(), set()
    scale = min(1, (width - 12) / 379, (height - 12) / 461)
    ox, oy = (width - 379 * scale) / 2, (height - 461 * scale) / 2
    tw = math.floor(ox + 211 * scale) - math.ceil(ox + 170 * scale)
    th = math.floor(oy + 193 * scale) - math.ceil(oy + 115 * scale)
    for capture in captures:
        if not isinstance(capture, dict):
            raise SmokeFailure("Invalid capture object")
        name = capture.get("name")
        if name not in CAPTURES or name in seen:
            raise SmokeFailure("Duplicate, unexpected or unsafe capture name")
        seen.add(name)
        capacity = capture.get("capacity")
        if (type(capacity) is not int or capacity <= 0
                or type(capture.get("amount")) is not int
                or capture["amount"] != capacity * CAPTURES[name] // 2
                or capture.get("screen_class") != SCREEN_CLASS
                or type(capture.get("menu_id")) is not int or capture["menu_id"] <= 0
                or type(capture.get("view_scale")) not in (int, float)
                or not math.isfinite(capture["view_scale"])
                or abs(capture["view_scale"] - scale) > .00001
                or type(capture.get("width")) is not int or capture["width"] != width
                or type(capture.get("height")) is not int or capture["height"] != height):
            raise SmokeFailure("Native capture state or transform differs")
        folder = game / "barrel-reference-screenshots"
        png(folder / name, width, height, capture.get("sha256"))
        tank_name = name[:-4] + "-tank.png"
        if (capture.get("tank_name") != tank_name
                or type(capture.get("tank_width")) is not int or capture["tank_width"] != tw
                or type(capture.get("tank_height")) is not int or capture["tank_height"] != th):
            raise SmokeFailure("Tank crop geometry differs")
        png(folder / tank_name, tw, th, capture.get("tank_sha256"))
        ring = capture.get("frame_ring_sha256")
        if not isinstance(ring, str) or not re.fullmatch(r"[a-f0-9]{64}", ring):
            raise SmokeFailure("Missing native frame ring digest")
        menu_ids.add(capture["menu_id"])
        capacities.add(capacity)
        ring_hashes.add(ring)
        hashes[name] = capture["tank_sha256"]
    if len(menu_ids) != 1 or len(capacities) != 1:
        raise SmokeFailure("Tank captures changed menu identity or capacity")
    if len(ring_hashes) != 1:
        raise SmokeFailure("Fluid states changed surrounding artwork pixels")
    for fluid in ("water", "lava", "milk"):
        if len({hashes["empty.png"], hashes[f"{fluid}-half.png"], hashes[f"{fluid}-full.png"]}) != 3:
            raise SmokeFailure("Native tank crops did not distinguish empty/half/full")
    if len({hashes[f"{fluid}-full.png"] for fluid in ("water", "lava", "milk")}) != 3:
        raise SmokeFailure("All fluids rendered identical full tank crops")
    if f"IEDCTR_BARREL_REFERENCE PASSED nonce={nonce}" not in logs:
        raise SmokeFailure("Missing current-run completion marker")
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
    art = project / "src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/barrel_reference_v5/background.png"
    if hashlib.sha256(art.read_bytes()).hexdigest() != ART_SHA:
        parser.error("Retained Barrel V5 art changed")
    missing = [name for name in ("java", "Xvfb", "xvfb-run", "xauth") if not shutil.which(name)]
    if missing:
        parser.error("Missing native prerequisites: " + ", ".join(missing))
    (project / "build").mkdir(exist_ok=True)
    evidence = Path(tempfile.mkdtemp(prefix="barrel-reference-native-", dir=project / "build"))
    game = evidence / "run"
    game.mkdir()
    nonce = secrets.token_hex(16)
    (game / ".iedctr-client-gameplay-nonce").write_text(nonce, encoding="utf-8")
    (game / "options.txt").write_text(
        "onboardAccessibility:false\ntutorialStep:none\nrenderDistance:2\nsimulationDistance:2\n"
        "maxFps:60\npauseOnLostFocus:false\nsoundCategory_master:0.0\nguiScale:1\n"
        f"lang:{args.language}\n", encoding="utf-8")
    init = evidence / "barrel.init.gradle"
    init.write_text("""gradle.projectsEvaluated {
    rootProject.extensions.getByName('neoForge').runs.named('client') {
        gameDirectory.set(rootProject.file(System.getenv('IEDCTR_GAMEPLAY_DIR')))
        systemProperty 'iedctr.clientGameplay', 'false'
        systemProperty 'iedctr.clientGameplayNonce', System.getenv('IEDCTR_GAMEPLAY_NONCE')
        systemProperty 'iedctr.clientGameplayDirectory', System.getenv('IEDCTR_GAMEPLAY_DIR')
        systemProperty 'iedctr.barrelReferenceNative', 'true'
        systemProperty 'iedctr.dev.barrelV5Reference', 'true'
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
    print(f"Barrel isolated native evidence: {evidence}", flush=True)
    try:
        code, expired = run_bounded(gradle + ["prepareClientRun"], project, env,
                                    evidence / "prepare.log", args.prepare_timeout)
        if expired or code != 0:
            raise SmokeFailure(f"Barrel client preparation failed (exit={code}, timeout={expired})")
        log = evidence / "client.log"
        code, expired = run_bounded(["xvfb-run", "-a", "-s", f"-screen 0 1280x{args.height}x24"]
                                    + gradle + ["runClient"], project, env, log, args.timeout)
        proof = json.loads((game / "client-barrel-reference-result.json").read_text())
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
        validate_proof(proof, nonce, source_sha, game, logs, code, expired, 1280, args.height, args.language)
        print("PASS bounded native Barrel smoke. Full visual/input acceptance is not claimed.")
        return 0
    except (SmokeFailure, OSError, ValueError) as error:
        print(f"FAIL: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
