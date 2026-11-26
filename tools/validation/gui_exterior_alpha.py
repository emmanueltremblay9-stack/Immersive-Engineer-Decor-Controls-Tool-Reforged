"""Exact exterior-alpha identities; preserve all historical source pins.

Only the fourteen named PNGs and recorded hash/verification text deltas are
admitted. This does not accept arbitrary alpha edits or transfer native proof.
"""
import hashlib
import json
from pathlib import Path
from prism_210_history_projection import project_current_candidate

ROOT = Path(__file__).resolve().parents[2]
RECORD = ROOT / 'docs/qualification/gui-exterior-alpha/manifest.json'
RECORD_SHA256 = 'ab61a30812d5a775dd423d2f8176e910555f624ec3193236bac23d534b98f33f'
RECORD_BYTES = RECORD.read_bytes()


def records():
    assert hashlib.sha256(RECORD_BYTES).hexdigest() == RECORD_SHA256, 'Exterior alpha record changed'
    return json.loads(RECORD_BYTES)


def current_hash(path, previous):
    manifest = records()
    record = manifest['textures'].get(path) or manifest['text_files'].get(path)
    if record is None:
        return previous
    assert previous == record['source_sha256'], 'Unexpected exterior alpha baseline: ' + path
    return record['output_sha256']


def project_history(path, data):
    data = project_current_candidate(path, data)
    record = records()['text_files'].get(path)
    if record is None:
        return data
    actual = hashlib.sha256(data).hexdigest()
    if actual == record['source_sha256']:
        return data
    assert actual == record['output_sha256'], 'Unreviewed exterior alpha text change: ' + path
    lines = data.decode().splitlines(keepends=True)
    for hunk in reversed(record['hunks']):
        assert ''.join(lines[hunk['start']:hunk['end']]) == hunk['current'], 'Exterior alpha hunk changed'
        lines[hunk['start']:hunk['end']] = hunk['previous'].splitlines(keepends=True)
    result = ''.join(lines).encode()
    assert hashlib.sha256(result).hexdigest() == record['source_sha256'], 'Exterior alpha base reconstruction changed'
    return result
