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
FREEZER_NATIVE_PORT_RECORD = Path(__file__).with_name("prism_210_freezer_native_port_projection.json")
FREEZER_NATIVE_PORT_RECORD_SHA256 = "45c7eda7753117ce2ffccb7206bf00e7568435d6cf3bcf7b53754e1e15aa7071"
FREEZER_RECOVERY_TOOLTIP_RECORD = Path(__file__).with_name("prism_210_freezer_recovery_tooltip_projection.json")
FREEZER_RECOVERY_TOOLTIP_RECORD_SHA256 = "ff20fadb6932b79b83a0cf7afb6f8d2284b7ff9aff0fff7bc0ae13fe4e8bc841"
FREEZER_RECOVERY_KEY_RECORD = Path(__file__).with_name("prism_210_freezer_recovery_key_projection.json")
FREEZER_RECOVERY_KEY_RECORD_SHA256 = "32a85aed550eef187c74978d996d99a54e4ce9e26e2e86ecc78416516b4400c8"
FREEZER_ENERGY_SCISSOR_RECORD = Path(__file__).with_name("prism_210_freezer_energy_scissor_projection.json")
FREEZER_ENERGY_SCISSOR_RECORD_SHA256 = "0d0c106569237b2a69d78abbe70fbd0b1170699f218b516238526b19aee56e2a"
FREEZER_QA_FOLLOWUP_RECORD = Path(__file__).with_name("prism_210_freezer_qa_followup_projection.json")
FREEZER_QA_FOLLOWUP_RECORD_SHA256 = "32299db7dc9ff2dffba801f2455976b54e1b75df64c0592aea5ff25c8e1427d2"
ELECTRICAL_CONTROLS_RECORD = Path(__file__).with_name("prism_210_electrical_controls_projection.json")
ELECTRICAL_CONTROLS_RECORD_SHA256 = "d9c5f924307e814e3cc772782c145fcd9e104f8741df305bf93395d3c5967f98"
ORDINARY_JAR_REPIN_RECORD = Path(__file__).with_name("prism_210_ordinary_jar_repin_projection.json")
ORDINARY_JAR_REPIN_RECORD_SHA256 = "2d4f192fda610d20c805869a86745f7dcb625a24e366b3d2d987d7b3c70aff0e"
ORDINARY_JAR_FOLLOWUP_REPIN_RECORD = Path(__file__).with_name("prism_210_ordinary_jar_followup_repin_projection.json")
ORDINARY_JAR_FOLLOWUP_REPIN_RECORD_SHA256 = "5447c5ac95323a4d4c9f3511ad76da2b63be3377db25ac0b1297691930870cdd"
SOLAR_GUI11_R3_RECORD = Path(__file__).with_name("prism_210_solar_gui11_r3_projection.json")
SOLAR_GUI11_R3_RECORD_SHA256 = "8cfc9a97852d0462dab0cf776fc05611fdb77db26376f9096ea14e72839e5b57"
GUI11_V5C_RECORD = Path(__file__).with_name("prism_210_gui11_v5c_projection.json")
GUI11_V5C_RECORD_SHA256 = "743a1d0d4f6540abef585c1ebbe441239329be9171d5a9a962dd911ffebf788e"
BASE_COMMIT = "d7dce0724698a069855d2284c758e8761edd17b6"
HEAD_COMMIT = "276cd7c0a13e7bfacc58921611c605fb0f2411d2"
RECORD_BYTES = RECORD.read_bytes()
LATE_VALIDATION_RECORD_BYTES = LATE_VALIDATION_RECORD.read_bytes()
FREEZER_NATIVE_PORT_RECORD_BYTES = FREEZER_NATIVE_PORT_RECORD.read_bytes()
FREEZER_RECOVERY_TOOLTIP_RECORD_BYTES = FREEZER_RECOVERY_TOOLTIP_RECORD.read_bytes()
FREEZER_RECOVERY_KEY_RECORD_BYTES = FREEZER_RECOVERY_KEY_RECORD.read_bytes()
FREEZER_ENERGY_SCISSOR_RECORD_BYTES = FREEZER_ENERGY_SCISSOR_RECORD.read_bytes()
FREEZER_QA_FOLLOWUP_RECORD_BYTES = FREEZER_QA_FOLLOWUP_RECORD.read_bytes()
ELECTRICAL_CONTROLS_RECORD_BYTES = ELECTRICAL_CONTROLS_RECORD.read_bytes()
ORDINARY_JAR_REPIN_RECORD_BYTES = ORDINARY_JAR_REPIN_RECORD.read_bytes()
ORDINARY_JAR_FOLLOWUP_REPIN_RECORD_BYTES = ORDINARY_JAR_FOLLOWUP_REPIN_RECORD.read_bytes()
SOLAR_GUI11_R3_RECORD_BYTES = SOLAR_GUI11_R3_RECORD.read_bytes()
GUI11_V5C_RECORD_BYTES = GUI11_V5C_RECORD.read_bytes()


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


def _freezer_native_port_record():
    if sha256(FREEZER_NATIVE_PORT_RECORD_BYTES) != FREEZER_NATIVE_PORT_RECORD_SHA256:
        raise AssertionError("#210 Freezer native port projection record changed")
    data = json.loads(FREEZER_NATIVE_PORT_RECORD_BYTES)
    if data.get("schema") != "iedctr-prism-freezer-native-port-v1":
        raise AssertionError("#210 Freezer native port projection schema changed")
    if data.get("base_commit") != "e429a1fa1551c4532eb3e59d531ccf86570ff9b5":
        raise AssertionError("#210 Freezer native port projection base changed")
    return data


def _freezer_recovery_tooltip_record():
    if sha256(FREEZER_RECOVERY_TOOLTIP_RECORD_BYTES) != FREEZER_RECOVERY_TOOLTIP_RECORD_SHA256:
        raise AssertionError("#210 Freezer recovery tooltip projection record changed")
    data = json.loads(FREEZER_RECOVERY_TOOLTIP_RECORD_BYTES)
    if data.get("schema") != "iedctr-prism-freezer-recovery-tooltip-wrap-v1":
        raise AssertionError("#210 Freezer recovery tooltip projection schema changed")
    if data.get("base_commit") != "1a2963e3120987eee036007b209ef87cf70b44ae":
        raise AssertionError("#210 Freezer recovery tooltip projection base changed")
    return data


def _freezer_recovery_key_record():
    if sha256(FREEZER_RECOVERY_KEY_RECORD_BYTES) != FREEZER_RECOVERY_KEY_RECORD_SHA256:
        raise AssertionError("#210 Freezer recovery key projection record changed")
    data = json.loads(FREEZER_RECOVERY_KEY_RECORD_BYTES)
    if data.get("schema") != "iedctr-prism-freezer-recovery-key-stage-v1":
        raise AssertionError("#210 Freezer recovery key projection schema changed")
    if data.get("base_commit") != "d6ed3f6bf31190c8da5205ae9430354f78f6da29":
        raise AssertionError("#210 Freezer recovery key projection base changed")
    return data


def _freezer_energy_scissor_record():
    if sha256(FREEZER_ENERGY_SCISSOR_RECORD_BYTES) != FREEZER_ENERGY_SCISSOR_RECORD_SHA256:
        raise AssertionError("#210 Freezer FE scissor projection record changed")
    data = json.loads(FREEZER_ENERGY_SCISSOR_RECORD_BYTES)
    if data.get("schema") != "iedctr-prism-freezer-energy-scissor-v1":
        raise AssertionError("#210 Freezer FE scissor projection schema changed")
    if data.get("base_commit") != "b9fd7697645654633873a967eebaf2d61eda35ce":
        raise AssertionError("#210 Freezer FE scissor projection base changed")
    return data


def _freezer_qa_followup_record():
    if sha256(FREEZER_QA_FOLLOWUP_RECORD_BYTES) != FREEZER_QA_FOLLOWUP_RECORD_SHA256:
        raise AssertionError("#210 Freezer QA follow-up projection record changed")
    data = json.loads(FREEZER_QA_FOLLOWUP_RECORD_BYTES)
    if data.get("schema") != "iedctr-prism-freezer-qa-followup-v1":
        raise AssertionError("#210 Freezer QA follow-up projection schema changed")
    if data.get("base_commit") != "a5723c09d735bf5500586737c8a04ae97ff63e60":
        raise AssertionError("#210 Freezer QA follow-up projection base changed")
    return data


def _electrical_controls_record():
    if sha256(ELECTRICAL_CONTROLS_RECORD_BYTES) != ELECTRICAL_CONTROLS_RECORD_SHA256:
        raise AssertionError("#210 electrical controls projection record changed")
    data = json.loads(ELECTRICAL_CONTROLS_RECORD_BYTES)
    if data.get("schema") != "iedctr-prism-electrical-controls-v1":
        raise AssertionError("#210 electrical controls projection schema changed")
    if data.get("base_commit") != "366ecf2c9d7e74afcc6398b80cfa84896b74e819":
        raise AssertionError("#210 electrical controls projection base changed")
    return data


def _ordinary_jar_repin_record():
    if sha256(ORDINARY_JAR_REPIN_RECORD_BYTES) != ORDINARY_JAR_REPIN_RECORD_SHA256:
        raise AssertionError("#210 ordinary JAR repin projection record changed")
    data = json.loads(ORDINARY_JAR_REPIN_RECORD_BYTES)
    if data.get("schema") != "iedctr-prism-ordinary-jar-repin-v1":
        raise AssertionError("#210 ordinary JAR repin projection schema changed")
    if data.get("base_commit") != "f3189eb153a4c1ac31f9072be49a2c8872c0f1e6":
        raise AssertionError("#210 ordinary JAR repin projection base changed")
    return data


def _ordinary_jar_followup_repin_record():
    if sha256(ORDINARY_JAR_FOLLOWUP_REPIN_RECORD_BYTES) != ORDINARY_JAR_FOLLOWUP_REPIN_RECORD_SHA256:
        raise AssertionError("#210 ordinary JAR follow-up repin projection record changed")
    data = json.loads(ORDINARY_JAR_FOLLOWUP_REPIN_RECORD_BYTES)
    if data.get("schema") != "iedctr-prism-ordinary-jar-followup-repin-v1":
        raise AssertionError("#210 ordinary JAR follow-up repin projection schema changed")
    if data.get("base_commit") != "0c9299718701e9374b102617631f3b319d224bf3":
        raise AssertionError("#210 ordinary JAR follow-up repin projection base changed")
    return data


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def _solar_gui11_r3_record():
    if sha256(SOLAR_GUI11_R3_RECORD_BYTES) != SOLAR_GUI11_R3_RECORD_SHA256:
        raise AssertionError("#210 Solar GUI11 r3 projection record changed")
    data = json.loads(SOLAR_GUI11_R3_RECORD_BYTES)
    if (data.get("schema") != "iedctr-prism-solar-gui11-r3-projection-v1"
            or data.get("base_commit") != "05439f5aa9a5423d45721a4240b60284c237164f"):
        raise AssertionError("#210 Solar GUI11 r3 projection baseline changed")
    return data


def _gui11_v5c_record():
    if sha256(GUI11_V5C_RECORD_BYTES) != GUI11_V5C_RECORD_SHA256:
        raise AssertionError("#210 GUI11 V5-Corrigee projection record changed")
    data = json.loads(GUI11_V5C_RECORD_BYTES)
    if (data.get("schema") != "iedctr-prism-gui11-v5c-source-projection-v1"
            or data.get("base_commit") != "2aa82b630ee4c5abd465daecb6d718bf731b36da"):
        raise AssertionError("#210 GUI11 V5-Corrigee projection base changed")
    return data


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


def project_before_solar_gui11_r3(path: str, source: bytes) -> bytes:
    """Recover the exact pre-r3 implementation for historical Solar V2 checks."""
    layer = _solar_gui11_r3_record()["files"].get(path)
    if layer is not None and sha256(source) == layer["new_sha256"]:
        return _reverse_layer(path, source, layer)
    return source


def project_before_gui11_v5c(path: str, source: bytes, *, strict: bool = False) -> bytes:
    """Remove only the exact V5-Corrigee GUI11 integration top layer."""
    layer = _gui11_v5c_record()["files"].get(path)
    if layer is None:
        return source
    digest = sha256(source)
    if digest == layer["new_sha256"]:
        return _reverse_layer(path, source, layer)
    if digest == layer["old_sha256"]:
        return source
    if strict:
        raise AssertionError("Unrecognized #210 candidate bytes (GUI11 V5-Corrigee layer): " + path)
    return source


def project_current_candidate(path: str, source: bytes, *, strict: bool = False) -> bytes:
    """Project an exact recorded candidate state back to its #210 base.

    Unknown bytes pass through by default so an older isolated candidate can still
    be checked by its own fail-closed validator. Call with strict=True in tests
    that specifically validate this candidate's recorded state.
    """
    gui11_v5c = _gui11_v5c_record()["files"].get(path)
    gui11_v5c_digest = sha256(source)
    gui11_v5c_reversed = gui11_v5c is not None and gui11_v5c_digest in (
        gui11_v5c["new_sha256"], gui11_v5c["old_sha256"]
    )
    # Older historical tests may intentionally supply an exact pre-V5 layer.
    # Let those immutable layers validate that state; current-candidate strict
    # rejection still happens at the layer that actually owns the path.
    source = project_before_gui11_v5c(path, source, strict=False)
    source = project_before_solar_gui11_r3(path, source)
    followup_repin = _ordinary_jar_followup_repin_record()["files"].get(path)
    followup_repin_reversed = False
    if followup_repin is not None:
        if sha256(source) == followup_repin["new_sha256"]:
            source = _reverse_layer(path, source, followup_repin)
            followup_repin_reversed = True
        elif sha256(source) == followup_repin["old_sha256"]:
            followup_repin_reversed = True
        # Older exact states pass through this layer and are checked by the
        # historical repin/projection layers below. These paths are shared with
        # the prior repin, which rejects any unrecorded bytes in strict mode.
    repin = _ordinary_jar_repin_record()["files"].get(path)
    repin_reversed = False
    if repin is not None:
        if sha256(source) == repin["new_sha256"]:
            source = _reverse_layer(path, source, repin)
            repin_reversed = True
        elif sha256(source) == repin["old_sha256"]:
            repin_reversed = True
        elif strict:
            raise AssertionError("Unrecognized #210 candidate bytes: " + path)
    electrical = _electrical_controls_record()["files"].get(path)
    electrical_reversed = False
    if electrical is not None:
        if sha256(source) == electrical["new_sha256"]:
            source = _reverse_layer(path, source, electrical)
            electrical_reversed = True
        elif sha256(source) == electrical["old_sha256"]:
            electrical_reversed = True
        elif strict:
            raise AssertionError("Unrecognized #210 candidate bytes: " + path)
    followup = _freezer_qa_followup_record()["files"].get(path)
    followup_reversed = False
    if followup is not None:
        if sha256(source) == followup["new_sha256"]:
            source = _reverse_layer(path, source, followup)
            followup_reversed = True
        elif sha256(source) == followup["old_sha256"]:
            followup_reversed = True
        elif strict:
            raise AssertionError("Unrecognized #210 candidate bytes: " + path)
    scissor = _freezer_energy_scissor_record()["files"].get(path)
    scissor_reversed = False
    if scissor is not None:
        if sha256(source) == scissor["new_sha256"]:
            source = _reverse_layer(path, source, scissor)
            scissor_reversed = True
        elif strict:
            raise AssertionError("Unrecognized #210 candidate bytes: " + path)
    recovery_key = _freezer_recovery_key_record()["files"].get(path)
    recovery_key_reversed = False
    if recovery_key is not None:
        if sha256(source) == recovery_key["new_sha256"]:
            source = _reverse_layer(path, source, recovery_key)
            recovery_key_reversed = True
        elif strict:
            raise AssertionError("Unrecognized #210 candidate bytes: " + path)
    recovery_tooltip = _freezer_recovery_tooltip_record()["files"].get(path)
    recovery_tooltip_reversed = False
    if recovery_tooltip is not None:
        if sha256(source) == recovery_tooltip["new_sha256"]:
            source = _reverse_layer(path, source, recovery_tooltip)
            recovery_tooltip_reversed = True
        elif strict and not recovery_key_reversed:
            raise AssertionError("Unrecognized #210 candidate bytes: " + path)
    port = _freezer_native_port_record()["files"].get(path)
    port_reversed = False
    if port is not None:
        if sha256(source) == port["new_sha256"]:
            source = _reverse_layer(path, source, port)
            port_reversed = True
        elif strict and not (recovery_tooltip_reversed or recovery_key_reversed):
            raise AssertionError("Unrecognized #210 candidate bytes: " + path)
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
            if not gui11_v5c_reversed and not followup_repin_reversed and not repin_reversed and not electrical_reversed and not followup_reversed and not scissor_reversed and not port_reversed and not recovery_tooltip_reversed and not recovery_key_reversed:
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
    gui11_v5c = _gui11_v5c_record()["files"].get(path)
    if gui11_v5c is not None:
        return gui11_v5c["new_sha256"]
    solar_gui11 = _solar_gui11_r3_record()["files"].get(path)
    if solar_gui11 is not None:
        return solar_gui11["new_sha256"]
    followup_repin = _ordinary_jar_followup_repin_record()["files"].get(path)
    if followup_repin is not None:
        return followup_repin["new_sha256"]
    repin = _ordinary_jar_repin_record()["files"].get(path)
    if repin is not None:
        return repin["new_sha256"]
    electrical = _electrical_controls_record()["files"].get(path)
    if electrical is not None:
        return electrical["new_sha256"]
    followup = _freezer_qa_followup_record()["files"].get(path)
    if followup is not None:
        return followup["new_sha256"]
    scissor = _freezer_energy_scissor_record()["files"].get(path)
    if scissor is not None:
        return scissor["new_sha256"]
    recovery_key = _freezer_recovery_key_record()["files"].get(path)
    if recovery_key is not None:
        return recovery_key["new_sha256"]
    recovery_tooltip = _freezer_recovery_tooltip_record()["files"].get(path)
    if recovery_tooltip is not None:
        return recovery_tooltip["new_sha256"]
    port = _freezer_native_port_record()["files"].get(path)
    if port is not None:
        return port["new_sha256"]
    record = _record()["files"].get(path)
    if record is None:
        return None
    late = _late_validation_record()["files"].get(path)
    return record["states"][-1]["sha256"] if late is None else late["new_sha256"]
