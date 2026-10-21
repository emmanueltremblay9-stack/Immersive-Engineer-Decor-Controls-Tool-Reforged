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
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Real server Entity.move collision with a representative adult zombie body. Jump input is a
 * deterministic ordinary ground-jump trajectory (0.42 impulse, 0.08 gravity, 0.98 vertical drag),
 * not a simulation of client auto-jump input, AI, flight, climbing, or modified movement effects.
 */
@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NativeRailingJumpBarrierGameTests {
   private static final BlockPos CENTER = new BlockPos(3, 1, 3);
   private static final double EPSILON = 1.0E-6;
   private static final double HORIZONTAL_STEP = 0.1;
   private static final double JUMP_IMPULSE = (double)0.42F;
   private static final double GRAVITY = 0.08;
   private static final double VERTICAL_DRAG = 0.98;
   private static final int ARC_STEPS = 20;
   private static final BooleanProperty[] SIDES = {
      PortedBlocks.NORTH, PortedBlocks.EAST, PortedBlocks.SOUTH, PortedBlocks.WEST
   };
   private static final Vec3[] ROUTES = {
      new Vec3(0, 0, -1), new Vec3(1, 0, 0), new Vec3(0, 0, 1), new Vec3(-1, 0, 0),
      new Vec3(1, 0, -1).normalize(), new Vec3(1, 0, 1).normalize(),
      new Vec3(-1, 0, 1).normalize(), new Vec3(-1, 0, -1).normalize()
   };
   private static final String[] ROUTE_NAMES = {
      "north", "east", "south", "west", "north-east", "south-east", "south-west", "north-west"
   };

   private NativeRailingJumpBarrierGameTests() {
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void catwalk_blocks_ordinary_ground_jumps_at_every_edge_and_corner(GameTestHelper helper) {
      assertClosedJumps(helper, ModBlocks.STEEL_CATWALK.get());
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void standalone_railing_blocks_ordinary_ground_jumps_at_every_edge_and_corner(GameTestHelper helper) {
      assertClosedJumps(helper, ModBlocks.STEEL_RAILING.get());
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void railing_barriers_block_grounded_steps_and_pushes_at_every_edge_and_corner(GameTestHelper helper) {
      for (Block block : blocks()) {
         prepareFixture(helper, sides(block, 15));
         for (int route = 0; route < ROUTES.length; route++) {
            for (boolean pushed : new boolean[]{false, true}) {
               String context = block + ", route=" + ROUTE_NAMES[route] + ", pushed=" + pushed;
               Zombie mob = spawnMob(helper, block);
               try {
                  Vec3 start = mob.position();
                  Vec3 movement = ROUTES[route].scale(HORIZONTAL_STEP);
                  for (int step = 0; step < ARC_STEPS; step++) {
                     groundedMove(mob, movement, pushed);
                     assertInside(helper, mob, context + ", step=" + step);
                     assertGrounded(helper, mob, start.y, context);
                  }
                  helper.assertTrue(mob.position().subtract(start).dot(ROUTES[route]) > 0.025,
                     "mob must have room to approach the barrier: " + context);
               } finally {
                  mob.discard();
               }
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void opening_selected_side_allows_walking_and_normal_jumping_through_it(GameTestHelper helper) {
      for (Block block : blocks()) {
         for (int side = 0; side < SIDES.length; side++) {
            BlockState open = sides(block, 15).setValue(SIDES[side], false);
            prepareFixture(helper, open);
            for (boolean jumping : new boolean[]{false, true}) {
               String context = block + ", opening=" + ROUTE_NAMES[side] + ", jumping=" + jumping;
               Zombie mob = spawnMob(helper, block);
               try {
                  Vec3 start = mob.position();
                  Vec3 movement = ROUTES[side].scale(HORIZONTAL_STEP);
                  if (jumping) {
                     jumpArc(helper, mob, movement, false, context);
                  } else {
                     for (int step = 0; step < ARC_STEPS; step++) {
                        groundedMove(mob, movement, false);
                        assertGrounded(helper, mob, start.y, context);
                        assertClear(helper, mob, context);
                     }
                  }
                  Vec3 expected = start.add(movement.scale(ARC_STEPS));
                  helper.assertTrue(mob.position().distanceToSqr(expected) < EPSILON * EPSILON,
                     "selected opening must permit the full attempted movement: " + context);
                  helper.assertTrue(completelyOutside(mob.getBoundingBox(), helper.absolutePos(CENTER), side),
                     "the complete body must leave through the selected opening: " + context);
                  helper.assertValueEqual(helper.getBlockState(CENTER), open, "movement must preserve the selected opening");
               } finally {
                  mob.discard();
               }
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void physical_barriers_extend_above_unchanged_selection_shapes_for_every_side_mask(GameTestHelper helper) {
      for (Block block : blocks()) {
         boolean catwalk = block == ModBlocks.STEEL_CATWALK.get();
         double physicalTop = catwalk ? 27.0 : 24.0;
         for (int mask = 0; mask < 16; mask++) {
            for (boolean waterlogged : new boolean[]{false, true}) {
               BlockState state = sides(block, mask).setValue(PortedBlocks.WATERLOGGED, waterlogged);
               // A standalone zero-side state retains its established north-side fallback.
               int effectiveMask = !catwalk && mask == 0 ? 1 : mask;
               VoxelShape physical = state.getCollisionShape(helper.getLevel(), helper.absolutePos(CENTER));
               VoxelShape outline = state.getShape(helper.getLevel(), helper.absolutePos(CENTER));
               String context = block + ", mask=" + mask + ", waterlogged=" + waterlogged;
               assertSameShape(helper, physical, expectedShape(effectiveMask, catwalk, physicalTop),
                  "physical side barriers must end exactly 1.5 blocks above the walking floor: " + context);
               assertSameShape(helper, outline, expectedShape(effectiveMask, catwalk, 16.0),
                  "outline/raycast shape must retain the original visible rail height: " + context);
            }
         }
      }
      helper.succeed();
   }

   private static void assertClosedJumps(GameTestHelper helper, Block block) {
      BlockState state = sides(block, 15);
      prepareFixture(helper, state);
      for (int route = 0; route < ROUTES.length; route++) {
         String context = block + ", route=" + ROUTE_NAMES[route];
         Zombie mob = spawnMob(helper, block);
         try {
            Vec3 start = mob.position();
            Vec3 movement = ROUTES[route].scale(HORIZONTAL_STEP);
            // First jump approaches the rail; the second starts against it after landing.
            // Never reposition, teleport, disable collision, or resize the body during a route.
            jumpArc(helper, mob, movement, true, context + ", first jump");
            jumpArc(helper, mob, movement, true, context + ", repeated jump against rail");
            helper.assertTrue(mob.position().subtract(start).dot(ROUTES[route]) > 0.025,
               "mob must approach the rail rather than start stuck: " + context);
            helper.assertValueEqual(helper.getBlockState(CENTER), state, "jumping must preserve all installed sides");
         } finally {
            mob.discard();
         }
      }
   }

   private static void jumpArc(GameTestHelper helper, Zombie mob, Vec3 horizontal, boolean enclosed, String context) {
      double floorY = mob.getY();
      assertGrounded(helper, mob, floorY, "ordinary jump must start on the floor: " + context);
      double vertical = JUMP_IMPULSE;
      double maximumRise = 0;
      for (int step = 0; step < ARC_STEPS; step++) {
         mob.setDeltaMovement(horizontal.x, vertical, horizontal.z);
         mob.move(MoverType.SELF, mob.getDeltaMovement());
         maximumRise = Math.max(maximumRise, mob.getY() - floorY);
         if (enclosed) {
            assertInside(helper, mob, context + ", step=" + step);
         } else {
            assertClear(helper, mob, context + ", step=" + step);
         }
         // Entity.move owns world collision and vanilla step-up resolution. These constants
         // supply only the ordinary vertical input between moves, without running mob AI.
         vertical = mob.verticalCollision ? -GRAVITY : (vertical - GRAVITY) * VERTICAL_DRAG;
      }
      helper.assertTrue(maximumRise > 1.20 && maximumRise < 1.30,
         "fixture must execute a complete ordinary-height jump, not merely walk into the rail: "
            + context + ", maximum rise=" + maximumRise);
      assertGrounded(helper, mob, floorY, "jump must land back on the walking floor: " + context);
   }

   private static void groundedMove(Zombie mob, Vec3 horizontal, boolean pushed) {
      mob.setDeltaMovement(0, -GRAVITY, 0);
      if (pushed) {
         mob.push(horizontal.x, 0, horizontal.z);
      } else {
         mob.setDeltaMovement(horizontal.x, -GRAVITY, horizontal.z);
      }
      // Ground contact leaves Entity.move's normal step-up handling active.
      mob.move(MoverType.SELF, mob.getDeltaMovement());
   }

   private static void assertInside(GameTestHelper helper, Zombie mob, String context) {
      BlockPos pos = helper.absolutePos(CENTER);
      AABB body = mob.getBoundingBox();
      helper.assertTrue(body.minX >= pos.getX() - EPSILON && body.maxX <= pos.getX() + 1 + EPSILON
            && body.minZ >= pos.getZ() - EPSILON && body.maxZ <= pos.getZ() + 1 + EPSILON,
         "the whole body must remain inside on every movement step: " + context);
      assertClear(helper, mob, context);
   }

   private static void assertClear(GameTestHelper helper, Zombie mob, String context) {
      helper.assertTrue(helper.getLevel().noCollision(mob), "movement must not overlap any physical barrier: " + context);
   }

   private static void assertGrounded(GameTestHelper helper, Zombie mob, double floorY, String context) {
      helper.assertTrue(mob.onGround() && Math.abs(mob.getY() - floorY) < EPSILON,
         "mob must be grounded at the expected floor height: " + context);
   }

   private static Zombie spawnMob(GameTestHelper helper, Block block) {
      Zombie mob = EntityType.ZOMBIE.create(helper.getLevel());
      helper.assertTrue(mob != null, "adult zombie fixture must be created");
      mob.setBaby(false);
      mob.setNoAi(true);
      BlockPos pos = helper.absolutePos(CENTER);
      double floorY = pos.getY() + (block == ModBlocks.STEEL_CATWALK.get() ? 3.0 / 16.0 : 0.0);
      mob.setPos(pos.getX() + 0.5, floorY, pos.getZ() + 0.5);
      mob.setOnGround(true);
      helper.assertTrue(!mob.noPhysics && Math.abs(mob.getBoundingBox().getXsize() - 0.6) < EPSILON,
         "fixture must have a normal colliding adult zombie body");
      assertClear(helper, mob, "fixture must begin inside the open center");
      helper.assertTrue(helper.getLevel().addFreshEntity(mob), "fixture must be a real server entity");
      return mob;
   }

   private static void prepareFixture(GameTestHelper helper, BlockState state) {
      BlockPos pos = helper.absolutePos(CENTER);
      for (BlockPos clear : BlockPos.betweenClosed(pos.offset(-2, 0, -2), pos.offset(2, 3, 2))) {
         helper.getLevel().setBlock(clear, Blocks.AIR.defaultBlockState(), 2);
      }
      for (BlockPos floor : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, -1, 2))) {
         helper.getLevel().setBlock(floor, Blocks.STONE.defaultBlockState(), 2);
         if (state.is(ModBlocks.STEEL_CATWALK.get())) {
            helper.getLevel().setBlock(floor.above(), ModBlocks.STEEL_CATWALK.get().defaultBlockState(), 2);
         }
      }
      helper.getLevel().setBlock(pos, state, 2);
   }

   private static VoxelShape expectedShape(int mask, boolean floor, double top) {
      VoxelShape shape = floor ? Block.box(0, 0, 0, 16, 3, 16) : Shapes.empty();
      VoxelShape[] sides = {
         Block.box(0, 3, 0, 16, top, 2), Block.box(14, 3, 0, 16, top, 16),
         Block.box(0, 3, 14, 16, top, 16), Block.box(0, 3, 0, 2, top, 16)
      };
      for (int side = 0; side < SIDES.length; side++) {
         if ((mask & (1 << side)) != 0) {
            shape = Shapes.or(shape, sides[side]);
         }
      }
      return shape;
   }

   private static void assertSameShape(GameTestHelper helper, VoxelShape actual, VoxelShape expected, String message) {
      helper.assertTrue(!Shapes.joinIsNotEmpty(actual, expected, BooleanOp.NOT_SAME), message);
   }

   private static List<Block> blocks() {
      return List.of(ModBlocks.STEEL_CATWALK.get(), ModBlocks.STEEL_RAILING.get());
   }

   private static BlockState sides(Block block, int mask) {
      BlockState state = block.defaultBlockState();
      for (int side = 0; side < SIDES.length; side++) {
         state = state.setValue(SIDES[side], (mask & (1 << side)) != 0);
      }
      return state;
   }

   private static boolean completelyOutside(AABB body, BlockPos pos, int side) {
      return switch (side) {
         case 0 -> body.maxZ < pos.getZ();
         case 1 -> body.minX > pos.getX() + 1;
         case 2 -> body.minZ > pos.getZ() + 1;
         case 3 -> body.maxX < pos.getX();
         default -> throw new IllegalArgumentException("Expected a horizontal side");
      };
   }
}
