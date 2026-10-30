package com.oblixorprime.engineersdecorreforged.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class CrushingHammerItem extends NonEnchantableTooltipItem {
   public CrushingHammerItem(Properties properties) {
      super("crushing_hammer", properties);
   }

   @Override
   public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
      return true;
   }

   @Override
   public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
      if (target instanceof LivingEntity living) {
         Level level = player.level();
         if (level.isClientSide) {
            return true;
         }

         boolean hard = target instanceof Monster monster && monster.getTarget() != null;
         living.knockback(hard ? 1.2F : 0.3F, Math.sin(Math.toRadians(player.getYRot())), -Math.cos(Math.toRadians(player.getYRot())));
         if (hard) {
            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);

            level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_PLACE, player.getSoundSource(), 0.2F, 0.05F);
         } else {
            level.playSound(null, player.blockPosition(), SoundEvents.BAMBOO_HIT, player.getSoundSource(), 0.5F, 0.3F);
         }

         return true;
      } else {
         return true;
      }
   }

   public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
      if (level.isClientSide || !(miningEntity instanceof Player)) {
         return true;
      }

      if (state.getDestroySpeed(level, pos) > 0.5F) {
         stack.hurtAndBreak(1, miningEntity, EquipmentSlot.MAINHAND);
      }

      return false;
   }
}
