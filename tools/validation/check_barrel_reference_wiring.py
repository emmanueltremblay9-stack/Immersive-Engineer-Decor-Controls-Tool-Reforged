"""Source contract guardrails; does not substitute for native interactions."""
from pathlib import Path
import hashlib
import json
import struct
import re
r=Path(__file__).resolve().parents[2]
j=r/'src/main/java/com/oblixorprime/engineersdecorreforged'
s=(j/'client/BarrelReferenceScreen.java').read_text()
m=(j/'menu/MachineMenu.java').read_text()
c=(j/'client/ClientModEvents.java').read_text()
for event in ('mouseClicked','mouseReleased','mouseDragged','mouseScrolled','mouseMoved'):
    body=s.split(' '+event+'(',1)[1].split('\n   }',1)[0]
    assert 'view.localX(mouseX)' in body and 'view.localY(mouseY)' in body,event
assert 'view.localDelta(dragX), view.localDelta(dragY)' in s
assert 'view.localY(mouseY), scrollX, scrollY)' in s
assert s.index('graphics.pose().popPose()') < s.index('renderTooltip(graphics, mouseX, mouseY)')
assert 'mouseX = renderLocalMouseX' in s and 'mouseY = renderLocalMouseY' in s
assert 'FluidBarrelSpriteRenderer.draw' in s
assert 'this(kind, id, inventory, container, data, false)' in m
assert 'kind == MachineKind.FLUID_BARREL && inventory.player.level().isClientSide' in m
assert 'Boolean.getBoolean(DEV_BARREL_REFERENCE_PROPERTY)' in m
assert 'new MachineScreen.FluidBarrelScreen(menu, inventory, title)' in c
# Everything after resolveMenuType, including transfer rules and synchronized fluid data,
# is byte-identical to the exact PR151 baseline.
assert hashlib.sha256(m.split('   private static MenuType<?> resolveMenuType',1)[1].encode()).hexdigest() == '48406a039c8b1ab4e78c9cd8ac4d3bb76408838dfee1696047d915f27cdbe88a'
print('PASS: event routing, legacy fallback, client-only opt-in, unchanged transfer/data/validation code')

g=(j/'utility/BarrelReferenceGeometry.java').read_text()
source_width=int(re.search(r'SOURCE_WIDTH = (\d+)',g)[1])
source_height=int(re.search(r'SOURCE_HEIGHT = (\d+)',g)[1])
art=r/'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/barrel_reference_v5/background.png'
assert struct.unpack('>II',art.read_bytes()[16:24])==(source_width,source_height)==(1137,1383)
manifest=json.loads((r/'docs/testing/barrel-reference-v5/geometry-manifest.json').read_text())
assert manifest['source_size']==[source_width,source_height]
assert manifest['canvas_size']==[379,461] and [source_width,source_height]==[3*x for x in manifest['canvas_size']]
assert manifest['source_sha256']==hashlib.sha256(art.read_bytes()).hexdigest()
for a in manifest['slots']:
    x,y,w,h=a['source_interior']
    assert a['canvas_item_origin']==[int((x+w/2)/3-8+0.5),int((y+h/2)/3-8+0.5)]
assert '(int)Math.ceil(view.screenX(BarrelReferenceState.TANK_X))' in s
assert '(int)Math.floor(view.screenY(BarrelReferenceState.TANK_Y + BarrelReferenceState.TANK_HEIGHT))' in s
renderer=(j/'client/FluidBarrelSpriteRenderer.java').read_text()
assert 'graphics.enableScissor(clipLeft, clipTop, clipRight, clipBottom)' in renderer
assert 'return draw(graphics, fluid, x, y, width, height, x, y, x + width, y + height)' in renderer
print('PASS: PNG dimensions, exact aspect ratio, manifest mapping, explicit screen clip and legacy renderer signature')
