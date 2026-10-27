package com.oblixorprime.engineersdecorreforged.block;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

/** Four existing lamps, translated only; ordinary block placement remains the fallback. */
@EventBusSubscriber(modid=EngineersDecorReforged.MOD_ID)
public final class SupportLights {
   public static final BooleanProperty ATTACHED=BooleanProperty.create("support_light");
   private SupportLights(){}
   public static boolean supported(Block block){return block==ModBlocks.IRON_BULB_LIGHT.get()||block==ModBlocks.IRON_CEILING_EDGE_LIGHT.get()
      ||block==ModBlocks.IRON_FLOOR_EDGE_LIGHT.get()||block==ModBlocks.IRON_INSET_LIGHT.get();}
   public static boolean attached(BlockState state){return state.hasProperty(ATTACHED)&&state.getValue(ATTACHED);}
   public static boolean legacy(BlockState state){return state.getBlock() instanceof SteelDoubleTBulbBlock&&SteelDoubleTBulbBlock.hasBulb(state);}
   public static VoxelShape lampShape(BlockState lamp){
      if(lamp.is(ModBlocks.IRON_BULB_LIGHT.get()))return PortedBlocks.orientedNorthShape(Block.box(6,6,0,10,10,4.25),lamp.getValue(PortedBlocks.FACING),false);
      return lamp.getShape(EmptyBlockGetter.INSTANCE,BlockPos.ZERO);
   }
   public static boolean inside(BlockState lamp,Vec3 offset){
      if(!Double.isFinite(offset.x)||!Double.isFinite(offset.y)||!Double.isFinite(offset.z))return false;
      var box=lampShape(lamp).move(offset.x,offset.y,offset.z).bounds();
      return box.minX>=-1.e-7&&box.minY>=-1.e-7&&box.minZ>=-1.e-7&&box.maxX<=1+1.e-7&&box.maxY<=1+1.e-7&&box.maxZ<=1+1.e-7;
   }
   @Nullable public static DoubleTLightBlockEntity entity(BlockGetter level,BlockPos pos){return level.getBlockEntity(pos) instanceof DoubleTLightBlockEntity be?be:null;}
   @Nullable public static DoubleTLightBlockEntity.Attachment data(BlockState state,BlockGetter level,BlockPos pos){
      var be=attached(state)?entity(level,pos):null;return be==null?null:be.attachment();
   }
   @Nullable public static DoubleTLightBlockEntity.Attachment pose(BlockState host,BlockGetter level,BlockPos pos,BlockState lamp,Direction face,boolean paid){
      if(!(host.getBlock() instanceof DoubleTLightHost owner)||!supported(lamp.getBlock())||legacy(host))return null;
      boolean ceiling=lamp.is(ModBlocks.IRON_CEILING_EDGE_LIGHT.get()),floor=lamp.is(ModBlocks.IRON_FLOOR_EDGE_LIGHT.get());
      if((ceiling||floor)&&face!=lamp.getValue(PortedBlocks.FACING).getOpposite()&&!(ceiling&&face==Direction.DOWN)&&!(floor&&face==Direction.UP))return null;
      var structure=owner.supportShape(host,level,pos);var bounds=owner.mountingShape(host,level,pos).bounds();
      var original=lampShape(lamp).bounds();var axis=face.getAxis();boolean positive=face.getAxisDirection()==Direction.AxisDirection.POSITIVE;
      double target=positive?bounds.max(axis):bounds.min(axis),anchor=positive?original.min(axis):original.max(axis);
      // Cell-boundary mounts already work as ordinary independent blocks. Only combine internal faces.
      if(target<1.e-7||target>1-1.e-7)return null;
      double[] shift={0,0,0};shift[axis.ordinal()]=target-anchor;
      if(ceiling&&axis!=Direction.Axis.Y)shift[1]=bounds.maxY-original.maxY;
      else if(floor&&axis!=Direction.Axis.Y)shift[1]=bounds.minY-original.minY;
      else if(!ceiling&&!floor){
         for(var other:Direction.Axis.values())if(other!=axis)shift[other.ordinal()]=(bounds.min(other)+bounds.max(other)-original.min(other)-original.max(other))/2;
      }
      var offset=new Vec3(shift[0],shift[1],shift[2]);
      if(!inside(lamp,offset))return null;
      var shape=lampShape(lamp).move(offset.x,offset.y,offset.z);
      if(Shapes.joinIsNotEmpty(structure,shape,BooleanOp.AND))return null;
      // Move a tiny distance into the mounting face to establish genuine metal contact.
      if(!Shapes.joinIsNotEmpty(structure,shape.move(-face.getStepX()*1.e-5,-face.getStepY()*1.e-5,-face.getStepZ()*1.e-5),BooleanOp.AND))return null;
      return new DoubleTLightBlockEntity.Attachment(NativeFluidloggedBlock.dry(lamp),offset,paid);
   }
   public static VoxelShape outline(BlockState state,BlockGetter level,BlockPos pos,VoxelShape base){
      var lamp=data(state,level,pos);return lamp==null?base:lamp.outline(base);
   }
   @Nullable static PortedBlocks.RailingPart select(BlockState state,Level level,BlockPos pos,Vec3 start,Vec3 end){
      var host=(DoubleTLightHost)state.getBlock();var base=host.supportShape(state,level,pos);var beam=base.clip(start,end,pos);
      var lamp=data(state,level,pos);var hit=lamp==null?null:lamp.shape().clip(start,end,pos);
      if(hit!=null&&(beam==null||start.distanceToSqr(hit.getLocation())<start.distanceToSqr(beam.getLocation())-1.e-9))
         return new PortedBlocks.RailingPart(null,lamp.shape(),PortedBlocks.PartKind.SUPPORT_LIGHT,lamp);
      return beam==null?null:new PortedBlocks.RailingPart(null,base);
   }
   static boolean remove(BlockState state,Level level,BlockPos pos,Player player,boolean harvest,RailingMining.Completion completion){
      var be=entity(level,pos);var lamp=be==null?null:be.attachment();
      if(lamp==null||lamp!=completion.part().token())return false;
      if(level.setBlock(pos,state.setValue(ATTACHED,false),Block.UPDATE_ALL)){
         be.clear();
         if(level.isClientSide)RailingMining.clientInsetLightRemoved(player,pos);
         else{
            if(harvest&&!player.getAbilities().instabuild&&lamp.paid){
               Block.dropResources(lamp.lamp,level,pos,null,player,completion.tool());player.awardStat(Stats.BLOCK_MINED.get(lamp.lamp.getBlock()));player.causeFoodExhaustion(.005F);
            }
            level.gameEvent(player,GameEvent.BLOCK_CHANGE,pos);
         }
      }
      return false;
   }
   static List<ItemStack> drops(BlockState state,LootParams.Builder params,List<ItemStack> original){
      if(!attached(state)||original.stream().noneMatch(s->s.is(state.getBlock().asItem())))return original;
      var be=params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
      var lamp=be instanceof DoubleTLightBlockEntity entity?entity.attachment():null;
      if(lamp==null||!lamp.paid)return original;
      var result=new ArrayList<>(original);result.add(new ItemStack(lamp.lamp.getBlock()));return result;
   }
   @SubscribeEvent(priority=EventPriority.LOWEST)
   public static void preventOverlap(BlockEvent.EntityPlaceEvent event){
      var level=event.getLevel();var pos=event.getPos();
      for(Direction direction:Direction.values()){
         BlockPos target=pos.relative(direction);var state=level.getBlockState(target);
         if(!(state.getBlock() instanceof DoubleTLightHost host))continue;
         var lamp=data(state,level,target);
         if(lamp!=null&&Shapes.joinIsNotEmpty(host.supportShape(state,level,target),lamp.shape(),BooleanOp.AND)){event.setCanceled(true);return;}
      }
   }
}
