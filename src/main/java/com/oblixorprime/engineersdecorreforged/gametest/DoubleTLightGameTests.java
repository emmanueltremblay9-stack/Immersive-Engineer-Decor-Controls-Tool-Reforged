package com.oblixorprime.engineersdecorreforged.gametest;

import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.*;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoubleTLightGameTests {
   private static final BlockPos P=new BlockPos(3,4,3);
   private static List<Block> lamps(){return List.of(ModBlocks.IRON_BULB_LIGHT.get(),ModBlocks.IRON_CEILING_EDGE_LIGHT.get(),ModBlocks.IRON_FLOOR_EDGE_LIGHT.get(),ModBlocks.IRON_INSET_LIGHT.get());}
   private static FakePlayer player(GameTestHelper h,BlockPos pos){
      var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"SupportLampTests"));p.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
      p.setPos(pos.getX()+.5,pos.getY()+.5-p.getEyeHeight(),pos.getZ()-2);p.setYRot(0);p.setXRot(0);return p;
   }
   private static void clear(GameTestHelper h,BlockPos pos){
      for(var q:BlockPos.betweenClosed(pos.offset(-1,-1,-1),pos.offset(1,1,1)))h.getLevel().setBlock(q,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
      for(var item:h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2)))item.discard();
   }
   private static BlockState wall(boolean across){return ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get().defaultBlockState().setValue(VerticalDoubleTSupportBlock.WALL_HORIZONTAL,across);}
   private static BlockHitResult front(BlockPos pos){return new BlockHitResult(Vec3.atLowerCornerOf(pos).add(.5,.5,11./16),Direction.NORTH,pos,false);}
   private static void use(FakePlayer player,BlockHitResult hit){player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));}
   private static int drops(GameTestHelper h,BlockPos pos,Block block){return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2)).stream().filter(e->e.getItem().is(block.asItem())).mapToInt(e->e.getItem().getCount()).sum();}

   @GameTest(template="empty",timeoutTicks=120)
   public static void four_lamp_items_attach_to_both_wall_mounts_preserving_fluid_facing_and_payment(GameTestHelper h){
      var level=h.getLevel();var pos=h.absolutePos(P);var p=player(h,pos);
      for(boolean across:List.of(false,true))for(Block lamp:lamps())for(int fluid=0;fluid<4;fluid++){
         clear(h,pos);var state=wall(across);
         if(fluid==1)state=NativeFluidloggedBlock.placement(state,Fluids.WATER.getSource(false));
         if(fluid==2)state=NativeFluidloggedBlock.placement(state,Fluids.LAVA.getSource(false));
         if(fluid==3)state=StairWaterFlow.withFlow(state,Fluids.FLOWING_WATER.getFlowing(4,false));
         level.setBlock(pos,state,Block.UPDATE_ALL);var base=state.getCollisionShape(level,pos);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(lamp,3));
         use(p,front(pos));var changed=level.getBlockState(pos);var be=SupportLights.entity(level,pos);var data=be==null?null:be.attachment();
         h.assertTrue(SupportLights.attached(changed)&&data!=null,"real existing item attaches in host cell");
         h.assertTrue(data.lamp.is(lamp)&&data.paid,"identity and paid ownership retained");
         h.assertValueEqual(data.lamp.getValue(PortedBlocks.FACING),Direction.SOUTH,"ordinary wall-facing rule is retained for each lamp");
         h.assertValueEqual(p.getMainHandItem().getCount(),2,"one paid once");h.assertValueEqual(changed.getFluidState(),state.getFluidState(),"only host owns original fluid");
         h.assertFalse(Shapes.joinIsNotEmpty(base,changed.getCollisionShape(level,pos),BooleanOp.NOT_SAME),"lamps add no collision");
         h.assertFalse(Shapes.joinIsNotEmpty(base,data.shape(),BooleanOp.AND),"light does not overlap structural metal");
         h.assertTrue(changed.getLightEmission(level,pos)>=14,"attachment emits without overriding brighter fluid");
         use(p,front(pos));h.assertValueEqual(p.getMainHandItem().getCount(),2,"occupied host cannot buy a second lamp");
         h.assertTrue(level.getBlockState(pos.north()).isAir(),"internal occupied slot must not fall back to a second adjacent lamp");
         h.assertTrue(SupportLights.entity(level,pos).attachment()==data,"rejected second attachment retains the original ownership token");
      }
      h.succeed();
   }

   @GameTest(template="empty",timeoutTicks=120)
   public static void lamp_first_native_beam_item_preserves_four_identities_and_natural_click_zones(GameTestHelper h){
      var level=h.getLevel();var pos=h.absolutePos(P);var p=player(h,pos);
      for(boolean across:List.of(false,true))for(Block lamp:lamps()){
         clear(h,pos);var old=lamp.defaultBlockState().setValue(PortedBlocks.FACING,Direction.SOUTH);level.setBlock(pos,old,Block.UPDATE_ALL);
         level.setBlock(pos.south(),Blocks.BRICKS.defaultBlockState(),Block.UPDATE_ALL);
         p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get(),3));
         var point=Vec3.atLowerCornerOf(pos).add(across?.1:.3,.5,1);
         use(p,new BlockHitResult(point,Direction.NORTH,pos.south(),false));var state=level.getBlockState(pos);var be=SupportLights.entity(level,pos);
         h.assertTrue(state.is(ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get())&&SupportLights.attached(state),"beam combines with existing lamp in intended target cell");
         h.assertValueEqual(state.getValue(VerticalDoubleTSupportBlock.WALL_HORIZONTAL),across,"ordinary wall click zones apply even with light-first order");
         h.assertTrue(be.attachment().lamp.is(lamp)&&be.attachment().paid,"pre-existing light value conserved");
         h.assertValueEqual(p.getMainHandItem().getCount(),2,"one beam consumed; light not rebought");h.assertValueEqual(drops(h,pos,lamp),0,"no dropped copy of the merged lamp");
      }
      h.succeed();
   }

   @GameTest(template="empty",timeoutTicks=120)
   public static void support_lamp_snapshot_cancellation_restores_original_data_and_items_both_orders(GameTestHelper h){
      var level=h.getLevel();var pos=h.absolutePos(P);var p=player(h,pos);
      for(boolean reverse:List.of(false,true))for(Block lamp:lamps()){
         clear(h,pos);var old=reverse?lamp.defaultBlockState().setValue(PortedBlocks.FACING,Direction.SOUTH):NativeFluidloggedBlock.placement(wall(false),Fluids.WATER.getSource(false));
         level.setBlock(pos,old,Block.UPDATE_ALL);level.setBlock(pos.south(),Blocks.BRICKS.defaultBlockState(),Block.UPDATE_ALL);
         p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(reverse?ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get():lamp,3));boolean[] fired={false};
         Consumer<BlockEvent.EntityPlaceEvent> listener=e->{if(e.getEntity()==p&&e.getPos().equals(pos)){fired[0]=true;e.setCanceled(true);}};
         NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,BlockEvent.EntityPlaceEvent.class,listener);
         try{use(p,reverse?new BlockHitResult(Vec3.atLowerCornerOf(pos).add(.5,.5,1),Direction.NORTH,pos.south(),false):front(pos));}
         finally{NeoForge.EVENT_BUS.unregister(listener);}
         h.assertTrue(fired[0],"normal NeoForge event covers actual target cell");h.assertValueEqual(level.getBlockState(pos),old,"full previous block/fluid restored");
         h.assertValueEqual(p.getMainHandItem().getCount(),3,"no cancellation cost");var be=SupportLights.entity(level,pos);h.assertTrue(be==null||!be.occupied(),"rollback cannot retain a paid ghost attachment");
      }
      h.succeed();
   }

   private static void ordinaryAdjacent(GameTestHelper h,FakePlayer p,BlockPos pos,Block lamp,Direction face,Vec3 point){
      var level=h.getLevel();var before=level.getBlockState(pos);var owner=SupportLights.entity(level,pos);
      var data=owner==null?null:owner.attachment();var target=pos.relative(face);
      h.assertTrue(level.getBlockState(target).isAir(),"ordinary adjacent target starts empty");
      p.setYRot(0);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(lamp,3));
      var context=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(point,face,pos,false));
      var ordinary=lamp.getStateForPlacement(new net.minecraft.world.item.context.BlockPlaceContext(context));
      h.assertTrue(ordinary!=null,"original lamp permits its ordinary placement");
      p.getMainHandItem().useOn(context);
      h.assertValueEqual(level.getBlockState(target),ordinary,"occupied support preserves ordinary adjacent lamp placement/facing; host="+before+" face="+face);
      h.assertValueEqual(p.getMainHandItem().getCount(),2,"only the ordinary lamp is paid once");
      h.assertValueEqual(level.getBlockState(pos),before,"existing support/lamp/fluid state is retained");
      h.assertTrue(SupportLights.entity(level,pos)==owner&&owner.attachment()==data,"ordinary placement does not replace existing lamp data");
      h.assertValueEqual(drops(h,pos,lamp),0,"ordinary placement emits no duplicate lamp item");
   }

   @GameTest(template="empty",timeoutTicks=120)
   public static void occupied_supports_keep_ordinary_boundary_face_placement(GameTestHelper h){
      var level=h.getLevel();var pos=h.absolutePos(P);var p=player(h,pos);
      for(boolean across:List.of(false,true))for(Block lamp:lamps()){
         clear(h,pos);level.setBlock(pos,NativeFluidloggedBlock.placement(wall(across),Fluids.WATER.getSource(false)),Block.UPDATE_ALL);
         p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModBlocks.IRON_INSET_LIGHT.get()));use(p,front(pos));
         h.assertTrue(SupportLights.attached(level.getBlockState(pos)),"first paid attachment established");
         if(across&&lamp==ModBlocks.IRON_BULB_LIGHT.get()){
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(lamp,3));
            use(p,new BlockHitResult(Vec3.atLowerCornerOf(pos).add(.5,11./16,15.5/16),Direction.UP,pos,false));
            h.assertValueEqual(p.getMainHandItem().getCount(),3,"top of a horizontal wall beam is an occupied internal mount, not the cell boundary");
            h.assertTrue(level.getBlockState(pos.above()).isAir(),"occupied internal top mount does not place an adjacent bulb");
         }
         var face=across?Direction.EAST:Direction.UP;
         var state=level.getBlockState(pos);
         h.assertValueEqual(((DoubleTLightHost)state.getBlock()).mountingShape(state,level,pos).max(face.getAxis()),1.,"ordinary test clicks an actual cell-boundary structural face");
         ordinaryAdjacent(h,p,pos,lamp,face,Vec3.atLowerCornerOf(pos).add(across?1:.5,across?.5:1,15.5/16));
      }
      for(boolean eastWest:List.of(false,true))for(Block lamp:lamps()){
         clear(h,pos);var state=ModBlocks.STEEL_DOUBLE_T_SUPPORT.get().defaultBlockState().setValue(PortedBlocks.EASTWEST,eastWest).setValue(SteelDoubleTBulbBlock.BULB_LIGHT,SteelDoubleTBulbBlock.Bulb.PAID);
         level.setBlock(pos.above(),Blocks.BRICKS.defaultBlockState(),Block.UPDATE_ALL);level.setBlock(pos,state,Block.UPDATE_ALL);
         var face=eastWest?Direction.EAST:Direction.SOUTH;
         ordinaryAdjacent(h,p,pos,lamp,face,Vec3.atLowerCornerOf(pos).add(eastWest?1:.5,15.5/16,eastWest?.5:1));
      }
      h.succeed();
   }

   @GameTest(template="empty",timeoutTicks=120)
   public static void occupied_supports_keep_incompatible_face_and_legacy_bulb_normal_placement(GameTestHelper h){
      var level=h.getLevel();var pos=h.absolutePos(P);var p=player(h,pos);
      for(boolean across:List.of(false,true))for(Block lamp:List.of(ModBlocks.IRON_CEILING_EDGE_LIGHT.get(),ModBlocks.IRON_FLOOR_EDGE_LIGHT.get())){
         clear(h,pos);level.setBlock(pos,wall(across),Block.UPDATE_ALL);
         p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModBlocks.IRON_INSET_LIGHT.get()));use(p,front(pos));
         ordinaryAdjacent(h,p,pos,lamp,Direction.WEST,Vec3.atLowerCornerOf(pos).add(5./16,.5,15.5/16));
      }
      for(boolean eastWest:List.of(false,true))for(Block lamp:List.of(ModBlocks.IRON_CEILING_EDGE_LIGHT.get(),ModBlocks.IRON_FLOOR_EDGE_LIGHT.get(),ModBlocks.IRON_INSET_LIGHT.get())){
         clear(h,pos);var state=ModBlocks.STEEL_DOUBLE_T_SUPPORT.get().defaultBlockState().setValue(PortedBlocks.EASTWEST,eastWest).setValue(SteelDoubleTBulbBlock.BULB_LIGHT,SteelDoubleTBulbBlock.Bulb.PAID);
         level.setBlock(pos.above(),Blocks.BRICKS.defaultBlockState(),Block.UPDATE_ALL);level.setBlock(pos,state,Block.UPDATE_ALL);
         ordinaryAdjacent(h,p,pos,lamp,Direction.DOWN,Vec3.atLowerCornerOf(pos).add(eastWest?.1:.5,11./16,eastWest?.5:.1));
         h.assertTrue(SteelDoubleTBulbBlock.paidBulb(level.getBlockState(pos)),"legacy paid bulb remains independent and unchanged");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void four_lamps_keep_original_horizontal_support_and_legacy_bulb_contract(GameTestHelper h){
      var level=h.getLevel();var pos=h.absolutePos(P);var p=player(h,pos);
      for(boolean eastWest:List.of(false,true))for(Block lamp:lamps()){
         clear(h,pos);var before=ModBlocks.STEEL_DOUBLE_T_SUPPORT.get().defaultBlockState().setValue(PortedBlocks.EASTWEST,eastWest);level.setBlock(pos,before,Block.UPDATE_ALL);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(lamp,3));
         boolean floor=lamp==ModBlocks.IRON_FLOOR_EDGE_LIGHT.get();p.setYRot(floor?(eastWest?180:90):eastWest?90:0);
         var hit=new BlockHitResult(Vec3.atLowerCornerOf(pos).add(floor&&!eastWest?11./16:.5,11./16,floor&&eastWest?11./16:.5),floor?(eastWest?Direction.SOUTH:Direction.EAST):Direction.DOWN,pos,false);use(p,hit);
         var state=level.getBlockState(pos);if(lamp==ModBlocks.IRON_BULB_LIGHT.get())h.assertTrue(SteelDoubleTBulbBlock.paidBulb(state)&&!SupportLights.attached(state),"old paid-bulb state remains the exact legacy path");
         else h.assertTrue(SupportLights.attached(state)&&SupportLights.entity(level,pos).attachment().lamp.is(lamp),"other identities attach without becoming bulbs");
         h.assertValueEqual(p.getMainHandItem().getCount(),2,"one lamp paid");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void support_lamp_save_update_tag_and_unknown_payload_preserve_data(GameTestHelper h){
      var level=h.getLevel();var pos=h.absolutePos(P);var p=player(h,pos);
      for(Block lamp:lamps()){
         clear(h,pos);level.setBlock(pos,wall(true),Block.UPDATE_ALL);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(lamp));use(p,front(pos));
         var be=SupportLights.entity(level,pos);var data=be.attachment();h.assertTrue(be.getModelData().get(DoubleTLightBlockEntity.MODEL_ATTACHMENT)==data,"standard model data carries the immutable attachment, not a renderer/world reference");var tag=be.saveWithoutMetadata(level.registryAccess());
         var copy=new DoubleTLightBlockEntity(pos,level.getBlockState(pos));copy.loadWithComponents(tag,level.registryAccess());
         h.assertTrue(copy.attachment().lamp==data.lamp&&copy.attachment().paid,"saved identity/facing/payment exactly restored");h.assertValueEqual(copy.attachment().offset,data.offset,"saved rigid translation restored");
         var client=new DoubleTLightBlockEntity(pos,level.getBlockState(pos));client.handleUpdateTag(be.getUpdateTag(level.registryAccess()),level.registryAccess());
         h.assertTrue(client.attachment().lamp==data.lamp&&client.attachment().paid,"minimal sync data retains same attachment");
         var unknown=tag.copy();unknown.getCompound("SupportLamp").putInt("Version",999);copy.loadWithComponents(unknown,level.registryAccess());
         h.assertTrue(copy.attachment()==null&&copy.occupied(),"unknown payload cannot mint lamp items");
         h.assertValueEqual(copy.saveWithoutMetadata(level.registryAccess()).getCompound("SupportLamp"),unknown.getCompound("SupportLamp"),"unknown payload survives unchanged");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void native_selective_support_lamp_mining_returns_one_and_keeps_beam(GameTestHelper h){
      var level=h.getLevel();var pos=h.absolutePos(P);var p=player(h,pos);
      for(Block lamp:lamps()){
         clear(h,pos);level.setBlock(pos,wall(false),Block.UPDATE_ALL);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(lamp));use(p,front(pos));
         var data=SupportLights.entity(level,pos).attachment();var centre=data.shape().bounds().getCenter();
         p.setPos(pos.getX()+centre.x,pos.getY()+centre.y-p.getEyeHeight(),pos.getZ()-2);p.setYRot(0);p.setXRot(0);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));
         var eye=p.getEyePosition();var part=PortedBlocks.selectRailingPart(level,pos,eye,eye.add(p.getLookAngle().scale(p.blockInteractionRange())),p);
         h.assertTrue(part!=null&&part.kind()==PortedBlocks.PartKind.SUPPORT_LIGHT,"native eye ray selects actual translated lamp before metal");
         p.gameMode.handleBlockBreakAction(pos,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.NORTH,level.getMaxBuildHeight(),0);
         p.gameMode.destroyBlock(pos); // The native method reports an accepted operation, not whole-block removal.
         h.assertTrue(level.getBlockState(pos).is(ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get())&&!SupportLights.attached(level.getBlockState(pos)),"support remains unlit");
         h.assertValueEqual(drops(h,pos,lamp),1,"one paid original lamp returned");
         p.gameMode.destroyBlock(pos);h.assertTrue(level.getBlockState(pos).is(ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get()),"stale completion cannot turn partial removal into beam removal");
         h.assertValueEqual(drops(h,pos,lamp),1,"stale completion cannot refund twice");h.assertValueEqual(drops(h,pos,ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get()),0,"no beam duplication");
      }
      h.succeed();
   }
   @GameTest(template="empty")
   public static void whole_support_native_loot_refunds_paid_lamps_once_and_never_creative_additions(GameTestHelper h){
      var level=h.getLevel();var pos=h.absolutePos(P);var p=player(h,pos);
      for(boolean across:List.of(false,true))for(Block lamp:lamps())for(boolean creative:List.of(false,true)){
         clear(h,pos);level.setBlock(pos,wall(across),Block.UPDATE_ALL);p.gameMode.changeGameModeForPlayer(creative?GameType.CREATIVE:GameType.SURVIVAL);
         p.setYRot(0);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(lamp,3));use(p,front(pos));
         h.assertValueEqual(p.getMainHandItem().getCount(),creative?3:2,"creative use does not buy a paid refund");
         p.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));
         p.setPos(pos.getX()+(across?.1:.5),pos.getY()+(across?.5:.2)-p.getEyeHeight(),pos.getZ()-2);p.setYRot(0);p.setXRot(0);
         var eye=p.getEyePosition();var part=PortedBlocks.selectRailingPart(level,pos,eye,eye.add(p.getLookAngle().scale(p.blockInteractionRange())),p);
         h.assertTrue(part!=null&&part.kind()==PortedBlocks.PartKind.BASE,"clear native ray deliberately selects structural metal");
         p.gameMode.handleBlockBreakAction(pos,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.NORTH,level.getMaxBuildHeight(),0);p.gameMode.destroyBlock(pos);
         h.assertTrue(level.getBlockState(pos).isAir(),"whole support removed normally");
         h.assertValueEqual(drops(h,pos,ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get()),1,"one support item");
         h.assertValueEqual(drops(h,pos,lamp),creative?0:1,"only one genuinely paid lamp is returned");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void four_lamps_ordinary_block_placement_keeps_six_face_and_player_orientation_rules(GameTestHelper h){
      var level=h.getLevel();var pos=h.absolutePos(P);var p=player(h,pos);
      for(Block lamp:lamps())for(Direction face:Direction.values()){
         clear(h,pos);var support=pos.relative(face.getOpposite());level.setBlock(support,Blocks.BRICKS.defaultBlockState(),Block.UPDATE_ALL);p.setYRot(0);
         p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(lamp,3));
         var hit=new BlockHitResult(Vec3.atCenterOf(support).add(Vec3.atLowerCornerOf(face.getNormal()).scale(.5)),face,support,false);use(p,hit);
         var state=level.getBlockState(pos);h.assertTrue(state.is(lamp),"ordinary block placement stays ordinary, not a Double T slot");
         Direction expected=lamp==ModBlocks.IRON_BULB_LIGHT.get()||lamp==ModBlocks.IRON_INSET_LIGHT.get()?face.getOpposite():Direction.SOUTH;
         h.assertValueEqual(state.getValue(PortedBlocks.FACING),expected,"original clicked-face versus player-direction rule preserved");h.assertValueEqual(p.getMainHandItem().getCount(),2,"ordinary placement cost unchanged");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void attached_lamp_tracks_native_host_rotations_and_keeps_the_saved_pose_frame(GameTestHelper h){
      var level=h.getLevel();var pos=h.absolutePos(P);var p=player(h,pos);
      for(boolean across:List.of(false,true))for(Block lamp:lamps()){
         clear(h,pos);level.setBlock(pos,wall(across),Block.UPDATE_ALL);p.setYRot(0);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(lamp));use(p,front(pos));
         var be=SupportLights.entity(level,pos);var before=be.attachment();var state=level.getBlockState(pos).rotate(net.minecraft.world.level.block.Rotation.CLOCKWISE_90);level.setBlock(pos,state,Block.UPDATE_ALL);
         var changed=be.attachment();h.assertValueEqual(changed.offset,new Vec3(-before.offset.z,before.offset.y,before.offset.x),"rigid translation rotates with the support");
         h.assertValueEqual(changed.lamp.getValue(PortedBlocks.FACING),Direction.WEST,"original lamp orientation rotates with the wall frame");
         var copy=new DoubleTLightBlockEntity(pos,state);copy.loadWithComponents(be.saveWithoutMetadata(level.registryAccess()),level.registryAccess());
         h.assertValueEqual(copy.attachment().offset,changed.offset,"rotated saved pose frame resolves after reload");h.assertTrue(copy.attachment().lamp==changed.lamp,"rotated light identity/facing restores");
         h.assertFalse(Shapes.joinIsNotEmpty(state.getCollisionShape(level,pos),changed.shape(),BooleanOp.AND),"native rotation introduces no beam overlap");
      }
      h.succeed();
   }

}
