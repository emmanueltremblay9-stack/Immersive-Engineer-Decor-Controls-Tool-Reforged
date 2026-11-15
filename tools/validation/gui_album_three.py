#!/usr/bin/env python3
"""Capture six ordinary machine GUIs in a real client; no gameplay acceptance verdict."""
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
import urllib.request
from client_mixin_smoke import FATAL_PATTERN, SmokeFailure, run_bounded
from factory_placer_v2_reference_native import png




def require(condition, message):
    if not condition:
        raise SmokeFailure(message)


CASES = ['factory-hopper', 'metal-crafting-table', 'small-electrical-furnace', 'small-waste-incinerator', 'labeled-crate', 'factory-dropper']
SCREENS = ['FactoryHopperScreen', 'MetalCraftingTableScreen', 'SmallElectricalFurnaceScreen', 'SmallWasteIncineratorScreen', 'LabeledCrateScreen', 'FactoryDropperScreen']
def validate(proof,nonce,sha,jar,jar_sha,game,width,height):
    for key,value in dict(suite='gui-album-three-v1',status='observed',nonce=nonce,
                         executed_source_sha=sha,visual_acceptance=False,physical_os_input=False).items():
        require(type(proof.get(key)) is type(value) and proof[key]==value,'Proof differs: '+key)
    runtime=proof.get('packaged_runtime',{})
    require(runtime.get('jar_path')==str(jar.resolve()) and runtime.get('jar_sha256')==jar_sha,'Wrong production JAR')
    require(runtime.get('fixture_free_jar') is True and runtime.get('separate_harness_module') is True,'Isolation missing')
    cases=proof.get('cases',[])
    require([x.get('case') for x in cases]==CASES,'Missing screens')
    for index,row in enumerate(cases):
        require(row.get('screen')=='com.oblixorprime.engineersdecorreforged.client.'+('MachineScreen$' if index!=4 else '')+SCREENS[index],'Wrong screen')
        require(row.get('server_menu_acknowledged') is True,'Missing server menu acknowledgement')
        require(re.fullmatch('[0-9a-f]{64}',row.get('screen_class_sha256','')),'Screen provenance missing')
        require(row.get('capture')==CASES[index]+'.png','Unexpected capture name')
        png(game/row['capture'],width,height,row.get('capture_sha256'))


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
    (game/'options.txt').write_text('onboardAccessibility:false\ntutorialStep:none\nrenderDistance:2\nsimulationDistance:2\nmaxFps:60\npauseOnLostFocus:false\nsoundCategory_master:0.0\nguiScale:1\nlang:'+args.language+'\n')
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
        validate(proof,nonce,sha,jar,jar_sha,game,args.width,args.height)
        final_sha = subprocess.check_output(['git','rev-parse','HEAD'],cwd=project,text=True).strip()
        require(clean() and final_sha == sha and hashlib.sha256(jar.read_bytes()).hexdigest()==jar_sha,
                'Source/JAR changed during run')
        print('OBSERVED six native machine screens; screenshots still require independent review. Not gameplay acceptance.')
        return 0
    except (SmokeFailure,OSError,ValueError,KeyError) as error:
        print('FAIL: '+str(error),file=sys.stderr);return 1


if __name__=='__main__':
    raise SystemExit(main())
