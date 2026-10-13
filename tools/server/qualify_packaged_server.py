#!/usr/bin/env python3
"""Opt-in packaged dedicated-server smoke test. No Minecraft code in this file.

Import and unit tests are inert. main() requires explicit EULA acknowledgement
before downloads, writes, or launching anything. It never runs GameTestServer.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import queue
import re
import shutil
import signal
import subprocess
import tempfile
import threading
import time
import tomllib
import urllib.request
import uuid
import zipfile

SOURCE_COMMIT = "19c6bc7f8322002fd1d4710885d13f4482b4cb45"
MOD_ID = "immersive_engineer_decor_controls_tool_reforged"
VERSION = "1.1.58-reconstructed"
RUNTIME_SHA256 = "bc77067f9f6e0c03f987123b490812d32aa9171ad388fcd87ecdf50777883fdb"
NEOFORGE_VERSION = "21.1.230"
IE_VERSION = "1.21.1-12.4.2-194"
EULA_ACK = "I_ACCEPT_MINECRAFT_EULA_AND_MICROSOFT_SERVICES_AGREEMENT"
EULA_URL = "https://www.minecraft.net/en-us/eula"
MSA_URL = "https://www.microsoft.com/servicesagreement"
NEOFORGE_URL = (
    "https://maven.neoforged.net/releases/net/neoforged/neoforge/"
    f"{NEOFORGE_VERSION}/neoforge-{NEOFORGE_VERSION}-installer.jar"
)
IE_URL = (
    "https://maven.blamejared.com/blusunrize/immersiveengineering/"
    f"ImmersiveEngineering/{IE_VERSION}/ImmersiveEngineering-{IE_VERSION}.jar"
)
READY = re.compile(r'Done \([\d.,]+s\)! For help, type "help"')
BAD_LOG = re.compile(
    r"\b(?:ERROR|FATAL)\b|NoClassDefFoundError|ClassNotFoundException|"
    r"Attempted to load class .*?(?:client|invalid dist)|"
    r"Exception in server tick loop|Missing mandatory dependencies|"
    r"Failed to (?:load|start)|Errors in currently selected datapacks",
    re.IGNORECASE,
)


class QualificationError(RuntimeError):
    pass


def require_ack(value: str) -> None:
    if value != EULA_ACK:
        raise QualificationError(
            f"Server operation requires explicit acknowledgement of {EULA_URL} "
            f"and its incorporated Microsoft Services Agreement ({MSA_URL}); "
            "no agreement is accepted by validation or unit tests."
        )


def digest(path: Path) -> str:
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def jar_mods(path: Path) -> list[dict]:
    with zipfile.ZipFile(path) as jar:
        metadata = tomllib.loads(jar.read("META-INF/neoforge.mods.toml").decode())
        mods = metadata.get("mods", [])
        if any(mod.get("version") == "${file.jarVersion}" for mod in mods):
            manifest = jar.read("META-INF/MANIFEST.MF").decode().replace("\r\n", "\n")
            manifest = manifest.replace("\n ", "")
            values = dict(line.split(": ", 1) for line in manifest.splitlines() if ": " in line)
            for mod in mods:
                if mod.get("version") == "${file.jarVersion}":
                    mod["version"] = values.get("Implementation-Version")
        return mods


def check_runtime(path: Path, expected_hash: str = RUNTIME_SHA256) -> str:
    actual = digest(path)
    if actual != expected_hash:
        raise QualificationError(f"Runtime SHA-256 mismatch: {actual}")
    if not any(mod.get("modId") == MOD_ID and mod.get("version") == VERSION
               for mod in jar_mods(path)):
        raise QualificationError("Runtime mod ID/version differs from the pinned candidate")
    return actual


def check_notices(runtime: Path, source: Path) -> dict:
    hashes = {}
    with zipfile.ZipFile(runtime) as jar:
        for name in ("LICENSE", "NOTICE.md", "CREDITS.md"):
            expected = (source / name).read_bytes()
            actual = jar.read("META-INF/" + name)
            if actual != expected or not actual:
                raise QualificationError(f"Packaged attribution differs from exact source: {name}")
            hashes[name] = hashlib.sha256(actual).hexdigest()
    return hashes


def resolved_ie_hash(gradle_home: Path) -> str:
    coordinate = (gradle_home / "caches/modules-2/files-2.1" /
                  "blusunrize.immersiveengineering/ImmersiveEngineering" / IE_VERSION)
    candidates = list(coordinate.glob(f"*/ImmersiveEngineering-{IE_VERSION}.jar"))
    if len(candidates) != 1:
        raise QualificationError("Expected one original Immersive Engineering JAR resolved by pinned build")
    return digest(candidates[0])


def check_source(root: Path) -> None:
    def git(*args: str) -> str:
        return subprocess.check_output(["git", "-C", str(root), *args], text=True).strip()
    if git("rev-parse", "HEAD") != SOURCE_COMMIT:
        raise QualificationError("Runtime source commit differs from pinned candidate")
    if git("status", "--porcelain", "--untracked-files=all"):
        raise QualificationError("Runtime source is not clean")
    props = dict(line.split("=", 1) for line in (root / "gradle.properties").read_text().splitlines()
                 if "=" in line and not line.startswith("#"))
    for key, expected in {
        "mod_id": MOD_ID, "mod_version": VERSION, "minecraft_version": "1.21.1",
        "neo_version": NEOFORGE_VERSION, "immersive_engineering_version": IE_VERSION,
    }.items():
        if props.get(key) != expected:
            raise QualificationError(f"Pinned source property differs: {key}")


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise QualificationError("Dependency download redirected; review the new destination")


def download(url: str, path: Path, maximum: int) -> None:
    # Fixed official project Maven endpoints only; no arbitrary URLs or insecure fallback.
    if url not in {NEOFORGE_URL, NEOFORGE_URL + ".sha256", IE_URL}:
        raise QualificationError("Unapproved dependency URL")
    opener = urllib.request.build_opener(NoRedirect())
    with opener.open(url, timeout=90) as response, path.open("xb") as target:
        count = 0
        while chunk := response.read(1024 * 1024):
            count += len(chunk)
            if count > maximum:
                raise QualificationError("Dependency download exceeds expected size bound")
            target.write(chunk)


def install_server(root: Path, runtime: Path, expected_ie_hash: str) -> dict:
    installer = root / "neoforge-installer.jar"
    checksum = root / "neoforge-installer.sha256"
    download(NEOFORGE_URL, installer, 32 * 1024 * 1024)
    download(NEOFORGE_URL + ".sha256", checksum, 512)
    expected = checksum.read_text().strip()
    if not re.fullmatch(r"[0-9a-fA-F]{64}", expected) or digest(installer) != expected.lower():
        raise QualificationError("Official NeoForge installer checksum mismatch")
    # The vendor installer retrieves Mojang/NeoForge runtime files into this fresh folder.
    subprocess.run(["java", "-jar", str(installer), "--installServer"],
                   cwd=root, check=True, timeout=600)
    if not (root / "run.sh").is_file():
        raise QualificationError("NeoForge installation did not create run.sh")
    mods = root / "mods"
    mods.mkdir(exist_ok=True)
    ie = mods / f"ImmersiveEngineering-{IE_VERSION}.jar"
    download(IE_URL, ie, 64 * 1024 * 1024)
    if digest(ie) != expected_ie_hash:
        raise QualificationError("Immersive Engineering differs from the dependency resolved by pinned build")
    if not any(mod.get("modId") == "immersiveengineering"
               and mod.get("version") in {IE_VERSION, "12.4.2-194"}
               for mod in jar_mods(ie)):
        raise QualificationError("Immersive Engineering download metadata mismatch")
    destination = mods / runtime.name
    shutil.copyfile(runtime, destination)
    check_runtime(destination)
    return {"neoForgeInstallerSha256": digest(installer), "immersiveEngineeringSha256": digest(ie)}


def write_configuration(root: Path, acknowledgment: str) -> None:
    require_ack(acknowledgment)  # Defense in depth at the exact legal acceptance write.
    (root / "eula.txt").write_text("# Explicit operator acknowledgement supplied for this run\neula=true\n")
    (root / "user_jvm_args.txt").write_text("-Xms1G\n-Xmx3G\n")
    # No RCON, no authentication weakening, no firewall changes, no player access.
    (root / "server.properties").write_text(
        "server-ip=127.0.0.1\nserver-port=25565\nonline-mode=true\n"
        "enable-rcon=false\nenable-query=false\nwhite-list=true\nenforce-whitelist=true\n"
        "max-players=1\nlevel-name=qualification-world\nlevel-seed=1158\n"
        "view-distance=2\nsimulation-distance=2\nspawn-protection=0\n"
        "motd=IEDCTR disposable packaged-server qualification\n"
    )


class ConsoleServer:
    """Pipe-only console controller; never opens an authenticated player session."""
    def __init__(self, root: Path, phase: str):
        self.phase = phase
        self.lines: list[str] = []
        self.assertions: list[str] = []
        self.events: queue.Queue[str | None] = queue.Queue()
        self.process = subprocess.Popen(
            ["bash", "run.sh", "nogui"], cwd=root, stdin=subprocess.PIPE,
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
            encoding="utf-8", errors="replace", bufsize=1, start_new_session=True,
        )
        self.reader = threading.Thread(target=self._read, daemon=True)
        self.reader.start()

    def _read(self) -> None:
        try:
            assert self.process.stdout is not None
            for raw in self.process.stdout:
                line = raw.rstrip("\n")
                self.lines.append(line)
                self.events.put(line)
        finally:
            self.events.put(None)

    def wait_for(self, pattern: re.Pattern, timeout: float = 60, *, allow_timeout: bool = False) -> bool:
        deadline = time.monotonic() + timeout
        while (remaining := deadline - time.monotonic()) > 0:
            try:
                line = self.events.get(timeout=remaining)
            except queue.Empty:
                break
            if line is None:
                raise QualificationError(f"{self.phase}: server exited before expected evidence")
            if BAD_LOG.search(line):
                raise QualificationError(f"{self.phase}: server error: {line}")
            if pattern.search(line):
                return True
        if allow_timeout:
            return False
        raise QualificationError(f"{self.phase}: timed out awaiting {pattern.pattern}")

    def send(self, command: str) -> None:
        if "\n" in command or "\r" in command:
            raise QualificationError("Console command contains a newline")
        if self.process.poll() is not None or self.process.stdin is None:
            raise QualificationError("Server console is closed")
        self.process.stdin.write(command + "\n")
        self.process.stdin.flush()

    def barrier(self, condition: str = "") -> str:
        token = "IEDCTR_ASSERT_" + uuid.uuid4().hex
        self.send((f"execute {condition} run " if condition else "") + "say " + token)
        # Match server output, never an echoed command or an earlier run's marker.
        self.wait_for(re.compile(r"\[Server\] " + token + r"$"))
        self.assertions.append(condition or "console-barrier")
        return token

    def assert_block(self, position: str, state: str) -> None:
        self.barrier(f"if block {position} {state}")

    def stop(self) -> None:
        self.send("save-all flush")
        self.wait_for(re.compile(r"Saved the game"))
        self.send("stop")
        self.wait_for(re.compile(r"Stopping server"))
        if self.process.wait(timeout=120) != 0:
            raise QualificationError(f"{self.phase}: unclean server exit")
        self.reader.join(timeout=10)
        if self.reader.is_alive():
            raise QualificationError("Console reader did not finish after shutdown")
        errors = [line for line in self.lines if BAD_LOG.search(line)]
        if errors:
            raise QualificationError(f"{self.phase}: errors in complete server log: {errors[0]}")

    def close(self) -> None:
        if self.process.poll() is None:
            # Only this newly created child process group; never touch another server.
            os.killpg(self.process.pid, signal.SIGTERM)
            try:
                self.process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                os.killpg(self.process.pid, signal.SIGKILL)
                self.process.wait(timeout=15)
        self.reader.join(timeout=10)
        for stream in (self.process.stdin, self.process.stdout):
            if stream is not None:
                stream.close()


HATCH = f"{MOD_ID}:iron_hatch[facing=north,open=true,powered=true]"
LADDER = f"{MOD_ID}:metal_rung_ladder[facing=north,waterlogged=false]"
LADDER_HATCH = f"{MOD_ID}:iron_hatch[facing=south,open=true,powered=false]"
GATE = f"{MOD_ID}:steel_mesh_fence_gate"
STATES = (
    ("2 80 2", HATCH),
    ("6 80 4", LADDER),
    ("6 81 4", LADDER_HATCH),
    ("10 80 2", GATE + "[facing=north,open=true,segment=0]"),
    ("10 81 2", GATE + "[facing=north,open=true,segment=1]"),
)


def wait_for_fixture_chunk(server: ConsoleServer) -> None:
    token = "IEDCTR_LOADED_" + uuid.uuid4().hex
    loaded = re.compile(r"\[Server\] " + token + r"$")
    deadline = time.monotonic() + 60
    while time.monotonic() < deadline:
        server.send("execute if loaded 0 80 0 run say " + token)
        if server.wait_for(loaded, timeout=2, allow_timeout=True):
            break
    else:
        raise QualificationError("Fixture chunk did not become loaded")


def prepare_fixture(server: ConsoleServer) -> None:
    server.send("forceload add 0 0")
    wait_for_fixture_chunk(server)
    for command in (
        "gamerule randomTickSpeed 0",
        "fill 0 79 0 15 84 15 minecraft:air",
        "fill 0 79 0 15 79 15 minecraft:stone",
        f"setblock 2 80 2 {MOD_ID}:iron_hatch[facing=north,open=false,powered=false]",
        "setblock 3 80 2 minecraft:redstone_block",
    ):
        server.send(command)
    server.assert_block("2 80 2", HATCH)
    server.send("setblock 3 80 2 minecraft:air")
    server.assert_block("2 80 2", f"{MOD_ID}:iron_hatch[open=false,powered=false]")
    server.send("setblock 3 80 2 minecraft:redstone_block")
    server.send("setblock 6 80 5 minecraft:stone")
    for position, state in STATES[1:]:
        server.send(f"setblock {position} {state}")
    assert_persistent_states(server)


def assert_persistent_states(server: ConsoleServer) -> None:
    for position, state in STATES:
        server.assert_block(position, state)


def qualify(root: Path) -> dict:
    phases = []
    for phase in ("initial", "restart"):
        server = ConsoleServer(root, phase)
        try:
            server.wait_for(READY, timeout=300)
            if phase == "initial":
                prepare_fixture(server)
            else:
                # Only a read-only loaded query before assertions; persisted force-load tickets must work.
                wait_for_fixture_chunk(server)
                assert_persistent_states(server)
                server.send("setblock 3 80 2 minecraft:air")
                server.assert_block("2 80 2", f"{MOD_ID}:iron_hatch[open=false,powered=false]")
                server.send("setblock 10 80 2 minecraft:air")
                server.assert_block("10 80 2", "minecraft:air")
                server.assert_block("10 81 2", "minecraft:air")
            server.stop()
            # Retain auditable text in Actions logs; no world or runtime binaries are uploaded.
            print(f"--- BEGIN COMPLETE CONSOLE {phase} ---", flush=True)
            print("\n".join(server.lines), flush=True)
            print(f"--- END COMPLETE CONSOLE {phase} ---", flush=True)
            phases.append({"phase": phase, "cleanStop": True, "errorCount": 0,
                           "consoleBlockAssertions": server.assertions,
                           "consoleLogSha256": hashlib.sha256("\n".join(server.lines).encode()).hexdigest()})
        except Exception:
            # Console-only diagnostic text; never publish world/server/runtime binaries.
            print(f"Last console lines ({phase}):", flush=True)
            print("\n".join(server.lines[-80:]), flush=True)
            raise
        finally:
            server.close()
    return {"phases": phases, "persistentBlockStates": len(STATES),
            "nativeRedstoneAndLinkedRemoval": True, "realPlayerClicksVerified": False,
            "clientRenderingVerified": False, "playerClimbingVerified": False,
            "lootVerified": False, "multiplayerSynchronizationVerified": False}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--eula-acknowledgment", default="")
    parser.add_argument("--check-ack-only", action="store_true")
    parser.add_argument("--source-root", type=Path)
    parser.add_argument("--gradle-user-home", type=Path, default=os.getenv("GRADLE_USER_HOME"))
    args = parser.parse_args(argv)
    require_ack(args.eula_acknowledgment)  # MUST remain before any side effects.
    if args.check_ack_only:
        print("Explicit acknowledgement supplied; server execution has not started.")
        return 0
    if args.source_root is None:
        parser.error("--source-root is required for a real qualification run")
    source = args.source_root.resolve()
    check_source(source)
    runtime = source / "build/libs" / f"{MOD_ID}-{VERSION}.jar"
    actual = check_runtime(runtime)
    attribution = check_notices(runtime, source)
    if args.gradle_user_home is None:
        parser.error("--gradle-user-home (or GRADLE_USER_HOME) must identify the isolated pinned build cache")
    dependency_hash = resolved_ie_hash(args.gradle_user_home.resolve())
    report = {"status": "FAILED", "sourceCommit": SOURCE_COMMIT,
              "runtimeSha256": actual, "runtimeVersion": VERSION,
              "runtimeSizeBytes": runtime.stat().st_size, "packagedAttributionSha256": attribution,
              "immersiveEngineeringBuildResolvedSha256": dependency_hash,
              "minecraftVersion": "1.21.1", "neoForgeVersion": NEOFORGE_VERSION,
              "immersiveEngineeringVersion": IE_VERSION,
              "scope": "packaged-dedicated-server-console-smoke"}
    try:
        with tempfile.TemporaryDirectory(prefix="iedctr-server-", dir=os.getenv("RUNNER_TEMP")) as folder:
            root = Path(folder)
            report.update(install_server(root, runtime, dependency_hash))
            write_configuration(root, args.eula_acknowledgment)
            report.update(qualify(root))
        # Cleanup of the disposable runtime must also succeed before issuing a pass.
        report["status"] = "PACKAGED_SERVER_SMOKE_PASSED"
    finally:
        text = json.dumps(report, indent=2, sort_keys=True)
        print(text)
        if summary := os.getenv("GITHUB_STEP_SUMMARY"):
            with open(summary, "a", encoding="utf-8") as stream:
                stream.write("## Packaged dedicated-server smoke qualification\n\n```json\n" + text + "\n```\n")
                stream.write("\nConsole-only evidence. Real client clicks, rendering, climbing, loot, and multiplayer synchronization remain unverified.\n")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (QualificationError, OSError, subprocess.SubprocessError, zipfile.BadZipFile) as error:
        raise SystemExit(str(error))
