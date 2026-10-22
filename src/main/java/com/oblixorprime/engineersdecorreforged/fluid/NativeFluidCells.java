package com.oblixorprime.engineersdecorreforged.fluid;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.block.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.*;
import net.neoforged.neoforge.attachment.*;
import net.neoforged.neoforge.registries.*;

/** Exterior cells only, scoped to explicit native block opt-ins. Existing machine tanks are unrelated. */
public final class NativeFluidCells {
   public static final DeferredRegister<AttachmentType<?>> TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, EngineersDecorReforged.MOD_ID);
   public static final DeferredHolder<AttachmentType<?>, AttachmentType<FluidCellData>> CELLS = TYPES.register("fluid_cells", () ->
      AttachmentType.builder(() -> FluidCellData.EMPTY).serialize(FluidCellData.CODEC).sync(new AttachmentSyncHandler<FluidCellData>() {
         public void write(RegistryFriendlyByteBuf buf, FluidCellData cells, boolean initial) { cells.writeNetwork(buf,initial); }
         public FluidCellData read(IAttachmentHolder holder, RegistryFriendlyByteBuf buf, FluidCellData old) {
            if(!(holder instanceof LevelChunk))throw new IllegalArgumentException("Fluid cells require a chunk holder");
            var cells=FluidCellData.readNetwork(buf,old,(LevelChunk)holder);
            if (holder instanceof LevelChunk chunk) {
               var positions = new HashSet<Long>(cells.changedPositions()); if (old != null && cells.changedPositions().equals(cells.positions())) positions.addAll(old.positions());
               for (long packed : positions) {
                  var pos = BlockPos.of(packed); var state = chunk.getBlockState(pos);
                  refreshLight(chunk,pos,cells);
                  chunk.getLevel().sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
                  chunk.getLevel().getChunkSource().getLightEngine().checkBlock(pos);
               }
            }
            if (holder instanceof LevelChunk chunk) refreshRandomTicks(chunk, cells);
            return cells;
         }
      }).build());
   private NativeFluidCells() {}
   public static void chunkLoaded(net.neoforged.neoforge.event.level.ChunkEvent.Load event) {
      if (event.getChunk() instanceof LevelChunk chunk) {
         var cells=snapshot(chunk);refreshRandomTicks(chunk,cells);
         for(long packed:cells.positions())refreshLight(chunk,BlockPos.of(packed),cells);
      }
   }
   public static void refreshLight(LevelChunk chunk,BlockPos pos,FluidCellData cells) {
      var state=chunk.getBlockState(pos);
      var fluid=supported(state) && state.getFluidState().isEmpty()?cells.get(pos):Fluids.EMPTY.defaultFluidState();
      chunk.getAuxLightManager(chunk.getPos()).setLightAt(pos,fluid.isEmpty()?0:fluid.getFluidType().getLightLevel());
   }
   public static void refreshRandomTicks(LevelChunk chunk, FluidCellData data) {
      boolean[] ticking = new boolean[chunk.getSectionsCount()];
      for (long packed : data.positions()) {
         var pos = BlockPos.of(packed); int section = chunk.getSectionIndex(pos.getY());
         if (section >= 0 && section < ticking.length && data.get(pos).isRandomlyTicking()) ticking[section] = true;
      }
      for (int i=0; i<ticking.length; i++) ((FluidTickSection)(Object)chunk.getSections()[i]).iedctr$randomFluid(ticking[i]);
   }
   public static boolean supported(BlockState state) { return state.getBlock() instanceof NativeFluidloggedBlock; }
   public static boolean standard(Fluid fluid) {
      if (!(fluid instanceof FlowingFluid)) return false;
      var worldState=fluid.defaultFluidState().createLegacyBlock().getFluidState();
      return !worldState.isEmpty() && worldState.getType().isSame(fluid);
   }
   public static FluidCellData snapshot(LevelChunk chunk) { return chunk.getExistingDataOrNull(CELLS.get()) instanceof FluidCellData data ? data : FluidCellData.EMPTY; }
   public static FluidState stored(LevelChunk chunk, BlockPos pos) {
      var data=snapshot(chunk);
      if(data.isEmpty())return Fluids.EMPTY.defaultFluidState();
      var state = chunk.getBlockState(pos);
      return supported(state) && state.getFluidState().isEmpty() ? data.get(pos) : Fluids.EMPTY.defaultFluidState();
   }
   public static boolean reserved(BlockGetter level, BlockPos pos) {
      return level instanceof Level world && snapshot(world.getChunkAt(pos)).contains(pos);
   }
   public static boolean active(BlockGetter level, BlockPos pos) {
      return level instanceof Level world && !stored(world.getChunkAt(pos), pos).isEmpty();
   }
   public static void set(LevelAccessor level, BlockPos pos, FluidState fluid) {
      if (!(level instanceof Level world)) throw new IllegalArgumentException("Exterior fluid cells require a loaded level");
      var chunk = world.getChunkAt(pos);
      var before=snapshot(chunk);var after=before.with(pos,fluid);
      if(after==before)return;
      chunk.setData(CELLS.get(),after);
      chunk.setUnsaved(true);
      refreshRandomTicks(chunk, snapshot(chunk));
      refreshLight(chunk,pos,after);
      var state = world.getBlockState(pos);
      world.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
      world.getChunkSource().getLightEngine().checkBlock(pos);
      if (!world.isClientSide) {
         world.updateNeighborsAt(pos, state.getBlock());
         // Attachment-only changes do not call updateShape on adjacent decorative blocks.
         // Wake their existing fluids at the normal delay so changed levels and removal propagate.
         for (var direction : net.minecraft.core.Direction.values()) {
            var neighbor = pos.relative(direction);
            if (!world.hasChunkAt(neighbor)) continue;
            var neighborState = world.getBlockState(neighbor);
            if (supported(neighborState)) NativeFluidloggedBlock.schedule(neighborState, world, neighbor);
         }
         if (!fluid.isEmpty()) world.scheduleTick(pos, fluid.getType(), fluid.getType().getTickDelay(world));
      }
   }
   public static boolean place(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
      if (!(level instanceof Level) || !standard(fluid.getType()) || !supported(state)) return false;
      if (!fluid.isSource() && !StairWaterFlow.supported(state)) return false;
      if (!level.isClientSide()) {
         var dry = NativeFluidloggedBlock.dry(state);
         if (dry != state && !level.setBlock(pos, dry, Block.UPDATE_ALL)) return false;
         set(level, pos, fluid);
      }
      return true;
   }
}
