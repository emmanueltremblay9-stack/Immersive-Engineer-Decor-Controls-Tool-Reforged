package com.oblixorprime.engineersdecorreforged.stool;

import com.oblixorprime.engineersdecorreforged.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

/** Exact furniture identities only: no tags, steel tables or third-party furniture. */
public final class DiningSaturation {
   private DiningSaturation() {
   }

   public static float seatedRate(Player player) {
      if (!(player.getVehicle() instanceof StoolSeatEntity seat) || !seat.isValidSeatFor(player)) {
         return 0.0F;
      }
      for (Direction side : Direction.Plane.HORIZONTAL) {
         if (player.level().getBlockState(seat.blockPosition().relative(side)).is(ModBlocks.TREATED_WOOD_TABLE.get())) {
            return 0.30F;
         }
      }
      return 0.20F;
   }

   public static float cakeRate(Player player, BlockPos cakePos) {
      float seated = seatedRate(player);
      if (seated > 0.0F) {
         return seated;
      }
      return !player.isPassenger() && player.isAlive() && !player.isSpectator()
         && player.level().getBlockState(cakePos.below()).is(ModBlocks.TREATED_WOOD_TABLE.get()) ? 0.10F : 0.0F;
   }

   public static void add(Player player, float normalSaturation, float rate) {
      if (rate <= 0.0F || !Float.isFinite(normalSaturation) || normalSaturation <= 0.0F) {
         return;
      }
      FoodData data = player.getFoodData();
      data.setSaturation(Math.min(data.getFoodLevel(), data.getSaturationLevel() + normalSaturation * rate));
   }
}
