package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.fluid.ExternalFluidAdapter;
import com.oblixorprime.engineersdecorreforged.fluid.NativeFluidCells;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
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

/** Real NeoForge default methods; models the null ChunkSource contract, not the BG2 renderer. */
@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ExternalPreviewLightingGameTests {
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

   private static BlockGetter view(boolean accessor, Integer light, RuntimeException failure) {
      if (accessor && light == null && failure == null) return new ChunklessView();
      Class<?> type = accessor ? LevelAccessor.class : BlockGetter.class;
      return (BlockGetter) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
         (Object proxy, Method method, Object[] args) -> {
            String name = method.getName();
            if (name.equals("getChunkSource")) return null;
            if (name.equals("getBlockState") || name.equals("getFluidState") || name.equals("getChunk"))
               throw new AssertionError("Preview lighting must not read world data or load chunks: " + name);
            if (name.equals("getAuxLightManager")) {
               if (failure != null) throw failure;
               if (light != null) return light(light);
            }
            if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args == null ? new Object[0] : args);
            throw new AssertionError("Unexpected preview access: " + method);
         });
   }

   @GameTest(template="empty")
   public static void external_chunkless_preview_preserves_native_light(GameTestHelper h) {
      var preview = view(true, null, null);
      var pos = new BlockPos(3, 257, 3);
      for (var block : List.of(Blocks.OAK_STAIRS, Blocks.STONE_SLAB, Blocks.OAK_FENCE, Blocks.IRON_BARS, Blocks.OAK_TRAPDOOR)) {
         var dry = block.defaultBlockState();
         h.assertTrue(ExternalFluidAdapter.supported(dry), "fixture is an eligible external block: " + block);
         h.assertValueEqual(dry.getLightEmission(preview, pos), 0, "dry external preview must not throw: " + block);
         h.assertValueEqual(dry.setValue(BlockStateProperties.WATERLOGGED, true).getLightEmission(preview, pos), 0,
            "waterlogged external preview retains intrinsic light: " + block);
      }
      h.assertValueEqual(Blocks.GLOWSTONE.defaultBlockState().getLightEmission(preview, pos), 15, "unadapted emitting block retains intrinsic light");
      System.out.println("IEDCTR_EXTERNAL_PREVIEW_CHUNKLESS_PASS blocks=5 waterlogged=true");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void external_preview_keeps_auxiliary_light_and_unrelated_failures(GameTestHelper h) {
      var dry = Blocks.OAK_STAIRS.defaultBlockState();
      h.assertValueEqual(dry.getLightEmission(view(true, 15, null), BlockPos.ZERO), 15, "chunkless LevelAccessor retains its custom auxiliary emission");
      h.assertValueEqual(dry.getLightEmission(view(false, null, null), BlockPos.ZERO), 0, "plain BlockGetter without auxiliary support");
      h.assertValueEqual(dry.getLightEmission(view(false, 15, null), BlockPos.ZERO), 15, "supported auxiliary emission remains available");
      h.assertValueEqual(dry.getLightEmission(view(false, 0, null), BlockPos.ZERO), 0, "zero auxiliary emission remains zero");
      var expected = new NullPointerException("unrelated external auxiliary failure");
      RuntimeException actual = null;
      try { dry.getLightEmission(view(false, null, expected), BlockPos.ZERO); }
      catch (RuntimeException thrown) { actual = thrown; }
      h.assertTrue(actual == expected, "unrelated exceptions propagate unchanged");
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
      System.out.println("IEDCTR_EXTERNAL_PREVIEW_AUXILIARY_PASS chunkless_custom=15 chunkpos_override=15 unrelated_failure_preserved=true");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void external_world_light_preserves_sources_flow_and_drain(GameTestHelper h) {
      var level = h.getLevel();
      var pos = h.absolutePos(new BlockPos(3, 2, 3));
      for (var fluid : List.of(Fluids.WATER.getSource(false), Fluids.LAVA.getSource(false), Fluids.FLOWING_LAVA.getFlowing(4, false))) {
         level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
         var dry = Blocks.OAK_STAIRS.defaultBlockState();
         level.setBlock(pos, dry, Block.UPDATE_ALL);
         h.assertTrue(ExternalFluidAdapter.place(level, pos, dry, fluid), "eligible external fluid placement");
         h.assertValueEqual(level.getBlockState(pos).getLightEmission(level, pos), fluid.getType().isSame(Fluids.LAVA) ? 15 : 0,
            "world emission preserved for exact source/current");
         if (fluid.getType().isSame(Fluids.WATER)) ExternalFluidAdapter.pickup(level, pos, level.getBlockState(pos));
         else NativeFluidCells.set(level, pos, Fluids.EMPTY.defaultFluidState());
         h.assertValueEqual(level.getBlockState(pos).getLightEmission(level, pos), 0, "drain clears external auxiliary light");
         h.assertTrue(level.getBlockState(pos).is(Blocks.OAK_STAIRS), "lighting/drain preserves container");
      }
      System.out.println("IEDCTR_EXTERNAL_PREVIEW_WORLD_PASS cases=3");
      h.succeed();
   }
}
