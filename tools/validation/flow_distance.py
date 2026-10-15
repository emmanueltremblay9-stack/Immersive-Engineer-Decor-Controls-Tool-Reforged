"""Evidence gates for natural flow observations; pixels require separate review."""
import math
import struct
from pathlib import Path
from client_mixin_smoke import SmokeFailure, FATAL_PATTERN
from renderer_profiles import PROFILES

def validate_flow_distance_proof(proof: dict, session: dict, nonce: str, profile: str,
                                 directory: Path, logs: str, exit_code: int) -> dict:
    if exit_code != 0 or FATAL_PATTERN.search(logs) or list(directory.glob("crash-reports/*")):
        raise SmokeFailure("Natural-flow client did not complete cleanly")
    if (session.get("suite") != "native-water-flow-session-v1" or session.get("nonce") != nonce
            or session.get("status") != "passed"
            or f"IEDCTR_WATER_FLOW_SESSION PASSED nonce={nonce}" not in logs):
        raise SmokeFailure("Missing current successful natural-flow session")
    if (proof.get("suite") != "native-water-flow-distance-v1" or proof.get("nonce") != nonce
            or proof.get("status") != "captured" or proof.get("actual_integrated_world") is not True
            or proof.get("orientation") != "east" or proof.get("renderer_mods") != PROFILES[profile]):
        raise SmokeFailure("Natural-flow proof identity/profile mismatch")
    cases = proof.get("cases")
    if not isinstance(cases, list) or len(cases) != 9:
        raise SmokeFailure("Incomplete source-distance sweep")
    if {c.get("distance") for c in cases} != set(range(9)):
        raise SmokeFailure("Missing or duplicated source distance")
    angles = {}
    source_pos = None
    targets = set()
    for case in cases:
        distance = case["distance"]
        if type(distance) is not int:
            raise SmokeFailure("Invalid distance type")
        if case.get("direct_cell_fill") is not False or case.get("initial_sources_placed") != 1:
            raise SmokeFailure("Distance case did not preserve one natural source")
        if case.get("settled_ticks", 0) < 100 or case.get("unchanged_client_ticks", 0) < 20:
            raise SmokeFailure("Flow was captured before stabilization")
        source = case.get("source", {})
        if source.get("source") is not True or source.get("water") is not True:
            raise SmokeFailure("Upstream source was not retained")
        if source_pos is None: source_pos = source.get("position")
        if not source_pos or source.get("position") != source_pos:
            raise SmokeFailure("Source moved during distance sweep")
        samples = case.get("channel")
        if not isinstance(samples, list) or len(samples) != 10:
            raise SmokeFailure("Missing actual channel level samples")
        if (case.get("server_elapsed_ticks",0) < 100 or case.get("server_client_agree") is not True
                or case.get("server_channel") != samples):
            raise SmokeFailure("Missing settled authoritative server/client agreement")
        for sample in samples:
            if (type(sample.get("amount")) is not int or not 0 <= sample["amount"] <= 8
                    or type(sample.get("height")) not in (float, int) or not 0 <= sample["height"] <= 1
                    or type(sample.get("water")) is not bool or type(sample.get("source")) is not bool):
                raise SmokeFailure("Invalid actual fluid sample")
        if distance == 0:
            if not all(samples[d]["water"] for d in range(8)) or any(samples[d]["water"] for d in (8,9)):
                raise SmokeFailure("Free-channel reference did not establish the observed seven-block reach")
        else:
            target = case.get("target", {})
            if 'steel_catwalk_stairs' not in target.get("block_state", "") or 'facing:"east"' not in target.get("block_state", ""):
                raise SmokeFailure("Target is not the east-facing stair")
            if (target != samples[distance] or case.get("upstream") != samples[distance-1]
                    or case.get("downstream") != samples[distance+1] or source != samples[0]):
                raise SmokeFailure("Summary cells contradict the measured channel")
            targets.add(target.get("position"))
            try:
                origin = tuple(int(v.strip()) for v in source_pos.split(","))
                position = tuple(int(v.strip()) for v in target.get("position", "").split(","))
            except ValueError as error:
                raise SmokeFailure("Invalid source/target coordinates") from error
            if len(origin) != 3 or position != (origin[0]-distance, origin[1], origin[2]):
                raise SmokeFailure("Target does not move one block per source distance")
            if (case.get("mechanical_flow_in_cell") is not target.get("water")
                    or case.get("mechanical_flow_after_cell") is not case.get("downstream", {}).get("water")):
                raise SmokeFailure("Mechanical summary contradicts the observed fluid states")
        for angle in ("overview", "close"):
            yaw, pitch = case.get(angle+"_yaw"), case.get(angle+"_pitch")
            if type(yaw) not in (float,int) or type(pitch) not in (float,int) or not math.isfinite(yaw+pitch):
                raise SmokeFailure("Missing constant camera angles")
            if angle not in angles: angles[angle] = (yaw,pitch)
            if abs(angles[angle][0]-yaw) > 0.01 or abs(angles[angle][1]-pitch) > 0.01:
                raise SmokeFailure("Camera angle changed between distances")
            name = f"flow-distance-{distance}-{angle}.png"
            if case.get(angle+"_screenshot") != name:
                raise SmokeFailure("Screenshot identity mismatch")
            data = (directory / "canal-screenshots" / name).read_bytes()
            if len(data) < 1000 or data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR":
                raise SmokeFailure("Invalid distance screenshot")
            width,height = struct.unpack(">II",data[16:24])
            if width < 600 or height < 400:
                raise SmokeFailure("Distance screenshot too small to inspect")
    if len(targets) != 8:
        raise SmokeFailure("Stair did not move for each independent distance")
    return proof

