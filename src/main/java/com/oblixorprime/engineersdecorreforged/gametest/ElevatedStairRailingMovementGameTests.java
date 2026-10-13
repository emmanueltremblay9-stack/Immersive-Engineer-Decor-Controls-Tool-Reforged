package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real server Entity.move, including its normal step-up resolution, on an elevated stair flight.
 * The ordinary jump input is deterministic (0.42 impulse / 0.08 gravity / 0.98 drag).
 * This does not claim to drive LocalPlayer's client auto-jump input routine.
 */
@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ElevatedStairRailingMovementGameTests {
   private static final BlockPos STAIR = new BlockPos(4, 4, 4);
   private static final double EPSILON = 1.0E-6;
   private static final int MOVES = 24;

   private ElevatedStairRailingMovementGameTests() {}

   @GameTest(template = "elevated_stair_collision", timeoutTicks = 100)
   public static void elevated_treads_block_sideways_steps_and_ordinary_jumps_in_all_facings(GameTestHelper helper) {
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         for (boolean high : new boolean[]{false, true}) {
            for (boolean left : new boolean[]{false, true}) {
               for (boolean jumping : new boolean[]{false, true}) {
                  runRoute(helper, facing, high, left, jumping, false, true);
               }
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "elevated_stair_collision", timeoutTicks = 100)
   public static void elevated_upper_tread_diagonal_jumps_cannot_cross_the_lower_side_rail(GameTestHelper helper) {
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         for (boolean left : new boolean[]{false, true}) {
            runRoute(helper, facing, true, left, true, true, true);
         }
      }
      helper.succeed();
   }

   @GameTest(template = "elevated_stair_collision", timeoutTicks = 100)
   public static void removing_only_the_attempted_stair_side_allows_the_same_movements_out(GameTestHelper helper) {
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         for (boolean high : new boolean[]{false, true}) {
            for (boolean left : new boolean[]{false, true}) {
               for (boolean jumping : new boolean[]{false, true}) {
                  runRoute(helper, facing, high, left, jumping, false, false);
               }
            }
         }
         for (boolean left : new boolean[]{false, true}) {
            runRoute(helper, facing, true, left, true, true, false);
         }
      }
      helper.succeed();
   }

   @GameTest(template = "elevated_stair_collision", timeoutTicks = 100)
   public static void elevated_side_rails_preserve_normal_uphill_and_downhill_center_movement(GameTestHelper helper) {
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         for (boolean uphill : new boolean[]{false, true}) {
            BlockState state = stair(facing, true, true);
            prepareFixture(helper, state);
            Zombie mob = spawn(helper, facing, !uphill);
            try {
               Vec3 start = mob.position();
               Direction direction = uphill ? facing : facing.getOpposite();
               String context = "facing=" + facing + ", uphill=" + uphill;
               double vertical = -0.08;
               for (int tick = 0; tick < 12; tick++) {
                  mob.move(MoverType.SELF,
                     new Vec3(direction.getStepX() * 0.1, vertical, direction.getStepZ() * 0.1));
                  vertical = mob.verticalCollision ? -0.08 : (vertical - 0.08) * 0.98;
                  helper.assertTrue(helper.getLevel().noCollision(mob),
                     "normal center movement may not overlap the side guards: " + context);
               }
               double distance = mob.position().subtract(start).dot(Vec3.atLowerCornerOf(direction.getNormal()));
               double expectedY = helper.absolutePos(STAIR).getY() + (uphill ? 1.1875 : 0.1875);
               helper.assertTrue(Math.abs(distance - 1.2) < EPSILON,
                  "normal step-up/down movement must cross the entire center route: " + context);
               helper.assertTrue(mob.onGround() && Math.abs(mob.getY() - expectedY) < EPSILON,
                  "normal movement must reach its connected platform/landing: " + context);
               helper.assertValueEqual(helper.getBlockState(STAIR), state, "center travel must preserve installed sides");
            } finally {
               mob.discard();
            }
         }
      }
      helper.succeed();
   }

   private static void runRoute(GameTestHelper helper, Direction facing, boolean high, boolean left,
                                boolean jumping, boolean downhill, boolean closed) {
      BlockState state = stair(facing, left, closed);
      prepareFixture(helper, state);
      Direction side = left ? facing.getCounterClockWise() : facing.getClockWise();
      String context = "facing=" + facing + ", high=" + high + ", left=" + left
         + ", jump=" + jumping + ", downhill=" + downhill + ", closed=" + closed;
      Zombie mob = spawn(helper, facing, high);
      try {
         Vec3 start = mob.position();
         double vertical = jumping ? (double)0.42F : -0.08;
         double maximumRise = 0;
         for (int tick = 0; tick < MOVES; tick++) {
            // From the upper tread, cross the tread seam while airborne, then continue sideways.
            // Longitudinal travel stops with the body still overlapping the lower side rail;
            // walking around the open end of a stair is deliberately not a containment claim.
            double descent = downhill && tick < 4 ? -0.12 : 0;
            Vec3 movement = new Vec3(side.getStepX() * 0.15 + facing.getStepX() * descent,
               vertical, side.getStepZ() * 0.15 + facing.getStepZ() * descent);
            mob.setDeltaMovement(movement);
            mob.move(MoverType.SELF, movement);
            maximumRise = Math.max(maximumRise, mob.getY() - start.y);
            if (closed) {
               assertInsideSide(helper, mob, side, context + ", move=" + tick);
            }
            helper.assertTrue(helper.getLevel().noCollision(mob), "body may not overlap blocks: " + context);
            vertical = mob.verticalCollision ? -0.08 : (vertical - 0.08) * 0.98;
         }
         if (jumping) {
            helper.assertTrue(maximumRise > 1.20 && maximumRise < 1.30,
               "must execute an ordinary complete jump, actual rise=" + maximumRise + ": " + context);
         }
         if (closed) {
            double floor = helper.absolutePos(STAIR).getY() + (high && !downhill ? 0.75 : 0.25);
            helper.assertTrue(mob.onGround() && Math.abs(mob.getY() - floor) < EPSILON,
               "contained entity must land back on the expected tread: " + context);
            helper.assertTrue(mob.position().subtract(start).dot(Vec3.atLowerCornerOf(side.getNormal())) > 0.025,
               "fixture must actually approach the rail: " + context);
         } else {
            helper.assertTrue(completelyOutside(mob.getBoundingBox(), helper.absolutePos(STAIR), side),
               "the full body must exit through the removed side using identical movement: " + context);
         }
         helper.assertValueEqual(helper.getBlockState(STAIR), state,
            "physical movement must preserve sides, paid credits, edit flags and waterlogging: " + context);
      } finally {
         mob.discard();
      }
   }

   private static BlockState stair(Direction facing, boolean left, boolean closed) {
      return ModBlocks.STEEL_CATWALK_STAIRS.get().defaultBlockState()
         .setValue(PortedBlocks.HORIZONTAL_FACING, facing)
         .setValue(PortedBlocks.LEFT_RAILING, !left || closed)
         .setValue(PortedBlocks.RIGHT_RAILING, left || closed)
         .setValue(PortedBlocks.EDITED_STAIR_SIDES, 3)
         .setValue(PortedBlocks.PAID_RAILINGS, closed ? 2 : 1);
   }

   private static void prepareFixture(GameTestHelper helper, BlockState state) {
      BlockPos pos = helper.absolutePos(STAIR);
      for (BlockPos clear : BlockPos.betweenClosed(pos.offset(-4, -3, -4), pos.offset(4, 4, 4))) {
         helper.getLevel().setBlock(clear, Blocks.AIR.defaultBlockState(), 2);
      }
      for (BlockPos floor : BlockPos.betweenClosed(pos.offset(-4, -4, -4), pos.offset(4, -4, 4))) {
         helper.getLevel().setBlock(floor, Blocks.STONE.defaultBlockState(), 2);
      }
      Direction facing = state.getValue(PortedBlocks.HORIZONTAL_FACING);
      // Real elevated landing and upper platform, with air outside each guarded side.
      helper.getLevel().setBlock(pos.relative(facing.getOpposite()),
         ModBlocks.STEEL_CATWALK.get().defaultBlockState(), 2);
      helper.getLevel().setBlock(pos.relative(facing).above(),
         ModBlocks.STEEL_CATWALK.get().defaultBlockState(), 2);
      helper.getLevel().setBlock(pos, state, 2);
   }

   private static Zombie spawn(GameTestHelper helper, Direction facing, boolean high) {
      Zombie mob = EntityType.ZOMBIE.create(helper.getLevel());
      helper.assertTrue(mob != null, "adult movement fixture must be created");
      mob.setBaby(false);
      mob.setNoAi(true);
      BlockPos pos = helper.absolutePos(STAIR);
      // A 0.6-wide body fits the lower half only when slightly overhanging its open bottom end.
      // Upper-tread placement stays clear of the adjacent raised platform.
      double depth = high ? 0.15 : -0.31;
      mob.setPos(pos.getX() + 0.5 + facing.getStepX() * depth,
         pos.getY() + (high ? 0.75 : 0.25), pos.getZ() + 0.5 + facing.getStepZ() * depth);
      mob.setOnGround(true);
      helper.assertTrue(!mob.noPhysics && Math.abs(mob.getBoundingBox().getXsize() - 0.6) < EPSILON,
         "use an ordinary colliding adult body");
      helper.assertTrue(helper.getLevel().noCollision(mob), "fixture must start clear on its tread");
      helper.assertTrue(helper.getLevel().addFreshEntity(mob), "fixture must be a real server entity");
      mob.move(MoverType.SELF, new Vec3(0, -0.08, 0));
      helper.assertTrue(mob.onGround(), "fixture must have real ground contact before moving");
      return mob;
   }

   private static void assertInsideSide(GameTestHelper helper, Zombie mob, Direction side, String context) {
      AABB body = mob.getBoundingBox();
      BlockPos pos = helper.absolutePos(STAIR);
      boolean inside = switch (side) {
         case NORTH -> body.minZ >= pos.getZ() - EPSILON;
         case SOUTH -> body.maxZ <= pos.getZ() + 1 + EPSILON;
         case WEST -> body.minX >= pos.getX() - EPSILON;
         case EAST -> body.maxX <= pos.getX() + 1 + EPSILON;
         default -> false;
      };
      helper.assertTrue(inside, "whole body must remain behind the installed side on every move: " + context);
   }

   private static boolean completelyOutside(AABB body, BlockPos pos, Direction side) {
      return switch (side) {
         case NORTH -> body.maxZ < pos.getZ() - EPSILON;
         case SOUTH -> body.minZ > pos.getZ() + 1 + EPSILON;
         case WEST -> body.maxX < pos.getX() - EPSILON;
         case EAST -> body.minX > pos.getX() + 1 + EPSILON;
         default -> false;
      };
   }
}
