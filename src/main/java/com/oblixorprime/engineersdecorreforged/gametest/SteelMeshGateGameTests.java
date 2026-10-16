package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SteelMeshGateGameTests {
   private static final BlockPos GATE = new BlockPos(3, 1, 3);
   private static final double EPSILON = 1.0E-6;

   private SteelMeshGateGameTests() {
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void open_gate_segments_allow_real_player_passage(GameTestHelper helper) {
      assertPassageMatrix(helper, true, false);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void saved_open_gate_segments_allow_real_player_passage(GameTestHelper helper) {
      assertPassageMatrix(helper, true, true);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void closed_gate_segments_physically_block_both_directions(GameTestHelper helper) {
      assertPassageMatrix(helper, false, false);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void open_gate_retains_physical_posts_and_folded_side(GameTestHelper helper) {
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         for (Direction side : List.of(facing.getClockWise(), facing.getCounterClockWise())) {
            for (Direction travel : List.of(facing, facing.getOpposite())) {
               for (int arrangement = 0; arrangement <= 2; arrangement++) {
                  clearFixture(helper);
                  helper.setBlock(GATE, gateState(facing, true, arrangement == 1 ? 1 : 0));
                  if (arrangement == 2) {
                     helper.setBlock(GATE.above(), gateState(facing, true, 1));
                  }
                  // No adjacent fences: only the gate itself can obstruct either structural side.
                  Vec3 lane = new Vec3(side.getStepX() * 0.4375, 0, side.getStepZ() * 0.4375);
                  double distance = movePlayerThroughGate(helper, travel, lane);
                  helper.assertTrue(distance < 2.5 - EPSILON,
                     "open gate must keep real side collision: facing=" + facing + ", side=" + side
                        + ", travel=" + travel + ", arrangement=" + arrangement + ", moved=" + distance);
               }
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void mesh_fence_connects_to_gate_in_both_placement_orders(GameTestHelper helper) {
      List<String> failures = new ArrayList<>();
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         for (Direction side : Direction.Plane.HORIZONTAL) {
            for (boolean open : new boolean[]{false, true}) {
               for (int segment = 0; segment <= 1; segment++) {
                  for (boolean gateFirst : new boolean[]{false, true}) {
                     clearFixture(helper);
                     BlockPos fencePos = GATE.relative(side);
                     if (gateFirst) {
                        helper.setBlock(GATE, gateState(facing, open, segment));
                        placeFence(helper, fencePos);
                     } else {
                        placeFence(helper, fencePos);
                        helper.setBlock(GATE, gateState(facing, open, segment));
                     }
                     WallSide expected = facing.getAxis() != side.getAxis() ? WallSide.LOW : WallSide.NONE;
                     WallSide actual = helper.getBlockState(fencePos).getValue(wallSide(side.getOpposite()));
                     if (actual != expected) {
                        failures.add("facing=" + facing + ", side=" + side + ", open=" + open + ", segment=" + segment
                           + ", gateFirst=" + gateFirst + ": expected=" + expected + ", actual=" + actual);
                     }
                     helper.setBlock(GATE, Blocks.AIR);
                     helper.assertBlockProperty(fencePos, wallSide(side.getOpposite()), WallSide.NONE);
                  }
               }
            }
         }
      }
      helper.assertTrue(failures.isEmpty(), "mesh fence connection failures=" + failures.size() + "; " + failures);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void paired_gate_connections_survive_clicks_and_disconnect_on_removal(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         for (int removedSegment = 0; removedSegment <= 1; removedSegment++) {
            clearFixture(helper);
            helper.setBlock(GATE, gateState(facing, false, 0));
            helper.setBlock(GATE.above(), gateState(facing, false, 1));
            for (Direction side : List.of(facing.getClockWise(), facing.getCounterClockWise())) {
               placeFence(helper, GATE.relative(side));
               placeFence(helper, GATE.above().relative(side));
            }
            assertPairedConnections(helper, facing, true);
            helper.useBlock(GATE, player);
            assertGatePairOpen(helper, true);
            assertPairedConnections(helper, facing, true);
            helper.useBlock(GATE.above(), player);
            assertGatePairOpen(helper, false);
            assertPairedConnections(helper, facing, true);
            helper.setBlock(GATE.above(removedSegment), Blocks.AIR);
            helper.assertTrue(helper.getBlockState(GATE).isAir() && helper.getBlockState(GATE.above()).isAir(),
               "removing either segment must remove the linked segment");
            assertPairedConnections(helper, facing, false);
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void mesh_fence_gate_connections_keep_vanilla_wall_topology(GameTestHelper helper) {
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         for (boolean covered : new boolean[]{false, true}) {
            for (boolean waterlogged : new boolean[]{false, true}) {
               clearFixture(helper);
               Direction side = facing.getClockWise();
               BlockPos fencePos = GATE.relative(side);
               helper.setBlock(fencePos.relative(side), Blocks.COBBLESTONE_WALL);
               if (covered) {
                  helper.setBlock(fencePos.above(), Blocks.STONE);
               }
               helper.setBlock(GATE, Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, facing));
               if (waterlogged) {
                  helper.setBlock(fencePos, Blocks.WATER);
               }
               placeFence(helper, fencePos);
               BlockState reference = helper.getBlockState(fencePos);
               helper.assertBlockProperty(fencePos, WallBlock.WATERLOGGED, waterlogged);
               helper.assertTrue(reference.getValue(wallSide(side.getOpposite())) != WallSide.NONE,
                  "vanilla reference gate must establish a connection");
               helper.setBlock(GATE, gateState(facing, false, 0));
               helper.assertValueEqual(helper.getBlockState(fencePos), reference,
                  "custom gate must preserve vanilla side height, center post and waterlogging after replacement");
               helper.setBlock(fencePos, waterlogged ? Blocks.WATER : Blocks.AIR);
               placeFence(helper, fencePos);
               helper.assertValueEqual(helper.getBlockState(fencePos), reference,
                  "placement beside a custom gate must preserve the vanilla dry/waterlogged wall state");
               helper.setBlock(fencePos.above(), covered ? Blocks.AIR : Blocks.STONE);
               BlockState afterTopChange = helper.getBlockState(fencePos);
               helper.setBlock(GATE, Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, facing));
               helper.assertValueEqual(helper.getBlockState(fencePos), afterTopChange,
                  "vertical updates beside the custom gate must match vanilla wall topology");
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void gate_above_mesh_fence_keeps_its_real_shape_for_wall_updates(GameTestHelper helper) {
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         for (Direction topFacing : Direction.Plane.HORIZONTAL) {
            for (boolean open : new boolean[]{false, true}) {
               clearFixture(helper);
               Direction side = facing.getClockWise();
               BlockPos fencePos = GATE.relative(side);
               BlockPos top = fencePos.above();
               helper.setBlock(fencePos.relative(side), Blocks.COBBLESTONE_WALL);
               helper.setBlock(GATE, Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, facing));
               helper.setBlock(top, gateState(topFacing, open, 0));
               placeFence(helper, fencePos);
               BlockState reference = helper.getBlockState(fencePos);
               helper.setBlock(GATE, gateState(facing, false, 0));
               helper.assertValueEqual(helper.getBlockState(fencePos), reference,
                  "horizontal gate adaptation must leave the distinct custom gate above the wall unchanged");
               helper.setBlock(fencePos, Blocks.AIR);
               placeFence(helper, fencePos);
               helper.assertValueEqual(helper.getBlockState(fencePos), reference,
                  "placement must use the real above-gate shape for wall side heights and post");
               helper.setBlock(top, gateState(topFacing, !open, 0));
               BlockState afterTopUpdate = helper.getBlockState(fencePos);
               helper.setBlock(GATE, Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, facing));
               helper.assertValueEqual(helper.getBlockState(fencePos), afterTopUpdate,
                  "vertical update must not substitute vanilla gate geometry for the actual gate above");
               helper.assertValueEqual(helper.getBlockState(top), gateState(topFacing, !open, 0),
                  "connection queries must not mutate the real gate above the fence");
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void gate_legacy_states_round_trip_with_empty_native_cells(GameTestHelper helper) {
      Set<String> wallProperties = ModBlocks.STEEL_MESH_FENCE.get().defaultBlockState().getProperties().stream()
         .map(Property::getName).collect(Collectors.toSet());
      helper.assertValueEqual(wallProperties, Set.of("north", "east", "south", "west", "up", "waterlogged", "fluid_cell", "water_flow"),
         "native cells extend the six-property wall topology");
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         for (boolean open : new boolean[]{false, true}) {
            for (int segment = 0; segment <= 1; segment++) {
               BlockState state = gateState(facing, open, segment);
               var saved = NbtUtils.writeBlockState(state);
               BlockState loaded = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), saved);
               helper.assertValueEqual(loaded, state, "all existing gate states must load without migration");
               var legacy = saved.copy();
               for (String key : java.util.List.of("waterlogged", "fluid_cell", "water_flow")) legacy.getCompound("Properties").remove(key);
               helper.assertValueEqual(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), legacy), state,
                  "legacy gate geometry loads with empty cells");
               Set<String> properties = loaded.getProperties().stream().map(Property::getName).collect(Collectors.toSet());
               helper.assertValueEqual(properties, Set.of("facing", "open", "segment", "waterlogged", "fluid_cell", "water_flow"),
                  "native cells extend the three-property gate geometry");
               loaded.getShape(helper.getLevel(), helper.absolutePos(GATE));
               loaded.getCollisionShape(helper.getLevel(), helper.absolutePos(GATE));
               helper.assertValueEqual(NbtUtils.writeBlockState(loaded), saved, "shape queries must preserve saved values");
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void redstone_updates_preserve_manual_gate_interaction(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         for (boolean initiallyOpen : new boolean[]{false, true}) {
            clearFixture(helper);
            helper.setBlock(GATE, gateState(facing, initiallyOpen, 0));
            helper.setBlock(GATE.above(), gateState(facing, initiallyOpen, 1));
            BlockPos signal = GATE.relative(facing);
            helper.setBlock(signal, Blocks.REDSTONE_BLOCK);
            helper.assertTrue(helper.getLevel().hasNeighborSignal(helper.absolutePos(GATE)), "fixture must provide real redstone power");
            assertGatePairOpen(helper, initiallyOpen);
            helper.useBlock(GATE.above(), player);
            assertGatePairOpen(helper, !initiallyOpen);
            helper.setBlock(signal, Blocks.AIR);
            assertGatePairOpen(helper, !initiallyOpen);
            helper.useBlock(GATE, player);
            assertGatePairOpen(helper, initiallyOpen);
         }
      }
      helper.succeed();
   }

   private static void assertPassageMatrix(GameTestHelper helper, boolean open, boolean reload) {
      List<String> failures = new ArrayList<>();
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         for (int arrangement = 0; arrangement <= 2; arrangement++) {
            for (Direction travel : List.of(facing, facing.getOpposite())) {
               clearFixture(helper);
               BlockState lower = gateState(facing, open, arrangement == 1 ? 1 : 0);
               helper.setBlock(GATE, reload ? roundTrip(lower) : lower);
               if (arrangement == 2) {
                  BlockState upper = gateState(facing, open, 1);
                  helper.setBlock(GATE.above(), reload ? roundTrip(upper) : upper);
               }
               // Include the adjacent mesh fences from the report. Their own collisions must not
               // steal the clearance gained by correcting the gate's physical collision.
               for (Direction side : List.of(facing.getClockWise(), facing.getCounterClockWise())) {
                  placeFence(helper, GATE.relative(side));
                  if (arrangement == 2) {
                     placeFence(helper, GATE.above().relative(side));
                  }
               }
               double distance = movePlayerThroughGate(helper, travel);
               boolean expected = open ? Math.abs(distance - 2.5) < EPSILON : distance < 1.25;
               if (!expected) {
                  failures.add("facing=" + facing + ", travel=" + travel + ", arrangement=" + arrangement
                     + ", open=" + open + ", saved=" + reload + ", moved=" + distance);
               }
               helper.assertValueEqual(helper.getBlockState(GATE), lower, "movement must preserve gate state");
            }
         }
      }
      helper.assertTrue(failures.isEmpty(), "real player passage failures=" + failures.size() + "; " + failures);
   }

   private static double movePlayerThroughGate(GameTestHelper helper, Direction travel) {
      return movePlayerThroughGate(helper, travel, Vec3.ZERO);
   }

   private static double movePlayerThroughGate(GameTestHelper helper, Direction travel, Vec3 lane) {
      BlockPos pos = helper.absolutePos(GATE);
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      Vec3 start = new Vec3(pos.getX() + 0.5 - 1.25 * travel.getStepX() + lane.x, pos.getY(),
         pos.getZ() + 0.5 - 1.25 * travel.getStepZ() + lane.z);
      player.setPos(start.x, start.y, start.z);
      player.setOnGround(false);
      helper.assertTrue(!player.isSpectator() && !player.noPhysics && !player.getAbilities().flying,
         "fixture must use a real colliding, non-flying survival player");
      helper.assertTrue(Math.abs(player.getBoundingBox().getXsize() - 0.6) < EPSILON,
         "passage must fit the actual 0.6-block player width");
      helper.assertTrue(helper.getLevel().noCollision(player), "player must start outside all gate and fence collisions");
      // Deterministic prescribed walking displacements, resolved by Entity.move against real world
      // collisions. No teleport crosses the gate; a Boolean OPEN or a small center probe cannot pass.
      for (int step = 0; step < 25; step++) {
         player.move(MoverType.SELF, new Vec3(travel.getStepX() * 0.1, 0, travel.getStepZ() * 0.1));
      }
      helper.assertTrue(Math.abs(player.getY() - start.y) < EPSILON, "test may not step or jump over the gate");
      return (player.getX() - start.x) * travel.getStepX() + (player.getZ() - start.z) * travel.getStepZ();
   }

   private static void clearFixture(GameTestHelper helper) {
      BlockPos center = helper.absolutePos(GATE);
      for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, 0, -2), center.offset(2, 3, 2))) {
         helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
      }
      for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -1, -2), center.offset(2, -1, 2))) {
         helper.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
      }
   }

   private static BlockState gateState(Direction facing, boolean open, int segment) {
      return ModBlocks.STEEL_MESH_FENCE_GATE.get().defaultBlockState().setValue(PortedBlocks.HORIZONTAL_FACING, facing)
         .setValue(PortedBlocks.OPEN, open).setValue(PortedBlocks.SEGMENT, segment);
   }

   private static BlockState roundTrip(BlockState state) {
      return NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), NbtUtils.writeBlockState(state));
   }

   private static void placeFence(GameTestHelper helper, BlockPos relativePos) {
      Block fence = ModBlocks.STEEL_MESH_FENCE.get();
      BlockPos pos = helper.absolutePos(relativePos);
      BlockPlaceContext context = new BlockPlaceContext(helper.getLevel(), helper.makeMockPlayer(GameType.SURVIVAL),
         InteractionHand.MAIN_HAND, new ItemStack(fence), new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
      BlockState state = fence.getStateForPlacement(context);
      helper.assertTrue(state != null, "mesh fence placement must produce a state");
      helper.setBlock(relativePos, state);
   }

   private static void assertGatePairOpen(GameTestHelper helper, boolean open) {
      helper.assertBlockProperty(GATE, PortedBlocks.OPEN, open);
      helper.assertBlockProperty(GATE.above(), PortedBlocks.OPEN, open);
   }

   private static void assertPairedConnections(GameTestHelper helper, Direction facing, boolean connected) {
      for (Direction side : List.of(facing.getClockWise(), facing.getCounterClockWise())) {
         for (int segment = 0; segment <= 1; segment++) {
            BlockState state = helper.getBlockState(GATE.above(segment).relative(side));
            helper.assertTrue((state.getValue(wallSide(side.getOpposite())) != WallSide.NONE) == connected,
               "both fence levels must update beside both gate posts: facing=" + facing + ", side=" + side + ", segment=" + segment);
         }
      }
   }

   private static EnumProperty<WallSide> wallSide(Direction direction) {
      return switch (direction) {
         case NORTH -> WallBlock.NORTH_WALL;
         case SOUTH -> WallBlock.SOUTH_WALL;
         case EAST -> WallBlock.EAST_WALL;
         case WEST -> WallBlock.WEST_WALL;
         default -> throw new IllegalArgumentException("Expected horizontal wall direction");
      };
   }
}
