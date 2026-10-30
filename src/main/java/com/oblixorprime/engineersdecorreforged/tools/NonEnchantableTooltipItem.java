package com.oblixorprime.engineersdecorreforged.tools;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;

abstract class NonEnchantableTooltipItem extends TooltipItem {
   protected NonEnchantableTooltipItem(String itemName, Properties properties) {
      super(itemName, properties.setNoRepair());
   }

   @Override
   public boolean isEnchantable(ItemStack stack) {
      return false;
   }

   @Override
   public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
      return false;
   }
}
