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
import zipfile
from client_mixin_smoke import FATAL_PATTERN, SmokeFailure, run_bounded
from factory_placer_v2_reference_native import png, decode_png

NAMES = {'hopper': ['normal','delayindicator','inversion','inversion_changed','inversion_normal','triggermode','triggermode_changed'],
         'dropper': ['normal','velocity','filtergate','filtergate_changed','externgate','externgate_changed','triggermode','triggermode_changed']}


def require(condition, message):
    if not condition: raise SmokeFailure(message)


HOPPER_ATLAS='assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/factory_hopper_gui.png'


def validate_hopper_icon_pixels(icons,game,width,height,jar):
    """Match ordinary-JAR sprite RGB exactly on the native icon draw path.

    Native screenshots only, scale 1. The native draw copies sprite RGB without
    alpha blending, verified across all 81 pixels of each mode in run 44307407908.
    No tolerance, guessed icon semantics or external repaint is used.
    """
    with zipfile.ZipFile(jar) as archive:
        aw,ah,atlas=decode_png(zipfile.Path(archive,HOPPER_ATLAS))
    require((aw,ah)==(256,256),'Unexpected original Hopper atlas dimensions')
    left=(width-176)//2;top=(height-166)//2
    expected_patches=[]
    for record,(u,v) in zip(icons,((188,49),(177,59),(177,49))):
        w,h,image=decode_png(game/record['file'])
        require((w,h)==(width,height),'Icon screenshot viewport differs')
        expected=[]
        for y in range(9):
            for x in range(9):
                sprite=((v+y)*aw+u+x)*4
                rgb=tuple(atlas[sprite+k] for k in range(3))
                offset=((top+49+y)*w+left+145+x)*4
                require(all(image[offset+k]==rgb[k] for k in range(3)),
                        'Hopper icon pixels mismatch '+record['name']+' at '+str((x,y)))
                expected.extend(rgb)
        expected_patches.append(bytes(expected))
    require(len(set(expected_patches))==3,'Mode sprite fixture is not distinct')


def validate(proof, nonce, sha, jar, jar_sha, game, width, height, language, machine):
    for key,value in dict(suite='native-factory-control-tooltip-v2',status='passed',nonce=nonce,
        executed_source_sha=sha, width=width,height=height,selected_language=language,machine=machine,
        presentation_selector_absent=True,opened_via_native_block_interaction=1,native_visual_acceptance=False,
        state_change_server_acks=3).items():
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
        if machine=='hopper' and c['name'] in ('inversion','inversion_changed','inversion_normal','triggermode','triggermode_changed'):
            require(type(t.get('synced_logic')) is int,'Missing actual synchronized logic state')

    if machine=='hopper':
        states={c['name']:c.get('tooltip',{}).get('synced_logic') for c in captures}
        before=proof.get('trigger_setup_before'); after=proof.get('trigger_setup_after')
        require(type(before) is int and type(after) is int and before & 4 != 0 and after & 4 == 0
                and before == states['inversion_changed'] and after == states['inversion_normal'] == states['triggermode'],'Trigger setup state acknowledgement differs')
        require(states['triggermode'] & 4 == 0 and states['triggermode_changed'] & 4 == 0
                and states['triggermode'] ^ states['triggermode_changed'] == 2,'Trigger pulse/continuous transition differs')
        require([states[n] for n in ('inversion','inversion_changed','inversion_normal','triggermode','triggermode_changed')]==[3,6,2,2,0],
                'Hopper exact native mode sequence differs')
        icons=proof.get('hopper_icons',[])
        require([c.get('name') for c in icons]==['icon-inversion','icon-inversion_changed','icon-inversion_normal'],'Missing unhovered mode icons')
        for c,state in zip(icons,(3,6,2)):
            require(type(c.get('client_logic')) is int and type(c.get('server_logic')) is int
                    and c['client_logic']==c['server_logic']==state,'Icon server/client state differs')
            require(type(c.get('render_frames')) is int and c['render_frames']>=3
                    and c.get('tooltip_absent') is True and c.get('gui_scale')==1,'Icon visibility/scale differs')
            require(c.get('file')=='factory-control-'+c['name']+'.png','Icon filename differs')
            png(game/c['file'],width,height,c.get('sha256'))
        validate_hopper_icon_pixels(icons,game,width,height,jar)
        texts={c['name']:c.get('tooltip',{}).get('text','') for c in captures}
        phrases={'en_us':('Current: inverted — enabled by an unpowered signal (default).',
                            'Current: redstone ignored — always enabled.',
                            'Current: normal — enabled by a powered signal.'),
                 'fr_fr':('État : inversé — activé sans signal (par défaut).',
                          'État : redstone ignorée — toujours activé.',
                          'État : normal — activé par un signal alimenté.')}
        for name,phrase in zip(('inversion','inversion_changed','inversion_normal'),phrases[language]):
            require(phrase in ' '.join(texts[name].split()),'Hopper state description differs from real mode: '+name)
        for name in ('inversion','triggermode'):
            require(' '.join(texts[name].split())!=' '.join(texts[name+'_changed'].split()),'Visible state text unchanged: '+name)

            require(states[name]!=states[name+'_changed'],'Hopper state change absent: '+name)

    if machine=='dropper':
        transitions=proof.get('dropper_transitions',[])
        require([t.get('control') for t in transitions]==['filtergate','externgate','triggermode'],'Missing real Dropper transitions')
        states={c['name']:c.get('tooltip',{}).get('synced_logic') for c in captures}
        for index,t in enumerate(transitions):
            name=t['control']; before=t.get('before'); after=t.get('client_after')
            require(all(type(t.get(k)) is int for k in ('before','client_after','server_after','expected_after')),'Missing Dropper state values')
            require(index==0 or before==transitions[index-1]['client_after'],'Dropper transition chain broken')
            expected=before^1 if name=='filtergate' else before^16 if name=='triggermode' else (before&~34)|({0:32,2:0}.get(before&34,2))
            require(after==expected and after!=before and t.get('server_after')==after and t.get('expected_after')==after,'Dropper transition acknowledgement differs')
            require(states[name]==before and states[name+'_changed']==after,'Dropper captures not tied to transition')


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
