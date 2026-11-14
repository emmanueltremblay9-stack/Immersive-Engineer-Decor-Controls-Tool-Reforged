"""Fail-closed verifier for bounded native Milking manual traversal evidence."""
import hashlib
import re
import struct
from pathlib import Path
from zipfile import ZipFile
from client_mixin_smoke import SmokeFailure


def require(value, message):
    if not value:
        raise SmokeFailure(message)


def validate_proof(proof, nonce, source_sha, game, logs, code, expired, width, height, language, scale, report):
    require(code == 0 and not expired, 'Native client failed or timed out')
    require(proof.get('suite') == 'native-manual-pagination-v1' and proof.get('status') == 'PASS', 'Native manual did not pass')
    for key, expected in [('nonce', nonce), ('source_sha', source_sha), ('width', width), ('height', height), ('language', language), ('gui_scale', scale)]:
        require(proof.get(key) == expected, 'Wrong proof ' + key)
    require(re.fullmatch('[a-f0-9]{32}', nonce) and re.fullmatch('[a-f0-9]{40}', source_sha), 'Invalid identity')
    runtime = proof['runtime']
    require(runtime.get('jar_sha256') == report['jar_sha256'], 'Wrong runtime JAR hash')
    require(runtime.get('fixture_free_jar') is True and runtime.get('separate_harness_module') is True, 'Harness isolation missing')
    jar = Path(report['jar_path'])
    require(hashlib.sha256(jar.read_bytes()).hexdigest() == report['jar_sha256'], 'JAR changed')
    with ZipFile(jar) as archive:
        require(not any(n.startswith('com/oblixorprime/iedctrtest/') for n in archive.namelist()), 'Harness leaked')
        fit_class='com.oblixorprime.engineersdecorreforged.client.ManualViewportFit'
        fit_origins=[r for r in runtime.get('class_origins', []) if r.get('class') == fit_class]
        require(len(fit_origins)==1 and fit_origins[0].get('sha256') == hashlib.sha256(archive.read(fit_class.replace('.','/')+'.class')).hexdigest(), 'Viewport implementation provenance missing')
        resources = runtime['active_resources']
        expected_resources = {f'manual/{language}/{name}.txt' for name in ('small_milking_machine', 'workshop_machines')} | {f'manual/{name}.json' for name in ('small_milking_machine', 'workshop_machines')}
        require(len(resources) == 4 and {r['path'] for r in resources} == expected_resources, 'Exact four manual resources required')
        for resource in resources:
            name = 'assets/immersive_engineer_decor_controls_tool_reforged/' + resource['path']
            require(resource['sha256'] == hashlib.sha256(archive.read(name)).hexdigest(), 'Manual resource changed')
    groups = proof['groups']
    transitions = proof.get('transitions', [])
    require(len(transitions) == 2, 'Entry scope transitions missing')
    for row in transitions:
        require(row.get('entry', '').startswith('immersive_engineer_decor_controls_tool_reforged:'), 'Unowned entry qualified')
        require(row.get('other_entry', '').startswith('immersiveengineering:'), 'IE control missing')
        for key in ['roundtrip', 'reopen', 'resize_reinit', 'config_unchanged']:
            require(row.get(key) is True, 'Transition failed: '+key)
    require(transitions[0].get('physical_resize_restored') is True
            and transitions[0].get('resized_width') == 960 and transitions[0].get('resized_height') == 540, 'Physical resize proof missing')
    require(len(groups) == 2, 'Individual and category groups required')
    require({g['entry'].split(':')[-1] for g in groups} == {'small_milking_machine', 'workshop_machines'}, 'Wrong entries')
    pages = proof['pages']
    require(len(pages) >= 4, 'Long manual content not traversed')
    captures = proof['captures']
    require(len(captures) == len(pages), 'Every page needs a capture')
    for group in groups:
        subset = [p for p in pages if p['entry'] == group['entry']]
        sequence = list(range(group['start_page'], group['end_page_exclusive']))
        require([p['page'] for p in subset] == sequence and len(subset) == group['captured_pages'], 'Incomplete page sequence')
        for key in ['body_complete', 'recipe_present']:
            require(group.get(key) is True, 'Missing group evidence: ' + key)
        for key in ['navigation_forward', 'navigation_backward']:
            require(type(group.get(key)) is int and group[key] == len(subset)-1, 'Missing native navigation: ' + key)
    for page in pages:
        require(page.get('bounds_ok') is True, 'Page bounds fail')
        require(len(page['lines']) == len(page['line_widths']), 'Line evidence mismatch')
        require(all(0 <= w <= page['page_width'] for w in page['line_widths']), 'Line overflow')
        require(0 <= page['text_height'] + page['special_height'] <= page['page_height']+1, 'Vertical overflow')
    paths = set()
    for page, capture in zip(pages, captures):
        require(capture.get('entry') == page['entry'] and capture.get('page') == page['page'] and page.get('capture') == capture, 'Capture-page identity mismatch')
        path = game / capture['path']
        require(path.resolve().is_relative_to(game.resolve()), 'Unsafe capture path')
        require(capture['path'] not in paths, 'Duplicate capture')
        paths.add(capture['path'])
        raw = path.read_bytes()
        require(raw[:8] == b'\x89PNG\r\n\x1a\n' and len(raw) > 1000, 'Bad capture')
        require(raw[12:16] == b'IHDR' and struct.unpack('>II', raw[16:24]) == (width, height), 'Capture pixel dimensions differ')
        require(capture.get('width') == width and capture.get('height') == height, 'Capture dimension metadata differs')
        require(hashlib.sha256(raw).hexdigest() == capture['sha256'], 'Capture digest differs')
    require('MANUAL_PAGINATION_PASS:' + nonce in logs, 'Pass marker absent')
