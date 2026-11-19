"""Fail-closed inverse of *only* the Factory R04 MachineScreen delta.

The historic full-file pins are never changed: first prove the exact R04 Git
blob, invert exactly five anchored Factory edits, then prove the exact main blob.
Anything else, including one character in a different machine, is rejected.
"""
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCREEN_PATH = 'src/main/java/com/oblixorprime/engineersdecorreforged/client/MachineScreen.java'
GAME_TEST_PATH = 'src/main/java/com/oblixorprime/engineersdecorreforged/gametest/MachineGuiGameTests.java'
LAYOUT_PATH = 'src/main/java/com/oblixorprime/engineersdecorreforged/utility/MachineLayout.java'
RECIPE_PATH = Path(__file__).with_name('factory_r04_inverse_edits.json')
RECIPE_GIT_BLOB = '1c3d1dabb7b9ab3443843bd70ecec8e54d70a112'
LAYOUT_RECIPE_PATH = Path(__file__).with_name('factory_r04_layout_inverse_edits.json')
LAYOUT_RECIPE_GIT_BLOB = '8acd629656cc94401fe406e99c4f10702792755d'
R04_LAYOUT_BLOB = '606ff668080cf82adcf256e309ae15bcede16f99'
MAIN_LAYOUT_BLOB = '710069d40e28dba9caa13e54d47f5e6ad573937f'
R04_SCREEN_BLOB = 'c740cb61b0fa6185d0dd2e665cc9c13a4f21659c'
MAIN_SCREEN_BLOB = 'ca92525bc350e161c136e3dd295c70c381bf1153'
MAIN_GAME_TEST_BLOB = '36e1cac30819b332e185006bf211f804f2e67826'
GAME_TEST_BEFORE = 'new MachineGuiGameTests.MachineEntry(ModBlocks.FACTORY_DROPPER, MachineKind.FACTORY_DROPPER, 15, "factory_dropper_gui")'
GAME_TEST_AFTER = 'new MachineGuiGameTests.MachineEntry(ModBlocks.FACTORY_DROPPER, MachineKind.FACTORY_DROPPER, 15, "retained_dropper/background")'


class FactoryProjectionError(AssertionError):
    pass


def git_blob_sha(data):
    payload = data.encode('utf-8') if isinstance(data, str) else data
    return hashlib.sha1(b'blob ' + str(len(payload)).encode('ascii') + b'\0' + payload).hexdigest()


def project_screen_to_main(source, *, recipe_bytes=None):
    if not isinstance(source, str):
        raise FactoryProjectionError('Expected UTF-8 decoded MachineScreen text')
    # Owner v1 is first validated independently, then exactly restored to R04.
    from factory_owner_20261119 import validate_candidate, project_source
    try:
        validate_candidate()
        if git_blob_sha(source) != R04_SCREEN_BLOB:
            source = project_source(SCREEN_PATH, source)
    except (AssertionError, OSError, ValueError) as error:
        raise FactoryProjectionError(str(error)) from error
    if git_blob_sha(source) != R04_SCREEN_BLOB:
        raise FactoryProjectionError('Unexpected MachineScreen bytes; Factory-only allowlist rejected mutation')
    if recipe_bytes is None:
        with RECIPE_PATH.open('rb') as stream:
            data = stream.read()
    else:
        data = recipe_bytes
    if git_blob_sha(data) != RECIPE_GIT_BLOB:
        raise FactoryProjectionError('Inverse recipe changed')
    recipe = json.loads(data)
    if (recipe.get('schema_version') != 1 or recipe.get('source_git_blob') != R04_SCREEN_BLOB
            or recipe.get('baseline_git_blob') != MAIN_SCREEN_BLOB):
        raise FactoryProjectionError('Inverse recipe identities changed')
    edits = recipe.get('edits')
    if not isinstance(edits, list) or len(edits) != 5:
        raise FactoryProjectionError('Factory-only inverse edit count changed')
    projected = source
    for record in edits:
        before = record.get('from')
        after = record.get('to')
        if not isinstance(before, str) or not before or not isinstance(after, str):
            raise FactoryProjectionError('Malformed anchored inverse')
        if projected.count(before) != 1:
            raise FactoryProjectionError('Factory-only inverse anchor missing or duplicated: ' + str(record.get('name')))
        projected = projected.replace(before, after, 1)
    if git_blob_sha(projected) != MAIN_SCREEN_BLOB:
        raise FactoryProjectionError('Foreign or unaccounted MachineScreen modification')
    return projected


def project_layout_to_main(source, *, recipe_bytes=None):
    """Precisely undo four Dropper-only layout edits; no other layout may change."""
    if not isinstance(source, str):
        raise FactoryProjectionError('Expected UTF-8 decoded MachineLayout text')
    if git_blob_sha(source) != R04_LAYOUT_BLOB:
        raise FactoryProjectionError('Unexpected MachineLayout bytes; Dropper-only allowlist rejected mutation')
    if recipe_bytes is None:
        with LAYOUT_RECIPE_PATH.open('rb') as stream:
            data = stream.read()
    else:
        data = recipe_bytes
    if git_blob_sha(data) != LAYOUT_RECIPE_GIT_BLOB:
        raise FactoryProjectionError('Dropper layout inverse recipe changed')
    recipe = json.loads(data)
    if (recipe.get('schema_version') != 1 or recipe.get('source_git_blob') != R04_LAYOUT_BLOB
            or recipe.get('baseline_git_blob') != MAIN_LAYOUT_BLOB):
        raise FactoryProjectionError('Dropper layout inverse identities changed')
    edits = recipe.get('edits')
    if not isinstance(edits, list) or len(edits) != 4:
        raise FactoryProjectionError('Dropper-only layout edit count changed')
    projected = source
    for record in edits:
        before = record.get('from')
        after = record.get('to')
        if not isinstance(before, str) or not before or not isinstance(after, str):
            raise FactoryProjectionError('Malformed Dropper-only layout inverse')
        if projected.count(before) != 1:
            raise FactoryProjectionError('Dropper layout anchor missing or duplicated: ' + str(record.get('name')))
        projected = projected.replace(before, after, 1)
    if git_blob_sha(projected) != MAIN_LAYOUT_BLOB:
        raise FactoryProjectionError('Foreign or unaccounted MachineLayout modification')
    return projected


def project_preserved_bytes(path, original_bytes):
    """Project only the two exact Factory-changed Java sources. All other pins stay raw."""
    try:
        if path == SCREEN_PATH:
            return project_screen_to_main(original_bytes.decode('utf-8')).encode('utf-8')
        if path == LAYOUT_PATH:
            return project_layout_to_main(original_bytes.decode('utf-8')).encode('utf-8')
    except UnicodeDecodeError as error:
        raise FactoryProjectionError('Pinned Java source is not UTF-8') from error
    return original_bytes


def assert_exact_dropper_game_test(source):
    """The sole allowed GameTest modification is its actual Dropper parent texture."""
    if source.count(GAME_TEST_AFTER) != 1 or GAME_TEST_BEFORE in source:
        raise FactoryProjectionError('Dropper GameTest expectation missing or ambiguous')
    reverted = source.replace(GAME_TEST_AFTER, GAME_TEST_BEFORE, 1)
    if git_blob_sha(reverted) != MAIN_GAME_TEST_BLOB:
        raise FactoryProjectionError('Foreign GameTest edit detected')
    return reverted
