package com.oblixorprime.engineersdecorreforged.crafting;

import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.ModRecipeSerializers;
import com.oblixorprime.engineersdecorreforged.utility.LabeledCrateItemRules;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class LabeledCrateUpgradeRecipe extends CustomRecipe {
   private static final TagKey<Item> STEEL_RODS = TagKey.create(
      Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "rods/steel")
   );
   private static final TagKey<Item> STEEL_PLATES = TagKey.create(
      Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "plates/steel")
   );
   private static final ResourceLocation STANDARD_CRATE_ID = ResourceLocation.fromNamespaceAndPath(
      "immersiveengineering", "crate"
   );

   public LabeledCrateUpgradeRecipe(CraftingBookCategory category) {
      super(category);
   }

   @Override
   public boolean matches(CraftingInput input, Level level) {
      return hasRecipePattern(input)
         && LabeledCrateItemRules.canImportContents(input.getItem(4).get(DataComponents.CONTAINER));
   }

   @Override
   public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
      if (!hasRecipePattern(input)
         || !LabeledCrateItemRules.canImportContents(input.getItem(4).get(DataComponents.CONTAINER))) {
         return ItemStack.EMPTY;
      }

      ItemStack result = new ItemStack(ModBlocks.LABELED_CRATE.get());
      result.applyComponents(input.getItem(4).getComponents());
      return result;
   }

   private static boolean hasRecipePattern(CraftingInput input) {
      return input.width() == 3 && input.height() == 3 && input.ingredientCount() == 9
         && input.getItem(0).is(STEEL_RODS)
         && input.getItem(1).is(STEEL_PLATES)
         && input.getItem(2).is(STEEL_RODS)
         && input.getItem(3).is(STEEL_PLATES)
         && isStandardCrate(input.getItem(4))
         && input.getItem(5).is(STEEL_PLATES)
         && input.getItem(6).is(STEEL_RODS)
         && input.getItem(7).is(STEEL_PLATES)
         && input.getItem(8).is(STEEL_RODS);
   }

   @Override
   public NonNullList<Ingredient> getIngredients() {
      NonNullList<Ingredient> ingredients = NonNullList.withSize(9, Ingredient.EMPTY);
      ingredients.set(0, Ingredient.of(STEEL_RODS));
      ingredients.set(1, Ingredient.of(STEEL_PLATES));
      ingredients.set(2, Ingredient.of(STEEL_RODS));
      ingredients.set(3, Ingredient.of(STEEL_PLATES));
      Item standardCrate = BuiltInRegistries.ITEM.get(STANDARD_CRATE_ID);
      ingredients.set(4, Ingredient.of(standardCrate));
      ingredients.set(5, Ingredient.of(STEEL_PLATES));
      ingredients.set(6, Ingredient.of(STEEL_RODS));
      ingredients.set(7, Ingredient.of(STEEL_PLATES));
      ingredients.set(8, Ingredient.of(STEEL_RODS));
      return ingredients;
   }

   @Override
   public ItemStack getResultItem(HolderLookup.Provider registries) {
      return new ItemStack(ModBlocks.LABELED_CRATE.get());
   }

   @Override
   public boolean canCraftInDimensions(int width, int height) {
      return width >= 3 && height >= 3;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return ModRecipeSerializers.LABELED_CRATE_UPGRADE.get();
   }

   private static boolean isStandardCrate(ItemStack stack) {
      return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(STANDARD_CRATE_ID);
   }
}
