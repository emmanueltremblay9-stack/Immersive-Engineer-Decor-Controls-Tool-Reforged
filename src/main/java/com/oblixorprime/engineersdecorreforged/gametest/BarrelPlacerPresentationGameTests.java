package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Presentation flags must never change authoritative server menu coordinates or indices. */
@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class BarrelPlacerPresentationGameTests {
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void barrel_and_placer_client_projections_remain_server_isolated(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        String barrelKey = MachineMenu.DEV_BARREL_REFERENCE_PROPERTY;
        String placerKey = MachineMenu.DEV_FACTORY_PLACER_V2_REFERENCE_PROPERTY;
        String oldBarrel = System.getProperty(barrelKey), oldPlacer = System.getProperty(placerKey);
        try {
            for (boolean barrel : new boolean[] {false, true}) {
                for (boolean placer : new boolean[] {false, true}) {
                    System.setProperty(barrelKey, Boolean.toString(barrel));
                    System.setProperty(placerKey, Boolean.toString(placer));
                    for (var kind : new MachineKind[] {MachineKind.FLUID_BARREL, MachineKind.FACTORY_PLACER}) {
                        var authoritative = new MachineMenu(kind, 41, player.getInventory(),
                            new SimpleContainer(27), new SimpleContainerData(MachineMenu.DATA_COUNT));
                        var factory = MachineMenu.client(42, player.getInventory(), kind);
                        for (var menu : new MachineMenu[] {authoritative, factory}) {
                            helper.assertTrue(!menu.barrelReferencePresentation(), "Server must not project Barrel V5");
                            helper.assertTrue(!menu.factoryPlacerV2ReferencePresentation(), "Server must not project Placer V2");
                            int machines = kind == MachineKind.FLUID_BARREL ? 2 : 18;
                            helper.assertValueEqual(machines + 36, menu.slots.size(), "Protocol slot count");
                            for (int i = 0; i < menu.slots.size(); i++) {
                                var expected = authoritative.getSlot(i);
                                var actual = menu.getSlot(i);
                                helper.assertValueEqual(expected.x, actual.x, "Server slot x " + i);
                                helper.assertValueEqual(expected.y, actual.y, "Server slot y " + i);
                                helper.assertValueEqual(expected.getContainerSlot(), actual.getContainerSlot(), "Backing slot " + i);
                            }
                            menu.removed(player);
                        }
                    }
                }
            }
        } finally {
            if (oldBarrel == null) System.clearProperty(barrelKey); else System.setProperty(barrelKey, oldBarrel);
            if (oldPlacer == null) System.clearProperty(placerKey); else System.setProperty(placerKey, oldPlacer);
        }
        helper.succeed();
    }
}
