package com.oblixorprime.engineersdecorreforged.tools;

import com.oblixorprime.engineersdecorreforged.ModRecipeSerializers;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;

public final class RediaToolRepairRecipe extends CustomRecipe {
   private static final int REPAIR_PERCENT = 65;

   public RediaToolRepairRecipe(CraftingBookCategory category) {
      super(category);
   }

   @Override
   public boolean matches(CraftingInput input, Level level) {
      return findToolAndDiamond(input) != null;
   }

   @Override
   public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
      List<ItemStack> inputs = findToolAndDiamond(input);
      if (inputs == null) {
         return ItemStack.EMPTY;
      }

      ItemStack result = inputs.get(0).copyWithCount(1);
      int previousDamage = result.getDamageValue();
      int repairAmount = Math.max(1, result.getMaxDamage() * REPAIR_PERCENT / 100);
      result.setDamageValue(Math.max(previousDamage - repairAmount, 0));
      return RediaToolItem.onShapelessRecipeRepaired(result, previousDamage, result.getDamageValue(), registries);
   }

   private static List<ItemStack> findToolAndDiamond(CraftingInput input) {
      if (input.ingredientCount() != 2) {
         return null;
      }

      ItemStack tool = ItemStack.EMPTY;
      ItemStack diamond = ItemStack.EMPTY;
      for (int i = 0; i < input.size(); i++) {
         ItemStack stack = input.getItem(i);
         if (stack.isEmpty()) {
            continue;
         }

         if (stack.is(EngineerToolsModule.REDIA_TOOL.get())) {
            if (!tool.isEmpty()) {
               return null;
            }

            tool = stack;
         } else if (stack.is(Tags.Items.GEMS_DIAMOND)) {
            if (!diamond.isEmpty()) {
               return null;
            }

            diamond = stack;
         } else {
            return null;
         }
      }

      return !tool.isEmpty() && !diamond.isEmpty() ? List.of(tool, diamond) : null;
   }

   @Override
   public boolean canCraftInDimensions(int width, int height) {
      return width * height >= 2;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return ModRecipeSerializers.REDIA_TOOL_REPAIR.get();
   }
}
