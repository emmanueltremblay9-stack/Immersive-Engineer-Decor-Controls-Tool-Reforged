#!/usr/bin/env python3
"""Bounded Linux client transformation/load smoke, not an interactive gameplay test."""

from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import re
import shutil
import signal
import subprocess
import sys
import tempfile
import time


TARGET = "net.minecraft.client.renderer.GameRenderer"
PICK_DESCRIPTOR = "(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;"
HELPER_DESCRIPTOR = "(Lnet/minecraft/world/entity/Entity;DFLnet/minecraft/world/phys/HitResult;)Lnet/minecraft/world/phys/HitResult;"
HANDLER_DESCRIPTOR = "(Lnet/minecraft/world/entity/Entity;DDFLorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V"
HANDLER_SUFFIX = "engineersdecorreforged$pickUpperStairRail"
HELPER = "com/oblixorprime/engineersdecorreforged/block/StairRailingTargeting.extendPick"
FATAL_PATTERN = re.compile(
    r"MixinApplyError|MixinTransformerError|InvalidMixinException|InvalidInjectionException|"
    r"InjectionError|VerifyError|NoSuchMethodError|NoSuchFieldError|NoClassDefFoundError|"
    r"IncompatibleClassChangeError|UnsupportedClassVersionError|LinkageError|"
    r"Exception in thread|ReportedException|ModLoadingException|"
    r"Failed to create window|Failed to initialize graphics|BUILD FAILED|\bFATAL\b"
)


class SmokeFailure(RuntimeError):
    pass


def method_bodies(disassembly: str) -> dict[tuple[str, str], str]:
    """Read javap -p -s -c method boundaries; never accept a constant-pool name alone."""
    methods = {}
    header = re.compile(r"^  (?:public |protected |private )?(?:static )?.*?([^\s(]+)\([^\n]*\);$")
    lines = disassembly.splitlines()
    starts = [(index, header.match(line)) for index, line in enumerate(lines) if header.match(line)]
    for offset, (start, match) in enumerate(starts):
        stop = starts[offset + 1][0] if offset + 1 < len(starts) else len(lines)
        body = "\n".join(lines[start:stop])
        descriptor = re.search(r"^    descriptor: (\S+)$", body, re.MULTILINE)
        if descriptor:
            methods[(match.group(1), descriptor.group(1))] = body
    return methods


def invocations(body: str) -> list[tuple[str, str]]:
    return re.findall(r"\b(?:invokevirtual|invokespecial|invokestatic)\s+[^\n]*// Method (\S+?):(\S+)", body)


def validate_evidence(disassembly: str, class_log: str, client_log: str,
                      return_code: int | None, timed_out: bool, crash_files: list[Path]) -> dict:
    if not timed_out and return_code != 0:
        raise SmokeFailure(f"Client exited unsuccessfully before the smoke deadline: {return_code}")
    if crash_files:
        raise SmokeFailure("Client produced a crash report: " + ", ".join(map(str, crash_files)))
    fatal = FATAL_PATTERN.search(client_log)
    if fatal:
        raise SmokeFailure(f"Client loading/injection error found: {fatal.group(0)}")
    if not re.search(r"\[class,load\]\s+" + re.escape(TARGET) + r"\s+source:", class_log):
        raise SmokeFailure("No JVM class-definition evidence for the transformed GameRenderer")

    methods = method_bodies(disassembly)
    pick = methods.get(("pick", PICK_DESCRIPTOR), "")
    if not pick:
        raise SmokeFailure("The transformed class lacks the exact native pick method")
    handlers = [(name.rsplit(".", 1)[-1], descriptor) for name, descriptor in invocations(pick)
                if name.rsplit(".", 1)[-1].endswith(HANDLER_SUFFIX)]
    # Minecraft 1.21.1 has both entity and block return branches. Both must be transformed.
    if len(handlers) < 2:
        raise SmokeFailure("Native pick must invoke the railing handler at both original return branches")
    for name, descriptor in handlers:
        if descriptor != HANDLER_DESCRIPTOR:
            raise SmokeFailure("The merged railing handler has the wrong descriptor")
        body = methods.get((name, descriptor), "")
        if (HELPER, HELPER_DESCRIPTOR) not in invocations(body):
            raise SmokeFailure("The called railing handler does not invoke the exact targeting helper")
    return {"target": TARGET, "native_pick_descriptor": PICK_DESCRIPTOR,
            "injected_return_calls": len(handlers), "jvm_class_defined": True,
            "scope": "Client transformation and class loading only; interactive gameplay is untested"}


def process_identity(pid: int) -> tuple[int, str] | None:
    try:
        # Field 2 (comm) may contain spaces or parentheses. Read only PPID/start time.
        fields = Path(f"/proc/{pid}/stat").read_text().rsplit(") ", 1)[1].split()
        return int(fields[1]), fields[19]
    except (OSError, ValueError, IndexError):
        return None


def track_descendants(root: int, tracked: dict[int, str]) -> None:
    processes = {int(path.name): process_identity(int(path.name))
                 for path in Path("/proc").iterdir() if path.name.isdecimal()}
    parents = {root} | {pid for pid, start in tracked.items()
                        if processes.get(pid) and processes[pid][1] == start}
    while True:
        found = {pid for pid, info in processes.items()
                 if info and info[0] in parents and pid not in parents}
        if not found:
            break
        for pid in found:
            tracked[pid] = processes[pid][1]
        parents.update(found)


def signal_descendants(tracked: dict[int, str], sig: signal.Signals) -> None:
    for pid, start in tracked.items():
        current = process_identity(pid)
        if current and current[1] == start:
            try:
                os.kill(pid, sig)
            except ProcessLookupError:
                pass


def run_bounded(command: list[str], cwd: Path, env: dict[str, str], log: Path,
                timeout: int) -> tuple[int, bool]:
    """Stop this invocation and its descendants, including detached single-use Gradle daemons."""
    with log.open("w", encoding="utf-8") as output:
        process = subprocess.Popen(command, cwd=cwd, env=env, stdout=output,
                                   stderr=subprocess.STDOUT, start_new_session=True)
        timed_out = False
        tracked = {}
        deadline = time.monotonic() + timeout
        try:
            while process.poll() is None:
                track_descendants(process.pid, tracked)
                remaining = deadline - time.monotonic()
                if remaining <= 0:
                    timed_out = True
                    break
                try:
                    process.wait(timeout=min(0.5, remaining))
                except subprocess.TimeoutExpired:
                    pass
        finally:
            track_descendants(process.pid, tracked)
            # Preserve the actual observation window. Intentionally stopping Gradle can
            # append BUILD FAILED / daemon-disappeared noise after a healthy client run.
            output.flush()
            with log.open("rb") as source:
                observed = source.read(os.fstat(source.fileno()).st_size)
            log.with_suffix(".observed.log").write_bytes(observed)
            # The group can still contain Java/Xvfb descendants after its leader exits.
            try:
                os.killpg(process.pid, signal.SIGTERM)
            except ProcessLookupError:
                pass
            signal_descendants(tracked, signal.SIGTERM)
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                pass
            try:
                os.killpg(process.pid, signal.SIGKILL)
            except ProcessLookupError:
                pass
            signal_descendants(tracked, signal.SIGKILL)
            process.wait()
        return process.returncode, timed_out


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--timeout", type=int, default=180, help="Client observation window in seconds")
    parser.add_argument("--prepare-timeout", type=int, default=600, help="Separate asset/preparation deadline")
    args = parser.parse_args()
    if sys.platform != "linux":
        parser.error("This smoke test supports Linux CI only")
    if args.timeout <= 0 or args.prepare_timeout <= 0:
        parser.error("Timeouts must be positive")
    missing = [name for name in ("java", "javap", "Xvfb", "xvfb-run", "xauth") if not shutil.which(name)]
    if missing:
        print("Client smoke prerequisites missing: " + ", ".join(missing), file=sys.stderr)
        return 1
    project = args.project.resolve()
    if not (project / "gradlew").is_file():
        parser.error("Project must contain the checked-in Gradle wrapper")
    (project / "build").mkdir(exist_ok=True)
    evidence = Path(tempfile.mkdtemp(prefix="client-load-smoke-", dir=project / "build"))
    game = evidence / "run"
    game.mkdir()
    print(f"Client transformation/load evidence: {evidence}", flush=True)
    init = evidence / "smoke.init.gradle"
    init.write_text("""// CI-only run configuration. No code or settings are shipped in the mod.
gradle.projectsEvaluated {
    rootProject.extensions.getByName('neoForge').runs.named('client') {
        gameDirectory.set(rootProject.file(System.getenv('IEDCTR_SMOKE_GAME_DIR')))
        systemProperty 'mixin.debug.export', 'true'
        systemProperty 'mixin.debug.export.filter', 'net.minecraft.client.renderer.GameRenderer'
        systemProperty 'mixin.debug.export.decompile', 'false'
        systemProperty 'mixin.debug.verbose', 'true'
        jvmArgument '-Xlog:class+load=info:file=' + System.getenv('IEDCTR_SMOKE_CLASS_LOG')
    }
}
""", encoding="utf-8")
    env = dict(os.environ, IEDCTR_SMOKE_GAME_DIR=str(game),
               IEDCTR_SMOKE_CLASS_LOG=str(evidence / "class-load.log"),
               LIBGL_ALWAYS_SOFTWARE="1", ALSOFT_DRIVERS="null")
    gradle = [str(project / "gradlew"), "--no-daemon", "--init-script", str(init), "--stacktrace"]
    prepare_log = evidence / "prepare.log"
    try:
        code, expired = run_bounded(gradle + ["prepareClientRun"], project, env,
                                    prepare_log, args.prepare_timeout)
        if expired or code != 0:
            raise SmokeFailure(f"Client preparation failed (exit={code}, deadline={expired}); see {prepare_log}")
        client_log = evidence / "client.log"
        code, expired = run_bounded(["xvfb-run", "-a", "-s", "-screen 0 1280x720x24"] + gradle + ["runClient"],
                                    project, env, client_log, args.timeout)
        exported = game / ".mixin.out/class" / (TARGET.replace(".", "/") + ".class")
        if not exported.is_file():
            raise SmokeFailure("The normal client launch did not export a transformed GameRenderer")
        result = subprocess.run(["javap", "-p", "-s", "-c", str(exported)], text=True,
                                capture_output=True, check=True, timeout=30)
        (evidence / "GameRenderer.javap.txt").write_text(result.stdout, encoding="utf-8")
        class_log = evidence / "class-load.log"
        logs = client_log.with_suffix(".observed.log").read_text(encoding="utf-8", errors="replace")
        for name in ("latest.log", "debug.log"):
            runtime_log = game / "logs" / name
            if runtime_log.is_file():
                logs += "\n" + runtime_log.read_text(encoding="utf-8", errors="replace")
        crashes = list(game.glob("crash-reports/*")) + list(evidence.rglob("hs_err_pid*.log"))
        proof = validate_evidence(result.stdout,
                                  class_log.read_text(encoding="utf-8", errors="replace") if class_log.is_file() else "",
                                  logs, code, expired, crashes)
        proof.update({"client_reached_deadline": expired, "client_exit_code": code})
        (evidence / "proof.json").write_text(json.dumps(proof, indent=2) + "\n", encoding="utf-8")
        print(json.dumps(proof, indent=2))
        return 0
    except (SmokeFailure, OSError, subprocess.SubprocessError) as error:
        print(f"Client transformation/load smoke FAILED: {error}\nEvidence: {evidence}", file=sys.stderr)
        # Surface bounded diagnostics in Actions; the ephemeral runner's paths alone are not actionable.
        for diagnostic in (prepare_log, evidence / "client.observed.log", game / "logs/latest.log"):
            if diagnostic.is_file():
                print(f"--- {diagnostic.name}: last 100 lines ---", file=sys.stderr)
                print("\n".join(diagnostic.read_text(encoding="utf-8", errors="replace").splitlines()[-100:]),
                      file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
