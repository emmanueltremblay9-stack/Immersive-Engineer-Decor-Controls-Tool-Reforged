package com.oblixorprime.engineersdecorreforged.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Full glass retains the existing native fluid cell and suppresses shared glass faces. */
public final class PanzerGlassBlock extends NativeDecorativeBlocks.NativeSolidBlock {
   public static final MapCodec<PanzerGlassBlock> CODEC = simpleCodec(PanzerGlassBlock::new);

   public PanzerGlassBlock(Properties properties) { super(properties); }

   @Override public MapCodec<PanzerGlassBlock> codec() { return CODEC; }

   @Override public boolean skipRendering(BlockState state, BlockState adjacent, Direction side) {
      return adjacent.is(this) || super.skipRendering(state, adjacent, side);
   }
}
