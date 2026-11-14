"""Conservative source contract checks; NOT mapped compilation or execution."""
from pathlib import Path
import json
import re

root = Path(__file__).resolve().parents[2]
base = root / 'src/main/java/com/oblixorprime/engineersdecorreforged'
screen = (base / 'client/FreezerReferenceScreen.java').read_text()
menu = (base / 'menu/MachineMenu.java').read_text()
entity = (base / 'utility/MachineBlockEntity.java').read_text()
model = (base / 'utility/FreezerReferenceLiveState.java').read_text()
gradle = (root / 'build.gradle').read_text()
checks = {
    'separate_default_off_client_property': "gradleProperty('dev_freezer_v5_live_energy').map { it.toBoolean() }.getOrElse(false)" in gradle,
    'new_property_in_client_run_only': gradle.index("systemProperty 'iedctr.dev.freezerV5LiveEnergy'") < gradle.index('        server {'),
    'geometry_normal_freezer': 'kind == MachineKind.SMALL_FREEZER,\n         kind == MachineKind.FLUID_BARREL' in menu,
    'geometry_still_client_only': '&& kind == MachineKind.SMALL_FREEZER && inventory.player.level().isClientSide' in menu,
    'raw_energy_sync': 'case SMALL_FREEZER -> this.data.get(FREEZER_ENERGY_FIELD);' in menu,
    'raw_energy_field3': 'private static final int FREEZER_ENERGY_FIELD = 3;' in menu,
    'capacity16000': 'COMPACT_MACHINE_ENERGY_CAPACITY = 16000;' in menu and 'case SMALL_WASTE_INCINERATOR, SMALL_FREEZER -> COMPACT_MACHINE_ENERGY_CAPACITY;' in menu,
    'progress_total180': 'FREEZER_PROCESS_TICKS = 180;' in entity,
    'state_uses_real_fe_getters': 'FreezerReferenceLiveState.energy(menu.energyStored(), menu.energyCapacity())' in screen,
    'state_uses_real_frost_getters': 'FreezerReferenceLiveState.frost(menu.freezerProgressTicks(), MachineBlockEntity.FREEZER_PROCESS_TICKS,\n         menu.freezerPhase(), menu.freezerStatus(), menu.freezerRedstonePowered())' in screen,
    'renderer_uses_tested_mode_and_drawplan': 'FreezerReferenceLiveState.energyPatches(previewMode, liveEnergy())' in screen,
    'one_source_transform_not_rounded_canvas_cells': 'graphics.pose().scale((float)imageWidth / FreezerReferenceGeometry.SOURCE_WIDTH,' in screen and 'graphics.blit(FACEPLATE, destination.x(), destination.y(), destination.width(), destination.height(),' in screen,
    'source_uvs_use_original_texture': 'source.x(), source.y(), source.width(), source.height(),' in screen,
    'no_clock_or_tick_simulation_in_model': not re.search(r'System\.(?:nanoTime|currentTimeMillis)|getGameTime|partialTick|\bTimer\b', model),
    'no_stale_100FE_granularity': 'energyDisplayGranularity' not in screen and 'energyDisplayGranularity' not in model,
    'frost_uses_server_phase': 'FROST_LAYERS.get(liveFrost().phase())' in screen,
    'snapshot_keeps_original': 'dynamicFrost ? CLEAN_CHAMBER : FACEPLATE' in screen,
    'no_new_packets': 'sendToServer' not in screen and 'PacketDistributor' not in screen,
    'no_added_title_or_cover': 'graphics.drawString' not in screen,
    'vanilla_recovery_unchanged': 'slotClicked(menu.getSlot(1), 1, 0, ClickType.QUICK_MOVE);' in screen,
}
assert all(checks.values()), {key: value for key, value in checks.items() if not value}
print(json.dumps({'status': 'PASS_SOURCE_CONTRACT_ONLY_NOT_RUNTIME', 'checks': checks}, indent=2))
