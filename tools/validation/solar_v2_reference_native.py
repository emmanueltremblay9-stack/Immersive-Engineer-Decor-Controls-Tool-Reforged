#!/usr/bin/env python3
"""Bounded native Solar V2 validation in a fresh nonce-owned integrated world."""
from __future__ import annotations
import argparse
import hashlib
import json
import math
import os
from pathlib import Path
import re
import secrets
import shutil
import struct
import subprocess
import sys
import tempfile
import zlib
from client_mixin_smoke import FATAL_PATTERN, SmokeFailure, run_bounded

SUITE = "native-solar-v2-reference-v1"
ART_SHA = "d702051b09e09cd7c9ebd7ea874356ba82324db07bcb0657cf8ac66bdf54c796"
SCREEN_CLASS = "com.oblixorprime.engineersdecorreforged.client.SolarV2ReferenceScreen"
CASES = {"all_37_pickup_return_server_confirmed", "native_empty_half_full_fe",
         "close_reaches_server", "reopen_preserves_energy_and_geometry"}
CAPTURES = {"empty.png": 0, "half.png": 1, "full.png": 2}
CASES.add("cell_acceptance_and_real_fe_charging_server_confirmed")


def expected_geometry(width: int, height: int, gui_scale: int) -> tuple[dict, float, tuple[int, int, int, int]]:
    if (type(width) is not int or type(height) is not int or width < 320 or height < 240
            or type(gui_scale) is not int or gui_scale not in (1, 2, 3)
            or width // gui_scale < 320 or height // gui_scale < 240):
        raise SmokeFailure("Requested viewport cannot support the explicit GUI scale")
    gw, gh = math.ceil(width / gui_scale), math.ceil(height / gui_scale)
    viewport = dict(requested_gui_scale=gui_scale, actual_gui_scale=gui_scale,
                    gui_width=gw, gui_height=gh, framebuffer_width=width, framebuffer_height=height)
    # Match Java's float fit/origin rounding, including its nextDown guard.
    def f32(value: float) -> float:
        return struct.unpack(">f", struct.pack(">f", value))[0]
    scale = f32(min(1, (gw - 12) / 418, (gh - 12) / 418))
    if scale * 418 > gw - 12 or scale * 418 > gh - 12:
        bits = struct.unpack(">I", struct.pack(">f", scale))[0]
        scale = struct.unpack(">f", struct.pack(">I", bits - 1))[0]
    ox, oy = f32((gw - 418 * scale) / 2), f32((gh - 418 * scale) / 2)
    # GuiGraphics scales the integer GUI scissor, not its pre-rounded bounds.
    x0, y0 = math.ceil(ox + 49 * scale) * gui_scale, math.ceil(oy + 196 * scale) * gui_scale
    x1, y1 = math.floor(ox + 332 * scale) * gui_scale, math.floor(oy + 208 * scale) * gui_scale
    return viewport, scale, (x0, y0, x1 - x0, y1 - y0)


def expected_presentation_bounds(width: int, height: int, gui_scale: int) -> dict:
    """Public container bounds cover the transformed canvas, rounded outwards in GUI pixels."""
    viewport, scale, _ = expected_geometry(width, height, gui_scale)
    def f32(value: float) -> float:
        return struct.unpack(">f", struct.pack(">f", value))[0]
    ox = f32((viewport["gui_width"] - 418 * scale) / 2)
    oy = f32((viewport["gui_height"] - 418 * scale) / 2)
    left, top = math.floor(ox), math.floor(oy)
    return dict(presentation_gui_left=left, presentation_gui_top=top,
                presentation_gui_width=math.ceil(ox + 418 * scale) - left,
                presentation_gui_height=math.ceil(oy + 418 * scale) - top)


def validate_viewport(proof: dict, expected: dict) -> None:
    for key, value in expected.items():
        actual = proof.get(key)
        valid_type = type(actual) in (int, float) if key == "actual_gui_scale" else type(actual) is int
        if not valid_type or actual != value:
            raise SmokeFailure(f"Requested/actual GUI scale or viewport differs: {key}")


def png(path: Path, width: int, height: int, expected_hash: str) -> None:
    if not path.is_file() or path.is_symlink():
        raise SmokeFailure("Missing native PNG or unsafe symlink")
    data = path.read_bytes()
    if (len(data) < 33 or data[:16] != b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR"
            or struct.unpack(">II", data[16:24]) != (width, height)
            or hashlib.sha256(data).hexdigest() != expected_hash):
        raise SmokeFailure("Native PNG hash/dimensions differ from evidence")


def validate_proof(proof: dict, nonce: str, source_sha: str, game: Path,
                   logs: str, exit_code: int | None, timed_out: bool,
                   width: int, height: int, language: str, gui_scale: int = 1) -> dict:
    if not isinstance(proof, dict):
        raise SmokeFailure("Native proof must be a JSON object")
    # Keep the fixture's concrete failure visible even when its exception also
    # triggers the generic fatal-log/exit checks. This never relaxes acceptance.
    error = proof.get("error")
    detail = ("; reported failure: " + " ".join(error.split())[:1000]
              if proof.get("status") == "failed" and isinstance(error, str) and error.strip() else "")
    if timed_out or exit_code != 0 or FATAL_PATTERN.search(logs):
        raise SmokeFailure("Native client did not finish cleanly" + detail)
    if list(game.glob("crash-reports/*")) or list(game.parent.rglob("hs_err_pid*.log")):
        raise SmokeFailure("Native client wrote a crash report")
    viewport, scale, (tx, ty, tw, th) = expected_geometry(width, height, gui_scale)
    validate_viewport(proof, viewport)
    if (proof.get("suite") != SUITE or proof.get("nonce") != nonce
            or proof.get("executed_source_sha") != source_sha
            or not re.fullmatch(r"[a-f0-9]{40}", source_sha)
            or proof.get("source_art_sha256") != ART_SHA
            or proof.get("actual_integrated_world") is not True
            or proof.get("native_visual_acceptance") is not False
            or type(proof.get("menu_slot_count")) is not int or proof["menu_slot_count"] != 37
            or language not in ("en_us", "fr_fr") or proof.get("selected_language") != language):
        raise SmokeFailure("Solar V2 proof identity, locale, bounded scope or slot contract differs")
    if proof.get("status") != "passed_bounded_smoke":
        raise SmokeFailure("Native fixture did not pass" + detail)
    if "error" in proof:
        raise SmokeFailure("Passing native proof must not contain a fixture error")
    if (proof.get("cell_item")!="immersiveengineering:capacitor_lv"
            or any(type(proof.get(k)) is not int for k in ("cell_initial_buffer_fe","cell_initial_item_fe","cell_charged_fe","cell_remaining_buffer_fe"))
            or proof["cell_initial_buffer_fe"]!=1000 or proof["cell_initial_item_fe"]!=0
            or not 0<proof["cell_charged_fe"]<=1000 or proof["cell_remaining_buffer_fe"]<0
            or proof["cell_charged_fe"]+proof["cell_remaining_buffer_fe"]!=1000):
        raise SmokeFailure("Missing real Cell charge and FE conservation evidence")
    routes = proof.get("slot_routes")
    if not isinstance(routes, list) or any(type(i) is not int for i in routes) or routes != list(range(37)):
        raise SmokeFailure("All 37 real slot routes must pass exactly once in order")
    cases = proof.get("cases")
    if not isinstance(cases, list) or any(not isinstance(c, str) for c in cases) or len(cases) != len(CASES) or set(cases) != CASES:
        raise SmokeFailure("Missing or duplicate native cases")
    captures = proof.get("captures")
    if not isinstance(captures, list) or len(captures) != len(CAPTURES):
        raise SmokeFailure("Incomplete native capture matrix")
    seen, menu_ids, hashes, ring_hashes, capacities = set(), set(), {}, set(), set()
    for capture in captures:
        if not isinstance(capture, dict):
            raise SmokeFailure("Invalid capture object")
        validate_viewport(capture, viewport)
        for key, expected in expected_presentation_bounds(width, height, gui_scale).items():
            if type(capture.get(key)) is not int or capture[key] != expected:
                raise SmokeFailure(f"Public presentation bounds differ: {key}")
        name = capture.get("name")
        if name not in CAPTURES or name in seen:
            raise SmokeFailure("Duplicate, unexpected or unsafe capture name")
        seen.add(name)
        capacity = capture.get("capacity")
        if (type(capacity) is not int or capacity <= 0
                or type(capture.get("amount")) is not int
                or capture["amount"] != capacity * CAPTURES[name] // 2
                or type(capture.get("comparator")) is not int or capture["comparator"]!=math.ceil(CAPTURES[name]*7.5)
                or type(capture.get("sync_granularity_fe")) is not int or capture["sync_granularity_fe"]!=100
                or capture.get("screen_class") != SCREEN_CLASS
                or type(capture.get("menu_id")) is not int or capture["menu_id"] <= 0
                or type(capture.get("view_scale")) not in (int, float)
                or not math.isfinite(capture["view_scale"])
                or abs(capture["view_scale"] - scale) > .00001
                or type(capture.get("width")) is not int or capture["width"] != width
                or type(capture.get("height")) is not int or capture["height"] != height):
            raise SmokeFailure("Native capture state or transform differs")
        folder = game / "solar-v2-reference-screenshots"
        png(folder / name, width, height, capture.get("sha256"))
        energy_name = name[:-4] + "-energy.png"
        if (capture.get("energy_name") != energy_name
                or type(capture.get("energy_x")) is not int or capture["energy_x"] != tx
                or type(capture.get("energy_y")) is not int or capture["energy_y"] != ty
                or type(capture.get("energy_width")) is not int or capture["energy_width"] != tw
                or type(capture.get("energy_height")) is not int or capture["energy_height"] != th):
            raise SmokeFailure("FE energy crop geometry differs")
        png(folder / energy_name, tw, th, capture.get("energy_sha256"))
        ring = capture.get("frame_exterior_sha256")
        if not isinstance(ring, str) or not re.fullmatch(r"[a-f0-9]{64}", ring):
            raise SmokeFailure("Missing native full artwork exterior digest")
        actual_ring=image_evidence(folder/name,folder/energy_name,width,height,gui_scale)
        if actual_ring!=ring:raise SmokeFailure("Full artwork digest does not match native pixels")
        menu_ids.add(capture["menu_id"])
        capacities.add(capacity)
        ring_hashes.add(ring)
        hashes[name] = capture["energy_sha256"]
    if len(menu_ids) != 1 or len(capacities) != 1:
        raise SmokeFailure("FE energy captures changed menu identity or capacity")
    if len(ring_hashes) != 1:
        raise SmokeFailure("FE energy states changed surrounding artwork pixels")
    samples={}
    for name in CAPTURES:
        w,h,pixels=decode_png(game/'solar-v2-reference-screenshots'/name.replace('.png','-energy.png'))
        samples[name]=[pixels[((h//2)*w+x)*4:((h//2)*w+x+1)*4] for x in (w//4,3*w//4)]
    orange=bytes((242,164,50,255))
    if (samples['full.png']!=[orange,orange] or samples['half.png'][0]!=orange
            or samples['half.png'][1]!=samples['empty.png'][1]
            or orange in samples['empty.png']):
        raise SmokeFailure("FE meter does not match empty/half/full horizontal fill")
    if len(set(hashes.values())) != 3:
        raise SmokeFailure("Native energy crops did not distinguish empty/half/full")
    if f"IEDCTR_SOLAR_V2_REFERENCE PASSED nonce={nonce}" not in logs:
        raise SmokeFailure("Missing current-run completion marker")
    return proof


SLOT_ORIGINS = ((344, 124), (60, 257), (96, 257), (131, 257), (166, 257), (201, 257), (237, 257), (272, 257), (308, 257), (343, 257), (60, 289), (96, 289), (131, 289), (166, 289), (201, 289), (237, 289), (272, 289), (308, 289), (343, 289), (60, 321), (96, 321), (131, 321), (166, 321), (201, 321), (237, 321), (272, 321), (308, 321), (343, 321), (60, 365), (96, 365), (131, 365), (166, 365), (201, 365), (237, 365), (272, 365), (308, 365), (343, 365))

def decode_png(path: Path) -> tuple[int, int, bytes]:
    """Strict dependency-free decoder for native 8-bit RGB/RGBA noninterlaced PNG."""
    data=path.read_bytes()
    if data[:8]!=b'\x89PNG\r\n\x1a\n':raise SmokeFailure("Invalid PNG signature")
    pos=8; compressed=bytearray(); header=None; ended=False
    while pos<len(data):
        if pos+12>len(data):raise SmokeFailure("Truncated PNG chunk")
        size=struct.unpack('>I',data[pos:pos+4])[0];kind=data[pos+4:pos+8]
        end=pos+12+size
        if end>len(data):raise SmokeFailure("Truncated PNG payload")
        payload=data[pos+8:pos+8+size]
        if zlib.crc32(kind+payload)!=struct.unpack('>I',data[pos+8+size:end])[0]:raise SmokeFailure("PNG CRC differs")
        if kind==b'IHDR':
            if header is not None or pos!=8 or size!=13:raise SmokeFailure("Invalid PNG header")
            header=struct.unpack('>IIBBBBB',payload)
        elif kind==b'IDAT':compressed.extend(payload)
        elif kind==b'IEND':
            if size or end!=len(data):raise SmokeFailure("Invalid PNG ending")
            ended=True;break
        elif kind[:1].isupper():raise SmokeFailure("Unsupported critical PNG chunk")
        pos=end
    if not ended or header is None:raise SmokeFailure("Incomplete PNG")
    w,h,depth,color,compression,filtering,interlace=header
    if not (0<w<=8192 and 0<h<=8192 and depth==8 and color in (2,6) and compression==filtering==interlace==0):raise SmokeFailure("Unsupported native PNG format")
    channels=3 if color==2 else 4;stride=w*channels
    dec=zlib.decompressobj();raw=dec.decompress(compressed,(stride+1)*h+1)
    if not dec.eof or dec.unused_data or len(raw)!=(stride+1)*h:raise SmokeFailure("PNG pixel stream length differs")
    rows=[];previous=bytearray(stride)
    def paeth(a,b,c):
        p=a+b-c;pa,pb,pc=abs(p-a),abs(p-b),abs(p-c)
        return a if pa<=pb and pa<=pc else b if pb<=pc else c
    for y in range(h):
        f=raw[y*(stride+1)];row=bytearray(raw[y*(stride+1)+1:(y+1)*(stride+1)])
        if f>4:raise SmokeFailure("Invalid PNG filter")
        for i in range(stride) if f else ():
            a=row[i-channels] if i>=channels else 0;b=previous[i];c=previous[i-channels] if i>=channels else 0
            predictor=(0,a,b,(a+b)//2,paeth(a,b,c))[f]
            row[i]=(row[i]+predictor)&255
        rows.append(bytes(row) if channels==4 else b''.join(row[i:i+3]+b'\xff' for i in range(0,stride,3)))
        previous=row
    return w,h,b''.join(rows)


def image_evidence(path: Path, crop_path: Path, width: int, height: int, gui_scale: int) -> str:
    w,h,pixels=decode_png(path);cw,ch,crop=decode_png(crop_path)
    viewport,scale,(tx,ty,tw,th)=expected_geometry(width,height,gui_scale)
    if (w,h,cw,ch)!=(width,height,tw,th):raise SmokeFailure("Decoded PNG dimensions differ")
    actual=b''.join(pixels[(y*w+tx)*4:(y*w+tx+tw)*4] for y in range(ty,ty+th))
    if actual!=crop:raise SmokeFailure("Energy crop is not the native screenshot region")
    def f32(v):return struct.unpack('>f',struct.pack('>f',v))[0]
    ox=f32((viewport['gui_width']-418*scale)/2);oy=f32((viewport['gui_height']-418*scale)/2)
    sx=lambda x:(ox+x*scale)*gui_scale
    sy=lambda y:(oy+y*scale)*gui_scale
    rects=[(49,196,332,208)]+[(x,y,x+16,y+16) for x,y in SLOT_ORIGINS]
    rects=[(math.floor(sx(l))-2,math.floor(sy(t))-2,math.ceil(sx(r))+2,math.ceil(sy(b))+2) for l,t,r,b in rects]
    digest=hashlib.sha256()
    for y in range(math.ceil(sy(0)),math.floor(sy(418))):
        spans=[(l,r) for l,t,r,b in rects if t<=y<=b]
        for x in range(math.ceil(sx(0)),math.floor(sx(418))):
            if not any(l<=x<=r for l,r in spans):digest.update(pixels[(y*w+x)*4:(y*w+x+1)*4])
    return digest.hexdigest()

def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--language", choices=("en_us", "fr_fr"), default="en_us")
    parser.add_argument("--width", type=int, default=1280)
    parser.add_argument("--height", type=int, default=720)
    parser.add_argument("--gui-scale", type=int, choices=(1, 2, 3), default=1)
    parser.add_argument("--prepare-timeout", type=int, default=600)
    parser.add_argument("--timeout", type=int, default=600)
    args = parser.parse_args()
    if sys.platform != "linux" or min(args.prepare_timeout, args.timeout) <= 0:
        parser.error("Requires Linux and positive bounded timeouts")
    try:
        expected_geometry(args.width, args.height, args.gui_scale)
    except SmokeFailure as error:
        parser.error(str(error))
    project = args.project.resolve()
    # No uncommitted candidate may borrow the passing status of its base commit.
    clean = subprocess.run(["git", "status", "--porcelain", "--untracked-files=all"], cwd=project,
                           check=True, capture_output=True, text=True)
    if clean.stdout.strip():
        parser.error("Requires a clean committed candidate; base SHA alone cannot identify uncommitted code")
    source_sha = subprocess.run(["git", "rev-parse", "HEAD"], cwd=project, check=True,
                                capture_output=True, text=True).stdout.strip()
    if not re.fullmatch(r"[a-f0-9]{40}", source_sha):
        parser.error("Missing exact source SHA")
    art = project / "src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/solar_v2_reference/background.png"
    if hashlib.sha256(art.read_bytes()).hexdigest() != ART_SHA:
        parser.error("Retained Solar V2 art changed")
    missing = [name for name in ("java", "Xvfb", "xvfb-run", "xauth") if not shutil.which(name)]
    if missing:
        parser.error("Missing native prerequisites: " + ", ".join(missing))
    (project / "build").mkdir(exist_ok=True)
    evidence = Path(tempfile.mkdtemp(prefix="solar-v2-reference-native-", dir=project / "build"))
    game = evidence / "run"
    game.mkdir()
    nonce = secrets.token_hex(16)
    (game / ".iedctr-client-gameplay-nonce").write_text(nonce, encoding="utf-8")
    (game / "options.txt").write_text(
        "onboardAccessibility:false\ntutorialStep:none\nrenderDistance:2\nsimulationDistance:2\n"
        f"maxFps:60\npauseOnLostFocus:false\nsoundCategory_master:0.0\nguiScale:{args.gui_scale}\n"
        f"lang:{args.language}\n", encoding="utf-8")
    init = evidence / "solar.init.gradle"
    init.write_text("""gradle.projectsEvaluated {
    rootProject.extensions.getByName('neoForge').runs.named('client') {
        gameDirectory.set(rootProject.file(System.getenv('IEDCTR_GAMEPLAY_DIR')))
        systemProperty 'iedctr.clientGameplay', 'false'
        systemProperty 'iedctr.clientGameplayNonce', System.getenv('IEDCTR_GAMEPLAY_NONCE')
        systemProperty 'iedctr.clientGameplayDirectory', System.getenv('IEDCTR_GAMEPLAY_DIR')
        systemProperty 'iedctr.solarV2ReferenceNative', 'true'
        systemProperty 'iedctr.dev.solarV2Reference', 'true'
        systemProperty 'iedctr.executedSourceSha', System.getenv('IEDCTR_EXECUTED_SOURCE_SHA')
        systemProperty 'iedctr.requestedGuiScale', System.getenv('IEDCTR_GUI_SCALE')
        systemProperty 'iedctr.requestedWidth', System.getenv('IEDCTR_SCREEN_WIDTH')
        systemProperty 'iedctr.requestedHeight', System.getenv('IEDCTR_SCREEN_HEIGHT')
        programArgument '--width'
        programArgument System.getenv('IEDCTR_SCREEN_WIDTH')
        programArgument '--height'
        programArgument System.getenv('IEDCTR_SCREEN_HEIGHT')
    }
}
""", encoding="utf-8")
    env = dict(os.environ, IEDCTR_GAMEPLAY_DIR=str(game), IEDCTR_GAMEPLAY_NONCE=nonce,
               IEDCTR_EXECUTED_SOURCE_SHA=source_sha, IEDCTR_SCREEN_WIDTH=str(args.width),
               IEDCTR_SCREEN_HEIGHT=str(args.height), IEDCTR_GUI_SCALE=str(args.gui_scale),
               LIBGL_ALWAYS_SOFTWARE="1", ALSOFT_DRIVERS="null")
    gradle = ["bash", str(project / "gradlew"), "--no-daemon", "--init-script", str(init), "--stacktrace"]
    print(f"Solar V2 isolated native evidence: {evidence} ({args.width}x{args.height}, GUI scale {args.gui_scale}, {args.language})", flush=True)
    try:
        code, expired = run_bounded(gradle + ["prepareClientRun"], project, env,
                                    evidence / "prepare.log", args.prepare_timeout)
        if expired or code != 0:
            raise SmokeFailure(f"Solar V2 client preparation failed (exit={code}, timeout={expired})")
        log = evidence / "client.log"
        code, expired = run_bounded(["xvfb-run", "-a", "-s", f"-screen 0 {args.width}x{args.height}x24"]
                                    + gradle + ["runClient"], project, env, log, args.timeout)
        proof = json.loads((game / "client-solar-v2-reference-result.json").read_text())
        logs = log.with_suffix(".observed.log").read_text(errors="replace")
        for name in ("latest.log", "debug.log"):
            runtime = game / "logs" / name
            if runtime.is_file():
                logs += "\n" + runtime.read_text(errors="replace")
        final_status = subprocess.run(["git", "status", "--porcelain", "--untracked-files=all"], cwd=project,
                                      check=True, capture_output=True, text=True).stdout.strip()
        final_sha = subprocess.run(["git", "rev-parse", "HEAD"], cwd=project, check=True,
                                   capture_output=True, text=True).stdout.strip()
        if final_status or final_sha != source_sha:
            raise SmokeFailure("Candidate changed during native execution; source identity is not exact")
        validate_proof(proof, nonce, source_sha, game, logs, code, expired,
                       args.width, args.height, args.language, args.gui_scale)
        print("PASS bounded native Solar V2 smoke. Full visual/input acceptance is not claimed.")
        return 0
    except (SmokeFailure, OSError, ValueError) as error:
        print(f"FAIL: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
