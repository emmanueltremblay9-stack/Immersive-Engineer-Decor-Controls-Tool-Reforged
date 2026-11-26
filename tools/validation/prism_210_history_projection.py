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
LATE_VALIDATION_RECORD = Path(__file__).with_name("prism_210_late_validation_projection.json")
LATE_VALIDATION_RECORD_SHA256 = "87336694ba35c1d9d01dc8fe9fb7ae97546ede5a1a712c31ce36566bad83220f"
BASE_COMMIT = "d7dce0724698a069855d2284c758e8761edd17b6"
HEAD_COMMIT = "276cd7c0a13e7bfacc58921611c605fb0f2411d2"
RECORD_BYTES = RECORD.read_bytes()
LATE_VALIDATION_RECORD_BYTES = LATE_VALIDATION_RECORD.read_bytes()


def _record():
    if hashlib.sha256(RECORD_BYTES).hexdigest() != RECORD_SHA256:
        raise AssertionError("#210 candidate projection record changed")
    data = json.loads(RECORD_BYTES)
    if data.get("schema") != "iedctr-prism-candidate-projection-v1":
        raise AssertionError("#210 candidate projection schema changed")
    if data.get("base_commit") != BASE_COMMIT or data.get("head_commit") != HEAD_COMMIT:
        raise AssertionError("#210 candidate projection commit range changed")
    return data


def _late_validation_record():
    if sha256(LATE_VALIDATION_RECORD_BYTES) != LATE_VALIDATION_RECORD_SHA256:
        raise AssertionError("#210 late validation projection record changed")
    data = json.loads(LATE_VALIDATION_RECORD_BYTES)
    if data.get("schema") != "iedctr-prism-late-validation-projection-v1":
        raise AssertionError("#210 late validation projection schema changed")
    if data.get("base_commit") != HEAD_COMMIT:
        raise AssertionError("#210 late validation projection base changed")
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
        # A few native validation files are new after the pinned #210 history
        # range. Admit only an exact late-layer projection with its source commit
        # explicitly pinned; all other unknown paths remain fail-closed in strict
        # mode and pass through untouched for legacy layer composition.
        late = _late_validation_record()["files"].get(path)
        if late is not None and late.get("previous_source_commit"):
            if sha256(source) == late["new_sha256"]:
                return _reverse_layer(path, source, late)
            if strict:
                raise AssertionError("Unrecognized #210 late validation bytes: " + path)
        if strict:
            raise AssertionError("Path is outside the #210 projection allowlist: " + path)
        return source
    digest = sha256(source)
    states = record["states"]
    current = source
    state_index = next((i for i in range(len(states) - 1, -1, -1)
                        if states[i]["sha256"] == digest), None)
    if state_index is None:
        late = _late_validation_record()["files"].get(path)
        if late is not None and digest == late["new_sha256"]:
            if late["old_sha256"] != states[-1]["sha256"]:
                raise AssertionError("#210 late validation projection is detached from the recorded candidate: " + path)
            current = _reverse_layer(path, current, late)
            if sha256(current) != states[-1]["sha256"]:
                raise AssertionError("#210 late validation projection did not return to the exact recorded candidate: " + path)
            digest = sha256(current)
            state_index = len(states) - 1
    if state_index is None:
        if strict:
            raise AssertionError("Unrecognized #210 candidate bytes: " + path)
        return source
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
    if record is None:
        return None
    late = _late_validation_record()["files"].get(path)
    return record["states"][-1]["sha256"] if late is None else late["new_sha256"]
