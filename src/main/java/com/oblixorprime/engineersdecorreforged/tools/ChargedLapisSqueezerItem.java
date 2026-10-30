package com.oblixorprime.engineersdecorreforged.tools;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

public class ChargedLapisSqueezerItem extends TooltipItem {
   public ChargedLapisSqueezerItem(Properties properties) {
      super("charged_lapis_squeezer", properties);
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (level.isClientSide) {
         return InteractionResultHolder.success(stack);
      }

      if (player.experienceLevel < 1) {
         failure(level, player, "item.immersive_engineer_decor_controls_tool_reforged.charged_lapis_squeezer.msg.noxp");
         return InteractionResultHolder.fail(stack);
      }

      if (player.getHealth() <= player.getMaxHealth() / 10.0F) {
         failure(level, player, "item.immersive_engineer_decor_controls_tool_reforged.charged_lapis_squeezer.msg.lowhealth");
         return InteractionResultHolder.fail(stack);
      }

      ItemStack lapis = findLapis(player.getInventory());
      if (lapis.isEmpty()) {
         failure(level, player, "item.immersive_engineer_decor_controls_tool_reforged.charged_lapis_squeezer.msg.nolapis");
         return InteractionResultHolder.fail(stack);
      }

      lapis.shrink(1);
      ItemStack charged = new ItemStack((ItemLike)EngineerToolsModule.CHARGED_LAPIS.get());
      if (!player.getInventory().add(charged)) {
         player.drop(charged, false);
      }
      player.giveExperienceLevels(-1);
      player.causeFoodExhaustion(4.0F);
      player.setHealth(player.getHealth() - player.getMaxHealth() / 10.0F);
      player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0));

      level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT, player.getSoundSource(), 0.2F, 1.4F);
      level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, player.getSoundSource(), 0.5F, 1.4F);
      return InteractionResultHolder.success(stack);
   }

   @Override
   public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
      return false;
   }

   private static ItemStack findLapis(Inventory inventory) {
      for (int i = 0; i < inventory.getContainerSize(); i++) {
         ItemStack stack = inventory.getItem(i);
         if (stack.is(Items.LAPIS_LAZULI)) {
            return stack;
         }
      }

      return ItemStack.EMPTY;
   }

   private static void failure(Level level, Player player, String key) {
      player.displayClientMessage(Component.translatable(key), true);
      level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), player.getSoundSource(), 1.0F, 0.6F);
   }
}
