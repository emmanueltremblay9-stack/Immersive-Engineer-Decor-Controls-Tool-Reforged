package com.oblixorprime.engineersdecorreforged.block;

import com.oblixorprime.engineersdecorreforged.ModBlockEntities;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Persistent residual-energy protection for one selected LV channel. */
public final class MicroConnectorBlockEntity extends BlockEntity {
   private boolean source = false;
   // Saved by lane: changing C03 selector cannot redirect residual FE to another independent circuit.
   private final int[] pendingByChannel = new int[9];
   private int nextSink = 0;

   public MicroConnectorBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.MICRO_CONNECTOR.get(), pos, state);
   }

   public boolean isSource() {
      return source;
   }

   public void toggleSource() {
      source = !source;
      setChanged();
   }

   public int pendingEnergy(int channel) {
      return pendingByChannel[channel];
   }

   public void setPendingEnergy(int channel, int value) {
      int clamped = Math.max(0, Math.min(MicroConduitNetwork.MAX_PENDING, value));
      if (pendingByChannel[channel] != clamped) {
         pendingByChannel[channel] = clamped;
         setChanged();
      }
   }

   public int nextSink() {
      return nextSink;
   }

   public void setNextSink(int next) {
      if (nextSink != next) {
         nextSink = next;
         setChanged();
      }
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, MicroConnectorBlockEntity entity) {
      if (level instanceof ServerLevel server && entity.source && state.getBlock() instanceof MicroMachineConnectorBlock) {
         MicroConduitNetwork.transfer(server, pos, state, entity);
      }
   }

   @Override
   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.saveAdditional(tag, registries);
      tag.putBoolean("Source", source);
      tag.putIntArray("PendingByChannel", pendingByChannel);
      tag.putInt("NextSink", nextSink);
   }

   @Override
   protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);
      source = tag.getBoolean("Source");
      Arrays.fill(pendingByChannel, 0);
      if (tag.contains("PendingByChannel")) {
         int[] stored = tag.getIntArray("PendingByChannel");
         for (int i = 0; i < Math.min(stored.length, pendingByChannel.length); i++) {
            pendingByChannel[i] = Math.max(0, Math.min(MicroConduitNetwork.MAX_PENDING, stored[i]));
         }
      } else if (tag.contains("PendingEnergy")) {
         // WIP-save recovery for previous single-residual schema.
         int selected = getBlockState().getValue(MicroMachineConnectorBlock.CHANNEL);
         pendingByChannel[selected] = Math.max(0, Math.min(MicroConduitNetwork.MAX_PENDING, tag.getInt("PendingEnergy")));
      }
      nextSink = Math.max(0, tag.getInt("NextSink"));
   }
}
