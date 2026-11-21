package com.oblixorprime.engineersdecorreforged.block;

import com.oblixorprime.engineersdecorreforged.ModBlocks;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Bounded, loaded-chunk-only transfer for one of nine physically parallel channels.
 * No FE is stored in passive C01 segments. C03 uses a persistent residual to avoid
 * silently deleting energy if a recipient changes between simulation and transfer.
 */
public final class MicroConduitNetwork {
   /** Candidate balancing constants; qualification against actual IE LV configuration required. */
   public static final int MAX_TRANSFER_PER_TICK = 256;
   public static final int MAX_PENDING = 256;
   public static final int MAX_CONDUIT_VISITS = 512;

   private MicroConduitNetwork() { }

   public static void transfer(ServerLevel level, BlockPos origin, BlockState sourceState, MicroConnectorBlockEntity source) {
      Direction machineFacing = sourceState.getValue(MicroMachineConnectorBlock.FACING);
      int channel = sourceState.getValue(MicroMachineConnectorBlock.CHANNEL);
      BlockPos first = origin.relative(machineFacing.getOpposite());

      if (!level.hasChunkAt(first) || !isConduitAlong(level.getBlockState(first), machineFacing.getAxis())) return;
      List<BlockPos> sinks = findSinks(level, first, channel);
      if (sinks.isEmpty()) return;

      BlockPos machine = origin.relative(machineFacing);
      IEnergyStorage power = level.hasChunkAt(machine)
         ? level.getCapability(EnergyStorage.BLOCK, machine, machineFacing.getOpposite()) : null;

      int budget = MAX_TRANSFER_PER_TICK;
      int start = Math.floorMod(source.nextSink(), sinks.size());
      for (int index = 0; index < sinks.size() && budget > 0; ++index) {
         BlockPos sinkPos = sinks.get((start + index) % sinks.size());
         if (!level.hasChunkAt(sinkPos)) continue;
         BlockState sinkState = level.getBlockState(sinkPos);
         if (!sinkState.is(ModBlocks.MICRO_MACHINE_CONNECTOR.get()) ||
               sinkState.getValue(MicroMachineConnectorBlock.CHANNEL) != channel ||
               !(level.getBlockEntity(sinkPos) instanceof MicroConnectorBlockEntity sink) || sink.isSource()) continue;
         Direction sinkFacing = sinkState.getValue(MicroMachineConnectorBlock.FACING);
         BlockPos target = sinkPos.relative(sinkFacing);
         if (!level.hasChunkAt(target)) continue;
         IEnergyStorage receiver = level.getCapability(EnergyStorage.BLOCK, target, sinkFacing.getOpposite());
         if (receiver == null || !receiver.canReceive()) continue;

         int planned = Math.max(0, Math.min(budget, receiver.receiveEnergy(budget, true)));
         if (planned == 0) continue;
         int pending = source.pendingEnergy(channel);
         int needed = Math.max(0, planned - pending);
         if (needed > 0 && power != null && power.canExtract()) {
            int simulated = Math.max(0, Math.min(needed, power.extractEnergy(needed, true)));
            if (simulated > 0) {
               int extracted = Math.max(0, Math.min(simulated, power.extractEnergy(simulated, false)));
               pending += extracted;
               source.setPendingEnergy(channel, pending);
            }
         }
         int offered = Math.min(planned, source.pendingEnergy(channel));
         if (offered <= 0) continue;
         int accepted = Math.max(0, Math.min(offered, receiver.receiveEnergy(offered, false)));
         source.setPendingEnergy(channel, source.pendingEnergy(channel) - accepted);
         budget -= accepted;
      }
      source.setNextSink(start + 1);
   }

   private static List<BlockPos> findSinks(ServerLevel level, BlockPos root, int channel) {
      ArrayDeque<BlockPos> queue = new ArrayDeque<>();
      Set<Long> visited = new HashSet<>();
      List<BlockPos> sinks = new ArrayList<>();
      queue.add(root);
      while (!queue.isEmpty() && visited.size() < MAX_CONDUIT_VISITS) {
         BlockPos pos = queue.removeFirst();
         if (!level.hasChunkAt(pos) || !visited.add(pos.asLong())) continue;
         BlockState state = level.getBlockState(pos);
         if (!state.is(ModBlocks.MICROCONDUIT_9LV.get())) continue;
         Direction.Axis axis = state.getValue(MicroConduitBlock.AXIS);
         for (Direction direction : Direction.values()) {
            if (direction.getAxis() != axis) continue;
            BlockPos nextPos = pos.relative(direction);
            if (!level.hasChunkAt(nextPos)) continue;
            BlockState next = level.getBlockState(nextPos);
            if (isConduitAlong(next, axis)) {
               if (!visited.contains(nextPos.asLong())) queue.add(nextPos);
            } else if (next.is(ModBlocks.MICRO_MACHINE_CONNECTOR.get()) &&
                       next.getValue(MicroMachineConnectorBlock.FACING) == direction &&
                       next.getValue(MicroMachineConnectorBlock.CHANNEL) == channel) {
               sinks.add(nextPos.immutable());
            }
         }
      }
      return sinks;
   }

   private static boolean isConduitAlong(BlockState state, Direction.Axis axis) {
      return state.is(ModBlocks.MICROCONDUIT_9LV.get()) && state.getValue(MicroConduitBlock.AXIS) == axis;
   }
}
