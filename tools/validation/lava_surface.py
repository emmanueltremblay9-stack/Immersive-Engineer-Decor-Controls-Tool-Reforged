"""Qualify actual lava heights and shared top corners, without substituting block geometry."""
import json
import math
from catalogue_visual import valid_png
from client_mixin_smoke import SmokeFailure

CASES = ("controlled-vanilla", "controlled-decor", "natural-vanilla", "natural-decor")

def shared_corner_gaps(meshes):
    corners = {}
    for mesh in meshes:
        if mesh.get("top_present") is not True:
            continue
        x, y, z = map(int, mesh["pos"].split(", "))
        points = mesh["actual_vertices"][:4]
        expected = [(x & 15, z & 15), (x & 15, (z & 15)+1),
                    ((x & 15)+1, (z & 15)+1), ((x & 15)+1, z & 15)]
        if len(points) != 4 or [(v[0], v[2]) for v in points] != expected:
            raise SmokeFailure("Lava top face identity mismatch")
        for vx, vy, vz in points:
            if not (y & 15) < vy <= (y & 15)+1:
                raise SmokeFailure("Lava top vertex outside its cell")
            key = (vx+(x//16)*16, y, vz+(z//16)*16)
            corners.setdefault(key, []).append(vy)
    return [max(values)-min(values) for values in corners.values()
            if len(values)>1 and max(values)-min(values)>1e-5]

def validate_lava(proof, nonce, profile, directory):
    if proof.get("suite") != "lava-surface-v1" or proof.get("nonce") != nonce or proof.get("status") != "passed":
        raise SmokeFailure("Missing current complete lava-surface proof")
    cases = proof.get("cases")
    if not isinstance(cases, list) or [c.get("case") for c in cases] != list(CASES):
        raise SmokeFailure("Incomplete/duplicate lava-surface cases")
    for case in cases:
        name = case["case"]
        if case.get("server_client_agree") is not True:
            raise SmokeFailure("Lava server/client disagreement")
        for field in ("native_mesh_mismatches", "position_height_mismatches", "embeddium_height_mismatches"):
            if type(case.get(field)) is not int or case[field] != 0:
                raise SmokeFailure("Lava renderer height/mesh mismatch: "+name+" "+field)
        meshes = case.get("meshes")
        if not isinstance(meshes, list) or not meshes:
            raise SmokeFailure("Vacuous lava mesh proof")
        positions = set()
        for mesh in meshes:
            pos = mesh.get("pos")
            if pos in positions:
                raise SmokeFailure("Duplicate lava probe")
            positions.add(pos)
            a, b = mesh.get("observed_height"), mesh.get("expected_height")
            if any(type(v) not in (int,float) or not math.isfinite(v) for v in (a,b)) or abs(a-b)>1e-5 or mesh.get("position_height_equal") is not True:
                raise SmokeFailure("Raw lava height mismatch")
            for vertex in mesh.get("actual_vertices", []):
                if len(vertex)!=3 or any(type(v) not in (int,float) or not math.isfinite(v) for v in vertex):
                    raise SmokeFailure("Invalid lava vertex")
            if mesh.get("top_present") is True:
                if mesh.get("top_matches_position_heights") is not True:
                    raise SmokeFailure("Raw lava corner mismatch")
                expected = mesh.get("expected_top_y", [])
                points = mesh["actual_vertices"][:4]
                if (len(expected)!=4 or len(points)!=4
                    or any(type(h) not in (int,float) or not math.isfinite(h) for h in expected)
                    or any(abs(v[1]-h)>1e-5 for v,h in zip(points,expected))):
                    raise SmokeFailure("Raw lava corner values mismatch")
        tops = sum(m.get("top_present") is True for m in meshes)
        if tops<4 or case.get("top_faces_checked")!=tops or shared_corner_gaps(meshes):
            raise SmokeFailure("Discontinuous or missing lava top faces")
        attachments = case.get("attachment_cells")
        if type(attachments) is not int or (attachments<1 if name.endswith("-decor") else attachments!=0):
            raise SmokeFailure("Missing attachment/native baseline coverage")
        probes = case.get("embeddium_height_probes")
        if type(probes) is not int or probes != (4*len(meshes) if profile=="embeddium" else 0):
            raise SmokeFailure("Missing actual Embeddium height probes")
        screenshot = name+".png"
        if case.get("screenshot")!=screenshot or not valid_png((directory/"lava-surface-screenshots"/screenshot).read_bytes()):
            raise SmokeFailure("Missing current lava screenshot")
    return proof
