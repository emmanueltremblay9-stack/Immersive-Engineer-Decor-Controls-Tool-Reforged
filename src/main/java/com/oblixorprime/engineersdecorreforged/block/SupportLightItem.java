package com.oblixorprime.engineersdecorreforged.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Native BlockItem snapshot/payment transaction, with ordinary placement unchanged. */
public class SupportLightItem extends BlockItem {
   public SupportLightItem(Block block,Properties properties){super(block,properties);}
   @Override public InteractionResult useOn(UseOnContext context){
      var level=context.getLevel();var pos=context.getClickedPos();var host=level.getBlockState(pos);
      if(!(host.getBlock() instanceof DoubleTLightHost))return super.useOn(context);
      var ordinary=getBlock().getStateForPlacement(new BlockPlaceContext(context));
      if(ordinary==null)return super.useOn(context);
      var player=context.getPlayer();var plan=SupportLights.pose(host,level,pos,ordinary,context.getClickedFace(),player==null||!player.getAbilities().instabuild);
      if(plan==null)return super.useOn(context);
      // Occupancy rejects an internal attachment, not ordinary adjacent placement.
      if(SupportLights.attached(host)||SupportLights.legacy(host))return InteractionResult.FAIL;
      if(!allowed(context))return InteractionResult.FAIL;
      return super.place(new AttachedContext(context,host,plan));
   }
   static boolean allowed(UseOnContext context){
      return allowedAt(context,context.getClickedPos());
   }
   static boolean allowedAt(UseOnContext context,BlockPos pos){
      var level=context.getLevel();var player=context.getPlayer();
      return !level.isOutsideBuildHeight(pos)&&level.getWorldBorder().isWithinBounds(pos)
         &&(player==null||!player.isSpectator()&&level.mayInteract(player,pos)&&player.mayUseItemAt(pos,context.getClickedFace(),context.getItemInHand()));
   }
   @Override protected BlockState getPlacementState(BlockPlaceContext context){
      if(!(context instanceof AttachedContext attached))return super.getPlacementState(context);
      var changed=attached.original.setValue(SupportLights.ATTACHED,true);
      return attached.canPlace()&&canPlace(context,changed)?changed:null;
   }
   @Override protected boolean placeBlock(BlockPlaceContext context,BlockState state){
      if(!(context instanceof AttachedContext attached))return super.placeBlock(context,state);
      if(!attached.canPlace()||!super.placeBlock(context,state))return false;
      var be=SupportLights.entity(context.getLevel(),context.getClickedPos());
      if(be==null)throw new IllegalStateException("Missing registered Double T attachment entity");
      be.install(attached.plan);return true;
   }
   @Override protected boolean updateCustomBlockEntityTag(BlockPos pos,Level level,Player player,ItemStack stack,BlockState state){
      return state.getBlock() instanceof DoubleTLightHost?false:super.updateCustomBlockEntityTag(pos,level,player,stack,state);
   }
   private static final class AttachedContext extends BlockPlaceContext {
      final BlockState original;final DoubleTLightBlockEntity.Attachment plan;
      AttachedContext(UseOnContext context,BlockState original,DoubleTLightBlockEntity.Attachment plan){super(context);replaceClicked=true;this.original=original;this.plan=plan;}
      @Override public boolean canPlace(){
         var be=SupportLights.entity(getLevel(),getClickedPos());
         return getLevel().getBlockState(getClickedPos())==original&&!SupportLights.attached(original)&&!SupportLights.legacy(original)&&(be==null||!be.occupied());
      }
   }
}
