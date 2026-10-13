package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Physical containment for a walking adult zombie, not jumping, climbing, flying, or teleporting mobs. */
@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NativeRailingContainmentGameTests {
   private static final BlockPos CENTER = new BlockPos(3, 1, 3);
   private static final double EPSILON = 1.0E-6;
   private static final double WALK_STEP = 0.1;
   private static final double CENTER_STEP = 0.025;
   private static final int WALK_STEPS = 15;
   private static final BooleanProperty[] SIDES = {
      PortedBlocks.NORTH, PortedBlocks.EAST, PortedBlocks.SOUTH, PortedBlocks.WEST
   };
   // Cardinal edges first, followed by all four corners. Diagonals use the same walking speed.
   private static final Vec3[] ROUTES = {
      new Vec3(0, 0, -1), new Vec3(1, 0, 0), new Vec3(0, 0, 1), new Vec3(-1, 0, 0),
      new Vec3(1, 0, -1).normalize(), new Vec3(1, 0, 1).normalize(),
      new Vec3(-1, 0, 1).normalize(), new Vec3(-1, 0, -1).normalize()
   };
   private static final String[] ROUTE_NAMES = {
      "north", "east", "south", "west", "north-east", "south-east", "south-west", "north-west"
   };

   private NativeRailingContainmentGameTests() {
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void four_sided_catwalk_contains_walking_mob_at_every_edge_and_corner(GameTestHelper helper) {
      assertContainment(helper, ModBlocks.STEEL_CATWALK.get());
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void four_sided_standalone_railing_contains_walking_mob_at_every_edge_and_corner(GameTestHelper helper) {
      assertContainment(helper, ModBlocks.STEEL_RAILING.get());
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void four_sided_railing_cells_leave_real_mob_movement_in_the_center(GameTestHelper helper) {
      for (Block block : blocks()) {
         prepareFixture(helper, allSides(block));
         for (int route = 0; route < ROUTES.length; route++) {
            String context = block + ", center route=" + ROUTE_NAMES[route];
            Zombie mob = spawnWalkingMob(helper, block);
            try {
               Vec3 start = mob.position();
               Vec3 offset = ROUTES[route].scale(CENTER_STEP);
               walk(mob, offset);
               assertPosition(helper, mob, start.add(offset), "center must remain navigable: " + context);
               helper.assertTrue(helper.getLevel().noCollision(mob), "center movement must not overlap a rail: " + context);
               walk(mob, offset.scale(-1));
               assertPosition(helper, mob, start, "mob must be able to walk back to the center: " + context);
            } finally {
               mob.discard();
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void opening_each_side_allows_the_same_walking_mob_to_leave(GameTestHelper helper) {
      for (Block block : blocks()) {
         for (int side = 0; side < SIDES.length; side++) {
            BlockState state = allSides(block).setValue(SIDES[side], false);
            prepareFixture(helper, state);
            String context = block + ", open side=" + ROUTE_NAMES[side];
            Zombie mob = spawnWalkingMob(helper, block);
            try {
               Vec3 start = mob.position();
               Vec3 movement = ROUTES[side].scale(WALK_STEP);
               for (int step = 0; step < WALK_STEPS; step++) {
                  walk(mob, movement);
                  assertWalkingHeight(helper, mob, start.y, context);
                  helper.assertTrue(helper.getLevel().noCollision(mob), "open route must have real clearance: " + context);
               }
               assertPosition(helper, mob, start.add(movement.scale(WALK_STEPS)),
                  "removing the selected rail must permit the entire attempted walk: " + context);
               helper.assertTrue(completelyOutside(mob.getBoundingBox(), helper.absolutePos(CENTER), side),
                  "the whole mob must leave through the selected opening: " + context);
               helper.assertValueEqual(helper.getBlockState(CENTER), state, "walking must preserve the selected sides");
            } finally {
               mob.discard();
            }
         }
      }
      helper.succeed();
   }

   private static void assertContainment(GameTestHelper helper, Block block) {
      BlockState state = allSides(block);
      prepareFixture(helper, state);
      BlockPos pos = helper.absolutePos(CENTER);
      for (int route = 0; route < ROUTES.length; route++) {
         String context = block + ", route=" + ROUTE_NAMES[route];
         Zombie mob = spawnWalkingMob(helper, block);
         try {
            Vec3 start = mob.position();
            Vec3 movement = ROUTES[route].scale(WALK_STEP);
            // AI is disabled only to make input deterministic. Entity.move still resolves actual
            // world collision with the mob's normal dimensions and ordinary step height.
            for (int step = 0; step < WALK_STEPS; step++) {
               walk(mob, movement);
               AABB body = mob.getBoundingBox();
               helper.assertTrue(body.minX >= pos.getX() - EPSILON && body.maxX <= pos.getX() + 1 + EPSILON
                     && body.minZ >= pos.getZ() - EPSILON && body.maxZ <= pos.getZ() + 1 + EPSILON,
                  "the whole mob must remain inside the cell on every movement step: " + context + ", step=" + step);
               assertWalkingHeight(helper, mob, start.y, context);
               helper.assertTrue(helper.getLevel().noCollision(mob), "blocked movement must not penetrate a rail: " + context);
            }
            helper.assertTrue(mob.position().subtract(start).dot(ROUTES[route]) > CENTER_STEP,
               "mob must approach the rail rather than start stuck in an invalid fixture: " + context);
            helper.assertValueEqual(helper.getBlockState(CENTER), state, "movement must preserve all four installed sides");
         } finally {
            mob.discard();
         }
      }
   }

   private static List<Block> blocks() {
      return List.of(ModBlocks.STEEL_CATWALK.get(), ModBlocks.STEEL_RAILING.get());
   }

   private static BlockState allSides(Block block) {
      BlockState state = block.defaultBlockState();
      for (BooleanProperty side : SIDES) {
         state = state.setValue(side, true);
      }
      return state;
   }

   private static void prepareFixture(GameTestHelper helper, BlockState state) {
      BlockPos pos = helper.absolutePos(CENTER);
      for (BlockPos clear : BlockPos.betweenClosed(pos.offset(-2, 0, -2), pos.offset(2, 3, 2))) {
         helper.getLevel().setBlock(clear, Blocks.AIR.defaultBlockState(), 2);
      }
      for (BlockPos floor : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, -1, 2))) {
         helper.getLevel().setBlock(floor, Blocks.STONE.defaultBlockState(), 2);
         if (state.is(ModBlocks.STEEL_CATWALK.get())) {
            // Continue the catwalk's 3/16-block deck outside the cell so the open-side control
            // walks at the same height instead of falling off the edge of an isolated platform.
            helper.getLevel().setBlock(floor.above(), ModBlocks.STEEL_CATWALK.get().defaultBlockState(), 2);
         }
      }
      helper.getLevel().setBlock(pos, state, 2);
   }

   private static Zombie spawnWalkingMob(GameTestHelper helper, Block block) {
      Zombie mob = EntityType.ZOMBIE.create(helper.getLevel());
      helper.assertTrue(mob != null, "adult zombie fixture must be created");
      mob.setBaby(false);
      mob.setNoAi(true);
      BlockPos pos = helper.absolutePos(CENTER);
      double feetY = pos.getY() + (block == ModBlocks.STEEL_CATWALK.get() ? 3.0 / 16.0 : 0.0);
      mob.setPos(pos.getX() + 0.5, feetY, pos.getZ() + 0.5);
      mob.setOnGround(true);
      helper.assertTrue(!mob.noPhysics && Math.abs(mob.getBoundingBox().getXsize() - 0.6) < EPSILON,
         "fixture must use an ordinary colliding adult zombie that fits between the rails");
      helper.assertTrue(helper.getLevel().noCollision(mob), "mob must start clear of all rails and the supporting floor");
      helper.assertTrue(helper.getLevel().addFreshEntity(mob), "walking fixture must contain a real server entity");
      return mob;
   }

   private static void walk(Zombie mob, Vec3 horizontalMovement) {
      // A downward component keeps contact with the real floor and allows vanilla stepping.
      // No jump, levitation, teleport, or setPos is used to traverse any tested boundary.
      mob.move(MoverType.SELF, new Vec3(horizontalMovement.x, -0.08, horizontalMovement.z));
   }

   private static void assertWalkingHeight(GameTestHelper helper, Zombie mob, double feetY, String context) {
      helper.assertTrue(Math.abs(mob.getY() - feetY) < EPSILON && mob.onGround(),
         "mob must remain grounded at normal walking height: " + context);
   }

   private static void assertPosition(GameTestHelper helper, Zombie mob, Vec3 expected, String context) {
      helper.assertTrue(mob.position().distanceToSqr(expected) < EPSILON * EPSILON,
         context + ", expected=" + expected + ", actual=" + mob.position());
      assertWalkingHeight(helper, mob, expected.y, context);
   }

   private static boolean completelyOutside(AABB body, BlockPos pos, int side) {
      return switch (side) {
         case 0 -> body.maxZ < pos.getZ();
         case 1 -> body.minX > pos.getX() + 1;
         case 2 -> body.minZ > pos.getZ() + 1;
         case 3 -> body.maxX < pos.getX();
         default -> throw new IllegalArgumentException("Expected one of the four horizontal sides");
      };
   }
}
