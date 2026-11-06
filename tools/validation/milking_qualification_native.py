#!/usr/bin/env python3
"""Bounded qualification of the existing production Milking GUI, never a new screen.

Synthetic evidence tests exercise this validator, not Minecraft. Native execution
requires a clean committed candidate and an isolated nonce-owned integrated world.
"""
from __future__ import annotations
import argparse
import hashlib
import json
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

SUITE = "native-milking-qualification-v1"
SCREEN_CLASS = "com.oblixorprime.engineersdecorreforged.client.MachineScreen$SmallMilkingMachineScreen"
BACKGROUND_SHA = "871f7c4512227153a4c49591c5facdbc95b1443114cc3206659d6dd524d1326b"
MILK_SHA = "f6f0507deb081a2a83b6bb455abc6237ac2b41f4ab21d43c833e356fb2d9bbe5"
PROFILES = {
    "en-1280x720-scale1": (1280, 720, 1, "en_us"),
    "fr-1280x260-scale1": (1280, 260, 1, "fr_fr"),
    "en-1920x1080-scale1": (1920, 1080, 1, "en_us"),
    "en-1920x1080-scale2": (1920, 1080, 2, "en_us"),
    "en-1920x1080-scale3": (1920, 1080, 3, "en_us"),
}
CASES = {"all_38_pickup_return_server_confirmed", "native_empty_half_full_milk",
         "real_bucket_conversion_conserves_items_and_fluid"}
DEFERRED = {"resource_reload", "close_reopen", "shift_click", "complex_gestures", "physical_os_input", "jei_actions"}
CAPTURES = {"empty.png": 0, "half.png": 2000, "full.png": 4000}


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SmokeFailure(message)


def exact(obj: dict, key: str, expected) -> None:
    actual = obj.get(key)
    # bool is an int in Python; it must never authenticate a count/coordinate.
    types = (int, float) if key == "actual_gui_scale" else (type(expected),)
    require(type(actual) in types and actual == expected, f"Missing/invalid {key}")


def expected_geometry(width: int, height: int, scale: int) -> dict:
    require(all(type(v) is int for v in (width, height, scale)), "Viewport must use integer dimensions/scale")
    require(scale in (1, 2, 3) and width >= 320 * scale and height >= 240 * scale,
            "Requested viewport cannot support explicit GUI scale")
    # Keep this qualification exact: odd framebuffer projection at GUI scale > 1
    # requires separate rasterization evidence and is deliberately not accepted.
    require(width % scale == height % scale == 0, "Viewport must be divisible by GUI scale")
    gw, gh = width // scale, height // scale
    left, top = (gw - 228) // 2, (gh - 228) // 2
    return dict(requested_width=width, requested_height=height, requested_gui_scale=scale,
                actual_gui_scale=scale, gui_width=gw, gui_height=gh,
                framebuffer_width=width, framebuffer_height=height,
                presentation_gui_left=left, presentation_gui_top=top,
                presentation_gui_width=228, presentation_gui_height=228,
                tank_x=(left + 104) * scale, tank_y=(top + 49) * scale,
                tank_width=22 * scale, tank_height=51 * scale)


def slot_position(index: int) -> tuple[int, int]:
    if index == 0:
        return 49, 94
    if index == 1:
        return 163, 94
    require(type(index) is int and 2 <= index < 38, "Invalid menu slot")
    return 26 + ((index - 2) % 9) * 20, 132 + ((index - 2) // 9) * 20 if index < 29 else 195


def validate_routes(proof: dict, geometry: dict) -> None:
    routes = proof.get("slot_routes")
    require(isinstance(routes, list) and len(routes) == 38, "All 38 routes required")
    left, top = geometry["presentation_gui_left"], geometry["presentation_gui_top"]
    for index, route in enumerate(routes):
        require(isinstance(route, dict), "Route must be an object")
        interval = route.get("pickup_to_return_interval_nanos")
        require(type(interval) is int and 300_000_000 <= interval < 900_000_000_000,
                "Route must observe single-click spacing, not double-click")
        destination = 2 if index == 1 else index
        x, y = slot_position(index)
        dx, dy = slot_position(destination)
        for key, value in dict(source_menu_index=index, destination_menu_index=destination,
                               source_x=left+x+8, source_y=top+y+8,
                               destination_x=left+dx+8, destination_y=top+dy+8,
                               item_id="minecraft:bucket" if index == 0 else "minecraft:milk_bucket" if index == 1 else "minecraft:dirt",
                               count=1, pickup_server_ack=True, return_server_ack=True,
                               conserved_items=1, final_fluid_amount=0).items():
            exact(route, key, value)


def validate_conversion(proof: dict) -> None:
    conversion = proof.get("bucket_conversion")
    require(isinstance(conversion, dict), "Missing actual bucket conversion")
    for key, value in dict(native_tick_observed=True, server_confirmed=True, client_confirmed=True,
                           seeded_output=False, fluid_id="minecraft:milk", conserved_milk_mb=1000).items():
        exact(conversion, key, value)
    for phase, expected in {
        "before": dict(amount=1000, input_buckets=1, output_milk_buckets=0, player_items=0, carried_items=0),
        "after": dict(amount=0, input_buckets=0, output_milk_buckets=1, player_items=0, carried_items=0),
    }.items():
        state = conversion.get(phase)
        require(isinstance(state, dict), "Missing conversion snapshot")
        for key, value in expected.items():
            exact(state, key, value)


def safe_png(folder: Path, name: str, expected_hash: str) -> Path:
    path = folder / name
    require(folder.is_dir() and not folder.is_symlink() and path.is_file() and not path.is_symlink(),
            "Missing PNG or unsafe symlink")
    require(isinstance(expected_hash, str) and re.fullmatch(r"[0-9a-f]{64}", expected_hash) is not None,
            "Missing PNG content hash")
    require(hashlib.sha256(path.read_bytes()).hexdigest() == expected_hash, "PNG hash differs")
    return path


def decode_png(path: Path) -> tuple[int, int, bytes]:
    """Decode native RGB/RGBA PNG bytes independently; hashes cannot prove pixels."""
    data = path.read_bytes()
    require(data[:8] == b'\x89PNG\r\n\x1a\n', "Invalid PNG signature")
    pos = 8; compressed = bytearray(); header = None; ended = False
    while pos < len(data):
        require(pos + 12 <= len(data), "Truncated PNG chunk")
        size = struct.unpack('>I', data[pos:pos+4])[0]; kind = data[pos+4:pos+8]
        end = pos + 12 + size
        require(end <= len(data), "Truncated PNG payload")
        payload = data[pos+8:pos+8+size]
        require(zlib.crc32(kind+payload) == struct.unpack('>I', data[pos+8+size:end])[0], "PNG CRC differs")
        if kind == b'IHDR':
            require(header is None and pos == 8 and size == 13, "Invalid PNG header")
            header = struct.unpack('>IIBBBBB', payload)
        elif kind == b'IDAT':
            compressed.extend(payload)
        elif kind == b'IEND':
            require(size == 0 and end == len(data), "Invalid PNG ending")
            ended = True; break
        elif kind[:1].isupper():
            raise SmokeFailure("Unsupported critical PNG chunk")
        pos = end
    require(ended and header is not None, "Incomplete PNG")
    w, h, depth, color, compression, filtering, interlace = header
    require(0 < w <= 8192 and 0 < h <= 8192 and depth == 8 and color in (2, 6)
            and compression == filtering == interlace == 0, "Unsupported native PNG format")
    channels = 3 if color == 2 else 4; stride = w * channels
    dec = zlib.decompressobj()
    try:
        raw = dec.decompress(compressed, (stride+1)*h+1)
    except zlib.error as error:
        raise SmokeFailure("Invalid compressed PNG pixels") from error
    require(dec.eof and not dec.unused_data and len(raw) == (stride+1)*h, "PNG pixel stream length differs")
    rows = []; previous = bytearray(stride)
    def paeth(a, b, c):
        p=a+b-c; pa,pb,pc=abs(p-a),abs(p-b),abs(p-c)
        return a if pa<=pb and pa<=pc else b if pb<=pc else c
    for y in range(h):
        f = raw[y*(stride+1)]; row = bytearray(raw[y*(stride+1)+1:(y+1)*(stride+1)])
        require(f <= 4, "Invalid PNG filter")
        for i in range(stride) if f else ():
            a=row[i-channels] if i>=channels else 0; b=previous[i]; c=previous[i-channels] if i>=channels else 0
            row[i]=(row[i]+(0,a,b,(a+b)//2,paeth(a,b,c))[f]) & 255
        rows.append(bytes(row) if channels == 4 else b''.join(row[i:i+3]+b'\xff' for i in range(0,stride,3)))
        previous = row
    return w, h, b''.join(rows)


def crop_pixels(pixels: bytes, width: int, x: int, y: int, w: int, h: int) -> bytes:
    return b''.join(pixels[(row*width+x)*4:(row*width+x+w)*4] for row in range(y, y+h))


def image_evidence(frame: Path, crop: Path, geometry: dict) -> tuple[bytes, bytes]:
    w, h, pixels = decode_png(frame); cw, ch, tank = decode_png(crop)
    g = geometry
    require((w,h,cw,ch) == (g['framebuffer_width'],g['framebuffer_height'],g['tank_width'],g['tank_height']),
            "Decoded PNG dimensions differ")
    require(crop_pixels(pixels,w,g['tank_x'],g['tank_y'],cw,ch) == tank, "Tank crop is not the native screenshot region")
    scale = g['requested_gui_scale']; left=g['presentation_gui_left']*scale; top=g['presentation_gui_top']*scale
    # Full opaque artwork panel, not just a small ring. No hover exclusion is
    # needed: fixture disables tooltips and moves the cursor outside the panel.
    # Only live tank and actual two amount-label lines (x146..209,y72..88) vary.
    exterior=bytearray()
    for ly in range(228*scale):
        ranges = [(0,228*scale)]
        if 49*scale <= ly < 100*scale:
            ranges = [(0,104*scale),(126*scale,228*scale)]
        if 72*scale <= ly < 89*scale:
            ranges = [(0,104*scale),(126*scale,146*scale),(210*scale,228*scale)]
        for start,end in ranges:
            exterior.extend(pixels[((top+ly)*w+left+start)*4:((top+ly)*w+left+end)*4])
    return tank, bytes(exterior)


def validate_pixels(tanks: dict[str, bytes], exterior: dict[str, bytes], geometry: dict) -> None:
    require(len(set(exterior.values())) == 1, "Fluid states changed exterior artwork pixels")
    empty, half, full = (tanks[n] for n in CAPTURES)
    stride = geometry['tank_width']*4; top_rows=26*geometry['requested_gui_scale']
    split=top_rows*stride
    require(half[:split] == empty[:split], "Half fill changed pixels above bottom-up fill")
    require(half[split:] == full[split:], "Half fill bottom does not match full milk layer")
    # The pinned milk crop has antialiased, near-transparent top/bottom edges.
    # Require substantial density in the interior, not those two-GUI-row margins.
    # Full ROI/crop correspondence and half/full comparisons still include every pixel.
    # This catches empty, one-pixel, tiny-stripe and metadata-only fake distinctions.
    margin = 2 * geometry["requested_gui_scale"]
    for y in range(margin, geometry['tank_height'] - margin):
        a=empty[y*stride:(y+1)*stride]; b=full[y*stride:(y+1)*stride]
        changes=sum(a[i:i+4] != b[i:i+4] for i in range(0,stride,4))
        require(changes >= geometry['tank_width']//2, "Full milk has insufficient decoded interior row coverage")
    require(len(set(tanks.values())) == 3, "Decoded tank states must be distinct")


def validate_proof(proof: dict, nonce: str, source_sha: str, game: Path, logs: str,
                   exit_code: int | None, timed_out: bool, width: int, height: int,
                   language: str, gui_scale: int = 1) -> dict:
    require(isinstance(proof,dict), "Native proof must be an object")
    require(not timed_out and type(exit_code) is int and exit_code == 0 and not FATAL_PATTERN.search(logs),
            "Native client did not finish cleanly")
    require(not list(game.glob('crash-reports/*')) and not list(game.parent.glob('**/hs_err_pid*.log')),
            "Native client wrote a crash report")
    require(re.fullmatch(r'[0-9a-f]{40}',source_sha) is not None and re.fullmatch(r'[0-9a-f]{32}',nonce) is not None,
            "Invalid run/source identity")
    require(language in ('en_us','fr_fr'), "Unsupported language")
    for key,value in dict(suite=SUITE,nonce=nonce,executed_source_sha=source_sha,status='passed_bounded_smoke',
                          source_background_sha256=BACKGROUND_SHA,source_milk_sha256=MILK_SHA,
                          glass_overlay_enabled=False,actual_integrated_world=True,native_visual_acceptance=False,
                          requested_language=language,selected_language=language,menu_slot_count=38,
                          screen_class=SCREEN_CLASS).items():
        exact(proof,key,value)
    require('error' not in proof, "Passing proof cannot contain an error")
    for key,expected in [('cases',CASES),('deferred_checks',DEFERRED)]:
        values=proof.get(key)
        require(isinstance(values,list) and all(isinstance(v,str) for v in values)
                and len(values)==len(expected) and set(values)==expected, f"Invalid {key}")
    geometry=expected_geometry(width,height,gui_scale)
    for key,value in geometry.items():
        if key.startswith(('tank_','presentation_')):
            continue
        exact(proof,key,value)
    validate_routes(proof,geometry); validate_conversion(proof)
    captures=proof.get('captures')
    require(isinstance(captures,list) and len(captures)==3, "Incomplete native capture matrix")
    seen=set(); menus=set(); tanks={}; exterior={}
    for capture in captures:
        require(isinstance(capture,dict), "Capture must be an object")
        name=capture.get('name')
        require(isinstance(name,str) and name in CAPTURES and name not in seen, "Duplicate/unsafe capture name")
        seen.add(name)
        for key,value in geometry.items():
            exact(capture,key,value)
        amount=CAPTURES[name]
        for key,value in dict(width=width,height=height,screen_class=SCREEN_CLASS,amount=amount,capacity=4000,
                              client_fluid_amount=amount,server_amount=amount,fluid_id='minecraft:milk' if amount else 'minecraft:empty',
                              empty_slots_and_cursor=True,server_confirmed=True).items():
            exact(capture,key,value)
        settled=capture.get("settled_client_ticks")
        require(type(settled) is int and 12 <= settled < 1800, "Capture did not settle on synchronized client state")
        menu=capture.get('menu_id')
        require(type(menu) is int and menu>0, "Missing menu identity")
        exact(capture,'server_menu_id',menu); menus.add(menu)
        tank_name=name[:-4]+'-tank.png'; exact(capture,'tank_name',tank_name)
        folder=game/'milking-qualification-screenshots'
        frame=safe_png(folder,name,capture.get('sha256'))
        tank=safe_png(folder,tank_name,capture.get('tank_sha256'))
        tanks[name],exterior[name]=image_evidence(frame,tank,geometry)
    require(len(menus)==1, "Capture menu identity changed")
    for route in proof["slot_routes"]:
        exact(route,"menu_id",next(iter(menus)))
    validate_pixels(tanks,exterior,geometry)
    require(f'IEDCTR_MILKING_QUALIFICATION PASSED nonce={nonce}' in logs, "Missing current-run completion marker")
    return proof


def current_candidate(project: Path) -> str:
    status=subprocess.run(['git','status','--porcelain','--untracked-files=all'],cwd=project,
                          check=True,capture_output=True,text=True).stdout
    require(not status.strip(), "Native execution requires a clean committed candidate; uncommitted files cannot borrow the base SHA")
    sha=subprocess.run(['git','rev-parse','HEAD'],cwd=project,check=True,capture_output=True,text=True).stdout.strip()
    require(re.fullmatch(r'[a-f0-9]{40}',sha) is not None, "Missing exact source SHA")
    return sha


def main() -> int:
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--project',type=Path,default=Path(__file__).resolve().parents[2])
    parser.add_argument('--profile',choices=PROFILES,default='en-1280x720-scale1')
    parser.add_argument('--list-profiles',action='store_true')
    parser.add_argument('--prepare-timeout',type=int,default=600)
    parser.add_argument('--timeout',type=int,default=600)
    args=parser.parse_args()
    if args.list_profiles:
        print(json.dumps(PROFILES,indent=2));return 0
    try:
        require(sys.platform=='linux' and min(args.prepare_timeout,args.timeout)>0, "Requires Linux and positive bounded timeouts")
        project=args.project.resolve()
        from check_milking_qualification import check_preservation
        check_preservation(project)
        source=current_candidate(project)
        missing=[name for name in ('java','Xvfb','xvfb-run','xauth') if not shutil.which(name)]
        require(not missing, "Missing native prerequisites: "+', '.join(missing))
        width,height,scale,language=PROFILES[args.profile]
        (project/'build').mkdir(exist_ok=True)
        evidence=Path(tempfile.mkdtemp(prefix='milking-qualification-native-',dir=project/'build'))
        game=evidence/'run';game.mkdir();nonce=secrets.token_hex(16)
        (game/'.iedctr-client-gameplay-nonce').write_text(nonce,encoding='utf-8')
        (game/'options.txt').write_text('onboardAccessibility:false\ntutorialStep:none\nrenderDistance:2\nsimulationDistance:2\n'
            f'maxFps:60\npauseOnLostFocus:false\nsoundCategory_master:0.0\nguiScale:{scale}\nlang:{language}\n',encoding='utf-8')
        init=evidence/'milking.init.gradle'
        init.write_text("""gradle.projectsEvaluated {
    rootProject.extensions.getByName('neoForge').runs.named('client') {
        gameDirectory.set(rootProject.file(System.getenv('IEDCTR_GAMEPLAY_DIR')))
        systemProperty 'iedctr.clientGameplay', 'false'
        systemProperty 'iedctr.clientGameplayNonce', System.getenv('IEDCTR_GAMEPLAY_NONCE')
        systemProperty 'iedctr.clientGameplayDirectory', System.getenv('IEDCTR_GAMEPLAY_DIR')
        systemProperty 'iedctr.milkingQualificationNative', 'true'
        systemProperty 'iedctr.executedSourceSha', System.getenv('IEDCTR_EXECUTED_SOURCE_SHA')
        systemProperty 'iedctr.requestedGuiScale', System.getenv('IEDCTR_GUI_SCALE')
        systemProperty 'iedctr.requestedWidth', System.getenv('IEDCTR_SCREEN_WIDTH')
        systemProperty 'iedctr.requestedHeight', System.getenv('IEDCTR_SCREEN_HEIGHT')
        systemProperty 'iedctr.requestedLanguage', System.getenv('IEDCTR_GUI_LANGUAGE')
        programArgument '--width'
        programArgument System.getenv('IEDCTR_SCREEN_WIDTH')
        programArgument '--height'
        programArgument System.getenv('IEDCTR_SCREEN_HEIGHT')
    }
}
""",encoding='utf-8')
        env=dict(os.environ,IEDCTR_GAMEPLAY_DIR=str(game),IEDCTR_GAMEPLAY_NONCE=nonce,
            IEDCTR_EXECUTED_SOURCE_SHA=source,IEDCTR_SCREEN_WIDTH=str(width),IEDCTR_SCREEN_HEIGHT=str(height),
            IEDCTR_GUI_SCALE=str(scale),IEDCTR_GUI_LANGUAGE=language,LIBGL_ALWAYS_SOFTWARE='1',ALSOFT_DRIVERS='null')
        gradle=['bash',str(project/'gradlew'),'--no-daemon','--init-script',str(init),'--stacktrace']
        print(f'Milking isolated native evidence: {evidence} ({args.profile})',flush=True)
        code,expired=run_bounded(gradle+['prepareClientRun'],project,env,evidence/'prepare.log',args.prepare_timeout)
        require(not expired and code==0, f'Milking client preparation failed (exit={code}, timeout={expired})')
        log=evidence/'client.log'
        code,expired=run_bounded(['xvfb-run','-a','-s',f'-screen 0 {width}x{height}x24']+gradle+['runClient'],
                                 project,env,log,args.timeout)
        proof=json.loads((game/'client-milking-qualification-result.json').read_text())
        logs=log.with_suffix('.observed.log').read_text(errors='replace')
        for name in ('latest.log','debug.log'):
            runtime=game/'logs'/name
            if runtime.is_file():logs+='\n'+runtime.read_text(errors='replace')
        require(current_candidate(project)==source, "Candidate changed during execution")
        validate_proof(proof,nonce,source,game,logs,code,expired,width,height,language,scale)
        print('PASS bounded native Milking qualification. Full visual/input acceptance is not claimed.')
        return 0
    except (SmokeFailure,OSError,ValueError,subprocess.CalledProcessError) as error:
        print(f'FAIL: {error}',file=sys.stderr);return 1


if __name__=='__main__':
    raise SystemExit(main())
