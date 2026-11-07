"""Fail-closed packaged/default additions to the independent physical gesture proof."""
from pathlib import Path
import hashlib
import math
import re
import zipfile
from fluid_gestures_native import require, strict_equal
from factory_placer_delivery_evidence import uri_identity

MOD = 'immersive_engineer_decor_controls_tool_reforged'
HARNESS = 'iedctr_fluid_validation'
PREFIX = 'com.oblixorprime.engineersdecorreforged.'
CLASSES = [PREFIX+x for x in ('EngineersDecorReforged', 'client.MachineScreen',
    'client.MachineScreen$SmallFluidFunnelScreen', 'client.MachineScreen$PassiveFluidAccumulatorScreen',
    'menu.MachineMenu', 'utility.FluidMachineRedesignLayout', 'utility.MachineBlockEntity',
    'client.RetainedFluidMachineArtwork', 'client.FluidTooltipPositioner')]
TEXTURES = {
    'textures/gui/small_fluid_funnel_v1/background.png': 'c5dcc2ee56fdace5f9bf66456e0a4dca91ce4fddb78887a9a6b60ff914336a64',
    'textures/gui/passive_fluid_accumulator_v1/background.png': 'da2a83c0f8cc68be5f69ad4f3cc392cc4a600206ae39ac8b81142e421e466872',
}



def validate_tooltip_geometry(proof, gui_width, gui_height):
    hovers = proof.get('hovers')
    require(type(hovers) is list and len(hovers) == 12, 'Incomplete tooltip geometry matrix')
    previous_frame = 0
    screen_ids = {}
    roles = ('header', 'tank', 'empty_input', 'empty_output', 'structure', 'occupied_input')
    for index, row in enumerate(hovers):
        require(type(row) is dict, 'Malformed tooltip capture')
        geometry = row.get('tooltip_geometry')
        require(type(geometry) is dict, 'Missing tooltip geometry')
        integer_fields = ('render_id', 'screen_id', 'menu_id', 'text_x', 'text_y', 'text_width',
                          'text_height', 'frame_margin', 'frame_left', 'frame_top', 'frame_right',
                          'frame_bottom', 'gui_width', 'gui_height')
        require(all(type(geometry.get(k)) is int for k in integer_fields), 'Invalid tooltip geometry integer')
        require(all(type(geometry.get(k)) in (int, float) and math.isfinite(geometry[k])
                    for k in ('pointer_x', 'pointer_y')), 'Invalid tooltip geometry pointer')
        require(type(row.get('capture_render_id')) is int and type(row.get('capture_screen_id')) is int
                and row['capture_render_id'] == geometry['render_id'] > previous_frame
                and row['capture_screen_id'] == geometry['screen_id'] > 0, 'Stale tooltip frame/screen')
        previous_frame = geometry['render_id']
        machine = ('funnel', 'accumulator')[index//6]
        role = roles[index%6]
        menu = proof['slot_contracts'][index//6]['menu_id']
        require(row.get('machine') == geometry.get('machine') == machine
                and row.get('role') == geometry.get('role') == role
                and type(row.get('menu_id')) is int and row['menu_id'] == geometry['menu_id'] == menu,
                'Stale tooltip menu/role')
        screen_ids.setdefault(machine, geometry['screen_id'])
        require(screen_ids[machine] == geometry['screen_id']
                and (index < 6 or geometry['screen_id'] > screen_ids['funnel']), 'Stale tooltip screen identity')
        require(geometry['gui_width'] == row.get('gui_width') == gui_width
                and geometry['gui_height'] == row.get('gui_height') == gui_height, 'Wrong tooltip viewport')
        for axis in ('x', 'y'):
            actual, target = row.get('actual_'+axis), row.get('target_'+axis)
            require(type(actual) in (int, float) and math.isfinite(actual) and type(target) is int
                    and geometry['pointer_'+axis] == actual and abs(actual-target) < 1,
                    'Stale tooltip pointer')
        x, y, width, height = (geometry[k] for k in ('text_x', 'text_y', 'text_width', 'text_height'))
        require(width > 0 and height > 0 and geometry['frame_margin'] == 4, 'Invalid tooltip extent/margin')
        expected = dict(frame_left=x-4, frame_top=y-4, frame_right=x+width+4, frame_bottom=y+height+4)
        require(all(geometry[k] == v for k, v in expected.items()), 'Inconsistent tooltip frame geometry')
        require(0 <= expected['frame_left'] < expected['frame_right'] <= gui_width
                and 0 <= expected['frame_top'] < expected['frame_bottom'] <= gui_height,
                'Tooltip frame clipped')

def validate_packaged(proof, report, width, height, scale):
    require(type(proof) is dict and type(report) is dict, 'Missing packaged report')
    runtime = proof.get('packaged_runtime')
    require(type(runtime) is dict, 'Missing runtime origin proof')
    require(report.get('fixture_free_jar') is True and report.get('split_packages') is False
            and report.get('loaded_development_mods') == [HARNESS], 'Unsafe development mod registration')
    jar = Path(report['jar_path']).resolve(strict=True)
    digest = hashlib.sha256(jar.read_bytes()).hexdigest()
    require(jar.suffix == '.jar' and digest == report.get('jar_sha256') == runtime.get('jar_sha256')
            and Path(runtime.get('jar_path', '')).resolve() == jar, 'Wrong production JAR')
    cp, outputs = report.get('runtime_classpath'), report.get('forbidden_main_outputs')
    require(type(cp) is list and type(outputs) is list and outputs
            and all(type(p) is str for p in cp+outputs), 'Missing classpath exclusion proof')
    cp_paths, main_paths = {Path(p).resolve() for p in cp}, {Path(p).resolve() for p in outputs}
    require(jar in cp_paths and not cp_paths.intersection(main_paths), 'Source-output runtime or missing JAR')
    require(runtime.get('harness_mod_id') == HARNESS and runtime.get('fixture_free_jar') is True
            and runtime.get('separate_harness_module') is True and runtime.get('production_selectors_absent') is True
            and type(runtime.get('harness_mod_path')) is str
            and Path(runtime['harness_mod_path']).resolve() != jar, 'Not an independent default-path test mod')
    root = runtime.get('mod_root_uri')
    require(type(root) is str and root and root.endswith('/'), 'Missing authoritative mod root')
    root_identity, jar_identity = uri_identity(root), uri_identity(jar.as_uri())
    origins, textures = runtime.get('class_origins'), runtime.get('active_textures')
    require(type(origins) is list and all(type(r) is dict for r in origins)
            and [r.get('class') for r in origins] == CLASSES, 'Incomplete class provenance')
    require(type(textures) is list and len(textures) == 2 and all(type(r) is dict for r in textures)
            and {r.get('path') for r in textures} == set(TEXTURES), 'Incomplete active artwork provenance')
    with zipfile.ZipFile(jar) as archive:
        names = archive.namelist()
        require(len(names) == len(set(names)), 'Duplicate production archive entries')
        require(not any(n.startswith('com/oblixorprime/iedctrtest/') or 'NativeFluidGestureValidation' in n
                        for n in archive.namelist()), 'Driver leaked into production JAR')
        for row in origins:
            entry = row['class'].replace('.', '/')+'.class'
            require(uri_identity(row.get('code_source_uri')) in (root_identity, jar_identity)
                    and uri_identity(row.get('resource_uri')) == uri_identity(root+entry),
                    'Wrong class module root/code source')
            require(row.get('sha256') == hashlib.sha256(archive.read(entry)).hexdigest(), 'Loaded class bytes differ')
        for row in textures:
            require(row.get('pack') == 'mod/'+MOD and row.get('sha256') == TEXTURES[row['path']]
                    and hashlib.sha256(archive.read('assets/'+MOD+'/'+row['path'])).hexdigest() == row['sha256'],
                    'Artwork overridden or changed')
    opens, tanks = proof.get('native_openings'), proof.get('tank_captures')
    require(type(opens) is list and len(opens) == 2 and type(tanks) is list and len(tanks) == 6,
            'Incomplete opening/tank matrix')
    names = set()
    gw, gh = math.ceil(width/scale), math.ceil(height/scale)
    validate_tooltip_geometry(proof, gw, gh)
    for i, machine in enumerate(('funnel', 'accumulator')):
        opening, menu = opens[i], proof['slot_contracts'][i]['menu_id']
        require(type(opening) is dict and opening.get('machine') == machine
                and type(opening.get('menu_id')) is int and opening['menu_id'] == menu
                and opening.get('hit_block') == '0,80,0' and opening.get('method') == 'XTEST_RMB_BLOCK_HIT'
                and opening.get('native_right_press_observed') is True
                and opening.get('native_right_release_observed') is True, 'Missing native block opening')
        for j, kind in enumerate(('empty', 'half', 'full')):
            row = tanks[3*i+j]
            require(type(row) is dict, 'Malformed tank capture')
            cap, amount = row.get('capacity'), row.get('amount')
            require(type(cap) is int and cap == (3000 if i == 0 else 4000)
                    and type(amount) is int and amount == (0, cap//2, cap)[j], 'Wrong tank fill')
            require(row.get('setup_seed') is True and row.get('machine') == machine
                    and type(row.get('menu_id')) is int and row['menu_id'] == menu
                    and row.get('name') == f'{machine}-tank-{kind}.png', 'Wrong tank identity/setup label')
            require(type(row.get('client')) is dict and strict_equal(row['client'], row.get('server'))
                    and row['client'].get('menu_id') == menu
                    and strict_equal(row['client'].get('state'), [0,0,1,0,0,0,amount])
                    and type(row['client'].get('total')) is int and row['client']['total'] == 1,
                    'Tank state not conserved')
            expected = dict(width=width, height=height, gui_width=gw, gui_height=gh,
                            window_width=width, window_height=height)
            require(all(type(row.get(key)) is int and row[key] == value for key, value in expected.items())
                    and type(row.get('gui_scale')) in (int, float) and row['gui_scale'] == scale, 'Wrong tank viewport')
            screen = PREFIX+'client.MachineScreen$'+('SmallFluidFunnelScreen' if i == 0 else 'PassiveFluidAccumulatorScreen')
            require(row.get('screen_class') == screen and type(row.get('sha256')) is str
                    and re.fullmatch('[a-f0-9]{64}', row['sha256']), 'Wrong tank screen/digest')
            require(type(row.get('xtest_motion_count')) is int and row['xtest_motion_count'] > 0,
                    'Missing native tank delivery')
            require((row.get('target_x'), row.get('target_y')) == ((gw-256)//2+8, (gh-256)//2+8),
                    'Wrong tank capture pointer')
            for axis, logical, physical in [('x', gw, width), ('y', gh, height)]:
                raw, actual, target = row.get('raw_mouse_'+axis), row.get('actual_'+axis), row.get('target_'+axis)
                require(type(raw) in (int, float) and math.isfinite(raw) and 0 <= raw < physical
                        and type(actual) in (int, float) and math.isfinite(actual) and type(target) is int
                        and abs(raw*logical/physical-actual) < 0.000001 and abs(actual-target) < 1,
                        'Tank pointer not delivered')
            require(row['name'] not in names, 'Duplicate tank capture')
            names.add(row['name'])
    require(all(row.get('recipe_toast_absent') is True for row in proof['hovers']+tanks),
            'Recipe toast masks capture')
    return proof
