package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HatchLadderTravelGameTests {
   private static final BlockPos HATCH = new BlockPos(3, 4, 3);
   private static final double EPSILON = 1.0E-6;

   private HatchLadderTravelGameTests() {
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void vanilla_ladder_physically_clears_open_iron_hatch(GameTestHelper helper) {
      assertAscentInEveryDirection(helper, Blocks.LADDER, false);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void metal_rung_ladder_physically_clears_open_iron_hatch(GameTestHelper helper) {
      assertAscentInEveryDirection(helper, ModBlocks.METAL_RUNG_LADDER.get(), false);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void staggered_metal_steps_physically_clear_open_iron_hatch(GameTestHelper helper) {
      assertAscentInEveryDirection(helper, ModBlocks.METAL_RUNG_STEPS.get(), false);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void saved_metal_ladders_physically_clear_open_iron_hatch(GameTestHelper helper) {
      for (Block ladder : metalLadders()) {
         assertAscentInEveryDirection(helper, ladder, true);
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void metal_ladder_collision_preserves_vanilla_wall_clearance(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(HATCH.below());
      for (Block ladder : metalLadders()) {
         for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (boolean waterlogged : new boolean[]{false, true}) {
               BlockState state = ladder.defaultBlockState().setValue(LadderBlock.FACING, facing)
                  .setValue(LadderBlock.WATERLOGGED, waterlogged);
               BlockState vanilla = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, facing)
                  .setValue(LadderBlock.WATERLOGGED, waterlogged);
               var saved = NbtUtils.writeBlockState(state);
               BlockState loaded = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), saved);
               helper.assertValueEqual(loaded, state, "facing and waterlogged must load without migration");
               helper.assertTrue(loaded.getProperties().size() == 4, "only native cell and transient level extend the existing ladder schema");
               var legacy = saved.copy();
               legacy.getCompound("Properties").remove("fluid_cell");
               legacy.getCompound("Properties").remove("water_flow");
               helper.assertValueEqual(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), legacy), loaded,
                  "legacy ladder facing and waterlogging load with empty new properties");
               var actualCollision = loaded.getCollisionShape(helper.getLevel(), pos);
               var referenceCollision = vanilla.getCollisionShape(helper.getLevel(), pos);
               helper.assertTrue(!referenceCollision.isEmpty(), "vanilla reference must have physical wall clearance");
               helper.assertTrue(!Shapes.joinIsNotEmpty(actualCollision, referenceCollision, BooleanOp.NOT_SAME),
                  "ladder collision must retain vanilla wall clearance: " + ladder + ", facing=" + facing
                     + ", waterlogged=" + waterlogged);
               helper.assertValueEqual(loaded.getShape(helper.getLevel(), pos).bounds(),
                  vanilla.getShape(helper.getLevel(), pos).bounds(), "selection geometry must remain vanilla ladder geometry");
               helper.assertValueEqual(NbtUtils.writeBlockState(loaded), saved, "shape queries must not mutate saved state");
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void closed_iron_hatch_still_physically_blocks_ladder_ascent(GameTestHelper helper) {
      for (Block ladder : List.of(Blocks.LADDER, ModBlocks.METAL_RUNG_LADDER.get(), ModBlocks.METAL_RUNG_STEPS.get())) {
         for (Direction hinge : Direction.Plane.HORIZONTAL) {
            prepareFixture(helper, ladder, hinge, false, false, false);
            Ascent result = moveUpAgainstWall(helper, hinge);
            helper.assertTrue(result.finalFeetY() < helper.absolutePos(HATCH).getY(),
               "closed hatch must block real upward movement: " + ladder + ", hinge=" + hinge);
            helper.assertTrue(result.finalHeadY() <= helper.absolutePos(HATCH).getY() + EPSILON,
               "player must remain below the closed collision plate");
         }
      }
      helper.succeed();
   }

   private static List<Block> metalLadders() {
      return List.of(ModBlocks.METAL_RUNG_LADDER.get(), ModBlocks.METAL_RUNG_STEPS.get());
   }

   private static void assertAscentInEveryDirection(GameTestHelper helper, Block ladder, boolean reload) {
      for (Direction hinge : Direction.Plane.HORIZONTAL) {
         for (boolean powered : new boolean[]{false, true}) {
            prepareFixture(helper, ladder, hinge, true, powered, reload);
            BlockState originalHatch = helper.getBlockState(HATCH);
            BlockState originalLadder = helper.getBlockState(HATCH.below());
            Ascent result = moveUpAgainstWall(helper, hinge);
            double hatchY = helper.absolutePos(HATCH).getY();
            helper.assertTrue(result.finalFeetY() > hatchY + 0.05,
               "real upward movement must clear the hatch while hugging the support wall: " + ladder
                  + ", hinge=" + hinge + ", powered=" + powered + ", saved=" + reload
                  + ", initial wall gap=" + result.initialWallGap() + ", feet relative to hatch="
                  + (result.finalFeetY() - hatchY) + ", head relative to hatch=" + (result.finalHeadY() - hatchY));
            helper.assertValueEqual(helper.getBlockState(HATCH), originalHatch, "movement must not rotate or replace the hatch");
            helper.assertValueEqual(helper.getBlockState(HATCH.below()), originalLadder, "movement must preserve ladder state");
         }
      }
   }

   private static void prepareFixture(GameTestHelper helper, Block ladder, Direction hinge, boolean open,
                                      boolean powered, boolean reload) {
      BlockPos hatchPos = helper.absolutePos(HATCH);
      for (BlockPos pos : BlockPos.betweenClosed(hatchPos.offset(-1, -4, -1), hatchPos.offset(1, 2, 1))) {
         helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
      }
      helper.getLevel().setBlock(hatchPos.below(4), Blocks.STONE.defaultBlockState(), 2);
      BlockState ladderState = ladder.defaultBlockState().setValue(LadderBlock.FACING, hinge.getOpposite());
      BlockState hatchState = ModBlocks.IRON_HATCH.get().defaultBlockState()
         .setValue(PortedBlocks.HORIZONTAL_FACING, hinge).setValue(PortedBlocks.OPEN, open)
         .setValue(PortedBlocks.POWERED, powered);
      if (reload) {
         ladderState = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), NbtUtils.writeBlockState(ladderState));
         hatchState = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), NbtUtils.writeBlockState(hatchState));
      }
      for (int depth = 1; depth <= 3; depth++) {
         BlockPos pos = hatchPos.below(depth);
         helper.getLevel().setBlock(pos.relative(hinge), Blocks.STONE.defaultBlockState(), 2);
         helper.getLevel().setBlock(pos, ladderState, 2);
         helper.assertTrue(ladderState.canSurvive(helper.getLevel(), pos), "each shaft ladder must have a real supporting wall");
      }
      helper.getLevel().setBlock(hatchPos, hatchState, 2);
   }

   private static Ascent moveUpAgainstWall(GameTestHelper helper, Direction hinge) {
      BlockPos hatchPos = helper.absolutePos(HATCH);
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      player.setPos(hatchPos.getX() + 0.5, hatchPos.getY() - 2.75, hatchPos.getZ() + 0.5);
      helper.assertTrue(!player.isSpectator() && !player.getAbilities().flying, "fixture must use a non-flying survival player");
      helper.assertTrue(player.getBoundingBox().maxY < hatchPos.getY(), "initial player head must be below the hatch");
      // Approach the wall through the collision engine, rather than guessing the stand-off or
      // teleporting the player into an obstacle. Missing ladder collision lets the body reach the wall.
      player.move(MoverType.SELF, new Vec3(hinge.getStepX() * 0.5, 0.0, hinge.getStepZ() * 0.5));
      double initialWallGap = wallGap(player.getBoundingBox(), hatchPos, hinge);
      helper.assertTrue(initialWallGap >= -EPSILON, "player must not penetrate the supporting wall");
      helper.assertTrue(player.onClimbable(), "the player must begin on an actual climbable ladder");
      // Supply deterministic upward displacements to isolate physical clearance from client input
      // and gravity. Every step uses Entity.move's real block-collision resolution; setPos is never
      // used to cross the hatch. A true onClimbable() alone cannot satisfy this regression.
      for (int step = 0; step < 36 && player.getY() <= hatchPos.getY() + 0.05; step++) {
         helper.assertTrue(player.onClimbable(), "the route must remain climbable before each physical ascent step");
         player.move(MoverType.SELF, new Vec3(hinge.getStepX() * 0.05, 0.125, hinge.getStepZ() * 0.05));
      }
      return new Ascent(initialWallGap, player.getY(), player.getBoundingBox().maxY);
   }

   private static double wallGap(AABB body, BlockPos hatchPos, Direction hinge) {
      return switch (hinge) {
         case NORTH -> body.minZ - hatchPos.getZ();
         case SOUTH -> hatchPos.getZ() + 1.0 - body.maxZ;
         case WEST -> body.minX - hatchPos.getX();
         case EAST -> hatchPos.getX() + 1.0 - body.maxX;
         default -> throw new IllegalArgumentException("Expected horizontal hinge");
      };
   }

   private record Ascent(double initialWallGap, double finalFeetY, double finalHeadY) {
   }
}
