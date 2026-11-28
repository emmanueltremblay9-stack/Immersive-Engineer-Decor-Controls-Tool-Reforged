from pathlib import Path
import ctypes
import ctypes.util
import hashlib
import json
import os
import secrets
import shutil
import subprocess
import sys

if sys.platform != "linux":
    raise SystemExit("Qualification native automatique prise en charge uniquement sous Linux/X11. "
                     "La compilation du mod reste possible sous Windows avec gradlew.bat.")
if not os.environ.get("DISPLAY"):
    raise SystemExit("DISPLAY absent : lancer ce banc dans une session X11 accessible.")
for library in ("X11", "Xtst"):
    name = ctypes.util.find_library(library)
    if not name:
        raise SystemExit(f"Bibliothèque de qualification absente : lib{library}.")
    try:
        ctypes.CDLL(name)
    except OSError as error:
        raise SystemExit(f"Bibliothèque de qualification inaccessible : {name}: {error}")

project = Path(__file__).resolve().parents[2]
wrapper = ["bash", "gradlew"]
source_baseline = "e8b21c91d5a7a3a6beb9d0b12c71e8bccc0f8d2c"
languages = ("en_us", "fr_fr")
machines = ("factory_hopper", "factory_dropper", "small_lab_furnace")

def gradle(*args, env=None):
    subprocess.run(wrapper + list(args) + ["--console=plain", "--no-daemon"],
                   cwd=project, env=env, check=True)

def sha(data):
    return hashlib.sha256(data).hexdigest()

def source_entries():
    paths = subprocess.check_output(
        ["git", "ls-files", "--cached", "--others", "--exclude-standard", "-z"],
        cwd=project).decode().split("\0")
    files = []
    for name in sorted(set(paths)):
        if not name or not (name.startswith(("src/", "gradle/")) or name in
            ("build.gradle", "settings.gradle", "gradle.properties", "gradlew", "gradlew.bat")):
            continue
        path = project / name
        if path.is_file():
            data = path.read_bytes()
            files.append({"path": name, "size_bytes": len(data), "sha256": sha(data)})
    return files

actual_head = subprocess.check_output(
    ["git", "rev-parse", "HEAD"], cwd=project, text=True).strip()
expected_head = os.environ.get("EXPECTED_SOURCE_SHA", "")
assert expected_head == actual_head and len(actual_head) == 40, "Le checkout ne correspond pas au SHA attendu."
subprocess.run(["git", "merge-base", "--is-ancestor", source_baseline, actual_head], cwd=project, check=True)
assert not subprocess.check_output(["git", "status", "--porcelain"], cwd=project, text=True).strip(), "Le worktree doit être propre."
gradle("verifyFactoryPlacerPackagedRuntime",
       "-Pinclude_factory_placer_native_validation=true")
report = json.loads((project / "build/reports/factory-placer-packaged-runtime.json").read_text())
jar = Path(report["jar_path"]).resolve()
jar_sha = sha(jar.read_bytes())
assert jar_sha == report["jar_sha256"]

campaigns = project / "build/factory-study-native" / secrets.token_hex(16)
campaigns.mkdir(parents=True, exist_ok=False)
files = source_entries()
manifest = campaigns / "working-tree-source-manifest.json"
manifest.write_text(json.dumps({
    "baseline_commit": source_baseline,
    "candidate_commit": actual_head,
    "source_state": "EXACT_COMMIT_WORKTREE",
    "baseline_contains_integration_changes": False,
    "files": files
}, indent=2) + "\n")
manifest_sha = sha(manifest.read_bytes())
(campaigns / "packaged-runtime-classpath.json").write_text(json.dumps(report, indent=2) + "\n")
proofs = []

for language in languages:
    assert source_entries() == files, "Sources ou harness modifiés entre les campagnes."
    assert sha(jar.read_bytes()) == jar_sha, "JAR modifié entre les campagnes."
    nonce = secrets.token_hex(16)
    evidence = campaigns / f"{language}-{nonce}"
    game = evidence / "game"
    game.mkdir(parents=True, exist_ok=False)
    (game / ".iedctr-client-gameplay-nonce").write_text(nonce)
    (game / "options.txt").write_text(
        "onboardAccessibility:false\ntutorialStep:none\nrenderDistance:2\n"
        "simulationDistance:2\nmaxFps:30\npauseOnLostFocus:false\n"
        f"soundCategory_master:0.0\nguiScale:2\nlang:{language}\nfullscreen:false\n")
    init = evidence / "factory-study.init.gradle"
    init.write_text("""gradle.projectsEvaluated {
 rootProject.extensions.getByName('neoForge').runs.named('factoryStudyPackagedClient') {
  gameDirectory.set(rootProject.file(System.getenv('IEDCTR_STUDY_GAME_DIR')))
  systemProperty 'iedctr.factoryStudyLanguage', System.getenv('IEDCTR_STUDY_LANGUAGE')
  systemProperty 'iedctr.clientGameplayNonce', System.getenv('IEDCTR_STUDY_NONCE')
  systemProperty 'iedctr.clientGameplayDirectory', System.getenv('IEDCTR_STUDY_GAME_DIR')
  systemProperty 'iedctr.executedSourceSha', System.getenv('IEDCTR_STUDY_CANDIDATE')
  systemProperty 'iedctr.factoryStudyCandidateCommit', System.getenv('IEDCTR_STUDY_CANDIDATE')
  systemProperty 'iedctr.workingTreeManifestSha256', System.getenv('IEDCTR_STUDY_MANIFEST_SHA256')
  systemProperty 'iedctr.factoryStudySourceBaseline', System.getenv('IEDCTR_STUDY_BASELINE')
  systemProperty 'iedctr.factoryStudyWorkingManifestSha256', System.getenv('IEDCTR_STUDY_MANIFEST_SHA256')
  systemProperty 'iedctr.factoryStudyWorkingManifest', System.getenv('IEDCTR_STUDY_MANIFEST')
  systemProperty 'iedctr.expectedModJar', System.getenv('IEDCTR_STUDY_JAR')
  systemProperty 'iedctr.expectedModJarSha256', System.getenv('IEDCTR_STUDY_JAR_SHA256')
  jvmArgument '-Xms512M'
  jvmArgument '-Xmx4G'
  jvmArgument '-XX:ActiveProcessorCount=4'
  programArgument '--width'; programArgument '1920'
  programArgument '--height'; programArgument '1080'
 }
 rootProject.tasks.named('createFactoryStudyPackagedClientLaunchScript') {
  dependsOn rootProject.tasks.named('verifyFactoryPlacerPackagedRuntime')
 }
}
""")
    env = os.environ.copy()
    env.update({
        "IEDCTR_STUDY_LANGUAGE": language,
        "IEDCTR_STUDY_GAME_DIR": str(game), "IEDCTR_STUDY_NONCE": nonce,
        "IEDCTR_STUDY_BASELINE": source_baseline,
        "IEDCTR_STUDY_CANDIDATE": actual_head, "IEDCTR_STUDY_MANIFEST": str(manifest),
        "IEDCTR_STUDY_MANIFEST_SHA256": manifest_sha, "IEDCTR_STUDY_JAR": str(jar),
        "IEDCTR_STUDY_JAR_SHA256": jar_sha,
        "LP_NUM_THREADS": "4", "ALSOFT_DRIVERS": "null"
    })
    print("Campagne à froid :", language, "Dossier de preuves :", evidence, flush=True)
    # Cette tâche produit le lanceur officiel sans démarrer Minecraft.
    gradle("-Dorg.gradle.jvmargs=-Xmx768M -XX:ActiveProcessorCount=4",
           "-Pinclude_factory_placer_native_validation=true",
           "--init-script", str(init), "createFactoryStudyPackagedClientLaunchScript", env=env)
    launch_script = project / "build/moddev/runFactoryStudyPackagedClient.sh"
    assert launch_script.is_file(), "Lanceur officiel ModDev absent."
    launch_files = evidence / "generated-launch-files"
    launch_files.mkdir()
    launch_manifest = []
    for name in (
        "runFactoryStudyPackagedClient.sh",
        "factoryStudyPackagedClientRunClasspath.txt",
        "factoryStudyPackagedClientRunVmArgs.txt",
        "factoryStudyPackagedClientRunProgramArgs.txt",
        "factoryStudyPackagedClientLegacyClasspath.txt",
        "factoryStudyPackagedClientLog4j2.xml",
    ):
        path = project / "build/moddev" / name
        assert path.is_file(), f"Fichier de lancement absent : {name}"
        data = path.read_bytes()
        shutil.copyfile(path, launch_files / name)
        launch_manifest.append({"file": name, "size_bytes": len(data), "sha256": sha(data)})
    (evidence / "generated-launch-manifest.json").write_text(
        json.dumps(launch_manifest, indent=2) + "\n")
    assert source_entries() == files and sha(jar.read_bytes()) == jar_sha
    # Gradle est terminé. Aucune commande Gradle entre cet export et la fin du client.
    # Exécuter le script généré tel quel ; ne pas recomposer ses arguments Java.
    with (evidence / "client.log").open("w") as log:
        subprocess.run(["bash", str(launch_script)], cwd=project, env=env,
                       stdout=log, stderr=subprocess.STDOUT, check=True)
    proof_path = game / "factory-study-proof.json"
    assert proof_path.is_file(), "Aucun rapport du banc ; consulter les journaux de lancement."
    proof = json.loads(proof_path.read_text())
    assert proof["status"] == "PASS", proof.get("error", proof.get("failed_step"))
    assert proof["language_profile"] == language
    assert proof["full_resource_reload"] == "NOT_PERFORMED_COLD_START_PROFILE"
    assert proof["opened_registered_screens"] == 3
    assert proof["filled_visible_slots"] == 152
    assert len(proof["controls"]) == 14
    assert len(proof["tooltips"]) == 9
    assert all(row["language"] == language for row in proof["tooltips"])
    assert len(proof["jei_checks"]) == 3
    jei = {row["test"]: row for row in proof["jei_checks"]}
    assert set(jei) == {"progress_shortcut", "recipes_R", "uses_U"}
    assert jei["progress_shortcut"]["selected_recipe_type"] == "minecraft:smelting"
    assert jei["recipes_R"]["focus_role"] == "OUTPUT"
    assert jei["recipes_R"]["focus_item"] == "minecraft:iron_ingot"
    assert jei["recipes_R"]["focus_matches_hovered_item"] is True
    assert jei["uses_U"]["focus_role"] == "INPUT"
    assert jei["uses_U"]["focus_item"] == "minecraft:iron_ore"
    assert jei["uses_U"]["focus_matches_hovered_item"] is True
    assert proof["packaged_runtime"]["jar_sha256"] == jar_sha
    assert proof["working_tree_manifest_sha256"] == manifest_sha
    assert sha(jar.read_bytes()) == jar_sha
    assert source_entries() == files
    proofs.append((proof_path, proof))
    print("Campagne PASS :", language, "Rapport :", proof_path, flush=True)

observed = [(row["machine"], row["language"], row["gui_scale"])
            for _, proof in proofs for row in proof["tooltips"]]
expected = {(machine, language, scale) for machine in machines
            for language in languages for scale in (1, 2, 4)}
assert len(observed) == 18 and set(observed) == expected, "Union des 18 combinaisons incomplète."
summary = {
    "status": "PASS",
    "qualification_profile": "TWO_COLD_START_CAMPAIGNS",
    "native_launcher": "OFFICIAL_MODDEV_LAUNCH_SCRIPT_GRADLE_TERMINATED",
    "jar_sha256": jar_sha,
    "working_tree_manifest_sha256": manifest_sha,
    "tooltip_combination_count": len(observed),
    "full_resource_reload": "NOT_PERFORMED_COLD_START_PROFILE",
    "reference_repeated_reload_status": "BLOCKED_MEMORY",
    "proofs": [{"language": proof["language_profile"],
                "path": str(path.relative_to(campaigns)),
                "sha256": sha(path.read_bytes())} for path, proof in proofs]
}
summary_path = campaigns / "two-cold-start-campaigns.json"
summary_path.write_text(json.dumps(summary, indent=2) + "\n")
print("Qualification native à froid PASS pour ce JAR :", jar_sha)
print("Deux preuves originales et synthèse conservées dans :", campaigns)
