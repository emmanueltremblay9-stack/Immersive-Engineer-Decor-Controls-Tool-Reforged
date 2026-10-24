package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock;
import com.oblixorprime.engineersdecorreforged.fluid.NativeFluidCells;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PreviewLightingGameTests {
   /** Exercises real NeoForge defaults with BG2's relevant null-ChunkSource contract.
    * This fixture does not load Building Gadgets or reproduce its renderer.
    */
   private static BlockGetter view(boolean accessor, Integer auxiliaryLight, RuntimeException auxiliaryFailure) {
      Class<?> type = accessor ? LevelAccessor.class : BlockGetter.class;
      return (BlockGetter) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
         (Object proxy, Method method, Object[] args) -> {
            String name = method.getName();
            if (name.equals("getChunkSource")) return null;
            if (name.equals("getBlockState") || name.equals("getFluidState") || name.equals("getChunk"))
               throw new AssertionError("Lighting must not read world data or load chunks: " + name);
            if (name.equals("getAuxLightManager")) {
               if (auxiliaryFailure != null) throw auxiliaryFailure;
               if (auxiliaryLight != null) return new AuxiliaryLightManager() {
                  public void setLightAt(BlockPos pos, int value) { throw new AssertionError("read-only view"); }
                  public int getLightAt(BlockPos pos) { return auxiliaryLight; }
               };
            }
            if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args == null ? new Object[0] : args);
            throw new AssertionError("Unexpected preview access: " + method);
         });
   }

   @GameTest(template="empty")
   public static void chunkless_preview_preserves_intrinsic_light(GameTestHelper h) {
      var preview = view(true, null, null);
      var pos = new BlockPos(3, 257, 3);
      var dry = ModBlocks.CLINKER_BRICK_BLOCK.get().defaultBlockState();
      h.assertTrue(dry.getFluidState().isEmpty(), "Clinker Brick control is dry");
      h.assertValueEqual(Blocks.GRASS_BLOCK.defaultBlockState().getLightEmission(preview, pos), 0, "vanilla grass preview control");
      h.assertValueEqual(dry.getLightEmission(preview, pos), 0, "dry Clinker Brick preview must not throw");
      h.assertValueEqual(dry.setValue(BlockStateProperties.WATERLOGGED, true).getLightEmission(preview, pos), 0, "water does not add intrinsic light");
      h.assertValueEqual(dry.setValue(NativeFluidloggedBlock.FLUID_CELL, NativeFluidloggedBlock.Cell.LAVA).getLightEmission(preview, pos), 15, "preview retains intrinsic lava light");
      h.assertValueEqual(ModBlocks.IRON_INSET_LIGHT.get().defaultBlockState().getLightEmission(preview, pos), 14, "preview retains lamp light");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void preview_light_keeps_auxiliary_reads_and_unrelated_failures(GameTestHelper h) {
      var dry = ModBlocks.CLINKER_BRICK_BLOCK.get().defaultBlockState();
      h.assertValueEqual(dry.getLightEmission(view(false, null, null), BlockPos.ZERO), 0, "plain BlockGetter without auxiliary support");
      h.assertValueEqual(dry.getLightEmission(view(false, 15, null), BlockPos.ZERO), 15, "supported auxiliary light is retained without world reads");
      var lava = dry.setValue(NativeFluidloggedBlock.FLUID_CELL, NativeFluidloggedBlock.Cell.LAVA);
      h.assertValueEqual(lava.getLightEmission(view(false, 0, null), BlockPos.ZERO), 15, "zero auxiliary light cannot dim intrinsic lava light");
      var expected = new NullPointerException("unrelated auxiliary failure");
      RuntimeException actual = null;
      try { dry.getLightEmission(view(false, null, expected), BlockPos.ZERO); }
      catch (RuntimeException failure) { actual = failure; }
      h.assertTrue(actual == expected, "unrelated exceptions must propagate unchanged");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void normal_world_fluid_light_survives_preview_guard(GameTestHelper h) {
      var level = h.getLevel();
      var pos = h.absolutePos(new BlockPos(3, 2, 3));
      var dry = ModBlocks.CLINKER_BRICK_BLOCK.get().defaultBlockState();
      level.setBlock(pos, dry, Block.UPDATE_ALL);
      h.assertValueEqual(level.getBlockState(pos).getLightEmission(level, pos), 0, "normal dry world");
      level.setBlock(pos, dry.setValue(BlockStateProperties.WATERLOGGED, true), Block.UPDATE_ALL);
      h.assertValueEqual(level.getBlockState(pos).getLightEmission(level, pos), 0, "normal water source");
      level.setBlock(pos, dry.setValue(NativeFluidloggedBlock.FLUID_CELL, NativeFluidloggedBlock.Cell.LAVA), Block.UPDATE_ALL);
      h.assertValueEqual(level.getBlockState(pos).getLightEmission(level, pos), 15, "normal intrinsic lava source");
      level.setBlock(pos, ModBlocks.STEEL_CATWALK.get().defaultBlockState(), Block.UPDATE_ALL);
      h.assertTrue(NativeFluidCells.place(level, pos, level.getBlockState(pos), Fluids.FLOWING_LAVA.getFlowing(4, false)), "flowing lava accepted by the catwalk");
      h.assertValueEqual(level.getBlockState(pos).getLightEmission(level, pos), 15, "normal generic flowing lava auxiliary light");
      NativeFluidCells.set(level, pos, Fluids.EMPTY.defaultFluidState());
      h.assertValueEqual(level.getBlockState(pos).getLightEmission(level, pos), 0, "drain clears auxiliary light");
      h.succeed();
   }
}
