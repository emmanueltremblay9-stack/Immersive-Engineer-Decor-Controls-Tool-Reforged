"""Exact reviewed GUI delta, not a transfer of historical native qualification.

Projection permits historical preservation checks to examine the unchanged base.
Separate current-commit native qualification remains mandatory.
"""
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RECORD = ROOT / 'docs/qualification/missing-gui-source-delta.json'
RECORD_SHA256 = '15aa54afc778afc2774330f0a44ec433274ef056825b167d84fd75014ee3f19c'
RECORD_BYTES = RECORD.read_bytes()


def records():
    data = RECORD_BYTES
    assert hashlib.sha256(data).hexdigest() == RECORD_SHA256, 'GUI integration record changed'
    return json.loads(data)['files']


def project_history(path, data):
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
        return previous
    assert previous == record['base_sha256'], 'Unexpected GUI integration baseline: ' + path
    return record['current_sha256']
