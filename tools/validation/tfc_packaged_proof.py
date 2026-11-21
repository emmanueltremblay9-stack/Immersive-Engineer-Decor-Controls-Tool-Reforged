"""Independent packaged TFC/Mooshroom ordinary-JAR provenance and physical-input gate."""
from pathlib import Path
import hashlib
import os
import zipfile
from fluid_gestures_native import require
from factory_placer_delivery_evidence import uri_identity
from client_mixin_smoke import SmokeFailure

MOD = 'immersive_engineer_decor_controls_tool_reforged'
HARNESS = 'iedctr_tfc_validation'
PREFIX = 'com.oblixorprime.engineersdecorreforged.'
CLASSES = [PREFIX+x for x in ('EngineersDecorReforged', 'client.MachineScreen',
    'client.MachineScreen$SmallMilkingMachineScreen', 'menu.MachineMenu', 'utility.MachineLayout', 'utility.MachineBlockEntity','compat.TfcMilkingCompat','compat.TfcMilkingAdapter')]
TEXTURES = {
    'textures/gui/milking_redesign_v1/background.png': '1405a002a4cf0ece130e1a77befa8c947891e4c6d6b96d104b7177646d060411',
    'textures/gui/milking_redesign_v1/milk.png': 'f6f0507deb081a2a83b6bb455abc6237ac2b41f4ab21d43c833e356fb2d9bbe5',
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
            and evidence_path(runtime['harness_mod_path']) != jar, 'Not an independent default Milking test mod')
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
        require(not any(n.startswith('com/oblixorprime/iedctrtest/') or 'NativeMilkingPackagedValidation' in n or 'NativeTfcMilkingRecoveryValidation' in n or 'NativeMooshroomMilkingValidation' in n
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
        raise SmokeFailure('Malformed or unreadable packaged Milking evidence: '+str(error)) from error


DEPENDENCIES = {'tfc': ('4.2.12','3c2d80e47c78cfbd617823ebe0d3d5e2f9ba9e7f04c1b34867dbab3894584440'),
                'patchouli': ('1.21.1-92-NEOFORGE',None)}

def _validate_dependencies(runtime, dependency_archives=None):
    if dependency_archives is not None:
        require(type(dependency_archives) is dict and set(dependency_archives)==set(DEPENDENCIES),
                'Offline dependency archives must identify exactly tfc and patchouli')
    require(runtime.get('neoforge_version')=='21.1.234','Wrong compatibility loader')
    rows=runtime.get('compatibility_dependencies')
    require(type(rows) is list and len(rows)==2 and all(type(r) is dict for r in rows)
        and [r.get('mod_id') for r in rows]==list(DEPENDENCIES),'Missing/duplicate compatibility dependency')
    for row in rows:
        version, pinned = DEPENDENCIES[row['mod_id']]
        require(type(row.get('jar_path')) is str and row['jar_path'], 'Missing original dependency path')
        path=Path(os.path.normpath(row['jar_path']))
        require(path.is_absolute() and path.suffix=='.jar','Original dependency JAR path must be absolute')
        # Offline copies supply bytes only; original evidence paths/URIs retain their meaning.
        archive_file=path if dependency_archives is None else Path(dependency_archives[row['mod_id']]).resolve(strict=True)
        require(archive_file.is_file() and archive_file.suffix=='.jar','Missing actual dependency JAR')
        actual=hashlib.sha256(archive_file.read_bytes()).hexdigest()
        require(row.get('version')==version and actual==row.get('sha256') and (pinned is None or actual==pinned),'Compatibility dependency identity differs')
        require(type(row.get('mod_root_uri')) is str and row['mod_root_uri'].endswith('/'),'Missing dependency module origin')
        if row['mod_id']=='tfc':
            name='net.dries007.tfc.common.entities.livestock.DairyAnimal'
            entry=name.replace('.','/')+'.class';root=row['mod_root_uri']
            require(row.get('class')==name and uri_identity(row.get('resource_uri'))==uri_identity(root+entry)
                and uri_identity(row.get('code_source_uri')) in (uri_identity(root),uri_identity(path.as_uri())),'TFC class source differs')
            with zipfile.ZipFile(archive_file) as archive:
                require(hashlib.sha256(archive.read(entry)).hexdigest()==row.get('class_sha256'),'TFC class bytes differ')

def validate_dependencies(runtime, *, dependency_archives=None):
    try:
        return _validate_dependencies(runtime, dependency_archives)
    except (OSError, ValueError, TypeError, KeyError, zipfile.BadZipFile) as error:
        raise SmokeFailure('Malformed or unreadable compatibility evidence: '+str(error)) from error

def _composed(kind,proof,nonce,source_sha,game,logs,exit_code,timed_out,width,height,language,gui_scale,*,report,
              archive_path=None, dependency_archives=None):
    from tfc_milking_recovery_native import validate_proof as recovery
    from mooshroom_milking_native import validate_proof as mooshroom
    validate_runtime(proof, report, archive_path=archive_path)
    after=proof.get('packaged_runtime_after')
    require(type(after) is dict and after==proof.get('packaged_runtime'),'Compatibility runtime changed')
    validate_dependencies(after, dependency_archives=dependency_archives)
    return (recovery if kind=='recovery' else mooshroom)(proof,nonce,source_sha,game,logs,exit_code,timed_out,width,height,language,gui_scale)

def validate_recovery(*args,**kwargs):return _composed('recovery',*args,**kwargs)
def validate_mooshroom(*args,**kwargs):return _composed('mooshroom',*args,**kwargs)
