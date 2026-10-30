package com.oblixorprime.engineersdecorreforged.tools;

import com.oblixorprime.engineersdecorreforged.ModSounds;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public class AutoStimPackItem extends NonEnchantableTooltipItem {
   private static final String TAG_ARMING = "ta";
   private static final String TAG_COOLDOWN = "cd";
   private static final int ARMING_TICKS = 20;
   private static final int OTHER_PACK_COOLDOWN_TICKS = 30;
   private static final float TRIGGER_HEALTH = 6.0F;
   private static final float HEALING = 6.0F;

   public AutoStimPackItem(Properties properties) {
      super("stimpack", properties);
   }

   @Override
   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
      if (level.isClientSide || !(entity instanceof Player player)) {
         return;
      }

      CompoundTag data = data(stack);
      if (data.contains(TAG_COOLDOWN)) {
         data.remove(TAG_ARMING);
         int remaining = data.getInt(TAG_COOLDOWN);
         if (remaining > 0) {
            data.putInt(TAG_COOLDOWN, remaining - 1);
         } else {
            data.remove(TAG_COOLDOWN);
         }
         setData(stack, data);
         return;
      }

      if (!player.isAlive() || player.getHealth() > TRIGGER_HEALTH) {
         return;
      }

      int arming = data.getInt(TAG_ARMING);
      if (arming < ARMING_TICKS) {
         if (arming == 2) {
            level.playSound(null, player.blockPosition(), ModSounds.STIMPACK_INJECT.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
         }
         data.putInt(TAG_ARMING, arming + 1);
      } else {
         data.remove(TAG_ARMING);
         damageOrConsume(stack, 1);
         player.heal(HEALING);
         player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400));
         player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300));
         player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200));
         player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200));
      }

      coordinateOtherPacks(player.getInventory(), stack, slotId);
      setData(stack, data);
   }

   private static void coordinateOtherPacks(Inventory inventory, ItemStack active, int activeSlot) {
      for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
         if (slot == activeSlot) {
            continue;
         }
         ItemStack other = inventory.getItem(slot);
         if (!other.isEmpty() && ItemStack.isSameItem(other, active)) {
            CompoundTag data = data(other);
            data.remove(TAG_ARMING);
            data.putInt(TAG_COOLDOWN, OTHER_PACK_COOLDOWN_TICKS);
            setData(other, data);
         }
      }
   }

   private static void damageOrConsume(ItemStack stack, int amount) {
      int nextDamage = stack.getDamageValue() + amount;
      if (nextDamage >= stack.getMaxDamage()) {
         stack.shrink(1);
      } else {
         stack.setDamageValue(nextDamage);
      }
   }

   private static CompoundTag data(ItemStack stack) {
      return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
   }

   private static void setData(ItemStack stack, CompoundTag data) {
      if (data.isEmpty()) {
         stack.remove(DataComponents.CUSTOM_DATA);
      } else {
         stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
      }
   }
}
