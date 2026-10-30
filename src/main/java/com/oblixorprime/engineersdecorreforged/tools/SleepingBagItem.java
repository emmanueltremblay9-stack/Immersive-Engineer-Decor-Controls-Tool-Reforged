package com.oblixorprime.engineersdecorreforged.tools;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Player.BedSleepingProblem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import net.neoforged.neoforge.event.EventHooks;

public class SleepingBagItem extends NonEnchantableTooltipItem {
   public static final int MAX_DAMAGE = 4096;

   public SleepingBagItem(Properties properties) {
      super("sleeping_bag", properties);
   }

   @Override
   public boolean isBarVisible(ItemStack stack) {
      return false;
   }

   @Override
   public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
      if (context.getLevel().getBlockState(context.getClickedPos()).isBed(context.getLevel(), context.getClickedPos(), context.getPlayer())) {
         return InteractionResult.PASS;
      }
      if (context.getLevel().isClientSide) {
         return InteractionResult.SUCCESS;
      }
      if (context.getClickedFace() != Direction.UP || !(context.getPlayer() instanceof ServerPlayer player)) {
         return InteractionResult.CONSUME;
      }

      BlockPos sleepPos = context.getClickedPos().above();
      if (player.isSpectator() || player.isCreative() || player.isSleeping()) {
         return InteractionResult.CONSUME;
      }

      BedSleepingProblem problem = null;
      if (!player.isAlive()) {
         problem = BedSleepingProblem.OTHER_PROBLEM;
      } else if (!player.level().dimensionType().natural()) {
         problem = BedSleepingProblem.NOT_POSSIBLE_HERE;
      } else if (!player.level()
         .getEntitiesOfClass(
            Monster.class,
            new AABB(
               sleepPos.getX() - 8.0,
               sleepPos.getY() - 5.0,
               sleepPos.getZ() - 8.0,
               sleepPos.getX() + 8.0,
               sleepPos.getY() + 5.0,
               sleepPos.getZ() + 8.0
            ),
            monster -> monster.isPreventingPlayerRest(player)
         )
         .isEmpty()) {
         problem = BedSleepingProblem.NOT_SAFE;
      } else if (player.level().isDay()) {
         problem = BedSleepingProblem.NOT_POSSIBLE_NOW;
      }
      if (problem != null && problem != BedSleepingProblem.NOT_POSSIBLE_NOW) {
         player.displayClientMessage(problem.getMessage(), true);
         return InteractionResult.CONSUME;
      }

      var sleepResult = EventHooks.canPlayerStartSleeping(
         player,
         sleepPos,
         problem == null ? Either.<BedSleepingProblem, Unit>right(Unit.INSTANCE) : Either.<BedSleepingProblem, Unit>left(problem)
      );
      BedSleepingProblem rejected = sleepResult.left().orElse(null);
      if (rejected != null) {
         player.displayClientMessage(rejected.getMessage(), true);
      } else {
         player.startSleeping(sleepPos);
         player.serverLevel().updateSleepingPlayerList();
      }
      return InteractionResult.CONSUME;
   }

   @Override
   public InteractionResult useOn(UseOnContext context) {
      return InteractionResult.FAIL;
   }

   public static void onCanContinueSleeping(CanContinueSleepingEvent event) {
      if (event.getProblem() == BedSleepingProblem.NOT_POSSIBLE_HERE
         && event.getEntity() instanceof Player player
         && isHeldSleepingBag(player)) {
         event.setContinueSleeping(true);
      }
   }

   private static boolean isHeldSleepingBag(Player player) {
      return player.getMainHandItem().getItem() instanceof SleepingBagItem
         || player.getOffhandItem().getItem() instanceof SleepingBagItem;
   }
}
