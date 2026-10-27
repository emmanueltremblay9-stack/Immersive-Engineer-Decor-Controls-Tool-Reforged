package com.oblixorprime.engineersdecorreforged.utility;

import com.oblixorprime.engineersdecorreforged.ModBlockEntities;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import blusunrize.immersiveengineering.api.fluid.IFluidPipe;

/** Stateless, axial forwarding. FACING is the outlet; this entity never owns fluid. */
public final class PipeValveBlockEntity extends BlockEntity implements IFluidPipe {
   public static final int MAX_FLOW = 1000;
   // Reference field's integer slope; upstream configuration historically selected another default.
   public static final int ANALOG_SLOPE = MAX_FLOW / 15;
   private final IFluidHandler[] faces = new IFluidHandler[6];
   private boolean forwarding;

   public PipeValveBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.PIPE_VALVE.get(), pos, state);
      for (Direction face : Direction.values()) faces[face.ordinal()] = new FaceHandler(face);
   }

   public IFluidHandler fluidHandler(Direction side) {
      if (side == null || isRemoved() || !getBlockState().hasProperty(MachineBlocks.FACING)) return null;
      return side.getAxis() == getBlockState().getValue(MachineBlocks.FACING).getAxis() ? faces[side.ordinal()] : null;
   }

   @Override public void setBlockState(BlockState state) {
      BlockState previous = getBlockState();
      super.setBlockState(state);
      if (level != null && previous.hasProperty(MachineBlocks.FACING) && state.hasProperty(MachineBlocks.FACING)
            && previous.getValue(MachineBlocks.FACING) != state.getValue(MachineBlocks.FACING)) {
         level.invalidateCapabilities(worldPosition);
      }
   }

   private int limit() {
      if (getBlockState().is(ModBlocks.STRAIGHT_PIPE_VALVE.get())) return MAX_FLOW;
      int signal = LoadedValveSignals.best(level, worldPosition);
      if (signal <= 0) return 0;
      return getBlockState().is(ModBlocks.STRAIGHT_PIPE_VALVE_REDSTONE_ANALOG.get()) && signal < 15
         ? signal * ANALOG_SLOPE : MAX_FLOW;
   }

   private final class FaceHandler implements IFluidHandler {
      private final Direction face;
      private FaceHandler(Direction face) { this.face = face; }
      private boolean input() {
         return level != null && !level.isClientSide && !isRemoved()
            && getBlockState().hasProperty(MachineBlocks.FACING)
            && face == getBlockState().getValue(MachineBlocks.FACING).getOpposite();
      }
      @Override public int getTanks() { return 1; }
      @Override public FluidStack getFluidInTank(int tank) { return FluidStack.EMPTY; }
      @Override public int getTankCapacity(int tank) { return tank == 0 && input() ? limit() : 0; }
      @Override public boolean isFluidValid(int tank, FluidStack stack) { return tank == 0 && input() && !stack.isEmpty(); }
      @Override public int fill(FluidStack stack, FluidAction action) {
         if (!input() || forwarding || stack.isEmpty()) return 0;
         int offered = Math.min(stack.getAmount(), limit());
         if (offered <= 0) return 0;
         Direction output = getBlockState().getValue(MachineBlocks.FACING);
         BlockPos target = worldPosition.relative(output);
         if (!level.hasChunkAt(target)) return 0;
         forwarding = true;
         try {
            IFluidHandler destination = level.getCapability(Capabilities.FluidHandler.BLOCK, target, output.getOpposite());
            if (destination == null) return 0;
            return Math.max(0, Math.min(offered, destination.fill(stack.copyWithAmount(offered), action)));
         } finally {
            forwarding = false;
         }
      }
      @Override public FluidStack drain(FluidStack stack, FluidAction action) { return FluidStack.EMPTY; }
      @Override public FluidStack drain(int amount, FluidAction action) { return FluidStack.EMPTY; }
   }
}
