#!/usr/bin/env python3
"""Real TFC cow client evidence; synthetic validator tests are not gameplay proof."""
from __future__ import annotations
import argparse
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import subprocess
import sys
import tempfile
import uuid
from client_mixin_smoke import FATAL_PATTERN, SmokeFailure, run_bounded
from milking_qualification_native import (require, exact, expected_geometry, safe_png,
    decode_png, crop_pixels, current_candidate, BACKGROUND_SHA, MILK_SHA, SCREEN_CLASS)

SUITE='native-tfc-milking-recovery-v1'
PROFILES={'fr-1280x720-scale2': (1280,720,2,'fr_fr')}
PHASES=('ready','recovering','recovering-tooltip','post-expiry')


def integer(obj,key,minimum=None):
    value=obj.get(key)
    require(type(value) is int and (minimum is None or value>=minimum), 'Invalid '+key)
    return value


def snapshot(s, identity=None):
    require(isinstance(s,dict),'Missing native snapshot')
    for k,v in dict(source='tfc:cow',client_source='TFC',server_source='TFC',
                    client_name='Native TFC Cow',server_name='Native TFC Cow',total_ticks=400,
                    client_total_ticks=400).items(): exact(s,k,v)
    uid=s.get('server_uuid')
    try: require(str(uuid.UUID(uid))==uid,'Invalid source UUID')
    except (ValueError,TypeError,AttributeError): raise SmokeFailure('Invalid source UUID')
    exact(s,'client_uuid',uid)
    entity=integer(s,'server_entity_id',1);exact(s,'client_entity_id',entity)
    if identity is not None: require((uid,entity)==identity,'Source identity changed')
    amount=integer(s,'amount',0);exact(s,'client_amount',amount)
    uses=integer(s,'uses',0);produced=integer(s,'produced_tick');calendar=integer(s,'calendar_tick')
    remaining=integer(s,'remaining_ticks',0);client_remaining=integer(s,'client_remaining_ticks',0)
    require(remaining<=400 and client_remaining<=400 and abs(client_remaining-remaining)<=30,'Native sync discrepancy')
    exact(s,'remaining_ticks',max(0,produced+400-calendar))
    state='READY' if remaining==0 else 'RECOVERING'
    exact(s,'ready',remaining==0);exact(s,'server_state',state);exact(s,'client_state',state)
    return uid,entity


def validate_proof(proof,nonce,source_sha,game,logs,exit_code,timed_out,width=1280,height=720,language='fr_fr',gui_scale=2):
    require(isinstance(proof,dict),'Missing proof')
    require(not timed_out and type(exit_code) is int and exit_code==0 and not FATAL_PATTERN.search(logs),'Unclean client execution')
    require(not list(game.glob('crash-reports/*')) and not list(game.parent.glob('**/hs_err_pid*.log')),'Native crash evidence')
    require(re.fullmatch('[a-f0-9]{40}',source_sha) is not None and re.fullmatch('[a-f0-9]{32}',nonce) is not None,'Invalid run identity')
    require((width,height,gui_scale,language)==PROFILES['fr-1280x720-scale2'],'Unsupported viewport')
    for key,value in dict(suite=SUITE,nonce=nonce,executed_source_sha=source_sha,status='passed_bounded_smoke',
            source_background_sha256=BACKGROUND_SHA,source_milk_sha256=MILK_SHA,
            actual_integrated_world=True,native_visual_acceptance=False,requested_language=language,selected_language=language,
            screen_class=SCREEN_CLASS,tfc_version='4.2.12',config_restored=True,native_tick_production=True,
            produced_tick_mutated_after_setup=False).items():exact(proof,key,value)
    require('error' not in proof,'Error in passing proof')
    geometry=expected_geometry(width,height,gui_scale)
    for key,value in geometry.items():
        if not key.startswith(('tank_','presentation_')): exact(proof,key,value)
    captures=proof.get('captures')
    require(isinstance(captures,list) and len(captures)==4,'Incomplete native captures')
    identity=None;seen={};images={};menus=set()
    folder=game/'tfc-milking-recovery-screenshots'
    for c in captures:
        require(isinstance(c,dict),'Invalid capture')
        phase=c.get('phase');require(phase in PHASES and phase not in seen,'Duplicate or invalid phase')
        exact(c,'name',phase+'.png');identity=snapshot(c,identity);seen[phase]=c
        exact(c,'screen_class',SCREEN_CLASS)
        for key,value in geometry.items():
            if not key.startswith(('tank_','presentation_')): exact(c,key,value)
        menu=integer(c,'menu_id',1);exact(c,'server_menu_id',menu);menus.add(menu)
        file=safe_png(folder,c['name'],c.get('sha256'));w,h,pixels=decode_png(file)
        require((w,h)==(width,height),'Wrong screenshot viewport');images[phase]=pixels
        for key,value in dict(portrait_x=(geometry['presentation_gui_left']+27)*gui_scale,
                portrait_y=(geometry['presentation_gui_top']+34)*gui_scale,portrait_width=47*gui_scale,
                portrait_height=41*gui_scale,portrait_name=phase+'-portrait.png').items():exact(c,key,value)
        crop=safe_png(folder,c['portrait_name'],c.get('portrait_sha256'));cw,ch,cp=decode_png(crop)
        require((cw,ch)==(c['portrait_width'],c['portrait_height']),'Wrong portrait dimensions')
        require(cp==crop_pixels(pixels,w,c['portrait_x'],c['portrait_y'],cw,ch),'Portrait is not screenshot crop')
        require(len({cp[i:i+4] for i in range(0,len(cp),4)})>=16,'Blank portrait crop')
    require(len(menus)==1,'Native menu changed between captures')
    ready,first,hover,second=(seen[p] for p in PHASES)
    for s,amount,uses in ((ready,0,0),(first,1000,1),(hover,1000,1),(second,2000,2)):
        exact(s,'amount',amount);exact(s,'uses',uses)
    exact(ready,'ready',True)
    for s in (first,hover,second): exact(s,'ready',False)
    require(ready['calendar_tick']<first['calendar_tick']<=hover['calendar_tick']<second['calendar_tick'],'No native time progression')
    require(first['produced_tick']==hover['produced_tick'] and second['produced_tick']-first['produced_tick']>=400,'No native expiry and second production')
    exact(hover,'tooltip_hovered',True)
    x=integer(hover,'hover_x');y=integer(hover,'hover_y');left=geometry['presentation_gui_left'];top=geometry['presentation_gui_top']
    require((left+24<=x<left+209 and top+112<=y<top+124) or (left+160<=x<left+207 and top+56<=y<top+73),'Cursor outside recovery tooltip')
    require(images['recovering']!=images['recovering-tooltip'],'Missing rendered tooltip difference')
    repeat=proof.get('no_repeat');require(isinstance(repeat,dict),'Missing no-repeat observation')
    a,b=repeat.get('first'),repeat.get('later')
    for s in (a,b):
        snapshot(s,identity);exact(s,'amount',1000);exact(s,'uses',1);exact(s,'produced_tick',first['produced_tick']);exact(s,'ready',False)
    for sample,capture in ((a,first),(b,hover)):
        for key in ('amount','uses','produced_tick','calendar_tick','remaining_ticks','total_ticks','server_uuid','server_entity_id'):
            exact(sample,key,capture[key])
    require(first['calendar_tick']<=a['calendar_tick']<b['calendar_tick']<second['calendar_tick'],'No-repeat outside production interval')
    require(b['calendar_tick']-a['calendar_tick']>=40 and b['remaining_ticks']<a['remaining_ticks'],'No sustained native cooldown observation')
    require(f'IEDCTR_TFC_MILKING_RECOVERY PASSED nonce={nonce}' in logs,'Missing nonce completion marker')
    return proof


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
        evidence=Path(tempfile.mkdtemp(prefix='tfc-milking-recovery-native-',dir=project/'build'))
        game=evidence/'run';game.mkdir();nonce=secrets.token_hex(16)
        (game/'.iedctr-client-gameplay-nonce').write_text(nonce,encoding='utf-8')
        (game/'options.txt').write_text('onboardAccessibility:false\ntutorialStep:none\nrenderDistance:2\nsimulationDistance:2\n'
            f'maxFps:60\npauseOnLostFocus:false\nsoundCategory_master:0.0\nguiScale:{scale}\nlang:{language}\n',encoding='utf-8')
        init=evidence/'milking.init.gradle'
        init.write_text("""gradle.projectsEvaluated {
    rootProject.extensions.getByName('neoForge').runs.named('client') {
        gameDirectory.set(rootProject.file(System.getenv('IEDCTR_GAMEPLAY_DIR')))
        systemProperty 'iedctr.clientGameplay', 'false'
        systemProperty 'iedctr.clientGameplayNonce', System.getenv('IEDCTR_GAMEPLAY_NONCE')
        systemProperty 'iedctr.clientGameplayDirectory', System.getenv('IEDCTR_GAMEPLAY_DIR')
        systemProperty 'iedctr.tfcMilkingRecoveryNative', 'true'
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
        gradle=['bash',str(project/'gradlew'),'--no-daemon','--init-script',str(init),'--stacktrace','-Pinclude_tfc_milking_runtime=true']
        print(f'TFC recovery isolated native evidence: {evidence} ({args.profile})',flush=True)
        code,expired=run_bounded(gradle+['prepareClientRun'],project,env,evidence/'prepare.log',args.prepare_timeout)
        require(not expired and code==0, f'Milking client preparation failed (exit={code}, timeout={expired})')
        log=evidence/'client.log'
        code,expired=run_bounded(['xvfb-run','-a','-s',f'-screen 0 {width}x{height}x24']+gradle+['runClient'],
                                 project,env,log,args.timeout)
        proof=json.loads((game/'client-tfc-milking-recovery-result.json').read_text())
        logs=log.with_suffix('.observed.log').read_text(errors='replace')
        for name in ('latest.log','debug.log'):
            runtime=game/'logs'/name
            if runtime.is_file():logs+='\n'+runtime.read_text(errors='replace')
        require(current_candidate(project)==source, "Candidate changed during execution")
        validate_proof(proof,nonce,source,game,logs,code,expired,width,height,language,scale)
        print('PASS bounded real TFC recovery proof. Owner visual acceptance is not claimed.')
        return 0
    except (SmokeFailure,OSError,ValueError,subprocess.CalledProcessError) as error:
        print(f'FAIL: {error}',file=sys.stderr);return 1


if __name__=='__main__':
    raise SystemExit(main())
