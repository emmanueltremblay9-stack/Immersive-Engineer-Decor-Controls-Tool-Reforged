package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;

/** Capability-only inventories, registered exclusively in the development GameTest server. */
@EventBusSubscriber(modid = EngineersDecorReforged.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ItemHandlerTestTargets {
   private record Target(Direction side, IItemHandler handler) {}
   private static final Map<Level, Map<BlockPos, Target>> TARGETS = new WeakHashMap<>();

   @SubscribeEvent
   public static void capabilities(RegisterCapabilitiesEvent event) {
      if (!Boolean.getBoolean("neoforge.gameTestServer")) return;
      event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, be, side) -> {
         var targets = TARGETS.get(level);
         Target target = targets == null ? null : targets.get(pos);
         return target != null && side == target.side() ? target.handler() : null;
      }, Blocks.BEDROCK);
   }

   public static AutoCloseable install(ServerLevel level, BlockPos pos, Direction side, IItemHandler handler) {
      if (!Boolean.getBoolean("neoforge.gameTestServer") || !level.hasChunkAt(pos)) {
         throw new IllegalStateException("Requires loaded development GameTest fixture");
      }
      level.setBlock(pos, Blocks.BEDROCK.defaultBlockState(), 3);
      TARGETS.computeIfAbsent(level, key -> new HashMap<>()).put(pos.immutable(), new Target(side, handler));
      level.invalidateCapabilities(pos);
      return () -> {
         var targets = TARGETS.get(level);
         if (targets != null) {
            targets.remove(pos);
            if (targets.isEmpty()) TARGETS.remove(level);
         }
         level.invalidateCapabilities(pos);
      };
   }

   private ItemHandlerTestTargets() {}
}
