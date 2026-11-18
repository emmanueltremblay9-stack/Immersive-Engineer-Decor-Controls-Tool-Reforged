#!/usr/bin/env python3
"""Exact-JAR Heavy world captures; visual and pixel-CTM acceptance remain manual."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import subprocess
import sys
import tempfile
from client_mixin_smoke import FATAL_PATTERN, SmokeFailure, run_bounded
from factory_placer_v2_reference_native import png

CASES = ([f"orientation_{axis}_{roll}" for axis in ("down","up","north","south","west","east") for roll in range(4)]
         + [f"anchor_{face}" for face in ("down","up","north","south","west","east")]
         + ["axial_join","axial_removed","branch_join","branch_removed"])


def require(condition, message):
    if not condition:
        raise SmokeFailure(message)


def validate(proof, nonce, sha, jar, jar_sha, game, width, height, language):
    expected = dict(suite='native-heavy-scenes-v1', status='passed_bounded_capture', nonce=nonce,
                    executed_source_sha=sha, actual_integrated_world=True, visual_acceptance=False,
                    pixel_ctm_verified=False, physical_item_placement=False, mesh_refresh_acceptance=False,
                    width=width, height=height, selected_language=language, scene_setup='server setBlock; no runtime/art edits')
    for key, value in expected.items():
        require(type(proof.get(key)) is type(value) and proof[key] == value, 'Proof differs: ' + key)
    require('error' not in proof, 'Passing proof contains an error')
    runtime = proof.get('packaged_runtime', {})
    require(runtime.get('jar_path') == str(jar.resolve()) and runtime.get('jar_sha256') == jar_sha,
            'Loaded JAR identity differs')
    require(runtime.get('fixture_free_jar') is True and runtime.get('separate_harness_module') is True,
            'Harness isolation missing')
    require(type(runtime.get('active_models')) is list and len(runtime['active_models']) == 86, 'Active Heavy model coverage missing')
    require(type(runtime.get('active_textures')) is list and len(runtime['active_textures']) == 3, 'Active Heavy textures missing')
    origins = runtime.get('class_origins', [])
    classes = {row.get('class') for row in origins}
    for name in ['block.HeavyDoubleTSupportBlock','block.HeavyDoubleTTopology',
                 'block.HeavyDoubleTOrientation','client.HeavyDoubleTConnectedModels']:
        require('com.oblixorprime.engineersdecorreforged.'+name in classes, 'Heavy packaged origin missing: '+name)
    captures = proof.get('captures', [])
    require([row.get('case') for row in captures] == CASES, 'Native case coverage/order differs')
    for index, row in enumerate(captures):
        require(row.get('client_synced') is True and type(row.get('stable_ticks')) is int and row['stable_ticks'] >= 40,
                'Client state did not settle')
        require(type(row.get('rendered_frames')) is int and row['rendered_frames'] >= 40, 'Insufficient rendered frames')
        require(type(row.get('quad_count')) is int and row['quad_count'] > 0, 'No baked geometry')
        require(re.fullmatch('[a-f0-9]{64}', row.get('baked_quads_sha256','')) is not None, 'Missing quad fingerprint')
        axis_order = ['down','up','north','south','west','east']
        axis = axis_order[index//4] if index < 24 else 'up'
        roll = index%4 if index < 24 else 0
        kind = 'isolated' if index < 24 else ('anchor' if index < 30 else ['axial','remove','branch','remove'][index-30])
        require(row.get('axis') == axis and type(row.get('roll')) is int and row['roll'] == roll and row.get('kind') == kind,
                'Scene orientation/kind differs')
        masks = dict(anchor_mask=(1 << (index-24)) if 24 <= index < 30 else 0,
                     axial_mask=2 if index == 30 else 0, junction_mask=32 if index == 32 else 0)
        for key,value in masks.items():
            require(type(row.get(key)) is int and row[key] == value, 'Scene topology differs: '+key)
        expected_path = 'heavy-captures/'+row['case']+'.png'
        require(row.get('capture') == expected_path, 'Capture path differs')
        png(game/expected_path, width, height, row.get('sha256'))
    for before, after in [(30,31),(32,33)]:
        require(captures[before]['baked_quads_sha256'] != captures[after]['baked_quads_sha256'],
                'Client baked model unchanged on removal')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--language', choices=['en_us','fr_fr'], default='en_us')
    parser.add_argument('--width', type=int, default=1280)
    parser.add_argument('--height', type=int, default=720)
    parser.add_argument('--prepare-timeout', type=int, default=900)
    parser.add_argument('--timeout', type=int, default=900)
    args = parser.parse_args()
    project = Path(__file__).resolve().parents[2]
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
    evidence = Path(tempfile.mkdtemp(prefix='heavy-native-',dir=project/'build'))
    game=evidence/'run';game.mkdir();nonce=secrets.token_hex(16)
    (game/'.iedctr-client-gameplay-nonce').write_text(nonce)
    (game/'options.txt').write_text('onboardAccessibility:false\ntutorialStep:none\nrenderDistance:2\nsimulationDistance:2\n'
        'renderClouds:false\nentityShadows:false\nmaxFps:60\npauseOnLostFocus:false\nsoundCategory_master:0.0\nguiScale:1\nlang:'+args.language+'\n')
    init=evidence/'heavy.init.gradle'
    init.write_text("""gradle.projectsEvaluated {
 rootProject.extensions.getByName('neoForge').runs.named('heavyPackagedDefaultClient') {
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
marker=game/'.iedctr-heavy-xvfb'
assert not marker.exists()
marker.write_text(nonce+'\n'+os.environ['DISPLAY'])
os.execvpe(sys.argv[1],sys.argv[1:],os.environ)
""")
    env=dict(os.environ,IEDCTR_GAMEPLAY_DIR=str(game),IEDCTR_GAMEPLAY_NONCE=nonce,IEDCTR_EXECUTED_SOURCE_SHA=sha,
             IEDCTR_SCREEN_WIDTH=str(args.width),IEDCTR_SCREEN_HEIGHT=str(args.height),LIBGL_ALWAYS_SOFTWARE='1',ALSOFT_DRIVERS='null')
    base=['bash',str(project/'gradlew'),'--no-daemon','--stacktrace']
    print('Independent native Heavy evidence: '+str(evidence),flush=True)
    try:
        code,expired=run_bounded(base+['jar'],project,env,evidence/'jar.log',args.prepare_timeout)
        require(code==0 and not expired,'Normal JAR build failed')
        base+=['-Pinclude_heavy_packaged_validation=true']
        code,expired=run_bounded(base+['verifyHeavyPackagedRuntime'],project,env,evidence/'package.log',args.prepare_timeout)
        require(code==0 and not expired,'Separate harness/classpath validation failed')
        report=json.loads((project/'build/reports/heavy-packaged-runtime.json').read_text())
        jar=Path(report['jar_path']);jar_sha=hashlib.sha256(jar.read_bytes()).hexdigest()
        require(jar_sha==report['jar_sha256'],'Classpath JAR hash differs')
        (evidence/'packaged-runtime-classpath.json').write_text(json.dumps(report,indent=2))
        env.update(IEDCTR_EXPECTED_MOD_JAR=str(jar.resolve()),IEDCTR_EXPECTED_MOD_JAR_SHA256=jar_sha)
        gradle=base+['--init-script',str(init)]
        code,expired=run_bounded(gradle+['prepareHeavyPackagedDefaultClientRun'],project,env,evidence/'prepare.log',args.prepare_timeout)
        require(code==0 and not expired,'Native preparation failed')
        log=evidence/'client.log'
        code,expired=run_bounded(['xvfb-run','-a','-s',f'-screen 0 {args.width}x{args.height}x24',sys.executable,str(guard)]
                                +gradle+['runHeavyPackagedDefaultClient'],project,env,log,args.timeout)
        logs=log.read_text(errors='replace')
        require(code==0 and not expired and not FATAL_PATTERN.search(logs)
                and 'IEDCTR_HEAVY PASSED nonce='+nonce in logs,'Native exit/log gate failed')
        require(not list(game.glob('crash-reports/*')) and not list(evidence.rglob('hs_err_pid*.log')), 'Native crash report present')
        proof=json.loads((game/'heavy-proof.json').read_text())
        validate(proof,nonce,sha,jar,jar_sha,game,args.width,args.height,args.language)
        final_sha = subprocess.check_output(['git','rev-parse','HEAD'],cwd=project,text=True).strip()
        require(clean() and final_sha == sha and hashlib.sha256(jar.read_bytes()).hexdigest()==jar_sha,
                'Source/JAR changed during run')
        print('PASS bounded native Heavy captures; no pixel CTM, visual or chunk-mesh acceptance claimed.')
        return 0
    except (SmokeFailure,OSError,ValueError,KeyError) as error:
        print('FAIL: '+str(error),file=sys.stderr);return 1


if __name__=='__main__':
    raise SystemExit(main())
