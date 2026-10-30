package com.oblixorprime.engineersdecorreforged.tools;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.EffectCures;

public class ChargedLapisItem extends TooltipItem {
   public ChargedLapisItem(Properties properties) {
      super("charged_lapis", properties);
   }

   @Override
   public boolean isFoil(ItemStack stack) {
      return true;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (level.isClientSide) {
         float randomPitch = level.getRandom().nextFloat() * 0.2F;
         for (float pitch : new float[]{0.45F, 0.4F, 0.5F}) {
            player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.05F, pitch + randomPitch);
         }
         return InteractionResultHolder.success(stack);
      }

      player.giveExperienceLevels(1);
      player.removeEffectsCuredBy(EffectCures.MILK);
      player.clearFire();
      player.heal(player.getMaxHealth() / 20.0F);
      stack.shrink(1);

      return InteractionResultHolder.success(stack);
   }

   @Override
   public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
      return false;
   }
}
