package com.oblixorprime.engineersdecorreforged.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.oblixorprime.engineersdecorreforged.ModRecipeSerializers;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class CrushingRecipe extends CustomRecipe {
   private final String group;
   private final Ingredient tool;
   private final Ingredient material;
   private final ItemStack result;
   private final int toolDamage;

   public CrushingRecipe(String group, Ingredient tool, Ingredient material, ItemStack result, int toolDamage) {
      super(CraftingBookCategory.MISC);
      this.group = group;
      this.tool = tool;
      this.material = material;
      this.result = result;
      this.toolDamage = toolDamage;
   }

   public Ingredient tool() {
      return this.tool;
   }

   public Ingredient material() {
      return this.material;
   }

   public int toolDamage() {
      return this.toolDamage;
   }

   @Override
   public String getGroup() {
      return this.group;
   }

   @Override
   public boolean matches(CraftingInput input, Level level) {
      return findInputs(input) != null;
   }

   @Override
   public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
      return findInputs(input) == null ? ItemStack.EMPTY : this.result.copy();
   }

   @Override
   public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
      NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
      for (int slot = 0; slot < input.size(); slot++) {
         ItemStack stack = input.getItem(slot);
         if (this.tool.test(stack)) {
            remaining.set(slot, damageTool(stack, this.toolDamage));
         } else if (stack.hasCraftingRemainingItem()) {
            remaining.set(slot, stack.getCraftingRemainingItem());
         }
      }
      return remaining;
   }

   @Override
   public boolean canCraftInDimensions(int width, int height) {
      return width * height >= 2;
   }

   @Override
   public ItemStack getResultItem(HolderLookup.Provider registries) {
      return this.result;
   }

   @Override
   public boolean isSpecial() {
      return false;
   }

   @Override
   public NonNullList<Ingredient> getIngredients() {
      NonNullList<Ingredient> ingredients = NonNullList.create();
      ingredients.add(this.material);
      ingredients.add(this.tool);
      return ingredients;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return ModRecipeSerializers.CRUSHING.get();
   }

   private List<ItemStack> findInputs(CraftingInput input) {
      if (input.ingredientCount() != 2) {
         return null;
      }
      List<ItemStack> tools = new ArrayList<>(1);
      List<ItemStack> materials = new ArrayList<>(1);
      for (int slot = 0; slot < input.size(); slot++) {
         ItemStack stack = input.getItem(slot);
         if (stack.isEmpty()) {
            continue;
         }
         if (this.tool.test(stack)) {
            tools.add(stack);
         } else if (this.material.test(stack)) {
            materials.add(stack);
         } else {
            return null;
         }
      }
      return tools.size() == 1 && materials.size() == 1 ? List.of(tools.get(0), materials.get(0)) : null;
   }

   private static ItemStack damageTool(ItemStack tool, int damage) {
      ItemStack remaining = tool.copyWithCount(1);
      int nextDamage = remaining.getDamageValue() + damage;
      if (nextDamage >= remaining.getMaxDamage()) {
         return ItemStack.EMPTY;
      }
      remaining.setDamageValue(nextDamage);
      return remaining;
   }

   public static final class Serializer implements RecipeSerializer<CrushingRecipe> {
      private static final MapCodec<CrushingRecipe> CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(
               Codec.STRING.optionalFieldOf("group", "").forGetter(CrushingRecipe::getGroup),
               Ingredient.CODEC_NONEMPTY.fieldOf("tool").forGetter(CrushingRecipe::tool),
               Ingredient.CODEC_NONEMPTY.fieldOf("material").forGetter(CrushingRecipe::material),
               ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
               com.mojang.serialization.Codec.intRange(1, 640).fieldOf("tool_damage").forGetter(CrushingRecipe::toolDamage)
            )
            .apply(instance, CrushingRecipe::new)
      );
      private static final StreamCodec<RegistryFriendlyByteBuf, CrushingRecipe> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.STRING_UTF8,
         CrushingRecipe::getGroup,
         Ingredient.CONTENTS_STREAM_CODEC,
         CrushingRecipe::tool,
         Ingredient.CONTENTS_STREAM_CODEC,
         CrushingRecipe::material,
         ItemStack.STREAM_CODEC,
         recipe -> recipe.result,
         ByteBufCodecs.VAR_INT,
         CrushingRecipe::toolDamage,
         CrushingRecipe::new
      );

      @Override
      public MapCodec<CrushingRecipe> codec() {
         return CODEC;
      }

      @Override
      public StreamCodec<RegistryFriendlyByteBuf, CrushingRecipe> streamCodec() {
         return STREAM_CODEC;
      }
   }
}
