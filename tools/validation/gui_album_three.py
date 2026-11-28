#!/usr/bin/env python3
"""Capture seven ordinary GUIs and exercise the GUI11 V5-Corrigee Electrical dial in a real client."""
import argparse
import hashlib
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
import urllib.request
from client_mixin_smoke import FATAL_PATTERN, SmokeFailure, run_bounded
from factory_placer_v2_reference_native import png, decode_png




def require(condition, message):
    if not condition:
        raise SmokeFailure(message)


CASES = ['factory-hopper', 'metal-crafting-table', 'small-electrical-furnace', 'small-waste-incinerator', 'labeled-crate', 'factory-dropper', 'small-lab-furnace']
SCREENS = [
    'com.oblixorprime.engineersdecorreforged.client.FactoryStudyScreen',
    'com.oblixorprime.engineersdecorreforged.client.MachineScreen$MetalCraftingTableScreen',
    'com.oblixorprime.engineersdecorreforged.client.MachineScreen$SmallElectricalFurnaceScreen',
    'com.oblixorprime.engineersdecorreforged.client.MachineScreen$SmallWasteIncineratorScreen',
    'com.oblixorprime.engineersdecorreforged.client.LabeledCrateScreen',
    'com.oblixorprime.engineersdecorreforged.client.FactoryStudyScreen',
    'com.oblixorprime.engineersdecorreforged.client.FactoryStudyScreen',
]
V5C_LAYOUTS = {0: (18, 54), 5: (15, 51), 6: (11, 47)}
V5C_OCCUPIED_SLOTS = {0: (18, 54), 5: (15, 51), 6: (5, 41)}
ELECTRICAL_INPUT = 'synthetic XTest pointer and Electrical Furnace button events through GLFW; server-opened menus; no human input'
ELECTRICAL_CASES = ['empty-off','left-one','left-two','left-three','left-wrap','right-wrap',
                    'rapid-left-three','middle-inert','border-inert','reopened','half-energy','full-energy']
ELECTRICAL_SPEEDS = [0,1,2,3,0,3,2,2,2,2,2,2]
ELECTRICAL_ENERGY = [0,0,0,0,0,0,0,0,0,0,16000,32000]
ELECTRICAL_CLICKS = [0,1,1,1,1,1,3,1,1,0,0,0]
ELECTRICAL_BUTTONS = [0,1,1,1,1,3,1,2,1,0,0,0]
ELECTRICAL_DIAL_X, ELECTRICAL_DIAL_Y = 226, 110
ELECTRICAL_BORDER_X, ELECTRICAL_BORDER_Y = 213, 110


def v5c_hopper_outline(width, height, gui_scale, row):
    """Project the V5-Corrigee 321x321 canvas and its measured Hopper slots."""
    logical_width = math.ceil(width / gui_scale)
    logical_height = math.ceil(height / gui_scale)
    margin = min(6.0, (min(logical_width, logical_height) - 1.0) / 2.0)
    view_scale = min(1.0, (logical_width - 2 * margin) / 321.0,
                     (logical_height - 2 * margin) / 321.0)
    origin_x = (logical_width - 321.0 * view_scale) / 2.0
    origin_y = (logical_height - 321.0 * view_scale) / 2.0
    x = origin_x + 43.0 * view_scale
    y = origin_y + (66.0 + 24.0 * row) * view_scale
    left = round(x * gui_scale)
    top = round(y * gui_scale)
    right = round((x + 18.0 * view_scale) * gui_scale) - 1
    bottom = round((y + 18.0 * view_scale) * gui_scale) - 1
    return left, top, right, bottom


def validate_selection_pixels(selections,game,width,height,scale):
    """Compare the same border in selected/unselected real screenshots, not art mockups.

    A top/bottom corner-marker delta proves a rendered marker reaches the expected row;
    it is not an exact-color aesthetic approval or exhaustive 18-slot test.
    """
    require(type(scale) is int and scale in (1,2) and width>=321*scale and height>=321*scale,
            'Selection panel outside viewport')
    images=[]
    for record in selections:
        w,h,pixels=decode_png(game/record['capture'])
        require((w,h)==(width,height),'Selection viewport differs')
        images.append(pixels)
    for row in range(3):
        active=images[row];inactive=images[(row+1)%3];other_inactive=images[(row+2)%3]
        left,top,right,bottom=v5c_hopper_outline(width,height,scale,row)
        border={(x,top) for x in range(left,right+1)} | {(x,bottom) for x in range(left,right+1)}
        border|={(left,y) for y in range(top,bottom+1)} | {(right,y) for y in range(top,bottom+1)}
        changed=0
        for x,y in border:
            offset=(y*width+x)*4
            if (active[offset:offset+3]!=inactive[offset:offset+3]
                and inactive[offset:offset+3]==other_inactive[offset:offset+3]): changed+=1
        require(changed>=16*scale*scale,'Factory Study V5-Corrigee Hopper outline missing/misaligned at row '+str(row))


def validate(proof,nonce,sha,jar,jar_sha,game,width,height,gui_scale=2):
    for key,value in dict(suite='gui-album-three-v5c',status='observed',nonce=nonce,
                         executed_source_sha=sha,visual_acceptance=False,physical_os_input=True,human_input=False,
                         input_method=ELECTRICAL_INPUT,synthetic_button_events=20).items():
        require(type(proof.get(key)) is type(value) and proof[key]==value,'Proof differs: '+key)
    runtime=proof.get('packaged_runtime',{})
    require(runtime.get('jar_path')==str(jar.resolve()) and runtime.get('jar_sha256')==jar_sha,'Wrong production JAR')
    require(runtime.get('fixture_free_jar') is True and runtime.get('separate_harness_module') is True,'Isolation missing')
    cases=proof.get('cases',[])
    require([x.get('case') for x in cases]==CASES,'Missing screens')
    for index,row in enumerate(cases):
        require(row.get('screen')==SCREENS[index],'Wrong screen')
        require(row.get('server_menu_acknowledged') is True,'Missing server menu acknowledgement')
        require(row.get('gui_scale') == gui_scale,'Wrong native GUI scale')
        require(type(row.get('visible_nonempty_slots')) is int and row['visible_nonempty_slots'] > 0,'Empty visual evidence')
        if index in V5C_LAYOUTS:
            machine_slots,total_slots=V5C_LAYOUTS[index]
            require(row.get('visible_machine_slots')==machine_slots,'Wrong V5-Corrigee machine slot count')
            require(row.get('total_menu_slots')==total_slots,'Wrong V5-Corrigee total menu slot count')
            machine_occupied,total_occupied=V5C_OCCUPIED_SLOTS[index]
            require(row.get('visible_nonempty_machine_slots')==machine_occupied,'Wrong V5-Corrigee occupied machine stack count')
            require(row.get('visible_nonempty_slots')==total_occupied,'V5-Corrigee GUI occupied-stack count differs')
        require(re.fullmatch('[0-9a-f]{64}',row.get('screen_class_sha256','')),'Screen provenance missing')
        require(row.get('capture')==CASES[index]+'.png','Unexpected capture name')
        png(game/row['capture'],width,height,row.get('capture_sha256'))

    selections=proof.get('hopper_selections',[])
    require(len(selections)==3,'Missing three Hopper selection rows')
    for row,c in enumerate(selections):
        require(all(type(c.get(k)) is int for k in ('server_selection','client_selection','ring_x','ring_y','visible_nonempty_slots')),'Selection integer evidence missing')
        require(all(c.get(k)==cases[0].get(k) for k in ('screen','case','screen_class_sha256')),'Selection screen provenance differs')
        require(c.get('server_selection')==row*6 and c.get('client_selection')==row*6,
                'Hopper selection server/client mismatch')
        require(c.get('ring_x')==43 and c.get('ring_y')==66+24*row,'Hopper ring/slot alignment differs')
        require(c.get('selection_source')=='server-seeded ContainerData; not user selection','Selection origin differs')
        require(c.get('server_menu_acknowledged') is True and c.get('gui_scale')==gui_scale,'Selection menu/scale mismatch')
        require(c.get('visible_nonempty_slots')==54,'Hopper inventory not full')
        require(type(c.get('selection_render_frames')) is int and c['selection_render_frames']>=3,'Selection not rendered stably')
        require(c.get('capture')=='factory-hopper'+('' if row==0 else '-selection-row-'+str(row))+'.png','Unexpected selection capture')
        png(game/c['capture'],width,height,c.get('capture_sha256'))
    validate_selection_pixels(selections,game,width,height,gui_scale)
    require(selections[0]==cases[0],'Initial Hopper observation differs')
    require(cases[5].get('visible_nonempty_slots')==51,'Dropper 15 machine slots and 36 player slots are not all present')
    require(cases[5].get('dropper_filter_gate')==2,'Dropper capture did not use the AND filter gate')
    require(cases[5].get('dropper_external_powered') is False,'Dropper capture fixture was powered')
    validate_electrical_controls(proof.get('electrical_controls',{}),cases[2],game,width,height,gui_scale)


REMAINING = [
    ('04','factory_placer','FactoryPlacerV2ReferenceScreen',18),
    ('05','small_freezer','ThermalVerticalScreen',3),
    ('06','small_mineral_smelter','ThermalVerticalScreen',2),
    ('09','small_block_breaker','MachineScreen$SmallBlockBreakerScreen',1),
    ('10','small_tree_cutter','TreeCutterV3ReferenceScreen',1),
    ('12','small_milking_machine','MachineScreen$SmallMilkingMachineScreen',2),
    ('13','small_fluid_funnel','MachineScreen$SmallFluidFunnelScreen',2),
    ('14','passive_fluid_accumulator','MachineScreen$PassiveFluidAccumulatorScreen',2),
    ('15','fluid_barrel','BarrelReferenceScreen',2),
    ('18','labeled-crate-label-editor','LabeledCrateEditScreen',0),
]
CLIENT_PREFIX = 'com.oblixorprime.engineersdecorreforged.client.'


def validate_remaining(proof, project, game, width, height, gui_scale):
    source = json.loads((project/'docs/testing/gui11-v5c-runtime-map.json').read_text())['surfaces']
    rows = proof.get('remaining_cases',[])
    require([r.get('id') for r in rows] == [c[0] for c in REMAINING], 'Required remaining panels absent or out of order')
    for row,(panel,name,screen,machine_slots) in zip(rows,REMAINING):
        reference=source[panel]
        require(row.get('case')==name and row.get('screen')==CLIENT_PREFIX+screen,'Wrong screen for '+panel)
        require(re.fullmatch('[0-9a-f]{64}',row.get('screen_class_sha256','')),'Missing screen provenance for '+panel)
        require(row.get('texture_sha256')==reference['sha256'],'Wrong active PNG for '+panel)
        require(re.fullmatch(r'\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d+Z',row.get('observed_utc','')),
                'Undated capture for '+panel)
        require(row.get('capture')==name+'.png','Wrong screenshot name for '+panel)
        png(game/row['capture'],width,height,row.get('capture_sha256'))
        if panel=='18':
            require(row.get('opening')=='registered label client bridge'
                    and row.get('typed_label')=='V5C CONFIRMED' and row.get('server_confirmed') is True,
                    'Missing editor confirmation in the integrated server')
            continue
        require(row.get('server_menu_acknowledged') is True and row.get('gui_scale')==gui_scale
                and row.get('machine_slots')==machine_slots,'Unexpected menu or scale for '+panel)
        slots=row.get('slots',[])
        require([s.get('index') for s in slots]==list(range(machine_slots+36)),'Unexpected inventory span for '+panel)
        expected=reference['menu_slots']
        if panel=='06':  # Smelter omits the historical reserved slot from the actual menu.
            expected=[p for p in expected if p is not None]
        require(len(expected)==len(slots),'Artwork/menu count mismatch for '+panel)
        for slot,point in zip(slots,expected):
            if point is None:
                require(panel=='05' and slot.get('index')==1 and slot.get('active') is False,
                        'Wrong state for the reserved Freezer slot')
            else:
                require(slot.get('active') is True and [slot.get('x'),slot.get('y')]==point,
                        'Slot %s is not active at its approved coordinate for %s' % (slot.get('index'),panel))
        require(all(type(row.get(key)) is int and row[key]>=0 for key in ('panel_left','panel_top')) and
                0 < row.get('panel_width',0) <= (width+gui_scale-1)//gui_scale and
                0 < row.get('panel_height',0) <= (height+gui_scale-1)//gui_scale,
                'Panel extent outside viewport for '+panel)
    tree=rows[4]
    require(tree['slots'][0].get('inventory_index')==0 and tree['slots'][0].get('count')==4
            and tree['slots'][1].get('inventory_index')==9 and tree['slots'][1].get('count')==0,
            'Unexpected Tree Cutter output/player fixture')
    require(tree.get('native_inventory')==[
        dict(step='TREE_MISS',output_count=4,carried_count=0,player_count=0,server_acknowledged=True),
        dict(step='TREE_PICKUP',output_count=0,carried_count=4,player_count=0,server_acknowledged=True),
        dict(step='TREE_PLACE',output_count=0,carried_count=0,player_count=4,server_acknowledged=True),
    ], 'Tree Cutter physical hitbox/pickup/placement proof incomplete')
    hover=tree.get('output_hover_capture',{})
    require(hover.get('capture')=='small_tree_cutter-output-hover.png','Missing native output hover photo')
    png(game/hover['capture'],width,height,hover.get('capture_sha256'))


def validate_electrical_controls(proof,base_case,game,width,height,scale):
    for key,value in dict(suite='electrical-furnace-native-controls-v1',complete=True,
                         input_method=ELECTRICAL_INPUT,native_presses_sent=10,native_presses_observed=10,
                         synthetic_button_events=20,fixture_energy_capacity=32000,
                         fixture_has_recipe=False,visual_acceptance=False).items():
        require(type(proof.get(key)) is type(value) and proof[key]==value,'Electrical proof differs: '+key)
    initial=proof.get('initial_menu_id')
    require(type(initial) is int and initial>0,'Electrical initial menu missing')
    records=proof.get('observations',[])
    require([r.get('case') for r in records]==ELECTRICAL_CASES,'Electrical native scenarios missing')
    for i,row in enumerate(records):
        expected=dict(expected_speed=ELECTRICAL_SPEEDS[i],client_speed=ELECTRICAL_SPEEDS[i],
                      expected_energy=ELECTRICAL_ENERGY[i],client_energy=ELECTRICAL_ENERGY[i],client_capacity=32000,
                      native_presses=ELECTRICAL_CLICKS[i],x11_button=ELECTRICAL_BUTTONS[i],
                      click_x=ELECTRICAL_BORDER_X if i==8 else ELECTRICAL_DIAL_X,
                      click_y=ELECTRICAL_DIAL_Y,gui_scale=scale)
        for key,value in expected.items():
            require(type(row.get(key)) is int and row[key]==value,'Electrical observation differs: '+row['case']+'/'+key)
        require(row.get('screen')==base_case.get('screen')==SCREENS[2],'Electrical screen differs from packaged screen')
        require(type(row.get('render_frames')) is int and row['render_frames']>=3,'Electrical state not rendered stably')
        menu_id=row.get('menu_id')
        require(type(menu_id) is int and (menu_id==initial if i<9 else menu_id>initial),'Electrical reopen identity differs')
        require(row.get('input_source')==('XTest via GLFW' if ELECTRICAL_CLICKS[i] else
                'server fixture or programmatic reopen; no button input'),'Electrical input origin differs')
        server=row.get('server',{})
        for key,value in dict(matching_menu=True,menu_id=menu_id,speed=ELECTRICAL_SPEEDS[i],
                             machine_speed=ELECTRICAL_SPEEDS[i],energy=ELECTRICAL_ENERGY[i],capacity=32000,
                             cursor_empty=True,machine_empty=True).items():
            require(type(server.get(key)) is type(value) and server[key]==value,'Electrical server acknowledgement differs: '+key)
        name='electrical-'+ELECTRICAL_CASES[i]+'.png'
        require(row.get('capture')==name,'Unexpected electrical screenshot name')
        png(game/name,width,height,row.get('capture_sha256'))
    require(len({r['menu_id'] for r in records[9:]})==1,'Electrical menu changed after reopen')
    events=proof.get('input_events',[])
    expected_events=[(i,n) for i,count in enumerate(ELECTRICAL_CLICKS) for n in range(count)]
    require(len(events)==len(expected_events),'Native Electrical callback count differs')
    for event,(i,_) in zip(events,expected_events):
        require(event.get('case')==ELECTRICAL_CASES[i],'Electrical callbacks reordered or absent')
        require(type(event.get('glfw_button')) is int and event['glfw_button']=={1:0,2:2,3:1}[ELECTRICAL_BUTTONS[i]],
                'Electrical callback button differs')
        for axis,value in [('x',ELECTRICAL_BORDER_X if i==8 else ELECTRICAL_DIAL_X),('y',ELECTRICAL_DIAL_Y)]:
            actual=event.get(axis)
            require(type(actual) in (int,float) and math.isfinite(actual) and abs(actual-value)<=1,
                    'Electrical callback misses independent acceptance point')
        require(type(event.get('handled')) is bool and (event['handled'] or i>=7),'Electrical dial did not handle click')
        # In particular, all three rapid callbacks see the same pre-acknowledgement
        # speed. The final server speed must still count all three increments.
        require(type(event.get('client_speed_during_callback')) is int
                and event['client_speed_during_callback']==ELECTRICAL_SPEEDS[i-1],
                'Electrical click used unexpected client state or burst was not queued together')
    validate_electrical_pixels(records,game,width,height,scale)


def electrical_panel_origin(width,height,scale):
    require(type(scale) is int and scale in (1,2) and width>=291*scale and height>=284*scale,
            'Electrical panel outside viewport')
    return ((math.ceil(width/scale)-291)//2)*scale,((math.ceil(height/scale)-284)//2)*scale


def validate_electrical_pixels(records,game,width,height,scale):
    """Independent native pixel checks: actual grip rotation and FE fill.

    Geometric acceptance comes from the retained artwork, not renderer helpers.
    These reject a moving lamp with a static grip, an always-full meter, opaque
    rectangle fill, reversed fill, and frame overpainting. They do not
    replace visual acceptance by the owner.
    """
    left,top=electrical_panel_origin(width,height,scale)
    images=[]
    for row in records:
        w,h,pixels=decode_png(game/row['capture'])
        require((w,h)==(width,height),'Electrical screenshot viewport differs')
        images.append(pixels)
    def rgb(image,x,y):
        offset=((top+y)*width+left+x)*4
        return image[offset:offset+3]
    # Probe the mapped GUI11 rotary cap, excluding the detached detent markings.
    # A lamp-only implementation cannot satisfy these four grip comparisons.
    core=[(x,y) for y in range(102*scale,119*scale) for x in range(218*scale,236*scale)]
    for a in range(4):
        for b in range(a+1,4):
            changed=sum(rgb(images[a],x,y)!=rgb(images[b],x,y) for x,y in core)
            require(changed>=8*scale*scale,'Electrical grip does not visibly rotate between '+str(a)+' and '+str(b))
    for earlier,later in [(0,4),(3,5),(2,6),(2,7),(2,8),(2,9),(2,10),(2,11)]:
        require(all(rgb(images[earlier],x,y)==rgb(images[later],x,y) for x,y in core),
                'Electrical repeated/reopened detent does not preserve the same grip orientation')

    # The measured GUI11 energy well has a 44x6 logical glass interior at x=129..173,
    # y=107..113. Half charge fills its left half; the frame remains unchanged.
    glass=[(x,y) for y in range(108*scale,113*scale) for x in range(130*scale,173*scale)]
    empty,half,full=images[9],images[10],images[11]
    for x,y in glass:
        logical_x=x//scale
        expected=full if logical_x < 151 else empty
        require(rgb(half,x,y)==rgb(expected,x,y),'Electrical half FE does not fill the left half of the glass')
    changed=sum(rgb(empty,x,y)!=rgb(full,x,y) for x,y in glass)
    require(changed>=.9*len(glass),'Electrical full FE glass is not filled')
    require(sum(sum(rgb(full,x,y))-sum(rgb(empty,x,y)) for x,y in glass)>15*len(glass),
            'Electrical full FE is not brighter than empty')


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
    parser.add_argument('--all-v5c', action='store_true', help='Require the ten additional V5C screen observations')
    parser.add_argument('--language', choices=['en_us','fr_fr'], default='en_us')
    parser.add_argument('--width', type=int, default=1280)
    parser.add_argument('--gui-scale', type=int, choices=[1,2], default=2)
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
    (game/'options.txt').write_text('onboardAccessibility:false\ntutorialStep:none\nrenderDistance:2\nsimulationDistance:2\nmaxFps:60\npauseOnLostFocus:false\nsoundCategory_master:0.0\nguiScale:'+str(args.gui_scale)+'\nlang:'+args.language+'\n')
    init=evidence/'gui-album-three.init.gradle'
    init.write_text("""gradle.projectsEvaluated {
 rootProject.sourceSets.factoryPlacerNativeValidation.java.srcDir(rootProject.file('src/guiAlbumThree/java'))
 rootProject.extensions.getByName('neoForge').runs.named('crateLabelPackagedClient') {
  systemProperty 'iedctr.crateLabelPackagedNative', 'false'
  systemProperty 'iedctr.guiAlbumThree', 'true'
  systemProperty 'iedctr.gui11RemainingAlbum', System.getenv('IEDCTR_GUI11_REMAINING_ALBUM')
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
             IEDCTR_GUI11_REMAINING_ALBUM=str(args.all_v5c).lower(),IEDCTR_SCREEN_WIDTH=str(args.width),IEDCTR_SCREEN_HEIGHT=str(args.height),LIBGL_ALWAYS_SOFTWARE='1',ALSOFT_DRIVERS='null')
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
        validate(proof,nonce,sha,jar,jar_sha,game,args.width,args.height,args.gui_scale)
        if args.all_v5c:
            validate_remaining(proof,project,game,args.width,args.height,args.gui_scale)
            print('OBSERVED ten additional V5C panels, including physical Tree Cutter clicks and label confirmation.',flush=True)
        final_sha = subprocess.check_output(['git','rev-parse','HEAD'],cwd=project,text=True).strip()
        require(clean() and final_sha == sha and hashlib.sha256(jar.read_bytes()).hexdigest()==jar_sha,
                'Source/JAR changed during run')
        print('OBSERVED seven native screens, three Hopper rows, native Electrical Furnace dial clicks/reopen and FE 0/16000/32000. Screenshots still require independent visual review; this is bounded interaction evidence, not full gameplay acceptance.')
        return 0
    except (SmokeFailure,OSError,ValueError,KeyError) as error:
        print('FAIL: '+str(error),file=sys.stderr);return 1


if __name__=='__main__':
    raise SystemExit(main())
