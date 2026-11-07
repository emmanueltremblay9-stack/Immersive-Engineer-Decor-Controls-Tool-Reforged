#!/usr/bin/env python3
"""Offline production-snapshot/artwork preservation guard. Not native execution."""
import hashlib
import json
from pathlib import Path
import re
from client_mixin_smoke import SmokeFailure

ROOT=Path(__file__).resolve().parents[2]


def check_textless_milking_labels(screen):
    labels = screen.split('protected void renderLabels(', 1)[1].split('private void drawSmallFreezerTexture(', 1)[0]
    labels = re.sub(r'/\*.*?\*/|//[^\n]*', '', labels, flags=re.S)
    body = labels.split('{', 1)[1]
    match = re.match(r'\s*MachineLayout\s+layout\s*=\s*\(\(MachineMenu\)this\.menu\)\.layout\(\);\s*'
                     r'if\s*\(\(\(MachineMenu\)this\.menu\)\.kind\(\)\s*==\s*'
                     r'MachineKind\.SMALL_MILKING_MACHINE\)\s*\{([^{}]*)\}', body)
    if match is None or match.group(1).strip() != 'return;':
        raise SmokeFailure('Milking labels must return without drawing permanent text or masks')
    if 'drawSmallMilkingMachineLabels(' in screen or 'drawMilkingLine(' in screen:
        raise SmokeFailure('Obsolete permanent Milking text helper remains')


def check_preservation(root=ROOT):
    pins=json.loads((root/'docs/testing/milking-qualification/production-pins.json').read_text())
    for name in pins.get('preserved_paths', []):
        if pins['sha256'].get(name) != pins['baseline_sha256'].get(name):
            raise SmokeFailure('Baseline artwork/layout preservation failed: '+name)
    for name,expected in pins['sha256'].items():
        path=root/name
        if path.is_symlink() or not path.is_file() or hashlib.sha256(path.read_bytes()).hexdigest()!=expected:
            raise SmokeFailure('Production preservation failed: '+name)
    j=root/'src/main/java/com/oblixorprime/engineersdecorreforged'
    screen=(j/'client/MachineScreen.java').read_text()
    check_textless_milking_labels(screen)
    renderer=(j/'client/RedesignedMachineArtwork.java').read_text()
    body=renderer.split('static void milking(',1)[1].split('static void breaker(',1)[0]
    active=screen.split('protected void renderBg(',1)[1].split('public boolean mouseClicked(',1)[0]
    required=['RedesignedMachineArtwork.milking(graphics, (MachineMenu)this.menu, x, y);','return;']
    if not all(s in active for s in required):raise SmokeFailure('Active production Milking routing differs')
    # Guard the active blits/scissor, not the old unused MachineScreen fill helper.
    required=['full(graphics, "milking_redesign_v1/background", x, y, 228)',
              'fillPixels(menu.fluidAmount(), menu.fluidCapacity(), 51)',
              'graphics.enableScissor(x + 104, y + 100 - fill, x + 126, y + 100)',
              'part(graphics, "milking_redesign_v1/milk", x + 104, y + 49, 22, 51,',
              '479, 228, 296, 802)', 'graphics.disableScissor()']
    if not all(s in body for s in required):raise SmokeFailure('Active Milking layer/crop contract differs')
    if '"milking_redesign_v1/glass"' in body:raise SmokeFailure('Glass is intentionally disabled')
    native=(j/'client/validation/NativeMilkingQualificationValidation.java').read_text()
    if 'menu.storedFluid()' in native:raise SmokeFailure('Milking menu does not synchronize fluid identity')
    if not all(s in native for s in ['screen.getGuiLeft()+104','screen.getGuiTop()+49','screen.mouseClicked(x,y,0)',
                                     'screen.mouseReleased(x,y,0)','FluidAction.EXECUTE','NeoForgeMod.MILK.value()']):
        raise SmokeFailure('Fixture bypasses required live screen/server paths')
    if not all(s in native for s in ['if(!isolatedDirectoryValidated)', 'iedctr.solarV2ReferenceNative',
                                     'captureStableTicks=0', 'if(++captureStableTicks<12)return;',
                                     'MIN_CLICK_INTERVAL_NANOS=300_000_000L']):
        raise SmokeFailure('Missing fixture isolation, settling, or single-click guards')
    finish=native.split('private static void finish(',1)[1]
    if finish.index('if(!isolatedDirectoryValidated)') > finish.index('Files.writeString('):
        raise SmokeFailure('Proof write precedes isolation guard')
    return len(pins['sha256'])


if __name__=='__main__':
    print(f'PASS {check_preservation()} pinned production files, preserved baseline artwork/layout and active renderer routing. Native compile/execution not claimed.')
