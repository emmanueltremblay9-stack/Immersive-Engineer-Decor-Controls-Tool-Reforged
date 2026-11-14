#!/usr/bin/env python3
"""Independent exact-JAR Factory control-tooltip native qualification."""
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

NAMES = {'hopper': ['normal','delayindicator','inversion','inversion_changed','triggermode','triggermode_changed'],
         'dropper': ['normal','velocity','filtergate','externgate','triggermode']}


def require(condition, message):
    if not condition: raise SmokeFailure(message)


def validate(proof, nonce, sha, jar, jar_sha, game, width, height, language, machine):
    for key,value in dict(suite='native-factory-control-tooltip-v2',status='passed',nonce=nonce,
        executed_source_sha=sha, width=width,height=height,selected_language=language,machine=machine,
        presentation_selector_absent=True,opened_via_native_block_interaction=1,native_visual_acceptance=False,
        state_change_server_acks=3 if machine=='hopper' else 0).items():
        require(type(proof.get(key)) is type(value) and proof[key]==value,'Proof differs: '+key)
    runtime=proof.get('packaged_runtime',{})
    require(runtime.get('jar_path')==str(jar.resolve()) and runtime.get('jar_sha256')==jar_sha,'Runtime identity differs')
    require(runtime.get('fixture_free_jar') is True and runtime.get('separate_harness_module') is True,'Isolation missing')
    captures=proof.get('captures',[])
    require([c.get('name') for c in captures]==NAMES[machine],'Capture coverage differs')
    for c in captures:
        require(c.get('file')=='factory-control-'+c['name']+'.png','Capture path differs')
        png(game/c['file'],width,height,c.get('sha256'))
        if c['name']=='normal': continue
        t=c.get('tooltip',{})
        require(type(c.get('render_frames')) is int and c['render_frames']>=3,'Insufficient actual render frames')
        require(t.get('in_bounds') is True and type(t.get('width')) is int and 0<t['width']<=min(240,width-16)
            and type(t.get('height')) is int and t['height']>10,'Tooltip bounds differ')
        require(type(t.get('gray_characters')) is int and t['gray_characters']>0,'Missing gray styled characters')
        require(type(t.get('text')) is str and len(t['text'])>20 and '\n' in t['text'],'Missing rendered multiline text')
        require(type(t.get('expected_text')) is str and ' '.join(t['text'].split())==' '.join(t['expected_text'].split()),'Rendered semantic text differs')
        if machine=='hopper' and c['name'] in ('inversion','inversion_changed','triggermode','triggermode_changed'):
            require(type(t.get('synced_logic')) is int,'Missing actual synchronized logic state')

    if machine=='hopper':
        states={c['name']:c.get('tooltip',{}).get('synced_logic') for c in captures}
        before=proof.get('trigger_setup_before'); after=proof.get('trigger_setup_after')
        require(type(before) is int and type(after) is int and before & 4 != 0 and after & 4 == 0
                and before == states['inversion_changed'] and after == states['triggermode'],'Trigger setup state acknowledgement differs')
        require(states['triggermode'] & 4 == 0 and states['triggermode_changed'] & 4 == 0
                and states['triggermode'] ^ states['triggermode_changed'] == 2,'Trigger pulse/continuous transition differs')
        texts={c['name']:c.get('tooltip',{}).get('text','') for c in captures}
        for name in ('inversion','triggermode'):
            require(' '.join(texts[name].split())!=' '.join(texts[name+'_changed'].split()),'Visible state text unchanged: '+name)

            require(states[name]!=states[name+'_changed'],'Hopper state change absent: '+name)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--machine', choices=['hopper','dropper'], required=True)
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
    evidence = Path(tempfile.mkdtemp(prefix='factory-control-native-',dir=project/'build'))
    game=evidence/'run';game.mkdir();nonce=secrets.token_hex(16)
    (game/'.iedctr-client-gameplay-nonce').write_text(nonce)
    (game/'options.txt').write_text('onboardAccessibility:false\ntutorialStep:none\nrenderDistance:2\nsimulationDistance:2\n'
        'maxFps:60\npauseOnLostFocus:false\nsoundCategory_master:0.0\nguiScale:1\nlang:'+args.language+'\n')
    init=evidence/'factory-control.init.gradle'
    init.write_text("""gradle.projectsEvaluated {
 rootProject.extensions.getByName('neoForge').runs.named('factoryControlTooltipClient') {
  gameDirectory.set(rootProject.file(System.getenv('IEDCTR_GAMEPLAY_DIR')))
  systemProperty 'iedctr.clientGameplayNonce', System.getenv('IEDCTR_GAMEPLAY_NONCE')
  systemProperty 'iedctr.clientGameplayDirectory', System.getenv('IEDCTR_GAMEPLAY_DIR')
  systemProperty 'iedctr.factoryControlMachine', System.getenv('IEDCTR_FACTORY_MACHINE')
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
marker=game/'.iedctr-factory-control-xvfb'
assert not marker.exists()
marker.write_text(nonce+'\n'+os.environ['DISPLAY'])
os.execvpe(sys.argv[1],sys.argv[1:],os.environ)
""")
    env=dict(os.environ,IEDCTR_FACTORY_MACHINE=args.machine,IEDCTR_GAMEPLAY_DIR=str(game),IEDCTR_GAMEPLAY_NONCE=nonce,IEDCTR_EXECUTED_SOURCE_SHA=sha,
             IEDCTR_SCREEN_WIDTH=str(args.width),IEDCTR_SCREEN_HEIGHT=str(args.height),LIBGL_ALWAYS_SOFTWARE='1',ALSOFT_DRIVERS='null')
    base=['bash',str(project/'gradlew'),'--no-daemon','--stacktrace']
    print('Independent native Factory control evidence: '+str(evidence),flush=True)
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
        code,expired=run_bounded(gradle+['prepareFactoryControlTooltipClientRun'],project,env,evidence/'prepare.log',args.prepare_timeout)
        require(code==0 and not expired,'Native preparation failed')
        log=evidence/'client.log'
        code,expired=run_bounded(['xvfb-run','-a','-s',f'-screen 0 {args.width}x{args.height}x24',sys.executable,str(guard)]
                                +gradle+['runFactoryControlTooltipClient'],project,env,log,args.timeout)
        logs=log.read_text(errors='replace')
        require(code==0 and not expired and not FATAL_PATTERN.search(logs)
                and 'IEDCTR_FACTORY_CONTROL PASSED nonce='+nonce in logs,'Native exit/log gate failed')
        proof=json.loads((game/'factory-control-proof.json').read_text())
        validate(proof,nonce,sha,jar,jar_sha,game,args.width,args.height,args.language,args.machine)
        require(clean() and hashlib.sha256(jar.read_bytes()).hexdigest()==jar_sha,'Source/JAR changed during run')
        print('PASS bounded native Factory control tooltips; not owner visual acceptance or complete modpack qualification.')
        return 0
    except (SmokeFailure,OSError,ValueError,KeyError) as error:
        print('FAIL: '+str(error),file=sys.stderr);return 1


if __name__=='__main__':
    raise SystemExit(main())
