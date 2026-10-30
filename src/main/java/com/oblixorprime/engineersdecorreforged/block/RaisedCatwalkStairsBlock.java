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

   /** Geometry identity is finer than material: the steel poles have different sections. */
   public static Section section(BlockState pole) {
      return switch (RaisedCatwalkBlock.poleVariant(pole)) {
         case 1 -> Section.THICK_STEEL;
         case 2 -> Section.THIN_STEEL;
         case 5 -> Section.WOOD;
         default -> Section.NONE;
      };
   }

   public static boolean matchesStraightSection(BlockState pole, Section section) {
      if (!pole.hasProperty(net.minecraft.world.level.block.DirectionalBlock.FACING)
            || !pole.getValue(net.minecraft.world.level.block.DirectionalBlock.FACING).getAxis().isVertical()) return false;
      return switch(section) {
         case WOOD -> pole.is(com.oblixorprime.engineersdecorreforged.ModBlocks.TREATED_WOOD_POLE.get());
         case THIN_STEEL -> pole.is(com.oblixorprime.engineersdecorreforged.ModBlocks.THIN_STEEL_POLE.get());
         case THICK_STEEL -> pole.is(com.oblixorprime.engineersdecorreforged.ModBlocks.THICK_STEEL_POLE.get());
         default -> false;
      };
   }

   public enum Section implements StringRepresentable {
      NONE("none"), WOOD("wood"), THIN_STEEL("thin_steel"), THICK_STEEL("steel");
      private final String name;
      Section(String name) { this.name=name; }
      @Override public String getSerializedName() { return name; }
   }

   public enum Support implements StringRepresentable {
      NONE("none"), WOOD("wood"), STEEL("steel");
      private final String name;
      Support(String name) { this.name = name; }
      @Override public String getSerializedName() { return name; }
   }
}
