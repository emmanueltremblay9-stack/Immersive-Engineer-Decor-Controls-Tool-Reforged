package com.oblixorprime.engineersdecorreforged;

import java.util.Set;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;

public final class ReforgedConfig {
   public static final ModConfigSpec SPEC;
   public static final BooleanValue ENABLE_DECOR_BLOCKS;
   public static final BooleanValue ENABLE_UTILITY_BLOCKS;
   public static final BooleanValue ENABLE_REDSTONE_CONTROLS;
   public static final BooleanValue ENABLE_GAUGES;
   public static final BooleanValue ENABLE_INDICATORS;
   public static final BooleanValue ENABLE_SENSORS;
   public static final BooleanValue ENABLE_WIRELESS_CONTROLS;
   public static final BooleanValue ENABLE_SOUND_INDICATORS;
   public static final BooleanValue ENABLE_STYLE_INDUSTRIAL;
   public static final BooleanValue ENABLE_STYLE_RETRO_INDUSTRIAL;
   public static final BooleanValue ENABLE_STYLE_RUSTIC;
   public static final BooleanValue ENABLE_STYLE_OLD_FANCY;
   public static final BooleanValue ENABLE_STYLE_GLASS;
   private static final Set<String> UTILITY_BLOCK_ITEMS = Set.of(
      "metal_crafting_table",
      "labeled_crate",
      "factory_hopper",
      "factory_dropper",
      "factory_placer",
      "small_block_breaker",
      "small_waste_incinerator",
      "small_lab_furnace",
      "small_electrical_furnace",
      "small_mineral_smelter",
      "small_freezer",
      "fluid_barrel",
      "small_fluid_funnel",
      "passive_fluid_accumulator",
      "small_solar_panel",
      "small_milking_machine",
      "small_tree_cutter",
      "straight_pipe_valve",
      "straight_pipe_valve_redstone",
      "straight_pipe_valve_redstone_analog"
   );

   private ReforgedConfig() {
   }

   public static boolean isEnabled(BooleanValue value) {
      return !SPEC.isLoaded() || value.get();
   }

   public static boolean isUtilityBlockItem(String name) {
      return UTILITY_BLOCK_ITEMS.contains(name);
   }

   static {
      Builder builder = new Builder();
      builder.push("content");
      ENABLE_DECOR_BLOCKS = builder.comment("Shows Engineer's Decor decorative block items in the creative tab. Registry IDs remain loaded for world compatibility.")
         .define("enableDecorBlocks", true);
      ENABLE_UTILITY_BLOCKS = builder.comment("Shows utility-machine block items in the creative tab. Registry IDs, menus, and block entities remain loaded for world compatibility.")
         .define("enableUtilityBlocks", true);
      ENABLE_REDSTONE_CONTROLS = builder.comment("Shows RsGauges switch/control items in the creative tab. Registry IDs remain loaded for world compatibility.")
         .define("enableRedstoneControls", true);
      ENABLE_GAUGES = builder.comment("Shows redstone gauge items in the creative tab. Registry IDs remain loaded.").define("enableGauges", true);
      ENABLE_INDICATORS = builder.comment("Shows indicator and alarm items in the creative tab. Registry IDs remain loaded.").define("enableIndicators", true);
      ENABLE_SENSORS = builder.comment("Shows sensor items in the creative tab. Registry IDs remain loaded.").define("enableSensors", true);
      ENABLE_WIRELESS_CONTROLS = builder.comment("Shows wireless-control items in the creative tab. Registry IDs remain loaded.")
         .define("enableWirelessControls", true);
      ENABLE_SOUND_INDICATORS = builder.comment("Shows sound-indicator items in the creative tab. Registry IDs remain loaded.")
         .define("enableSoundIndicators", true);
      builder.pop();
      builder.push("styles");
      ENABLE_STYLE_INDUSTRIAL = builder.comment("Shows industrial RsGauges style items in the creative tab. Registry IDs remain loaded.")
         .define("enableStyleIndustrial", true);
      ENABLE_STYLE_RETRO_INDUSTRIAL = builder.comment("Shows retro industrial RsGauges style items in the creative tab. Registry IDs remain loaded.")
         .define("enableStyleRetroIndustrial", true);
      ENABLE_STYLE_RUSTIC = builder.comment("Shows rustic RsGauges style items in the creative tab. Registry IDs remain loaded.").define("enableStyleRustic", true);
      ENABLE_STYLE_OLD_FANCY = builder.comment("Shows old fancy RsGauges style items in the creative tab. Registry IDs remain loaded.")
         .define("enableStyleOldFancy", true);
      ENABLE_STYLE_GLASS = builder.comment("Shows glass RsGauges style items in the creative tab. Registry IDs remain loaded.").define("enableStyleGlass", true);
      builder.pop();
      SPEC = builder.build();
   }
}
