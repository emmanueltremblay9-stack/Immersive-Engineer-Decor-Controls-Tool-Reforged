package com.oblixorprime.engineersdecorreforged.compat;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;

/** Optional boundary: the common machine never links a TFC type. */
public final class TfcMilkingCompat {
   private TfcMilkingCompat() {}

   public static boolean tryMilk(ServerLevel level, BlockPos machine, AABB area, Predicate<FluidStack> accept) {
      // This preview targets the actual API/release exercised by the compatibility profile.
      // Other TFC versions keep loading normally, without an unverified native adapter.
      if (ModList.get().getModContainerById("tfc")
            .filter(mod -> mod.getModInfo().getVersion().toString().equals("4.2.12")).isEmpty()) return false;
      return Loaded.tryMilk(level, machine, area, accept);
   }

   private static final class Loaded {
      private static boolean tryMilk(ServerLevel level, BlockPos machine, AABB area, Predicate<FluidStack> accept) {
         return TfcMilkingAdapter.tryMilk(level, machine, area, accept);
      }
   }
}
