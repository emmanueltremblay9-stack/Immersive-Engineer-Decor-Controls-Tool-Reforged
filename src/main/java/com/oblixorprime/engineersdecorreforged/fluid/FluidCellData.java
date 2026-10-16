package com.oblixorprime.engineersdecorreforged.fluid;

import com.mojang.serialization.Codec;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.material.*;

/** Immutable chunk snapshot. Unknown or unsupported entries stay verbatim on disk and remain reserved. */
public final class FluidCellData {
   public static final FluidCellData EMPTY = new FluidCellData(Map.of(),Map.of(),null);
   public static final Codec<FluidCellData> CODEC = CompoundTag.CODEC.xmap(FluidCellData::decode, FluidCellData::encode);
   private final Map<Long, CompoundTag> encoded;
   private final Map<Long, FluidState> states;
   private final Long changed;
   private FluidCellData(Map<Long, CompoundTag> encoded, Map<Long,FluidState> states, Long changed) {
      this.encoded=Map.copyOf(encoded);this.states=Map.copyOf(states);this.changed=changed;
   }
   private static FluidState resolve(CompoundTag tag) {
      return FluidState.CODEC.parse(NbtOps.INSTANCE,tag).result().filter(s->!s.isEmpty() && NativeFluidCells.standard(s.getType())).orElse(Fluids.EMPTY.defaultFluidState());
   }
   public FluidState get(BlockPos pos) { return states.getOrDefault(pos.asLong(), Fluids.EMPTY.defaultFluidState()); }
   public boolean contains(BlockPos pos) { return encoded.containsKey(pos.asLong()); }
   public Set<Long> positions() { return encoded.keySet(); }
   public boolean isEmpty() { return encoded.isEmpty(); }
   public Set<Long> changedPositions() { return changed==null ? positions() : Set.of(changed); }
   public FluidCellData with(BlockPos pos, FluidState fluid) {
      if(fluid.isEmpty() ? !contains(pos) : fluid.equals(get(pos)))return this;
      if(!fluid.isEmpty() && !NativeFluidCells.standard(fluid.getType()))throw new IllegalArgumentException("Fluid lacks supported world behavior: "+fluid);
      return withEncoded(pos.asLong(),fluid.isEmpty()?null:(CompoundTag)FluidState.CODEC.encodeStart(NbtOps.INSTANCE,fluid).getOrThrow());
   }
   private FluidCellData withEncoded(long pos, CompoundTag tag) {
      var tags=new HashMap<>(encoded);var fluids=new HashMap<>(states);
      if(tag==null){tags.remove(pos);fluids.remove(pos);}else{
         tags.put(pos,tag.copy());var fluid=resolve(tag);if(fluid.isEmpty())fluids.remove(pos);else fluids.put(pos,fluid);
      }
      return new FluidCellData(tags,fluids,pos);
   }
   public CompoundTag encode() {
      var tag = new CompoundTag();encoded.forEach((pos,state)->tag.put(Long.toString(pos),state.copy()));return tag;
   }
   public static FluidCellData decode(CompoundTag tag) {
      var tags=new HashMap<Long,CompoundTag>();var fluids=new HashMap<Long,FluidState>();
      for(String key:tag.getAllKeys())try{
         if(tag.contains(key,Tag.TAG_COMPOUND)){
            long pos=Long.parseLong(key);var value=tag.getCompound(key).copy();tags.put(pos,value);var fluid=resolve(value);if(!fluid.isEmpty())fluids.put(pos,fluid);
         }
      }catch(NumberFormatException ignored){ /* Invalid position does not identify a cell. */ }
      return tags.isEmpty()?EMPTY:new FluidCellData(tags,fluids,null);
   }
   /** Initial snapshots use a registry-name palette; subsequent ordered NeoForge attachment packets carry one delta. */
   public void writeNetwork(RegistryFriendlyByteBuf buf,boolean initial) {
      boolean full=initial || changed==null;buf.writeBoolean(full);
      if(!full){buf.writeLong(changed);buf.writeNbt(encoded.get(changed));return;}
      var palette=new LinkedHashMap<CompoundTag,Integer>();
      for(var value:encoded.values())palette.computeIfAbsent(value,key->palette.size());
      buf.writeVarInt(palette.size());for(var value:palette.keySet())buf.writeNbt(value);
      buf.writeVarInt(encoded.size());encoded.forEach((pos,state)->{buf.writeLong(pos);buf.writeVarInt(palette.get(state));});
   }
   public static FluidCellData readNetwork(RegistryFriendlyByteBuf buf,FluidCellData previous,net.minecraft.world.level.chunk.LevelChunk chunk) {
      if(!buf.readBoolean()){
         long pos=buf.readLong();validatePosition(pos,chunk);return (previous==null?EMPTY:previous).withEncoded(pos,buf.readNbt());
      }
      int maximum=chunk.getSectionsCount()*4096;
      int count=buf.readVarInt();if(count<0 || count>maximum)throw new IllegalArgumentException("Invalid fluid palette size");
      var palette=new ArrayList<CompoundTag>(count);
      for(int i=0;i<count;i++){var tag=buf.readNbt();if(tag==null)throw new IllegalArgumentException("Missing palette state");palette.add(tag);}
      int cells=buf.readVarInt();if(cells<0 || cells>maximum)throw new IllegalArgumentException("Invalid fluid cell count");
      var tags=new HashMap<Long,CompoundTag>();var states=new HashMap<Long,FluidState>();
      for(int i=0;i<cells;i++){
         long pos=buf.readLong();validatePosition(pos,chunk);int id=buf.readVarInt();
         if(id<0 || id>=palette.size() || tags.containsKey(pos))throw new IllegalArgumentException("Invalid palette cell");
         var tag=palette.get(id);tags.put(pos,tag);var state=resolve(tag);if(!state.isEmpty())states.put(pos,state);
      }
      return tags.isEmpty()?EMPTY:new FluidCellData(tags,states,null);
   }
   private static void validatePosition(long packed,net.minecraft.world.level.chunk.LevelChunk chunk) {
      var pos=BlockPos.of(packed);
      if(!new net.minecraft.world.level.ChunkPos(pos).equals(chunk.getPos()) || chunk.isOutsideBuildHeight(pos))throw new IllegalArgumentException("Fluid cell outside holder chunk");
   }
}
