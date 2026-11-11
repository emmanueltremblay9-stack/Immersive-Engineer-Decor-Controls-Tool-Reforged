"""Independent Barrel-only ordinary-JAR proof. Does not replace cumulative GUI gates."""
from pathlib import Path
import hashlib
import math
import re
import zipfile
import struct
from fluid_gestures_native import require, strict_equal
from factory_placer_delivery_evidence import uri_identity
from factory_placer_v2_reference_native import png, decode_png
from client_mixin_smoke import FATAL_PATTERN

MOD = 'immersive_engineer_decor_controls_tool_reforged'
HARNESS = 'iedctr_barrel_v5_validation'
PREFIX = 'com.oblixorprime.engineersdecorreforged.'
CLASSES = [PREFIX+x for x in ('EngineersDecorReforged', 'client.BarrelReferenceScreen',
    'utility.BarrelReferenceGeometry', 'utility.BarrelReferenceState', 'menu.MachineMenu', 'utility.MachineLayout', 'utility.MachineBlockEntity',
    'client.FluidBarrelSpriteRenderer')]
TEXTURES = {
    'textures/gui/barrel_reference_v5/background.png': 'df89575afd98ccee396b38cee34f230ef03f94528d5a4ad5bc3425658ef54ca9',
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
            and runtime.get('production_selectors') == [] and 'opt_in_selector' not in runtime
            and type(runtime.get('harness_mod_path')) is str
            and Path(runtime['harness_mod_path']).resolve() != jar, 'Not an independent default V5 test mod')
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
        require(not any(n.startswith('com/oblixorprime/iedctrtest/') or 'NativeBarrelV5PackagedValidation' in n
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


def f32(value):
    return struct.unpack('!f', struct.pack('!f', value))[0]


def transform(width, height):
    available_width, available_height = width-12, height-12
    scale = f32(min(1, available_width/379, available_height/461))
    if scale*379 > available_width or scale*461 > available_height:
        bits = struct.unpack('!I', struct.pack('!f', scale))[0]
        scale = struct.unpack('!f', struct.pack('!I', bits-1))[0]
    return scale, f32((width-379*scale)/2), f32((height-461*scale)/2)


def screen_geometry(width, height):
    scale, ox, oy = transform(width, height)
    return scale, ox, oy, math.floor(ox), math.floor(oy), math.ceil(ox+379*scale)-math.floor(ox), math.ceil(oy+461*scale)-math.floor(oy)


def sample_geometry(width, height, index):
    scale, ox, oy = transform(width, height)
    top = (115,154,115)[index]
    x, y = math.ceil(ox+173*scale), math.ceil(oy+(top+2)*scale)
    return dict(x=x, y=y, width=math.floor(ox+208*scale)-x, height=math.floor(oy+191*scale)-y)


def slot_anchors():
    source = [(166,451,140,140),(838,451,140,140)]
    source += [(x,y,76,72) for y in (802,904,1006) for x in (132,232,332,432,532,632,732,832,932)]
    source += [(x,1148,76,76) for x in (148,244,340,436,532,628,724,820,916)]
    return [(math.floor((x+w/2)/3-8+.5),math.floor((y+h/2)/3-8+.5)) for x,y,w,h in source]


def number(obj, key, expected):
    value = obj.get(key)
    require(type(value) in (int,float) and math.isfinite(value) and abs(value-expected)<1e-4,
            'Wrong transformed coordinate '+key)
    return value


def validate_evidence(proof, nonce, source_sha, width, height, language, fluid):
    require(type(proof) is dict and proof.get('suite') == 'barrel-v5-packaged-native-v2'
            and re.fullmatch('[a-f0-9]{32}', nonce) and proof.get('nonce') == nonce
            and re.fullmatch('[a-f0-9]{40}', source_sha) and proof.get('executed_source_sha') == source_sha,
            'Wrong run/source identity')
    require(fluid in FLUID_IDS and width == 1280 and (language, height) in (('en_us', 720), ('fr_fr', 260)),
            'Unsupported Barrel profile')
    require(proof.get('selected_fluid') == fluid and proof.get('selected_language') == language
            and proof.get('runtime_mode') == 'v5-default'
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
    scale, ox, oy, left, top, image_width, image_height = screen_geometry(width, height)
    for index, row in enumerate(captures):
        require(type(row) is dict, 'Malformed capture')
        state = STATES[index]
        require(row.get('name') == 'barrel-'+state+'.png' and row.get('state_name') == state
                and row.get('machine') == 'barrel' and row.get('selected_fluid') == fluid
                and row.get('screen_class') == PREFIX+'client.BarrelReferenceScreen'
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
            gui_width=width, gui_height=height, gui_left=left, gui_top=top, image_width=image_width, image_height=image_height).items(): integer(g, key, value)
        for key, value in dict(view_scale=scale, view_origin_x=ox, view_origin_y=oy,
            tank_x=ox+170*scale, tank_y=oy+115*scale, tank_width=41*scale, tank_height=78*scale).items(): number(g,key,value)
        bounds = g.get('slot_hover_bounds')
        require(type(bounds) is list and len(bounds)==38 and g.get('hover_probe_method') == 'read-only screen hook; not physical edge gestures', 'Missing or overstated hover probes')
        for slot, (b, (x,y)) in enumerate(zip(bounds,slot_anchors())):
            require(type(b) is dict and b.get('center_hover') is True and b.get('four_outside_edges_rejected') is True, 'Hover bounds failed')
            for key,value in dict(menu_slot=slot,x=x,y=y).items(): integer(b,key,value)
        require(g.get('state_name') == state, 'Stale geometry state')
        for axis, value in (('x', math.floor(ox+(190 if index == 3 else 8)*scale+.5)), ('y', math.floor(oy+(150 if index == 3 else 8)*scale+.5))):
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
            for key, value in sample_geometry(width,height,index).items(): integer(sample, key, value)
            require(sample.get('fluid_id') == row['client']['fluid_id'], 'Wrong sample fluid identity')
            colors = integer(sample, 'distinct_rgba_colors')
            if index == 0:
                require(colors > 0 and 'still_texture' not in sample and 'tint_argb' not in sample, 'Invalid empty sample')
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
        x,y=slot_anchors()[2]
        integer(row, 'target_x', math.floor(ox+(x+8)*scale+.5))
        integer(row, 'target_y', math.floor(oy+(y+8)*scale+.5))
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
    folder = game/'barrel-v5-packaged-screenshots'
    require(not folder.is_symlink(), 'Unsafe capture directory')
    empty_pixels = None
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
            if index == 0:
                empty_pixels = pixels
            else:
                require(len(colors) > 1, 'Occupied native V5 fluid pixels are flat')
                require(any(pixels[4*(py*width+px):4*(py*width+px)+4] != empty_pixels[4*(py*width+px):4*(py*width+px)+4]
                            for py in range(y,y+h) for px in range(x,x+w)), 'Occupied sample unchanged from empty artwork')
            if index == 1:
                scale,ox,oy=transform(width,height)
                require(all(pixels[4*(py*width+px):4*(py*width+px)+4] == empty_pixels[4*(py*width+px):4*(py*width+px)+4]
                    for py in range(math.ceil(oy+117*scale),math.floor(oy+152*scale))
                    for px in range(math.ceil(ox+173*scale),math.floor(ox+208*scale))),
                    'Half-full tank upper region differs from captured empty artwork')
    require(f'IEDCTR_BARREL_V5_PACKAGED PASSED nonce={nonce} source={source_sha}' in logs, 'Missing current-run completion marker')
    return proof
