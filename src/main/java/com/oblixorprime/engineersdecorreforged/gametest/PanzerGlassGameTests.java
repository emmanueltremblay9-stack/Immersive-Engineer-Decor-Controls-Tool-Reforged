package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PanzerGlassGameTests {
   private PanzerGlassGameTests() {}

   private static List<BlockState> cells() {
      var dry = ModBlocks.PANZERGLASS_BLOCK.get().defaultBlockState();
      return List.of(dry,
         NativeFluidloggedBlock.placement(dry, Fluids.WATER.getSource(false)),
         NativeFluidloggedBlock.placement(dry, Fluids.LAVA.getSource(false)),
         NativeFluidloggedBlock.placement(dry, ((FlowingFluid)NativeFluidloggedBlock.creosote()).getSource(false)));
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void panzer_glass_hides_only_shared_faces_in_all_directions_and_cells(GameTestHelper h) {
      var level = h.getLevel();
      var pos = h.absolutePos(new BlockPos(2, 2, 2));
      for (var state : cells()) {
         level.setBlock(pos, state, Block.UPDATE_ALL);
         for (var neighbor : cells()) {
            for (var face : Direction.values()) {
               var other = pos.relative(face);
               level.setBlock(other, neighbor, Block.UPDATE_ALL);
               h.assertFalse(Block.shouldRenderFace(state, level, pos, face, other),
                  "no internal glass face for " + face + " / " + state + " / " + neighbor);
               h.assertFalse(Block.shouldRenderFace(neighbor, level, other, face.getOpposite(), pos),
                  "the opposite shared face is also hidden");
               h.assertValueEqual(level.getFluidState(pos), state.getFluidState(), "render decisions preserve the first fluid");
               h.assertValueEqual(level.getFluidState(other), neighbor.getFluidState(), "render decisions preserve the second fluid");
               level.setBlock(other, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
               h.assertTrue(Block.shouldRenderFace(state, level, pos, face, other), "exterior face returns after neighbor removal");
            }
         }
      }
      h.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void panzer_glass_does_not_join_other_glass_or_change_other_blocks(GameTestHelper h) {
      var level = h.getLevel();
      var pos = h.absolutePos(new BlockPos(2, 2, 2));
      var glass = ModBlocks.PANZERGLASS_BLOCK.get().defaultBlockState();
      for (var other : List.of(Blocks.GLASS.defaultBlockState(), Blocks.TINTED_GLASS.defaultBlockState(),
            ModBlocks.PANZERGLASS_SLAB.get().defaultBlockState(), Blocks.AIR.defaultBlockState())) {
         level.setBlock(pos, glass, Block.UPDATE_ALL);
         for (var face : Direction.values()) {
            var neighbor = pos.relative(face);
            level.setBlock(neighbor, other, Block.UPDATE_ALL);
            h.assertFalse(glass.skipRendering(other, face), "only this exact full block may suppress shared glass faces");
            h.assertTrue(Block.shouldRenderFace(glass, level, pos, face, neighbor), "unrelated transparent neighbor retains exterior face");
            level.setBlock(neighbor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
         }
      }
      var brick = ModBlocks.CLINKER_BRICK_BLOCK.get().defaultBlockState();
      h.assertFalse(brick.skipRendering(brick, Direction.NORTH), "other native solid registrations retain their existing behavior");
      h.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void panzer_glass_retains_collision_drops_fluids_and_light(GameTestHelper h) {
      var level = h.getLevel();
      var pos = h.absolutePos(new BlockPos(2, 2, 2));
      var block = ModBlocks.PANZERGLASS_BLOCK.get();
      var dry = block.defaultBlockState();
      h.assertValueEqual(block.getStateDefinition().getPossibleStates().size(), 6, "no connection properties are added to saved block states");
      h.assertValueEqual(block.getExplosionResistance(), 2000F, "blast resistance is unchanged");
      for (var state : cells()) {
         level.setBlock(pos, state, Block.UPDATE_ALL);
         h.assertFalse(Shapes.joinIsNotEmpty(state.getCollisionShape(level, pos), Shapes.block(), BooleanOp.NOT_SAME), "full-cube collision is retained");
         h.assertFalse(Shapes.joinIsNotEmpty(state.getShape(level, pos), Shapes.block(), BooleanOp.NOT_SAME), "full-cube outline is retained");
         h.assertFalse(state.canOcclude(), "glass keeps its original no-occlusion lighting policy");
         h.assertValueEqual(state.getDestroySpeed(level, pos), .5F, "mining hardness is unchanged");
         h.assertValueEqual(state.getLightEmission(level, pos), state.getFluidState().is(Fluids.LAVA) ? 15 : 0,
            "stored lava light and non-luminous fluids retain existing emission");
         var drops = Block.getDrops(state, level, pos, null, null, new ItemStack(Items.IRON_PICKAXE));
         h.assertValueEqual(drops.size(), 1, "exactly one item stack drops");
         h.assertTrue(drops.getFirst().is(block.asItem()) && drops.getFirst().getCount() == 1, "one original Panzer Glass item drops");
      }
      for (Fluid fluid : List.of(Fluids.WATER, Fluids.LAVA, NativeFluidloggedBlock.creosote())) {
         level.setBlock(pos, dry, Block.UPDATE_ALL);
         var container = (NativeFluidloggedBlock)block;
         h.assertTrue(container.placeLiquid(level, pos, dry, ((FlowingFluid)fluid).getSource(false)), "native source insertion succeeds");
         var filled = level.getBlockState(pos);
         h.assertTrue(level.getFluidState(pos).isSource() && level.getFluidState(pos).is(fluid), "source identity survives insertion");
         h.assertTrue(container.pickupBlock(null, level, pos, filled).is(fluid.getBucket()), "draining returns the existing source bucket");
         h.assertTrue(level.getFluidState(pos).isEmpty() && level.getBlockState(pos).is(block), "draining preserves the glass block");
      }
      h.succeed();
   }
}
