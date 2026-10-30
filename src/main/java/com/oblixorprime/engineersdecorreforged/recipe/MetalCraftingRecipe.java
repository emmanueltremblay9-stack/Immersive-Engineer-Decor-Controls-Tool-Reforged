package com.oblixorprime.engineersdecorreforged.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.ModRecipeSerializers;
import com.oblixorprime.engineersdecorreforged.ModRecipeTypes;
import com.oblixorprime.engineersdecorreforged.tools.EngineerToolsModule;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public final class MetalCraftingRecipe implements CraftingRecipe {
   private static final TagKey<Item> IE_HAMMERS = TagKey.create(
      Registries.ITEM, ResourceLocation.fromNamespaceAndPath("immersiveengineering", "tools/hammers")
   );
   private static final TagKey<Item> IE_WIRECUTTERS = TagKey.create(
      Registries.ITEM, ResourceLocation.fromNamespaceAndPath("immersiveengineering", "tools/wirecutters")
   );
   private final Ingredient tool;
   private final Ingredient material;
   private final ItemStack result;
   private final int toolDamage;

   public MetalCraftingRecipe(Ingredient tool, Ingredient material, ItemStack result, int toolDamage) {
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

   public ItemStack result() {
      return this.result.copy();
   }

   public int toolDamage() {
      return this.toolDamage;
   }

   public static boolean isTableTool(ItemStack stack) {
      return !stack.isEmpty()
         && (stack.is(IE_HAMMERS) || stack.is(IE_WIRECUTTERS) || stack.is(EngineerToolsModule.CRUSHING_HAMMER.get()));
   }

   public static boolean isPlateHammer(ItemStack stack) {
      return !stack.isEmpty() && stack.is(IE_HAMMERS);
   }

   @Override
   public boolean matches(CraftingInput input, Level level) {
      return input.size() == 2 && this.tool.test(input.getItem(0)) && this.material.test(input.getItem(1));
   }

   @Override
   public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
      return this.result.copy();
   }

   @Override
   public boolean canCraftInDimensions(int width, int height) {
      return width * height >= 2;
   }

   @Override
   public CraftingBookCategory category() {
      return CraftingBookCategory.MISC;
   }

   @Override
   public ItemStack getResultItem(HolderLookup.Provider registries) {
      return this.result;
   }

   @Override
   public NonNullList<Ingredient> getIngredients() {
      NonNullList<Ingredient> ingredients = NonNullList.create();
      ingredients.add(this.tool);
      ingredients.add(this.material);
      return ingredients;
   }

   @Override
   public ItemStack getToastSymbol() {
      return new ItemStack(ModBlocks.METAL_CRAFTING_TABLE.get());
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return ModRecipeSerializers.METAL_CRAFTING.get();
   }

   @Override
   public RecipeType<?> getType() {
      return ModRecipeTypes.METAL_CRAFTING.get();
   }

   public static final class Serializer implements RecipeSerializer<MetalCraftingRecipe> {
      private static final MapCodec<MetalCraftingRecipe> CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(
               Ingredient.CODEC_NONEMPTY.fieldOf("tool").forGetter(MetalCraftingRecipe::tool),
               Ingredient.CODEC_NONEMPTY.fieldOf("material").forGetter(MetalCraftingRecipe::material),
               ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
               com.mojang.serialization.Codec.intRange(-1, 640).optionalFieldOf("tool_damage", -1).forGetter(MetalCraftingRecipe::toolDamage)
            )
            .apply(instance, MetalCraftingRecipe::new)
      );
      private static final StreamCodec<RegistryFriendlyByteBuf, MetalCraftingRecipe> STREAM_CODEC = StreamCodec.composite(
         Ingredient.CONTENTS_STREAM_CODEC,
         MetalCraftingRecipe::tool,
         Ingredient.CONTENTS_STREAM_CODEC,
         MetalCraftingRecipe::material,
         ItemStack.STREAM_CODEC,
         recipe -> recipe.result,
         ByteBufCodecs.VAR_INT,
         MetalCraftingRecipe::toolDamage,
         MetalCraftingRecipe::new
      );

      @Override
      public MapCodec<MetalCraftingRecipe> codec() {
         return CODEC;
      }

      @Override
      public StreamCodec<RegistryFriendlyByteBuf, MetalCraftingRecipe> streamCodec() {
         return STREAM_CODEC;
      }
   }
}
