package com.oblixorprime.engineersdecorreforged.gametest;

import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import com.oblixorprime.engineersdecorreforged.block.RaisedCatwalkStairsBlock;
import com.oblixorprime.engineersdecorreforged.block.RaisedCatwalkStairsBlock.Support;
import com.oblixorprime.engineersdecorreforged.block.StairWaterFlow;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RaisedStairVGameTests {
   private static final BlockPos CENTER = new BlockPos(3,4,3);
   private RaisedStairVGameTests() {}

   @GameTest(template="empty")
   public static void raised_stair_v_native_item_placement_preserves_independent_poles(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);
      var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"VStairPlacement"));
      player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
      player.setPos(pos.getX()+3.5,pos.getY()+2,pos.getZ()+3.5);
      for(var pole:List.of(ModBlocks.TREATED_WOOD_POLE.get(),ModBlocks.THIN_STEEL_POLE.get(),ModBlocks.THICK_STEEL_POLE.get())) {
         for(boolean poleFirst:new boolean[]{true,false}) for(Direction facing:Direction.Plane.HORIZONTAL) {
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
            BlockState below=pole.defaultBlockState().setValue(DirectionalBlock.FACING,Direction.UP);
            level.setBlock(pos.below(),poleFirst?below:Blocks.BRICKS.defaultBlockState(),Block.UPDATE_ALL);
            var stack=new ItemStack(ModBlocks.RAISED_STEEL_CATWALK_STAIRS.get(),2);
            player.setItemInHand(InteractionHand.MAIN_HAND,stack);player.setYRot(facing.toYRot());
            var hit=new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0,.5,0),Direction.UP,pos.below(),false);
            var context=new BlockPlaceContext(level,player,InteractionHand.MAIN_HAND,stack,hit);
            h.assertTrue(((BlockItem)stack.getItem()).place(context).consumesAction(),"registered item places on the supporting cell");
            h.assertTrue(level.getBlockState(pos).is(ModBlocks.RAISED_STEEL_CATWALK_STAIRS.get()),"new stair uses its own registry ID");
            h.assertValueEqual(level.getBlockState(pos).getValue(PortedBlocks.HORIZONTAL_FACING),facing,"uphill orientation follows original placement rule");
            h.assertValueEqual(stack.getCount(),1,"one stair item consumed; pole never consumed");
            if(!poleFirst)level.setBlock(pos.below(),below,Block.UPDATE_ALL);
            h.assertValueEqual(level.getBlockState(pos.below()),below,"pole remains a separate unchanged block");
            h.assertValueEqual(RaisedCatwalkStairsBlock.support(below),pole==ModBlocks.TREATED_WOOD_POLE.get()?Support.WOOD:Support.STEEL,"material matches actual pole");
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
            h.assertValueEqual(level.getBlockState(pos.below()),below,"stair removal leaves pole intact");
         }
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void raised_stair_v_exact_platform_material_contract_and_negatives(GameTestHelper h) {
      for(var pole:List.of(ModBlocks.TREATED_WOOD_POLE.get(),ModBlocks.TREATED_WOOD_POLE_HEAD.get(),
            ModBlocks.THIN_STEEL_POLE.get(),ModBlocks.THIN_STEEL_POLE_HEAD.get(),ModBlocks.THICK_STEEL_POLE.get(),ModBlocks.THICK_STEEL_POLE_HEAD.get())) {
         boolean wood=pole==ModBlocks.TREATED_WOOD_POLE.get()||pole==ModBlocks.TREATED_WOOD_POLE_HEAD.get();
         for(Direction axis:Direction.values())h.assertValueEqual(RaisedCatwalkStairsBlock.support(pole.defaultBlockState().setValue(DirectionalBlock.FACING,axis)),wood?Support.WOOD:Support.STEEL,"same exact identity classification as platform, without new orientation rules");
      }
      for(Direction axis:Direction.values())h.assertValueEqual(RaisedCatwalkStairsBlock.support(ModBlocks.TREATED_WOOD_POLE_SUPPORT.get().defaultBlockState().setValue(DirectionalBlock.FACING,axis)),axis==Direction.UP?Support.WOOD:Support.NONE,"straight mount has the established UP-only guard");
      for(var block:List.of(Blocks.AIR,Blocks.OAK_FENCE,Blocks.IRON_BLOCK,ModBlocks.STEEL_CATWALK_TA.get()))
         h.assertValueEqual(RaisedCatwalkStairsBlock.support(block.defaultBlockState()),Support.NONE,"unrecognized supports do not create a V");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void raised_stair_v_replacement_and_loss_never_rewrites_fluid_or_rail_credits(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);
      BlockState stair=ModBlocks.RAISED_STEEL_CATWALK_STAIRS.get().defaultBlockState()
         .setValue(PortedBlocks.LEFT_RAILING,true).setValue(PortedBlocks.EDITED_STAIR_SIDES,1).setValue(PortedBlocks.PAID_RAILINGS,1);
      stair=NativeFluidloggedBlock.placement(stair,Fluids.LAVA.getSource(false));
      level.setBlock(pos,stair,Block.UPDATE_ALL);
      for(var block:List.of(ModBlocks.TREATED_WOOD_POLE.get(),ModBlocks.THIN_STEEL_POLE.get(),ModBlocks.THICK_STEEL_POLE.get(),Blocks.AIR,Blocks.STONE)) {
         level.setBlock(pos.below(),block.defaultBlockState(),Block.UPDATE_ALL);
         h.assertValueEqual(level.getBlockState(pos),stair,"appearance-only support changes retain every saved stair property");
         h.assertTrue(level.getFluidState(pos).getType().isSame(Fluids.LAVA),"support changes preserve exterior fluid");
      }
      var loaded=NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),NbtUtils.writeBlockState(stair));
      h.assertValueEqual(loaded,stair,"new registry state and credits roundtrip without migration");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void raised_stair_v_keeps_original_stair_shapes_and_saved_schema(GameTestHelper h) {
      var block=ModBlocks.RAISED_STEEL_CATWALK_STAIRS.get();var original=ModBlocks.STEEL_CATWALK_STAIRS.get();
      h.assertValueEqual(block.getStateDefinition().getProperties(),original.getStateDefinition().getProperties(),"no additional appearance BlockStates or pole ownership field");
      h.assertValueEqual(block.getStateDefinition().getPossibleStates().size(),original.getStateDefinition().getPossibleStates().size(),"material does not multiply state enumeration");
      for(Direction facing:Direction.Plane.HORIZONTAL)for(boolean left:new boolean[]{false,true})for(boolean right:new boolean[]{false,true}) {
         var state=block.defaultBlockState().setValue(PortedBlocks.HORIZONTAL_FACING,facing).setValue(PortedBlocks.LEFT_RAILING,left).setValue(PortedBlocks.RIGHT_RAILING,right);
         var old=original.withPropertiesOf(state);var pos=h.absolutePos(CENTER);
         h.assertFalse(Shapes.joinIsNotEmpty(state.getCollisionShape(h.getLevel(),pos),old.getCollisionShape(h.getLevel(),pos),BooleanOp.NOT_SAME),"walking/headroom and railing collision unchanged");
         h.assertFalse(Shapes.joinIsNotEmpty(state.getShape(h.getLevel(),pos),old.getShape(h.getLevel(),pos),BooleanOp.NOT_SAME),"original stair selection remains authoritative");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void raised_stair_v_real_source_and_flow_placement_keep_fluid_identity(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);
      for(var fluid:List.of(Fluids.WATER.getSource(false),Fluids.LAVA.getSource(false),Fluids.FLOWING_WATER.getFlowing(4,false))) {
         var state=fluid.isSource()?NativeFluidloggedBlock.placement(ModBlocks.RAISED_STEEL_CATWALK_STAIRS.get().defaultBlockState(),fluid):StairWaterFlow.withFlow(ModBlocks.RAISED_STEEL_CATWALK_STAIRS.get().defaultBlockState(),fluid);
         level.setBlock(pos,state,Block.UPDATE_ALL);
         h.assertValueEqual(level.getFluidState(pos),fluid,"explicit existing fluidlogging state preserved by new stair class");
         h.assertValueEqual(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),NbtUtils.writeBlockState(state)),state,"all source/current selectors save exactly");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void raised_stair_v_native_mining_drops_one_stair_without_consuming_pole(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);
      var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"VStairMining"));
      player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
      player.setPos(pos.getX()+2.5,pos.getY()+2,pos.getZ()+2.5);
      player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));
      var pole=ModBlocks.TREATED_WOOD_POLE.get().defaultBlockState().setValue(DirectionalBlock.FACING,Direction.UP);
      level.setBlock(pos.below(),pole,Block.UPDATE_ALL);
      level.setBlock(pos,ModBlocks.RAISED_STEEL_CATWALK_STAIRS.get().defaultBlockState(),Block.UPDATE_ALL);
      player.setPos(pos.getX()+.5,pos.getY()+2.5-player.getEyeHeight(),pos.getZ()+.25);
      player.setYRot(0);player.setXRot(90);
      var eye=player.getEyePosition();
      var hit=level.clip(new net.minecraft.world.level.ClipContext(eye,eye.add(player.getViewVector(1).scale(player.blockInteractionRange())),
         net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.NONE,player));
      h.assertValueEqual(hit.getBlockPos(),pos,"native eye ray selects the actual stair deck");
      player.gameMode.handleBlockBreakAction(pos,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,hit.getDirection(),level.getMaxBuildHeight(),0);
      h.assertTrue(player.gameMode.destroyBlock(pos),"native survival mining succeeds with pickaxe");
      h.assertTrue(level.getBlockState(pos).isAir(),"only stair removed");
      h.assertValueEqual(level.getBlockState(pos.below()),pole,"native mining leaves independent pole unchanged");
      int count=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(1)).stream()
         .filter(e->e.getItem().is(ModBlocks.RAISED_STEEL_CATWALK_STAIRS.get().asItem())).mapToInt(e->e.getItem().getCount()).sum();
      h.assertValueEqual(count,1,"exactly one stair item, no duplicate support refund");
      h.succeed();
   }
}
