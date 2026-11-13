"""Fail-closed source/art contract guardrails. Not a replacement for mapped/native tests."""
from pathlib import Path
import hashlib
import json
import re
import struct
from tree_integration_preservation import assert_menu_tail_preserved, assert_solar_baseline_preserved

r = Path(__file__).resolve().parents[2]
j = r / 'src/main/java/com/oblixorprime/engineersdecorreforged'
s = (j / 'client/SolarV2ReferenceScreen.java').read_text()
m = (j / 'menu/MachineMenu.java').read_text()
c = (j / 'client/ClientModEvents.java').read_text()
g = (j / 'utility/SolarV2ReferenceGeometry.java').read_text()
state = (j / 'utility/SolarV2ReferenceState.java').read_text()
d = r / 'docs/testing/solar-v2-reference'

for event in ('mouseClicked', 'mouseReleased', 'mouseDragged', 'mouseScrolled', 'mouseMoved'):
    body = s.split(' ' + event + '(', 1)[1].split('\n   }', 1)[0]
    assert 'view.localX(mouseX)' in body and 'view.localY(mouseY)' in body, event
assert 'view.localDelta(dragX), view.localDelta(dragY)' in s
assert 'view.localY(mouseY), scrollX, scrollY)' in s
assert s.index('graphics.pose().popPose()') < s.index('renderTooltip(graphics, mouseX, mouseY)')
assert 'mouseX = renderLocalMouseX' in s and 'mouseY = renderLocalMouseY' in s
assert 'SolarV2ReferenceState.fillWidth(menu.energyStored(), menu.energyCapacity())' in s
assert 'menu.energyStored(), menu.energyCapacity(), menu.energyDisplayGranularity()' in s
# External integrations see transformed screen bounds; vanilla internals stay local.
for method, geometry_method in (('getGuiLeft', 'guiLeft'), ('getGuiTop', 'guiTop'),
                                ('getXSize', 'guiWidth'), ('getYSize', 'guiHeight')):
    assert f'public int {method}() {{ return view.{geometry_method}(); }}' in s
assert 'leftPos = 0; topPos = 0;' in s
assert 'imageWidth = SolarV2ReferenceGeometry.CANVAS_WIDTH;' in s
assert 'imageHeight = SolarV2ReferenceGeometry.CANVAS_HEIGHT;' in s
assert 'Component.literal(menu.layout().machineSlots().getFirst().role())' in s
assert 'this(kind, id, inventory, container, data, false, false, false)' in m
assert 'boolean barrelReferenceRequested, boolean factoryPlacerV2ReferenceRequested,' in m
assert 'kind == MachineKind.SMALL_SOLAR_PANEL && inventory.player.level().isClientSide' in m
assert 'kind == MachineKind.SMALL_SOLAR_PANEL && Boolean.parseBoolean(System.getProperty(DEV_SOLAR_V2_REFERENCE_PROPERTY, "true"))' in m
assert 'DEV_SOLAR_V2_REFERENCE_PROPERTY = "iedctr.dev.solarV2Reference"' in m
assert '? new SolarV2ReferenceScreen(menu, inventory, title) : new MachineScreen.SmallSolarPanelScreen(menu, inventory, title)' in c
assert 'SolarV2ReferenceGeometry.anchor(slot.containerIndex())' in m
assert 'SolarV2ReferenceGeometry.anchor(1 + row * 9 + column)' in m
assert 'SolarV2ReferenceGeometry.anchor(28 + column)' in m
# PR173 adds only an exact presentation accessor to the pinned downstream menu code.
assert_menu_tail_preserved(m)
# Keep historical pins, with exact documented Tree integration identities only.
preserved = json.loads((d / 'preserved-baseline-sha256.json').read_text())
# Milking evolves only these shared production files; retain the exact reconciled
# Solar baseline and all remaining source/art pins without broad exemptions.
milking = json.loads((r / 'docs/testing/milking-qualification/production-pins.json').read_text())
assert milking['reconciliation_commit'] == 'e8999e7cdd7e3a5d68d42b05ca3b0d6ecc9ac8d0'
milking_shared_paths = {
    'src/main/java/com/oblixorprime/engineersdecorreforged/client/MachineScreen.java',
    'src/main/java/com/oblixorprime/engineersdecorreforged/utility/MachineBlockEntity.java',
}
for path, expected in preserved['files'].items():
    if path in milking_shared_paths:
        assert milking['reconciliation_baseline_sha256'][path] == expected, path
milking_current_hashes = {path: milking['sha256'][path] for path in milking_shared_paths}
assert_solar_baseline_preserved(r, preserved, milking_current_hashes)
print('PASS: independent client-only default client presentation with explicit false fallback, 37 index routes, inverse pointer paths, legacy and Barrel preservation')

art = r / 'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/solar_v2_reference/background.png'
source_hash = hashlib.sha256(art.read_bytes()).hexdigest()
assert source_hash == 'd702051b09e09cd7c9ebd7ea874356ba82324db07bcb0657cf8ac66bdf54c796'
assert source_hash in g
assert struct.unpack('>II', art.read_bytes()[16:24]) == (1254, 1254)
manifest = json.loads((d / 'geometry-manifest.json').read_text())
assert manifest['source_sha256'] == source_hash
assert manifest['source_size'] == [1254, 1254]
assert manifest['canvas_size'] == [418, 418]
assert manifest['fe_source_safe_interior'] == [146, 588, 851, 37]
assert manifest['fe_canvas_interior'] == [49, 196, 283, 12]
assert len(manifest['slots']) == 37
anchors = [list(map(int, match)) for match in re.findall(r'centeredAnchor\((\d+), (\d+), (\d+), (\d+), (\d+)\)', g)]
assert len(anchors) == 37
for i, a in enumerate(manifest['slots']):
    assert a['menu_index'] == i
    x, y, w, h = a['source_interior']
    assert anchors[i] == [i, x, y, w, h]
    assert a['canvas_item_origin'] == [int((x+w/2)/3-8+0.5), int((y+h/2)/3-8+0.5)]
    assert a['inventory_index'] == (0 if i == 0 else i+8 if i < 28 else i-28)

bg = s.split('protected void renderBg(', 1)[1].split('\n   @Override', 1)[0]
assert bg.count('graphics.blit(') == 1 and 'graphics.blit(FACEPLATE, 0, 0, imageWidth, imageHeight, 0, 0,' in bg
assert bg.count('graphics.fill(') == 2
assert bg.index('if (fill > 0)') < bg.index('graphics.enableScissor(') < bg.index('graphics.fill(')
assert 'SolarV2ReferenceState.FILL_COLOR' in bg and 'SolarV2ReferenceState.HIGHLIGHT_COLOR' in bg
assert '0xFFF2A432' in state and '0xFFFFD06A' in state
assert 'finally {\n            graphics.disableScissor();' in bg
assert '(int)Math.ceil(view.screenX(SolarV2ReferenceState.METER_X))' in bg
assert '(int)Math.floor(view.screenY(SolarV2ReferenceState.METER_Y + SolarV2ReferenceState.METER_HEIGHT))' in bg
assert 'graphics.' not in s.split('protected void renderLabels(', 1)[1].split('\n   }', 1)[0]
assert '0xFF17191A' not in s  # No legacy dark-backing redraw over retained zero-charge artwork.
for locale in ('en_us', 'fr_fr'):
    lang = json.loads((r / f'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/lang/{locale}.json').read_text())
    assert 'gui.immersive_engineer_decor_controls_tool_reforged.small_solar_panel.energy_tooltip' in lang
print('PASS: exact retained PNG/manifest, uniform mapping, empty-art preservation, live FE colours, bounded screen-space scissor, backgroundless labels')

# Regression: client SimpleContainer.mayPlace is permissive. Cell acceptance must
# be checked on the authoritative integrated-server menu before the screen click.
native = (r / 'src/solarPackagedValidation/java/com/oblixorprime/iedctrtest/solar/NativeSolarV2ReferenceValidation.java').read_text()
cell_ready = native.split('case CELL_READY -> {', 1)[1].split('case CELL_PICKED -> {', 1)[0]
assert 'if(!serverMatches(mc,p->{' in cell_ready
server_contract = cell_ready.split('if(!serverMatches(mc,p->{', 1)[1].split('}))return;', 1)[0]
assert 'p.containerMenu instanceof MachineMenu' in server_contract
assert 'var serverMenu=(MachineMenu)p.containerMenu;' in server_contract
assert 'serverMenu.kind()==MachineKind.SMALL_SOLAR_PANEL' in server_contract
assert 'serverMenu.containerId==serverMenuId' in server_contract
assert 'serverMenu.slots.size()==37' in server_contract
assert 'require(!p.containerMenu.getSlot(0).mayPlace(new ItemStack(Items.STONE))' in server_contract
assert 'require(p.containerMenu.getSlot(0).mayPlace(new ItemStack(cellItem()))' in server_contract
assert 'menu.getSlot(0).mayPlace' not in native
assert cell_ready.index('}))return;') < cell_ready.index('click(screen,28)')
print('PASS: native Cell acceptance is fail-closed on the authoritative server menu before physical input')

assert 'screen.mouseClicked(' not in native and 'screen.mouseReleased(' not in native
assert 'glfwSetCursorPos' not in native
for required in ('XTestFakeMotionEvent', 'XTestFakeButtonEvent', 'mc.mouseHandler.xpos()',
                 'mc.mouseHandler.ypos()', 'ScreenEvent.Render.Post', 'parkedFrames<2',
                 'pointerParked(mc)', 'SERVER_ROUTES.add(serverRoute)', '.iedctr-solar-xvfb'):
    assert required in native, required
print('PASS: physical XTest input, observed pointer, server transitions and completed parked render gates')
