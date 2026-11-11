"""Exact PR173 integration exceptions, not broad exclusions from preservation checks."""
import hashlib
import json


MENU_TAIL_SHA256 = 'f471ad91c8d8acfabc55d73a19a21807cb7cf1e41052a369bb91a50708772633'
TREE_ACCESSOR = ('   public boolean treeCutterV3ReferencePresentation() '
                 '{ return this.treeCutterV3ReferencePresentation; }\n\n')
TREE_COMMIT = '96ac07aac200e10e9936b0ae4ae99f9053fae11c'
MAIN_COMMIT = '890958ff300e9a88dde2a77fc6c145d19c0f98f9'
UTILITY = 'src/main/java/com/oblixorprime/engineersdecorreforged/utility/'
TREE_FILES = {
    UTILITY + 'MachineBlockEntity.java': '34e971011cfeacd68695fe756fd6c4f0dd67b1a65d82d5af6ef5adb1e45fafa7',
    UTILITY + 'MachineLayout.java': '6e56fb8b448c1b19c65cdb3663aeb34ccbdc7ba88f778dc2e5ee79ba8577642b',
}
BARREL_GUARD = 'tools/validation/check_barrel_reference_wiring.py'
INTEGRATION_FILES = {
    **TREE_FILES,
    BARREL_GUARD: '89128c598d2e698f4f670b0551b8f918661fca3b836100890873853ec76bc7c3',
}
MILKING_FILES = {
    'src/main/java/com/oblixorprime/engineersdecorreforged/client/MachineScreen.java',
    UTILITY + 'MachineBlockEntity.java',
}
MILKING_UPSTREAM_INTEGRATED_FILES = {
    'src/main/java/com/oblixorprime/engineersdecorreforged/client/ClientModEvents.java': '3f32ffeccd725fe8d54b95f5a54ae11acb8a31bfb124172bee60bbf420977723',
    UTILITY + 'MachineLayout.java': TREE_FILES[UTILITY + 'MachineLayout.java'],
}


def assert_menu_tail_preserved(source):
    """Remove one exact new accessor; every other downstream byte remains pinned."""
    marker = '   private static MenuType<?> resolveMenuType'
    assert source.count(marker) == 1, 'Ambiguous menu preservation boundary'
    tail = source.split(marker, 1)[1]
    assert tail.count(TREE_ACCESSOR) == 1, 'Missing or changed Tree presentation accessor'
    normalized = tail.replace(TREE_ACCESSOR, '', 1)
    assert hashlib.sha256(normalized.encode()).hexdigest() == MENU_TAIL_SHA256, 'Menu transfer/data/validation changed'


def assert_solar_baseline_preserved(root, preserved, milking_current_hashes=None):
    """Retain historical pins and admit only exact documented integration identities."""
    milking_current_hashes = milking_current_hashes or {}
    assert not milking_current_hashes or set(milking_current_hashes) == MILKING_FILES, 'Unexpected Milking current-hash allowlist'
    assert preserved['remote_baseline_commit'] == '15593b28a6853ba36775ebb419b8465a5e2f3240'
    assert preserved['baseline_tree'] == '6dc6f3185624808ce7127331bb2f62c26f102d34'
    files = preserved['files']
    historical = json.dumps(files, sort_keys=True, separators=(',', ':')).encode()
    assert hashlib.sha256(historical).hexdigest() == '8dfad5336a1c39a48481ecfeeb691390e9ea9e3cd80bbfd5811435329c5d3b52', 'Historical preservation pins changed'
    integration = preserved['tree_cutter_integration']
    assert integration['main_commit'] == MAIN_COMMIT
    assert integration['tree_commit'] == TREE_COMMIT
    overrides = integration['files']
    assert set(overrides) == set(INTEGRATION_FILES), 'Unexpected integration exception'
    for path, record in overrides.items():
        assert record['baseline_sha256'] == files[path], path
        assert isinstance(record['reason'], str) and record['reason'].strip(), path
        assert record['integrated_sha256'] == INTEGRATION_FILES[path], path
    for path, expected in files.items():
        if path in overrides:
            expected = overrides[path]['integrated_sha256']
        if path in milking_current_hashes:
            expected = milking_current_hashes[path]
        assert hashlib.sha256((root / path).read_bytes()).hexdigest() == expected, path
