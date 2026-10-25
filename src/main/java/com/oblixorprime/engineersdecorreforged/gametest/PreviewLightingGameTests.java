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
   // These API fixtures do not load Building Gadgets or reproduce its renderer.
   /** Inherits NeoForge's actual default overloads; a Proxy would itself override both methods. */
   private static class ChunklessView implements LevelAccessor {
      private AssertionError unused() { return new AssertionError("Preview must not read/write world data or load chunks"); }
      public net.minecraft.world.level.chunk.ChunkSource getChunkSource() { return null; }
      public long nextSubTickCount() { throw unused(); }
      public net.minecraft.world.ticks.LevelTickAccess<Block> getBlockTicks() { throw unused(); }
      public net.minecraft.world.ticks.LevelTickAccess<net.minecraft.world.level.material.Fluid> getFluidTicks() { throw unused(); }
      public net.minecraft.world.level.storage.LevelData getLevelData() { throw unused(); }
      public net.minecraft.world.DifficultyInstance getCurrentDifficultyAt(BlockPos pos) { throw unused(); }
      public net.minecraft.server.MinecraftServer getServer() { throw unused(); }
      public net.minecraft.util.RandomSource getRandom() { throw unused(); }
      public void playSound(net.minecraft.world.entity.player.Player player, BlockPos pos, net.minecraft.sounds.SoundEvent event, net.minecraft.sounds.SoundSource source, float volume, float pitch) { throw unused(); }
      public void addParticle(net.minecraft.core.particles.ParticleOptions particle, double x, double y, double z, double dx, double dy, double dz) { throw unused(); }
      public void levelEvent(net.minecraft.world.entity.player.Player player, int id, BlockPos pos, int data) { throw unused(); }
      public void gameEvent(net.minecraft.core.Holder<net.minecraft.world.level.gameevent.GameEvent> event, net.minecraft.world.phys.Vec3 pos, net.minecraft.world.level.gameevent.GameEvent.Context context) { throw unused(); }
      public net.minecraft.world.level.chunk.ChunkAccess getChunk(int x, int z, net.minecraft.world.level.chunk.status.ChunkStatus status, boolean load) { throw unused(); }
      public int getHeight(net.minecraft.world.level.levelgen.Heightmap.Types type, int x, int z) { throw unused(); }
      public int getSkyDarken() { throw unused(); }
      public net.minecraft.world.level.biome.BiomeManager getBiomeManager() { throw unused(); }
      public net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> getUncachedNoiseBiome(int x, int y, int z) { throw unused(); }
      public boolean isClientSide() { throw unused(); }
      public int getSeaLevel() { throw unused(); }
      public net.minecraft.world.level.dimension.DimensionType dimensionType() { throw unused(); }
      public net.minecraft.core.RegistryAccess registryAccess() { throw unused(); }
      public net.minecraft.world.flag.FeatureFlagSet enabledFeatures() { throw unused(); }
      public java.util.List<net.minecraft.world.entity.Entity> getEntities(net.minecraft.world.entity.Entity except, net.minecraft.world.phys.AABB box, java.util.function.Predicate<? super net.minecraft.world.entity.Entity> filter) { throw unused(); }
      public <T extends net.minecraft.world.entity.Entity> java.util.List<T> getEntities(net.minecraft.world.level.entity.EntityTypeTest<net.minecraft.world.entity.Entity, T> type, net.minecraft.world.phys.AABB box, java.util.function.Predicate<? super T> filter) { throw unused(); }
      public java.util.List<? extends net.minecraft.world.entity.player.Player> players() { throw unused(); }
      public boolean isStateAtPosition(BlockPos pos, java.util.function.Predicate<net.minecraft.world.level.block.state.BlockState> filter) { throw unused(); }
      public boolean isFluidAtPosition(BlockPos pos, java.util.function.Predicate<net.minecraft.world.level.material.FluidState> filter) { throw unused(); }
      public boolean setBlock(BlockPos pos, net.minecraft.world.level.block.state.BlockState state, int flags, int recursion) { throw unused(); }
      public boolean removeBlock(BlockPos pos, boolean moving) { throw unused(); }
      public boolean destroyBlock(BlockPos pos, boolean drops, net.minecraft.world.entity.Entity source, int recursion) { throw unused(); }
      public net.minecraft.world.level.block.entity.BlockEntity getBlockEntity(BlockPos pos) { throw unused(); }
      public net.minecraft.world.level.block.state.BlockState getBlockState(BlockPos pos) { throw unused(); }
      public net.minecraft.world.level.material.FluidState getFluidState(BlockPos pos) { throw unused(); }
      public float getShade(net.minecraft.core.Direction side, boolean shade) { throw unused(); }
      public net.minecraft.world.level.lighting.LevelLightEngine getLightEngine() { throw unused(); }
      public net.minecraft.world.level.border.WorldBorder getWorldBorder() { throw unused(); }
   }

   private static AuxiliaryLightManager light(int light) {
      return new AuxiliaryLightManager() {
         public void setLightAt(BlockPos pos, int value) { throw new AssertionError("read-only preview"); }
         public int getLightAt(BlockPos pos) { return light; }
      };
   }

   private static BlockGetter view(boolean accessor, Integer auxiliaryLight, RuntimeException auxiliaryFailure) {
      if (accessor && auxiliaryLight == null && auxiliaryFailure == null) return new ChunklessView();
      Class<?> type = accessor ? LevelAccessor.class : BlockGetter.class;
      return (BlockGetter) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
         (Object proxy, Method method, Object[] args) -> {
            String name = method.getName();
            if (name.equals("getChunkSource")) return null;
            if (name.equals("getBlockState") || name.equals("getFluidState") || name.equals("getChunk"))
               throw new AssertionError("Lighting must not read world data or load chunks: " + name);
            if (name.equals("getAuxLightManager")) {
               if (auxiliaryFailure != null) throw auxiliaryFailure;
               if (auxiliaryLight != null) return light(auxiliaryLight);
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
      h.assertValueEqual(dry.getLightEmission(view(true, 15, null), BlockPos.ZERO), 15, "chunkless LevelAccessor retains its custom auxiliary emission");
      h.assertValueEqual(dry.getLightEmission(view(false, null, null), BlockPos.ZERO), 0, "plain BlockGetter without auxiliary support");
      h.assertValueEqual(dry.getLightEmission(view(false, 15, null), BlockPos.ZERO), 15, "supported auxiliary light is retained without world reads");
      var lava = dry.setValue(NativeFluidloggedBlock.FLUID_CELL, NativeFluidloggedBlock.Cell.LAVA);
      h.assertValueEqual(lava.getLightEmission(view(false, 0, null), BlockPos.ZERO), 15, "zero auxiliary light cannot dim intrinsic lava light");
      var expected = new NullPointerException("unrelated auxiliary failure");
      RuntimeException actual = null;
      try { dry.getLightEmission(view(false, null, expected), BlockPos.ZERO); }
      catch (RuntimeException failure) { actual = failure; }
      h.assertTrue(actual == expected, "unrelated exceptions must propagate unchanged");
      var chunkPosOverride = new ChunklessView() {
         @Override
         public AuxiliaryLightManager getAuxLightManager(net.minecraft.world.level.ChunkPos pos) { return light(15); }
      };
      h.assertValueEqual(dry.getLightEmission(chunkPosOverride, BlockPos.ZERO), 15, "documented ChunkPos override remains available without a chunk source");
      h.assertValueEqual(dry.getLightEmission(view(true, 0, null), BlockPos.ZERO), 0, "chunkless custom zero emission remains zero");
      actual = null;
      try { dry.getLightEmission(view(true, null, expected), BlockPos.ZERO); }
      catch (RuntimeException thrown) { actual = thrown; }
      h.assertTrue(actual == expected, "chunkless custom lookup exceptions propagate unchanged");
      var failingManager = new ChunklessView() {
         @Override
         public AuxiliaryLightManager getAuxLightManager(net.minecraft.world.level.ChunkPos pos) {
            return new AuxiliaryLightManager() {
               public void setLightAt(BlockPos blockPos, int value) { throw new AssertionError("read-only preview"); }
               public int getLightAt(BlockPos blockPos) { throw expected; }
            };
         }
      };
      actual = null;
      try { dry.getLightEmission(failingManager, BlockPos.ZERO); }
      catch (RuntimeException thrown) { actual = thrown; }
      h.assertTrue(actual == expected, "custom manager read exceptions propagate unchanged");
      System.out.println("IEDCTR_NATIVE_PREVIEW_AUXILIARY_PASS chunkless_custom=15 chunkpos_override=15 unrelated_failure_preserved=true");
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
