package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PoleHeadGameTests {
   private static Block[] heads() {
      return new Block[] {ModBlocks.THICK_STEEL_POLE_HEAD.get(), ModBlocks.THIN_STEEL_POLE_HEAD.get(),
         ModBlocks.TREATED_WOOD_POLE_HEAD.get()};
   }

   private static Block[] poles() {
      return new Block[] {ModBlocks.THICK_STEEL_POLE.get(), ModBlocks.THIN_STEEL_POLE.get(),
         ModBlocks.TREATED_WOOD_POLE.get()};
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void all_three_heads_place_as_feet_on_solids_and_heads_on_poles(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
      var player = helper.makeMockPlayer(GameType.CREATIVE);
      player.setPos(Vec3.atCenterOf(pos).add(8, 0, 8));
      for (Block head : heads()) {
         for (Block supportBlock : new Block[] {Blocks.STONE, poles()[0], poles()[1], poles()[2],
               heads()[0], heads()[1], heads()[2], ModBlocks.TREATED_WOOD_POLE_SUPPORT.get()}) {
            for (Direction face : Direction.values()) {
               for (float pitch : new float[] {-89, 0, 89}) {
                  for (Direction d : Direction.values()) helper.getLevel().setBlock(pos.relative(d), Blocks.AIR.defaultBlockState(), 2);
                  helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                  BlockPos supportPos = pos.relative(face.getOpposite());
                  BlockState support = supportBlock.defaultBlockState();
                  if (support.hasProperty(PortedBlocks.DirectionalPortBlock.FACING)) {
                     support = support.setValue(PortedBlocks.DirectionalPortBlock.FACING, face);
                  }
                  helper.getLevel().setBlock(supportPos, support, 2);
                  player.setXRot(pitch);
                  var stack = new ItemStack(head);
                  player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                  var hit = new BlockHitResult(Vec3.atCenterOf(supportPos)
                     .add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5)), face, supportPos, false);
                  var context = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit);
                  helper.assertValueEqual(context.getClickedPos(), pos, "actual adjacent placement target");
                  var result = ((BlockItem)head.asItem()).place(context);
                  helper.assertTrue(result.consumesAction(), "head must place against a narrow pole in every direction");
                  BlockState placed = helper.getLevel().getBlockState(pos);
                  helper.assertTrue(placed.is(head), "actual BlockItem placement must succeed");
                  Direction expected = supportBlock == Blocks.STONE ? face : face.getOpposite();
                  helper.assertValueEqual(placed.getValue(PortedBlocks.DirectionalPortBlock.FACING), expected,
                     "clicked support decides head/foot, not camera pitch: " + head + " " + face + " " + pitch);
               }
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void saved_head_directions_survive_neighbors_and_keep_full_structural_shapes(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
      for (Block head : heads()) {
         for (Direction facing : Direction.values()) {
            BlockState state = head.defaultBlockState().setValue(PortedBlocks.DirectionalPortBlock.FACING, facing);
            var saved = NbtUtils.writeBlockState(state);
            BlockState loaded = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), saved);
            helper.assertValueEqual(loaded, state, "legacy facing states round-trip without migration");
            helper.getLevel().setBlock(pos, loaded, 2);
            for (Direction neighbor : Direction.values()) {
               helper.getLevel().setBlock(pos.relative(neighbor), Blocks.STONE.defaultBlockState(), 3);
               helper.getLevel().setBlock(pos.relative(neighbor), Blocks.AIR.defaultBlockState(), 3);
               helper.assertValueEqual(helper.getLevel().getBlockState(pos), loaded,
                  "actual world head state survives adding and removing each neighboring support");
               BlockState updated = loaded.updateShape(neighbor, Blocks.AIR.defaultBlockState(), helper.getLevel(), pos, pos.relative(neighbor));
               helper.assertValueEqual(updated, loaded, "structural pole ends must not disappear or reorient when support changes");
            }
            helper.assertTrue(loaded.canSurvive(helper.getLevel(), pos), "structural ends do not require a full sturdy face");
            var shape = loaded.getShape(helper.getLevel(), pos);
            helper.assertValueEqual((shape.max(facing.getAxis()) - shape.min(facing.getAxis())), 1.0, "selection spans the full pole length");
            helper.assertValueEqual(loaded.getCollisionShape(helper.getLevel(), pos), shape, "collision matches selectable shape");
            Vec3 outward = Vec3.atLowerCornerOf(facing.getNormal());
            Vec3 lateral = facing.getAxis() == Direction.Axis.X ? new Vec3(0, 0, -0.2) : new Vec3(-0.2, 0, 0);
            Vec3 plateProbe = new Vec3(0.5, 0.5, 0.5).add(outward.scale(-0.45)).add(lateral);
            Vec3 shaftSideProbe = new Vec3(0.5, 0.5, 0.5).add(outward.scale(0.45)).add(lateral);
            helper.assertTrue(shape.toAabbs().stream().anyMatch(box -> box.contains(plateProbe)),
               "wide plate must sit opposite facing, matching the model: " + facing);
            helper.assertFalse(shape.toAabbs().stream().anyMatch(box -> box.contains(shaftSideProbe)),
               "opposite end must remain a narrow shaft, not a second plate or full cube: " + facing);
            helper.assertTrue(shape.toAabbs().stream().anyMatch(box -> box.contains(new Vec3(0.5, 0.5, 0.5))),
               "middle of the visible shaft must be selectable");
            helper.assertValueEqual(NbtUtils.writeBlockState(loaded), saved, "shape and neighbor checks preserve saved facing");
         }
      }
      helper.succeed();
   }
}
