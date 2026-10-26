package com.oblixorprime.engineersdecorreforged.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Existing stair mechanics; connection material is derived by the client, never a saved property. */
public final class RaisedCatwalkStairsBlock extends PortedBlocks.CatwalkStairsBlock {
   public static final MapCodec<RaisedCatwalkStairsBlock> CODEC = simpleCodec(RaisedCatwalkStairsBlock::new);
   public RaisedCatwalkStairsBlock(Properties properties) { super(properties); }
   @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }

   public static Support support(BlockState pole) {
      return switch (RaisedCatwalkBlock.poleVariant(pole)) {
         case 1, 2 -> Support.STEEL;
         case 5 -> Support.WOOD;
         default -> Support.NONE;
      };
   }

   public enum Support implements StringRepresentable {
      NONE("none"), WOOD("wood"), STEEL("steel");
      private final String name;
      Support(String name) { this.name = name; }
      @Override public String getSerializedName() { return name; }
   }
}
