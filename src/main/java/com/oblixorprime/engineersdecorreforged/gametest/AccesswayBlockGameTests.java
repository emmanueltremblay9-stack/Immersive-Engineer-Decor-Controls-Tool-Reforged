package com.oblixorprime.engineersdecorreforged.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
   private static final String ASSET_ROOT = "/assets/immersive_engineer_decor_controls_tool_reforged/";

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
   public static void iron_hatch_outline_and_collision_match_all_horizontal_states(GameTestHelper helper) {
      Block hatch = (Block)ModBlocks.IRON_HATCH.get();
      AABB centeredPlayer = new AABB(0.2, 0.0, 0.2, 0.8, 1.8, 0.8);
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         for (boolean powered : new boolean[]{false, true}) {
            BlockState base = (BlockState)((BlockState)hatch.defaultBlockState().setValue(PortedBlocks.HORIZONTAL_FACING, facing))
               .setValue(PortedBlocks.POWERED, powered);
            BlockState closed = (BlockState)base.setValue(PortedBlocks.OPEN, false);
            VoxelShape closedOutline = closed.getShape(helper.getLevel(), helper.absolutePos(LOWER_POS));
            VoxelShape closedCollision = closed.getCollisionShape(helper.getLevel(), helper.absolutePos(LOWER_POS));
            assertShapeBounds(helper, closedOutline, 0.0, 0.0, 0.0, 16.0, 3.0, 16.0, "closed iron hatch outline should match the lower model for " + facing);
            assertShapeBounds(
               helper, closedCollision, 0.0, 0.0, 0.0, 16.0, 3.0, 16.0, "closed iron hatch collision should match the lower model for " + facing
            );
            assertShapesEqual(helper, closedOutline, closedCollision, "closed iron hatch outline and collision should match for " + facing);
            assertShapeIntersects(helper, closedCollision, centeredPlayer, "closed iron hatch should block a centered player for " + facing);
            assertShapeWithinBlock(helper, closedCollision, "closed iron hatch collision should remain within block bounds for " + facing);
            helper.assertTrue(shapeVolume(closedCollision) < 1.0, "closed iron hatch collision should not become a full cube for " + facing);

            BlockState open = (BlockState)base.setValue(PortedBlocks.OPEN, true);
            VoxelShape openOutline = open.getShape(helper.getLevel(), helper.absolutePos(LOWER_POS));
            VoxelShape openCollision = open.getCollisionShape(helper.getLevel(), helper.absolutePos(LOWER_POS));
            assertOpenHatchOutlineBounds(helper, openOutline, facing);
            assertShapeWithinBlock(helper, openOutline, "open iron hatch outline should remain within block bounds for " + facing);
            helper.assertTrue(!openOutline.isEmpty(), "open iron hatch outline should remain selectable for " + facing);
            assertShapesEqual(helper, openOutline, openCollision, "open iron hatch solid leaf should remain collisionable for " + facing);
            assertShapeDoesNotIntersect(helper, openCollision, centeredPlayer, "open iron hatch should allow a centered player path for " + facing);
            assertOpenHatchSolidLeaf(helper, openCollision, facing);
         }
      }

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

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void metal_sliding_door_open_collision_allows_player_path_all_facings_and_halves(GameTestHelper helper) {
      DoorBlock door = (DoorBlock)ModBlocks.METAL_SLIDING_DOOR.get();
      double referenceVolume = -1.0;
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         for (DoorHingeSide hinge : DoorHingeSide.values()) {
            for (DoubleBlockHalf half : DoubleBlockHalf.values()) {
               BlockState open = slidingDoorState(door, facing, hinge, half, true);
               VoxelShape outline = open.getShape(helper.getLevel(), helper.absolutePos(LOWER_POS));
               VoxelShape collision = open.getCollisionShape(helper.getLevel(), helper.absolutePos(LOWER_POS));
               VoxelShape expectedOutline = expectedSlidingDoorModelShape(facing, hinge, half, true);
               VoxelShape expectedCollision = expectedSlidingDoorCollisionShape(facing, hinge, half, true);
               assertShapesEqual(helper, outline, expectedOutline, "open sliding door outline should match model components for " + facing + " " + hinge + " " + half);
               assertShapesEqual(
                  helper, collision, expectedCollision, "open sliding door collision should retain only solid non-track components for " + facing + " " + hinge + " " + half
               );
               AABB playerPath = centeredPlayerPathForHalf(half);
               assertShapeDoesNotIntersect(
                  helper, collision, playerPath, "open metal sliding door should allow a centered 0.6 x 1.8 player for " + facing + " " + hinge + " " + half
               );
               assertSlidingDoorStackCollision(helper, collision, slidingDoorStackSide(facing, hinge), facing + " " + hinge + " " + half);
               assertShapeWithinBlock(helper, collision, "open sliding door collision should remain within block bounds for " + facing + " " + hinge + " " + half);
               helper.assertTrue(shapeVolume(collision) < 1.0, "open sliding door collision should not become a full cube for " + facing + " " + hinge + " " + half);
               double volume = shapeVolume(collision);
               if (half == DoubleBlockHalf.LOWER && referenceVolume < 0.0) {
                  referenceVolume = volume;
               } else if (half == DoubleBlockHalf.LOWER) {
                  helper.assertTrue(close(volume, referenceVolume), "open sliding door collision volume should be rotation-invariant; actual=" + volume);
               }
            }
         }
      }

      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void metal_sliding_door_open_world_collision_allows_real_player_sweep(GameTestHelper helper) {
      DoorBlock door = (DoorBlock)ModBlocks.METAL_SLIDING_DOOR.get();
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      BlockPos lowerRelative = new BlockPos(3, 1, 3);
      BlockPos lowerAbsolute = helper.absolutePos(lowerRelative);
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         for (DoorHingeSide hinge : DoorHingeSide.values()) {
            BlockState openLower = slidingDoorState(door, facing, hinge, DoubleBlockHalf.LOWER, true);
            BlockState openUpper = slidingDoorState(door, facing, hinge, DoubleBlockHalf.UPPER, true);
            helper.setBlock(lowerRelative, openLower);
            helper.setBlock(lowerRelative.above(), openUpper);
            for (int sample = 0; sample <= 32; sample++) {
               double progress = sample / 32.0;
               double centerX = lowerAbsolute.getX() + (facing.getAxis() == Direction.Axis.Z ? 0.5 : -0.4 + 1.8 * progress);
               double centerZ = lowerAbsolute.getZ() + (facing.getAxis() == Direction.Axis.Z ? -0.4 + 1.8 * progress : 0.5);
               AABB candidate = new AABB(centerX - 0.3, lowerAbsolute.getY(), centerZ - 0.3, centerX + 0.3, lowerAbsolute.getY() + 1.8, centerZ + 0.3);
               helper.assertTrue(
                  helper.getLevel().noCollision(player, candidate),
                  "open sliding door should allow the real 0.6 x 1.8 player AABB sweep for " + facing + " " + hinge + " sample=" + sample
               );
            }

            helper.setBlock(lowerRelative, slidingDoorState(door, facing, hinge, DoubleBlockHalf.LOWER, false));
            helper.setBlock(lowerRelative.above(), slidingDoorState(door, facing, hinge, DoubleBlockHalf.UPPER, false));
            AABB closedMidpoint = new AABB(
               lowerAbsolute.getX() + 0.2,
               lowerAbsolute.getY(),
               lowerAbsolute.getZ() + 0.2,
               lowerAbsolute.getX() + 0.8,
               lowerAbsolute.getY() + 1.8,
               lowerAbsolute.getZ() + 0.8
            );
            helper.assertTrue(
               !helper.getLevel().noCollision(player, closedMidpoint), "closed sliding door should block the real player AABB for " + facing + " " + hinge
            );
         }
      }

      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void metal_sliding_door_closed_collision_preserves_panel_all_facings_and_halves(GameTestHelper helper) {
      DoorBlock door = (DoorBlock)ModBlocks.METAL_SLIDING_DOOR.get();
      double referenceVolume = -1.0;
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         for (DoorHingeSide hinge : DoorHingeSide.values()) {
            for (DoubleBlockHalf half : DoubleBlockHalf.values()) {
               BlockState closed = slidingDoorState(door, facing, hinge, half, false);
               VoxelShape outline = closed.getShape(helper.getLevel(), helper.absolutePos(LOWER_POS));
               VoxelShape collision = closed.getCollisionShape(helper.getLevel(), helper.absolutePos(LOWER_POS));
               VoxelShape expected = expectedSlidingDoorModelShape(facing, hinge, half, false);
               assertShapesEqual(helper, outline, expected, "closed sliding door outline should match model components for " + facing + " " + hinge + " " + half);
               assertShapesEqual(helper, collision, expected, "closed sliding door collision should match solid model components for " + facing + " " + hinge + " " + half);
               assertShapeIntersects(
                  helper,
                  collision,
                  centeredPlayerPathForHalf(half),
                  "closed metal sliding door should preserve the solid center panel for " + facing + " " + hinge + " " + half
               );
               assertShapeWithinBlock(helper, collision, "closed sliding door collision should remain within block bounds for " + facing + " " + hinge + " " + half);
               helper.assertTrue(shapeVolume(collision) < 1.0, "closed sliding door collision should not become a full cube for " + facing + " " + hinge + " " + half);
               if (facing.getAxis() == Direction.Axis.Z) {
                  assertShapeIntersects(helper, collision, 3.2, 2.0, 7.25, 12.8, 14.0, 8.75, "closed north/south sliding door should keep its panel");
               } else {
                  assertShapeIntersects(helper, collision, 7.25, 2.0, 3.2, 8.75, 14.0, 12.8, "closed east/west sliding door should keep its panel");
               }

               double volume = shapeVolume(collision);
               if (half == DoubleBlockHalf.LOWER && referenceVolume < 0.0) {
                  referenceVolume = volume;
               } else if (half == DoubleBlockHalf.LOWER) {
                  helper.assertTrue(close(volume, referenceVolume), "closed sliding door collision volume should be rotation-invariant; actual=" + volume);
               }
            }
         }
      }

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

   private static BlockState slidingDoorState(
      DoorBlock door, Direction facing, DoorHingeSide hinge, DoubleBlockHalf half, boolean open
   ) {
      return (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)door.defaultBlockState().setValue(DoorBlock.FACING, facing))
                     .setValue(DoorBlock.HINGE, hinge))
                  .setValue(DoorBlock.HALF, half))
               .setValue(DoorBlock.POWERED, false))
            .setValue(DoorBlock.OPEN, open);
   }

   private static AABB centeredPlayerPathForHalf(DoubleBlockHalf half) {
      return new AABB(0.2, 0.0, 0.2, 0.8, half == DoubleBlockHalf.LOWER ? 1.0 : 0.8, 0.8);
   }

   private static Direction slidingDoorStackSide(Direction facing, DoorHingeSide hinge) {
      return switch (facing) {
         case NORTH -> hinge == DoorHingeSide.RIGHT ? Direction.EAST : Direction.WEST;
         case SOUTH -> hinge == DoorHingeSide.LEFT ? Direction.EAST : Direction.WEST;
         case EAST -> hinge == DoorHingeSide.RIGHT ? Direction.SOUTH : Direction.NORTH;
         case WEST -> hinge == DoorHingeSide.LEFT ? Direction.SOUTH : Direction.NORTH;
         default -> throw new IllegalArgumentException("Unsupported sliding door facing " + facing);
      };
   }

   private static VoxelShape expectedSlidingDoorModelShape(
      Direction facing, DoorHingeSide hinge, DoubleBlockHalf half, boolean open
   ) {
      return slidingDoorShapeFromResources(facing, hinge, half, open, false);
   }

   private static VoxelShape expectedSlidingDoorCollisionShape(
      Direction facing, DoorHingeSide hinge, DoubleBlockHalf half, boolean open
   ) {
      return slidingDoorShapeFromResources(facing, hinge, half, open, true);
   }

   private static VoxelShape slidingDoorShapeFromResources(
      Direction facing, DoorHingeSide hinge, DoubleBlockHalf half, boolean open, boolean collisionOnly
   ) {
      JsonObject blockstate = readJsonResource(ASSET_ROOT + "blockstates/metal_sliding_door.json");
      JsonObject apply = null;
      for (JsonElement partElement : blockstate.getAsJsonArray("multipart")) {
         JsonObject part = partElement.getAsJsonObject();
         JsonObject when = part.getAsJsonObject("when");
         if (when.get("facing").getAsString().equals(facing.getName())
            && when.get("hinge").getAsString().equals(hinge.getSerializedName())
            && when.get("half").getAsString().equals(half.getSerializedName())
            && when.get("open").getAsString().equals(Boolean.toString(open))) {
            if (apply != null) {
               throw new IllegalStateException("Multiple sliding-door model mappings matched " + facing + " " + hinge + " " + half + " open=" + open);
            }

            apply = part.getAsJsonObject("apply");
         }
      }

      if (apply == null) {
         throw new IllegalStateException("Missing sliding-door model mapping for " + facing + " " + hinge + " " + half + " open=" + open);
      }

      String modelId = apply.get("model").getAsString();
      int separator = modelId.indexOf(':');
      if (separator < 1 || separator == modelId.length() - 1) {
         throw new IllegalStateException("Invalid sliding-door model id " + modelId);
      }

      JsonObject model = readJsonResource("/assets/" + modelId.substring(0, separator) + "/models/" + modelId.substring(separator + 1) + ".json");
      VoxelShape shape = Shapes.empty();
      for (JsonElement elementValue : model.getAsJsonArray("elements")) {
         JsonObject element = elementValue.getAsJsonObject();
         JsonArray from = element.getAsJsonArray("from");
         JsonArray to = element.getAsJsonArray("to");
         double minY = from.get(1).getAsDouble();
         double maxY = to.get(1).getAsDouble();
         if (collisionOnly && open && maxY - minY <= 1.0) {
            continue;
         }

         shape = Shapes.or(
            shape,
            Block.box(
               from.get(0).getAsDouble(),
               minY,
               from.get(2).getAsDouble(),
               to.get(0).getAsDouble(),
               maxY,
               to.get(2).getAsDouble()
            )
         );
      }

      int rotation = apply.has("y") ? apply.get("y").getAsInt() : 0;
      if (rotation % 90 != 0) {
         throw new IllegalStateException("Sliding-door model rotation is not a multiple of 90: " + rotation);
      }

      return rotateShapeClockwise(shape, Math.floorMod(rotation / 90, 4));
   }

   private static JsonObject readJsonResource(String resourcePath) {
      InputStream stream = AccesswayBlockGameTests.class.getResourceAsStream(resourcePath);
      if (stream == null) {
         throw new IllegalStateException("Missing GameTest resource " + resourcePath);
      }

      try (InputStream input = stream; InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
         return JsonParser.parseReader(reader).getAsJsonObject();
      } catch (Exception exception) {
         throw new IllegalStateException("Could not read GameTest resource " + resourcePath, exception);
      }
   }

   private static VoxelShape rotateShapeClockwise(VoxelShape shape, int steps) {
      VoxelShape rotated = shape;
      for (int step = 0; step < steps; step++) {
         VoxelShape next = Shapes.empty();
         for (AABB box : rotated.toAabbs()) {
            next = Shapes.or(next, Shapes.create(1.0 - box.maxZ, box.minY, box.minX, 1.0 - box.minZ, box.maxY, box.maxX));
         }

         rotated = next;
      }

      return rotated;
   }

   private static void assertSlidingDoorStackCollision(GameTestHelper helper, VoxelShape collision, Direction side, String stateDescription) {
      switch (side) {
         case NORTH -> assertShapeIntersects(
            helper, collision, 7.0, 4.0, 0.25, 9.0, 12.0, 1.0, "open sliding door should preserve its north solid stack for " + stateDescription
         );
         case SOUTH -> assertShapeIntersects(
            helper, collision, 7.0, 4.0, 15.0, 9.0, 12.0, 15.75, "open sliding door should preserve its south solid stack for " + stateDescription
         );
         case WEST -> assertShapeIntersects(
            helper, collision, 0.25, 4.0, 7.0, 1.0, 12.0, 9.0, "open sliding door should preserve its west solid stack for " + stateDescription
         );
         case EAST -> assertShapeIntersects(
            helper, collision, 15.0, 4.0, 7.0, 15.75, 12.0, 9.0, "open sliding door should preserve its east solid stack for " + stateDescription
         );
         default -> throw new IllegalArgumentException("Unsupported sliding door stack side " + side);
      }
   }

   private static void assertOpenHatchOutlineBounds(GameTestHelper helper, VoxelShape outline, Direction facing) {
      switch (facing) {
         case NORTH -> assertShapeBounds(helper, outline, 0.0, 0.0, 0.0, 16.0, 16.0, 2.875, "open north hatch outline should match its model");
         case SOUTH -> assertShapeBounds(helper, outline, 0.0, 0.0, 13.125, 16.0, 16.0, 16.0, "open south hatch outline should match its model");
         case WEST -> assertShapeBounds(helper, outline, 0.0, 0.0, 0.0, 2.875, 16.0, 16.0, "open west hatch outline should match its model");
         case EAST -> assertShapeBounds(helper, outline, 13.125, 0.0, 0.0, 16.0, 16.0, 16.0, "open east hatch outline should match its model");
         default -> throw new IllegalArgumentException("Unsupported hatch facing " + facing);
      }
   }

   private static void assertOpenHatchSolidLeaf(GameTestHelper helper, VoxelShape collision, Direction facing) {
      switch (facing) {
         case NORTH -> assertShapeIntersects(helper, collision, 4.0, 4.0, 2.25, 12.0, 12.0, 2.75, "open north hatch should retain solid leaf collision");
         case SOUTH -> assertShapeIntersects(helper, collision, 4.0, 4.0, 13.25, 12.0, 12.0, 13.75, "open south hatch should retain solid leaf collision");
         case WEST -> assertShapeIntersects(helper, collision, 2.25, 4.0, 4.0, 2.75, 12.0, 12.0, "open west hatch should retain solid leaf collision");
         case EAST -> assertShapeIntersects(helper, collision, 13.25, 4.0, 4.0, 13.75, 12.0, 12.0, "open east hatch should retain solid leaf collision");
         default -> throw new IllegalArgumentException("Unsupported hatch facing " + facing);
      }
   }

   private static void assertShapeWithinBlock(GameTestHelper helper, VoxelShape shape, String message) {
      for (AABB box : shape.toAabbs()) {
         helper.assertTrue(
            box.minX >= 0.0 && box.minY >= 0.0 && box.minZ >= 0.0 && box.maxX <= 1.0 && box.maxY <= 1.0 && box.maxZ <= 1.0,
            message + "; offending AABB=" + box
         );
      }
   }

   private static void assertShapesEqual(GameTestHelper helper, VoxelShape left, VoxelShape right, String message) {
      helper.assertTrue(
         !Shapes.joinIsNotEmpty(left, right, BooleanOp.ONLY_FIRST) && !Shapes.joinIsNotEmpty(left, right, BooleanOp.ONLY_SECOND), message
      );
   }

   private static double shapeVolume(VoxelShape shape) {
      double volume = 0.0;
      for (AABB box : shape.toAabbs()) {
         volume += (box.maxX - box.minX) * (box.maxY - box.minY) * (box.maxZ - box.minZ);
      }

      return volume;
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

   private static void assertShapeIntersects(GameTestHelper helper, VoxelShape shape, AABB probe, String message) {
      helper.assertTrue(Shapes.joinIsNotEmpty(shape, Shapes.create(probe), BooleanOp.AND), message);
   }

   private static void assertShapeDoesNotIntersect(
      GameTestHelper helper, VoxelShape shape, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, String message
   ) {
      helper.assertTrue(!shapeIntersects(shape, minX, minY, minZ, maxX, maxY, maxZ), message);
   }

   private static void assertShapeDoesNotIntersect(GameTestHelper helper, VoxelShape shape, AABB probe, String message) {
      helper.assertTrue(!Shapes.joinIsNotEmpty(shape, Shapes.create(probe), BooleanOp.AND), message);
   }

   private static boolean shapeIntersects(VoxelShape shape, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
      return Shapes.joinIsNotEmpty(shape, Block.box(minX, minY, minZ, maxX, maxY, maxZ), BooleanOp.AND);
   }

   private static boolean close(double left, double right) {
      return Math.abs(left - right) < 1.0E-6;
   }
}
