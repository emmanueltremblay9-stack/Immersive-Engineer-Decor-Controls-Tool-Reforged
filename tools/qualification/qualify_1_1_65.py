#!/usr/bin/env python3
"""Exact-byte IEDCTR 1.1.65 qualification. Never uploads a mod or accepts terms implicitly."""
from __future__ import annotations

import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path, PurePosixPath
import re
import shutil
import struct
import subprocess
import sys
import tempfile
import tomllib
import urllib.request
import zipfile

REPOSITORY = "emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged"
REPOSITORY_ID = 1270933662
SOURCE_COMMIT = "52639384ea90aebe9d563b966027421cebe52b45"
SOURCE_TREE = "6ea18e8edf03b71b4bf5598c7c0c915cfaf4b980"
BUILD_RUN_ID = 40610004593
BUILD_ATTEMPT = 1
MOD_ID = "immersive_engineer_decor_controls_tool_reforged"
VERSION = "1.1.65-reconstructed"
JAR_NAME = f"{MOD_ID}-{VERSION}.jar"
# A CI expectation, NOT a claim that independently rebuilt bytes have been qualified.
EXPECTED_SHA256 = "c8b5b2768a3139fec4663368310b29a5b3c106653e05b8df8c356846316c8d9e"
EULA_ACK = "I_ACCEPT_MINECRAFT_EULA_AND_MICROSOFT_SERVICES_AGREEMENT"
PROPERTIES = {"mod_id": MOD_ID, "mod_version": VERSION, "minecraft_version": "1.21.1",
              "neo_version": "21.1.230", "immersive_engineering_version": "1.21.1-12.4.2-194"}
DEPENDENCIES = {
    "minecraft": ("required", "[1.21.1]"),
    "neoforge": ("required", "[21.1.230,)"),
    "immersiveengineering": ("required", "[12.4.2-194,)"),
    "jei": ("optional", "[19.32.0.359,)"),
}
NOTICES = ("LICENSE", "NOTICE.md", "CREDITS.md")


class QualificationError(RuntimeError):
    pass


def require(condition: bool, message: str) -> None:
    if not condition:
        raise QualificationError(message)


def digest(path: Path) -> str:
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def git(root: Path, *args: str) -> str:
    return subprocess.check_output(["git", "-C", str(root), *args], text=True).strip()


def check_source(root: Path) -> Path:
    root = root.resolve(strict=True)
    require(git(root, "rev-parse", "HEAD") == SOURCE_COMMIT, "Wrong source commit")
    require(git(root, "rev-parse", "HEAD^{tree}") == SOURCE_TREE, "Wrong source tree")
    require(not git(root, "status", "--porcelain", "--untracked-files=all"), "Dirty source checkout")
    properties = dict(line.split("=", 1) for line in (root / "gradle.properties").read_text(
        encoding="utf-8").splitlines() if "=" in line and not line.startswith("#"))
    require(all(properties.get(k) == v for k, v in PROPERTIES.items()), "Source metadata mismatch")
    return root


def validate_ci(data: dict) -> dict:
    expected = {"id": BUILD_RUN_ID, "run_attempt": BUILD_ATTEMPT,
                "head_sha": SOURCE_COMMIT, "event": "push", "status": "completed",
                "conclusion": "success", "path": ".github/workflows/build.yml", "name": "Build"}
    require(isinstance(data, dict), "Malformed Build response")
    require(all(data.get(k) == v for k, v in expected.items()), "Exact-source Build CI mismatch")
    for field in ("repository", "head_repository"):
        repo = data.get(field, {})
        require(isinstance(repo, dict) and repo.get("id") == REPOSITORY_ID
                and repo.get("full_name") == REPOSITORY, "Build repository identity mismatch")
    return {**expected, "buildStatus": expected["status"], "status": "EXACT_SOURCE_CI_VERIFIED"}


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise QualificationError("Refusing GitHub API redirect")


def check_ci() -> dict:
    # Fixed API endpoint; token never reaches a redirect, build process, report or exception message.
    headers = {"Accept": "application/vnd.github+json", "User-Agent": "IEDCTR-exact-qualification"}
    if token := os.environ.get("GITHUB_TOKEN"):
        headers["Authorization"] = "Bearer " + token
    request = urllib.request.Request(
        f"https://api.github.com/repos/{REPOSITORY}/actions/runs/{BUILD_RUN_ID}", headers=headers)
    with urllib.request.build_opener(NoRedirect()).open(request, timeout=30) as response:
        body = response.read(2 * 1024 * 1024 + 1)
    require(len(body) <= 2 * 1024 * 1024, "Oversized Build response")
    return validate_ci(json.loads(body))


def read_manifest(raw: bytes) -> dict[str, str]:
    # JAR manifest continuation lines must be unfolded before checking implementation version.
    lines: list[str] = []
    for line in raw.decode("utf-8").replace("\r\n", "\n").split("\n"):
        if line.startswith(" "):
            require(bool(lines), "Orphan manifest continuation")
            lines[-1] += line[1:]
        else:
            lines.append(line)
    result = {}
    for line in lines:
        if not line:
            break  # main attributes only
        require(": " in line, "Malformed JAR manifest")
        key, value = line.split(": ", 1)
        require(key not in result, "Duplicate manifest attribute")
        result[key] = value
    return result


def inspect_archive(path: Path, source: Path) -> dict:
    require(path.is_file() and not path.is_symlink(), "Missing or symlinked JAR")
    require(0 < path.stat().st_size <= 128 * 1024 * 1024, "Invalid JAR size")
    sha = digest(path)
    require(sha == EXPECTED_SHA256, "JAR SHA-256 differs from exact CI expectation")
    with zipfile.ZipFile(path) as jar:
        infos = jar.infolist()
        names = [entry.filename for entry in infos]
        require(len(names) == len(set(names)), "Duplicate ZIP entry")
        require(sum(e.file_size for e in infos) <= 1024 * 1024 * 1024, "Uncompressed JAR too large")
        for entry in infos:
            name = entry.filename
            p = PurePosixPath(name)
            require(not p.is_absolute() and ".." not in p.parts and "\\" not in name
                    and not re.match(r"^[A-Za-z]:", name), "Unsafe ZIP path")
            require((entry.external_attr >> 16) & 0o170000 != 0o120000, "ZIP symlink")
            require(not entry.flag_bits & 1, "Encrypted ZIP entry")
        require(jar.testzip() is None, "ZIP CRC mismatch")
        meta = tomllib.loads(jar.read("META-INF/neoforge.mods.toml").decode("utf-8"))
        mods = meta.get("mods", [])
        require(len(mods) == 1 and mods[0].get("modId") == MOD_ID
                and mods[0].get("version") == VERSION, "Embedded mod ID/version mismatch")
        require(meta.get("license") == "MIT" and meta.get("modLoader") == "javafml",
                "Embedded license/loader mismatch")
        deps = meta.get("dependencies", {}).get(MOD_ID, [])
        require(len(deps) == len(DEPENDENCIES), "Embedded dependency count mismatch")
        seen = set()
        for dep in deps:
            key = dep.get("modId")
            require(key not in seen and key in DEPENDENCIES, "Unknown or duplicate dependency")
            seen.add(key)
            require((dep.get("type"), dep.get("versionRange")) == DEPENDENCIES[key]
                    and dep.get("side") == "BOTH", "Embedded dependency mismatch")
        manifest = read_manifest(jar.read("META-INF/MANIFEST.MF"))
        require(manifest.get("Implementation-Version") == VERSION
                and manifest.get("Specification-Version") == VERSION, "JAR manifest version mismatch")
        attribution = {}
        for name in NOTICES:
            actual = jar.read("META-INF/" + name)
            require(actual and actual == (source / name).read_bytes(), "Attribution differs from source")
            attribution[name] = hashlib.sha256(actual).hexdigest()
        mixins = meta.get("mixins", [])
        require(len(mixins) == 2, "Unexpected mixin configuration count")
        for mixin in mixins:
            require(mixin.get("config") in names, "Missing mixin configuration")
            require(isinstance(json.loads(jar.read(mixin["config"])), dict), "Malformed mixin JSON")
        classes = 0
        for name in names:
            if name.endswith(".class"):
                with jar.open(name) as stream:
                    header = stream.read(8)
                require(len(header) == 8 and header[:4] == b"\xca\xfe\xba\xbe", "Invalid class header")
                require(struct.unpack(">H", header[6:8])[0] <= 65 and header[4:6] != b"\xff\xff",
                        "Class requires newer Java or preview mode")
                classes += 1
        require(classes > 0, "No runtime classes")
    require(digest(path) == sha, "JAR changed during inspection")
    return {"assetName": JAR_NAME, "assetSha256": sha, "assetSize": path.stat().st_size,
            "modId": MOD_ID, "version": VERSION, "archiveEntries": len(names),
            "classCount": classes, "noticesSha256": attribution}


def inspect_source(root: Path) -> dict:
    source = check_source(root)
    jar = source / "build" / "libs" / JAR_NAME
    require(jar.resolve(strict=True) == jar, "Symlinked runtime path")
    result = inspect_archive(jar, source)
    check_source(source)
    return result


def compare_sources(a: Path, b: Path) -> dict:
    require(a.resolve() != b.resolve(), "Independent source directories required")
    first, second = inspect_source(a), inspect_source(b)
    require(first == second, "Independent JAR builds differ")
    return {"status": "BINARY_REPRODUCED", "sourceCommit": SOURCE_COMMIT, "sourceTree": SOURCE_TREE,
            "artifact": first, "buildDirectories": 2, "sameSourceIndependentBuilds": True,
            "packagedServer": "NOT_RUN", "exactJarClient": "NOT_RUN",
            "publication": "NOT_PERFORMED", "publicationReady": False,
            "scope": "Binary identity and two-build reproducibility; not gameplay acceptance"}


def server_smoke(source: Path, gradle_home: Path, acknowledgment: str) -> dict:
    # No filesystem writes, downloads, server import or EULA file before explicit consent.
    require(acknowledgment == EULA_ACK, "Explicit Minecraft EULA and Microsoft agreement acknowledgement required")
    artifact = inspect_source(source)
    source = source.resolve()
    spec = importlib.util.spec_from_file_location("iedctr_legacy_smoke", source / "tools/server/qualify_packaged_server.py")
    require(spec is not None and spec.loader is not None, "Missing pinned server harness")
    q = importlib.util.module_from_spec(spec)
    old_bytecode = sys.dont_write_bytecode
    try:
        sys.dont_write_bytecode = True
        spec.loader.exec_module(q)
    finally:
        sys.dont_write_bytecode = old_bytecode
    q.require_ack(acknowledgment)
    require(q.MOD_ID == MOD_ID and q.NEOFORGE_VERSION == PROPERTIES["neo_version"]
            and q.IE_VERSION == PROPERTIES["immersive_engineering_version"], "Server harness dependency drift")
    ie_hash = q.resolved_ie_hash(gradle_home.resolve(strict=True))
    runtime = source / "build/libs" / JAR_NAME
    # Reuse proven console assertions, NOT legacy check_source/check_runtime/install_server:
    # those deliberately remain pinned to historical 1.1.58.
    with tempfile.TemporaryDirectory(prefix="iedctr-1165-", dir=os.getenv("RUNNER_TEMP")) as folder:
        root = Path(folder)
        installer, checksum = root / "neoforge-installer.jar", root / "neoforge-installer.sha256"
        q.download(q.NEOFORGE_URL, installer, 32 * 1024 * 1024)
        q.download(q.NEOFORGE_URL + ".sha256", checksum, 512)
        expected = checksum.read_text(encoding="utf-8").strip()
        require(bool(re.fullmatch(r"[0-9a-fA-F]{64}", expected)) and digest(installer) == expected.lower(),
                "Official NeoForge installer hash mismatch")
        subprocess.run(["java", "-jar", str(installer), "--installServer"], cwd=root, check=True, timeout=600)
        require((root / "run.sh").is_file(), "Server installer did not produce run.sh")
        mods = root / "mods"
        mods.mkdir(exist_ok=True)
        ie = mods / f"ImmersiveEngineering-{q.IE_VERSION}.jar"
        q.download(q.IE_URL, ie, 64 * 1024 * 1024)
        require(digest(ie) == ie_hash, "IE dependency differs from pinned build")
        require(any(m.get("modId") == "immersiveengineering" and m.get("version") in
                    {q.IE_VERSION, "12.4.2-194"} for m in q.jar_mods(ie)), "IE mod identity mismatch")
        destination = mods / JAR_NAME
        shutil.copyfile(runtime, destination)
        require(inspect_archive(destination, source) == artifact, "Server runtime copy differs")
        q.write_configuration(root, acknowledgment)
        console = q.qualify(root)
        require(inspect_archive(destination, source) == artifact, "Server runtime changed")
    check_source(source)
    return {"status": "PACKAGED_SERVER_BASELINE_SMOKE_PASSED", "sourceCommit": SOURCE_COMMIT,
            "artifact": artifact, "console": console, "immersiveEngineeringSha256": ie_hash,
            "newFeatureGameplay": "NOT_RUN", "exactJarClient": "NOT_RUN", "publicationReady": False,
            "scope": "Startup/restart, baseline hatch/ladder/gate assertions only; not full 1.1.65 QA"}


def write_report(path: Path, data: dict) -> None:
    # Report output must be new. Never overwrite evidence from an earlier attempt.
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("x", encoding="utf-8", newline="\n") as stream:
        json.dump(data, stream, ensure_ascii=False, indent=2, sort_keys=True)
        stream.write("\n")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mode", choices=("ci", "inspect", "compare", "server"))
    parser.add_argument("--source-a", type=Path)
    parser.add_argument("--source-b", type=Path)
    parser.add_argument("--gradle-home", type=Path)
    parser.add_argument("--eula-acknowledgment", default="")
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args(argv)
    # Server consent is checked before even writing a report for an unapproved run.
    if args.mode == "server":
        require(args.eula_acknowledgment == EULA_ACK, "Explicit server consent required")
        require(args.gradle_home is not None, "Isolated Gradle home required")
    if args.mode != "ci":
        require(args.source_a is not None, "Source A required")
    if args.mode == "compare":
        require(args.source_b is not None, "Source B required")
    require(not args.report.exists(), "Report already exists; use a new run directory")
    try:
        if args.mode == "ci":
            result = check_ci()
        elif args.mode == "inspect":
            result = {"status": "BINARY_INSPECTED", "sourceCommit": SOURCE_COMMIT,
                      "artifact": inspect_source(args.source_a), "publicationReady": False}
        elif args.mode == "compare":
            result = compare_sources(args.source_a, args.source_b)
            result["ci"] = check_ci()
        else:
            result = server_smoke(args.source_a, args.gradle_home, args.eula_acknowledgment)
        write_report(args.report, result)
        print(json.dumps(result, sort_keys=True))
        return 0
    except Exception as error:
        # Do not serialize URLs, headers, tokens, arbitrary payloads or local account information.
        write_report(args.report, {"status": "FAILED", "mode": args.mode,
                                  "errorType": type(error).__name__, "publicationReady": False})
        print(f"Qualification failed ({type(error).__name__}); inspect the failing step.", file=sys.stderr)
        return 1


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (QualificationError, OSError) as error:
        print(f"Qualification preflight failed ({type(error).__name__}).", file=sys.stderr)
        raise SystemExit(1)
