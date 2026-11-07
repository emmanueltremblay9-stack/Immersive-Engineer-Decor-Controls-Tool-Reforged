"""Fail-closed packaged Factory Placer evidence checks; synthetic unit tests are not gameplay proof."""
from __future__ import annotations

import hashlib
import json
import math
from pathlib import Path
import re
import zipfile
from urllib.parse import urlsplit

from factory_placer_v2_reference_native import (
    ART_SHA, ROW_ACTIVE, SLOT_ORIGINS, decode_png, expected_geometry,
    png, require, strict_equal, transform, validate_proof, validate_viewport,
)
from client_mixin_smoke import FATAL_PATTERN

QUALIFICATION_SUITE = 'native-factory-placer-delivery-v1'
CONTROLS = ('ROW_TOP', 'ROW_MIDDLE', 'ROW_BOTTOM', 'MANUAL_TRIGGER', 'INVERSION', 'TRIGGER_MODE')
TARGETS = ('HEADER', 'SLOT_0', *CONTROLS)
GESTURES = ('drag_left_split', 'drag_right_split', 'shift_click', 'double_click_collect',
            'right_control_noop', 'middle_control_noop', 'held_control_noop', 'outside_carried_drop')
MOD_ID = 'immersive_engineer_decor_controls_tool_reforged'
HARNESS_ID = 'iedctr_factory_placer_validation'
ART_ENTRY = f'assets/{MOD_ID}/textures/gui/factory_placer_v2_reference/background.png'


def integer(value, minimum=0):
    return type(value) is int and value >= minimum


def slots(nonempty):
    return {str(i): nonempty.get(i, 0) for i in range(54)}


INITIAL_SLOTS = slots({0: 12, 1: 4, 18: 8})
LEFT_SLOTS = slots({1: 4, 3: 4, 4: 4, 5: 4, 18: 8})
RIGHT_SLOTS = slots({1: 4, 3: 4, 4: 4, 5: 4, 6: 1, 7: 1, 8: 1, 18: 5})
SHIFT_SLOTS = slots({0: 5, 1: 4, 3: 4, 4: 4, 5: 4, 6: 1, 7: 1, 8: 1})
COLLECTED_SLOTS = slots({18: 24})
EMPTY_SLOTS = slots({})
AFTER_SLOTS = (LEFT_SLOTS, RIGHT_SLOTS, SHIFT_SLOTS, COLLECTED_SLOTS,
               COLLECTED_SLOTS, COLLECTED_SLOTS, COLLECTED_SLOTS, EMPTY_SLOTS)


def validate_snapshot(snapshot, expected_slots, carried, drops, menu_id):
    require(type(snapshot) is dict, 'Missing authoritative gesture snapshot')
    expected = dict(menu_id=menu_id, logic=0, selected_slot=0, signal=0,
                    carried_count=carried, world_drop_items=drops, item_total=69,
                    hidden_sentinels_intact=True, slot_counts=expected_slots)
    for key, value in expected.items():
        require(strict_equal(snapshot.get(key), value), f'Gesture snapshot differs: {key}')
    require(sum(expected_slots.values()) + carried + drops + 45 == 69,
            'Gesture expectation does not conserve the nine hidden sentinels')


def expected_steps(index):
    if index == 0:
        return [(slots({1: 4, 18: 8}), 12, 0), (LEFT_SLOTS, 0, 0)]
    if index == 1:
        return [(slots({1: 4, 3: 4, 4: 4, 5: 4}), 8, 0),
                (slots({1: 4, 3: 4, 4: 4, 5: 4, 6: 1, 7: 1, 8: 1}), 5, 0),
                (RIGHT_SLOTS, 0, 0)]
    if index == 2:
        return [(SHIFT_SLOTS, 0, 0)]
    if index == 3:
        return [(EMPTY_SLOTS, 24, 0), (COLLECTED_SLOTS, 0, 0)]
    if index in (4, 5):
        return [(COLLECTED_SLOTS, 0, 0)] * 6
    if index == 6:
        return [(EMPTY_SLOTS, 24, 0)] * 7 + [(COLLECTED_SLOTS, 0, 0)]
    return [(EMPTY_SLOTS, 24, 0), (EMPTY_SLOTS, 0, 24)]


def validate_gestures(proof):
    records = proof.get('advanced_gestures')
    require(type(records) is list and len(records) == len(GESTURES), 'Incomplete advanced gesture suite')
    menu_id = records[0].get('before', {}).get('menu_id') if type(records[0]) is dict else None
    require(integer(menu_id, 1), 'Missing authoritative gesture menu identity')
    before_slots = INITIAL_SLOTS
    for index, (record, name) in enumerate(zip(records, GESTURES)):
        require(type(record) is dict and record.get('case') == name
                and record.get('server_confirmed') is True and record.get('server_ack') is True,
                'Missing, duplicate or unordered gesture case')
        method = record.get('input_method')
        require(type(method) is str and 'Screen.mouseClicked' in method,
                'Gesture input provenance missing')
        validate_snapshot(record.get('before'), before_slots, 0, 0, menu_id)
        validate_snapshot(record.get('after'), AFTER_SLOTS[index], 0, 24 if index == 7 else 0, menu_id)
        for key, value in dict(item_total_before=69, item_total_after=69, carried_count=0,
                               world_drop_items=24 if index == 7 else 0, slot_counts=AFTER_SLOTS[index]).items():
            require(strict_equal(record.get(key), value), 'Gesture summary contradicts authoritative snapshots')
        expected = expected_steps(index)
        steps = record.get('steps')
        require(type(steps) is list and len(steps) == len(expected), 'Gesture intermediate acknowledgments missing')
        for step_index, (step, (counts, carried, dropped)) in enumerate(zip(steps, expected)):
            validate_snapshot(step, counts, carried, dropped, menu_id)
            require(type(step.get('step')) is int and step['step'] == step_index
                    and step.get('server_ack') is True, 'Gesture step observation differs')
            control_index = step_index if index in (4, 5) else step_index - 1 if index == 6 and 1 <= step_index <= 6 else None
            if control_index is not None:
                require(step.get('control') == CONTROLS[control_index], 'Invalid-control coverage differs')
        if index == 2:
            require(record.get('native_shift_observed') is True and 'XTest Shift' in method,
                    'Shift-click lacks observed native modifier')
        if index in (4, 5, 6):
            require('does not prove receipt' in record.get('ack_scope', ''),
                    'Rejected input must not claim causal packet receipt')
        observations = record.get('control_observations')
        expected_observations = []
        if index in (4, 5, 6):
            for step in steps:
                if 'control' in step:
                    expected_observations.append(dict(step, server_observed_unchanged=True,
                                                      causal_packet_receipt_proven=False))
        require(strict_equal(observations, expected_observations), 'Invalid-control authoritative observations differ')
        before_slots = AFTER_SLOTS[index]


def expected_tooltip_texts(project, language):
    root = Path(project) / f'src/main/resources/assets/{MOD_ID}/lang'
    require(language in ('en_us', 'fr_fr', 'fr_ca'), 'Unsupported tooltip language')
    texts = json.loads((root / f'{language}.json').read_text(encoding='utf-8'))
    prefix = f'gui.{MOD_ID}.factory_placer.v2.'
    return dict(HEADER=texts[f'block.{MOD_ID}.factory_placer'],
                SLOT_0=texts[prefix + 'slot_tooltip'] % (1, 1),
                ROW_TOP=texts[prefix + 'row_tooltip'] % (1, 1, 1),
                ROW_MIDDLE=texts[prefix + 'row_tooltip'] % (2, 1, 1),
                ROW_BOTTOM=texts[prefix + 'row_tooltip'] % (3, 1, 1),
                MANUAL_TRIGGER=texts[prefix + 'manual_tooltip'] % texts[prefix + 'signal_off'],
                INVERSION=texts[prefix + 'redstone_tooltip'] % texts[prefix + 'redstone_0'],
                TRIGGER_MODE=texts[prefix + 'trigger_tooltip'] % texts[prefix + 'edge'])


def normalized(text):
    return ' '.join(text.split())


def uri_identity(uri):
    require(type(uri) is str and uri, 'Missing runtime origin URI')
    parsed = urlsplit(uri)
    require(parsed.scheme in ('file', 'union', 'jar') and not parsed.query and not parsed.fragment,
            'Invalid runtime origin URI')
    # file:/x and file:///x are equal absolute URIs. Do not resolve dot segments,
    # unquote separators or compare filename substrings across mod boundaries.
    path = re.sub(r'%[0-9a-fA-F]{2}', lambda match: match.group().upper(), parsed.path)
    return parsed.scheme.lower(), parsed.netloc.lower(), path


def validate_tooltips(proof, game, project, width, height, language, gui_scale):
    records = proof.get('tooltip_captures')
    require(type(records) is list and len(records) == len(TARGETS), 'Incomplete native tooltip matrix')
    viewport, _, _ = expected_geometry(width, height, gui_scale)
    expected = expected_tooltip_texts(project, language)
    for index, (record, target) in enumerate(zip(records, TARGETS)):
        require(type(record) is dict and record.get('target') == target
                and record.get('selected_language') == language, 'Tooltip target/locale differs')
        validate_viewport(record, viewport)
        lines, widths = record.get('lines'), record.get('line_widths')
        require(type(lines) is list and lines and all(type(line) is str and line for line in lines)
                and type(widths) is list and len(widths) == len(lines)
                and all(integer(value, 1) for value in widths), 'Native tooltip lines or widths missing')
        require(record.get('text') == '\n'.join(lines)
                and normalized(record['text']) == normalized(expected[target]), 'Native tooltip text truncated or untranslated')
        x, y, w, h = [record.get('tooltip_' + field) for field in ('x', 'y', 'width', 'height')]
        require(all(integer(value) for value in (x, y, w, h)) and w == max(widths) and h > 0,
                'Native tooltip geometry invalid')
        require(record.get('in_bounds') is True and x - 4 >= 0 and y - 4 >= 0
                and x + w + 4 <= viewport['gui_width'] and y + h + 4 <= viewport['gui_height'],
                'Native tooltip extends outside viewport')
        require(w <= max(1, min(240, viewport['gui_width'] - 16)), 'Native font wrapping exceeded bound')
        require(integer(record.get('tooltip_render_events'), 3)
                and record.get('tooltip_event_class') == 'RenderTooltipEvent.Color+ScreenEvent.Render.Post'
                and 'actual ClientTextTooltip' in record.get('text_source', '')
                and 'physical XTest pointer' in record.get('input_method', ''), 'Tooltip render/input provenance missing')
        require(integer(record.get('pointer_x')) and integer(record.get('pointer_y'))
                and record['pointer_x'] < viewport['gui_width'] and record['pointer_y'] < viewport['gui_height'],
                'Tooltip pointer outside viewport')
        name = f'tooltip-{index}.png'
        require(record.get('name') == name, 'Unsafe tooltip capture name')
        png(Path(game) / 'factory-placer-v2-reference-screenshots' / name, width, height, record.get('sha256'))


def selection_edges(width, height, scale, selected):
    sx, sy = transform(width, height, scale)
    x, y = SLOT_ORIGINS[selected]
    strips = ((x - 1, y - 1, x + 17, y), (x + 16, y, x + 17, y + 16),
              (x - 1, y + 16, x + 17, y + 17), (x - 1, y, x, y + 16))
    return {name: [(px, py) for py in range(math.ceil(sy(top) - .5), math.ceil(sy(bottom) - .5))
                   for px in range(math.ceil(sx(left) - .5), math.ceil(sx(right) - .5))]
            for name, (left, top, right, bottom) in zip(('top', 'right', 'bottom', 'left'), strips)}


def validate_selections(proof, game, width, height, gui_scale):
    records = proof.get('selection_captures')
    require(type(records) is list and len(records) == 18, 'Incomplete selection visibility matrix')
    viewport, _, _ = expected_geometry(width, height, gui_scale)
    failures = []
    for selected, record in enumerate(records):
        require(type(record) is dict and type(record.get('selected_slot')) is int
                and record['selected_slot'] == selected and record.get('server_confirmed') is True
                and record.get('state_source') == 'server-seeded selection render case; not a user selection gesture',
                'Selection state provenance differs')
        validate_viewport(record, viewport)
        name = f'selection-{selected}.png'
        require(record.get('name') == name, 'Unsafe selection capture name')
        path = Path(game) / 'factory-placer-v2-reference-screenshots' / name
        png(path, width, height, record.get('sha256'))
        _, _, pixels = decode_png(path)
        expected = selection_edges(width, height, gui_scale, selected)
        require(type(record.get('edges')) is dict and set(record['edges']) == set(expected), 'Selection edge reports missing')
        missing = []
        for name, coordinates in expected.items():
            unmatched = [[x, y] for x, y in coordinates if pixels[(y * width + x) * 4:(y * width + x + 1) * 4] != ROW_ACTIVE]
            edge = record['edges'][name]
            require(type(edge) is dict and strict_equal(edge.get('expected_pixels'), len(coordinates))
                    and strict_equal(edge.get('matched_pixels'), len(coordinates) - len(unmatched))
                    and strict_equal(edge.get('missing_pixels'), unmatched), 'Selection pixel report differs from native PNG')
            if not coordinates or unmatched:
                missing.append(name)
        require(strict_equal(record.get('missing_edges'), missing)
                and record.get('full_outline_visible') is (not missing), 'Selection visibility report contradicts pixels')
        if missing:
            failures.append(f'{selected}: {",".join(missing)}')
    require(not failures, 'Native selection outline incomplete: ' + '; '.join(failures))


def validate_delivery_proof(proof, nonce, source_sha, game, logs, exit_code, timed_out,
                            width, height, language, gui_scale, project, jar, classpath_report):
    validate_proof(proof, nonce, source_sha, game, logs, exit_code, timed_out,
                   width, height, language, gui_scale,
                   source_path=Path(project) / ('src/main/resources/' + ART_ENTRY))
    require(proof.get('qualification_suite') == QUALIFICATION_SUITE, 'Packaged delivery suite identity missing')
    validate_packaged_runtime(proof.get('packaged_runtime'), jar, classpath_report)
    validate_gestures(proof)
    validate_tooltips(proof, game, project, width, height, language, gui_scale)
    validate_selections(proof, game, width, height, gui_scale)
    return proof


def validate_packaged_runtime(identity, jar, classpath_report):
    jar = Path(jar)
    require(jar.is_file() and not jar.is_symlink() and jar.suffix == '.jar', 'Missing normal runtime JAR')
    jar = jar.resolve()
    digest = hashlib.sha256(jar.read_bytes()).hexdigest()
    require(type(identity) is dict and type(classpath_report) is dict, 'Missing packaged runtime identities')
    for record in (identity, classpath_report):
        require(record.get('jar_path') == str(jar) and record.get('jar_sha256') == digest
                and record.get('fixture_free_jar') is True, 'Packaged runtime artifact identity differs')
    require(classpath_report.get('split_packages') is False
            and classpath_report.get('loaded_development_mods') == [HARNESS_ID], 'Harness discovery differs')
    classpath = classpath_report.get('runtime_classpath')
    forbidden = classpath_report.get('forbidden_main_outputs')
    harness = classpath_report.get('harness_outputs')
    require(all(type(paths) is list and paths and all(type(path) is str and path for path in paths)
                for paths in (classpath, forbidden, harness)), 'Runtime classpath evidence missing')
    require(str(jar) in classpath and not set(classpath).intersection(forbidden)
            and not set(harness).intersection(forbidden), 'Main source output present in packaged runtime')
    require(identity.get('separate_harness_module') is True and identity.get('harness_mod_id') == HARNESS_ID
            and type(identity.get('harness_mod_path')) is str and identity['harness_mod_path'] != str(jar),
            'Harness module is not separate')
    require(identity.get('active_texture_sha256') == ART_SHA
            and identity.get('active_texture_pack') == 'mod/' + MOD_ID, 'Active artwork pack differs')
    root = identity.get('mod_root_uri')
    require(type(root) is str and root and root.startswith(('union:', 'jar:', 'file:')),
            'Missing authoritative runtime mod root')
    classes = ('EngineersDecorReforged', 'client.FactoryPlacerV2ReferenceScreen',
               'client.MachineScreen$FactoryPlacerScreen', 'menu.MachineMenu',
               'utility.FactoryPlacerV2ReferenceGeometry', 'utility.MachineBlockEntity')
    classes = ['com.oblixorprime.engineersdecorreforged.' + name for name in classes]
    origins = identity.get('class_origins')
    require(type(origins) is list and len(origins) == len(classes), 'Runtime class origin matrix incomplete')
    with zipfile.ZipFile(jar) as archive:
        names = archive.namelist()
        require(len(names) == len(set(names)) and not any(name.startswith('com/oblixorprime/iedctrtest/')
                    or 'NativeFactoryPlacerV2ReferenceValidation' in name for name in names),
                'Normal JAR contains duplicate entries or native fixture')
        require(hashlib.sha256(archive.read(ART_ENTRY)).hexdigest() == ART_SHA, 'Normal JAR artwork differs')
        for name, origin in zip(classes, origins):
            entry = name.replace('.', '/') + '.class'
            require(type(origin) is dict and origin.get('class') == name
                    and origin.get('sha256') == hashlib.sha256(archive.read(entry)).hexdigest(),
                    'Loaded class resource bytes differ from normal JAR')
            require(uri_identity(origin.get('code_source_uri')) in (uri_identity(root), uri_identity(jar.as_uri()))
                    and uri_identity(origin.get('resource_uri')) == uri_identity(root.rstrip('/') + '/' + entry),
                    'Production class origin is outside the packaged mod root')
    return digest


def validate_default_proof(proof, nonce, source_sha, game, logs, exit_code, timed_out,
                           width, height, language, jar, classpath_report):
    require(not timed_out and type(exit_code) is int and exit_code == 0
            and not FATAL_PATTERN.search(logs), 'Packaged default runtime did not exit cleanly')
    require(type(proof) is dict and proof.get('suite') == 'native-factory-placer-packaged-default-v1'
            and proof.get('nonce') == nonce and proof.get('executed_source_sha') == source_sha
            and proof.get('status') == 'passed_bounded_smoke' and 'error' not in proof,
            'Packaged default proof identity differs')
    expected = dict(v2_property_absent=True, actual_integrated_world=True, native_visual_acceptance=False,
                    screen_class='com.oblixorprime.engineersdecorreforged.client.MachineScreen$FactoryPlacerScreen',
                    menu_slot_count=54, machine_saved_slot_count=27, machine_visible_slot_count=18,
                    default_logic=6, default_selected_slot=0, pickup_server_acks=1, return_server_acks=1,
                    initial_item_total=1, final_item_total=1, world_drop_count=0,
                    width=width, height=height, selected_language=language)
    for key, value in expected.items():
        require(strict_equal(proof.get(key), value), f'Packaged default behavior differs: {key}')
    require(not list(Path(game).glob('crash-reports/*')) and not list(Path(game).parent.rglob('hs_err_pid*.log')),
            'Packaged default runtime wrote a crash report')
    validate_packaged_runtime(proof.get('packaged_runtime'), jar, classpath_report)
    require(proof.get('capture') == 'factory-placer-packaged-default.png', 'Default capture name differs')
    png(Path(game) / proof['capture'], width, height, proof.get('capture_sha256'))
    require(f'IEDCTR_FACTORY_PLACER_PACKAGED_DEFAULT PASSED nonce={nonce}' in logs,
            'Packaged default completion marker missing')
    return proof
