"""Owner v6 Dropper exterior-alpha gate and exact historical views. No native claim.

This is a closed, versioned transition, not a general allowlist. The old guards
and their inputs remain archived byte-for-byte. Only the named candidate bytes
can be projected to them; no fallback to accepting an old candidate is allowed.
"""
import hashlib
import json
import atexit
import io
import os
import subprocess
import tarfile
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PACKAGE = ROOT / 'tools/approved_gui_20261119'
BASELINE = None
_BASELINE_TEMP = None
RECIPE = PACKAGE / 'qualification-v6.json'
V5_RECIPE = PACKAGE / 'qualification-v5.json'
V5_RECIPE_SHA256 = '100c3b42e73965d59a7c956c79ec77791e7459b8763f7b8611e9ef9ccc560a38'
V5_COMMIT = 'cec601e71369e87fa39b1ed7498ad628480a0a80'
V4_RECIPE = PACKAGE / 'qualification-v4.json'
V4_RECIPE_SHA256 = 'b59832808af932dcd366dfbe0ca03b246bd1f1c44b4384780cb1c59a8b4e3955'
V4_COMMIT = '8f909beebf96b0197830813aaa415a0e72d44881'
PREVIOUS_RECIPE = PACKAGE / 'qualification-v3.json'
PREVIOUS_RECIPE_SHA256 = 'cefa38e5b9121bbe9524272b729b5387612c1c300390fa3742c0e164c14caa6c'
PREVIOUS_COMMIT = 'ef40b46776148026e5df5a815483c2f69fb982b4'
RECIPE_SHA256 = '096e5f36db9ac11c48b8620c7dda56739457436c5fee95faa22f79f6ee3c0082'
BASE = '87f8b8e03cac46a6154b54522d1497baa52c67b2'
SCREEN = 'src/main/java/com/oblixorprime/engineersdecorreforged/client/MachineScreen.java'


def read_exact(path):
    # Preserve filesystem evidence independently of historical test doubles.
    with path.open("rb") as stream:
        return stream.read()


def digest(data):
    return hashlib.sha256(data).hexdigest()


def historical_v4_recipe():
    v3 = read_exact(PREVIOUS_RECIPE)
    assert digest(v3) == PREVIOUS_RECIPE_SHA256, 'Historical v3 recipe changed'
    previous = json.loads(v3)
    raw = read_exact(V4_RECIPE)
    assert digest(raw) == V4_RECIPE_SHA256, 'Historical v4 recipe changed'
    data = json.loads(raw)
    assert data['schema'] == 'owner-20261119-v4-public' and data['base_commit'] == BASE
    assert data['previous_commit'] == PREVIOUS_COMMIT
    assert data['previous_recipe_sha256'] == PREVIOUS_RECIPE_SHA256
    assert previous['schema'] == 'owner-20261119-v3-public' and previous['base_commit'] == BASE
    assert set(data['candidate_delta']) == V4_CHANGED_PATHS
    assert set(data['inverse_delta']) == {SCREEN, HOPPER}
    merged = dict(previous)
    merged['schema'] = data['schema']
    merged['candidate'] = dict(previous['candidate'], **data['candidate_delta'])
    merged['inverse'] = dict(previous['inverse'], **data['inverse_delta'])
    merged['stripe_projection_sha256'] = data['stripe_projection_sha256']
    merged['change'] = data['change']
    return merged


def historical_v5_recipe():
    if not __debug__:
        raise RuntimeError("Owner qualification requires Python assertions; optimized mode refused")
    raw = read_exact(V5_RECIPE)
    assert digest(raw) == V5_RECIPE_SHA256, 'Historical v5 recipe changed'
    data = json.loads(raw)
    assert data['schema'] == 'owner-20261119-v5-metal-assembly' and data['base_commit'] == BASE
    assert data['previous_commit'] == V4_COMMIT and data['previous_recipe_sha256'] == V4_RECIPE_SHA256
    assert set(data['candidate_delta']) == V5_CHANGED_PATHS
    merged = historical_v4_recipe()
    merged['candidate'] = dict(merged['candidate'], **data['candidate_delta'])
    merged['schema'] = data['schema']
    merged['change'] = data['change']
    return merged


def recipe():
    merged = historical_v5_recipe()
    raw = read_exact(RECIPE)
    assert digest(raw) == RECIPE_SHA256, 'Owner public recipe changed'
    data = json.loads(raw)
    assert data['schema'] == 'owner-20261120-v6-dropper-alpha'
    assert data['previous_commit'] == V5_COMMIT
    assert data['previous_recipe_sha256'] == V5_RECIPE_SHA256
    assert set(data['candidate_delta']) == V6_CHANGED_PATHS
    merged['candidate'] = dict(merged['candidate'], **data['candidate_delta'])
    merged['schema'] = data['schema']
    return merged


def ensure_base_object(root=ROOT, anchor=BASE):
    assert anchor in {BASE, PREVIOUS_COMMIT, V4_COMMIT, V5_COMMIT}, "Unapproved historical Git anchor"
    check = subprocess.run(['git','cat-file','-e',anchor+'^{commit}'],cwd=root,capture_output=True)
    if check.returncode:
        # Only GitHub CI may acquire the exact historical object automatically.
        # Local validation never silently makes a network request.
        if os.environ.get('GITHUB_ACTIONS') != 'true' or os.environ.get('CI') != 'true':
            raise AssertionError('Missing exact historical Git object '+anchor+'; fetch that commit before validation')
        subprocess.run(['git','fetch','--no-tags','--depth=1','origin',anchor],cwd=root,check=True,capture_output=True,timeout=120)
    actual = subprocess.check_output(['git','rev-parse',anchor+'^{commit}'],cwd=root,text=True).strip()
    assert actual == anchor, 'Historical Git commit changed'


def ensure_baseline():
    global BASELINE, _BASELINE_TEMP
    if BASELINE is not None:
        return BASELINE
    data = recipe()
    ensure_base_object()
    raw = subprocess.check_output(['git','archive','--format=tar',BASE,'--',*sorted(data['baseline'])],cwd=ROOT)
    temporary = tempfile.TemporaryDirectory(prefix='iedctr-owner-baseline-')
    dest = Path(temporary.name)
    seen = set()
    try:
        with tarfile.open(fileobj=io.BytesIO(raw),mode='r:') as archive:
            for member in archive:
                if member.isdir():continue
                assert member.isfile() and member.name in data['baseline'], 'Unexpected historical Git entry'
                assert member.name not in seen and '..' not in Path(member.name).parts
                with archive.extractfile(member) as stream:
                    content = stream.read()
                assert digest(content) == data['baseline'][member.name], 'Historical Git bytes changed'
                path = dest/member.name;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(content)
                seen.add(member.name)
        assert seen == set(data['baseline']), 'Incomplete historical Git inventory'
    except BaseException:
        temporary.cleanup();raise
    _BASELINE_TEMP = temporary
    atexit.register(temporary.cleanup)
    BASELINE = dest
    return BASELINE


def project_source(path, content):
    data = recipe()
    assert path in data['inverse'], 'No owner inverse for this path'
    assert digest(content.encode()) == data['candidate'][path], 'Unapproved owner source bytes: ' + path
    restored = content
    for edit in reversed(data['inverse'][path]):
        start, end = edit['start'], edit['end']
        assert restored[start:end] == edit['from'], 'Owner inverse anchor changed'
        restored = restored[:start] + edit['to'] + restored[end:]
    assert digest(restored.encode()) == data['baseline'][path], 'Owner inverse does not restore 87f8'
    return restored


def validate_candidate(root=ROOT):
    """Verify real candidate, historical evidence, guards, source names and scope.

    Deliberately no memoization: changing an asset/guard between calls fails.
    Existing validation adapters have their exact bytes pinned separately.
    """
    data = recipe()
    ensure_baseline()
    # Close the inventory as well as known file hashes. Cache files are not
    # source; all additions to these protected scopes need a versioned recipe.
    for scope in ['tools/validation',
                  'src/main/java/com/oblixorprime/engineersdecorreforged/client',
                  'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/factory_steel_v2']:
        expected = {p for p in set(data['baseline']) | set(data['candidate']) if p.startswith(scope+'/')}
        expected |= {p for p in NEW_GATE_PATHS if p.startswith(scope+'/')}
        actual = {str(p.relative_to(root)) for p in (root/scope).rglob('*')
                  if p.is_file() and not (scope == 'tools/validation'
                      and p.parent.name == '__pycache__' and p.suffix == '.pyc')}
        assert actual == expected, 'Protected source inventory changed: ' + scope
    for path, expected in data['baseline'].items():
        assert digest(read_exact(BASELINE / path)) == expected, 'Historical baseline altered: ' + path
        if path not in data['candidate'] and path not in ADAPTER_HASHES:
            assert digest(read_exact(root / path)) == expected, 'Third-party or guard mutation: ' + path
    for path, expected in ADAPTER_HASHES.items():
        assert digest(read_exact(root / path)) == expected, 'Historical guard adapter altered: ' + path
    for path, expected in data['candidate'].items():
        assert digest(read_exact(root / path)) == expected, 'Wrong owner asset/source: ' + path
    # Original private references are intentionally absent. Public CI validates
    # the source attestations and exact outputs, not a source reconstruction.
    for name in ('sources','baseline_87f8'):
        assert not (root/'tools/approved_gui_20261119'/name).exists(), 'Private or duplicated evidence in public tree'
    expected_sources = {
        'ChatGPT Image 19 Nov 2026, 05_52_17-1.png': '566a03669af7e5299b4ec3c795f8cb4937fa47b8cb1770ca16d6b00676196282',
        'ChatGPT Image 19 Nov 2026, 05_52_20-2.png': '844ded3692701c998790b2d61b946355a6f6a44f72e4b389f6f558ce8b8d5fa2'}
    # Exact spelling is independently asserted in candidate tests as well.
    assert data['source_attestations'] == expected_sources, 'Source attestation changed'
    for path in data['inverse']:
        project_source(path, (root / path).read_text())
    return data


def historical_root():
    validate_candidate()
    return BASELINE


ADAPTER_HASHES = {'tools/validation/factory_r04_inverse_projection.py': 'de8208b3383b39d5afd2707d381e64b80b6e229c73790dc1667946f5f012b0f9', 'tools/validation/test_factory_r04_scoped.py': '1ccc93b2705266fd4d2e608810355e0937e090de25560e97b149af7cf76c4dd8', 'tools/validation/test_factory_registered_artwork.py': 'db126fe274db2944a833747e0fc2b8fa202c733fab5fa17180d8e667d1aea867'}

NEW_GATE_PATHS = {'tools/validation/factory_owner_20261119.py', 'tools/validation/test_factory_owner_tooltip_qualification.py'}

HOPPER = 'src/main/java/com/oblixorprime/engineersdecorreforged/client/FactoryHopperSteelArtwork.java'
V4_CHANGED_PATHS = {SCREEN, HOPPER,
 'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/factory_steel_v2/hopper.png',
 'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/factory_steel_v2/dropper.png',
 'tools/approved_gui_20261119/build_assets.py',
 'tools/approved_gui_20261119/test_assets.py'}

V5_CHANGED_PATHS = {
 'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/factory_steel_v2/hopper.png',
 'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/factory_steel_v2/dropper.png',
 'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/factory_steel_v2/owner_control_well.png',
 'tools/approved_gui_20261119/build_metal_assets.py',
 'tools/approved_gui_20261119/test_metal_assets.py'}
_V4_VIEW = None
V6_CHANGED_PATHS = {
 'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/factory_steel_v2/dropper.png',
 'tools/approved_gui_20261119/dropper_alpha.py',
 'tools/approved_gui_20261119/test_dropper_alpha.py'}


def historical_v4_root():
    """Ephemeral exact Git view for unchanged old-art tests, never current proof."""
    global _V4_VIEW
    validate_candidate()
    data = historical_v4_recipe()
    expected = dict(data['baseline'], **data['candidate'])
    expected.update(ADAPTER_HASHES)
    if _V4_VIEW is None:
        ensure_base_object(anchor=V4_COMMIT)
        raw = subprocess.check_output(['git','archive','--format=tar',V4_COMMIT,'--',*sorted(expected)],cwd=ROOT)
        temporary = tempfile.TemporaryDirectory(prefix='iedctr-v4-history-')
        dest = Path(temporary.name)
        seen = set()
        with tarfile.open(fileobj=io.BytesIO(raw),mode='r:') as archive:
            for member in archive:
                if member.isdir():continue
                assert member.isfile() and member.name in expected and member.name not in seen
                with archive.extractfile(member) as stream: content = stream.read()
                assert digest(content) == expected[member.name], 'Historical v4 Git bytes changed'
                path=dest/member.name;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(content);seen.add(member.name)
        assert seen == set(expected)
        # Read-only git-show calls in old tests resolve existing objects; no checkout.
        gitdir=subprocess.check_output(['git','rev-parse','--absolute-git-dir'],cwd=ROOT,text=True).strip()
        (dest/'.git').write_text('gitdir: '+gitdir+'\n')
        atexit.register(temporary.cleanup)
        _V4_VIEW=(dest,temporary)
    dest=_V4_VIEW[0]
    for path,sha in expected.items():assert digest(read_exact(dest/path)) == sha, 'Historical v4 view altered'
    return dest
