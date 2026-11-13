package com.oblixorprime.engineersdecorreforged.compat;

import java.util.function.Predicate;
import java.util.UUID;
import java.util.Set;
import com.oblixorprime.engineersdecorreforged.utility.MilkingState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;

/** Optional boundary: the common machine never links a TFC type. */
public final class TfcMilkingCompat {
   private TfcMilkingCompat() {}

   private static boolean supported() {
      // This preview targets the actual API/release exercised by the compatibility profile.
      // Other TFC versions keep loading normally, without an unverified native adapter.
      return ModList.get().getModContainerById("tfc")
            .filter(mod -> mod.getModInfo().getVersion().toString().equals("4.2.12")).isPresent();
   }

   public static MilkingState select(ServerLevel level, AABB area) {
      return select(level, area, Set.of(), -1);
   }

   public static MilkingState select(ServerLevel level, AABB area, Set<UUID> deferred, int previousAttemptId) {
      return supported() ? Loaded.select(level, area, deferred, previousAttemptId) : MilkingState.empty();
   }

   public static MilkingState inspect(ServerLevel level, AABB area, UUID animal) {
      return supported() && animal != null ? Loaded.inspect(level, area, animal) : MilkingState.empty();
   }

   public static boolean tryMilk(ServerLevel level, BlockPos machine, AABB area, UUID animal, Predicate<FluidStack> accept) {
      return supported() && animal != null && Loaded.tryMilk(level, machine, area, animal, accept);
   }

   private static final class Loaded {
      private static MilkingState select(ServerLevel level, AABB area, Set<UUID> deferred, int previousAttemptId) {
         return TfcMilkingAdapter.select(level, area, deferred, previousAttemptId);
      }
      private static MilkingState inspect(ServerLevel level, AABB area, UUID animal) { return TfcMilkingAdapter.inspect(level, area, animal); }
      private static boolean tryMilk(ServerLevel level, BlockPos machine, AABB area, UUID animal, Predicate<FluidStack> accept) {
         return TfcMilkingAdapter.tryMilk(level, machine, area, animal, accept);
      }
   }
}
