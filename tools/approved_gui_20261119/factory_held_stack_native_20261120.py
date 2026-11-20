#!/usr/bin/env python3
"""Exact ordinary-JAR, XTest physical carried-stack safety qualification for two Factory GUIs."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools" / "validation"))
from factory_control_tooltip_native import (
    FATAL_PATTERN, SmokeFailure, decode_png, fixture_coordinates, png,
    require, run_bounded,
)

CAPTURES = ["picked", "inert-border-held", "returned", "outside-witness"]


def validate_fields(proof, nonce, sha, jar, jar_sha, width, height, machine, scale):
    expected = {
        "suite": "native-factory-held-stack-v1",
        "status": "passed",
        "nonce": nonce,
        "executed_source_sha": sha,
        "machine": machine,
        "selected_language": "en_us",
        "width": width,
        "height": height,
        "gui_scale": scale,
        "opened_via_native_block_interaction": 1,
        "physical_left_clicks": 5,
        "server_client_pickups": 2,
        "inert_border_preserved": 1,
        "server_client_returns": 1,
        "outside_item_entity_confirmed": 1,
        "outside_witness_consumes_stack_by_design": True,
        "native_visual_acceptance": False,
    }
    for key, value in expected.items():
        require(type(proof.get(key)) is type(value) and proof[key] == value,
                "Native held-stack proof field differs: " + key)
    rt = proof.get("packaged_runtime", {})
    require(rt.get("jar_path") == str(jar.resolve())
            and rt.get("jar_sha256") == jar_sha
            and rt.get("fixture_free_jar") is True
            and rt.get("separate_harness_module") is True,
            "Ordinary-JAR or isolated harness identity differs")
    require("XTest" in proof.get("input_method", ""),
            "Native physical-input identity missing")
    captures = proof.get("captures", [])
    require([e.get("name") for e in captures] == CAPTURES, "Capture order or coverage differs")
    nw, nh = (176,166) if machine == "hopper" else (228,250)
    left, top = ((width//scale-nw)//2, (height//scale-nh)//2)
    player_slot = fixture_coordinates(machine)[18 if machine == "hopper" else 15]
    positions = [
        (left+player_slot[0]+8, top+player_slot[1]+8),
        (left+(-4 if machine == "hopper" else 6), top+(-8 if machine == "hopper" else 8)),
        (left+player_slot[0]+8, top+player_slot[1]+8),
        (left+(-9 if machine == "hopper" else -2), top+83),
    ]
    # The first target is a real item slot; the second is inside the visible
    # inert border; the final witness is genuinely outside the visible border.
    if machine == "hopper":
        require(-8 <= -4 < 184 and -16 <= -8 < 166 and -9 < -8,
                "Hopper half-open border geometry witness invalid")
    else:
        require(0 <= 6 < nw and 0 <= 8 < nh and -2 < 0,
                "Dropper half-open border geometry witness invalid")
    menu_ids = set()
    for entry, (x, y) in zip(captures, positions):
        require(entry.get("file") == "factory-held-" + entry["name"] + ".png",
                "Capture filename differs")
        require(entry.get("gui_scale") == scale, "Capture scale differs")
        require(type(entry.get("menu_id")) is int, "Missing native container ID")
        menu_ids.add(entry["menu_id"])
        require(all(isinstance(entry.get(k), (int,float)) and not isinstance(entry.get(k),bool)
                    for k in ("pointer_screen_x", "pointer_screen_y")),
                "Missing observed GLFW pointer position")
        require(abs(entry["pointer_screen_x"] - x) < 1
                and abs(entry["pointer_screen_y"] - y) < 1,
                "Native cursor did not reach expected screen-relative witness")
    require(len(menu_ids) == 1, "Menu changed during carried-stack witness")
    require(proof.get("error") is None, "Failed native run reported error")
    return captures, (left+player_slot[0],top+player_slot[1])


def validate(proof, nonce, sha, jar, jar_sha, game, width, height, machine, scale):
    captures, (x,y) = validate_fields(proof,nonce,sha,jar,jar_sha,width,height,machine,scale)
    images = []
    for entry in captures:
        f = game / entry["file"]
        png(f,width,height,entry.get("sha256"))
        w,h,data = decode_png(f)
        require((w,h) == (width,height), "Capture framebuffer differs")
        images.append(data)
    # Verify actual item pixels in the first player slot changed when the
    # genuinely exterior click removed the oak log. This is render smoke,
    # NOT a substitute for the authoritative client/server world-entity proof.
    returned, outside = images[2],images[3]
    xx,yy=x*scale,y*scale
    changed=0
    for row in range(3*scale,11*scale):
        for col in range(3*scale,13*scale):
            j=((yy+row)*width+xx+col)*4
            changed += returned[j:j+3] != outside[j:j+3]
    require(changed >= 12*scale*scale, "Missing rendered item removal witness")
    return True


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("--machine",required=True,choices=("hopper","dropper"))
    p.add_argument("--gui-scale",required=True,type=int,choices=(1,2))
    p.add_argument("--expect-source-sha",required=True)
    p.add_argument("--width",type=int,default=1280)
    p.add_argument("--height",type=int,default=720)
    p.add_argument("--timeout",type=int,default=900)
    p.add_argument("--prepare-timeout",type=int,default=900)
    args=p.parse_args()
    if sys.platform!="linux" or (args.width,args.height)!=(1280,720) or min(args.timeout,args.prepare_timeout)<=0:
        p.error("Requires Linux, exact 1280x720 and positive timeouts")
    for cmd in ("java","Xvfb","xvfb-run","xauth"):
        if not shutil.which(cmd):p.error("Missing prerequisite "+cmd)
    require(re.fullmatch(r"[a-f0-9]{40}",args.expect_source_sha) is not None,
            "Invalid expected commit SHA")
    def clean():
        return not subprocess.check_output(
            ["git","status","--porcelain","--untracked-files=all"],cwd=ROOT,text=True).strip()
    require(clean(),"Requires clean exact committed checkout")
    sha=subprocess.check_output(["git","rev-parse","HEAD"],cwd=ROOT,text=True).strip()
    require(sha==args.expect_source_sha,"Workflow checkout is not the requested commit")
    (ROOT/"build").mkdir(exist_ok=True)
    evidence=Path(tempfile.mkdtemp(prefix="factory-held-native-",dir=ROOT/"build"))
    game=evidence/"run";game.mkdir()
    nonce=secrets.token_hex(16)
    (game/".iedctr-client-gameplay-nonce").write_text(nonce)
    (game/"options.txt").write_text(
        "onboardAccessibility:false\ntutorialStep:none\nrenderDistance:2\nsimulationDistance:2\n"
        "maxFps:60\npauseOnLostFocus:false\nsoundCategory_master:0.0\n"
        "guiScale:"+str(args.gui_scale)+"\nlang:en_us\n")
    init=evidence/"factory-held.init.gradle"
    init.write_text("""gradle.projectsEvaluated {
 rootProject.extensions.getByName('neoForge').runs.named('factoryControlTooltipClient') {
  gameDirectory.set(rootProject.file(System.getenv('IEDCTR_GAMEPLAY_DIR')))
  systemProperty 'iedctr.factoryControlTooltipNative', 'false'
  systemProperty 'iedctr.factoryHeldStackNative', 'true'
  systemProperty 'iedctr.factoryControlMachine', System.getenv('IEDCTR_FACTORY_MACHINE')
  systemProperty 'iedctr.factoryHeldScale', System.getenv('IEDCTR_FACTORY_HELD_SCALE')
  systemProperty 'iedctr.clientGameplayNonce', System.getenv('IEDCTR_GAMEPLAY_NONCE')
  systemProperty 'iedctr.clientGameplayDirectory', System.getenv('IEDCTR_GAMEPLAY_DIR')
  systemProperty 'iedctr.executedSourceSha', System.getenv('IEDCTR_EXECUTED_SOURCE_SHA')
  systemProperty 'iedctr.expectedModJar', System.getenv('IEDCTR_EXPECTED_MOD_JAR')
  systemProperty 'iedctr.expectedModJarSha256', System.getenv('IEDCTR_EXPECTED_MOD_JAR_SHA256')
  programArgument '--width'
  programArgument System.getenv('IEDCTR_SCREEN_WIDTH')
  programArgument '--height'
  programArgument System.getenv('IEDCTR_SCREEN_HEIGHT')
 }
}
""")
    guard=evidence/"owned_display.py"
    guard.write_text(r"""import os,pathlib,re,sys
assert re.fullmatch(r':\d+(\.\d+)?',os.environ.get('DISPLAY',''))
game=pathlib.Path(os.environ['IEDCTR_GAMEPLAY_DIR'])
nonce=os.environ['IEDCTR_GAMEPLAY_NONCE']
assert (game/'.iedctr-client-gameplay-nonce').read_text()==nonce
marker=game/'.iedctr-factory-control-xvfb'
assert not marker.exists()
marker.write_text(nonce+'\n'+os.environ['DISPLAY'])
os.execvpe(sys.argv[1],sys.argv[1:],os.environ)
""")
    env=dict(os.environ,IEDCTR_FACTORY_MACHINE=args.machine,IEDCTR_FACTORY_HELD_SCALE=str(args.gui_scale),
             IEDCTR_GAMEPLAY_DIR=str(game),IEDCTR_GAMEPLAY_NONCE=nonce,
             IEDCTR_EXECUTED_SOURCE_SHA=sha,IEDCTR_SCREEN_WIDTH=str(args.width),
             IEDCTR_SCREEN_HEIGHT=str(args.height),LIBGL_ALWAYS_SOFTWARE="1",ALSOFT_DRIVERS="null")
    base=["bash",str(ROOT/"gradlew"),"--no-daemon","--stacktrace"]
    print("Factory held-stack native test checkout="+sha+" evidence="+str(evidence),flush=True)
    try:
        code,expired=run_bounded(base+["jar"],ROOT,env,evidence/"jar.log",args.prepare_timeout)
        require(code==0 and not expired,"Ordinary JAR failed")
        base+=["-Pinclude_factory_placer_native_validation=true"]
        code,expired=run_bounded(base+["verifyFactoryPlacerPackagedRuntime"],ROOT,env,evidence/"package.log",args.prepare_timeout)
        require(code==0 and not expired,"Isolated harness classpath proof failed")
        report=json.loads((ROOT/"build/reports/factory-placer-packaged-runtime.json").read_text())
        jar=Path(report["jar_path"])
        jar_sha=hashlib.sha256(jar.read_bytes()).hexdigest()
        require(jar_sha==report["jar_sha256"],"Packaged JAR identity differs")
        (evidence/"packaged-runtime-classpath.json").write_text(json.dumps(report,indent=2))
        env.update(IEDCTR_EXPECTED_MOD_JAR=str(jar.resolve()),IEDCTR_EXPECTED_MOD_JAR_SHA256=jar_sha)
        gradle=base+["--init-script",str(init)]
        code,expired=run_bounded(gradle+["prepareFactoryControlTooltipClientRun"],ROOT,env,evidence/"prepare.log",args.prepare_timeout)
        require(code==0 and not expired,"Factory held preparation failed")
        log=evidence/"client.log"
        code,expired=run_bounded(
            ["xvfb-run","-a","-s",f"-screen 0 {args.width}x{args.height}x24",
             sys.executable,str(guard)]+gradle+["runFactoryControlTooltipClient"],
             ROOT,env,log,args.timeout)
        logs=log.read_text(errors="replace")
        require(code==0 and not expired and not FATAL_PATTERN.search(logs)
                and "IEDCTR_FACTORY_HELD PASSED nonce="+nonce in logs,
                "Native Factory held-stack exit/log proof failed")
        proof=json.loads((game/"factory-held-proof.json").read_text())
        validate(proof,nonce,sha,jar,jar_sha,game,args.width,args.height,args.machine,args.gui_scale)
        require(clean() and hashlib.sha256(jar.read_bytes()).hexdigest()==jar_sha,
                "Checkout or JAR mutated during native test")
        print("PASS exact-JAR native held-stack border and exterior witnesses, machine="+args.machine
              +" scale="+str(args.gui_scale)+" jar="+jar_sha,flush=True)
        return 0
    except (SmokeFailure,OSError,ValueError,KeyError) as error:
        print("FAIL: "+str(error),file=sys.stderr)
        return 1

if __name__=="__main__":
    raise SystemExit(main())
