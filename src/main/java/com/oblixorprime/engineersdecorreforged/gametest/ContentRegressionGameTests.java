package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.ModItems;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ContentRegressionGameTests {
   private static final Set<String> WOOD_BLOCKS = Set.of(
      "old_industrial_wood_planks",
      "old_industrial_wood_slab",
      "old_industrial_wood_stairs",
      "old_industrial_wood_slabslice",
      "old_industrial_wood_door",
      "treated_wood_pole",
      "treated_wood_pole_head",
      "treated_wood_pole_support",
      "treated_wood_table",
      "treated_wood_stool",
      "labeled_crate"
   );
   private static final Set<String> EARTH_BLOCKS = Set.of("dense_grit_dirt_block", "dense_grit_sand_block");
   private static final Set<SoundType> EXPLICIT_SOUND_TYPES = Set.of(
      SoundType.METAL, SoundType.STONE, SoundType.WOOD, SoundType.GLASS, SoundType.GRAVEL
   );

   private ContentRegressionGameTests() {
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void every_registered_item_is_routed_to_the_main_creative_tab(GameTestHelper helper) {
      Set<Item> registeredItems = new HashSet<>();
      BuiltInRegistries.ITEM.entrySet().stream()
         .filter(entry -> entry.getKey().location().getNamespace().equals(EngineersDecorReforged.MOD_ID))
         .map(entry -> entry.getValue())
         .forEach(registeredItems::add);
      List<? extends Item> orderedItems = ModItems.ORDERED_ITEMS.stream().map(item -> item.get()).toList();
      Set<? extends Item> distinctOrderedItems = new HashSet<>(orderedItems);
      helper.assertValueEqual(orderedItems.size(), distinctOrderedItems.size(), "creative-tab routing should not contain duplicate items");
      helper.assertTrue(
         registeredItems.equals(distinctOrderedItems), "every current-namespace item should have exactly one creative-tab route"
      );
      ModItems.ORDERED_ITEMS.forEach(
         item -> helper.assertTrue(
            EngineersDecorReforged.shouldShowInCreativeTab(item), "default configuration should show creative item " + item.getId()
         )
      );
      helper.assertTrue(EngineersDecorReforged.MAIN_TAB.get() != null, "main creative tab should be registered");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void every_public_block_has_one_conventional_mining_tool(GameTestHelper helper) {
      for (var holder : ModBlocks.ORDERED_BLOCKS) {
         Block block = holder.get();
         if (block.asItem() == Items.AIR) {
            continue;
         }

         ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
         String path = id.getPath();
         BlockState state = block.defaultBlockState();
         boolean pickaxe = state.is(BlockTags.MINEABLE_WITH_PICKAXE);
         boolean axe = state.is(BlockTags.MINEABLE_WITH_AXE);
         boolean shovel = state.is(BlockTags.MINEABLE_WITH_SHOVEL);
         int categories = (pickaxe ? 1 : 0) + (axe ? 1 : 0) + (shovel ? 1 : 0);
         helper.assertValueEqual(categories, 1, "public block should have exactly one primary mining tool: " + id);

         ItemStack expectedTool;
         if (WOOD_BLOCKS.contains(path)) {
            helper.assertTrue(axe, "wood block should be mineable with an axe: " + id);
            expectedTool = new ItemStack(Items.IRON_AXE);
         } else if (EARTH_BLOCKS.contains(path)) {
            helper.assertTrue(shovel, "earth block should be mineable with a shovel: " + id);
            expectedTool = new ItemStack(Items.IRON_SHOVEL);
         } else {
            helper.assertTrue(pickaxe, "stone, glass, metal, and control blocks should be mineable with a pickaxe: " + id);
            expectedTool = new ItemStack(Items.IRON_PICKAXE);
         }

         helper.assertTrue(expectedTool.isCorrectToolForDrops(state), "vanilla tool should be correct for drops: " + id);
      }

      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void registered_blocks_and_accessways_use_material_appropriate_sounds(GameTestHelper helper) {
      for (var holder : ModBlocks.ORDERED_BLOCKS) {
         Block block = holder.get();
         if (block.asItem() != Items.AIR) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            SoundType soundType = block.defaultBlockState().getSoundType();
            helper.assertTrue(EXPLICIT_SOUND_TYPES.contains(soundType), "block should use an explicit material sound type: " + id);
         }
      }

      DoorBlock metalDoor = (DoorBlock)ModBlocks.METAL_SLIDING_DOOR.get();
      helper.assertTrue(metalDoor.type() == BlockSetType.IRON, "metal sliding door should use the iron door sound family");
      helper.assertTrue(metalDoor.type().doorOpen() == SoundEvents.IRON_DOOR_OPEN, "metal sliding door should use the iron opening sound");
      helper.assertTrue(metalDoor.type().doorClose() == SoundEvents.IRON_DOOR_CLOSE, "metal sliding door should use the iron closing sound");

      PortedBlocks.HatchBlock hatch = ModBlocks.IRON_HATCH.get();
      helper.assertTrue(hatch.type() == BlockSetType.IRON, "iron hatch should use the iron trapdoor sound family");
      helper.assertTrue(hatch.type().trapdoorOpen() == SoundEvents.IRON_TRAPDOOR_OPEN, "iron hatch should use the iron opening sound");
      helper.assertTrue(hatch.type().trapdoorClose() == SoundEvents.IRON_TRAPDOOR_CLOSE, "iron hatch should use the iron closing sound");

      DoorBlock woodDoor = (DoorBlock)ModBlocks.OLD_INDUSTRIAL_WOOD_DOOR.get();
      helper.assertTrue(woodDoor.type() == BlockSetType.OAK, "old industrial wood door should retain the oak sound family");
      helper.succeed();
   }
}
