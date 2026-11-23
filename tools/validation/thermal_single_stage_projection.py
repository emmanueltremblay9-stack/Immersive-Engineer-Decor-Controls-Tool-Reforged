"""Strict, in-memory projection of PR #209's exact thermal delta for historical-only checks.

The projected bytes are never written, compiled, packaged, or used as candidate evidence.
"""
from dataclasses import dataclass
from functools import lru_cache
import hashlib
from pathlib import Path
import re

BASE_COMMIT = "e8b21c91d5a7a3a6beb9d0b12c71e8bccc0f8d2c"
PATCH_PATH = Path(__file__).with_name("thermal_single_stage_main_projection.patch")

FILES = {
    "src/main/java/com/oblixorprime/engineersdecorreforged/utility/MachineBlockEntity.java": {
        "candidate_sha256": "991b52818e7a3f2e6d437f7e5f7039417e830fd45d9f6d6ebb3f2bafc2fca02b",
        "historical_sha256": "d49d082a4b67fbed04b0eb90fcf1f16f2c4b87f9ac93829ef8198fcdf0a7b894",
        "hunks": 27,
    },
    "src/main/java/com/oblixorprime/engineersdecorreforged/menu/MachineMenu.java": {
        "candidate_sha256": "384f61deb7ed502df0f7285a2e63e09cb56649726f88f7aaf11ea40942d869df",
        "historical_sha256": "74f95f7e8ab5af051ae6ce9ff435070a95cc12ea199f11c775e35fd14dfeb3de",
        "hunks": 9,
    },
    "src/main/java/com/oblixorprime/engineersdecorreforged/client/ClientModEvents.java": {
        "candidate_sha256": "90777c211657075dd5fc1e3c3f78e6fdfc771ed130468acc6a10760579fae147",
        "historical_sha256": "bf327e4fa3ff6b32afa1ec80c03c23b2c372ff8470a9181e86351cf29feba29c",
        "hunks": 1,
    },
}

@dataclass(frozen=True)
class Hunk:
    old_start: int
    old_count: int
    new_start: int
    new_count: int
    old_lines: tuple[str, ...]
    new_lines: tuple[str, ...]


_HEADER = re.compile(r"^@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@")


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


@lru_cache(maxsize=1)
def load_hunks() -> dict[str, tuple[Hunk, ...]]:
    """Parse the exact unified diff and pin its paths and hunk cardinalities."""
    lines = PATCH_PATH.read_text(encoding="utf-8").splitlines(keepends=True)
    result: dict[str, tuple[Hunk, ...]] = {}
    index = 0
    while index < len(lines):
        header = lines[index].rstrip("\n")
        if not header:
            index += 1
            continue
        match = re.fullmatch(r"diff --git a/(.+) b/(.+)", header)
        if match is None or match.group(1) != match.group(2):
            raise AssertionError("Malformed thermal projection file header")
        path = match.group(1)
        if path in result or path not in FILES:
            raise AssertionError("Unexpected or duplicate thermal projection path: " + path)
        index += 1
        if index + 1 >= len(lines) or lines[index].rstrip("\n") != "--- a/" + path or lines[index + 1].rstrip("\n") != "+++ b/" + path:
            raise AssertionError("Thermal projection path headers changed: " + path)
        index += 2
        hunks = []
        while index < len(lines) and lines[index].startswith("@@"):
            header = lines[index].rstrip("\n")
            match = _HEADER.match(header)
            if match is None:
                raise AssertionError("Malformed thermal projection hunk: " + path)
            old_start = int(match.group(1))
            old_count = int(match.group(2) or "1")
            new_start = int(match.group(3))
            new_count = int(match.group(4) or "1")
            old_lines: list[str] = []
            new_lines: list[str] = []
            index += 1
            while index < len(lines) and not lines[index].startswith("@@") and not lines[index].startswith("diff --git "):
                line = lines[index]
                if line in ("\n", "\r\n"):
                    index += 1
                    continue
                if line.startswith("\\"):
                    index += 1
                    continue
                if line.startswith(" "):
                    old_lines.append(line[1:])
                    new_lines.append(line[1:])
                elif line.startswith("-"):
                    old_lines.append(line[1:])
                elif line.startswith("+"):
                    new_lines.append(line[1:])
                else:
                    raise AssertionError("Malformed thermal projection line: " + path)
                index += 1
            if len(old_lines) != old_count or len(new_lines) != new_count:
                raise AssertionError("Thermal projection hunk length changed: " + path)
            hunks.append(Hunk(old_start, old_count, new_start, new_count, tuple(old_lines), tuple(new_lines)))
        if not hunks or len(hunks) != FILES[path]["hunks"]:
            raise AssertionError("Thermal projection hunk count changed: " + path)
        result[path] = tuple(hunks)
    if set(result) != set(FILES):
        raise AssertionError("Thermal projection file set is incomplete")
    return result


def reverse_hunks(path: str, source: bytes, hunks: tuple[Hunk, ...] | None = None) -> bytes:
    """Reverse exact current hunks in memory, then prove the historical file hash."""
    if path not in FILES:
        raise AssertionError("Path is not in the thermal projection allowlist: " + path)
    hunks = load_hunks()[path] if hunks is None else hunks
    lines = source.decode("utf-8").splitlines(keepends=True)
    for hunk in sorted(hunks, key=lambda item: item.new_start, reverse=True):
        start = hunk.new_start - 1 if hunk.new_count else hunk.new_start
        actual = tuple(lines[start:start + hunk.new_count])
        if actual != hunk.new_lines:
            raise AssertionError(f"Thermal projection hunk mismatch: {path}:{hunk.new_start}")
        lines[start:start + hunk.new_count] = hunk.old_lines
    historical = "".join(lines).encode("utf-8")
    if sha256(historical) != FILES[path]["historical_sha256"]:
        raise AssertionError("Thermal historical bytes changed outside declared hunks: " + path)
    return historical


def project_historical_file(path: str, source: bytes) -> bytes:
    """Require the exact current candidate before constructing one historical view."""
    if path not in FILES:
        raise AssertionError("Path is not in the thermal projection allowlist: " + path)
    if sha256(source) != FILES[path]["candidate_sha256"]:
        raise AssertionError("Thermal candidate bytes changed or delta is incomplete: " + path)
    return reverse_hunks(path, source)


def _project_exact_composite(path: str, source: bytes) -> bytes:
    """Reverse exact composite changes for historical checks only."""
    from json import loads
    projection = Path(__file__).with_name("thermal_gui_composite_projection.json")
    if not projection.exists():
        return source
    record = loads(projection.read_text(encoding="utf-8")).get(path)
    if record is None or sha256(source) != record["candidate"]:
        return source
    lines = source.decode("utf-8").splitlines(keepends=True)
    for start, end, current, previous in reversed(record["hunks"]):
        if "".join(lines[start:end]) != current:
            raise AssertionError("Composite thermal hunk mismatch: " + path)
        lines[start:end] = previous.splitlines(keepends=True)
    historical = "".join(lines).encode("utf-8")
    if sha256(historical) != record["gui_parent"]:
        raise AssertionError("Composite GUI parent bytes changed: " + path)
    return historical


def project_historical_if_candidate(path: str, source: bytes) -> bytes:
    """Project only exact candidate bytes; leave all other bytes for legacy guards to reject."""
    if path not in FILES:
        return _project_exact_composite(path, source)
    if sha256(source) != FILES[path]["candidate_sha256"]:
        return _project_exact_composite(path, source)
    return project_historical_file(path, source)


def project_historical_files(files: dict[str, bytes]) -> dict[str, bytes]:
    """Strict all-three-file projection for dedicated tests; never mutates the input mapping."""
    projected = dict(files)
    for path in FILES:
        if path not in files:
            raise AssertionError("Missing thermal candidate source: " + path)
        projected[path] = project_historical_file(path, files[path])
    return projected
