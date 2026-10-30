package com.oblixorprime.engineersdecorreforged;

import com.oblixorprime.engineersdecorreforged.recipe.MetalCraftingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipeTypes {
   public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, EngineersDecorReforged.MOD_ID);
   public static final DeferredHolder<RecipeType<?>, RecipeType<MetalCraftingRecipe>> METAL_CRAFTING = RECIPE_TYPES.register(
      "metal_crafting",
      () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(EngineersDecorReforged.MOD_ID, "metal_crafting"))
   );

   private ModRecipeTypes() {
   }
}
