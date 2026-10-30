package com.oblixorprime.engineersdecorreforged.compat.jei;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.ModRecipeTypes;
import com.oblixorprime.engineersdecorreforged.recipe.MetalCraftingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

public final class MetalCraftingJeiCategory implements IRecipeCategory<RecipeHolder<MetalCraftingRecipe>> {
   public static final RecipeType<RecipeHolder<MetalCraftingRecipe>> TYPE = RecipeType.createRecipeHolderType(
      ModRecipeTypes.METAL_CRAFTING.getId()
   );
   private final IDrawable icon;
   private final IDrawable arrow;
   private final IDrawable plus;

   public MetalCraftingJeiCategory(IGuiHelper guiHelper) {
      this.icon = guiHelper.createDrawableItemLike(ModBlocks.METAL_CRAFTING_TABLE.get());
      this.arrow = guiHelper.getRecipeArrow();
      this.plus = guiHelper.getRecipePlusSign();
   }

   @Override
   public RecipeType<RecipeHolder<MetalCraftingRecipe>> getRecipeType() {
      return TYPE;
   }

   @Override
   public Component getTitle() {
      return Component.translatable("jei.category." + EngineersDecorReforged.MOD_ID + ".metal_crafting");
   }

   @Override
   public int getWidth() {
      return 132;
   }

   @Override
   public int getHeight() {
      return 40;
   }

   @Override
   public IDrawable getIcon() {
      return this.icon;
   }

   @Override
   public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MetalCraftingRecipe> holder, IFocusGroup focuses) {
      MetalCraftingRecipe recipe = holder.value();
      builder.addInputSlot(1, 12).setStandardSlotBackground().addIngredients(recipe.tool());
      builder.addInputSlot(39, 12).setStandardSlotBackground().addIngredients(recipe.material());
      builder.addOutputSlot(111, 12).setOutputSlotBackground().addItemStack(recipe.result());
   }

   @Override
   public void draw(RecipeHolder<MetalCraftingRecipe> recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
      this.plus.draw(graphics, 21, 13);
      this.arrow.draw(graphics, 66, 12);
   }

   @Override
   public ResourceLocation getRegistryName(RecipeHolder<MetalCraftingRecipe> recipe) {
      return recipe.id();
   }
}
