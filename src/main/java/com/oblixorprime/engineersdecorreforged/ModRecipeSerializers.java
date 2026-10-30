package com.oblixorprime.engineersdecorreforged;

import com.oblixorprime.engineersdecorreforged.tools.RediaToolRepairRecipe;
import com.oblixorprime.engineersdecorreforged.recipe.MetalCraftingRecipe;
import com.oblixorprime.engineersdecorreforged.recipe.CrushingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipeSerializers {
   public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(
      Registries.RECIPE_SERIALIZER, EngineersDecorReforged.MOD_ID
   );
   public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<RediaToolRepairRecipe>> REDIA_TOOL_REPAIR = RECIPE_SERIALIZERS.register(
      "redia_tool_repair", () -> new SimpleCraftingRecipeSerializer<>(RediaToolRepairRecipe::new)
   );
   public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MetalCraftingRecipe>> METAL_CRAFTING = RECIPE_SERIALIZERS.register(
      "metal_crafting", MetalCraftingRecipe.Serializer::new
   );
   public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CrushingRecipe>> CRUSHING = RECIPE_SERIALIZERS.register(
      "crushing", CrushingRecipe.Serializer::new
   );

   private ModRecipeSerializers() {
   }
}
