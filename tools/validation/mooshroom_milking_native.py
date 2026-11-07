#!/usr/bin/env python3
"""Real vanilla Mooshroom client evidence in the TFC profile; synthetic validator tests are not gameplay proof."""
from __future__ import annotations
import argparse
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
import uuid
from client_mixin_smoke import FATAL_PATTERN, SmokeFailure, run_bounded
from milking_qualification_native import (require, exact, expected_geometry, safe_png,
    decode_png, crop_pixels, current_candidate, BACKGROUND_SHA, MILK_SHA, SCREEN_CLASS)

SUITE='native-mooshroom-milking-v1'
PROFILES={'fr-1280x720-scale2': (1280,720,2,'fr_fr')}
PHASES=('red-portrait','red-identity-tooltip','brown-portrait','brown-identity-tooltip','baby-red','baby-brown')


def integer(obj,key,minimum=None):
    value=obj.get(key)
    require(type(value) is int and (minimum is None or value>=minimum),'Invalid '+key)
    return value


def snapshot(s,variant,baby=False):
    require(isinstance(s,dict),'Missing native snapshot')
    for key,value in dict(source='minecraft:mooshroom',variant=variant,
            server_source='NONE' if baby else 'VANILLA',client_source='NONE' if baby else 'VANILLA',
            remaining_ticks=0,client_remaining_ticks=0,total_ticks=0,client_total_ticks=0,
            client_state_code=0 if baby else 1,amount=0 if baby else 1000,client_amount=0 if baby else 1000).items():exact(s,key,value)
    uid=s.get('server_uuid')
    try:require(str(uuid.UUID(uid))==uid,'Invalid source UUID')
    except (ValueError,TypeError,AttributeError):raise SmokeFailure('Invalid source UUID')
    exact(s,'client_uuid',uid)
    entity=integer(s,'server_entity_id',1);exact(s,'client_entity_id',entity)
    if baby:
        exact(s,'baby_rejected',True)
        for side in ('server','client'):
            require(side+'_selected_uuid' in s,'Missing explicit selected UUID')
            exact(s,side+'_selected_entity_id',0);exact(s,side+'_selected_uuid',None)
    else:
        for side in ('server','client'):
            exact(s,side+'_name','Native '+variant+' Mooshroom')
            exact(s,side+'_selected_entity_id',entity);exact(s,side+'_selected_uuid',uid)
    return uid,entity


def validate_hover(c,geometry,width,height,expected):
    native=c.get('native_input');require(isinstance(native,dict),'Missing native input evidence')
    x=integer(c,'hover_x');y=integer(c,'hover_y')
    left=geometry['presentation_gui_left'];top=geometry['presentation_gui_top']
    def hitbox(px,py):return left+27<=px<left+74 and top+34<=py<top+75
    exact(c,'tooltip_hovered',expected)
    require(hitbox(x,y)==expected,'Cursor hover disagrees with capture')
    for key,value in dict(glfw_focused=True,glfw_visible=True,glfw_cursor_mode=212993,
            window_width=width,window_height=height).items():exact(native,key,value)
    if expected:require(integer(native,'settled_hover_ticks',8)<6000,'Invalid hover settling')
    for prefix in ('glfw','minecraft'):
        coordinates=[]
        for axis,extent,gui_extent in (('x',width,geometry['gui_width']),('y',height,geometry['gui_height'])):
            coordinate=native.get(prefix+'_cursor_'+axis)
            require(type(coordinate) in (int,float) and math.isfinite(coordinate) and 0<=coordinate<extent,'Invalid native cursor coordinate')
            coordinates.append(int(coordinate*gui_extent/extent))
        require(hitbox(*coordinates)==expected,'Native cursor does not match requested hover')
        if prefix=='minecraft':require(coordinates==[x,y],'Hover differs from actual Minecraft cursor')


def validate_proof(proof,nonce,source_sha,game,logs,exit_code,timed_out,width=1280,height=720,language='fr_fr',gui_scale=2):
    require(isinstance(proof,dict),'Missing proof')
    require(not timed_out and type(exit_code) is int and exit_code==0 and not FATAL_PATTERN.search(logs),'Unclean client execution')
    require(not list(game.glob('crash-reports/*')) and not list(game.parent.glob('**/hs_err_pid*.log')),'Native crash evidence')
    require(re.fullmatch('[a-f0-9]{40}',source_sha) is not None and re.fullmatch('[a-f0-9]{32}',nonce) is not None,'Invalid run identity')
    require((width,height,gui_scale,language)==PROFILES['fr-1280x720-scale2'],'Unsupported viewport')
    for key,value in dict(suite=SUITE,nonce=nonce,executed_source_sha=source_sha,status='passed_bounded_smoke',
            source_background_sha256=BACKGROUND_SHA,source_milk_sha256=MILK_SHA,
            actual_integrated_world=True,native_visual_acceptance=False,requested_language=language,selected_language=language,
            screen_class=SCREEN_CLASS,tfc_version='4.2.12',native_tick_production=True).items():exact(proof,key,value)
    require('error' not in proof,'Error in passing proof')
    geometry=expected_geometry(width,height,gui_scale)
    for key,value in geometry.items():
        if not key.startswith(('tank_','presentation_')):exact(proof,key,value)
    captures=proof.get('captures');require(isinstance(captures,list) and len(captures)==6,'Incomplete native captures')
    seen={};identities={};images={};portraits={};menus={}
    folder=game/'mooshroom-milking-screenshots'
    for c in captures:
        require(isinstance(c,dict),'Invalid capture')
        phase=c.get('phase');require(phase in PHASES and phase not in seen,'Duplicate or invalid phase');seen[phase]=c
        variant='RED' if 'red' in phase else 'BROWN';baby=phase.startswith('baby-')
        identity=snapshot(c,variant,baby);group=('baby-' if baby else '')+variant
        if group in identities:require(identity==identities[group],'Source identity changed within variant')
        identities[group]=identity
        exact(c,'name',phase+'.png');exact(c,'screen_class',SCREEN_CLASS)
        for key,value in geometry.items():
            if not key.startswith(('tank_','presentation_')):exact(c,key,value)
        menu=integer(c,'menu_id',1);exact(c,'server_menu_id',menu)
        if group in menus:require(menus[group]==menu,'Native menu changed within scenario')
        menus[group]=menu
        file=safe_png(folder,c['name'],c.get('sha256'));w,h,pixels=decode_png(file)
        require((w,h)==(width,height),'Wrong screenshot viewport');images[phase]=pixels
        for key,value in dict(portrait_x=(geometry['presentation_gui_left']+27)*gui_scale,
                portrait_y=(geometry['presentation_gui_top']+34)*gui_scale,portrait_width=47*gui_scale,
                portrait_height=41*gui_scale,portrait_name=phase+'-portrait.png').items():exact(c,key,value)
        crop=safe_png(folder,c['portrait_name'],c.get('portrait_sha256'));cw,ch,cp=decode_png(crop)
        require((cw,ch)==(c['portrait_width'],c['portrait_height']),'Wrong portrait dimensions')
        require(cp==crop_pixels(pixels,w,c['portrait_x'],c['portrait_y'],cw,ch),'Portrait is not screenshot crop')
        require(len({cp[i:i+4] for i in range(0,len(cp),4)})>=16,'Blank portrait crop')
        portraits[phase]=cp
        validate_hover(c,geometry,width,height,phase.endswith('-identity-tooltip'))
    require(len(set(menus.values()))==4,'Scenario menus were not distinct')
    require(len(set(identities.values()))==4,'Variant or baby reused an animal identity')
    for variant in ('red','brown'):
        require(images[variant+'-portrait']!=images[variant+'-identity-tooltip'],'Missing rendered identity tooltip difference')
    require(portraits['red-portrait']!=portraits['brown-portrait'],'Red and brown portrait pixels are identical')
    for variant in ('red','brown'):
        for baby in ('red','brown'):
            require(portraits[variant+'-portrait']!=portraits['baby-'+baby],'Adult portrait equals unselected baby background')
    for key in ('production_checks','baby_checks'):
        checks=proof.get(key);require(isinstance(checks,list) and len(checks)==2,'Missing '+key)
        require({c.get('variant') for c in checks if isinstance(c,dict)}=={'RED','BROWN'},'Missing variant in '+key)
        for c in checks:
            if key=='production_checks':
                for k,v in dict(bucket=True,tank_amount=1000,cadence_ticks=120,no_early_repeat=True).items():exact(c,k,v)
                require(integer(c,'second_tick',1)-integer(c,'first_tick',0)==120,'Wrong machine cadence')
            else:
                for k,v in dict(baby=True,amount=0,bucket_input=1,bucket_output=0,source='NONE').items():exact(c,k,v)
                integer(c,'observed_ticks',240)
    require(f'IEDCTR_MOOSHROOM_MILKING PASSED nonce={nonce}' in logs,'Missing nonce completion marker')
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
        evidence=Path(tempfile.mkdtemp(prefix='mooshroom-milking-native-',dir=project/'build'))
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
        systemProperty 'iedctr.mooshroomMilkingNative', 'true'
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
        print(f'Mooshroom isolated native evidence: {evidence} ({args.profile})',flush=True)
        code,expired=run_bounded(gradle+['prepareClientRun'],project,env,evidence/'prepare.log',args.prepare_timeout)
        require(not expired and code==0, f'Milking client preparation failed (exit={code}, timeout={expired})')
        log=evidence/'client.log'
        code,expired=run_bounded(['xvfb-run','-a','-s',f'-screen 0 {width}x{height}x24']+gradle+['runClient'],
                                 project,env,log,args.timeout)
        proof=json.loads((game/'client-mooshroom-milking-result.json').read_text())
        logs=log.with_suffix('.observed.log').read_text(errors='replace')
        for name in ('latest.log','debug.log'):
            runtime=game/'logs'/name
            if runtime.is_file():logs+='\n'+runtime.read_text(errors='replace')
        require(current_candidate(project)==source, "Candidate changed during execution")
        validate_proof(proof,nonce,source,game,logs,code,expired,width,height,language,scale)
        print('PASS bounded real Mooshroom proof. Owner visual acceptance is not claimed.')
        return 0
    except (SmokeFailure,OSError,ValueError,subprocess.CalledProcessError) as error:
        print(f'FAIL: {error}',file=sys.stderr);return 1


if __name__=='__main__':
    raise SystemExit(main())
