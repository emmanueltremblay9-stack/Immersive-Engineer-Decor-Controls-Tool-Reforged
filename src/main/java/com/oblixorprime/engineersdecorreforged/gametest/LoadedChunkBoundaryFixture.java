package com.oblixorprime.engineersdecorreforged.gametest;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Finds an already-loaded high-air edge; neither setup nor the operation may load a chunk. */
final class LoadedChunkBoundaryFixture implements AutoCloseable {
   final ServerLevel level;
   final BlockPos pos;
   final int loadedBefore;
   private final Map<BlockPos,BlockState> original=new LinkedHashMap<>();
   LoadedChunkBoundaryFixture(GameTestHelper h) {
      level=h.getLevel();var anchor=h.absolutePos(new BlockPos(3,3,3));BlockPos found=null;
      search:for(int radius=0;radius<=64;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++) {
         if(radius>0&&Math.abs(dx)!=radius&&Math.abs(dz)!=radius)continue;
         var candidate=new BlockPos(((anchor.getX()>>4)+dx)*16+15,level.getMaxBuildHeight()-8,((anchor.getZ()>>4)+dz)*16+8);
         if(!level.hasChunkAt(candidate)||level.hasChunkAt(candidate.east()))continue;
         if(level.getBlockState(candidate).isAir()&&level.getBlockState(candidate.west()).isAir()
               &&level.getBlockState(candidate.south(2)).isAir()){found=candidate;break search;}
      }
      h.assertTrue(found!=null,"loaded arena has a nearby unloaded chunk edge for the fixture");pos=found;
      loadedBefore=level.getChunkSource().getLoadedChunksCount();
   }
   void set(BlockPos target,BlockState state) {
      if(!level.hasChunkAt(target))throw new IllegalStateException("fixture attempted unavailable chunk");
      original.putIfAbsent(target.immutable(),level.getBlockState(target));
      // Suppress unrelated shape/neighbor propagation at the deliberately incomplete boundary.
      level.setBlock(target,state,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
   }
   @Override public void close(){for(var entry:original.entrySet())level.setBlock(entry.getKey(),entry.getValue(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);}
}
