#!/usr/bin/env python3
"""Milking qualification against an ordinary fixture-free JAR and separate harness.
Existing 38-slot, milk-state, conversion, screenshot and input gates are preserved.
"""
import argparse, hashlib, json, os, re, secrets, shutil, subprocess, sys, tempfile
from pathlib import Path
from client_mixin_smoke import SmokeFailure, run_bounded
from milking_qualification_native import PROFILES, current_candidate, require
from milking_packaged_proof import validate_proof

def main() -> int:
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--project',type=Path,default=Path(__file__).resolve().parents[2])
    parser.add_argument('--profile',choices=PROFILES,default='en-1280x720-scale1')
    parser.add_argument('--expected-jar-sha256', required=True)
    parser.add_argument('--list-profiles',action='store_true')
    parser.add_argument('--prepare-timeout',type=int,default=600)
    parser.add_argument('--timeout',type=int,default=600)
    args=parser.parse_args()
    if args.list_profiles:
        print(json.dumps(PROFILES,indent=2));return 0
    try:
        require(sys.platform=='linux' and min(args.prepare_timeout,args.timeout)>0, "Requires Linux and positive bounded timeouts")
        require(re.fullmatch(r'[a-f0-9]{64}',args.expected_jar_sha256) is not None, 'Expected exact JAR hash required')
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
    rootProject.extensions.getByName('neoForge').runs.named('milkingPackagedDefaultClient') {
        gameDirectory.set(rootProject.file(System.getenv('IEDCTR_GAMEPLAY_DIR')))
        systemProperty 'iedctr.clientGameplayNonce', System.getenv('IEDCTR_GAMEPLAY_NONCE')
        systemProperty 'iedctr.clientGameplayDirectory', System.getenv('IEDCTR_GAMEPLAY_DIR')
        systemProperty 'iedctr.milkingPackagedNative', 'true'
        systemProperty 'iedctr.expectedModJar', System.getenv('IEDCTR_EXPECTED_MOD_JAR')
        systemProperty 'iedctr.expectedModJarSha256', System.getenv('IEDCTR_EXPECTED_MOD_JAR_SHA256')
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
        base=['bash',str(project/'gradlew'),'--no-daemon','--stacktrace']
        code,expired=run_bounded(base+['jar'],project,env,evidence/'jar.log',args.prepare_timeout)
        require(code==0 and not expired,'Normal JAR build failed')
        base+=['-Pinclude_milking_packaged_validation=true']
        code,expired=run_bounded(base+['verifyMilkingPackagedRuntime'],project,env,evidence/'package.log',args.prepare_timeout)
        require(code==0 and not expired,'Separate harness/classpath validation failed')
        report=json.loads((project/'build/reports/milking-packaged-runtime.json').read_text())
        jar=Path(report['jar_path']);jar_sha=hashlib.sha256(jar.read_bytes()).hexdigest()
        require(jar_sha==report['jar_sha256']==args.expected_jar_sha256,'Classpath or expected candidate JAR hash differs')
        (evidence/'packaged-runtime.json').write_text(json.dumps(report,indent=2))
        env.update(IEDCTR_EXPECTED_MOD_JAR=str(jar.resolve()),IEDCTR_EXPECTED_MOD_JAR_SHA256=jar_sha)
        gradle=base+['--init-script',str(init)]
        print(f'Milking isolated native evidence: {evidence} ({args.profile})',flush=True)
        code,expired=run_bounded(gradle+['prepareMilkingPackagedDefaultClientRun'],project,env,evidence/'prepare.log',args.prepare_timeout)
        require(not expired and code==0, f'Milking client preparation failed (exit={code}, timeout={expired})')
        log=evidence/'client.log'
        code,expired=run_bounded(['xvfb-run','-a','-s',f'-screen 0 {width}x{height}x24']+gradle+['runMilkingPackagedDefaultClient'],
                                 project,env,log,args.timeout)
        proof=json.loads((game/'client-milking-qualification-result.json').read_text())
        logs=log.with_suffix('.observed.log').read_text(errors='replace')
        for name in ('latest.log','debug.log'):
            runtime=game/'logs'/name
            if runtime.is_file():logs+='\n'+runtime.read_text(errors='replace')
        require(current_candidate(project)==source, "Candidate changed during execution")
        validate_proof(proof,nonce,source,game,logs,code,expired,width,height,language,report,scale)
        require(hashlib.sha256(jar.read_bytes()).hexdigest()==jar_sha,"Normal JAR changed during run")
        print('PASS bounded native Milking qualification. Full visual/input acceptance is not claimed.')
        return 0
    except (SmokeFailure,OSError,ValueError,subprocess.CalledProcessError) as error:
        print(f'FAIL: {error}',file=sys.stderr);return 1


if __name__=='__main__':
    raise SystemExit(main())
