package com.oblixorprime.engineersdecorreforged.tools;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.event.LootTableLoadEvent;

public final class EngineerToolsLoot {
   public static final String POOL_NAME = EngineersDecorReforged.MOD_ID + "_additional_loot";
   private static final Set<ResourceLocation> TARGET_TABLES = Set.of(
      minecraftChest("abandoned_mineshaft"),
      minecraftChest("buried_treasure"),
      minecraftChest("desert_pyramid"),
      minecraftChest("pillager_outpost"),
      minecraftChest("simple_dungeon"),
      minecraftChest("stronghold_crossing"),
      minecraftChest("village/village_armorer"),
      minecraftChest("village/village_cartographer"),
      minecraftChest("village/village_desert_house"),
      minecraftChest("village/village_plains_house"),
      minecraftChest("village/village_savanna_house"),
      minecraftChest("village/village_snowy_house"),
      minecraftChest("village/village_taiga_house"),
      minecraftChest("village/village_temple"),
      minecraftChest("village/village_toolsmith"),
      minecraftChest("village/village_weaponsmith")
   );

   private EngineerToolsLoot() {
   }

   public static void onLootTableLoad(LootTableLoadEvent event) {
      injectAdditionalLoot(event.getTable(), event.getName(), EngineerToolsConfig.withChestLoot());
   }

   public static boolean injectAdditionalLoot(LootTable table, ResourceLocation target, boolean enabled) {
      if (!enabled || !TARGET_TABLES.contains(target) || table.getPool(POOL_NAME) != null) {
         return false;
      }
      ResourceKey<LootTable> additionalTable = ResourceKey.create(
         Registries.LOOT_TABLE,
         ResourceLocation.fromNamespaceAndPath(EngineersDecorReforged.MOD_ID, "additional/" + target.getNamespace() + "/" + target.getPath())
      );
      table.addPool(
         LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1.0F))
            .setBonusRolls(UniformGenerator.between(0.0F, 1.0F))
            .add(NestedLootTable.lootTableReference(additionalTable))
            .name(POOL_NAME)
            .build()
      );
      return true;
   }

   public static int targetCount() {
      return TARGET_TABLES.size();
   }

   private static ResourceLocation minecraftChest(String path) {
      return ResourceLocation.withDefaultNamespace("chests/" + path);
   }
}
