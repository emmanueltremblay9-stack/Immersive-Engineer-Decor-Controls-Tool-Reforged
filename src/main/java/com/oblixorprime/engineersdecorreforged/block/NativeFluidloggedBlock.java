package com.oblixorprime.engineersdecorreforged.block;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * Native, source-only world cell for the explicitly opted-in decorative blocks.
 * Water retains its original property. Non-water fluid is carried by ordinary block
 * state palettes, save codecs and block/chunk packets; no numeric fluid-state IDs.
 */
public interface NativeFluidloggedBlock extends SimpleWaterloggedBlock {
   EnumProperty<Cell> FLUID_CELL = EnumProperty.create("fluid_cell", Cell.class);

   enum Cell implements StringRepresentable {
      EMPTY("empty"), LAVA("lava"), CREOSOTE("creosote");
      private final String name;
      Cell(String name) { this.name = name; }
      @Override public String getSerializedName() { return name; }
   }

   static Fluid creosote() {
      return BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("immersiveengineering", "creosote"));
   }

   static Cell cell(Fluid fluid) {
      if (fluid == Fluids.LAVA) return Cell.LAVA;
      if (fluid == creosote() && fluid != Fluids.EMPTY) return Cell.CREOSOTE;
      return Cell.EMPTY;
   }

   static FluidState fluidState(BlockState state) {
      // Water takes precedence for malformed states containing both properties.
      if (state.getValue(BlockStateProperties.WATERLOGGED)) return Fluids.WATER.getSource(false);
      Fluid fluid = switch (state.getValue(FLUID_CELL)) {
         case EMPTY -> Fluids.EMPTY;
         case LAVA -> Fluids.LAVA;
         case CREOSOTE -> creosote();
      };
      return fluid instanceof FlowingFluid flowing ? flowing.getSource(false) : Fluids.EMPTY.defaultFluidState();
   }

   static BlockState placement(BlockState state, BlockPlaceContext context) {
      return placement(state, context.getLevel().getFluidState(context.getClickedPos()));
   }

   static BlockState placement(BlockState state, FluidState replaced) {
      boolean source = replaced.isSource();
      return state.setValue(BlockStateProperties.WATERLOGGED, source && replaced.getType() == Fluids.WATER)
         .setValue(FLUID_CELL, source ? cell(replaced.getType()) : Cell.EMPTY);
   }

   static BlockState copyCell(BlockState target, BlockState cell) {
      target = target.setValue(BlockStateProperties.WATERLOGGED, cell.getValue(BlockStateProperties.WATERLOGGED))
         .setValue(FLUID_CELL, cell.getValue(FLUID_CELL));
      if (target.hasProperty(StairWaterFlow.WATER_FLOW))
         target = target.setValue(StairWaterFlow.WATER_FLOW, cell.hasProperty(StairWaterFlow.WATER_FLOW) ? cell.getValue(StairWaterFlow.WATER_FLOW) : 0);
      return target;
   }

   static void schedule(BlockState state, LevelAccessor level, BlockPos pos) {
      FluidState fluid = state.getFluidState();
      if (!fluid.isEmpty()) level.scheduleTick(pos, fluid.getType(), fluid.getType().getTickDelay(level));
   }

   static int light(BlockState state) {
      return !state.getValue(BlockStateProperties.WATERLOGGED) && state.getValue(FLUID_CELL) == Cell.LAVA ? 15 : 0;
   }

   @Override
   default boolean canPlaceLiquid(@Nullable Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
      return fluidState(state).isEmpty() && (fluid == Fluids.WATER || cell(fluid) != Cell.EMPTY);
   }

   @Override
   default boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
      if (!fluid.isSource() || !canPlaceLiquid(null, level, pos, state, fluid.getType())) return false;
      if (!level.isClientSide()) {
         BlockState filled = state.setValue(BlockStateProperties.WATERLOGGED, fluid.getType() == Fluids.WATER)
            .setValue(FLUID_CELL, cell(fluid.getType()));
         if (!level.setBlock(pos, filled, Block.UPDATE_ALL)) return false;
         schedule(filled, level, pos);
      }
      return true;
   }

   @Override
   default ItemStack pickupBlock(@Nullable Player player, LevelAccessor level, BlockPos pos, BlockState state) {
      FluidState fluid = fluidState(state);
      if (fluid.isEmpty() || fluid.getType().getBucket() == Items.AIR) return ItemStack.EMPTY;
      BlockState dry = state.setValue(BlockStateProperties.WATERLOGGED, false).setValue(FLUID_CELL, Cell.EMPTY);
      if (!level.setBlock(pos, dry, Block.UPDATE_ALL)) return ItemStack.EMPTY;
      if (!dry.canSurvive(level, pos)) level.destroyBlock(pos, true);
      return new ItemStack(fluid.getType().getBucket());
   }

   @Override
   default Optional<SoundEvent> getPickupSound(BlockState state) {
      return fluidState(state).getType().getPickupSound();
   }
}
