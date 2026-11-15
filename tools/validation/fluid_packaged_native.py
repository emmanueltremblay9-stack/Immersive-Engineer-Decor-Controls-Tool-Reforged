#!/usr/bin/env python3
"""Exact ordinary-JAR, default-path Funnel/Accumulator native qualification."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import subprocess
import sys
import tempfile
import zipfile
from client_mixin_smoke import SmokeFailure, run_bounded
from factory_placer_v2_reference_native import png
from fluid_gestures_native import XVFB_BIND, require, validate_proof as validate_gestures
from fluid_packaged_proof import validate_packaged


def validate_proof(proof, nonce, source_sha, game, logs, exit_code, timed_out,
                   width, height, language, gui_scale, report):
    validate_gestures(proof, nonce, source_sha, game, logs, exit_code, timed_out,
                      width, height, language, gui_scale)
    validate_packaged(proof, report, width, height, gui_scale)
    for row in proof['tank_captures']:
        png(game/'fluid-gesture-screenshots'/row['name'], width, height, row['sha256'])
    return proof


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--runtime-jar", type=Path, help="Exact ordinary JAR, byte-identical to this checkout rebuild")
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
        require(args.width >= 800 and args.height >= 600 and args.width // args.gui_scale >= 320 and args.height // args.gui_scale >= 256, "Bounded fixture requires at least 800x600 and a logical viewport of at least 320x256")
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
    evidence = Path(tempfile.mkdtemp(prefix="fluid-packaged-native-", dir=project / "build"))
    game = evidence / "run"
    game.mkdir()
    nonce = secrets.token_hex(16)
    (game / ".iedctr-client-gameplay-nonce").write_text(nonce, encoding="utf-8")
    (game / "options.txt").write_text(
        "onboardAccessibility:false\ntutorialStep:none\nrenderDistance:2\nsimulationDistance:2\n"
        f"maxFps:60\npauseOnLostFocus:false\nsoundCategory_master:0.0\nguiScale:{args.gui_scale}\n"
        f"lang:{args.language}\n", encoding="utf-8")
    init = evidence / "fluid_packaged.init.gradle"
    init.write_text("""gradle.projectsEvaluated {
    rootProject.extensions.getByName('neoForge').runs.named('fluidPackagedDefaultClient') {
        gameDirectory.set(rootProject.file(System.getenv('IEDCTR_GAMEPLAY_DIR')))
        systemProperty 'iedctr.clientGameplayNonce', System.getenv('IEDCTR_GAMEPLAY_NONCE')
        systemProperty 'iedctr.clientGameplayDirectory', System.getenv('IEDCTR_GAMEPLAY_DIR')
        systemProperty 'iedctr.fluidPackagedNative', 'true'
        if (System.getenv('IEDCTR_EXPECTED_JAR')) {
            systemProperty 'iedctr.expectedModJar', System.getenv('IEDCTR_EXPECTED_JAR')
            systemProperty 'iedctr.expectedModJarSha256', System.getenv('IEDCTR_EXPECTED_JAR_SHA256')
        }
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
    gradle = ["bash", str(project / "gradlew"), "--no-daemon", "--init-script", str(init), "--stacktrace", "-Pinclude_fluid_packaged_validation=true"]
    supplied_hash = None
    if args.runtime_jar:
        supplied = args.runtime_jar.resolve(strict=True)
        supplied_hash = hashlib.sha256(supplied.read_bytes()).hexdigest()
        gradle += ["-Pfluid_runtime_jar="+str(supplied)]
    print(f"Packaged Funnel/Accumulator isolated native evidence: {evidence} ({args.width}x{args.height}, GUI scale {args.gui_scale}, {args.language})", flush=True)
    try:
        code, expired = run_bounded(gradle + ["prepareFluidPackagedDefaultClientRun", "verifyFluidPackagedRuntime"], project, env,
                                    evidence / "prepare.log", args.prepare_timeout)
        if expired or code != 0:
            raise SmokeFailure(f"Funnel/Accumulator gestures client preparation failed (exit={code}, timeout={expired})")
        report_path = project/'build/reports/fluid-packaged-runtime.json'
        report = json.loads(report_path.read_text())
        jar = Path(report['jar_path']).resolve(strict=True)
        jar_hash = hashlib.sha256(jar.read_bytes()).hexdigest()
        require(jar_hash == report['jar_sha256'], 'Prepared JAR changed')
        if supplied_hash is not None:
            require(jar == supplied and jar_hash == supplied_hash, 'Supplied artifact changed during preparation')
        (evidence/'packaged-runtime.json').write_text(json.dumps(report, indent=2))
        (evidence/'artifact-identity.json').write_text(json.dumps(dict(
            executed_source_sha=source_sha, nonce=nonce, jar_path=str(jar), jar_sha256=jar_hash), indent=2))
        env.update(IEDCTR_EXPECTED_JAR=str(jar), IEDCTR_EXPECTED_JAR_SHA256=jar_hash)
        log = evidence / "client.log"
        code, expired = run_bounded(["xvfb-run", "-a", "-s", f"-screen 0 {args.width}x{args.height}x24"]
                                    + [sys.executable, "-c", XVFB_BIND] + gradle + ["runFluidPackagedDefaultClient"], project, env, log, args.timeout)
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
                       args.width, args.height, args.language, args.gui_scale, report)
        require(hashlib.sha256(jar.read_bytes()).hexdigest() == jar_hash, "Production JAR changed during execution")
        print("PASS bounded packaged native Funnel/Accumulator gestures smoke. Full visual acceptance is not claimed.")
        return 0
    except (SmokeFailure, OSError, ValueError, KeyError, TypeError, zipfile.BadZipFile) as error:
        print(f"FAIL: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
