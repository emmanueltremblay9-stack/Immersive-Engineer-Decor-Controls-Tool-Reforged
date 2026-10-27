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
   public static void wall_double_t_same_plane_joins_both_orders_and_updates(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);
      for(Direction face:Direction.Plane.HORIZONTAL)for(boolean across:List.of(false,true))for(boolean reverse:List.of(false,true)) {
         clear(h,pos);Direction cross=across?Direction.DOWN:face.getClockWise();var other=pos.relative(cross);
         var a=ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get().defaultBlockState().setValue(VerticalDoubleTSupportBlock.FACING,face).setValue(VerticalDoubleTSupportBlock.WALL_HORIZONTAL,across);
         var b=a.setValue(VerticalDoubleTSupportBlock.WALL_HORIZONTAL,!across);
         var player=player(h,pos);
         for(int step=0;step<2;step++) {
            boolean placeOther=(step==0)==reverse;var target=placeOther?other:pos;boolean horizontal=placeOther?!across:across;
            var wall=target.relative(face.getOpposite());level.setBlock(wall,Blocks.BRICKS.defaultBlockState(),Block.UPDATE_ALL);
            var point=Vec3.atCenterOf(wall).add(Vec3.atLowerCornerOf(face.getNormal()).scale(.5));
            if(horizontal)point=face.getAxis()==Direction.Axis.Z?point.add(-.4,0,0):point.add(0,0,-.4);
            var stack=new ItemStack(ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get(),2);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            h.assertTrue(stack.useOn(new net.minecraft.world.item.context.UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(point,face,wall,false))).consumesAction(),"native item path establishes each placement order");
         }
         var joined=level.getBlockState(pos);
         h.assertTrue(joined.getValue(VerticalDoubleTSupportBlock.JOIN_POSITIVE),"same-wall perpendicular neighbour creates original half-cross in either placement order");
         var shape=joined.getShape(level,pos);h.assertFalse(Shapes.joinIsNotEmpty(shape,joined.getCollisionShape(level,pos),BooleanOp.NOT_SAME),"connector has matching outline/collision");
         double[] point={15.99/16,.5,11.5/16};if(across){point=new double[]{point[1],1-point[0],point[2]};}
         for(int turns=0;turns<switch(face){case EAST->1;case SOUTH->2;case WEST->3;default->0;};turns++)point=new double[]{1-point[2],point[1],point[0]};
         final double x=point[0],y=point[1],z=point[2];
         h.assertTrue(shape.toAabbs().stream().anyMatch(box->box.contains(x,y,z)),"existing flange reaches the shared cell boundary without a gap");
         level.setBlock(other,b.setValue(VerticalDoubleTSupportBlock.FACING,face.getOpposite()),Block.UPDATE_ALL);
         h.assertFalse(level.getBlockState(pos).getValue(VerticalDoubleTSupportBlock.JOIN_POSITIVE),"opposite wall plane is not joined");
         level.setBlock(other,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
         h.assertValueEqual(level.getBlockState(pos),a,"removal restores original lone profile");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void wall_double_t_saved_mount_fluid_join_and_legacy_defaults(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);
      for(Direction face:Direction.Plane.HORIZONTAL)for(boolean across:List.of(false,true))for(var fluid:List.of(Fluids.WATER.getSource(false),Fluids.LAVA.getSource(false),Fluids.FLOWING_WATER.getFlowing(4,false))) {
         clear(h,pos);var base=ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get().defaultBlockState().setValue(VerticalDoubleTSupportBlock.FACING,face).setValue(VerticalDoubleTSupportBlock.WALL_HORIZONTAL,across);
         var state=fluid.isSource()?NativeFluidloggedBlock.placement(base,fluid):StairWaterFlow.withFlow(base,fluid);
         level.setBlock(pos,state,Block.UPDATE_ALL);Direction cross=across?Direction.DOWN:face.getClockWise();level.setBlock(pos.relative(cross),base,Block.UPDATE_ALL);
         state=level.getBlockState(pos);h.assertTrue(state.getValue(VerticalDoubleTSupportBlock.JOIN_POSITIVE),"joined fixture established");
         h.assertValueEqual(level.getFluidState(pos),fluid,"new mount and connection retain fluid");
         var saved=NbtUtils.writeBlockState(state);h.assertValueEqual(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),saved),state,"mount, connector and fluid survive state save/read");
         h.assertValueEqual(state.mirror(Mirror.LEFT_RIGHT).mirror(Mirror.LEFT_RIGHT),state,"mirror round trip preserves asymmetric connector");
         h.assertValueEqual(state.rotate(Rotation.CLOCKWISE_90).rotate(Rotation.COUNTERCLOCKWISE_90),state,"rotation round trip preserves fluid/mount");
         var legacy=NbtUtils.writeBlockState(base);var properties=legacy.getCompound("Properties");properties.remove("wall_horizontal");properties.remove("join_negative");properties.remove("join_positive");
         var read=NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),legacy);
         h.assertFalse(read.getValue(VerticalDoubleTSupportBlock.WALL_HORIZONTAL)||read.getValue(VerticalDoubleTSupportBlock.JOIN_NEGATIVE)||read.getValue(VerticalDoubleTSupportBlock.JOIN_POSITIVE),"old saves default to upright and unjoined; no migration rewrite");
         h.assertValueEqual(read.getValue(VerticalDoubleTSupportBlock.FACING),face,"old facing preserved");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void wall_double_t_horizontal_native_ray_drop_keeps_support_and_wall(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);clear(h,pos);
      level.setBlock(pos.south(),Blocks.BRICKS.defaultBlockState(),Block.UPDATE_ALL);
      level.setBlock(pos,ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get().defaultBlockState().setValue(VerticalDoubleTSupportBlock.WALL_HORIZONTAL,true),Block.UPDATE_ALL);
      var player=player(h,pos);player.setPos(pos.getX()+.5,pos.getY()+.5-player.getEyeHeight(),pos.getZ()-2);player.setYRot(0);player.setXRot(0);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));
      var eye=player.getEyePosition();var ray=level.clip(new ClipContext(eye,eye.add(player.getLookAngle().scale(player.blockInteractionRange())),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
      h.assertValueEqual(ray.getBlockPos(),pos,"horizontal flange can be selected before wall");
      player.gameMode.handleBlockBreakAction(pos,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,ray.getDirection(),level.getMaxBuildHeight(),0);
      h.assertTrue(player.gameMode.destroyBlock(pos),"native survival mining succeeds");h.assertTrue(level.getBlockState(pos.south()).is(Blocks.BRICKS),"supporting wall untouched");
      int count=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(1)).stream().filter(e->e.getItem().is(ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get().asItem())).mapToInt(e->e.getItem().getCount()).sum();
      h.assertValueEqual(count,1,"one unchanged beam item, no connector drop or duplication");h.succeed();
   }

   @GameTest(template="empty")
   public static void wall_double_t_native_horizontal_edge_and_vertical_centre(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);var player=player(h,pos);
      double[][] clicks={{.1,.5,1},{.9,.5,1},{.5,.1,0},{.5,.9,0},{.1,.2,1},{.2,.1,0},{.1,.1,0},{.5,.5,0},{.6,.5,0},{.25,.5,0}};
      for(Direction face:Direction.Plane.HORIZONTAL)for(InteractionHand hand:InteractionHand.values())for(double[] click:clicks) {
         clear(h,pos);var wall=pos.relative(face.getOpposite());level.setBlock(wall,Blocks.BRICKS.defaultBlockState(),Block.UPDATE_ALL);
         var centre=Vec3.atCenterOf(wall).add(Vec3.atLowerCornerOf(face.getNormal()).scale(.5));
         var point=face.getAxis()==Direction.Axis.Z?centre.add(click[0]-.5,click[1]-.5,0):centre.add(0,click[1]-.5,click[0]-.5);
         var stack=new ItemStack(ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get(),2);player.setItemInHand(hand,stack);
         h.assertTrue(stack.useOn(new net.minecraft.world.item.context.UseOnContext(player,hand,new BlockHitResult(point,face,wall,false))).consumesAction(),"native item placement succeeds");
         var state=level.getBlockState(pos);var box=state.getShape(level,pos).bounds();
         double length=face.getAxis()==Direction.Axis.Z?box.getXsize():box.getZsize();
         h.assertValueEqual(length,click[2]==1?1.:6./16,"left/right outer quarter chooses horizontal; nearest edge at corners; centre/ties vertical");
         h.assertValueEqual(box.getYsize(),click[2]==1?6./16:1.,"only rigid in-wall rotation, no new length");
         h.assertValueEqual(stack.getCount(),1,"one item consumed");h.assertTrue(level.getBlockState(wall).is(Blocks.BRICKS),"clicked wall preserved");
      }
      h.succeed();
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
      // ServerPlayer's visual head pose can lag its accepted packet rotation.
      // Deliberately retain that discrepancy: the old camera ray misses here,
      // while the authoritative server look ray must still select and mine it.
      p.setYHeadRot(10);
      var eye=p.getEyePosition();
      var cameraHit=level.clip(new ClipContext(eye,eye.add(p.getViewVector(1).scale(p.blockInteractionRange())),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,p));
      h.assertFalse(cameraHit.getBlockPos().equals(pos),"negative control reproduces the stale-head camera-ray miss");
      var direction=p.getLookAngle();
      h.assertTrue(direction.distanceTo(new Vec3(0,0,1))<1e-6,"server ray follows accepted body yaw rather than the render head pose");
      var hit=level.clip(new ClipContext(eye,eye.add(direction.scale(p.blockInteractionRange())),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,p));
      h.assertValueEqual(hit.getBlockPos(),pos,"actual server eye ray selects beam before wall; eye="+eye+" direction="+direction+" shape="+level.getBlockState(pos).getShape(level,pos).bounds());p.gameMode.handleBlockBreakAction(pos,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,hit.getDirection(),level.getMaxBuildHeight(),0);
      h.assertTrue(p.gameMode.destroyBlock(pos),"native survival removal succeeds");h.assertTrue(level.getBlockState(pos.south()).is(Blocks.BRICKS),"wall survives");
      int count=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(1)).stream().filter(e->e.getItem().is(ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get().asItem())).mapToInt(e->e.getItem().getCount()).sum();h.assertValueEqual(count,1,"exactly one beam item");h.succeed();
   }
   @GameTest(template="empty")
   public static void wall_double_t_single_mirrors_match_physical_shapes_for_asymmetric_arms(GameTestHelper h){
      var pos=h.absolutePos(CENTER);
      for(Direction facing:Direction.Plane.HORIZONTAL)for(boolean across:List.of(false,true))for(boolean positive:List.of(false,true))for(Mirror mirror:List.of(Mirror.LEFT_RIGHT,Mirror.FRONT_BACK)){
         var state=ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get().defaultBlockState().setValue(VerticalDoubleTSupportBlock.FACING,facing).setValue(VerticalDoubleTSupportBlock.WALL_HORIZONTAL,across)
            .setValue(VerticalDoubleTSupportBlock.JOIN_POSITIVE,positive).setValue(VerticalDoubleTSupportBlock.JOIN_NEGATIVE,!positive);
         var expected=Shapes.empty();for(var box:state.getCollisionShape(h.getLevel(),pos).toAabbs())expected=Shapes.or(expected,mirror==Mirror.FRONT_BACK?
            Shapes.box(1-box.maxX,box.minY,box.minZ,1-box.minX,box.maxY,box.maxZ):Shapes.box(box.minX,box.minY,1-box.maxZ,box.maxX,box.maxY,1-box.minZ));
         var changed=state.mirror(mirror);h.assertFalse(Shapes.joinIsNotEmpty(changed.getCollisionShape(h.getLevel(),pos),expected,BooleanOp.NOT_SAME),"one mirror maps the asymmetric actual shape, not just an involutive state round trip");
         h.assertValueEqual(changed.getValue(VerticalDoubleTSupportBlock.FACING),mirror.mirror(facing),"one mirror has the correct wall normal");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void wall_double_t_native_negative_and_double_arms_update_independently(GameTestHelper h){
      var level=h.getLevel();var pos=h.absolutePos(CENTER);var player=player(h,pos);
      for(Direction facing:Direction.Plane.HORIZONTAL)for(boolean across:List.of(false,true)){
         clear(h,pos);Direction axis=across?Direction.DOWN:facing.getClockWise();
         var neighbor=ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get().defaultBlockState().setValue(VerticalDoubleTSupportBlock.FACING,facing).setValue(VerticalDoubleTSupportBlock.WALL_HORIZONTAL,!across);
         level.setBlock(pos.relative(axis),neighbor,Block.UPDATE_ALL);level.setBlock(pos.relative(axis.getOpposite()),neighbor,Block.UPDATE_ALL);
         var wall=pos.relative(facing.getOpposite());level.setBlock(wall,Blocks.BRICKS.defaultBlockState(),Block.UPDATE_ALL);
         var point=Vec3.atCenterOf(wall).add(Vec3.atLowerCornerOf(facing.getNormal()).scale(.5));
         if(across)point=facing.getAxis()==Direction.Axis.Z?point.add(-.4,0,0):point.add(0,0,-.4);
         player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get()));
         player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(point,facing,wall,false)));
         var state=level.getBlockState(pos);h.assertTrue(state.getValue(VerticalDoubleTSupportBlock.JOIN_NEGATIVE)&&state.getValue(VerticalDoubleTSupportBlock.JOIN_POSITIVE),"native placement resolves both independent arms");
         level.setBlock(pos.relative(axis),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);state=level.getBlockState(pos);
         h.assertTrue(state.getValue(VerticalDoubleTSupportBlock.JOIN_NEGATIVE)&&!state.getValue(VerticalDoubleTSupportBlock.JOIN_POSITIVE),"negative-only arm remains after positive removal");
         level.setBlock(pos.relative(axis.getOpposite()),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);state=level.getBlockState(pos);
         h.assertFalse(state.getValue(VerticalDoubleTSupportBlock.JOIN_NEGATIVE)||state.getValue(VerticalDoubleTSupportBlock.JOIN_POSITIVE),"both removals restore original profile");
      }
      h.succeed();
   }

}
