package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.tools.EngineerToolsModule;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public class CampfireToolRedesignGameTests {
   @GameTest(template = "empty", timeoutTicks = 40)
   public static void sleeping_bag_accepts_all_eight_lit_campfire_neighbors(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      BlockPos rest = helper.absolutePos(new BlockPos(2, 2, 2));
      player.setPos(rest.getX() + 0.5, rest.getY(), rest.getZ() + 0.5);
      for (var fire : List.of(Blocks.CAMPFIRE)) {
         for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
               if (dx == 0 && dz == 0) continue;
               BlockPos pos = rest.offset(dx, 0, dz);
               helper.getLevel().setBlockAndUpdate(pos, fire.defaultBlockState().setValue(BlockStateProperties.LIT, true));
               ItemStack bag = new ItemStack(EngineerToolsModule.SLEEPING_BAG.get());
               player.setItemInHand(InteractionHand.MAIN_HAND, bag);
               helper.getLevel().setDayTime(13000);
               var result = bag.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
               helper.assertTrue(result.getResult().consumesAction(), "lit neighbor must allow sleep: " + pos);
               helper.assertValueEqual(0L, Math.floorMod(helper.getLevel().getDayTime(), 24000L), "sleep must advance to morning");
               helper.assertValueEqual(1, bag.getDamageValue(), "successful rest consumes one durability");
               helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void sleeping_bag_rejects_missing_extinguished_and_outside_campfires(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      BlockPos rest = helper.absolutePos(new BlockPos(2, 2, 2));
      player.setPos(rest.getX() + 0.5, rest.getY(), rest.getZ() + 0.5);
      assertDenied(helper, player);
      for (BlockPos offset : List.of(new BlockPos(0, 0, 0), new BlockPos(2, 0, 0),
            new BlockPos(2, 0, 2), new BlockPos(-2, 0, 0), new BlockPos(1, 1, 0), new BlockPos(1, -1, 0))) {
         BlockPos pos = rest.offset(offset);
         helper.getLevel().setBlockAndUpdate(pos, Blocks.CAMPFIRE.defaultBlockState());
         assertDenied(helper, player);
         helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
      }
      for (var fire : List.of(Blocks.CAMPFIRE)) {
         for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            if (dx == 0 && dz == 0) continue;
            BlockPos pos = rest.offset(dx, 0, dz);
            helper.getLevel().setBlockAndUpdate(pos, fire.defaultBlockState().setValue(BlockStateProperties.LIT, false));
            assertDenied(helper, player);
            helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
         }
      }
      helper.getLevel().setBlockAndUpdate(rest.east(), Blocks.SOUL_CAMPFIRE.defaultBlockState());
      assertDenied(helper, player);
      helper.getLevel().setBlockAndUpdate(rest.east(), Blocks.AIR.defaultBlockState());
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void sleeping_bag_lit_campfire_does_not_bypass_monster_safety(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      BlockPos rest = helper.absolutePos(new BlockPos(2, 2, 2));
      player.setPos(rest.getX() + 0.5, rest.getY(), rest.getZ() + 0.5);
      helper.getLevel().setBlockAndUpdate(rest.east(), Blocks.CAMPFIRE.defaultBlockState());
      var monster = EntityType.ZOMBIE.create(helper.getLevel());
      helper.assertTrue(monster != null, "zombie fixture must exist");
      monster.setPos(rest.getX(), rest.getY(), rest.getZ() + 2);
      helper.getLevel().addFreshEntity(monster);
      assertDenied(helper, player);
      monster.discard();
      helper.succeed();
   }

   private static void assertDenied(GameTestHelper helper, Player player) {
      ItemStack bag = new ItemStack(EngineerToolsModule.SLEEPING_BAG.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, bag);
      helper.getLevel().setDayTime(13000);
      var result = bag.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertTrue(result.getResult() == InteractionResult.FAIL, "invalid rest must fail");
      helper.assertValueEqual(13000L, helper.getLevel().getDayTime(), "failed rest preserves time");
      helper.assertValueEqual(0, bag.getDamageValue(), "failed rest preserves durability");
      helper.assertFalse(player.getCooldowns().isOnCooldown(bag.getItem()), "failed rest adds no cooldown");
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void material_box_uses_exact_hemp_rope_plate_and_treated_crate_grid(GameTestHelper helper) {
      List<ItemStack> slots = List.of(ie("wirecoil_structure_rope"), ie("plate_iron"), ie("wirecoil_structure_rope"),
            ie("plate_iron"), ie("crate"), ie("plate_iron"),
            ie("wirecoil_structure_rope"), ie("crate"), ie("wirecoil_structure_rope"));
      assertRecipe(helper, "material_box", slots, true);
      List<ItemStack> bagSlots = List.of(ie("hemp_fabric"), ie("hemp_fabric"), ie("hemp_fabric"),
            ie("hemp_fiber"), ie("hemp_fiber"), ie("hemp_fiber"),
            new ItemStack(Items.WHITE_CARPET), new ItemStack(Items.WHITE_CARPET), new ItemStack(Items.WHITE_CARPET));
      for (int i = 0; i < 9; i++) {
         var missing = new ArrayList<>(bagSlots);
         missing.set(i, ItemStack.EMPTY);
         assertRecipe(helper, "sleeping_bag", missing, false);
      }
      var wrongCrate = new ArrayList<>(slots);
      wrongCrate.set(4, new ItemStack(Items.CHEST));
      assertRecipe(helper, "material_box", wrongCrate, false);
      var swappedRows = new ArrayList<>(bagSlots);
      for (int i = 0; i < 3; i++) {
         swappedRows.set(i, bagSlots.get(i + 3));
         swappedRows.set(i + 3, bagSlots.get(i));
      }
      assertRecipe(helper, "sleeping_bag", swappedRows, false);
      for (int i = 0; i < 9; i++) {
         var missing = new ArrayList<>(slots);
         missing.set(i, ItemStack.EMPTY);
         assertRecipe(helper, "material_box", missing, false);
      }
      assertRecipe(helper, "material_box", List.of(new ItemStack(Items.STRING), new ItemStack(Items.STRING), new ItemStack(Items.STRING),
            new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT),
            new ItemStack(Items.CHEST), new ItemStack(Items.CHEST), new ItemStack(Items.IRON_INGOT)), false);
      assertRecipe(helper, "sleeping_bag", List.of(new ItemStack(Items.IRON_NUGGET), new ItemStack(Items.IRON_NUGGET), new ItemStack(Items.WHITE_CARPET),
            new ItemStack(Items.WHITE_CARPET), new ItemStack(Items.WHITE_CARPET), new ItemStack(Items.WHITE_CARPET),
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY), false);
      helper.succeed();
   }

   private static ItemStack ie(String id) {
      ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("immersiveengineering", id)));
      if (stack.isEmpty()) throw new IllegalStateException("Missing verified IE ingredient: " + id);
      return stack;
   }

   private static void assertRecipe(GameTestHelper helper, String id, List<ItemStack> slots, boolean expected) {
      var input = CraftingInput.of(3, 3, slots);
      var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
      var target = ResourceLocation.fromNamespaceAndPath("immersive_engineer_decor_controls_tool_reforged", id);
      helper.assertTrue(recipe.filter(r -> r.id().equals(target)).isPresent() == expected, "recipe match: " + id);
      if (expected) {
         ItemStack output = recipe.orElseThrow().value().assemble(input, helper.getLevel().registryAccess());
         helper.assertTrue(output.is(EngineerToolsModule.MATERIAL_BOX.get()), "recipe must assemble material box");
         helper.assertValueEqual(1, output.getCount(), "recipe must produce one box");
      }
   }
}
