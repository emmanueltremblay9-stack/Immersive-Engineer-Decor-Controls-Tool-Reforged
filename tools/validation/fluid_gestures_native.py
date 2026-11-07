#!/usr/bin/env python3
"""Bounded XTest Funnel/Accumulator proof; requires a clean exact-source Linux candidate."""
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
import subprocess
import sys
import tempfile
from client_mixin_smoke import FATAL_PATTERN, SmokeFailure, run_bounded
from factory_placer_v2_reference_native import png, strict_equal

# Runs inside xvfb-run, before Java: this newly allocated display owns the nonce.
XVFB_BIND = """import os, pathlib, sys
p = pathlib.Path(os.environ['IEDCTR_GAMEPLAY_DIR']) / '.iedctr-fluid-xvfb'
with p.open('x', encoding='utf-8') as f:
    f.write(os.environ['IEDCTR_GAMEPLAY_NONCE']+'\\n'+os.environ['DISPLAY'])
os.execvpe(sys.argv[1],sys.argv[1:],os.environ)
"""
SUITE = 'native-fluid-gestures-v1'
ACTIONS = ['pickup','insert','pickup_input','return','shift_insert','shift_return','process_insert','output_pickup','output_return']
STATES = [[0,0,0,0,1,0,0],[1,0,0,0,0,0,0],[0,0,0,0,1,0,0],[0,0,1,0,0,0,0],
          [1,0,0,0,0,0,0],[0,0,1,0,0,0,0],[0,1,0,0,0,0,0],[0,0,0,0,0,1,0],[0,0,0,1,0,0,0]]
ROLES = ['header','tank','empty_input','empty_output','structure','occupied_input']

def require(ok, message):
    if not ok: raise SmokeFailure(message)


def validate_evidence(proof, nonce, source_sha, width, height, language):
    require(type(proof) is dict, 'Proof is not an object')
    require(proof.get('suite') == SUITE and proof.get('nonce') == nonce
            and re.fullmatch('[a-f0-9]{32}', nonce) and re.fullmatch('[a-f0-9]{40}', source_sha)
            and proof.get('executed_source_sha') == source_sha, 'Wrong run/source identity')
    require(proof.get('status') == 'passed_bounded_native' and 'error' not in proof
            and proof.get('actual_integrated_world') is True and proof.get('input_backend') == 'XTEST'
            and proof.get('cleanup_released') is True and proof.get('selected_language') == language,
            'Missing native isolation, input or completion proof')
    h, g = proof.get('hovers'), proof.get('gestures')
    require(type(h) is list and len(h) == 12 and type(g) is list and len(g) == 18, 'Incomplete case matrix')
    seeds = proof.get('process_seeds')
    require(type(seeds) is list and len(seeds) == 2, 'Missing water processing seeds')
    for seed in seeds:
        require(type(seed) is dict and type(seed.get('client')) is dict and strict_equal(seed.get('client'),seed.get('server'))
                and strict_equal(seed['client'].get('state'),[0,0,1,0,0,0,1000]) and type(seed['client'].get('total')) is int and seed['client']['total'] == 1, 'Unproven water seed')
    names = set()
    menu_ids = set()
    for i, machine in enumerate(['funnel','accumulator']):
        menu_id = None
        for j, row in enumerate(g[i*9:i*9+9]):
            require(type(row) is dict and row.get('action') == ACTIONS[j] and row.get('machine') == machine, 'Wrong gesture order')
            client, server = row.get('client'), row.get('server')
            require(type(client) is dict and strict_equal(client, server), 'Missing authoritative agreement')
            require(strict_equal(client.get('state'), STATES[j]) and type(client.get('total')) is int and client['total'] == 1, 'Stack/carried/fluid conservation failed')
            require(type(client.get('menu_id')) is int and client['menu_id'] > 0, 'Missing menu context')
            if menu_id is None: menu_id = client['menu_id']
            require(client['menu_id'] == menu_id and row.get('menu_id') == menu_id, 'Menu context changed')
            require(seeds[i]['client'].get('menu_id') == menu_id, 'Cross-menu processing seed')
            require(row.get('released') is True, 'Held native input')
            require(row.get('native_press_observed') is True and row.get('native_release_observed') is True
                    and row.get('native_shift_observed') is (j in (4,5,6)), 'Missing native event delivery observation')
            require(type(row.get('button_events')) is int and row['button_events'] == (i*9+j+1)*2, 'Missing native press/release delivery')
            require(type(row.get('key_events')) is int and row['key_events'] == i*6 + (0 if j<4 else 2 if j==4 else 4 if j==5 else 6), 'Missing native Shift delivery')
            require(row.get('name') == machine+'-'+ACTIONS[j]+'.png', 'Wrong gesture capture')
            slot = row.get('menu_slot')
            require(type(slot) is int and 0 <= slot < 38, 'Missing target slot')
            if j in (0,3,8): require(slot == 2, 'Wrong ordinary player slot')
            elif j in (1,2,5): require(slot == 0, 'Wrong input slot')
            elif j == 7: require(slot == 1, 'Wrong output slot')
            else: require(slot >= 2, 'Shift insertion is not from player inventory')
            if slot < 2: x,y = (40 if slot == 0 else 200), (70 if i == 0 else 100)
            elif slot < 29: x,y = [44,63,82,101,120,139,158,177,196][(slot-2)%9], [138,156,175][(slot-2)//9]
            else: x,y = [44,63,82,101,120,139,158,177,196][slot-29],206
            require((row.get('target_x'),row.get('target_y')) == ((width-256)//2+x+8,(height-256)//2+y+8), 'Wrong gesture coordinates')
        require(menu_id not in menu_ids, 'Machine menu identities were reused')
        menu_ids.add(menu_id)
        for j, row in enumerate(h[i*6:i*6+6]):
            require(type(row) is dict and row.get('role') == ROLES[j] and row.get('machine') == machine, 'Wrong hover order')
            require(row.get('menu_id') == menu_id and type(row.get('client')) is dict
                    and strict_equal(row['client'],row.get('server')) and row['client'].get('menu_id') == menu_id
                    and strict_equal(row['client'].get('state'),[1,0,0,0,0,0,0] if j==5 else [0,0,1,0,0,0,0])
                    and type(row['client'].get('total')) is int and row['client']['total'] == 1, 'Wrong hover menu/item context')
            require(row.get('tooltip_event_class') == 'RenderTooltipEvent.Color+ScreenEvent.Render.Post', 'Missing tooltip rendering path identity')
            require(type(row.get('tooltip_render_events')) is int and row['tooltip_render_events'] > 0, 'Missing rendered tooltip event')
            require(row.get('name') == machine+'-hover-'+str(j)+'.png', 'Wrong hover capture')
            local = [(128,14),(128,102) if i==0 else (128,79),(48,78) if i==0 else (48,108),
                     (208,78) if i==0 else (208,108),(126,123) if i==0 else (128,35),(48,78) if i==0 else (48,108)][j]
            require((row.get('target_x'),row.get('target_y')) == ((width-256)//2+local[0],(height-256)//2+local[1]), 'Wrong hover coordinates')
    for row in h+g:
        expected_screen = 'SmallFluidFunnelScreen' if row['machine']=='funnel' else 'PassiveFluidAccumulatorScreen'
        require(row.get('screen_class') == 'com.oblixorprime.engineersdecorreforged.client.MachineScreen$'+expected_screen, 'Wrong registered screen')
        require(row['name'] not in names, 'Duplicate capture'); names.add(row['name'])
        require(type(row.get('xtest_motion_count')) is int and row['xtest_motion_count'] > 0, 'Missing native motion')
        require(type(row.get('width')) is int and row['width'] == width and type(row.get('height')) is int and row['height'] == height, 'Wrong framebuffer size')
        for axis, limit in [('x',width),('y',height)]:
            actual, target = row.get('actual_'+axis), row.get('target_'+axis)
            require(type(actual) in (int,float) and math.isfinite(actual) and type(target) is int
                    and 0 <= target < limit and abs(actual-target) < 1, 'Minecraft coordinate delivery failed')
        require(type(row.get('sha256')) is str and re.fullmatch('[a-f0-9]{64}',row['sha256']), 'Missing capture digest')
    return proof


def validate_proof(proof,nonce,source_sha,game,logs,exit_code,timed_out,width,height,language):
    require(not timed_out and type(exit_code) is int and exit_code == 0 and not FATAL_PATTERN.search(logs), 'Client failed or timed out')
    require(not list(game.glob('crash-reports/*')) and not list(game.parent.rglob('hs_err_pid*.log')), 'Crash report present')
    validate_evidence(proof,nonce,source_sha,width,height,language)
    folder = game/'fluid-gesture-screenshots'
    require(not folder.is_symlink(), 'Unsafe capture directory')
    for row in proof['hovers']+proof['gestures']:
        png(folder/row['name'], width,height,row['sha256'])
    require(f'IEDCTR_FLUID_GESTURES PASSED nonce={nonce}' in logs, 'Missing current-run completion marker')
    return proof

def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--language", choices=("en_us", "fr_fr"), default="en_us")
    parser.add_argument("--width", type=int, default=1280)
    parser.add_argument("--height", type=int, default=720)
    parser.add_argument("--gui-scale", type=int, choices=(1, 2, 3), default=1)
    parser.add_argument("--prepare-timeout", type=int, default=600)
    parser.add_argument("--timeout", type=int, default=600)
    args = parser.parse_args()
    if sys.platform != "linux" or min(args.prepare_timeout, args.timeout) <= 0:
        parser.error("Requires Linux and positive bounded timeouts")
    try:
        require(args.width >= 800 and args.height >= 600 and args.gui_scale == 1, "Bounded fixture requires at least 800x600 and GUI scale 1")
    except SmokeFailure as error:
        parser.error(str(error))
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
    missing = [name for name in ("java", "Xvfb", "xvfb-run", "xauth") if not shutil.which(name)]
    if missing:
        parser.error("Missing native prerequisites: " + ", ".join(missing))
    (project / "build").mkdir(exist_ok=True)
    evidence = Path(tempfile.mkdtemp(prefix="fluid-gestures-native-", dir=project / "build"))
    game = evidence / "run"
    game.mkdir()
    nonce = secrets.token_hex(16)
    (game / ".iedctr-client-gameplay-nonce").write_text(nonce, encoding="utf-8")
    (game / "options.txt").write_text(
        "onboardAccessibility:false\ntutorialStep:none\nrenderDistance:2\nsimulationDistance:2\n"
        f"maxFps:60\npauseOnLostFocus:false\nsoundCategory_master:0.0\nguiScale:{args.gui_scale}\n"
        f"lang:{args.language}\n", encoding="utf-8")
    init = evidence / "fluid_gesture.init.gradle"
    init.write_text("""gradle.projectsEvaluated {
    rootProject.extensions.getByName('neoForge').runs.named('client') {
        gameDirectory.set(rootProject.file(System.getenv('IEDCTR_GAMEPLAY_DIR')))
        systemProperty 'iedctr.clientGameplay', 'false'
        systemProperty 'iedctr.clientGameplayNonce', System.getenv('IEDCTR_GAMEPLAY_NONCE')
        systemProperty 'iedctr.clientGameplayDirectory', System.getenv('IEDCTR_GAMEPLAY_DIR')
        systemProperty 'iedctr.fluidGestureNative', 'true'
        systemProperty 'iedctr.executedSourceSha', System.getenv('IEDCTR_EXECUTED_SOURCE_SHA')
        systemProperty 'iedctr.requestedGuiScale', System.getenv('IEDCTR_GUI_SCALE')
        systemProperty 'iedctr.requestedWidth', System.getenv('IEDCTR_SCREEN_WIDTH')
        systemProperty 'iedctr.requestedHeight', System.getenv('IEDCTR_SCREEN_HEIGHT')
        programArgument '--width'
        programArgument System.getenv('IEDCTR_SCREEN_WIDTH')
        programArgument '--height'
        programArgument System.getenv('IEDCTR_SCREEN_HEIGHT')
    }
}
""", encoding="utf-8")
    env = dict(os.environ, IEDCTR_GAMEPLAY_DIR=str(game), IEDCTR_GAMEPLAY_NONCE=nonce,
               IEDCTR_EXECUTED_SOURCE_SHA=source_sha, IEDCTR_SCREEN_WIDTH=str(args.width),
               IEDCTR_SCREEN_HEIGHT=str(args.height), IEDCTR_GUI_SCALE=str(args.gui_scale),
               LIBGL_ALWAYS_SOFTWARE="1", ALSOFT_DRIVERS="null")
    gradle = ["bash", str(project / "gradlew"), "--no-daemon", "--init-script", str(init), "--stacktrace", "-Pinclude_fluid_gesture_native_validation=true"]
    print(f"Funnel/Accumulator gestures isolated native evidence: {evidence} ({args.width}x{args.height}, GUI scale {args.gui_scale}, {args.language})", flush=True)
    try:
        code, expired = run_bounded(gradle + ["prepareClientRun"], project, env,
                                    evidence / "prepare.log", args.prepare_timeout)
        if expired or code != 0:
            raise SmokeFailure(f"Funnel/Accumulator gestures client preparation failed (exit={code}, timeout={expired})")
        log = evidence / "client.log"
        code, expired = run_bounded(["xvfb-run", "-a", "-s", f"-screen 0 {args.width}x{args.height}x24"]
                                    + [sys.executable, "-c", XVFB_BIND] + gradle + ["runClient"], project, env, log, args.timeout)
        proof = json.loads((game / "client-fluid-gestures-result.json").read_text())
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
        validate_proof(proof, nonce, source_sha, game, logs, code, expired,
                       args.width, args.height, args.language)
        print("PASS bounded native Funnel/Accumulator gestures smoke. Full visual acceptance is not claimed.")
        return 0
    except (SmokeFailure, OSError, ValueError) as error:
        print(f"FAIL: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
