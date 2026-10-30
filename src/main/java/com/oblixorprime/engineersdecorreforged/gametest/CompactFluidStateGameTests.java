package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock;
import com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock.FluidContent;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import com.oblixorprime.engineersdecorreforged.block.StairWaterFlow;
import com.oblixorprime.engineersdecorreforged.fluid.NativeFluidCells;
import com.mojang.serialization.JsonOps;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CompactFluidStateGameTests {
   private static final Block[] BLOCKS = {
      ModBlocks.DARK_SHINGLE_ROOF_CHIMNEY.get(),
      ModBlocks.STEEL_CATWALK_STAIRS.get(),
      ModBlocks.RAISED_STEEL_CATWALK_STAIRS.get(),
      ModBlocks.STEEL_DOUBLE_T_SUPPORT.get()
   };
   private static final int[] EXPECTED_STATE_COUNTS = {9728, 6080, 6080, 2736};

   @GameTest(template = "empty", timeoutTicks = 1200)
   public static void compact_fluid_domain_roundtrips_and_has_expected_cardinality(GameTestHelper h) {
      h.assertValueEqual(FluidContent.values().length, 19, "one normalized empty/source/current domain");
      h.assertTrue(NativeFluidloggedBlock.creosote() != Fluids.EMPTY, "IE creosote source is registered");
      for (int i = 0; i < BLOCKS.length; i++) {
         Block block = BLOCKS[i];
         BlockState dry = block.defaultBlockState();
         h.assertTrue(dry.hasProperty(NativeFluidloggedBlock.FLUID_CONTENT), "target uses compact state: " + block);
         h.assertFalse(dry.hasProperty(BlockStateProperties.WATERLOGGED), "no duplicate waterlogged bit: " + block);
         h.assertFalse(dry.hasProperty(NativeFluidloggedBlock.FLUID_CELL), "no duplicate source-cell enum: " + block);
         h.assertFalse(dry.hasProperty(StairWaterFlow.WATER_FLOW), "no duplicate transient-water integer: " + block);
         h.assertValueEqual(block.getStateDefinition().getPossibleStates().size(), EXPECTED_STATE_COUNTS[i],
            "exact new state cardinality: " + block);

         for (FluidContent content : FluidContent.values()) {
            BlockState state = dry.setValue(NativeFluidloggedBlock.FLUID_CONTENT, content);
            FluidState fluid = state.getFluidState();
            h.assertValueEqual(fluid, content.fluidState(), "normalized fluid mapping: " + block + "/" + content);
            h.assertValueEqual(FluidContent.fromFluidState(fluid), content, "fluid mapping is reversible: " + content);
            if (content.isFlowingWater()) {
               h.assertFalse(fluid.isSource(), "flowing-water contents never create sources: " + content);
               h.assertValueEqual(StairWaterFlow.withFlow(dry, fluid), state, "legacy flow encoding normalizes to compact content");
            } else if (content != FluidContent.EMPTY) {
               h.assertTrue(fluid.isSource(), "water, lava, and creosote are native sources: " + content);
            }

            var tag = NbtUtils.writeBlockState(state);
            h.assertValueEqual(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag), state,
               "new palette NBT roundtrip: " + block + "/" + content);
            h.assertValueEqual(BlockState.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow(), state,
               "new disk codec NBT roundtrip: " + block + "/" + content);
            var json = BlockState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
            h.assertValueEqual(BlockState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(), state,
               "new data codec JSON roundtrip: " + block + "/" + content);
         }
      }
      h.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 300)
   public static void additional_compatible_fluids_remain_in_exterior_cells(GameTestHelper h) {
      var level = h.getLevel();
      var pos = h.absolutePos(new BlockPos(3, 2, 3));
      for (String path : List.of("ethanol", "biodiesel", "plantoil")) {
         Fluid fluid = BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("immersiveengineering", path));
         h.assertTrue(fluid != Fluids.EMPTY && fluid instanceof FlowingFluid,
            "existing compatible IE fluid is registered: " + path);
         FlowingFluid flowing = (FlowingFluid)fluid;
         FluidState source = flowing.getSource(false);
         h.assertTrue(NativeFluidCells.standard(fluid), "existing exterior-cell contract accepts " + path);
         h.assertValueEqual(FluidContent.fromFluidState(source), FluidContent.EMPTY,
            "unbounded fluid IDs are not squeezed into the compact enum: " + path);
         for (Block block : BLOCKS) {
            NativeFluidCells.set(level, pos, Fluids.EMPTY.defaultFluidState());
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
            BlockState before = level.getBlockState(pos);
            h.assertTrue(((NativeFluidloggedBlock)block).placeLiquid(level, pos, before, source),
               "compatible fluid stores through the existing exterior cell: " + block + "/" + path);
            BlockState after = level.getBlockState(pos);
            h.assertValueEqual(NativeFluidloggedBlock.fluidContent(after), FluidContent.EMPTY,
               "external fluid does not mutate compact blockstate contents: " + path);
            h.assertValueEqual(level.getFluidState(pos).getType(), fluid,
               "exterior cell exposes exact compatible fluid: " + path);
         }
      }
      h.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 1200)
   public static void compact_fluid_values_do_not_change_rotation_or_shapes(GameTestHelper h) {
      var level = h.getLevel();
      var pos = h.absolutePos(new BlockPos(3, 2, 3));
      var context = net.minecraft.world.phys.shapes.CollisionContext.empty();
      for (Block block : BLOCKS) {
         for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            BlockState dry = state.setValue(NativeFluidloggedBlock.FLUID_CONTENT, FluidContent.EMPTY);
            var shape = state.getShape(level, pos, context);
            var dryShape = dry.getShape(level, pos, context);
            var collision = state.getCollisionShape(level, pos, context);
            var dryCollision = dry.getCollisionShape(level, pos, context);
            h.assertValueEqual(shape.toAabbs(), dryShape.toAabbs(), "fluid content preserves outline/rotation: " + state);
            h.assertValueEqual(collision.toAabbs(), dryCollision.toAabbs(), "fluid content preserves collision/rotation: " + state);
         }
      }
      h.succeed();
   }
}
