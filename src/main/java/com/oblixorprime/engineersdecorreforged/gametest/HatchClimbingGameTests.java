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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HatchClimbingGameTests {
   private static final BlockPos CENTER = new BlockPos(2, 2, 2);

   private HatchClimbingGameTests() {
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void iron_hatch_continues_ladders_on_its_open_leaf_edge(GameTestHelper helper) {
      for (Block ladder : ladders()) {
         for (Direction hinge : Direction.Plane.HORIZONTAL) {
            for (boolean powered : new boolean[]{false, true}) {
               BlockState hatch = hatch(hinge, true, powered);
               prepareLadder(helper, ladder, hinge.getOpposite());
               assertSameSupportEdge(helper, hatch, helper.getBlockState(CENTER.below()));
               // A real vanilla trapdoor with the same physical hinge is the independent engine reference.
               BlockState vanilla = Blocks.IRON_TRAPDOOR.defaultBlockState()
                  .setValue(TrapDoorBlock.FACING, hinge.getOpposite())
                  .setValue(TrapDoorBlock.OPEN, true)
                  .setValue(TrapDoorBlock.POWERED, powered);
               assertPlayerClimbing(helper, vanilla, true, "vanilla aligned fixture");
               assertPlayerClimbing(helper, hatch, true, "aligned " + ladder + ", hinge=" + hinge + ", powered=" + powered);
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void iron_hatch_rejects_opposite_and_perpendicular_ladders(GameTestHelper helper) {
      for (Block ladder : ladders()) {
         for (Direction hinge : Direction.Plane.HORIZONTAL) {
            for (Direction ladderFacing : Direction.Plane.HORIZONTAL) {
               if (ladderFacing == hinge.getOpposite()) {
                  continue;
               }
               for (boolean powered : new boolean[]{false, true}) {
                  prepareLadder(helper, ladder, ladderFacing);
                  assertPlayerClimbing(helper, hatch(hinge, true, powered), false,
                     "misaligned " + ladder + ", hinge=" + hinge + ", ladder facing=" + ladderFacing);
               }
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void closed_iron_hatch_never_extends_a_ladder(GameTestHelper helper) {
      for (Block ladder : ladders()) {
         for (Direction hinge : Direction.Plane.HORIZONTAL) {
            for (Direction ladderFacing : Direction.Plane.HORIZONTAL) {
               for (boolean powered : new boolean[]{false, true}) {
                  prepareLadder(helper, ladder, ladderFacing);
                  assertPlayerClimbing(helper, hatch(hinge, false, powered), false,
                     "closed hatch, hinge=" + hinge + ", ladder facing=" + ladderFacing + ", powered=" + powered);
               }
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void iron_hatch_requires_a_ladder_immediately_below(GameTestHelper helper) {
      for (Direction hinge : Direction.Plane.HORIZONTAL) {
         for (boolean powered : new boolean[]{false, true}) {
            for (Block below : List.of(Blocks.AIR, Blocks.STONE, Blocks.SCAFFOLDING)) {
               clearFixture(helper);
               helper.getLevel().setBlock(helper.absolutePos(CENTER.below()), below.defaultBlockState(), 2);
               assertPlayerClimbing(helper, hatch(hinge, true, powered), false, "no matching ladder below: " + below);
            }
            prepareLadder(helper, ModBlocks.METAL_RUNG_LADDER.get(), hinge.getOpposite());
            BlockPos lower = helper.absolutePos(CENTER.below());
            BlockState ladder = helper.getLevel().getBlockState(lower);
            helper.getLevel().setBlock(lower.below().relative(hinge), Blocks.STONE.defaultBlockState(), 2);
            helper.getLevel().setBlock(lower.below(), ladder, 2);
            helper.getLevel().setBlock(lower, Blocks.AIR.defaultBlockState(), 2);
            assertPlayerClimbing(helper, hatch(hinge, true, powered), false, "a one-block gap must not extend the ladder");
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void iron_hatch_climbing_tracks_manual_and_redstone_open_state(GameTestHelper helper) {
      for (Direction hinge : Direction.Plane.HORIZONTAL) {
         prepareLadder(helper, ModBlocks.METAL_RUNG_LADDER.get(), hinge.getOpposite());
         BlockPos target = helper.absolutePos(CENTER);
         helper.getLevel().setBlock(target, hatch(hinge, false, false), 2);
         Player user = helper.makeMockPlayer(GameType.SURVIVAL);
         assertPlayerClimbing(helper, helper.getBlockState(CENTER), false, "initially closed");
         helper.useBlock(CENTER, user);
         helper.assertTrue(helper.getBlockState(CENTER).getValue(PortedBlocks.OPEN), "manual opening must succeed");
         assertPlayerClimbing(helper, helper.getBlockState(CENTER), true, "manually opened");
         helper.useBlock(CENTER, user);
         assertPlayerClimbing(helper, helper.getBlockState(CENTER), false, "manually closed");

         BlockPos power = target.relative(hinge);
         helper.getLevel().setBlock(power, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
         helper.assertTrue(helper.getBlockState(CENTER).getValue(PortedBlocks.POWERED), "redstone must power the hatch");
         helper.assertTrue(helper.getBlockState(CENTER).getValue(PortedBlocks.OPEN), "redstone must open the hatch");
         helper.useBlock(CENTER, user);
         assertPlayerClimbing(helper, helper.getBlockState(CENTER), true, "powered hatch stays open after use");
         helper.getLevel().setBlock(power, Blocks.AIR.defaultBlockState(), 3);
         helper.assertTrue(!helper.getBlockState(CENTER).getValue(PortedBlocks.POWERED), "removing power must clear powered state");
         helper.assertTrue(!helper.getBlockState(CENTER).getValue(PortedBlocks.OPEN), "removing power must close the hatch");
         assertPlayerClimbing(helper, helper.getBlockState(CENTER), false, "redstone closed");
         helper.assertValueEqual(helper.getBlockState(CENTER).getValue(PortedBlocks.HORIZONTAL_FACING), hinge,
            "opening and closing must not rotate the saved hinge");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void saved_iron_hatch_states_keep_their_hinge_and_climbing_behavior(GameTestHelper helper) {
      for (Block ladder : ladders()) {
         for (Direction hinge : Direction.Plane.HORIZONTAL) {
            for (boolean open : new boolean[]{false, true}) {
               for (boolean powered : new boolean[]{false, true}) {
                  prepareLadder(helper, ladder, hinge.getOpposite());
                  BlockState original = hatch(hinge, open, powered);
                  var saved = NbtUtils.writeBlockState(original);
                  BlockState loaded = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), saved);
                  helper.assertValueEqual(loaded, original, "saved facing/open/powered must load without migration");
                  helper.assertTrue(loaded.getProperties().size() == 6, "only native water/cell/flow extend the existing hatch schema");
                  var legacy = saved.copy();
                  for (String key : java.util.List.of("waterlogged", "fluid_cell", "water_flow")) legacy.getCompound("Properties").remove(key);
                  helper.assertValueEqual(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), legacy), original,
                     "legacy hatch hinge/open/power load dry unchanged");
                  BlockPos pos = helper.absolutePos(CENTER);
                  helper.assertValueEqual(loaded.getCollisionShape(helper.getLevel(), pos).bounds(),
                     original.getCollisionShape(helper.getLevel(), pos).bounds(), "save/load must preserve collision geometry");
                  assertPlayerClimbing(helper, loaded, open, "loaded state " + saved);
                  helper.assertValueEqual(NbtUtils.writeBlockState(helper.getBlockState(CENTER)), saved,
                     "climbing queries must not change serialized state");
               }
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void placed_iron_hatches_continue_ladders_after_floor_ceiling_and_wall_mounts(GameTestHelper helper) {
      for (Direction clickedFace : Direction.values()) {
         for (Direction look : Direction.Plane.HORIZONTAL) {
            clearFixture(helper);
            BlockPos target = helper.absolutePos(CENTER);
            BlockPos support = target.relative(clickedFace.getOpposite());
            helper.getLevel().setBlock(support, Blocks.STONE.defaultBlockState(), 2);
            Player user = helper.makeMockPlayer(GameType.CREATIVE);
            user.setYRot(look.toYRot());
            Vec3 hit = Vec3.atCenterOf(support).add(clickedFace.getStepX() * 0.5,
               clickedFace.getStepY() * 0.5, clickedFace.getStepZ() * 0.5);
            ItemStack stack = new ItemStack(ModBlocks.IRON_HATCH.get());
            BlockPlaceContext context = new BlockPlaceContext(helper.getLevel(), user, InteractionHand.MAIN_HAND,
               stack, new BlockHitResult(hit, clickedFace, support, false));
            helper.assertValueEqual(context.getClickedPos(), target, "placement must target the fixture hatch position");
            helper.assertTrue(((BlockItem)stack.getItem()).place(context).consumesAction(), "actual hatch item placement must succeed");
            helper.useBlock(CENTER, user);
            BlockState placed = helper.getBlockState(CENTER);
            helper.assertTrue(placed.getValue(PortedBlocks.OPEN), "placed hatch must be manually opened");
            Direction hinge = placed.getValue(PortedBlocks.HORIZONTAL_FACING);
            // Install the shaft ladder after placement, replacing the floor support when needed.
            installLadder(helper, ModBlocks.METAL_RUNG_LADDER.get(), hinge.getOpposite());
            assertSameSupportEdge(helper, placed, helper.getBlockState(CENTER.below()));
            assertPlayerClimbing(helper, placed, true, "placed on " + clickedFace + ", player look=" + look);
         }
      }
      helper.succeed();
   }

   private static List<Block> ladders() {
      return List.of(Blocks.LADDER, ModBlocks.METAL_RUNG_LADDER.get(), ModBlocks.METAL_RUNG_STEPS.get());
   }

   private static BlockState hatch(Direction hinge, boolean open, boolean powered) {
      return ModBlocks.IRON_HATCH.get().defaultBlockState()
         .setValue(PortedBlocks.HORIZONTAL_FACING, hinge)
         .setValue(PortedBlocks.OPEN, open)
         .setValue(PortedBlocks.POWERED, powered);
   }

   private static void prepareLadder(GameTestHelper helper, Block ladder, Direction facing) {
      clearFixture(helper);
      installLadder(helper, ladder, facing);
   }

   private static void installLadder(GameTestHelper helper, Block ladder, Direction facing) {
      BlockPos lower = helper.absolutePos(CENTER.below());
      helper.getLevel().setBlock(lower.relative(facing.getOpposite()), Blocks.STONE.defaultBlockState(), 2);
      helper.getLevel().setBlock(lower, ladder.defaultBlockState().setValue(LadderBlock.FACING, facing), 2);
      helper.assertTrue(helper.getLevel().getBlockState(lower).canSurvive(helper.getLevel(), lower),
         "fixture ladder must have a real supporting wall");
   }

   private static void clearFixture(GameTestHelper helper) {
      BlockPos target = helper.absolutePos(CENTER);
      for (BlockPos pos : BlockPos.betweenClosed(target.offset(-1, -2, -1), target.offset(1, 2, 1))) {
         helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
      }
   }

   private static void assertSameSupportEdge(GameTestHelper helper, BlockState hatch, BlockState ladder) {
      BlockPos pos = helper.absolutePos(CENTER);
      AABB leaf = hatch.getShape(helper.getLevel(), pos).bounds();
      AABB rungs = ladder.getShape(helper.getLevel(), pos.below()).bounds();
      helper.assertTrue(sameEdge(leaf.minX, leaf.maxX, rungs.minX, rungs.maxX)
         && sameEdge(leaf.minZ, leaf.maxZ, rungs.minZ, rungs.maxZ),
         "the open leaf and ladder must occupy the same physical support edge");
   }

   private static boolean sameEdge(double min, double max, double otherMin, double otherMax) {
      return (min == 0.0) == (otherMin == 0.0) && (max == 1.0) == (otherMax == 1.0);
   }

   private static void assertPlayerClimbing(GameTestHelper helper, BlockState state, boolean expected, String label) {
      BlockPos pos = helper.absolutePos(CENTER);
      helper.getLevel().setBlock(pos, state, 2);
      // Fresh entities avoid cached in-block state. Feet are above the lower ladder, and the entire
      // horizontal AABB stays in this column, so fullBoundingBoxLadders cannot mask a missing bridge.
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      player.setPos(pos.getX() + 0.5, pos.getY() + 0.05, pos.getZ() + 0.5);
      helper.assertValueEqual(player.blockPosition(), pos, "player feet must be in the hatch block");
      helper.assertTrue(player.getBoundingBox().minY > pos.getY(), "player AABB must not overlap the ladder below");
      helper.assertValueEqual(player.onClimbable(), expected, "actual player climbing: " + label);
      helper.assertValueEqual(state.isLadder(helper.getLevel(), pos, player), expected, "NeoForge block hook: " + label);
      helper.assertValueEqual(helper.getBlockState(CENTER), state, "climbing must leave the hatch state unchanged");
   }
}
