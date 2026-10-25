package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AccumulatorRecipeGameTests {
   private static final ResourceLocation RECIPE = ResourceLocation.fromNamespaceAndPath(
      EngineersDecorReforged.MOD_ID, "passive_fluid_accumulator");

   private AccumulatorRecipeGameTests() {}

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void accumulator_recipe_crafts_one_with_sponge_piston_and_vanilla_mirror(GameTestHelper h) {
      for (boolean mirrored : new boolean[] {false, true}) {
         var input = CraftingInput.of(3, 3, grid(Items.SPONGE, Items.PISTON, mirrored));
         var selected = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel());
         h.assertTrue(selected.isPresent(), "sponge/piston grid must match; mirrored=" + mirrored);
         h.assertValueEqual(selected.orElseThrow().id(), RECIPE, "recipe manager selects the existing accumulator recipe ID");
         var output = selected.orElseThrow().value().assemble(input, h.getLevel().registryAccess());
         h.assertTrue(output.is(ModBlocks.PASSIVE_FLUID_ACCUMULATOR.get().asItem()), "crafting produces the registered accumulator");
         h.assertValueEqual(output.getCount(), 1, "recipe produces exactly one accumulator");
         System.out.println("ACCUMULATOR_RECIPE_MATCH mirrored=" + mirrored + " output=passive_fluid_accumulator count=" + output.getCount());
      }
      h.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void accumulator_recipe_rejects_buckets_wet_sponge_and_sticky_piston(GameTestHelper h) {
      var recipe = loadedRecipe(h);
      Item[][] rejected = {
         {Items.BUCKET, Items.BUCKET},
         {Items.WET_SPONGE, Items.PISTON},
         {Items.SPONGE, Items.STICKY_PISTON},
         {Items.BUCKET, Items.PISTON},
         {Items.SPONGE, Items.BUCKET}
      };
      int cases = 0;
      for (Item[] middle : rejected) {
         for (boolean mirrored : new boolean[] {false, true}) {
            assertRejected(h, recipe, grid(middle[0], middle[1], mirrored),
               "invalid middle ingredients " + middle[0] + "/" + middle[1] + "; mirrored=" + mirrored);
            cases++;
         }
      }
      System.out.println("ACCUMULATOR_RECIPE_REJECT substitutions=" + cases);
      h.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void accumulator_recipe_requires_all_nine_slots_in_both_layouts(GameTestHelper h) {
      var recipe = loadedRecipe(h);
      int cases = 0;
      for (boolean mirrored : new boolean[] {false, true}) {
         for (int slot = 0; slot < 9; slot++) {
            var missing = grid(Items.SPONGE, Items.PISTON, mirrored);
            missing.set(slot, ItemStack.EMPTY);
            assertRejected(h, recipe, missing, "missing slot " + slot + "; mirrored=" + mirrored);
            cases++;
         }
      }
      System.out.println("ACCUMULATOR_RECIPE_REJECT missing_slots=" + cases);
      h.succeed();
   }

   private static ShapedRecipe loadedRecipe(GameTestHelper h) {
      var holder = h.getLevel().getRecipeManager().byKey(RECIPE);
      h.assertTrue(holder.isPresent() && holder.orElseThrow().value() instanceof ShapedRecipe,
         "existing recipe ID must load as a vanilla shaped crafting recipe");
      return (ShapedRecipe) holder.orElseThrow().value();
   }

   private static void assertRejected(GameTestHelper h, ShapedRecipe recipe, List<ItemStack> slots, String reason) {
      var input = CraftingInput.of(3, 3, slots);
      h.assertFalse(recipe.matches(input, h.getLevel()), reason);
      var selected = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel());
      h.assertFalse(selected.filter(candidate -> candidate.id().equals(RECIPE)).isPresent(),
         "recipe manager must not select accumulator: " + reason);
   }

   private static ArrayList<ItemStack> grid(Item left, Item right, boolean mirrored) {
      return new ArrayList<>(List.of(
         new ItemStack(Items.COPPER_INGOT), new ItemStack(ModBlocks.SMALL_FLUID_FUNNEL.get()), new ItemStack(Items.COPPER_INGOT),
         new ItemStack(mirrored ? right : left), new ItemStack(Items.COPPER_INGOT), new ItemStack(mirrored ? left : right),
         new ItemStack(Items.COPPER_INGOT), new ItemStack(ModBlocks.SMALL_FLUID_FUNNEL.get()), new ItemStack(Items.COPPER_INGOT)
      ));
   }
}
