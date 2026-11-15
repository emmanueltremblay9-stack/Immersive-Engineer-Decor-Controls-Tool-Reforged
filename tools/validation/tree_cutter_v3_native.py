#!/usr/bin/env python3
"""Exact-source/exact-JAR bounded native Tree Cutter GUI qualification, GUI crops only."""
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
from client_mixin_smoke import FATAL_PATTERN, SmokeFailure, run_bounded
from factory_placer_v2_reference_native import decode_png

ART_SHA = 'bdab10d19c0da44c113f84f1330513f4ff71e81dfe7d3eab602ce27202c0650d'
ART_PATH = Path(__file__).resolve().parents[2] / 'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/tree_cutter_v3_reference/background.png'
NAMES = ['normal','transferred','reopened','reloaded']
SCREEN = 'com.oblixorprime.engineersdecorreforged.client.TreeCutterV3ReferenceScreen'


def require(condition, message):
    if not condition:
        raise SmokeFailure(message)


def f32(value):
    return struct.unpack('f', struct.pack('f',value))[0]


def geometry(width,height,scale):
    require(all(type(v) is int for v in [width,height,scale]) and scale in (1,2,3)
            and width//scale >= 320 and height//scale >= 240,'Invalid actual viewport/scale')
    gw,gh=math.ceil(width/scale),math.ceil(height/scale)
    available=min(gw,gh)-12
    view=f32(min(1,available/418))
    if view*418 > available:
        bits=struct.unpack('I',struct.pack('f',view))[0]
        view=struct.unpack('f',struct.pack('I',bits-1))[0]
    ox,oy=f32((gw-418*view)/2),f32((gh-418*view)/2)
    x,y=math.ceil(ox*scale),math.ceil(oy*scale)
    return x,y,math.floor((ox+418*view)*scale)-x,math.floor((oy+418*view)*scale)-y,ox,oy,view


_SOURCE_CACHE = {}

def source_pixels(path):
    require(path.is_file() and not path.is_symlink(),'Missing retained source')
    require(hashlib.sha256(path.read_bytes()).hexdigest()==ART_SHA,'Retained source hash differs')
    if ART_SHA in _SOURCE_CACHE:
        return _SOURCE_CACHE[ART_SHA]
    w,h,pixels=decode_png(path)
    require((w,h)==(1254,1254),'Retained dimensions differ')
    _SOURCE_CACHE[ART_SHA]=pixels
    return pixels


def validate_pixels(path,width,height,scale,source):
    x,y,cw,ch,ox,oy,view=geometry(width,height,scale)
    w,h,pixels=decode_png(path)
    require((w,h)==(cw,ch),'Capture is not a strict GUI-only crop')
    factor=scale*view*f32(1/3)
    samples=0;colors=set()
    # Broad source-bound coverage outside every item/hover zone and the dynamic energy bar.
    for cy in range(5,ch-5,7):
        for cx in range(5,cw-5,7):
            sx=(cx+x+.5-ox*scale)/factor;sy=(cy+y+.5-oy*scale)/factor
            if 220<sx<970 and 545<sy<885:continue
            if 880<sx<1025 and 265<sy<405:continue
            if 830<sx<1040 and 160<sy<205:continue
            px=pixels[(cy*w+cx)*4:(cy*w+cx+1)*4]
            ix,iy=math.floor(sx),math.floor(sy)
            # One source texel tolerance for float rasterization at reduced GUI scales.
            candidates=[source[(yy*1254+xx)*4:(yy*1254+xx+1)*4]
                        for yy in range(max(0,iy-1),min(1254,iy+2))
                        for xx in range(max(0,ix-1),min(1254,ix+2))]
            require(px in candidates,'Native GUI pixels differ from retained source')
            colors.add(px);samples+=1
    require(samples>=500 and len(colors)>=25,'Insufficient source-bound pixel coverage')


def validate(proof,nonce,sha,jar,jar_sha,game,width,height,language,scale,source_path=ART_PATH):
    require(type(proof) is dict and 'error' not in proof,'Invalid or failed native proof')
    require('rejected_insertion_server_acks' not in proof,'Unsupported output packet acknowledgement claim')
    expected=dict(suite='native-tree-cutter-v3-v2',status='passed_bounded_smoke',nonce=nonce,
        executed_source_sha=sha,selected_language=language,width=width,height=height,
        menu_slot_count=37,native_visual_acceptance=False,opened_via_native_block_interaction=1,
        pickup_server_acks=37,return_server_acks=37,rejected_output_attempts_state_preserved=37,
        final_item_total=1,resource_reload_confirmed=True,output_packet_processing_observed=False,physical_slot_clicks=111,
        input_method='XTest physical block opening and slot clicks; observed GLFW cursor and server inventory transitions')
    for key,value in expected.items():
        require(type(proof.get(key)) is type(value) and proof[key]==value,'Proof differs: '+key)
    require(type(proof.get('gui_scale')) in (float,int) and proof['gui_scale']==scale,'GUI scale differs')
    require(proof.get('slot_routes')==list(range(37)) and all(type(i) is int for i in proof['slot_routes']),'37 routes missing')
    runtime=proof.get('packaged_runtime',{})
    require(type(runtime) is dict and runtime.get('jar_path')==str(jar.resolve()) and runtime.get('jar_sha256')==jar_sha,'JAR identity differs')
    require(runtime.get('fixture_free_jar') is True and runtime.get('separate_harness_module') is True,'Isolation differs')
    require(runtime.get('active_texture_sha256')==ART_SHA and runtime.get('active_texture_pack')=='mod/immersive_engineer_decor_controls_tool_reforged','Active artwork differs')
    origins=runtime.get('class_origins',[])
    expected_classes={SCREEN,'com.oblixorprime.engineersdecorreforged.EngineersDecorReforged',
        'com.oblixorprime.engineersdecorreforged.utility.TreeCutterV3ReferenceGeometry',
        'com.oblixorprime.engineersdecorreforged.menu.MachineMenu',
        'com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity'}
    require(type(origins) is list and len(origins)==5 and all(type(o) is dict for o in origins) and {o.get('class') for o in origins}==expected_classes,'Class provenance coverage differs')
    for origin in origins:
        require(all(type(origin.get(k)) is str and origin[k] for k in ('resource_uri','code_source_uri','sha256'))
                and re.fullmatch('[a-f0-9]{64}',origin['sha256']),'Class provenance missing')
    captures=proof.get('captures',[])
    require(type(captures) is list and all(type(c) is dict for c in captures) and [c.get('name') for c in captures]==NAMES,'Capture coverage differs')
    source=source_pixels(source_path)
    x,y,w,h,*_=geometry(width,height,scale)
    for c in captures:
        require(c.get('file')=='tree-'+c['name']+'.png','Unexpected capture path')
        for k,v in dict(x=x,y=y,width=w,height=h,screen_class=SCREEN).items():
            require(type(c.get(k)) is type(v) and c[k]==v,'Capture field differs: '+k)
        require(type(c.get('menu_id')) is int and c['menu_id']>0,'Missing menu identity')
        path=game/c['file'];require(path.is_file() and not path.is_symlink(),'Missing or linked capture')
        require(hashlib.sha256(path.read_bytes()).hexdigest()==c.get('sha256'),'Capture hash differs')
        validate_pixels(path,width,height,scale,source)
    require(captures[0]['menu_id']==captures[1]['menu_id'] and captures[2]['menu_id']!=captures[1]['menu_id']
            and captures[3]['menu_id']==captures[2]['menu_id'],'Reopen/reload menu identity differs')
    require(not (game/'crash-reports').exists(),'Crash report directory present')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--language', choices=['en_us','fr_fr'], default='en_us')
    parser.add_argument('--width', type=int, default=1280)
    parser.add_argument('--height', type=int, default=720)
    parser.add_argument('--gui-scale', type=int, choices=[1,2,3], default=1)
    parser.add_argument('--prepare-timeout', type=int, default=900)
    parser.add_argument('--timeout', type=int, default=900)
    args = parser.parse_args()
    project = Path(__file__).resolve().parents[2]
    if sys.platform != 'linux' or args.width < 320 or args.height < 260 or min(args.timeout,args.prepare_timeout) <= 0:
        parser.error('Linux, at least 320x260, positive timeouts required')
    geometry(args.width,args.height,args.gui_scale)
    for name in ['java','Xvfb','xvfb-run','xauth']:
        if not shutil.which(name): parser.error('Missing prerequisite ' + name)
    def clean():
        return not subprocess.check_output(['git','status','--porcelain','--untracked-files=all'],cwd=project,text=True).strip()
    require(clean(), 'Requires clean committed exact-source checkout')
    sha = subprocess.check_output(['git','rev-parse','HEAD'],cwd=project,text=True).strip()
    require(re.fullmatch('[a-f0-9]{40}',sha), 'Invalid source SHA')
    art = project/'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/tree_cutter_v3_reference/background.png'
    require(hashlib.sha256(art.read_bytes()).hexdigest()==ART_SHA,'Retained v3 changed')
    (project/'build').mkdir(exist_ok=True)
    evidence = Path(tempfile.mkdtemp(prefix='tree-cutter-native-',dir=project/'build'))
    game=evidence/'run';game.mkdir();nonce=secrets.token_hex(16)
    (game/'.iedctr-client-gameplay-nonce').write_text(nonce)
    (game/'options.txt').write_text('onboardAccessibility:false\ntutorialStep:none\nrenderDistance:2\nsimulationDistance:2\n'
        'maxFps:60\npauseOnLostFocus:false\nsoundCategory_master:0.0\nguiScale:'+str(args.gui_scale)+'\nlang:'+args.language+'\n')
    init=evidence/'tree.init.gradle'
    init.write_text("""gradle.projectsEvaluated {
 rootProject.extensions.getByName('neoForge').runs.named('treeCutterPackagedClient') {
  gameDirectory.set(rootProject.file(System.getenv('IEDCTR_GAMEPLAY_DIR')))
  systemProperty 'iedctr.clientGameplayNonce', System.getenv('IEDCTR_GAMEPLAY_NONCE')
  systemProperty 'iedctr.clientGameplayDirectory', System.getenv('IEDCTR_GAMEPLAY_DIR')
  systemProperty 'iedctr.executedSourceSha', System.getenv('IEDCTR_EXECUTED_SOURCE_SHA')
  systemProperty 'iedctr.requestedWidth', System.getenv('IEDCTR_SCREEN_WIDTH')
  systemProperty 'iedctr.requestedHeight', System.getenv('IEDCTR_SCREEN_HEIGHT')
  systemProperty 'iedctr.requestedGuiScale', System.getenv('IEDCTR_GUI_SCALE')
  systemProperty 'iedctr.expectedModJar', System.getenv('IEDCTR_EXPECTED_MOD_JAR')
  systemProperty 'iedctr.expectedModJarSha256', System.getenv('IEDCTR_EXPECTED_MOD_JAR_SHA256')
  programArgument '--width'
  programArgument System.getenv('IEDCTR_SCREEN_WIDTH')
  programArgument '--height'
  programArgument System.getenv('IEDCTR_SCREEN_HEIGHT')
 }
}
""")
    guard=evidence/'owned_display.py'
    guard.write_text(r"""import os,pathlib,re,sys
assert re.fullmatch(r':\d+(\.\d+)?',os.environ.get('DISPLAY',''))
game=pathlib.Path(os.environ['IEDCTR_GAMEPLAY_DIR'])
nonce=os.environ['IEDCTR_GAMEPLAY_NONCE']
assert (game/'.iedctr-client-gameplay-nonce').read_text()==nonce
marker=game/'.iedctr-tree-xvfb'
assert not marker.exists()
marker.write_text(nonce+'\n'+os.environ['DISPLAY'])
os.execvpe(sys.argv[1],sys.argv[1:],os.environ)
""")
    env=dict(os.environ,IEDCTR_GAMEPLAY_DIR=str(game),IEDCTR_GAMEPLAY_NONCE=nonce,IEDCTR_EXECUTED_SOURCE_SHA=sha,
             IEDCTR_GUI_SCALE=str(args.gui_scale),IEDCTR_SCREEN_WIDTH=str(args.width),IEDCTR_SCREEN_HEIGHT=str(args.height),LIBGL_ALWAYS_SOFTWARE='1',ALSOFT_DRIVERS='null')
    base=['bash',str(project/'gradlew'),'--no-daemon','--stacktrace']
    print('Independent native Tree Cutter evidence: '+str(evidence),flush=True)
    try:
        code,expired=run_bounded(base+['jar'],project,env,evidence/'jar.log',args.prepare_timeout)
        require(code==0 and not expired,'Normal JAR build failed')
        base+=['-Pinclude_factory_placer_native_validation=true']
        code,expired=run_bounded(base+['verifyFactoryPlacerPackagedRuntime'],project,env,evidence/'package.log',args.prepare_timeout)
        require(code==0 and not expired,'Separate harness/classpath validation failed')
        report=json.loads((project/'build/reports/factory-placer-packaged-runtime.json').read_text())
        jar=Path(report['jar_path']);jar_sha=hashlib.sha256(jar.read_bytes()).hexdigest()
        require(jar_sha==report['jar_sha256'],'Classpath JAR hash differs')
        (evidence/'packaged-runtime-classpath.json').write_text(json.dumps(report,indent=2))
        env.update(IEDCTR_EXPECTED_MOD_JAR=str(jar.resolve()),IEDCTR_EXPECTED_MOD_JAR_SHA256=jar_sha)
        gradle=base+['--init-script',str(init)]
        code,expired=run_bounded(gradle+['prepareTreeCutterPackagedClientRun'],project,env,evidence/'prepare.log',args.prepare_timeout)
        require(code==0 and not expired,'Native preparation failed')
        log=evidence/'client.log'
        code,expired=run_bounded(['xvfb-run','-a','-s',f'-screen 0 {args.width}x{args.height}x24',sys.executable,str(guard)]
                                +gradle+['runTreeCutterPackagedClient'],project,env,log,args.timeout)
        logs=log.read_text(errors='replace')
        require(code==0 and not expired and not FATAL_PATTERN.search(logs)
                and 'IEDCTR_TREE_PACKAGED PASSED nonce='+nonce in logs,'Native exit/log gate failed')
        proof=json.loads((game/'tree-proof.json').read_text())
        validate(proof,nonce,sha,jar,jar_sha,game,args.width,args.height,args.language,args.gui_scale)
        require(clean() and hashlib.sha256(jar.read_bytes()).hexdigest()==jar_sha,'Source/JAR changed during run')
        print('PASS bounded native default Tree Cutter; not owner visual acceptance or complete modpack qualification.')
        return 0
    except (SmokeFailure,OSError,ValueError,KeyError) as error:
        print('FAIL: '+str(error),file=sys.stderr);return 1


if __name__=='__main__':
    raise SystemExit(main())
