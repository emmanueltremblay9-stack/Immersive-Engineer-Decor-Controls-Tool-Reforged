package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Adversarial destinations in the development GameTest server only. No normal-world provider is registered. */
@EventBusSubscriber(modid=EngineersDecorReforged.MOD_ID,bus=EventBusSubscriber.Bus.MOD)
public final class FluidHandlerTestTargets {
   private static final Map<Level,Map<BlockPos,IFluidHandler>> TARGETS=new WeakHashMap<>();
   @SubscribeEvent public static void capabilities(RegisterCapabilitiesEvent event) {
      if(!Boolean.getBoolean("neoforge.gameTestServer"))return;
      event.registerBlock(Capabilities.FluidHandler.BLOCK,(level,pos,state,be,side)->{
         var targets=TARGETS.get(level);return targets==null?null:targets.get(pos);
      },Blocks.BEDROCK);
   }
   public static AutoCloseable install(ServerLevel level,BlockPos pos,IFluidHandler handler) {
      if(!Boolean.getBoolean("neoforge.gameTestServer")||!level.hasChunkAt(pos))throw new IllegalStateException("Requires loaded development GameTest fixture");
      level.setBlock(pos,Blocks.BEDROCK.defaultBlockState(),3);
      TARGETS.computeIfAbsent(level,key->new HashMap<>()).put(pos.immutable(),handler);level.invalidateCapabilities(pos);
      return ()->{var targets=TARGETS.get(level);if(targets!=null){targets.remove(pos);if(targets.isEmpty())TARGETS.remove(level);}level.invalidateCapabilities(pos);};
   }
   private FluidHandlerTestTargets(){}
}
