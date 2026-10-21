package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.util.ArrayList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tests the actual prototype policy without constructing unregistered items after registry freeze. */
@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class MusleeCapturePrototypeGameTests {
    private MusleeCapturePrototypeGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void capture_prototype_policy_refuses_mutable_defaults(GameTestHelper helper) {
        CompoundTag tag = new CompoundTag(); tag.putInt("value", 1);
        DataComponentMap customData = DataComponentMap.builder()
                .set(DataComponents.CUSTOM_DATA, CustomData.of(tag)).build();
        helper.assertTrue(rejected(customData), "mutable custom data default rejected by actual policy");
        DataComponentMap mutableLore = DataComponentMap.builder()
                .set(DataComponents.LORE, new ItemLore(new ArrayList<>())).build();
        helper.assertTrue(rejected(mutableLore), "empty but caller-mutable lore default rejected");
        MusleeItemStackCapture.requireSafePrototype(Items.BREAD.components());
        helper.succeed();
    }

    private static boolean rejected(DataComponentMap prototype) {
        try { MusleeItemStackCapture.requireSafePrototype(prototype); return false; }
        catch (IllegalArgumentException expected) { return true; }
    }
}
