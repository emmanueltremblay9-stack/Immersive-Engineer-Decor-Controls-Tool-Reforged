package com.oblixorprime.engineersdecorreforged.block;

import com.oblixorprime.engineersdecorreforged.fluid.PreviewLightAccess;
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
 * Native source-only world cell for explicitly opted-in decorative blocks.
 * CompactState opts a measured high-cardinality block into one enum property.
 * Other blocks keep the legacy properties until separately migrated.
 */
public interface NativeFluidloggedBlock extends SimpleWaterloggedBlock, net.neoforged.neoforge.common.extensions.IBlockExtension {
   EnumProperty<Cell> FLUID_CELL = EnumProperty.create("fluid_cell", Cell.class);
   EnumProperty<FluidContent> FLUID_CONTENT = EnumProperty.create("fluid_content", FluidContent.class);

   /** Marker for blocks using the compact fluid schema. */
   interface CompactState {}

   /** Normalized source and transient-water states for the compact schema. */
   enum FluidContent implements StringRepresentable {
      EMPTY("empty", 0),
      WATER("water", 0),
      FLOWING_WATER_1("flowing_water_1", 1),
      FLOWING_WATER_2("flowing_water_2", 2),
      FLOWING_WATER_3("flowing_water_3", 3),
      FLOWING_WATER_4("flowing_water_4", 4),
      FLOWING_WATER_5("flowing_water_5", 5),
      FLOWING_WATER_6("flowing_water_6", 6),
      FLOWING_WATER_7("flowing_water_7", 7),
      FLOWING_WATER_8("flowing_water_8", 8),
      FLOWING_WATER_9("flowing_water_9", 9),
      FLOWING_WATER_10("flowing_water_10", 10),
      FLOWING_WATER_11("flowing_water_11", 11),
      FLOWING_WATER_12("flowing_water_12", 12),
      FLOWING_WATER_13("flowing_water_13", 13),
      FLOWING_WATER_14("flowing_water_14", 14),
      FLOWING_WATER_15("flowing_water_15", 15),
      LAVA("lava", 0),
      CREOSOTE("creosote", 0);

      private final String name;
      private final int waterFlow;

      FluidContent(String name, int waterFlow) {
         this.name = name;
         this.waterFlow = waterFlow;
      }

      @Override
      public String getSerializedName() {
         return name;
      }

      public boolean isFlowingWater() {
         return waterFlow > 0;
      }

      public int waterFlow() {
         return waterFlow;
      }

      public FluidState fluidState() {
         if (isFlowingWater()) {
            boolean falling = waterFlow >= 8;
            int amount = falling ? 16 - waterFlow : waterFlow;
            return Fluids.FLOWING_WATER.getFlowing(amount, falling);
         }
         return switch (this) {
            case EMPTY -> Fluids.EMPTY.defaultFluidState();
            case WATER -> Fluids.WATER.getSource(false);
            case LAVA -> Fluids.LAVA.getSource(false);
            case CREOSOTE -> creosote() instanceof FlowingFluid flowing
               ? flowing.getSource(false) : Fluids.EMPTY.defaultFluidState();
            default -> throw new IllegalStateException("Unhandled compact fluid content " + this);
         };
      }

      public static FluidContent fromWaterFlow(int value) {
         if (value <= 0 || value > 15) return EMPTY;
         return values()[value + 1];
      }

      public static FluidContent fromFluidState(FluidState fluid) {
         if (fluid.isEmpty()) return EMPTY;
         if (fluid.getType() == Fluids.WATER && fluid.isSource()) return WATER;
         if (fluid.getType() == Fluids.FLOWING_WATER) {
            int value = fluid.getValue(FlowingFluid.FALLING) ? 16 - fluid.getAmount() : fluid.getAmount();
            return fromWaterFlow(value);
         }
         if (!fluid.isSource()) return EMPTY;
         return switch (cell(fluid.getType())) {
            case EMPTY -> EMPTY;
            case LAVA -> LAVA;
            case CREOSOTE -> CREOSOTE;
         };
      }
   }

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

   static FluidContent fluidContent(BlockState state) {
      if (state.hasProperty(FLUID_CONTENT)) return state.getValue(FLUID_CONTENT);
      if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED))
         return FluidContent.WATER;
      if (state.hasProperty(FLUID_CELL)) {
         FluidContent source = switch (state.getValue(FLUID_CELL)) {
            case EMPTY -> FluidContent.EMPTY;
            case LAVA -> FluidContent.LAVA;
            case CREOSOTE -> FluidContent.CREOSOTE;
         };
         if (source != FluidContent.EMPTY) return source;
      }
      return state.hasProperty(StairWaterFlow.WATER_FLOW)
         ? FluidContent.fromWaterFlow(state.getValue(StairWaterFlow.WATER_FLOW))
         : FluidContent.EMPTY;
   }

   static FluidState fluidState(BlockState state) {
      return fluidContent(state).fluidState();
   }

   static BlockState withFluidContent(BlockState state, FluidContent content) {
      if (state.hasProperty(FLUID_CONTENT)) return state.setValue(FLUID_CONTENT, content);
      if (state.hasProperty(BlockStateProperties.WATERLOGGED))
         state = state.setValue(BlockStateProperties.WATERLOGGED, content == FluidContent.WATER);
      if (state.hasProperty(FLUID_CELL)) {
         Cell cell = switch (content) {
            case LAVA -> Cell.LAVA;
            case CREOSOTE -> Cell.CREOSOTE;
            default -> Cell.EMPTY;
         };
         state = state.setValue(FLUID_CELL, cell);
      }
      if (state.hasProperty(StairWaterFlow.WATER_FLOW))
         state = state.setValue(StairWaterFlow.WATER_FLOW, content.waterFlow());
      return state;
   }

   static BlockState placement(BlockState state, BlockPlaceContext context) {
      return placement(state, context.getLevel().getFluidState(context.getClickedPos()));
   }

   static BlockState placement(BlockState state, FluidState replaced) {
      if (state == null) return null;
      boolean source = replaced.isSource() && replaced.getType() instanceof FlowingFluid flowing
         && replaced.equals(flowing.getSource(false));
      return withFluidContent(state, source ? FluidContent.fromFluidState(replaced) : FluidContent.EMPTY);
   }

   static BlockState dry(BlockState state) {
      if (state.hasProperty(FLUID_CONTENT)) return state.setValue(FLUID_CONTENT, FluidContent.EMPTY);
      state = state.setValue(BlockStateProperties.WATERLOGGED, false).setValue(FLUID_CELL, Cell.EMPTY);
      return state.hasProperty(StairWaterFlow.WATER_FLOW) ? state.setValue(StairWaterFlow.WATER_FLOW, 0) : state;
   }

   static BlockState copyCell(BlockState target, BlockState cell) {
      return withFluidContent(target, fluidContent(cell));
   }

   static void schedule(BlockState state, LevelAccessor level, BlockPos pos) {
      FluidState fluid = level.getFluidState(pos);
      if (!fluid.isEmpty()) level.scheduleTick(pos, fluid.getType(), fluid.getType().getTickDelay(level));
   }

   static int light(BlockState state) {
      return fluidContent(state) == FluidContent.LAVA ? 15 : 0;
   }

   @Override
   default boolean canPlaceLiquid(@Nullable Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
      FluidState current = level.getFluidState(pos);
      if (com.oblixorprime.engineersdecorreforged.fluid.NativeFluidCells.reserved(level, pos) && current.isEmpty()) return false;
      return com.oblixorprime.engineersdecorreforged.fluid.NativeFluidCells.standard(fluid)
         && (current.isEmpty() || (!current.isSource() && current.getType().isSame(fluid)
            && ((FlowingFluid)fluid).getSource() == fluid));
   }

   @Override
   default boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
      if (!fluid.isSource() || !canPlaceLiquid(null, level, pos, state, fluid.getType())) return false;
      BlockState filled = placement(state, fluid);
      if (fluid.getType() != Fluids.WATER && cell(fluid.getType()) == Cell.EMPTY
            || !fluid.equals(filled.getFluidState()))
         return com.oblixorprime.engineersdecorreforged.fluid.NativeFluidCells.place(level, pos, state, fluid);
      if (!level.isClientSide()) {
         if (!level.setBlock(pos, filled, Block.UPDATE_ALL)) return false;
         schedule(filled, level, pos);
      }
      return true;
   }

   @Override
   default ItemStack pickupBlock(@Nullable Player player, LevelAccessor level, BlockPos pos, BlockState state) {
      FluidState fluid = level.getFluidState(pos);
      if (!fluid.isSource() || fluid.isEmpty() || fluid.getType().getBucket() == Items.AIR) return ItemStack.EMPTY;
      if (com.oblixorprime.engineersdecorreforged.fluid.NativeFluidCells.active(level, pos)) {
         com.oblixorprime.engineersdecorreforged.fluid.NativeFluidCells.set(level, pos, Fluids.EMPTY.defaultFluidState());
         return new ItemStack(fluid.getType().getBucket());
      }
      BlockState dry = dry(state);
      if (!level.setBlock(pos, dry, Block.UPDATE_ALL)) return ItemStack.EMPTY;
      if (!dry.canSurvive(level, pos)) level.destroyBlock(pos, true);
      return new ItemStack(fluid.getType().getBucket());
   }

   @Override
   default boolean hasDynamicLightEmission(BlockState state) { return true; }

   @Override
   default int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
      var auxiliary = PreviewLightAccess.auxiliaryLight(level, pos);
      return Math.max(state.getLightEmission(), auxiliary == null ? 0 : auxiliary.getLightAt(pos));
   }

   @Override
   default Optional<SoundEvent> getPickupSound(BlockState state) {
      return fluidState(state).getType().getPickupSound();
   }
}
