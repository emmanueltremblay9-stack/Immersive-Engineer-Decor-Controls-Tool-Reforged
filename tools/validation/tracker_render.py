"""Require actual Tracker item-renderer evidence from the existing isolated client fixture."""
import json
from pathlib import Path

CONTEXTS = {
    "NONE", "THIRD_PERSON_LEFT_HAND", "THIRD_PERSON_RIGHT_HAND",
    "FIRST_PERSON_LEFT_HAND", "FIRST_PERSON_RIGHT_HAND", "HEAD", "GUI", "GROUND", "FIXED",
}
FIRST_PERSON = {"FIRST_PERSON_LEFT_HAND", "FIRST_PERSON_RIGHT_HAND"}

def validate_tracker(directory: Path, nonce: str) -> None:
    proof = json.loads((directory / "client-tracker-render-result.json").read_text(encoding="utf-8"))
    if (proof.get("suite") != "tracker-render-v1" or proof.get("status") != "passed"
            or proof.get("nonce") != nonce or proof.get("body_quads") != 45
            or proof.get("pointer_quads") != 66 or proof.get("pointer_moved") is not True
            or proof.get("first_person_original_skip") is not True):
        raise ValueError("Missing complete Tracker render proof")
    cases = proof.get("cases", [])
    expected = CONTEXTS | {"malformed_target", "foreign_dimension", "target_32", "target_-32"}
    if len(cases) != len(expected) or {case.get("name") for case in cases} != expected:
        raise ValueError("Incomplete or duplicate Tracker render cases")
    for case in cases:
        name = case["name"]
        context = name if name in CONTEXTS else "GUI"
        skipped = context in FIRST_PERSON
        if (case.get("context") != context or case.get("vertices") != (0 if skipped else 444)
                or case.get("unchanged_stack") is not True or case.get("pose_restored") is not True):
            raise ValueError("Invalid Tracker render case: " + name)
        buffers = case.get("buffers", -1)
        if not isinstance(buffers, int) or (buffers != 0 if skipped else buffers < 1):
            raise ValueError("Invalid Tracker render buffer receipt: " + name)
    print("IEDCTR_TRACKER_RENDER_VERIFIED cases=13 firstPersonSkipped=2", flush=True)
