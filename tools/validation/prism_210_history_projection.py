"""Bounded in-memory projection from the local #210 candidate commits to d7dce072.

Historical guards consume the projected bytes only. The candidate, JAR, and current
native checks always use the unprojected working tree. The machine-readable record
contains exact per-commit hashes and reversible text hunks; it accepts no edits
outside those recorded ranges.
"""
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RECORD = Path(__file__).with_name("prism_210_history_projection.json")
RECORD_SHA256 = "c0274bd87ceb4ddc040ac2d93d663344a0a59d0b85c7a513fe2ff6fd94e61522"
BASE_COMMIT = "d7dce0724698a069855d2284c758e8761edd17b6"
HEAD_COMMIT = "276cd7c0a13e7bfacc58921611c605fb0f2411d2"
RECORD_BYTES = RECORD.read_bytes()


def _record():
    if hashlib.sha256(RECORD_BYTES).hexdigest() != RECORD_SHA256:
        raise AssertionError("#210 candidate projection record changed")
    data = json.loads(RECORD_BYTES)
    if data.get("schema") != "iedctr-prism-candidate-projection-v1":
        raise AssertionError("#210 candidate projection schema changed")
    if data.get("base_commit") != BASE_COMMIT or data.get("head_commit") != HEAD_COMMIT:
        raise AssertionError("#210 candidate projection commit range changed")
    return data


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def _reverse_layer(path: str, source: bytes, layer: dict) -> bytes:
    if sha256(source) != layer["new_sha256"]:
        raise AssertionError("#210 projection candidate bytes changed: " + path)
    lines = source.decode("utf-8").splitlines(keepends=True)
    for hunk in reversed(layer["hunks"]):
        start, end = hunk["start"], hunk["end"]
        if "".join(lines[start:end]) != hunk["current"]:
            raise AssertionError("#210 projection hunk mismatch: " + path)
        lines[start:end] = hunk["previous"].splitlines(keepends=True)
    historical = "".join(lines).encode("utf-8")
    if sha256(historical) != layer["old_sha256"]:
        raise AssertionError("#210 projection bytes changed outside recorded hunks: " + path)
    return historical


def project_current_candidate(path: str, source: bytes, *, strict: bool = False) -> bytes:
    """Project an exact recorded candidate state back to its #210 base.

    Unknown bytes pass through by default so an older isolated candidate can still
    be checked by its own fail-closed validator. Call with strict=True in tests
    that specifically validate this candidate's recorded state.
    """
    record = _record()["files"].get(path)
    if record is None:
        if strict:
            raise AssertionError("Path is outside the #210 projection allowlist: " + path)
        return source
    digest = sha256(source)
    states = record["states"]
    state_index = next((i for i in range(len(states) - 1, -1, -1)
                        if states[i]["sha256"] == digest), None)
    if state_index is None:
        if strict:
            raise AssertionError("Unrecognized #210 candidate bytes: " + path)
        return source
    current = source
    commit_to_layer = {layer["commit"]: layer for layer in record["layers"]}
    for i in range(state_index, 0, -1):
        current_sha = states[i]["sha256"]
        previous_sha = states[i - 1]["sha256"]
        layer = commit_to_layer.get(states[i]["commit"])
        if layer is None:
            if current_sha != previous_sha or sha256(current) != current_sha:
                raise AssertionError("Unrecorded #210 change: " + path)
            continue
        if layer["old_sha256"] != previous_sha or layer["new_sha256"] != current_sha:
            raise AssertionError("#210 projection layer linkage changed: " + path)
        current = _reverse_layer(path, current, layer)
    if sha256(current) != states[0]["sha256"]:
        raise AssertionError("#210 base projection mismatch: " + path)
    return current


def project_file_from_root(path: str, *, strict: bool = True) -> bytes:
    """Read the current file and return its exact d7dce072 historical view."""
    source = (ROOT / path).read_bytes()
    return project_current_candidate(path, source, strict=strict)


def expected_current_sha256(path: str) -> str | None:
    """Return the pinned final candidate blob identity for an allowed path."""
    record = _record()["files"].get(path)
    return None if record is None else record["states"][-1]["sha256"]
