package com.oblixorprime.engineersdecorreforged.tools;

import com.oblixorprime.engineersdecorreforged.ModItems;
import java.util.Set;
import net.minecraft.world.food.FoodProperties.Builder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;
import net.neoforged.neoforge.registries.DeferredItem;

public final class EngineerToolsModule {
   private static final Set<String> LEGACY_COMPATIBILITY_ITEM_NAMES = Set.of("iron_grit", "gold_grit");
   private static final Set<String> ENGINEER_TOOLS_ITEM_NAMES = Set.of(
      "redia_tool",
      "crushing_hammer",
      "tracker",
      "ariadne_coal",
      "stimpack",
      "sleeping_bag",
      "material_box",
      "diving_capsule",
      "musli_bar_press",
      "musli_bar",
      "charged_lapis_squeezer",
      "charged_lapis",
      "iron_grit",
      "gold_grit"
   );
   public static final DeferredItem<Item> REDIA_TOOL = ModItems.registerItem(
      "redia_tool", () -> new RediaToolItem(new Properties().stacksTo(1).durability(RediaToolItem.MAX_DAMAGE).rarity(Rarity.UNCOMMON))
   );
   public static final DeferredItem<Item> CRUSHING_HAMMER = ModItems.registerItem(
      "crushing_hammer", () -> new CrushingHammerItem(new Properties().stacksTo(1).durability(640).rarity(Rarity.UNCOMMON))
   );
   public static final DeferredItem<Item> TRACKER = ModItems.registerItem(
      "tracker", () -> new TrackerItem(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON))
   );
   public static final DeferredItem<Item> ARIADNE_COAL = ModItems.registerItem(
      "ariadne_coal", () -> new AriadneCoalItem(new Properties().stacksTo(1).durability(100).rarity(Rarity.UNCOMMON))
   );
   public static final DeferredItem<Item> STIMPACK = ModItems.registerItem(
      "stimpack", () -> new AutoStimPackItem(new Properties().stacksTo(1).durability(2).rarity(Rarity.UNCOMMON))
   );
   public static final DeferredItem<Item> SLEEPING_BAG = ModItems.registerItem(
      "sleeping_bag", () -> new SleepingBagItem(new Properties().stacksTo(1).durability(SleepingBagItem.MAX_DAMAGE).rarity(Rarity.UNCOMMON))
   );
   public static final DeferredItem<Item> MATERIAL_BOX = ModItems.registerItem(
      "material_box", () -> new MaterialBoxItem(new Properties().stacksTo(64).rarity(Rarity.UNCOMMON))
   );
   public static final DeferredItem<Item> DIVING_CAPSULE = ModItems.registerItem(
      "diving_capsule", () -> new DivingCapsuleItem(new Properties().stacksTo(1).durability(100).rarity(Rarity.UNCOMMON))
   );
   public static final DeferredItem<Item> MUSLI_BAR_PRESS = ModItems.registerItem(
      "musli_bar_press", () -> new MusliBarPressItem(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON))
   );
   public static final DeferredItem<Item> MUSLI_BAR = ModItems.registerItem(
      "musli_bar", () -> new TooltipItem("musli_bar", new Properties().food(new Builder().nutrition(6).saturationModifier(1.2F).fast().alwaysEdible().build()))
   );
   public static final DeferredItem<Item> CHARGED_LAPIS_SQUEEZER = ModItems.registerItem(
      "charged_lapis_squeezer", () -> new ChargedLapisSqueezerItem(new Properties().stacksTo(64).rarity(Rarity.UNCOMMON))
   );
   public static final DeferredItem<Item> CHARGED_LAPIS = ModItems.registerItem(
      "charged_lapis", () -> new ChargedLapisItem(new Properties().rarity(Rarity.UNCOMMON))
   );
   public static final DeferredItem<Item> IRON_GRIT = ModItems.registerItem("iron_grit", () -> new TooltipItem("iron_grit", new Properties()));
   public static final DeferredItem<Item> GOLD_GRIT = ModItems.registerItem("gold_grit", () -> new TooltipItem("gold_grit", new Properties()));

   private EngineerToolsModule() {
   }

   public static boolean isLegacyCompatibilityItem(String name) {
      return LEGACY_COMPATIBILITY_ITEM_NAMES.contains(name);
   }

   public static boolean isEngineerToolsItem(String name) {
      return ENGINEER_TOOLS_ITEM_NAMES.contains(name);
   }

   public static void init() {
   }
}
