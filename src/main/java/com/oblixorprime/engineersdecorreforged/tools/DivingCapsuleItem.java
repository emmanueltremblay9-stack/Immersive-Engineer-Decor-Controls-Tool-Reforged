package com.oblixorprime.engineersdecorreforged.tools;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class DivingCapsuleItem extends NonEnchantableTooltipItem {
   private static final int ITEM_DAMAGE_PER_USE = 10;
   private static final int TRIGGER_AIR_TENTHS = 3;
   private static final int AIR_BOOST_TENTHS = 7;

   public DivingCapsuleItem(Properties properties) {
      super("diving_capsule", properties);
   }

   @Override
   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
      if (level.isClientSide || !(entity instanceof Player player) || !player.isAlive()) {
         return;
      }

      int maxAir = player.getMaxAirSupply();
      if (maxAir <= 0 || player.getAirSupply() > maxAir * TRIGGER_AIR_TENTHS / 10 || hasEarlierCapsule(player.getInventory(), stack, slotId)) {
         return;
      }

      player.setAirSupply(Math.min(maxAir, player.getAirSupply() + maxAir * AIR_BOOST_TENTHS / 10));
      boolean lastUse = stack.getDamageValue() + ITEM_DAMAGE_PER_USE >= stack.getMaxDamage();
      level.playSound(
         null,
         player.blockPosition(),
         lastUse ? SoundEvents.BUBBLE_COLUMN_BUBBLE_POP : SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE,
         player.getSoundSource(),
         lastUse ? 1.2F : 1.0F,
         lastUse ? 1.4F : 2.2F
      );
      damageOrConsume(stack, ITEM_DAMAGE_PER_USE);
   }

   private static boolean hasEarlierCapsule(Inventory inventory, ItemStack capsule, int activeSlot) {
      int lastSlot = activeSlot < 0 ? inventory.getContainerSize() : Math.min(activeSlot, inventory.getContainerSize());
      for (int slot = 0; slot < lastSlot; slot++) {
         if (ItemStack.isSameItem(inventory.getItem(slot), capsule)) {
            return true;
         }
      }
      return false;
   }

   private static void damageOrConsume(ItemStack stack, int amount) {
      int nextDamage = stack.getDamageValue() + amount;
      if (nextDamage >= stack.getMaxDamage()) {
         stack.shrink(1);
      } else {
         stack.setDamageValue(nextDamage);
      }
   }
}
