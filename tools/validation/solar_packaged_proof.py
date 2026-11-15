"""Independent default Solar ordinary-JAR provenance and physical-input gate."""
from pathlib import Path
import hashlib
import os
import zipfile
from fluid_gestures_native import require
from factory_placer_delivery_evidence import uri_identity
from client_mixin_smoke import SmokeFailure

MOD = 'immersive_engineer_decor_controls_tool_reforged'
HARNESS = 'iedctr_solar_validation'
PREFIX = 'com.oblixorprime.engineersdecorreforged.'
CLASSES = [PREFIX+x for x in ('EngineersDecorReforged', 'client.SolarV2ReferenceScreen',
    'utility.SolarV2ReferenceGeometry', 'utility.SolarV2ReferenceState', 'menu.MachineMenu', 'utility.MachineLayout', 'utility.MachineBlockEntity')]
TEXTURES = {
    'textures/gui/solar_v2_reference/background.png': 'd702051b09e09cd7c9ebd7ea874356ba82324db07bcb0657cf8ac66bdf54c796',
}



def _validate_runtime(proof, report, archive_path=None):
    require(type(proof) is dict and type(report) is dict, 'Missing packaged report')
    runtime = proof.get('packaged_runtime')
    require(type(runtime) is dict, 'Missing runtime origin proof')
    require(report.get('fixture_free_jar') is True and report.get('split_packages') is False
            and report.get('loaded_development_mods') == [HARNESS], 'Unsafe development mod registration')
    require(type(report.get('jar_path')) is str and report['jar_path']
            and type(runtime.get('jar_path')) is str and runtime['jar_path'], 'Missing production JAR path')
    require(Path(report['jar_path']).is_absolute() and Path(runtime['jar_path']).is_absolute(),
            'Production JAR evidence paths must be absolute')
    # An offline archive supplies bytes only. CI paths keep their original meaning,
    # without consulting unrelated local symlinks or rewriting provenance evidence.
    def evidence_path(value):
        return Path(os.path.normpath(value)) if archive_path is not None else Path(value).resolve()
    jar = evidence_path(report['jar_path'])
    archive_file = jar if archive_path is None else Path(archive_path).resolve(strict=True)
    digest = hashlib.sha256(archive_file.read_bytes()).hexdigest()
    require(jar.suffix == '.jar' and digest == report.get('jar_sha256') == runtime.get('jar_sha256')
            and evidence_path(runtime['jar_path']) == jar, 'Wrong production JAR')
    cp, outputs = report.get('runtime_classpath'), report.get('forbidden_main_outputs')
    require(type(cp) is list and type(outputs) is list and outputs
            and all(type(p) is str and p and Path(p).is_absolute() for p in cp+outputs), 'Missing classpath exclusion proof')
    cp_paths, main_paths = {evidence_path(p) for p in cp}, {evidence_path(p) for p in outputs}
    require(jar in cp_paths and not cp_paths.intersection(main_paths), 'Source-output runtime or missing JAR')
    require(runtime.get('harness_mod_id') == HARNESS and runtime.get('fixture_free_jar') is True
            and runtime.get('separate_harness_module') is True and runtime.get('production_selectors_absent') is True
            and runtime.get('production_selectors') == [] and 'opt_in_selector' not in runtime
            and type(runtime.get('harness_mod_path')) is str and runtime['harness_mod_path']
            and Path(runtime['harness_mod_path']).is_absolute()
            and evidence_path(runtime['harness_mod_path']) != jar, 'Not an independent default Solar test mod')
    root = runtime.get('mod_root_uri')
    require(type(root) is str and root and root.endswith('/'), 'Missing authoritative mod root')
    root_identity, jar_identity = uri_identity(root), uri_identity(jar.as_uri())
    origins, textures = runtime.get('class_origins'), runtime.get('active_textures')
    require(type(origins) is list and all(type(r) is dict for r in origins)
            and [r.get('class') for r in origins] == CLASSES, 'Incomplete class provenance')
    require(type(textures) is list and len(textures) == len(TEXTURES) and all(type(r) is dict for r in textures)
            and {r.get('path') for r in textures} == set(TEXTURES), 'Incomplete active artwork provenance')
    with zipfile.ZipFile(archive_file) as archive:
        names = archive.namelist()
        require(len(names) == len(set(names)), 'Duplicate production archive entries')
        require(not any(n.startswith('com/oblixorprime/iedctrtest/') or 'NativeSolarV2ReferenceValidation' in n
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


def validate_runtime(proof, report, *, archive_path=None):
    """Verify actual archive bytes and exact module origins, independently of input evidence."""
    try:
        return _validate_runtime(proof, report, archive_path)
    except (OSError, ValueError, TypeError, KeyError, zipfile.BadZipFile) as error:
        raise SmokeFailure('Malformed or unreadable packaged Solar evidence: '+str(error)) from error


def validate_proof(proof, nonce, source_sha, game, logs, exit_code, timed_out,
                   width, height, language, report, gui_scale=1, *, archive_path=None):
    """Compose provenance with the existing Solar physical-input/PNG validator."""
    # Local import also lets the native runner use validate_runtime without a cycle.
    from solar_v2_reference_native import validate_proof as validate_physical_proof
    require(type(exit_code) is int and type(timed_out) is bool,
            'Malformed native process result')
    validate_runtime(proof, report, archive_path=archive_path)
    return validate_physical_proof(proof, nonce, source_sha, game, logs, exit_code,
                                   timed_out, width, height, language, gui_scale)
