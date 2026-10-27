package com.oblixorprime.engineersdecorreforged.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Reverse placement order: the pre-existing light is conserved, not dropped/rebought. */
public final class DoubleTSupportItem extends BlockItem {
   public DoubleTSupportItem(Block block,Properties properties){super(block,properties);}
   @Override public InteractionResult useOn(UseOnContext context){
      BlockPos target=context.getClickedPos();var old=context.getLevel().getBlockState(target);
      if(!SupportLights.supported(old.getBlock())){
         target=new BlockPlaceContext(context).getClickedPos();old=context.getLevel().getBlockState(target);
         if(!SupportLights.supported(old.getBlock()))return super.useOn(context);
      }
      if(!SupportLightItem.allowed(context)||!SupportLightItem.allowedAt(context,target))return InteractionResult.FAIL;
      var merged=new MergedContext(context,target,old);var state=getBlock().getStateForPlacement(merged);
      if(state==null)return InteractionResult.FAIL;
      var face=old.getValue(PortedBlocks.FACING).getOpposite();
      var plan=SupportLights.pose(state,context.getLevel(),target,old,face,true);
      if(plan==null&&old.is(com.oblixorprime.engineersdecorreforged.ModBlocks.IRON_CEILING_EDGE_LIGHT.get()))plan=SupportLights.pose(state,context.getLevel(),target,old,Direction.DOWN,true);
      if(plan==null)return InteractionResult.FAIL;
      merged.state=state.setValue(SupportLights.ATTACHED,true);merged.plan=plan;
      return super.place(merged);
   }
   @Override protected BlockState getPlacementState(BlockPlaceContext context){
      if(!(context instanceof MergedContext merged))return super.getPlacementState(context);
      return merged.canPlace()&&canPlace(context,merged.state)?merged.state:null;
   }
   @Override protected boolean placeBlock(BlockPlaceContext context,BlockState state){
      if(!(context instanceof MergedContext merged))return super.placeBlock(context,state);
      if(!merged.canPlace()||!super.placeBlock(context,state))return false;
      var be=SupportLights.entity(context.getLevel(),context.getClickedPos());
      if(be==null)throw new IllegalStateException("Missing Double T entity after lamp-first placement");
      be.install(merged.plan);return true;
   }
   @Override protected boolean updateCustomBlockEntityTag(BlockPos pos,Level level,Player player,ItemStack stack,BlockState state){
      return contextFreeMerge(state)?false:super.updateCustomBlockEntityTag(pos,level,player,stack,state);
   }
   private static boolean contextFreeMerge(BlockState state){return SupportLights.attached(state);}
   private static final class MergedContext extends BlockPlaceContext {
      final BlockState original;final BlockPos target;BlockState state;DoubleTLightBlockEntity.Attachment plan;
      MergedContext(UseOnContext context,BlockPos target,BlockState original){super(context);replaceClicked=true;this.target=target;this.original=original;}
      @Override public BlockPos getClickedPos(){return target==null?super.getClickedPos():target;}
      @Override public boolean canPlace(){return getLevel().getBlockState(getClickedPos())==original;}
   }
}
