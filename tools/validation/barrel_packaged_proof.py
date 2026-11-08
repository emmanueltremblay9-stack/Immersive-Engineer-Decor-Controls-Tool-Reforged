"""Independent Barrel-only ordinary-JAR proof. Does not replace cumulative GUI gates."""
from pathlib import Path
import hashlib
import math
import re
import zipfile
from fluid_gestures_native import require, strict_equal
from factory_placer_delivery_evidence import uri_identity
from factory_placer_v2_reference_native import png, decode_png
from client_mixin_smoke import FATAL_PATTERN

MOD = 'immersive_engineer_decor_controls_tool_reforged'
HARNESS = 'iedctr_barrel_validation'
PREFIX = 'com.oblixorprime.engineersdecorreforged.'
CLASSES = [PREFIX+x for x in ('EngineersDecorReforged', 'client.MachineScreen',
    'client.MachineScreen$FluidBarrelScreen', 'menu.MachineMenu', 'utility.MachineLayout', 'utility.MachineBlockEntity',
    'client.FluidBarrelSpriteRenderer')]
TEXTURES = {
    'textures/gui/fluid_barrel/background.png': 'a67a62292a19ba3cb6c7d52946b2b89e1d3cf4af08b2ebcbe2f0b0c9ae5ab5f4',
}



def validate_runtime(proof, report):
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
    require(type(textures) is list and len(textures) == len(TEXTURES) and all(type(r) is dict for r in textures)
            and {r.get('path') for r in textures} == set(TEXTURES), 'Incomplete active artwork provenance')
    with zipfile.ZipFile(jar) as archive:
        names = archive.namelist()
        require(len(names) == len(set(names)), 'Duplicate production archive entries')
        require(not any(n.startswith('com/oblixorprime/iedctrtest/') or 'NativeBarrelPackagedValidation' in n
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
    return proof

STATES = ('empty', 'half', 'full', 'full-hover', 'inventory-pickup', 'inventory-return')
FLUID_IDS = {'water': 'minecraft:water', 'lava': 'minecraft:lava', 'milk': 'minecraft:milk'}
STILL_TEXTURES = {'water': 'minecraft:block/water_still', 'lava': 'minecraft:block/lava_still',
                  'milk': 'neoforge:block/milk_still'}


def integer(obj, key, expected=None):
    value = obj.get(key)
    require(type(value) is int and (expected is None or value == expected), 'Wrong integer '+key)
    return value


def validate_snapshot(snapshot, menu_id, fluid, index):
    require(type(snapshot) is dict, 'Missing synchronized snapshot')
    amount = (0, 16000, 32000, 32000, 32000, 32000)[index]
    carried = int(index == 4)
    for key, value in dict(menu_id=menu_id, fluid_capacity=32000, fluid_amount=amount,
            total_items=1, player_buckets=1-carried, player_slot_2_buckets=1-carried,
            carried_buckets=carried).items():
        integer(snapshot, key, value)
    require(snapshot.get('fluid_id') == ('minecraft:empty' if index == 0 else FLUID_IDS[fluid])
            and snapshot.get('machine_input_empty') is True and snapshot.get('machine_output_empty') is True,
            'Wrong selected fluid or changed machine inventory')


def validate_evidence(proof, nonce, source_sha, width, height, language, fluid):
    require(type(proof) is dict and proof.get('suite') == 'barrel-packaged-native-v1'
            and re.fullmatch('[a-f0-9]{32}', nonce) and proof.get('nonce') == nonce
            and re.fullmatch('[a-f0-9]{40}', source_sha) and proof.get('executed_source_sha') == source_sha,
            'Wrong run/source identity')
    require(fluid in FLUID_IDS and width == 1280 and (language, height) in (('en_us', 720), ('fr_fr', 260)),
            'Unsupported Barrel profile')
    require(proof.get('selected_fluid') == fluid and proof.get('selected_language') == language
            and proof.get('status') == 'passed_bounded_native' and 'error' not in proof
            and proof.get('actual_integrated_world') is True and proof.get('input_backend') == 'XTEST'
            and proof.get('cleanup_released') is True
            and proof.get('rendered_sprite_identity_verified') is False
            and proof.get('natural_collection_verified') is False
            and proof.get('visual_acceptance') is False, 'Incomplete native run or overstated visual scope')
    opening = proof.get('opening')
    require(type(opening) is dict and opening.get('method') == 'XTEST_RMB_BLOCK_HIT'
            and opening.get('hit_block') == '0,80,0' and opening.get('native_right_press_observed') is True
            and opening.get('native_right_release_observed') is True, 'Missing physical Barrel opening')
    menu_id = integer(opening, 'menu_id')
    require(menu_id > 0, 'Invalid opened menu')
    captures, gestures = proof.get('captures'), proof.get('gestures')
    require(type(captures) is list and len(captures) == 6 and type(gestures) is list and len(gestures) == 2,
            'Incomplete Barrel cases')
    previous_outer = previous_render = 0
    screen_id = None
    left, top = (width-201)//2, (height-226)//2
    for index, row in enumerate(captures):
        require(type(row) is dict, 'Malformed capture')
        state = STATES[index]
        require(row.get('name') == 'barrel-'+state+'.png' and row.get('state_name') == state
                and row.get('machine') == 'barrel' and row.get('selected_fluid') == fluid
                and row.get('screen_class') == PREFIX+'client.MachineScreen$FluidBarrelScreen'
                and row.get('setup_seed') is True, 'Wrong Barrel capture identity/setup label')
        integer(row, 'menu_id', menu_id)
        for key, value in dict(width=width, height=height, window_width=width, window_height=height,
                               gui_width=width, gui_height=height).items(): integer(row, key, value)
        require(type(row.get('gui_scale')) in (int, float) and row['gui_scale'] == 1,
                'Wrong/clamped GUI scale')
        require(row.get('recipe_toast_absent') is True and row.get('profiler_chart_disabled') is True
                and row.get('capture_phase') == 'RenderFrameEvent.Post:LOWEST',
                'Early/obscured capture')
        require(integer(row, 'xtest_motion_count') > 0, 'Missing physical pointer motion')
        require(type(row.get('sha256')) is str and re.fullmatch('[a-f0-9]{64}', row['sha256']), 'Missing PNG digest')
        for side in ('client', 'server'): validate_snapshot(row.get(side), menu_id, fluid, index)
        require(strict_equal(row['client'], row['server']), 'Client/server conservation differs')
        g = row.get('geometry')
        require(type(g) is dict, 'Missing renderer geometry')
        outer, render = integer(row, 'capture_outer_frame_id'), integer(row, 'capture_render_id')
        sid = integer(row, 'capture_screen_id')
        require(outer > previous_outer and render > previous_render and sid > 0, 'Stale capture frame')
        previous_outer, previous_render = outer, render
        if screen_id is None: screen_id = sid
        require(sid == screen_id, 'Screen identity changed')
        for key, value in dict(outer_frame_id=outer, render_id=render, screen_id=sid, menu_id=menu_id,
            gui_width=width, gui_height=height, gui_left=left, gui_top=top, image_width=201, image_height=226,
            tank_x=left+92, tank_y=top+35, tank_width=25, tank_height=74).items(): integer(g, key, value)
        require(g.get('state_name') == state, 'Stale geometry state')
        for axis, value in (('x', left+(105 if index == 3 else 8)), ('y', top+(65 if index == 3 else 8))):
            integer(row, 'target_'+axis, value)
            pointer = g.get('pointer_'+axis)
            require(type(pointer) in (int, float) and math.isfinite(pointer) and abs(pointer-value) < 1,
                    'Pointer/render mismatch')
        if index == 3:
            t = g.get('tooltip')
            require(type(t) is dict, 'Missing full tank tooltip')
            x, y = integer(t, 'text_x'), integer(t, 'text_y')
            w, h = integer(t, 'text_width'), integer(t, 'text_height')
            require(w > 0 and h > 0, 'Empty tank tooltip')
            for key, value in dict(frame_left=x-4, frame_top=y-4, frame_right=x+w+4, frame_bottom=y+h+4).items(): integer(t, key, value)
            require(0 <= x-4 < x+w+4 <= width and 0 <= y-4 < y+h+4 <= height, 'Tooltip frame clipped')
        else:
            require('tooltip' not in g, 'Unexpected tooltip masks capture')
        if index < 3:
            sample = row.get('barrel_fluid_pixels')
            require(type(sample) is dict, 'Missing native fluid sample')
            sample_top = (38, 73, 38)[index]
            for key, value in dict(x=left+99, y=top+sample_top, width=16, height=107-sample_top).items(): integer(sample, key, value)
            require(sample.get('fluid_id') == row['client']['fluid_id'], 'Wrong sample fluid identity')
            colors = integer(sample, 'distinct_rgba_colors')
            if index == 0:
                require(colors == 1 and 'still_texture' not in sample and 'tint_argb' not in sample, 'Invalid empty sample')
            else:
                require(colors > 1 and sample.get('still_texture') == STILL_TEXTURES[fluid]
                    and type(sample.get('tint_argb')) is str and re.fullmatch('[a-f0-9]{8}', sample['tint_argb']),
                    'Wrong native sprite/tint or flat fluid sample')
                namespace, path = STILL_TEXTURES[fluid].split(':')
                require(sample.get('atlas_sprite') == STILL_TEXTURES[fluid]
                    and sample.get('active_texture') == namespace+':textures/'+path+'.png'
                    and sample.get('active_texture_pack') == ('mod/neoforge' if fluid == 'milk' else 'vanilla')
                    and type(sample.get('active_texture_sha256')) is str
                    and re.fullmatch('[a-f0-9]{64}', sample['active_texture_sha256']), 'Wrong active fluid texture provenance')
    for index, row in enumerate(gestures):
        require(type(row) is dict and row.get('action') == STATES[index+4]
                and row.get('native_press_observed') is True and row.get('native_release_observed') is True,
                'Missing physical inventory gesture')
        integer(row, 'menu_slot', 2)
        integer(row, 'button_events', 2*(index+1))
        for side in ('client', 'server'):
            validate_snapshot(row.get(side), menu_id, fluid, index+4)
            require(strict_equal(row[side], captures[index+4][side]), 'Gesture/capture state differs')
    return proof


def validate_proof(proof, nonce, source_sha, game, logs, exit_code, timed_out, width, height, language, fluid, report):
    require(not timed_out and type(exit_code) is int and exit_code == 0 and not FATAL_PATTERN.search(logs),
            'Native client failed/timed out')
    require(not list(game.glob('crash-reports/*')) and not list(game.parent.rglob('hs_err_pid*.log')), 'Crash report present')
    validate_evidence(proof, nonce, source_sha, width, height, language, fluid)
    validate_runtime(proof, report)
    folder = game/'barrel-packaged-screenshots'
    require(not folder.is_symlink(), 'Unsafe capture directory')
    for index, row in enumerate(proof['captures']):
        path = folder/row['name']
        png(path, width, height, row['sha256'])
        if index < 3:
            _, _, pixels = decode_png(path)
            sample = row['barrel_fluid_pixels']
            x, y, w, h = (sample[k] for k in ('x', 'y', 'width', 'height'))
            colors = {pixels[4*(py*width+px):4*(py*width+px)+4]
                      for py in range(y, y+h) for px in range(x, x+w)}
            require(len(colors) == sample['distinct_rgba_colors'], 'Reported sample differs from actual PNG pixels')
            require((colors == {bytes.fromhex('142028ff')}) if index == 0 else len(colors) > 1,
                    'Empty backing or occupied native fluid pixels differ')
            if index == 1:
                left, top = row['geometry']['gui_left'], row['geometry']['gui_top']
                upper = {pixels[4*(py*width+px):4*(py*width+px)+4]
                         for py in range(top+38, top+72) for px in range(left+99, left+115)}
                require(upper == {bytes.fromhex('142028ff')}, 'Half-full tank upper region is not empty')
    require(f'IEDCTR_BARREL_PACKAGED PASSED nonce={nonce} source={source_sha}' in logs, 'Missing current-run completion marker')
    return proof
