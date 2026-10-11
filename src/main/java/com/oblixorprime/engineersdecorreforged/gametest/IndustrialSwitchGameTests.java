package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.rsgauges.ControlsBlockTypes;
import com.oblixorprime.engineersdecorreforged.rsgauges.IndustrialSwitchBlock;
import com.oblixorprime.engineersdecorreforged.rsgauges.IndustrialSwitchBlock.Part;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class IndustrialSwitchGameTests {
   private static final BlockPos CENTER = new BlockPos(3, 2, 3);
   private IndustrialSwitchGameTests() { }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void industrial_switch_floor_and_wall_placement_faces_outward(GameTestHelper helper) {
      for (Direction look : Direction.Plane.HORIZONTAL) {
         place(helper, Direction.UP, look);
         assertPair(helper, look.getOpposite());
         clear(helper);
         for (Direction face : Direction.Plane.HORIZONTAL) {
            place(helper, face, look);
            assertPair(helper, face);
            clear(helper);
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void industrial_switch_both_halves_collide_select_and_toggle(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         place(helper, Direction.UP, facing.getOpposite());
         BlockPos lower = helper.absolutePos(CENTER);
         for (int half = 0; half < 2; half++) {
            BlockPos pos = lower.above(half);
            BlockState state = helper.getLevel().getBlockState(pos);
            AABB box = expectedBounds(facing);
            helper.assertValueEqual(state.getShape(helper.getLevel(), pos).bounds(), box, "outline must match cabinet half");
            helper.assertValueEqual(state.getCollisionShape(helper.getLevel(), pos).bounds(), box, "collision must match cabinet half");
            Vec3 middle = Vec3.atCenterOf(pos);
            Vec3 front = middle.add(facing.getStepX() * 2, 0, facing.getStepZ() * 2);
            BlockHitResult hit = helper.getLevel().clip(new ClipContext(front, middle,
               ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            helper.assertValueEqual(hit.getBlockPos(), pos, "normal raycast must select each cabinet half");
            helper.useBlock(CENTER.above(half), player);
            for (int check = 0; check < 2; check++) {
               BlockPos checkPos = lower.above(check);
               BlockState toggled = helper.getLevel().getBlockState(checkPos);
               helper.assertValueEqual(toggled.getValue(ControlsBlockTypes.POWERED), half == 0, "both halves must toggle together");
               for (Direction side : Direction.values()) {
                  helper.assertValueEqual(toggled.getSignal(helper.getLevel(), checkPos, side), half == 0 ? 15 : 0, "weak signal");
                  helper.assertValueEqual(toggled.getDirectSignal(helper.getLevel(), checkPos, side), half == 0 ? 15 : 0, "strong signal");
               }
            }
         }
         clear(helper);
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void industrial_switch_rejects_blocked_headroom_without_overwrite(GameTestHelper helper) {
      BlockPos target = helper.absolutePos(CENTER);
      helper.getLevel().setBlock(target.above(), Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      BlockPlaceContext context = context(helper, Direction.UP, Direction.NORTH, player);
      ItemStack stack = context.getItemInHand();
      helper.assertTrue(!((BlockItem)stack.getItem()).place(context).consumesAction(), "blocked placement must fail");
      helper.assertTrue(helper.getLevel().getBlockState(target).isAir(), "failed placement must leave lower empty");
      helper.assertTrue(helper.getLevel().getBlockState(target.above()).is(Blocks.DIAMOND_BLOCK), "headroom obstruction must survive");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void industrial_switch_breaking_either_half_cleans_up_and_drops_once(GameTestHelper helper) {
      for (boolean creative : new boolean[]{false, true}) {
         for (int half = 0; half < 2; half++) {
            clear(helper);
            place(helper, Direction.UP, Direction.SOUTH);
            BlockPos lower = helper.absolutePos(CENTER);
            BlockPos target = lower.above(half);
            BlockState lowerState = helper.getLevel().getBlockState(lower);
            BlockState upperState = helper.getLevel().getBlockState(lower.above());
            helper.assertValueEqual(Block.getDrops(lowerState, helper.getLevel(), lower, null).stream().mapToInt(ItemStack::getCount).sum(), 1, "lower owns one drop");
            helper.assertTrue(Block.getDrops(upperState, helper.getLevel(), lower.above(), null).isEmpty(), "upper owns no duplicate drop");
            Player player = helper.makeMockPlayer(creative ? GameType.CREATIVE : GameType.SURVIVAL);
            BlockState before = helper.getLevel().getBlockState(target);
            before.getBlock().playerWillDestroy(helper.getLevel(), target, before, player);
            helper.getLevel().destroyBlock(target, !creative);
            helper.assertTrue(helper.getLevel().getBlockState(lower).isAir(), "lower must be removed");
            helper.assertTrue(helper.getLevel().getBlockState(lower.above()).isAir(), "upper must be removed");
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void industrial_switch_legacy_saved_states_remain_non_destructive(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      for (Direction facing : Direction.values()) {
         for (boolean powered : new boolean[]{false, true}) {
            BlockState original = cabinet().defaultBlockState().setValue(ControlsBlockTypes.FACING, facing)
               .setValue(ControlsBlockTypes.POWERED, powered);
            var tag = NbtUtils.writeBlockState(original);
            tag.getCompound("Properties").remove("part");
            BlockState loaded = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag);
            helper.assertValueEqual(BlockState.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow(), loaded, "codec must preserve old saves");
            helper.assertValueEqual(loaded.getValue(IndustrialSwitchBlock.PART), Part.LEGACY, "missing saved part must load as legacy");
            helper.getLevel().setBlock(pos, loaded, 3);
            helper.getLevel().setBlock(pos.above(), Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
            helper.getLevel().setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 3);
            helper.assertValueEqual(helper.getLevel().getBlockState(pos), loaded, "legacy must survive without conversion or deletion");
            helper.assertTrue(helper.getLevel().getBlockState(pos.above()).is(Blocks.DIAMOND_BLOCK), "legacy must not overwrite its neighbor");
            helper.assertValueEqual(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), NbtUtils.writeBlockState(loaded)), loaded, "legacy round trip");
            helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void industrial_switch_rotation_and_pair_serialization(GameTestHelper helper) {
      place(helper, Direction.UP, Direction.SOUTH);
      BlockPos pos = helper.absolutePos(CENTER);
      BlockState lower = helper.getLevel().getBlockState(pos);
      helper.getLevel().setBlock(pos, lower.rotate(Rotation.CLOCKWISE_90), 3);
      assertPair(helper, Direction.EAST);
      for (int half = 0; half < 2; half++) {
         BlockState state = helper.getLevel().getBlockState(pos.above(half));
         helper.assertValueEqual(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), NbtUtils.writeBlockState(state)), state, "pair state round trip");
      }
      helper.succeed();
   }

   private static Block cabinet() {
      return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(EngineersDecorReforged.MOD_ID, "industrialswitch"));
   }
   private static void place(GameTestHelper helper, Direction face, Direction look) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      BlockPlaceContext context = context(helper, face, look, player);
      helper.assertTrue(((BlockItem)context.getItemInHand().getItem()).place(context).consumesAction(), "cabinet placement must succeed");
   }
   private static BlockPlaceContext context(GameTestHelper helper, Direction face, Direction look, Player player) {
      player.setYRot(look.toYRot());
      BlockPos support = helper.absolutePos(CENTER).relative(face.getOpposite());
      helper.getLevel().setBlock(support, Blocks.STONE.defaultBlockState(), 3);
      Vec3 hit = Vec3.atCenterOf(support).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
      return new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, new ItemStack(cabinet()),
         new BlockHitResult(hit, face, support, false));
   }
   private static void assertPair(GameTestHelper helper, Direction facing) {
      BlockPos pos = helper.absolutePos(CENTER);
      for (int half = 0; half < 2; half++) {
         BlockState state = helper.getLevel().getBlockState(pos.above(half));
         helper.assertTrue(state.is(cabinet()), "cabinet half must exist");
         helper.assertValueEqual(state.getValue(ControlsBlockTypes.FACING), facing, "indicator side must face outward");
         helper.assertValueEqual(state.getValue(IndustrialSwitchBlock.PART), half == 0 ? Part.LOWER : Part.UPPER, "paired part");
      }
   }
   private static AABB expectedBounds(Direction facing) {
      return switch (facing) {
         case NORTH -> new AABB(0, 0, 7.5 / 16, 1, 1, 1);
         case EAST -> new AABB(0, 0, 0, 8.5 / 16, 1, 1);
         case SOUTH -> new AABB(0, 0, 0, 1, 1, 8.5 / 16);
         default -> new AABB(7.5 / 16, 0, 0, 1, 1, 1);
      };
   }
   private static void clear(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
      helper.getLevel().setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
      for (Direction direction : Direction.values()) {
         helper.getLevel().setBlock(pos.relative(direction), Blocks.AIR.defaultBlockState(), 3);
      }
   }
}
