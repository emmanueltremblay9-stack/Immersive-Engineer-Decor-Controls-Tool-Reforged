package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class AccesswayBlockGameTests {
   private static final String TEMPLATE = "empty";
   private static final BlockPos LOWER_POS = new BlockPos(1, 1, 1);
   private static final BlockPos UPPER_POS = LOWER_POS.above();

   private AccesswayBlockGameTests() {
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void stacked_steel_mesh_fence_gate_segments_open_together(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      Block gate = (Block)ModBlocks.STEEL_MESH_FENCE_GATE.get();
      BlockState lower = (BlockState)((BlockState)((BlockState)gate.defaultBlockState().setValue(PortedBlocks.HORIZONTAL_FACING, Direction.NORTH))
            .setValue(PortedBlocks.OPEN, false))
         .setValue(PortedBlocks.SEGMENT, 0);
      BlockState upper = (BlockState)((BlockState)((BlockState)gate.defaultBlockState().setValue(PortedBlocks.HORIZONTAL_FACING, Direction.NORTH))
            .setValue(PortedBlocks.OPEN, false))
         .setValue(PortedBlocks.SEGMENT, 1);
      helper.setBlock(LOWER_POS, lower);
      helper.setBlock(UPPER_POS, upper);
      helper.useBlock(LOWER_POS, player);
      helper.assertBlockProperty(LOWER_POS, PortedBlocks.OPEN, true);
      helper.assertBlockProperty(UPPER_POS, PortedBlocks.OPEN, true);
      helper.useBlock(UPPER_POS, player);
      helper.assertBlockProperty(LOWER_POS, PortedBlocks.OPEN, false);
      helper.assertBlockProperty(UPPER_POS, PortedBlocks.OPEN, false);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void stacked_steel_mesh_fence_gate_segments_remove_together(GameTestHelper helper) {
      Block gate = (Block)ModBlocks.STEEL_MESH_FENCE_GATE.get();
      BlockState lower = (BlockState)((BlockState)((BlockState)gate.defaultBlockState().setValue(PortedBlocks.HORIZONTAL_FACING, Direction.NORTH))
            .setValue(PortedBlocks.OPEN, false))
         .setValue(PortedBlocks.SEGMENT, 0);
      BlockState upper = (BlockState)((BlockState)((BlockState)gate.defaultBlockState().setValue(PortedBlocks.HORIZONTAL_FACING, Direction.NORTH))
            .setValue(PortedBlocks.OPEN, false))
         .setValue(PortedBlocks.SEGMENT, 1);
      helper.setBlock(LOWER_POS, lower);
      helper.setBlock(UPPER_POS, upper);
      helper.setBlock(LOWER_POS, Blocks.AIR.defaultBlockState());
      helper.assertTrue(
         helper.getLevel().getBlockState(helper.absolutePos(UPPER_POS)).isAir(), "removing lower steel mesh fence gate segment should remove upper segment"
      );
      helper.setBlock(LOWER_POS, lower);
      helper.setBlock(UPPER_POS, upper);
      helper.setBlock(UPPER_POS, Blocks.AIR.defaultBlockState());
      helper.assertTrue(
         helper.getLevel().getBlockState(helper.absolutePos(LOWER_POS)).isAir(), "removing upper steel mesh fence gate segment should remove lower segment"
      );
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void open_steel_mesh_fence_gate_collision_clears_center_passage(GameTestHelper helper) {
      Block gate = (Block)ModBlocks.STEEL_MESH_FENCE_GATE.get();
      BlockState closedNorth = (BlockState)((BlockState)((BlockState)gate.defaultBlockState().setValue(PortedBlocks.HORIZONTAL_FACING, Direction.NORTH))
            .setValue(PortedBlocks.OPEN, false))
         .setValue(PortedBlocks.SEGMENT, 0);
      BlockState openNorth = (BlockState)closedNorth.setValue(PortedBlocks.OPEN, true);
      VoxelShape closedNorthCollision = closedNorth.getCollisionShape(helper.getLevel(), helper.absolutePos(LOWER_POS));
      VoxelShape openNorthCollision = openNorth.getCollisionShape(helper.getLevel(), helper.absolutePos(LOWER_POS));

      assertShapeIntersects(helper, closedNorthCollision, 4.0, 4.0, 7.25, 12.0, 12.0, 8.75, "closed fence gate should block its center panel");
      assertShapeDoesNotIntersect(
         helper, openNorthCollision, 4.0, 4.0, 7.25, 12.0, 12.0, 8.75, "open fence gate should clear the center passage"
      );
      assertShapeIntersects(helper, openNorthCollision, 12.5, 4.0, 1.0, 15.5, 12.0, 9.0, "open fence gate should keep collision on the folded side leaf");

      BlockState openEast = (BlockState)((BlockState)openNorth.setValue(PortedBlocks.HORIZONTAL_FACING, Direction.EAST)).setValue(PortedBlocks.OPEN, true);
      VoxelShape openEastCollision = openEast.getCollisionShape(helper.getLevel(), helper.absolutePos(LOWER_POS));
      assertShapeDoesNotIntersect(
         helper, openEastCollision, 7.25, 4.0, 4.0, 8.75, 12.0, 12.0, "rotated open fence gate should clear its center passage"
      );
      assertShapeIntersects(helper, openEastCollision, 7.0, 4.0, 12.5, 9.0, 12.0, 15.5, "rotated open fence gate should keep collision on its folded side leaf");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void catwalk_stairs_center_placement_uses_vanilla_stair_facing(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      Block stairs = (Block)ModBlocks.STEEL_CATWALK_STAIRS.get();
      for (float yaw : new float[]{0.0F, 90.0F, 180.0F, 270.0F}) {
         player.setYRot(yaw);
         BlockPlaceContext context = placementContext(helper, player, stairs, LOWER_POS, 0.5, 1.0, 0.5, Direction.UP);
         BlockState state = stairs.getStateForPlacement(context);
         helper.assertTrue(state != null, "catwalk stair center placement state should not be null");
         helper.assertValueEqual(
            state.getValue(PortedBlocks.HORIZONTAL_FACING),
            context.getHorizontalDirection(),
            "catwalk stairs should ascend in the same direction as vanilla stair placement"
         );
      }

      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void catwalk_stairs_collision_ascends_toward_facing_and_keeps_center_headroom(GameTestHelper helper) {
      Block stairs = (Block)ModBlocks.STEEL_CATWALK_STAIRS.get();
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         BlockState state = (BlockState)stairs.defaultBlockState().setValue(PortedBlocks.HORIZONTAL_FACING, facing);
         assertCatwalkStairTreads(helper, state.getCollisionShape(helper.getLevel(), helper.absolutePos(LOWER_POS)), facing);

         BlockState railed = (BlockState)((BlockState)state.setValue(PortedBlocks.LEFT_RAILING, true)).setValue(PortedBlocks.RIGHT_RAILING, true);
         assertShapeDoesNotIntersect(
            helper,
            railed.getCollisionShape(helper.getLevel(), helper.absolutePos(LOWER_POS)),
            6.0,
            12.0,
            6.0,
            10.0,
            16.0,
            10.0,
            "catwalk stair side railings should not block center headroom for " + facing
         );
      }

      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void steel_double_t_support_updates_beam_and_pole_connectors(GameTestHelper helper) {
      Block support = (Block)ModBlocks.STEEL_DOUBLE_T_SUPPORT.get();
      BlockPos center = new BlockPos(2, 2, 2);
      helper.setBlock(center, (BlockState)support.defaultBlockState().setValue(PortedBlocks.EASTWEST, true));
      helper.setBlock(center.north(), support.defaultBlockState());
      helper.setBlock(center.south(), support.defaultBlockState());
      helper.setBlock(center.below(), ((PortedBlocks.CenteredPoleBlock)ModBlocks.THIN_STEEL_POLE.get()).defaultBlockState());
      helper.assertBlockProperty(center, PortedBlocks.LEFTBEAM, true);
      helper.assertBlockProperty(center, PortedBlocks.RIGHTBEAM, true);
      helper.assertBlockProperty(center, PortedBlocks.DOWNCONNECT, 1);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void slab_slices_select_vertical_part_from_click_height(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      Block slice = (Block)ModBlocks.OLD_INDUSTRIAL_WOOD_SLABSLICE.get();
      assertSlicePlacementPart(helper, player, slice, 0.05, 0, "low click should place the bottom slab slice");
      assertSlicePlacementPart(helper, player, slice, 0.52, 7, "middle click should place a middle slab slice");
      assertSlicePlacementPart(helper, player, slice, 0.98, 14, "high click should place the top slab slice");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void raised_catwalk_placement_selects_reachable_model_variant(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      Block catwalk = (Block)ModBlocks.STEEL_CATWALK_TA.get();
      BlockPos pos = new BlockPos(3, 2, 4);
      BlockState state = placementState(helper, player, catwalk, pos, 0.5);
      helper.assertTrue(state != null, "raised catwalk placement state should not be null");
      helper.assertValueEqual(
         (Integer)state.getValue(PortedBlocks.VARIANT),
         variantFor(helper.absolutePos(pos)),
         "raised catwalk placement should select its position-based visual variant"
      );
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void iron_hatch_placement_reads_existing_redstone_power(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      Block hatch = (Block)ModBlocks.IRON_HATCH.get();
      BlockPos placePos = helper.absolutePos(LOWER_POS);
      BlockHitResult hit = new BlockHitResult(new Vec3(placePos.getX() + 0.5, placePos.getY() + 0.5, placePos.getZ() + 0.5), Direction.UP, placePos, false);
      BlockPlaceContext context = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, new ItemStack(hatch), hit);
      helper.getLevel().setBlock(context.getClickedPos().east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
      helper.assertTrue(helper.getLevel().hasNeighborSignal(context.getClickedPos()), "iron hatch test fixture should place the hatch beside active redstone");
      BlockState state = hatch.getStateForPlacement(context);
      helper.assertTrue(state != null, "iron hatch placement state should not be null");
      helper.assertTrue((Boolean)state.getValue(PortedBlocks.POWERED), "iron hatch should initialize powered beside active redstone");
      helper.assertTrue((Boolean)state.getValue(PortedBlocks.OPEN), "iron hatch should initialize open beside active redstone");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void powered_iron_hatch_stays_open_when_used(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      Block hatch = (Block)ModBlocks.IRON_HATCH.get();
      helper.setBlock(
         LOWER_POS,
         (BlockState)((BlockState)((BlockState)hatch.defaultBlockState().setValue(PortedBlocks.HORIZONTAL_FACING, Direction.NORTH))
               .setValue(PortedBlocks.POWERED, true))
            .setValue(PortedBlocks.OPEN, true)
      );
      helper.useBlock(LOWER_POS, player);
      helper.assertBlockProperty(LOWER_POS, PortedBlocks.POWERED, true);
      helper.assertBlockProperty(LOWER_POS, PortedBlocks.OPEN, true);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void open_iron_hatch_collision_matches_visible_side_plate(GameTestHelper helper) {
      Block hatch = (Block)ModBlocks.IRON_HATCH.get();
      BlockState openNorth = (BlockState)((BlockState)hatch.defaultBlockState().setValue(PortedBlocks.HORIZONTAL_FACING, Direction.NORTH))
         .setValue(PortedBlocks.OPEN, true);
      VoxelShape collision = openNorth.getCollisionShape(helper.getLevel(), helper.absolutePos(LOWER_POS));
      assertShapeBounds(helper, collision, 0.0, 0.0, 0.0, 16.0, 16.0, 2.0, "open north iron hatch collision should match its visible side plate");
      assertShapeIntersects(helper, collision, 1.0, 4.0, 0.25, 15.0, 12.0, 1.75, "open iron hatch should collide with its visible side plate");
      assertShapeDoesNotIntersect(helper, collision, 1.0, 4.0, 4.0, 15.0, 12.0, 12.0, "open iron hatch should not block the center passage");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void slab_and_slab_slice_collision_matches_visible_height(GameTestHelper helper) {
      Block slab = (Block)ModBlocks.OLD_INDUSTRIAL_WOOD_SLAB.get();
      assertShapeBounds(
         helper,
         slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM).getCollisionShape(helper.getLevel(), helper.absolutePos(LOWER_POS)),
         0.0,
         0.0,
         0.0,
         16.0,
         8.0,
         16.0,
         "bottom slab collision should be half-height"
      );
      assertShapeBounds(
         helper,
         slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP).getCollisionShape(helper.getLevel(), helper.absolutePos(LOWER_POS)),
         0.0,
         8.0,
         0.0,
         16.0,
         16.0,
         16.0,
         "top slab collision should occupy the upper half"
      );

      Block slice = (Block)ModBlocks.OLD_INDUSTRIAL_WOOD_SLABSLICE.get();
      VoxelShape midSliceCollision = slice.defaultBlockState()
         .setValue(PortedBlocks.PARTS, 7)
         .getCollisionShape(helper.getLevel(), helper.absolutePos(LOWER_POS));
      assertShapeBounds(helper, midSliceCollision, 0.0, 7.0, 0.0, 16.0, 9.0, 16.0, "slab-slice collision should match the selected visible slice");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void metal_rung_ladders_are_marked_climbable(GameTestHelper helper) {
      assertClimbable(helper, ModBlocks.METAL_RUNG_LADDER.get(), "metal rung ladder");
      assertClimbable(helper, ModBlocks.METAL_RUNG_STEPS.get(), "staggered metal steps");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void surface_mounted_blocks_drop_when_support_is_removed(GameTestHelper helper) {
      BlockPos supportPos = new BlockPos(2, 2, 2);
      BlockPos lightPos = supportPos.north();
      Block light = (Block)ModBlocks.IRON_BULB_LIGHT.get();
      helper.setBlock(supportPos, Blocks.STONE);
      helper.setBlock(lightPos, (BlockState)light.defaultBlockState().setValue(PortedBlocks.FACING, Direction.NORTH));
      helper.assertTrue(helper.getBlockState(lightPos).is(light), "surface-mounted fixture should start attached to its support");
      helper.setBlock(supportPos, Blocks.AIR);
      helper.runAfterDelay(1L, () -> {
         helper.assertTrue(helper.getBlockState(lightPos).isAir(), "surface-mounted fixture should drop when its supporting face is removed");
         helper.succeed();
      });
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void custom_doors_open_from_redstone_power(GameTestHelper helper) {
      BlockPos oldWoodDoorPos = new BlockPos(1, 1, 1);
      BlockPos metalDoorPos = new BlockPos(4, 1, 1);
      placeClosedDoor(helper, (Block)ModBlocks.OLD_INDUSTRIAL_WOOD_DOOR.get(), oldWoodDoorPos);
      placeClosedDoor(helper, (Block)ModBlocks.METAL_SLIDING_DOOR.get(), metalDoorPos);
      helper.setBlock(oldWoodDoorPos.east(), Blocks.REDSTONE_BLOCK);
      helper.setBlock(metalDoorPos.east(), Blocks.REDSTONE_BLOCK);
      helper.runAfterDelay(2L, () -> {
         assertDoorPoweredOpen(helper, oldWoodDoorPos, "old industrial wood door");
         assertDoorPoweredOpen(helper, metalDoorPos, "metal sliding door");
         helper.succeed();
      });
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void metal_sliding_door_hitbox_matches_sliding_panel_model(GameTestHelper helper) {
      DoorBlock door = (DoorBlock)ModBlocks.METAL_SLIDING_DOOR.get();
      BlockState base = (BlockState)((BlockState)((BlockState)((BlockState)door.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER))
               .setValue(DoorBlock.POWERED, false))
            .setValue(DoorBlock.FACING, Direction.NORTH))
         .setValue(DoorBlock.HINGE, DoorHingeSide.RIGHT);

      VoxelShape closedNorth = ((BlockState)base.setValue(DoorBlock.OPEN, false)).getShape(helper.getLevel(), helper.absolutePos(LOWER_POS));
      assertShapeBounds(helper, closedNorth, 0.0, 0.0, 6.0, 16.0, 16.0, 10.0, "closed north sliding door shape should be centered on the visible model");
      assertShapeIntersects(helper, closedNorth, 0.0, 4.0, 7.25, 16.0, 12.0, 8.75, "closed sliding door should contain the centered main panel");
      assertShapeDoesNotIntersect(helper, closedNorth, 0.0, 4.0, 0.0, 16.0, 12.0, 3.0, "closed sliding door should not keep the vanilla edge hitbox");

      VoxelShape closedEast = ((BlockState)((BlockState)base.setValue(DoorBlock.FACING, Direction.EAST)).setValue(DoorBlock.OPEN, false))
         .getShape(helper.getLevel(), helper.absolutePos(LOWER_POS));
      assertShapeBounds(helper, closedEast, 6.0, 0.0, 0.0, 10.0, 16.0, 16.0, "closed east sliding door shape should rotate with the model");

      VoxelShape openNorthRight = ((BlockState)base.setValue(DoorBlock.OPEN, true)).getShape(helper.getLevel(), helper.absolutePos(LOWER_POS));
      assertShapeIntersects(helper, openNorthRight, 15.25, 4.0, 7.0, 15.75, 12.0, 9.0, "open right-hinged sliding door should keep its side stack on the east rail");
      assertShapeIntersects(helper, openNorthRight, 7.0, 0.0, 7.0, 9.0, 0.2, 9.0, "open sliding door should keep its low floor track");
      assertShapeDoesNotIntersect(
         helper, openNorthRight, 7.0, 4.0, 7.0, 9.0, 12.0, 9.0, "open sliding door should clear the human-height center passage"
      );

      VoxelShape openWestLeft = ((BlockState)((BlockState)((BlockState)base.setValue(DoorBlock.FACING, Direction.WEST))
               .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT))
            .setValue(DoorBlock.OPEN, true))
         .getShape(helper.getLevel(), helper.absolutePos(LOWER_POS));
      assertShapeIntersects(helper, openWestLeft, 7.0, 4.0, 15.25, 9.0, 12.0, 15.75, "open west-facing left-hinged sliding door should rotate its side stack south");
      assertShapeDoesNotIntersect(
         helper, openWestLeft, 7.0, 4.0, 7.0, 9.0, 12.0, 9.0, "rotated open sliding door should also clear the human-height center passage"
      );
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void metal_sliding_doors_pair_only_two_adjacent_doors(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      DoorBlock door = (DoorBlock)ModBlocks.METAL_SLIDING_DOOR.get();
      BlockPos leftDoorPos = new BlockPos(3, 1, 3);
      BlockState firstDoor = placeDoorFromItem(helper, player, door, leftDoorPos);
      Direction facing = firstDoor.getValue(DoorBlock.FACING);
      BlockPos rightDoorPos = leftDoorPos.relative(facing.getClockWise());
      BlockPos thirdDoorPos = rightDoorPos.relative(facing.getClockWise());

      placeDoorFromItem(helper, player, door, rightDoorPos);
      placeDoorFromItem(helper, player, door, thirdDoorPos);

      assertSlidingPairSide(helper, leftDoorPos, PortedBlocks.SlidingDoorPairSide.LEFT, "left metal sliding door");
      assertSlidingPairSide(helper, rightDoorPos, PortedBlocks.SlidingDoorPairSide.RIGHT, "right metal sliding door");
      assertSlidingPairSide(helper, thirdDoorPos, PortedBlocks.SlidingDoorPairSide.SINGLE, "third adjacent metal sliding door");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void metal_sliding_door_pair_toggles_from_either_half(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      DoorBlock door = (DoorBlock)ModBlocks.METAL_SLIDING_DOOR.get();
      BlockPos leftDoorPos = new BlockPos(3, 1, 3);
      Direction facing = placeDoorFromItem(helper, player, door, leftDoorPos).getValue(DoorBlock.FACING);
      BlockPos rightDoorPos = leftDoorPos.relative(facing.getClockWise());
      placeDoorFromItem(helper, player, door, rightDoorPos);

      helper.useBlock(rightDoorPos.above(), player);
      assertDoorState(helper, leftDoorPos, true, false, "left metal sliding door should open from paired upper-half use");
      assertDoorState(helper, rightDoorPos, true, false, "right metal sliding door should open from paired upper-half use");

      helper.useBlock(leftDoorPos, player);
      assertDoorState(helper, leftDoorPos, false, false, "left metal sliding door should close from paired lower-half use");
      assertDoorState(helper, rightDoorPos, false, false, "right metal sliding door should close from paired lower-half use");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 120)
   public static void metal_sliding_door_pair_redstone_does_not_chain(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      DoorBlock door = (DoorBlock)ModBlocks.METAL_SLIDING_DOOR.get();
      BlockPos leftDoorPos = new BlockPos(3, 1, 3);
      Direction facing = placeDoorFromItem(helper, player, door, leftDoorPos).getValue(DoorBlock.FACING);
      BlockPos rightDoorPos = leftDoorPos.relative(facing.getClockWise());
      BlockPos thirdDoorPos = rightDoorPos.relative(facing.getClockWise());
      placeDoorFromItem(helper, player, door, rightDoorPos);
      placeDoorFromItem(helper, player, door, thirdDoorPos);

      BlockPos powerPos = leftDoorPos.relative(facing.getOpposite());
      helper.setBlock(powerPos, Blocks.REDSTONE_BLOCK);
      helper.runAfterDelay(2L, () -> {
         assertDoorPoweredOpen(helper, leftDoorPos, "powered left metal sliding door");
         assertDoorPoweredOpen(helper, rightDoorPos, "paired right metal sliding door");
         assertDoorState(helper, thirdDoorPos, false, false, "third metal sliding door should not receive chained pair power");
         helper.setBlock(powerPos, Blocks.AIR);
         helper.runAfterDelay(2L, () -> {
            assertDoorState(helper, leftDoorPos, false, false, "left metal sliding door should close after pair power is removed");
            assertDoorState(helper, rightDoorPos, false, false, "right metal sliding door should close after pair power is removed");
            assertDoorState(helper, thirdDoorPos, false, false, "third metal sliding door should remain closed after pair power is removed");
            helper.succeed();
         });
      });
   }

   private static void assertSlicePlacementPart(GameTestHelper helper, Player player, Block block, double localY, int expectedPart, String message) {
      BlockState state = placementState(helper, player, block, LOWER_POS, localY);
      helper.assertTrue(state != null, "slab slice placement state should not be null");
      helper.assertValueEqual((Integer)state.getValue(PortedBlocks.PARTS), expectedPart, message);
   }

   private static BlockState placementState(GameTestHelper helper, Player player, Block block, BlockPos pos, double localY) {
      return placementState(helper, player, block, pos, localY, Direction.UP);
   }

   private static BlockState placementState(GameTestHelper helper, Player player, Block block, BlockPos pos, double localY, Direction face) {
      BlockPos absolutePos = helper.absolutePos(pos);
      Vec3 hitLocation = new Vec3(absolutePos.getX() + 0.5, absolutePos.getY() + localY, absolutePos.getZ() + 0.5);
      BlockHitResult hit = new BlockHitResult(hitLocation, face, absolutePos, false);
      BlockPlaceContext context = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, new ItemStack(block), hit);
      return block.getStateForPlacement(context);
   }

   private static BlockPlaceContext placementContext(
      GameTestHelper helper, Player player, Block block, BlockPos pos, double localX, double localY, double localZ, Direction face
   ) {
      BlockPos absolutePos = helper.absolutePos(pos);
      Vec3 hitLocation = new Vec3(absolutePos.getX() + localX, absolutePos.getY() + localY, absolutePos.getZ() + localZ);
      BlockHitResult hit = new BlockHitResult(hitLocation, face, absolutePos, false);
      return new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, new ItemStack(block), hit);
   }

   private static BlockState placeDoorFromItem(GameTestHelper helper, Player player, DoorBlock door, BlockPos lowerPos) {
      BlockPos absolutePos = helper.absolutePos(lowerPos);
      Vec3 hitLocation = new Vec3(absolutePos.getX() + 0.5, absolutePos.getY() + 0.5, absolutePos.getZ() + 0.5);
      BlockHitResult hit = new BlockHitResult(hitLocation, Direction.UP, absolutePos, false);
      ItemStack stack = new ItemStack(door);
      BlockPlaceContext context = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit);
      BlockState state = door.getStateForPlacement(context);
      helper.assertTrue(state != null, "metal sliding door placement state should not be null");
      helper.getLevel().setBlock(absolutePos, state, 3);
      door.setPlacedBy(helper.getLevel(), absolutePos, state, player, stack);
      return helper.getBlockState(lowerPos);
   }

   private static void assertClimbable(GameTestHelper helper, Block block, String name) {
      helper.assertTrue(block.defaultBlockState().is(BlockTags.CLIMBABLE), name + " should be in minecraft:climbable for ladder movement");
   }

   private static void assertCatwalkStairTreads(GameTestHelper helper, VoxelShape shape, Direction facing) {
      switch (facing) {
         case NORTH -> {
            assertShapeIntersects(helper, shape, 2.0, 4.5, 10.0, 14.0, 4.9, 14.0, "north catwalk stairs should have a low south entry tread");
            assertShapeIntersects(helper, shape, 2.0, 9.5, 2.0, 14.0, 9.9, 6.0, "north catwalk stairs should have a high north exit tread");
            assertShapeDoesNotIntersect(helper, shape, 2.0, 9.5, 10.0, 14.0, 9.9, 14.0, "north catwalk stairs should not put the high tread on the entry side");
         }
         case SOUTH -> {
            assertShapeIntersects(helper, shape, 2.0, 4.5, 2.0, 14.0, 4.9, 6.0, "south catwalk stairs should have a low north entry tread");
            assertShapeIntersects(helper, shape, 2.0, 9.5, 10.0, 14.0, 9.9, 14.0, "south catwalk stairs should have a high south exit tread");
            assertShapeDoesNotIntersect(helper, shape, 2.0, 9.5, 2.0, 14.0, 9.9, 6.0, "south catwalk stairs should not put the high tread on the entry side");
         }
         case WEST -> {
            assertShapeIntersects(helper, shape, 10.0, 4.5, 2.0, 14.0, 4.9, 14.0, "west catwalk stairs should have a low east entry tread");
            assertShapeIntersects(helper, shape, 2.0, 9.5, 2.0, 6.0, 9.9, 14.0, "west catwalk stairs should have a high west exit tread");
            assertShapeDoesNotIntersect(helper, shape, 10.0, 9.5, 2.0, 14.0, 9.9, 14.0, "west catwalk stairs should not put the high tread on the entry side");
         }
         case EAST -> {
            assertShapeIntersects(helper, shape, 2.0, 4.5, 2.0, 6.0, 4.9, 14.0, "east catwalk stairs should have a low west entry tread");
            assertShapeIntersects(helper, shape, 10.0, 9.5, 2.0, 14.0, 9.9, 14.0, "east catwalk stairs should have a high east exit tread");
            assertShapeDoesNotIntersect(helper, shape, 2.0, 9.5, 2.0, 6.0, 9.9, 14.0, "east catwalk stairs should not put the high tread on the entry side");
         }
         default -> throw new IllegalArgumentException("Unsupported catwalk stair facing " + facing);
      }
   }

   private static int variantFor(BlockPos pos) {
      return Math.floorMod(pos.getX() * 31 + pos.getY() * 7 + pos.getZ(), 5);
   }

   private static void placeClosedDoor(GameTestHelper helper, Block door, BlockPos lowerPos) {
      BlockState base = (BlockState)((BlockState)((BlockState)((BlockState)door.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH))
               .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT))
            .setValue(DoorBlock.OPEN, false))
         .setValue(DoorBlock.POWERED, false);
      helper.setBlock(lowerPos, (BlockState)base.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
      helper.setBlock(lowerPos.above(), (BlockState)base.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
   }

   private static void assertDoorPoweredOpen(GameTestHelper helper, BlockPos lowerPos, String name) {
      assertDoorState(helper, lowerPos, true, true, name);
   }

   private static void assertDoorState(GameTestHelper helper, BlockPos lowerPos, boolean open, boolean powered, String name) {
      helper.assertBlockProperty(lowerPos, DoorBlock.POWERED, powered);
      helper.assertBlockProperty(lowerPos, DoorBlock.OPEN, open);
      helper.assertBlockProperty(lowerPos.above(), DoorBlock.POWERED, powered);
      helper.assertBlockProperty(lowerPos.above(), DoorBlock.OPEN, open);
   }

   private static void assertSlidingPairSide(
      GameTestHelper helper, BlockPos lowerPos, PortedBlocks.SlidingDoorPairSide pairSide, String name
   ) {
      helper.assertBlockProperty(lowerPos, PortedBlocks.PAIR_SIDE, pairSide);
      helper.assertBlockProperty(lowerPos.above(), PortedBlocks.PAIR_SIDE, pairSide);
   }

   private static void assertShapeBounds(
      GameTestHelper helper, VoxelShape shape, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, String message
   ) {
      AABB bounds = shape.bounds();
      helper.assertTrue(
         close(bounds.minX, minX / 16.0)
            && close(bounds.minY, minY / 16.0)
            && close(bounds.minZ, minZ / 16.0)
            && close(bounds.maxX, maxX / 16.0)
            && close(bounds.maxY, maxY / 16.0)
            && close(bounds.maxZ, maxZ / 16.0),
         message + "; actual bounds=" + bounds
      );
   }

   private static void assertShapeIntersects(
      GameTestHelper helper, VoxelShape shape, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, String message
   ) {
      helper.assertTrue(shapeIntersects(shape, minX, minY, minZ, maxX, maxY, maxZ), message);
   }

   private static void assertShapeDoesNotIntersect(
      GameTestHelper helper, VoxelShape shape, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, String message
   ) {
      helper.assertTrue(!shapeIntersects(shape, minX, minY, minZ, maxX, maxY, maxZ), message);
   }

   private static boolean shapeIntersects(VoxelShape shape, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
      return Shapes.joinIsNotEmpty(shape, Block.box(minX, minY, minZ, maxX, maxY, maxZ), BooleanOp.AND);
   }

   private static boolean close(double left, double right) {
      return Math.abs(left - right) < 1.0E-6;
   }
}
