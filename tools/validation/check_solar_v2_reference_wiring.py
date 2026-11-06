"""Fail-closed source/art contract guardrails. Not a replacement for mapped/native tests."""
from pathlib import Path
import hashlib
import json
import re
import struct

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
assert 'this(kind, id, inventory, container, data, false)' in m
assert 'this(kind, id, inventory, container, data, barrelReferenceRequested, false)' in m
assert 'kind == MachineKind.SMALL_SOLAR_PANEL && inventory.player.level().isClientSide' in m
assert 'kind == MachineKind.SMALL_SOLAR_PANEL && Boolean.getBoolean(DEV_SOLAR_V2_REFERENCE_PROPERTY)' in m
assert 'DEV_SOLAR_V2_REFERENCE_PROPERTY = "iedctr.dev.solarV2Reference"' in m
assert '? new SolarV2ReferenceScreen(menu, inventory, title) : new MachineScreen.SmallSolarPanelScreen(menu, inventory, title)' in c
assert 'SolarV2ReferenceGeometry.anchor(slot.containerIndex())' in m
assert 'SolarV2ReferenceGeometry.anchor(1 + row * 9 + column)' in m
assert 'SolarV2ReferenceGeometry.anchor(28 + column)' in m
# All downstream menu data/transfer/validation logic remains byte-identical to PR151/PR166.
assert hashlib.sha256(m.split('   private static MenuType<?> resolveMenuType', 1)[1].encode()).hexdigest() == '48406a039c8b1ab4e78c9cd8ac4d3bb76408838dfee1696047d915f27cdbe88a'
# Preserve the historical Solar baseline. Only this explicitly reviewed Barrel
# follow-up may replace listed hashes; artwork, renderer, mechanics and workflow stay frozen.
preserved_path = d / 'preserved-baseline-sha256.json'
preserved = json.loads(preserved_path.read_text())
followup_path = r / 'docs/testing/barrel-reference-v5/reload-bounds-followup-sha256.json'
assert hashlib.sha256(followup_path.read_bytes()).hexdigest() == '97cf2df916113fa6e17389e0c3cf44d232270f93509500b74cf5ac19cd447b59'
followup = json.loads(followup_path.read_text())
assert followup['baseline_manifest_sha256'] == hashlib.sha256(preserved_path.read_bytes()).hexdigest()
allowed_followup_paths = {
    'docs/testing/barrel-reference-v5/README.md',
    'src/main/java/com/oblixorprime/engineersdecorreforged/client/BarrelReferenceScreen.java',
    'src/main/java/com/oblixorprime/engineersdecorreforged/client/validation/NativeBarrelReferenceValidation.java',
    'src/main/java/com/oblixorprime/engineersdecorreforged/utility/BarrelReferenceGeometry.java',
    'src/test/java/com/oblixorprime/engineersdecorreforged/utility/BarrelReferenceChecks.java',
    'tools/validation/barrel_reference_native.py',
    'tools/validation/check_barrel_reference_wiring.py',
    'tools/validation/test_barrel_reference_native.py',
}
assert set(followup['files']) == allowed_followup_paths
for path, expected in preserved['files'].items():
    if path in allowed_followup_paths:
        change = followup['files'][path]
        assert set(change) == {'before_sha256', 'after_sha256'}
        assert change['before_sha256'] == expected and change['after_sha256'] != expected, path
        expected = change['after_sha256']
    assert hashlib.sha256((r / path).read_bytes()).hexdigest() == expected, path
print('PASS: independent default-OFF gates, Solar routing and legacy preservation; exact authorized Barrel reload/bounds follow-up')

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
native = (j / 'client/validation/NativeSolarV2ReferenceValidation.java').read_text()
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
print('PASS: native Cell acceptance is fail-closed on the authoritative server menu before handler-driven input')
