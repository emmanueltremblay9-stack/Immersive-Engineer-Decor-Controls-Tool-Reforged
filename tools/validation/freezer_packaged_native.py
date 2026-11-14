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

from freezer_reference_native import validate_proof, ART_SHA
from freezer_packaged_proof import validate_runtime

def require_client_success(code, expired, log):
    """Report the process failure before attempting to read absent evidence."""
    if expired or type(code) is not int or code != 0:
        raise SmokeFailure(f"Freezer packaged client failed (exit={code}, timeout={expired}); inspect {log}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--runtime-jar", type=Path, required=True)
    parser.add_argument("--language", choices=("en_us", "fr_fr"), default="en_us")
    parser.add_argument("--height", type=int, choices=(260, 720), default=720)
    parser.add_argument("--prepare-timeout", type=int, default=600)
    parser.add_argument("--timeout", type=int, default=600)
    parser.add_argument("--full-inventory", action="store_true")
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
    evidence = Path(tempfile.mkdtemp(prefix="freezer-packaged-native-", dir=project / "build"))
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
    rootProject.extensions.getByName('neoForge').runs.named('freezerPackagedOptInClient') {
        gameDirectory.set(rootProject.file(System.getenv('IEDCTR_GAMEPLAY_DIR')))
        systemProperty 'iedctr.clientGameplay', 'true'
        systemProperty 'iedctr.clientGameplayNonce', System.getenv('IEDCTR_GAMEPLAY_NONCE')
        systemProperty 'iedctr.clientGameplayDirectory', System.getenv('IEDCTR_GAMEPLAY_DIR')
        systemProperty 'iedctr.freezerReferenceOnly', 'true'
        systemProperty 'iedctr.freezerPackagedNative', 'true'
        systemProperty 'iedctr.freezerFullInventory', System.getenv('IEDCTR_FULL_INVENTORY')
        systemProperty 'iedctr.expectedModJar', System.getenv('IEDCTR_EXPECTED_JAR')
        systemProperty 'iedctr.expectedModJarSha256', System.getenv('IEDCTR_EXPECTED_JAR_SHA256')
        systemProperty 'iedctr.executedSourceSha', System.getenv('IEDCTR_EXECUTED_SOURCE_SHA')
        programArgument '--width'
        programArgument '1280'
        programArgument '--height'
        programArgument System.getenv('IEDCTR_SCREEN_HEIGHT')
    }
}
""", encoding="utf-8")
    supplied = args.runtime_jar.resolve(strict=True)
    supplied_hash = hashlib.sha256(supplied.read_bytes()).hexdigest()
    env = dict(os.environ, IEDCTR_FULL_INVENTORY=str(args.full_inventory).lower(), IEDCTR_EXPECTED_JAR=str(supplied), IEDCTR_EXPECTED_JAR_SHA256=supplied_hash, IEDCTR_GAMEPLAY_DIR=str(game), IEDCTR_GAMEPLAY_NONCE=nonce,
               IEDCTR_EXECUTED_SOURCE_SHA=source_sha, IEDCTR_SCREEN_HEIGHT=str(args.height),
               LIBGL_ALWAYS_SOFTWARE="1", ALSOFT_DRIVERS="null")
    gradle = ["bash", str(project / "gradlew"), "--no-daemon", "--init-script", str(init), "--stacktrace", "-Pinclude_freezer_packaged_validation=true", "-Pfreezer_runtime_jar="+str(supplied)]
    print(f"Freezer isolated native evidence: {evidence}", flush=True)
    try:
        code, expired = run_bounded(gradle + ["prepareFreezerPackagedOptInClientRun", "verifyFreezerPackagedRuntime"], project, env,
                                    evidence / "prepare.log", args.prepare_timeout)
        if expired or code != 0:
            raise SmokeFailure(f"Freezer client preparation failed (exit={code}, timeout={expired})")
        report = json.loads((project/'build/reports/freezer-packaged-runtime.json').read_text())
        if Path(report['jar_path']).resolve() != supplied or report['jar_sha256'] != supplied_hash:
            raise SmokeFailure("Prepared artifact differs from supplied normal JAR")
        (evidence/'packaged-runtime.json').write_text(json.dumps(report, indent=2))
        (evidence/'artifact-identity.json').write_text(json.dumps(dict(executed_source_sha=source_sha, nonce=nonce, jar_sha256=supplied_hash), indent=2))
        shutil.copyfile(supplied, evidence/'tested-production.jar')
        log = evidence / "client.log"
        code, expired = run_bounded(["xvfb-run", "-a", "-s", f"-screen 0 1280x{args.height}x24"]
                                    + gradle + ["runFreezerPackagedOptInClient"], project, env, log, args.timeout)
        require_client_success(code, expired, log)
        logs = log.with_suffix(".observed.log").read_text(errors="replace")
        for name in ("latest.log", "debug.log"):
            runtime = game / "logs" / name
            if runtime.is_file():
                logs += "\n" + runtime.read_text(errors="replace")
        if FATAL_PATTERN.search(logs):
            raise SmokeFailure(f"Freezer packaged client emitted fatal errors; inspect {log}")
        proof = json.loads((game / "client-freezer-reference-result.json").read_text())
        main_proof = json.loads((game / "client-gameplay-result.json").read_text())
        final_status = subprocess.run(["git", "status", "--porcelain", "--untracked-files=all"], cwd=project,
                                      check=True, capture_output=True, text=True).stdout.strip()
        final_sha = subprocess.run(["git", "rev-parse", "HEAD"], cwd=project, check=True,
                                   capture_output=True, text=True).stdout.strip()
        if final_status or final_sha != source_sha:
            raise SmokeFailure("Candidate changed during native execution; source identity is not exact")
        for stage in ('before', 'after'):
            origin = json.loads((game/f'freezer-packaged-{stage}.json').read_text())
            if origin.get('stage') != stage or origin.get('nonce') != nonce or origin.get('executed_source_sha') != source_sha:
                raise SmokeFailure("Stale packaged provenance")
            validate_runtime(origin, report)
        if hashlib.sha256(supplied.read_bytes()).hexdigest() != supplied_hash:
            raise SmokeFailure("Production JAR changed during execution")
        validate_proof(proof, main_proof, nonce, source_sha, game, logs, code, expired, 1280, args.height, args.language)
        if args.full_inventory:
            from freezer_full_inventory_proof import validate_full_inventory
            validate_full_inventory(proof, game, 1280, args.height)
        print("PASS bounded ordinary-JAR normal-path Freezer smoke; bundled fixture explicitly active, no physical OS input claim. Chamber frost follows server phases; decorative rim remains static; full visual/input acceptance is not claimed.")
        return 0
    except (SmokeFailure, OSError, ValueError, KeyError, TypeError) as error:
        print(f"FAIL: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
