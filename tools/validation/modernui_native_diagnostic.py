#!/usr/bin/env python3
"""Bounded ModernUI additional-blur native diagnostic; no compatibility verdict."""
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


MODERNUI_URL='https://cdn.modrinth.com/data/3sjzyvGR/versions/eMf1VSQd/ModernUI-NeoForge-1.21.1-3.13.0.1-universal.jar'
MODERNUI_SHA512='6a6177ba866a1d5b3b3ed41b02f024b9783ab7b80c11d449093b802b79dc38863187b53be1bcb734c682f36bad1c164d1ee58d7b02cfd5336c5bedcff9501ac4'

def validate(proof,nonce,sha,jar,jar_sha,game,width,height,additional):
    for key,value in dict(suite='modernui-native-diagnostic-v1',status='observed',nonce=nonce,
                         executed_source_sha=sha,visual_acceptance=False,physical_os_input=False).items():
        require(type(proof.get(key)) is type(value) and proof[key]==value,'Proof differs: '+key)
    runtime=proof.get('packaged_runtime',{})
    require(runtime.get('jar_path')==str(jar.resolve()) and runtime.get('jar_sha256')==jar_sha,'Wrong production JAR')
    require(runtime.get('fixture_free_jar') is True and runtime.get('separate_harness_module') is True,'Isolation missing')
    cases=proof.get('cases',[])
    require([x.get('case') for x in cases]==['editor','machine-container','vanilla-chest'],'Missing screens')
    for index,row in enumerate(cases):
        for key,value in dict(modernui_jar_sha512=MODERNUI_SHA512,additional_blur_config=additional,
            additional_blur_effective=additional,server_acknowledged_interaction=True,blur_process_call_instrumented=False).items():
            require(type(row.get(key)) is type(value) and row[key]==value,'Case differs: '+key)
        expected_screen=['com.oblixorprime.engineersdecorreforged.client.LabeledCrateEditScreen','com.oblixorprime.engineersdecorreforged.client.MachineScreen$SmallElectricalFurnaceScreen','net.minecraft.client.gui.screens.inventory.ContainerScreen'][index]
        require(row.get('screen')==expected_screen,'Screen identity differs')
        if index<2: require(re.fullmatch('[0-9a-f]{64}',row.get('screen_class_sha256','')),'Screen provenance missing')
        if additional: require(row.get('mBlurring') is True and row.get('mBlurEffect') is True,'Blur not armed/loaded')
        require(row.get('capture')==row['case']+'.png','Unexpected capture name')
        png(game/row['capture'],width,height,row.get('capture_sha256'))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--additional-blur',choices=['on','off'],required=True)
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
    evidence = Path(tempfile.mkdtemp(prefix='modernui-native-',dir=project/'build'))
    game=evidence/'run';game.mkdir();nonce=secrets.token_hex(16)
    modernui=evidence/'modernui.jar'
    with urllib.request.urlopen(MODERNUI_URL,timeout=90) as response,modernui.open('wb') as out:
        shutil.copyfileobj(response,out)
    require(hashlib.sha512(modernui.read_bytes()).hexdigest()==MODERNUI_SHA512,'Official ModernUI digest mismatch')
    config=game/'config/ModernUI/client.toml';config.parent.mkdir(parents=True)
    config.write_text('[screen]\nblurEffect = true\nadditionalBlurEffect = '+str(args.additional_blur=='on').lower()+'\noverrideVanillaBlur = true\nanimationDuration = 200\nblurRadius = 7\n')

    (game/'.iedctr-client-gameplay-nonce').write_text(nonce)
    (game/'options.txt').write_text('onboardAccessibility:false\ntutorialStep:none\nrenderDistance:2\nsimulationDistance:2\n'
        'maxFps:60\npauseOnLostFocus:false\nsoundCategory_master:0.0\nguiScale:1\nlang:'+args.language+'\n')
    init=evidence/'modernui.init.gradle'
    init.write_text("""gradle.projectsEvaluated {
 rootProject.sourceSets.factoryPlacerNativeValidation.java.srcDir(rootProject.file('src/modernuiDiagnostic/java'))
 rootProject.dependencies.add('factoryPlacerNativeValidationRuntimeOnly', rootProject.files(System.getenv('IEDCTR_MODERNUI_JAR')))
 rootProject.extensions.getByName('neoForge').runs.named('crateLabelPackagedClient') {
  systemProperty 'iedctr.crateLabelPackagedNative', 'false'
  systemProperty 'iedctr.modernUIDiagnostic', 'true'
  systemProperty 'iedctr.modernUIJar', System.getenv('IEDCTR_MODERNUI_JAR')
  systemProperty 'iedctr.modernUIJarSha512', System.getenv('IEDCTR_MODERNUI_SHA512')
  systemProperty 'iedctr.expectedAdditionalBlur', System.getenv('IEDCTR_MODERNUI_ADDITIONAL')
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
marker=game/'.iedctr-modernui-xvfb'
assert not marker.exists()
marker.write_text(nonce+'\n'+os.environ['DISPLAY'])
os.execvpe(sys.argv[1],sys.argv[1:],os.environ)
""")
    env=dict(os.environ,IEDCTR_GAMEPLAY_DIR=str(game),IEDCTR_GAMEPLAY_NONCE=nonce,IEDCTR_EXECUTED_SOURCE_SHA=sha,
             IEDCTR_SCREEN_WIDTH=str(args.width),IEDCTR_SCREEN_HEIGHT=str(args.height),LIBGL_ALWAYS_SOFTWARE='1',ALSOFT_DRIVERS='null')
    env.update(IEDCTR_MODERNUI_JAR=str(modernui.resolve()),IEDCTR_MODERNUI_SHA512=MODERNUI_SHA512,IEDCTR_MODERNUI_ADDITIONAL=str(args.additional_blur=='on').lower(),TMPDIR=str(evidence.resolve()))
    base=['bash',str(project/'gradlew'),'--no-daemon','--stacktrace']
    print('ModernUI native diagnostic evidence: '+str(evidence),flush=True)
    try:
        code,expired=run_bounded(base+['jar'],project,env,evidence/'jar.log',args.prepare_timeout)
        require(code==0 and not expired,'Normal JAR build failed')
        base+=['-Pinclude_factory_placer_native_validation=true','--init-script',str(init)]
        code,expired=run_bounded(base+['verifyFactoryPlacerPackagedRuntime'],project,env,evidence/'package.log',args.prepare_timeout)
        require(code==0 and not expired,'Separate harness/classpath validation failed')
        report=json.loads((project/'build/reports/factory-placer-packaged-runtime.json').read_text())
        jar=Path(report['jar_path']);jar_sha=hashlib.sha256(jar.read_bytes()).hexdigest()
        require(jar_sha==report['jar_sha256']==args.expected_jar_sha256,'Exact candidate JAR hash differs')
        (evidence/'packaged-runtime-classpath.json').write_text(json.dumps(report,indent=2))
        env.update(IEDCTR_EXPECTED_MOD_JAR=str(jar.resolve()),IEDCTR_EXPECTED_MOD_JAR_SHA256=jar_sha)
        gradle=base
        code,expired=run_bounded(gradle+['prepareCrateLabelPackagedClientRun'],project,env,evidence/'prepare.log',args.prepare_timeout)
        require(code==0 and not expired,'Native preparation failed')
        log=evidence/'client.log'
        code,expired=run_bounded(['xvfb-run','-a','-s',f'-screen 0 {args.width}x{args.height}x24',sys.executable,str(guard)]
                                +gradle+['runCrateLabelPackagedClient'],project,env,log,args.timeout)
        logs=log.read_text(errors='replace')
        require(code==0 and not expired and not FATAL_PATTERN.search(logs)
                and 'IEDCTR_MODERNUI OBSERVED nonce='+nonce in logs,'Native exit/log gate failed')
        proof=json.loads((game/'modernui-proof.json').read_text())
        validate(proof,nonce,sha,jar,jar_sha,game,args.width,args.height,args.additional_blur=='on')
        final_sha = subprocess.check_output(['git','rev-parse','HEAD'],cwd=project,text=True).strip()
        require(clean() and final_sha == sha and hashlib.sha256(jar.read_bytes()).hexdigest()==jar_sha,
                'Source/JAR changed during run')
        print('OBSERVED three native screens/config/interaction; screenshots still require independent review. Not a compatibility PASS.')
        return 0
    except (SmokeFailure,OSError,ValueError,KeyError) as error:
        print('FAIL: '+str(error),file=sys.stderr);return 1


if __name__=='__main__':
    raise SystemExit(main())
