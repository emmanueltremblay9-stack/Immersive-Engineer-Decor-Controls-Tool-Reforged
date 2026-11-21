"""Exact reviewed GUI delta, not a transfer of historical native qualification.

Projection permits historical preservation checks to examine the unchanged base.
Separate current-commit native qualification remains mandatory.
"""
import hashlib
import json
from pathlib import Path
from gui_exterior_alpha import current_hash as alpha_hash, project_history as alpha_history

ROOT = Path(__file__).resolve().parents[2]
RECORD = ROOT / 'docs/qualification/missing-gui-source-delta.json'
RECORD_SHA256 = '6457ee9bb36b645973fa027f897fb436a44ed00d8692e86a5829b93a02d58502'
RECORD_BYTES = RECORD.read_bytes()


def records():
    data = RECORD_BYTES
    assert hashlib.sha256(data).hexdigest() == RECORD_SHA256, 'GUI integration record changed'
    return json.loads(data)['files']


def project_history(path, data):
    data = alpha_history(path, data)
    record = records().get(path)
    if record is None:
        return data
    actual = hashlib.sha256(data).hexdigest()
    if actual == record['base_sha256']:
        return data
    assert actual == record['current_sha256'], 'Unreviewed GUI candidate change: ' + path
    lines = data.decode().splitlines(keepends=True)
    for hunk in reversed(record['hunks']):
        assert ''.join(lines[hunk['start']:hunk['end']]) == hunk['current'], 'GUI hunk changed'
        lines[hunk['start']:hunk['end']] = hunk['previous'].splitlines(keepends=True)
    result = ''.join(lines).encode()
    assert hashlib.sha256(result).hexdigest() == record['base_sha256'], 'GUI base reconstruction changed'
    return result


def current_hash(path, previous):
    record = records().get(path)
    if record is None:
        return alpha_hash(path, previous)
    assert previous == record['base_sha256'], 'Unexpected GUI integration baseline: ' + path
    return alpha_hash(path, record['current_sha256'])
