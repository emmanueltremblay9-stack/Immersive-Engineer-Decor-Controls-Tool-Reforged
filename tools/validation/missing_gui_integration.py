"""Exact reviewed GUI delta, not a transfer of historical native qualification.

Projection permits historical preservation checks to examine the unchanged base.
Separate current-commit native qualification remains mandatory.
"""
import hashlib
import json
from pathlib import Path
from gui_exterior_alpha import current_hash as alpha_hash, project_history as alpha_history
from prism_210_history_projection import project_current_candidate

ROOT = Path(__file__).resolve().parents[2]
RECORD = ROOT / 'docs/qualification/missing-gui-source-delta.json'
RECORD_SHA256 = '6457ee9bb36b645973fa027f897fb436a44ed00d8692e86a5829b93a02d58502'
RECORD_BYTES = RECORD.read_bytes()
MACHINE_GUI_TESTS = 'src/main/java/com/oblixorprime/engineersdecorreforged/gametest/MachineGuiGameTests.java'
MACHINE_LAYOUT = 'src/main/java/com/oblixorprime/engineersdecorreforged/utility/MachineLayout.java'
# These paths were changed by the separately branched #210 baseline after the
# older retained-GUI source record was captured. Keep that baseline exact and
# don't project a different branch's hunk set over it.
SUPERSEDED_BASE_SHA256 = {
    MACHINE_GUI_TESTS: 'c1ad4c2369a17028721c7a223848d54b7c82fbfe2f6158294fb2213250265c77',
    MACHINE_LAYOUT: '25cc7fc7c0e5abb423e959b2ed4eacabb8d110bda56352b3e7025da30b15f55c',
}
SUPERSEDED_CURRENT_SHA256 = {
    MACHINE_GUI_TESTS: '2057fc0dbb8cf4748fad02e1637234cf2a0416af2d535221e7b65fea52d66aa8',
    MACHINE_LAYOUT: '25cc7fc7c0e5abb423e959b2ed4eacabb8d110bda56352b3e7025da30b15f55c',
}


def records():
    data = RECORD_BYTES
    assert hashlib.sha256(data).hexdigest() == RECORD_SHA256, 'GUI integration record changed'
    return json.loads(data)['files']


def project_history(path, data):
    # First undo only the exact later #210 composition layers, then let each
    # older thermal-composition and GUI provenance guards project their own
    # unchanged records as before.
    data = project_current_candidate(path, data)
    if path in SUPERSEDED_BASE_SHA256 and hashlib.sha256(data).hexdigest() == SUPERSEDED_BASE_SHA256[path]:
        return data
    from thermal_single_stage_projection import project_historical_if_candidate
    data = project_historical_if_candidate(path, data)
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
    if path in SUPERSEDED_CURRENT_SHA256:
        return SUPERSEDED_CURRENT_SHA256[path]
    return alpha_hash(path, record['current_sha256'])
