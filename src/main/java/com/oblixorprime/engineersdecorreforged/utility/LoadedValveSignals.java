package com.oblixorprime.engineersdecorreforged.utility;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Vanilla weak-power reads can visit the neighbour of a conducting neighbour. */
final class LoadedValveSignals {
   private LoadedValveSignals() {}
   static boolean available(Level level,BlockPos pos) {
      for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++) {
         if(Math.abs(x)+Math.abs(z)<=2&&!level.hasChunkAt(pos.offset(x,0,z)))return false;
      }
      return true;
   }
   static int best(Level level,BlockPos pos) {
      return available(level,pos)?level.getBestNeighborSignal(pos):0;
   }
}
