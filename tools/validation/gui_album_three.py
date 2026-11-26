#!/usr/bin/env python3
"""Capture seven ordinary machine GUIs in a real client; no gameplay acceptance verdict."""
import argparse
import hashlib
import json
import math
import os
from pathlib import Path
import re
import secrets
import shutil
import subprocess
import sys
import tempfile
import urllib.request
from client_mixin_smoke import FATAL_PATTERN, SmokeFailure, run_bounded
from factory_placer_v2_reference_native import png, decode_png




def require(condition, message):
    if not condition:
        raise SmokeFailure(message)


CASES = ['factory-hopper', 'metal-crafting-table', 'small-electrical-furnace', 'small-waste-incinerator', 'labeled-crate', 'factory-dropper', 'small-lab-furnace']
SCREENS = [
    'com.oblixorprime.engineersdecorreforged.client.FactoryStudyScreen',
    'com.oblixorprime.engineersdecorreforged.client.MachineScreen$MetalCraftingTableScreen',
    'com.oblixorprime.engineersdecorreforged.client.MachineScreen$SmallElectricalFurnaceScreen',
    'com.oblixorprime.engineersdecorreforged.client.MachineScreen$SmallWasteIncineratorScreen',
    'com.oblixorprime.engineersdecorreforged.client.LabeledCrateScreen',
    'com.oblixorprime.engineersdecorreforged.client.FactoryStudyScreen',
    'com.oblixorprime.engineersdecorreforged.client.FactoryStudyScreen',
]
V3_LAYOUTS = {0: (18, 54), 5: (15, 51), 6: (11, 47)}


def v3_outline(width, height, gui_scale, row):
    """Project the shared 316x312 Factory Study transform into screenshot pixels."""
    logical_width = math.ceil(width / gui_scale)
    logical_height = math.ceil(height / gui_scale)
    margin = min(6.0, (min(logical_width, logical_height) - 1.0) / 2.0)
    view_scale = min(1.0, (logical_width - 2 * margin) / 316.0,
                     (logical_height - 2 * margin) / 312.0)
    origin_x = (logical_width - 316.0 * view_scale) / 2.0
    origin_y = (logical_height - 312.0 * view_scale) / 2.0
    x = origin_x + 33.0 * view_scale
    y = origin_y + (67.0 + 25.0 * row) * view_scale
    left = round(x * gui_scale)
    top = round(y * gui_scale)
    right = round((x + 18.0 * view_scale) * gui_scale) - 1
    bottom = round((y + 18.0 * view_scale) * gui_scale) - 1
    return left, top, right, bottom


def validate_selection_pixels(selections,game,width,height,scale):
    """Compare the same border in selected/unselected real screenshots, not art mockups.

    A top/bottom corner-marker delta proves a rendered marker reaches the expected row;
    it is not an exact-color aesthetic approval or exhaustive 18-slot test.
    """
    require(type(scale) is int and scale in (1,2) and width>=316*scale and height>=312*scale,
            'Selection panel outside viewport')
    images=[]
    for record in selections:
        w,h,pixels=decode_png(game/record['capture'])
        require((w,h)==(width,height),'Selection viewport differs')
        images.append(pixels)
    for row in range(3):
        active=images[row];inactive=images[(row+1)%3];other_inactive=images[(row+2)%3]
        left,top,right,bottom=v3_outline(width,height,scale,row)
        border={(x,top) for x in range(left,right+1)} | {(x,bottom) for x in range(left,right+1)}
        border|={(left,y) for y in range(top,bottom+1)} | {(right,y) for y in range(top,bottom+1)}
        changed=0
        for x,y in border:
            offset=(y*width+x)*4
            if (active[offset:offset+3]!=inactive[offset:offset+3]
                and inactive[offset:offset+3]==other_inactive[offset:offset+3]): changed+=1
        require(changed>=16*scale*scale,'Factory Study V3 Hopper outline missing/misaligned at row '+str(row))


def validate(proof,nonce,sha,jar,jar_sha,game,width,height,gui_scale=2):
    for key,value in dict(suite='gui-album-three-v2',status='observed',nonce=nonce,
                         executed_source_sha=sha,visual_acceptance=False,physical_os_input=True,human_input=False,
                         input_method="synthetic XTest pointer positioning only; no clicks",synthetic_button_events=0).items():
        require(type(proof.get(key)) is type(value) and proof[key]==value,'Proof differs: '+key)
    runtime=proof.get('packaged_runtime',{})
    require(runtime.get('jar_path')==str(jar.resolve()) and runtime.get('jar_sha256')==jar_sha,'Wrong production JAR')
    require(runtime.get('fixture_free_jar') is True and runtime.get('separate_harness_module') is True,'Isolation missing')
    cases=proof.get('cases',[])
    require([x.get('case') for x in cases]==CASES,'Missing screens')
    for index,row in enumerate(cases):
        require(row.get('screen')==SCREENS[index],'Wrong screen')
        require(row.get('server_menu_acknowledged') is True,'Missing server menu acknowledgement')
        require(row.get('gui_scale') == gui_scale,'Wrong native GUI scale')
        require(type(row.get('visible_nonempty_slots')) is int and row['visible_nonempty_slots'] > 0,'Empty visual evidence')
        if index in V3_LAYOUTS:
            machine_slots,total_slots=V3_LAYOUTS[index]
            require(row.get('visible_machine_slots')==machine_slots,'Wrong V3 machine slot count')
            require(row.get('total_menu_slots')==total_slots,'Wrong V3 total menu slot count')
            require(row.get('visible_nonempty_slots')==total_slots,'V3 GUI does not show every expected slot item')
        require(re.fullmatch('[0-9a-f]{64}',row.get('screen_class_sha256','')),'Screen provenance missing')
        require(row.get('capture')==CASES[index]+'.png','Unexpected capture name')
        png(game/row['capture'],width,height,row.get('capture_sha256'))

    selections=proof.get('hopper_selections',[])
    require(len(selections)==3,'Missing three Hopper selection rows')
    for row,c in enumerate(selections):
        require(all(type(c.get(k)) is int for k in ('server_selection','client_selection','ring_x','ring_y','visible_nonempty_slots')),'Selection integer evidence missing')
        require(all(c.get(k)==cases[0].get(k) for k in ('screen','case','screen_class_sha256')),'Selection screen provenance differs')
        require(c.get('server_selection')==row*6 and c.get('client_selection')==row*6,
                'Hopper selection server/client mismatch')
        require(c.get('ring_x')==33 and c.get('ring_y')==67+25*row,'Hopper ring/slot alignment differs')
        require(c.get('selection_source')=='server-seeded ContainerData; not user selection','Selection origin differs')
        require(c.get('server_menu_acknowledged') is True and c.get('gui_scale')==gui_scale,'Selection menu/scale mismatch')
        require(c.get('visible_nonempty_slots')==54,'Hopper inventory not full')
        require(type(c.get('selection_render_frames')) is int and c['selection_render_frames']>=3,'Selection not rendered stably')
        require(c.get('capture')=='factory-hopper'+('' if row==0 else '-selection-row-'+str(row))+'.png','Unexpected selection capture')
        png(game/c['capture'],width,height,c.get('capture_sha256'))
    validate_selection_pixels(selections,game,width,height,gui_scale)
    require(selections[0]==cases[0],'Initial Hopper observation differs')
    require(cases[5].get('visible_nonempty_slots')==51,'Dropper 15 machine slots and 36 player slots are not all present')
    require(cases[5].get('dropper_filter_gate')==2,'Dropper capture did not use the AND filter gate')
    require(cases[5].get('dropper_external_powered') is False,'Dropper capture fixture was powered')


def bind_built_jar(project, expected_hash, env):
    """Bind verified artifact before Gradle evaluates any diagnostic init properties."""
    matches = [p.resolve() for p in (project/'build/libs').glob('*.jar')
               if p.is_file() and hashlib.sha256(p.read_bytes()).hexdigest() == expected_hash]
    require(len(matches) == 1, 'Expected exactly one built normal JAR matching candidate hash')
    jar = matches[0]
    env.update(IEDCTR_EXPECTED_MOD_JAR=str(jar), IEDCTR_EXPECTED_MOD_JAR_SHA256=expected_hash)
    return jar


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--expected-jar-sha256',required=True)
    parser.add_argument('--language', choices=['en_us','fr_fr'], default='en_us')
    parser.add_argument('--width', type=int, default=1280)
    parser.add_argument('--gui-scale', type=int, choices=[1,2], default=2)
    parser.add_argument('--height', type=int, default=720)
    parser.add_argument('--prepare-timeout', type=int, default=900)
    parser.add_argument('--timeout', type=int, default=900)
    args = parser.parse_args()
    project = Path(__file__).resolve().parents[2]
    require(re.fullmatch('[a-f0-9]{64}',args.expected_jar_sha256),'Exact normal JAR hash required')
    if sys.platform != 'linux' or args.width < 320 or args.height < 260 or min(args.timeout,args.prepare_timeout) <= 0:
        parser.error('Linux, at least 320x260, positive timeouts required')
    for name in ['java','Xvfb','xvfb-run','xauth']:
        if not shutil.which(name): parser.error('Missing prerequisite ' + name)
    def clean():
        return not subprocess.check_output(['git','status','--porcelain','--untracked-files=all'],cwd=project,text=True).strip()
    require(clean(), 'Requires clean committed exact-source checkout')
    sha = subprocess.check_output(['git','rev-parse','HEAD'],cwd=project,text=True).strip()
    require(re.fullmatch('[a-f0-9]{40}',sha), 'Invalid source SHA')
    (project/'build').mkdir(exist_ok=True)
    evidence = Path(tempfile.mkdtemp(prefix='gui-album-three-native-',dir=project/'build'))
    game=evidence/'run';game.mkdir();nonce=secrets.token_hex(16)
    (game/'.iedctr-client-gameplay-nonce').write_text(nonce)
    (game/'options.txt').write_text('onboardAccessibility:false\ntutorialStep:none\nrenderDistance:2\nsimulationDistance:2\nmaxFps:60\npauseOnLostFocus:false\nsoundCategory_master:0.0\nguiScale:'+str(args.gui_scale)+'\nlang:'+args.language+'\n')
    init=evidence/'gui-album-three.init.gradle'
    init.write_text("""gradle.projectsEvaluated {
 rootProject.sourceSets.factoryPlacerNativeValidation.java.srcDir(rootProject.file('src/guiAlbumThree/java'))
 rootProject.extensions.getByName('neoForge').runs.named('crateLabelPackagedClient') {
  systemProperty 'iedctr.crateLabelPackagedNative', 'false'
  systemProperty 'iedctr.guiAlbumThree', 'true'
  gameDirectory.set(rootProject.file(System.getenv('IEDCTR_GAMEPLAY_DIR')))
  systemProperty 'iedctr.clientGameplayNonce', System.getenv('IEDCTR_GAMEPLAY_NONCE')
  systemProperty 'iedctr.clientGameplayDirectory', System.getenv('IEDCTR_GAMEPLAY_DIR')
  systemProperty 'iedctr.executedSourceSha', System.getenv('IEDCTR_EXECUTED_SOURCE_SHA')
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
marker=game/'.iedctr-gui-album-three-xvfb'
assert not marker.exists()
marker.write_text(nonce+'\n'+os.environ['DISPLAY'])
os.execvpe(sys.argv[1],sys.argv[1:],os.environ)
""")
    env=dict(os.environ,IEDCTR_GAMEPLAY_DIR=str(game),IEDCTR_GAMEPLAY_NONCE=nonce,IEDCTR_EXECUTED_SOURCE_SHA=sha,
             IEDCTR_SCREEN_WIDTH=str(args.width),IEDCTR_SCREEN_HEIGHT=str(args.height),LIBGL_ALWAYS_SOFTWARE='1',ALSOFT_DRIVERS='null')
    base=['bash',str(project/'gradlew'),'--no-daemon','--stacktrace']
    print('GUI album capture evidence: '+str(evidence),flush=True)
    try:
        code,expired=run_bounded(base+['jar'],project,env,evidence/'jar.log',args.prepare_timeout)
        require(code==0 and not expired,'Normal JAR build failed')
        jar=bind_built_jar(project,args.expected_jar_sha256,env)
        jar_sha=args.expected_jar_sha256
        base+=['-Pinclude_factory_placer_native_validation=true',
               '-Pfactory_placer_runtime_jar='+str(jar),'--init-script',str(init)]
        code,expired=run_bounded(base+['verifyFactoryPlacerPackagedRuntime'],project,env,evidence/'package.log',args.prepare_timeout)
        require(code==0 and not expired,'Separate harness/classpath validation failed')
        report=json.loads((project/'build/reports/factory-placer-packaged-runtime.json').read_text())
        require(Path(report['jar_path']).resolve()==jar,'Verified runtime path differs from bound normal JAR')
        require(hashlib.sha256(jar.read_bytes()).hexdigest()==report['jar_sha256']==jar_sha,
                'Exact candidate JAR hash differs')
        (evidence/'packaged-runtime-classpath.json').write_text(json.dumps(report,indent=2))
        gradle=base
        code,expired=run_bounded(gradle+['prepareCrateLabelPackagedClientRun'],project,env,evidence/'prepare.log',args.prepare_timeout)
        require(code==0 and not expired,'Native preparation failed')
        log=evidence/'client.log'
        code,expired=run_bounded(['xvfb-run','-a','-s',f'-screen 0 {args.width}x{args.height}x24',sys.executable,str(guard)]
                                +gradle+['runCrateLabelPackagedClient'],project,env,log,args.timeout)
        logs=log.read_text(errors='replace')
        require(code==0 and not expired and not FATAL_PATTERN.search(logs)
                and 'IEDCTR_GUI_ALBUM_THREE OBSERVED nonce='+nonce in logs,'Native exit/log gate failed')
        proof=json.loads((game/'gui-album-three-proof.json').read_text())
        validate(proof,nonce,sha,jar,jar_sha,game,args.width,args.height,args.gui_scale)
        final_sha = subprocess.check_output(['git','rev-parse','HEAD'],cwd=project,text=True).strip()
        require(clean() and final_sha == sha and hashlib.sha256(jar.read_bytes()).hexdigest()==jar_sha,
                'Source/JAR changed during run')
        print('OBSERVED seven native machine screens, including Hopper/Dropper/Lab Furnace V3 and three Hopper selection rows; screenshots still require independent review. Not gameplay acceptance.')
        return 0
    except (SmokeFailure,OSError,ValueError,KeyError) as error:
        print('FAIL: '+str(error),file=sys.stderr);return 1


if __name__=='__main__':
    raise SystemExit(main())
