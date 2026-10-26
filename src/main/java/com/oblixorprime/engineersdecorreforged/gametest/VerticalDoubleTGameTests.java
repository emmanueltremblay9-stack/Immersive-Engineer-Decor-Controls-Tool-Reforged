package com.oblixorprime.engineersdecorreforged.gametest;

import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import com.oblixorprime.engineersdecorreforged.block.StairWaterFlow;
import com.oblixorprime.engineersdecorreforged.block.VerticalDoubleTSupportBlock;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VerticalDoubleTGameTests {
   private static final BlockPos CENTER=new BlockPos(3,3,3);
   private static FakePlayer player(GameTestHelper h,BlockPos pos) {
      var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"VerticalBeamTests"));
      p.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);p.setPos(pos.getX()+3,pos.getY()+2,pos.getZ()+3);return p;
   }
   private static void clear(GameTestHelper h,BlockPos pos) {
      for(Direction d:Direction.values())h.getLevel().setBlock(pos.relative(d),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
      h.getLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
   }
   private static BlockHitResult hit(BlockPos pos,Direction face) {
      var wall=pos.relative(face.getOpposite());return new BlockHitResult(Vec3.atCenterOf(wall).add(Vec3.atLowerCornerOf(face.getNormal()).scale(.5)),face,wall,false);
   }

   @GameTest(template="empty")
   public static void vertical_double_t_native_wall_item_placement_all_faces_and_hands(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);var player=player(h,pos);
      for(Direction face:Direction.Plane.HORIZONTAL)for(InteractionHand hand:InteractionHand.values()) {
         clear(h,pos);level.setBlock(pos.relative(face.getOpposite()),Blocks.BRICKS.defaultBlockState(),Block.UPDATE_ALL);
         player.setYRot(face.getOpposite().toYRot());var stack=new ItemStack(ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get(),2);player.setItemInHand(hand,stack);
         h.assertTrue(stack.useOn(new net.minecraft.world.item.context.UseOnContext(player,hand,hit(pos,face))).consumesAction(),"normal item path succeeds on clicked wall");
         var state=level.getBlockState(pos);h.assertTrue(state.is(ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get()),"places in adjacent target cell");
         h.assertValueEqual(state.getValue(VerticalDoubleTSupportBlock.FACING),face,"clicked wall normal, independent of player look");
         h.assertValueEqual(stack.getCount(),1,"one item consumed, no duplicate");
         h.assertTrue(level.getBlockState(pos.relative(face.getOpposite())).is(Blocks.BRICKS),"wall not replaced");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void vertical_double_t_rejects_floor_ceiling_and_occupied_targets(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);var player=player(h,pos);
      for(Direction face:List.of(Direction.UP,Direction.DOWN)) {
         clear(h,pos);level.setBlock(pos.relative(face.getOpposite()),Blocks.STONE.defaultBlockState(),Block.UPDATE_ALL);
         var stack=new ItemStack(ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get(),2);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
         var ctx=new BlockPlaceContext(level,player,InteractionHand.MAIN_HAND,stack,hit(pos,face));
         h.assertTrue(ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get().getStateForPlacement(ctx)==null,"wall-only item has no vertical clicked face fallback");
         ((BlockItem)stack.getItem()).place(ctx);h.assertTrue(level.getBlockState(pos).isAir(),"no unintended centred pillar");h.assertValueEqual(stack.getCount(),2,"failed placement costs nothing");
      }
      clear(h,pos);level.setBlock(pos.south(),Blocks.STONE.defaultBlockState(),Block.UPDATE_ALL);level.setBlock(pos,Blocks.STONE.defaultBlockState(),Block.UPDATE_ALL);
      var stack=new ItemStack(ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get(),2);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
      stack.useOn(new net.minecraft.world.item.context.UseOnContext(player,InteractionHand.MAIN_HAND,hit(pos,Direction.NORTH)));
      h.assertTrue(level.getBlockState(pos).is(Blocks.STONE),"occupied target not overwritten");h.assertValueEqual(stack.getCount(),2,"occupied target costs nothing");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void vertical_double_t_shapes_are_exact_rotated_four_box_profile(GameTestHelper h) {
      var pos=h.absolutePos(CENTER);var base=ModBlocks.STEEL_DOUBLE_T_SUPPORT.get().defaultBlockState().getCollisionShape(h.getLevel(),pos);
      var expected=Shapes.empty();for(var b:base.toAabbs())expected=Shapes.or(expected,Shapes.create(b.minX,1-b.maxZ,b.minY,b.maxX,1-b.minZ,b.maxY));
      for(Direction face:List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST)) {
         var state=ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get().defaultBlockState().setValue(VerticalDoubleTSupportBlock.FACING,face);
         var shape=state.getCollisionShape(h.getLevel(),pos);h.assertFalse(Shapes.joinIsNotEmpty(shape,expected,BooleanOp.NOT_SAME),"profile is a rigid rotation of original collision");
         h.assertFalse(Shapes.joinIsNotEmpty(shape,state.getShape(h.getLevel(),pos),BooleanOp.NOT_SAME),"outline and collision agree");
         var box=shape.bounds();double plane=switch(face){case NORTH->box.maxZ;case EAST->box.minX;case SOUTH->box.minZ;default->box.maxX;};
         h.assertValueEqual(plane,face==Direction.NORTH||face==Direction.WEST?1.:0.,"back flange touches clicked wall boundary without gap");
         h.assertValueEqual(box.minY,0.,"vertical length starts at cell floor");h.assertValueEqual(box.maxY,1.,"vertical length is exactly one block");
         var next=Shapes.empty();for(var b:expected.toAabbs())next=Shapes.or(next,Shapes.create(1-b.maxZ,b.minY,b.minX,1-b.minZ,b.maxY,b.maxX));expected=next;
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void vertical_double_t_fluids_rotations_and_state_saving_preserve_identity(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);
      for(Direction face:Direction.Plane.HORIZONTAL)for(var fluid:List.of(Fluids.WATER.getSource(false),Fluids.LAVA.getSource(false),Fluids.FLOWING_WATER.getFlowing(4,false))) {
         var dry=ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get().defaultBlockState().setValue(VerticalDoubleTSupportBlock.FACING,face);
         var state=fluid.isSource()?NativeFluidloggedBlock.placement(dry,fluid):StairWaterFlow.withFlow(dry,fluid);level.setBlock(pos,state,Block.UPDATE_ALL);
         h.assertValueEqual(level.getFluidState(pos),fluid,"source/current fluidlogging unchanged");
         h.assertValueEqual(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),NbtUtils.writeBlockState(state)),state,"registry facing and fluid save exactly");
         h.assertValueEqual(state.rotate(Rotation.CLOCKWISE_90).getValue(VerticalDoubleTSupportBlock.FACING),face.getClockWise(),"structure rotation changes wall-facing direction");
         h.assertValueEqual(state.mirror(Mirror.LEFT_RIGHT).mirror(Mirror.LEFT_RIGHT),state,"mirror round trip retains fluids and direction");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void vertical_double_t_neighbor_changes_preserve_horizontal_and_add_no_connectors(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);clear(h,pos);
      var vertical=ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get().defaultBlockState();level.setBlock(pos,vertical,Block.UPDATE_ALL);
      var horizontal=ModBlocks.STEEL_DOUBLE_T_SUPPORT.get().defaultBlockState();level.setBlock(pos.west(),horizontal,Block.UPDATE_ALL);
      h.assertValueEqual(level.getBlockState(pos.west()),horizontal,"new block does not add a cross connector to horizontal beam");
      level.setBlock(pos.south(),Blocks.STONE.defaultBlockState(),Block.UPDATE_ALL);level.setBlock(pos.south(),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
      h.assertValueEqual(level.getBlockState(pos),vertical,"no invented wall-loss destruction mechanic");
      h.assertFalse(vertical.hasProperty(PortedBlocks.LEFTBEAM)||vertical.hasProperty(PortedBlocks.DOWNCONNECT),"no inferred connector/lamp properties");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void vertical_double_t_native_mining_has_one_self_drop_and_keeps_wall(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);clear(h,pos);
      level.setBlock(pos.south(),Blocks.BRICKS.defaultBlockState(),Block.UPDATE_ALL);level.setBlock(pos,ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get().defaultBlockState(),Block.UPDATE_ALL);
      var p=player(h,pos);p.setPos(pos.getX()+.5,pos.getY()+.5-p.getEyeHeight(),pos.getZ()-2);p.setYRot(0);p.setXRot(0);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));
      var eye=p.getEyePosition();var hit=level.clip(new ClipContext(eye,eye.add(p.getViewVector(1).scale(p.blockInteractionRange())),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,p));
      h.assertValueEqual(hit.getBlockPos(),pos,"actual ray selects beam before wall; eye="+eye+" direction="+p.getViewVector(1)+" shape="+level.getBlockState(pos).getShape(level,pos).bounds());p.gameMode.handleBlockBreakAction(pos,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,hit.getDirection(),level.getMaxBuildHeight(),0);
      h.assertTrue(p.gameMode.destroyBlock(pos),"native survival removal succeeds");h.assertTrue(level.getBlockState(pos.south()).is(Blocks.BRICKS),"wall survives");
      int count=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(1)).stream().filter(e->e.getItem().is(ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get().asItem())).mapToInt(e->e.getItem().getCount()).sum();h.assertValueEqual(count,1,"exactly one beam item");h.succeed();
   }
}
