#!/usr/bin/env python3
"""Bounded native Factory Placer V2 validation in a fresh nonce-owned integrated world."""
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


SUITE = 'native-factory-placer-v2-reference-v1'
ART_SHA = 'ff079488dd0d1c7434597a6915231d88b81732472bb23ccc8a61235eca27ec2b'
SCREEN_CLASS = 'com.oblixorprime.engineersdecorreforged.client.FactoryPlacerV2ReferenceScreen'
CAPTURES = {'normal.png':(0,0),'row-middle.png':(0,6),'row-bottom.png':(0,12),
            'inverted.png':(1,0),'continuous.png':(2,0),'placed.png':(0,1),
            'reopened.png':(0,1),'reloaded.png':(0,1)}


def require(condition, message):
    if not condition: raise SmokeFailure(message)


def validate_viewport(proof, expected):
    for key,value in expected.items():
        actual=proof.get(key)
        valid=type(actual) in (int,float) if key=='actual_gui_scale' else type(actual) is int
        require(valid and actual==value, f'Requested/actual viewport differs: {key}')


def png(path,width,height,expected_hash):
    require(path.is_file() and not path.is_symlink(), 'Missing native PNG or unsafe symlink')
    require(hashlib.sha256(path.read_bytes()).hexdigest()==expected_hash,'Native PNG hash differs')
    w,h,_=decode_png(path)
    require((w,h)==(width,height),'Native PNG dimensions differ')


def validate_proof(proof,nonce,source_sha,game,logs,exit_code,timed_out,width,height,language,gui_scale=1,source_path=None):
    source=load_source(source_path)
    require(type(proof) is dict,'Native proof must be a JSON object')
    error=proof.get('error')
    detail='; reported failure: '+' '.join(error.split())[:1000] if proof.get('status')=='failed' and isinstance(error,str) and error.strip() else ''
    require(not timed_out and type(exit_code) is int and exit_code==0 and not FATAL_PATTERN.search(logs),'Native client did not finish cleanly'+detail)
    require(not list(game.glob('crash-reports/*')) and not list(game.parent.rglob('hs_err_pid*.log')),'Native client wrote a crash report')
    viewport,scale,crop=expected_geometry(width,height,gui_scale)
    validate_viewport(proof,viewport)
    require(proof.get('suite')==SUITE and proof.get('nonce')==nonce
        and proof.get('executed_source_sha')==source_sha and re.fullmatch(r'[a-f0-9]{40}',source_sha)
        and proof.get('source_art_sha256')==ART_SHA and proof.get('actual_integrated_world') is True
        and proof.get('native_visual_acceptance') is False and language in ('en_us','fr_fr','fr_ca')
        and proof.get('selected_language')==language,'Proof identity, locale or bounded scope differs')
    require(proof.get('status')=='passed_bounded_smoke','Native fixture did not pass'+detail)
    require('error' not in proof,'Passing native proof must not contain a fixture error')
    for key,value in {'menu_slot_count':54,'machine_saved_slot_count':27,'machine_visible_slot_count':18,
                      'route_pickup_server_acks':54,'route_return_server_acks':54}.items():
        require(type(proof.get(key)) is int and proof[key]==value,f'Slot contract differs: {key}')
    routes=proof.get('slot_routes')
    require(type(routes) is list and all(type(i) is int for i in routes) and routes==list(range(54)), 'All 54 real slot routes must pass exactly once in order')
    cases=proof.get('cases')
    require(type(cases) is list and all(type(i) is str for i in cases) and len(cases)==len(CASES) and set(cases)==CASES,'Missing or duplicate native cases')
    validate_server_evidence(proof)
    require(strict_equal(proof.get('control_routes'),CONTROL_ROUTES),'Control server acknowledgments differ')
    require(type(proof.get('manual_trigger_server_acks')) is int and proof['manual_trigger_server_acks']==3,'Manual trigger acknowledgment differs')
    captures=proof.get('captures')
    require(type(captures) is list and len(captures)==len(CAPTURES),'Incomplete native capture matrix')
    seen=set();rings=set();menus={};images={}
    folder=game/'factory-placer-v2-reference-screenshots'
    require(not folder.is_symlink(),'Unsafe screenshot directory symlink')
    tx,ty,tw,th=crop
    for c in captures:
        require(type(c) is dict,'Invalid capture object')
        name=c.get('name')
        require(type(name) is str and name in CAPTURES and name not in seen,'Duplicate, unexpected or unsafe capture name')
        seen.add(name);validate_viewport(c,viewport)
        for key,value in expected_presentation_bounds(width,height,gui_scale).items():
            require(type(c.get(key)) is int and c[key]==value,f'Public presentation bounds differ: {key}')
        for key,value in {'logic':CAPTURES[name][0],'selected_slot':CAPTURES[name][1], 'signal':0, 'width':width,'height':height}.items():
            require(type(c.get(key)) is int and c[key]==value,f'Native capture state differs: {key}')
        require(c.get('screen_class')==SCREEN_CLASS and type(c.get('menu_id')) is int and c['menu_id']>0
            and type(c.get('view_scale')) in (int,float) and math.isfinite(c['view_scale']) and abs(c['view_scale']-scale)<.00001,'Native capture identity/transform differs')
        menus[name]=c['menu_id'];cn=name[:-4]+'-controls.png'
        for key,value in {'controls_name':cn,'controls_x':tx,'controls_y':ty,'controls_width':tw,'controls_height':th}.items():
            require(type(c.get(key)) is type(value) and c[key]==value,'Controls crop geometry differs')
        png(folder/name,width,height,c.get('sha256'));png(folder/cn,tw,th,c.get('controls_sha256'))
        ring=image_evidence(folder/name,folder/cn,width,height,gui_scale)
        require(ring==c.get('frame_exterior_sha256'),'Full artwork digest does not match native pixels')
        rings.add(ring);images[name]=decode_png(folder/name)[2]
        validate_source_pixels(images[name],width,height,gui_scale,source)
    require(len({menus[n] for n in list(CAPTURES)[:6]})==1 and menus['reopened.png']!=menus['placed.png'],'Unexpected menu identity transitions')
    require(len(rings)==1,'Control states changed surrounding artwork pixels')
    require(menus['reloaded.png']==menus['reopened.png'] and menus['reopened.png']>menus['placed.png'],'Reopen/reload menu identity differs')
    validate_world_capture(proof,game,viewport,width,height)
    validate_control_pixels(images,width,height,gui_scale,source)
    require(f'IEDCTR_FACTORY_PLACER_V2_REFERENCE PASSED nonce={nonce}' in logs,'Missing current-run completion marker')
    return proof

CASES = {'all_54_pickup_return_server_confirmed','hidden_9_saved_slots_preserved',
 'repeated_row_selection_server_confirmed','repeated_inversion_and_trigger_mode_server_confirmed',
 'repeated_manual_trigger_server_confirmed','one_block_placement_consumes_one_server_item',
 'close_reaches_server','reopen_preserves_inventory_and_state','resource_reload_preserves_inventory_and_state'}
SENTINELS=[{'slot':18+i,'item':'minecraft:gold_block','count':i+1} for i in range(9)]
PLACEMENT={'block':'minecraft:stone','before_count':2,'after_count':1,'server_confirmed':True,
           'target':[0,80,1],'item_total_before':47,'item_total_after':46,'world_drop_count':0}


def strict_equal(actual,expected):
    if type(actual) is not type(expected):return False
    if isinstance(expected,dict):return actual.keys()==expected.keys() and all(strict_equal(actual[k],v) for k,v in expected.items())
    if isinstance(expected,list):return len(actual)==len(expected) and all(strict_equal(a,b) for a,b in zip(actual,expected))
    return actual==expected


def validate_server_evidence(proof):
    for key,value in {'initial_item_total':46,'final_item_total':46,
         'hidden_slot_sentinels_before':SENTINELS,'hidden_slot_sentinels_after':SENTINELS,'placement':PLACEMENT}.items():
        require(strict_equal(proof.get(key),value),f'Authoritative server evidence differs: {key}')


def validate_world_capture(proof,game,viewport,width,height):
    c=proof.get('world_capture');require(type(c) is dict,'Missing world placement screenshot')
    validate_viewport(c,viewport)
    for key,value in {'name':'placement-world.png','width':width,'height':height,'screen_class':'none',
                      'target_block':'minecraft:stone','target':[0,80,1],'server_confirmed':True}.items():
        require(strict_equal(c.get(key),value),'World placement screenshot metadata differs')
    png(game/'factory-placer-v2-reference-screenshots'/'placement-world.png',width,height,c.get('sha256'))

def f32(value):return struct.unpack('>f',struct.pack('>f',value))[0]


def expected_geometry(width,height,gui_scale):
    require(type(width) is int and type(height) is int and type(gui_scale) is int
            and gui_scale in (1,2,3) and width//gui_scale>=320 and height//gui_scale>=240
            and width<=8192 and height<=8192,'Requested viewport cannot support the explicit GUI scale')
    gw,gh=math.ceil(width/gui_scale),math.ceil(height/gui_scale)
    viewport=dict(requested_gui_scale=gui_scale,actual_gui_scale=gui_scale,gui_width=gw,gui_height=gh,framebuffer_width=width,framebuffer_height=height)
    scale=f32(min(1,(gw-12)/400,(gh-12)/438))
    if scale*400>gw-12 or scale*438>gh-12:scale=struct.unpack('>f',struct.pack('>I',struct.unpack('>I',struct.pack('>f',scale))[0]-1))[0]
    ox,oy=f32((gw-400*scale)/2),f32((gh-438*scale)/2)
    x0,y0=math.ceil((ox+240*scale)*gui_scale),math.ceil((oy+55*scale)*gui_scale)
    x1,y1=math.floor((ox+365*scale)*gui_scale),math.floor((oy+175*scale)*gui_scale)
    return viewport,scale,(x0,y0,x1-x0,y1-y0)


def transform(width,height,gui_scale):
    v,s,_=expected_geometry(width,height,gui_scale)
    ox,oy=f32((v['gui_width']-400*s)/2),f32((v['gui_height']-438*s)/2)
    return lambda x:(ox+x*s)*gui_scale,lambda y:(oy+y*s)*gui_scale


def expected_presentation_bounds(width,height,gui_scale):
    sx,sy=transform(width,height,gui_scale)
    left,top=math.floor(sx(0)/gui_scale),math.floor(sy(0)/gui_scale)
    return dict(presentation_gui_left=left,presentation_gui_top=top,
                presentation_gui_width=math.ceil(sx(400)/gui_scale)-left,presentation_gui_height=math.ceil(sy(438)/gui_scale)-top)


SLOT_ORIGINS=[]
for xs,ys,sw,sh in [([122,218,313,409,504,599],[222,331,440],56,56),
                   ([156,257,358,458,559,660,760,861,961],[681,786,892],80,72),
                   ([167,266,365,465,564,663,763,862,961],[1040],76,72)]:
    SLOT_ORIGINS.extend((math.floor((x+sw/2)/3-8+.5),math.floor((y+sh/2)/3-8+.5)) for y in ys for x in xs)
INDICATORS=[(266,67,276,71),(266,95,276,99),(266,124,276,128),(249,160,254,163),(282,171,310,173),(334,171,360,173)]


def image_evidence(path,crop_path,width,height,gui_scale):
    w,h,pixels=decode_png(path);cw,ch,crop=decode_png(crop_path)
    _,_,(tx,ty,tw,th)=expected_geometry(width,height,gui_scale)
    require((w,h,cw,ch)==(width,height,tw,th),'Decoded PNG dimensions differ')
    actual=b''.join(pixels[(y*w+tx)*4:(y*w+tx+tw)*4] for y in range(ty,ty+th))
    require(actual==crop,'Controls crop is not the native screenshot region')
    sx,sy=transform(width,height,gui_scale)
    rects=INDICATORS+[(x-1,y-1,x+17,y+17) for x,y in SLOT_ORIGINS]
    rects=[(math.floor(sx(l))-2,math.floor(sy(t))-2,math.ceil(sx(r))+2,math.ceil(sy(b))+2) for l,t,r,b in rects]
    digest=hashlib.sha256()
    left,right=math.ceil(sx(0)),math.floor(sx(f32(1199*f32(1/3))))
    for y in range(math.ceil(sy(0)),math.floor(sy(f32(1312*f32(1/3))))):
        cursor=left
        for l,r in sorted((l,r) for l,t,r,b in rects if t<=y<=b):
            if l>cursor:digest.update(pixels[(y*w+cursor)*4:(y*w+min(l,right))*4])
            cursor=max(cursor,r+1)
            if cursor>=right:break
        if cursor<right:digest.update(pixels[(y*w+cursor)*4:(y*w+right)*4])
    return digest.hexdigest()


def validate_control_pixels(images,width,height,gui_scale,source):
    sx,sy=transform(width,height,gui_scale)
    def sample(name,rect):
        l,t,r,b=rect;x=math.floor(sx((l+r)/2));y=math.floor(sy((t+b)/2))
        return images[name][(y*width+x)*4:(y*width+x+1)*4]
    # Unselected source regions must remain stable across every state.
    for i,rect in enumerate(INDICATORS[:3]):
        values={sample(name,rect) for name,(_,selected) in CAPTURES.items() if selected//6!=i}
        require(len(values)==1,'Unselected source row indicator pixels changed')
    for name,(logic,selected) in CAPTURES.items():
        edges=outline_samples(width,height,gui_scale,selected)
        require(bool(edges),'No reliable selected-slot outline raster samples')
        for x,y in edges:
            require(images[name][(y*width+x)*4:(y*width+x+1)*4]==ROW_ACTIVE,'Selected-slot outline pixels differ from observed cursor')
        sw,sh,art=source
        _,view_scale,_=expected_geometry(width,height,gui_scale)
        factor=view_scale*f32(1/3)*gui_scale
        for slot in range(18):
            if slot==selected:continue
            for x,y in outline_samples(width,height,gui_scale,slot):
                tx,ty=math.floor((x+.5-sx(0))/factor),math.floor((y+.5-sy(0))/factor)
                actual=images[name][(y*width+x)*4:(y*width+x+1)*4]
                require(any(actual==art[(yy*sw+xx)*4:(yy*sw+xx+1)*4]
                            for yy in range(max(0,ty-1),min(sh,ty+2))
                            for xx in range(max(0,tx-1),min(sw,tx+2))),
                        'Unselected-slot outline differs from retained source')
        require(sample(name,INDICATORS[3])==bytes((104,25,21,255)),'Settled signal indicator pixels differ from observed state')
        for i,rect in enumerate(INDICATORS[:3]):
            require((sample(name,rect)==ROW_ACTIVE)==(i==selected//6),'Row indicator pixels differ from observed state')
            if i!=selected//6:
                l,t,r,b=rect;x,y=math.floor(sx((l+r)/2)),math.floor(sy((t+b)/2))
                _,scale,_=expected_geometry(width,height,gui_scale)
                factor=scale*f32(1/3)*gui_scale
                tx,ty=math.floor((x+.5-sx(0))/factor),math.floor((y+.5-sy(0))/factor)
                sw,sh,art=source
                require(any(sample(name,rect)==art[(yy*sw+xx)*4:(yy*sw+xx+1)*4]
                            for yy in range(max(0,ty-1),min(sh,ty+2))
                            for xx in range(max(0,tx-1),min(sw,tx+2))),
                        'Unselected row indicator differs from retained source')
        for i,x in enumerate((282,293,304)):
            require(sample(name,(x,171,x+6,173))==(ACTIVE if i==(2 if logic&4 else logic&1) else INACTIVE),'Inversion pixels differ from observed state')
        for i,x in enumerate((334,350)):
            require(sample(name,(x,171,x+10,173))==(ACTIVE if i==bool(logic&2) else INACTIVE),'Trigger pixels differ from observed state')

def selection_rects(width,height,gui_scale,slot):
    """Independent framebuffer contract for the production selection overlay.

    Keep the legacy outline at >=1 physical pixel thickness. Below that,
    quantize a one-pixel ring outside the complete transformed item footprint.
    """
    sx,sy=transform(width,height,gui_scale)
    _,view_scale,_=expected_geometry(width,height,gui_scale)
    x,y=SLOT_ORIGINS[slot]
    if view_scale*gui_scale<1:
        l,t,r,b=math.floor(sx(x)),math.floor(sy(y)),math.ceil(sx(x+16)),math.ceil(sy(y+16))
        return ((l-1,t-1,r+1,t),(r,t,r+1,b),(l-1,b,r+1,b+1),(l-1,t,l,b))
    return tuple((sx(l),sy(t),sx(r),sy(b)) for l,t,r,b in
                 ((x-1,y-1,x+17,y),(x+16,y,x+17,y+16),(x-1,y+16,x+17,y+17),(x-1,y,x,y+16)))


def outline_samples(width,height,gui_scale,slot):
    """Four safely interior samples from the documented framebuffer strips."""
    samples=[]
    for left,top,right,bottom in selection_rects(width,height,gui_scale,slot):
        xs=range(math.ceil(left+.01-.5),math.floor(right-.01-.5)+1)
        ys=range(math.ceil(top+.01-.5),math.floor(bottom-.01-.5)+1)
        if xs and ys:samples.append((xs[len(xs)//2],ys[len(ys)//2]))
    return samples


ROW_ACTIVE=bytes((217,172,85,255))
ACTIVE=bytes((142,214,170,255))
INACTIVE=bytes((65,70,75,255))

CONTROL_ROUTES=[{'control':name,'logic':logic,'selected_slot':slot,'server_confirmed':True}
 for name,logic,slot in ([('ROW_MIDDLE',0,6),('ROW_BOTTOM',0,12),('ROW_TOP',0,0)]*2
 +[('INVERSION',1,0),('INVERSION',4,0),('INVERSION',0,0)]*2+[('TRIGGER_MODE',2,0),('TRIGGER_MODE',0,0)]*2)]


DEFAULT_SOURCE = Path(__file__).resolve().parents[2]/'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/factory_placer_v2_reference/background.png'


_SOURCE_CACHE=None


def load_source(source_path=None):
    global _SOURCE_CACHE
    path=DEFAULT_SOURCE if source_path is None else Path(source_path)
    require(path.is_file() and not path.is_symlink(),'Missing retained source artwork')
    require(hashlib.sha256(path.read_bytes()).hexdigest()==ART_SHA,'Retained source artwork hash differs')
    source=_SOURCE_CACHE if _SOURCE_CACHE is not None else decode_png(path)
    require(source[:2]==(1199,1312),'Retained source artwork dimensions differ')
    _SOURCE_CACHE=source
    return source


def source_samples(width,height,gui_scale):
    """Broad eight-framebuffer-pixel grid, excluding only drawn overlays/items.

    Pixel centers map through the inverse uniform source transform. A one-texel
    neighborhood accounts for single-precision GPU matrix/interpolation rounding
    at texel boundaries. No mismatch quota or RGB tolerance is permitted.
    """
    sx,sy=transform(width,height,gui_scale)
    _,scale,_=expected_geometry(width,height,gui_scale)
    factor=scale*f32(1/3)*gui_scale
    rects=INDICATORS+[(x-1,y-1,x+17,y+17) for x,y in SLOT_ORIGINS]
    rects=[(math.floor(sx(l))-2,math.floor(sy(t))-2,math.ceil(sx(r))+2,math.ceil(sy(b))+2) for l,t,r,b in rects]
    for y in range(math.ceil(sy(0))+2,math.floor(sy(f32(1312*f32(1/3))))-2,8):
        for x in range(math.ceil(sx(0))+2,math.floor(sx(f32(1199*f32(1/3))))-2,8):
            if not any(l<=x<=r and t<=y<=b for l,t,r,b in rects):
                yield x,y,math.floor((x+.5-sx(0))/factor),math.floor((y+.5-sy(0))/factor)


def validate_source_pixels(pixels,width,height,gui_scale,source):
    sw,sh,art=source
    count=0
    for x,y,tx,ty in source_samples(width,height,gui_scale):
        actual=pixels[(y*width+x)*4:(y*width+x+1)*4]
        require(any(actual==art[(yy*sw+xx)*4:(yy*sw+xx+1)*4]
                    for yy in range(max(0,ty-1),min(sh,ty+2))
                    for xx in range(max(0,tx-1),min(sw,tx+2))),
                f'Native exterior pixel differs from retained source at ({x},{y})')
        count+=1
    require(count>=100,'Insufficient retained-source pixel samples')


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



def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--language", choices=("en_us", "fr_fr", "fr_ca"), default="en_us")
    parser.add_argument("--presentation", choices=("v2", "default"), default="v2")
    parser.add_argument("--runtime-jar", type=Path,
                        help="Optional shared normal JAR; Gradle requires equality with the exact-source build")
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
    art = project / "src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/factory_placer_v2_reference/background.png"
    if hashlib.sha256(art.read_bytes()).hexdigest() != ART_SHA:
        parser.error("Retained Factory Placer V2 art changed")
    missing = [name for name in ("java", "Xvfb", "xvfb-run", "xauth") if not shutil.which(name)]
    if missing:
        parser.error("Missing native prerequisites: " + ", ".join(missing))
    (project / "build").mkdir(exist_ok=True)
    evidence = Path(tempfile.mkdtemp(prefix="factory-placer-v2-reference-native-", dir=project / "build"))
    game = evidence / "run"
    game.mkdir()
    nonce = secrets.token_hex(16)
    (game / ".iedctr-client-gameplay-nonce").write_text(nonce, encoding="utf-8")
    (game / "options.txt").write_text(
        "onboardAccessibility:false\ntutorialStep:none\nrenderDistance:2\nsimulationDistance:2\n"
        f"maxFps:60\npauseOnLostFocus:false\nsoundCategory_master:0.0\nguiScale:{args.gui_scale}\n"
        f"lang:{args.language}\n", encoding="utf-8")
    run_name = 'factoryPlacerPackagedClient' if args.presentation == 'v2' else 'factoryPlacerPackagedDefaultClient'
    task_suffix = run_name[0].upper() + run_name[1:]
    init = evidence / "factory_placer.init.gradle"
    init.write_text("""gradle.projectsEvaluated {
    rootProject.extensions.getByName('neoForge').runs.named(System.getenv('IEDCTR_PACKAGED_RUN')) {
        gameDirectory.set(rootProject.file(System.getenv('IEDCTR_GAMEPLAY_DIR')))
        systemProperty 'iedctr.clientGameplay', 'false'
        systemProperty 'iedctr.clientGameplayNonce', System.getenv('IEDCTR_GAMEPLAY_NONCE')
        systemProperty 'iedctr.clientGameplayDirectory', System.getenv('IEDCTR_GAMEPLAY_DIR')
        if (System.getenv('IEDCTR_PACKAGED_PRESENTATION') == 'v2') {
            systemProperty 'iedctr.factoryPlacerV2ReferenceNative', 'true'
        }
        systemProperty 'iedctr.expectedModJar', System.getenv('IEDCTR_EXPECTED_MOD_JAR')
        systemProperty 'iedctr.expectedModJarSha256', System.getenv('IEDCTR_EXPECTED_MOD_JAR_SHA256')
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
               IEDCTR_PACKAGED_RUN=run_name, IEDCTR_PACKAGED_PRESENTATION=args.presentation,
               LIBGL_ALWAYS_SOFTWARE="1", ALSOFT_DRIVERS="null")
    base_gradle = ["bash", str(project / "gradlew"), "--no-daemon", "--stacktrace", "-Pinclude_factory_placer_native_validation=true"]
    if args.runtime_jar is not None:
        supplied = args.runtime_jar.resolve()
        if not supplied.is_file() or supplied.suffix != '.jar':
            parser.error('Supplied normal JAR does not exist')
        base_gradle.append('-Pfactory_placer_runtime_jar=' + str(supplied))
    gradle = base_gradle + ["--init-script", str(init)]
    # The marker is created inside this new xvfb-run session, never against a user's DISPLAY.
    display_guard = evidence / 'owned_display.py'
    display_guard.write_text('''import os, pathlib, re, sys
display = os.environ.get('DISPLAY', '')
assert re.fullmatch(r':\\d+(\\.\\d+)?', display), 'Expected isolated local Xvfb display'
game = pathlib.Path(os.environ['IEDCTR_GAMEPLAY_DIR']).resolve()
nonce = os.environ['IEDCTR_GAMEPLAY_NONCE']
assert re.fullmatch(r'[a-f0-9]{32}', nonce)
assert (game / '.iedctr-client-gameplay-nonce').read_text() == nonce
marker = game / '.iedctr-placer-xvfb'
assert not marker.exists()
marker.write_text(nonce + '\\n' + display)
os.execvpe(sys.argv[1], sys.argv[1:], os.environ)
''', encoding='utf-8')
    print(f"Factory Placer V2 isolated native evidence: {evidence} ({args.width}x{args.height}, GUI scale {args.gui_scale}, {args.language})", flush=True)
    try:
        code, expired = run_bounded(base_gradle + ['verifyFactoryPlacerPackagedRuntime'], project, env,
                                    evidence / 'package.log', args.prepare_timeout)
        require(not expired and code == 0, 'Exact-source packaged runtime preparation failed')
        classpath_path = project / 'build/reports/factory-placer-packaged-runtime.json'
        classpath_report = json.loads(classpath_path.read_text())
        jar = Path(classpath_report['jar_path'])
        require(jar.is_file() and jar.suffix == '.jar' and not jar.is_symlink(), 'Missing packaged runtime JAR')
        require(args.runtime_jar is None or jar.resolve() == args.runtime_jar.resolve(), 'Runtime did not select shared JAR')
        jar_sha = hashlib.sha256(jar.read_bytes()).hexdigest()
        require(jar_sha == classpath_report['jar_sha256'], 'Packaged runtime report SHA differs')
        (evidence / 'packaged-runtime-classpath.json').write_text(json.dumps(classpath_report, indent=2) + '\n')
        (evidence / 'runtime-jar-sha256.txt').write_text(jar_sha + '\n')
        env['IEDCTR_EXPECTED_MOD_JAR'] = str(jar.resolve())
        env['IEDCTR_EXPECTED_MOD_JAR_SHA256'] = jar_sha
        code, expired = run_bounded(gradle + ['prepare' + task_suffix + 'Run'], project, env,
                                    evidence / "prepare.log", args.prepare_timeout)
        if expired or code != 0:
            raise SmokeFailure(f"Factory Placer V2 client preparation failed (exit={code}, timeout={expired})")
        log = evidence / "client.log"
        code, expired = run_bounded(["xvfb-run", "-a", "-s", f"-screen 0 {args.width}x{args.height}x24",
                                    sys.executable, str(display_guard)]
                                    + gradle + ['run' + task_suffix], project, env, log, args.timeout)
        result_name = 'client-factory-placer-v2-reference-result.json' if args.presentation == 'v2' else 'client-factory-placer-packaged-default-result.json'
        proof = json.loads((game / result_name).read_text())
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
        require(hashlib.sha256(jar.read_bytes()).hexdigest() == jar_sha, 'Runtime JAR changed during execution')
        from factory_placer_delivery_evidence import validate_delivery_proof, validate_default_proof
        if args.presentation == 'v2':
            validate_delivery_proof(proof, nonce, source_sha, game, logs, code, expired,
                                    args.width, args.height, args.language, args.gui_scale, project, jar, classpath_report)
        else:
            validate_default_proof(proof, nonce, source_sha, game, logs, code, expired,
                                   args.width, args.height, args.language, jar, classpath_report)
        print('PASS exact-JAR Factory Placer ' + args.presentation + ' qualification. No release or owner visual acceptance is claimed.')
        return 0
    except (SmokeFailure, OSError, ValueError) as error:
        print(f"FAIL: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
