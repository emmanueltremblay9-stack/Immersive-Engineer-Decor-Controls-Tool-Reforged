package com.oblixorprime.engineersdecorreforged.block;

import com.oblixorprime.engineersdecorreforged.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** One saved attachment, no ticker/capability. Unknown data is kept without paying out items. */
public final class DoubleTLightBlockEntity extends BlockEntity {
   // NeoForge's common BlockEntity model-data API; contains no client renderer objects.
   public static final net.neoforged.neoforge.client.model.data.ModelProperty<Attachment> MODEL_ATTACHMENT=new net.neoforged.neoforge.client.model.data.ModelProperty<>();
   public static final class Attachment {
      public final BlockState lamp;
      public final Vec3 offset;
      public final boolean paid;
      private final int frame;
      private final java.util.concurrent.ConcurrentMap<Integer,Attachment> rotations=new java.util.concurrent.ConcurrentHashMap<>();
      private final VoxelShape shape;
      private final java.util.concurrent.ConcurrentMap<VoxelShape,VoxelShape> outlines=new java.util.concurrent.ConcurrentHashMap<>();
      public Attachment(BlockState lamp,Vec3 offset,boolean paid){this(lamp,offset,paid,0);}
      private Attachment(BlockState lamp,Vec3 offset,boolean paid,int frame){this.lamp=lamp;this.offset=offset;this.paid=paid;this.frame=frame;this.shape=SupportLights.lampShape(lamp).move(offset.x,offset.y,offset.z);}
      private Attachment forFrame(int target){
         if(target==frame)return this;
         return rotations.computeIfAbsent(target,t->{
            int turns=(t-frame+4)%4;Vec3 moved=offset;var facing=lamp.getValue(PortedBlocks.FACING);
            for(int i=0;i<turns;i++){moved=new Vec3(-moved.z,moved.y,moved.x);facing=net.minecraft.world.level.block.Rotation.CLOCKWISE_90.rotate(facing);}
            return new Attachment(lamp.setValue(PortedBlocks.FACING,facing),moved,paid,t);
         });
      }
      public VoxelShape shape(){return shape;}
      public VoxelShape outline(VoxelShape base){return outlines.computeIfAbsent(base,b->net.minecraft.world.phys.shapes.Shapes.or(b,shape).optimize());}
   }
   private volatile Attachment attachment;
   private CompoundTag unknown;
   public DoubleTLightBlockEntity(BlockPos pos,BlockState state){super(ModBlockEntities.DOUBLE_T_LIGHT.get(),pos,state);}
   private static int frame(BlockState state){
      if(state.hasProperty(VerticalDoubleTSupportBlock.FACING))return switch(state.getValue(VerticalDoubleTSupportBlock.FACING)){case EAST->1;case SOUTH->2;case WEST->3;default->0;};
      return state.hasProperty(PortedBlocks.EASTWEST)&&state.getValue(PortedBlocks.EASTWEST)?1:0;
   }
   @Nullable public Attachment attachment(){var current=attachment;return current==null?null:current.forFrame(frame(getBlockState()));}
   @Override public net.neoforged.neoforge.client.model.data.ModelData getModelData(){
      var value=SupportLights.attached(getBlockState())?attachment():null;
      return value==null?net.neoforged.neoforge.client.model.data.ModelData.EMPTY:net.neoforged.neoforge.client.model.data.ModelData.builder().with(MODEL_ATTACHMENT,value).build();
   }
   @Override public void setBlockState(BlockState state){
      var old=getBlockState();super.setBlockState(state);
      if(old!=state&&level!=null&&level.isClientSide)requestModelDataUpdate();
   }
   public boolean occupied(){return attachment!=null||unknown!=null;}
   void install(Attachment value){if(occupied())throw new IllegalStateException("Occupied Double T lamp");attachment=new Attachment(value.lamp,value.offset,value.paid,frame(getBlockState()));changed();}
   void clear(){attachment=null;unknown=null;changed();}
   private void changed(){
      setChanged();if(level!=null){
         if(level.isClientSide)requestModelDataUpdate();
         level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
      }
   }
   @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
      super.saveAdditional(tag,registries);
      if(unknown!=null){tag.put("SupportLamp",unknown.copy());return;}
      if(attachment!=null){
         var data=new CompoundTag();data.putInt("Version",1);data.put("Lamp",NbtUtils.writeBlockState(attachment.lamp));
         data.putBoolean("Paid",attachment.paid);data.putInt("Frame",attachment.frame);data.putDouble("X",attachment.offset.x);data.putDouble("Y",attachment.offset.y);data.putDouble("Z",attachment.offset.z);
         tag.put("SupportLamp",data);
      }
   }
   @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
      super.loadAdditional(tag,registries);attachment=null;unknown=null;
      if(tag.contains("SupportLamp")){
         CompoundTag data=tag.getCompound("SupportLamp");unknown=data.copy();
         if(data.getInt("Version")==1&&data.contains("Lamp",10)&&data.contains("X",6)&&data.contains("Y",6)&&data.contains("Z",6)){
            var lamp=NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),data.getCompound("Lamp"));
            var offset=new Vec3(data.getDouble("X"),data.getDouble("Y"),data.getDouble("Z"));
            if(SupportLights.supported(lamp.getBlock())&&SupportLights.inside(lamp,offset)){
               attachment=new Attachment(NativeFluidloggedBlock.dry(lamp),offset,data.getBoolean("Paid"),data.getInt("Frame")&3);unknown=null;
            }
         }
      }
      if(level!=null&&level.isClientSide){requestModelDataUpdate();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
   }
   @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){var tag=new CompoundTag();saveAdditional(tag,registries);return tag;}
   @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
