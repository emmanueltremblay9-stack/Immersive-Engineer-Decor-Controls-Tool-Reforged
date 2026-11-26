#!/usr/bin/env python3
"""Exact ordinary-JAR mooshroom proof; separate NeoForge 234 compatibility runtime."""
import argparse, hashlib, json, os, re, secrets, shutil, subprocess, sys, tempfile
from pathlib import Path
from client_mixin_smoke import SmokeFailure, run_bounded
from milking_qualification_native import require, current_candidate
from mooshroom_milking_native import PROFILES
from tfc_packaged_proof import validate_mooshroom as validate_proof
EXPECTED_JAR='2734a258fd3b8c5d037ee82e34c2a38df8529e1055b5720581e7e1f059106ccc'
def main() -> int:
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--project',type=Path,default=Path(__file__).resolve().parents[2])
    parser.add_argument('--profile',choices=PROFILES,default='fr-1280x720-scale2')
    parser.add_argument('--list-profiles',action='store_true')
    parser.add_argument('--prepare-timeout',type=int,default=600)
    parser.add_argument('--timeout',type=int,default=600)
    args=parser.parse_args()
    if args.list_profiles:
        print(json.dumps(PROFILES,indent=2));return 0
    try:
        require(sys.platform=='linux' and min(args.prepare_timeout,args.timeout)>0, "Requires Linux and positive bounded timeouts")
        project=args.project.resolve()
        from check_milking_qualification import check_preservation
        check_preservation(project)
        source=current_candidate(project)
        missing=[name for name in ('java','Xvfb','xvfb-run','xauth') if not shutil.which(name)]
        require(not missing, "Missing native prerequisites: "+', '.join(missing))
        width,height,scale,language=PROFILES[args.profile]
        (project/'build').mkdir(exist_ok=True)
        evidence=Path(tempfile.mkdtemp(prefix='mooshroom-milking-native-',dir=project/'build'))
        game=evidence/'run';game.mkdir();nonce=secrets.token_hex(16)
        (game/'.iedctr-client-gameplay-nonce').write_text(nonce,encoding='utf-8')
        (game/'options.txt').write_text('onboardAccessibility:false\ntutorialStep:none\nrenderDistance:2\nsimulationDistance:2\n'
            f'maxFps:60\npauseOnLostFocus:false\nsoundCategory_master:0.0\nguiScale:{scale}\nlang:{language}\n',encoding='utf-8')
        init=evidence/'milking.init.gradle'
        init.write_text("""gradle.projectsEvaluated {
    rootProject.extensions.getByName('neoForge').runs.named('tfcPackagedDefaultClient') {
        gameDirectory.set(rootProject.file(System.getenv('IEDCTR_GAMEPLAY_DIR')))
        systemProperty 'iedctr.clientGameplayNonce', System.getenv('IEDCTR_GAMEPLAY_NONCE')
        systemProperty 'iedctr.clientGameplayDirectory', System.getenv('IEDCTR_GAMEPLAY_DIR')
        systemProperty 'iedctr.tfcPackagedMooshroomNative', 'true'
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
        code,expired=run_bounded(base+['jar'],project,env,evidence/'normal-jar.log',args.prepare_timeout)
        require(code==0 and not expired,'Normal NeoForge 230 JAR build failed')
        properties=dict(line.split('=',1) for line in (project/'gradle.properties').read_text().splitlines() if '=' in line and not line.lstrip().startswith('#'))
        normal=project/'build/libs'/(properties['mod_id'].strip()+'-'+properties['mod_version'].strip()+'.jar')
        require(hashlib.sha256(normal.read_bytes()).hexdigest()==EXPECTED_JAR,'Normal candidate bytes differ')
        jar=evidence/'normal.jar';shutil.copyfile(normal,jar)
        (evidence/'normal-build.json').write_text(json.dumps(dict(source_sha=source,neoforge_version=properties['neo_version'].strip(),jar_sha256=EXPECTED_JAR,jar_path=str(jar)),indent=2))
        require(properties['neo_version'].strip()=='21.1.230','Ordinary build loader changed')
        env.update(IEDCTR_EXPECTED_MOD_JAR=str(jar),IEDCTR_EXPECTED_MOD_JAR_SHA256=EXPECTED_JAR)
        gradle=base+['--init-script',str(init),'-Pinclude_tfc_packaged_validation=true','-Ptfc_runtime_jar='+str(jar)]
        code,expired=run_bounded(gradle+['verifyTfcPackagedRuntime'],project,env,evidence/'package.log',args.prepare_timeout)
        require(code==0 and not expired,'Compatibility classpath verification failed')
        report=json.loads((project/'build/reports/tfc-packaged-runtime.json').read_text())
        (evidence/'packaged-runtime.json').write_text(json.dumps(report,indent=2))
        print(f'Mooshroom isolated native evidence: {evidence} ({args.profile})',flush=True)
        code,expired=run_bounded(gradle+['prepareTfcPackagedDefaultClientRun'],project,env,evidence/'prepare.log',args.prepare_timeout)
        require(not expired and code==0, f'Milking client preparation failed (exit={code}, timeout={expired})')
        log=evidence/'client.log'
        code,expired=run_bounded(['xvfb-run','-a','-s',f'-screen 0 {width}x{height}x24']+gradle+['runTfcPackagedDefaultClient'],
                                 project,env,log,args.timeout)
        proof=json.loads((game/'client-mooshroom-milking-result.json').read_text())
        logs=log.with_suffix('.observed.log').read_text(errors='replace')
        for name in ('latest.log','debug.log'):
            runtime=game/'logs'/name
            if runtime.is_file():logs+='\n'+runtime.read_text(errors='replace')
        require(current_candidate(project)==source, "Candidate changed during execution")
        validate_proof(proof,nonce,source,game,logs,code,expired,width,height,language,scale,report=report)
        require(hashlib.sha256(jar.read_bytes()).hexdigest()==EXPECTED_JAR,'Candidate changed during compatibility run')
        dependencies=evidence/'compatibility-jars';dependencies.mkdir()
        for dependency in proof['packaged_runtime']['compatibility_dependencies']:
            shutil.copyfile(dependency['jar_path'], dependencies/(dependency['mod_id']+'.jar'))
        print('PASS bounded real Mooshroom proof. Owner visual acceptance is not claimed.')
        return 0
    except (SmokeFailure,OSError,ValueError,subprocess.CalledProcessError) as error:
        print(f'FAIL: {error}',file=sys.stderr);return 1


if __name__=='__main__':
    raise SystemExit(main())
