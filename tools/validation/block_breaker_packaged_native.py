#!/usr/bin/env python3
"""Independent exact-JAR, default-path Block Breaker native qualification."""
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

ART_SHA = '2594e227dd153ffa7b1954d8d5d3069e10d0176c0e8142c7f7d2727f315b0a0d'
NAMES = ['normal','title','energy','fluid','power','target','scene','work','worked','returned']


def require(condition, message):
    if not condition:
        raise SmokeFailure(message)


def validate(proof, nonce, sha, jar, jar_sha, game, width, height, language):
    expected = dict(suite='native-block-breaker-packaged-v1', status='passed', nonce=nonce,
                    executed_source_sha=sha, presentation_selector_absent=True,
                    opened_via_native_block_interaction=1, pickup_server_acks=1, return_server_acks=1,
                    energy_seeded=240, energy_final=0, width=width, height=height, selected_language=language)
    for key, value in expected.items():
        require(type(proof.get(key)) is type(value) and proof[key] == value, 'Proof differs: ' + key)
    work = dict(target_before='minecraft:dirt',target_after='minecraft:air',energy_seeded=240,energy_after=0,
                output_item='minecraft:dirt',output_count=1,world_drop_count=0,server_confirmed=True)
    require(proof.get('work') == work and all(type(proof['work'][k]) is type(v) for k,v in work.items()),
            'Authoritative gameplay snapshot differs')
    final = proof.get('final_conservation', {})
    expected_final = dict(destination='player_inventory',machine_item_count=0,player_item='minecraft:dirt',
                          player_item_count=1,carried_item_count=0,world_drop_count=0,
                          output_accepts_insertion=False,server_confirmed=True)
    require(type(final) is dict and set(final) == set(expected_final) | {'destination_menu_slot'},
            'Final conservation fields differ')
    for key,value in expected_final.items():
        require(type(final.get(key)) is type(value) and final[key] == value, 'Final conservation differs: ' + key)
    require(type(final.get('destination_menu_slot')) is int and 1 <= final['destination_menu_slot'] <= 36,
            'Return destination is not a player menu slot')
    runtime = proof.get('packaged_runtime', {})
    require(runtime.get('jar_path') == str(jar.resolve()) and runtime.get('jar_sha256') == jar_sha,
            'Loaded JAR identity differs')
    require(runtime.get('fixture_free_jar') is True and runtime.get('separate_harness_module') is True,
            'Harness isolation missing')
    require(runtime.get('active_texture_sha256') == ART_SHA and runtime.get('active_texture_pack') ==
            'mod/immersive_engineer_decor_controls_tool_reforged', 'Active v3 artwork differs')
    require(runtime.get('active_power_texture_sha256') == '23ff15c60420b8951117f748d0b6122e174c915f87e91466930af80fbdc12123',
            'Active neutral power artwork differs')
    captures = proof.get('captures', [])
    require([c.get('name') for c in captures] == NAMES, 'Capture coverage differs')
    for c in captures:
        require(c.get('file') == 'breaker-' + c['name'] + '.png', 'Unexpected capture path')
        png(game / c['file'], width, height, c.get('sha256'))
        if c['name'] in NAMES[1:8]:
            require(type(c.get('render_frames')) is int and c['render_frames'] >= 3
                    and c.get('tooltip', {}).get('in_bounds') is True, 'Native tooltip evidence missing')
    title = captures[1]['tooltip']['text'].replace('\n', ' ')
    require(title == ('Small Block Breaker' if language == 'en_us' else 'Petit brise-bloc'),
            'Localized title differs: ' + title)


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
    art = project/'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/block_breaker_rp_v3/background.png'
    require(hashlib.sha256(art.read_bytes()).hexdigest()==ART_SHA,'Retained v3 changed')
    (project/'build').mkdir(exist_ok=True)
    evidence = Path(tempfile.mkdtemp(prefix='block-breaker-native-',dir=project/'build'))
    game=evidence/'run';game.mkdir();nonce=secrets.token_hex(16)
    (game/'.iedctr-client-gameplay-nonce').write_text(nonce)
    (game/'options.txt').write_text('onboardAccessibility:false\ntutorialStep:none\nrenderDistance:2\nsimulationDistance:2\n'
        'maxFps:60\npauseOnLostFocus:false\nsoundCategory_master:0.0\nguiScale:1\nlang:'+args.language+'\n')
    init=evidence/'breaker.init.gradle'
    init.write_text("""gradle.projectsEvaluated {
 rootProject.extensions.getByName('neoForge').runs.named('blockBreakerPackagedClient') {
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
marker=game/'.iedctr-breaker-xvfb'
assert not marker.exists()
marker.write_text(nonce+'\n'+os.environ['DISPLAY'])
os.execvpe(sys.argv[1],sys.argv[1:],os.environ)
""")
    env=dict(os.environ,IEDCTR_GAMEPLAY_DIR=str(game),IEDCTR_GAMEPLAY_NONCE=nonce,IEDCTR_EXECUTED_SOURCE_SHA=sha,
             IEDCTR_SCREEN_WIDTH=str(args.width),IEDCTR_SCREEN_HEIGHT=str(args.height),LIBGL_ALWAYS_SOFTWARE='1',ALSOFT_DRIVERS='null')
    base=['bash',str(project/'gradlew'),'--no-daemon','--stacktrace']
    print('Independent native Breaker evidence: '+str(evidence),flush=True)
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
        code,expired=run_bounded(gradle+['prepareBlockBreakerPackagedClientRun'],project,env,evidence/'prepare.log',args.prepare_timeout)
        require(code==0 and not expired,'Native preparation failed')
        log=evidence/'client.log'
        code,expired=run_bounded(['xvfb-run','-a','-s',f'-screen 0 {args.width}x{args.height}x24',sys.executable,str(guard)]
                                +gradle+['runBlockBreakerPackagedClient'],project,env,log,args.timeout)
        logs=log.read_text(errors='replace')
        require(code==0 and not expired and not FATAL_PATTERN.search(logs)
                and 'IEDCTR_BREAKER_PACKAGED PASSED nonce='+nonce in logs,'Native exit/log gate failed')
        proof=json.loads((game/'breaker-proof.json').read_text())
        validate(proof,nonce,sha,jar,jar_sha,game,args.width,args.height,args.language)
        require(clean() and hashlib.sha256(jar.read_bytes()).hexdigest()==jar_sha,'Source/JAR changed during run')
        print('PASS bounded native default Block Breaker; not owner visual acceptance or complete modpack qualification.')
        return 0
    except (SmokeFailure,OSError,ValueError,KeyError) as error:
        print('FAIL: '+str(error),file=sys.stderr);return 1


if __name__=='__main__':
    raise SystemExit(main())
