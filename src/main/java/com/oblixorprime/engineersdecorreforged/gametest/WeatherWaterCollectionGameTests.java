package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlocks;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WeatherWaterCollectionGameTests {
   private WeatherWaterCollectionGameTests() {}

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void weather_collectors_match_rain_and_thunder_bucket_times(GameTestHelper h) {
      try (var f = new WeatherFixture(h)) {
         for (boolean funnel : new boolean[] {true, false}) {
            for (boolean thunder : new boolean[] {false, true}) {
               f.reset();
               f.weather(true, thunder);
               var machine = f.place(funnel, f.pos);
               h.assertTrue(f.level.isRainingAt(f.pos.above()), "native rainfall reaches the open collector");
               h.assertValueEqual(f.level.isThundering(), thunder, "native thunder state matches fixture");
               // User contract, in seconds per 1000 mB at 20 TPS; no production rate helper is reused.
               int bucketSeconds = funnel ? (thunder ? 40 : 80) : (thunder ? 20 : 40);
               int bucketTicks = bucketSeconds * 20;
               int quantum = 1000 / (bucketTicks / 80);
               int loadedChunks = f.level.getChunkSource().getLoadedChunksCount();
               tick(f, machine, 79);
               h.assertValueEqual(amount(machine), 0, "weather collection waits for the full 80-tick interval");
               tick(f, machine, 1);
               h.assertValueEqual(amount(machine), quantum, "first weather interval has the exact selected rate");
               tick(f, machine, bucketTicks - 80);
               h.assertValueEqual(amount(machine), 1000, "selected bucket time yields exactly 1000 mB");
               h.assertTrue(tank(machine).getFluidInTank(0).is(Fluids.WATER), "weather produces only water");
               h.assertValueEqual(f.level.getChunkSource().getLoadedChunksCount(), loadedChunks,
                  "weather ticks at an already-loaded opening do not load additional chunks");
               machine.setItem(0, new ItemStack(Items.BUCKET));
               tick(f, machine, 1);
               h.assertTrue(machine.getItem(0).isEmpty() && machine.getItem(1).is(Items.WATER_BUCKET),
                  "existing bucket slots fill from collected rainwater");
               h.assertValueEqual(amount(machine), 0, "one bucket removes exactly 1000 mB");
               System.out.println("WEATHER_WATER_RATE funnel=" + funnel + " thunder=" + thunder
                  + " mB_per_80_ticks=" + quantum + " ticks_per_bucket=" + bucketTicks);
            }
         }
      }
      h.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void weather_collectors_require_native_rainfall_at_the_opening(GameTestHelper h) {
      try (var f = new WeatherFixture(h)) {
         for (boolean funnel : new boolean[] {true, false}) {
            f.reset();
            f.weather(false, false);
            var machine = f.place(funnel, f.pos);
            h.assertFalse(f.level.isRainingAt(f.pos.above()), "clear weather has no native rainfall");
            tick(f, machine, 160);
            h.assertValueEqual(amount(machine), 0, "clear weather produces no water");

            for (ResourceKey<Biome> biome : java.util.List.of(Biomes.DESERT, Biomes.SNOWY_PLAINS)) {
               f.reset();
               f.biome(biome);
               f.weather(true, true);
               machine = f.place(funnel, f.pos);
               h.assertTrue(f.level.isRaining() && f.level.isThundering(), "global weather alone is deliberately active");
               var precipitation = f.level.getBiome(f.pos.above()).value().getPrecipitationAt(f.pos.above());
               h.assertValueEqual(precipitation, biome.equals(Biomes.DESERT) ? Biome.Precipitation.NONE : Biome.Precipitation.SNOW,
                  "the real biome reports dry or snow-only precipitation");
               h.assertFalse(f.level.isRainingAt(f.pos.above()), "native rainfall excludes the selected biome");
               tick(f, machine, 160);
               h.assertValueEqual(amount(machine), 0, "dry and snow-only biomes never generate weather water");
            }

            for (Block roof : new Block[] {Blocks.GLASS, Blocks.STONE}) {
               f.reset();
               f.weather(true, true);
               machine = f.place(funnel, f.pos);
               f.level.setBlock(f.pos.above(2), roof.defaultBlockState(), Block.UPDATE_ALL);
               h.assertFalse(f.level.isRainingAt(f.pos.above()), "native rain is blocked by the roof");
               tick(f, machine, 160);
               h.assertValueEqual(amount(machine), 0, "roofed collector generates no weather water");
            }
         }
      }
      h.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void weather_collection_keeps_native_exposure_and_accumulator_source_priority(GameTestHelper h) {
      try (var f = new WeatherFixture(h)) {
         for (boolean thunder : new boolean[] {false, true}) {
            for (Block source : new Block[] {Blocks.WATER, Blocks.LAVA}) {
               f.reset();
               f.weather(true, thunder);
               var funnel = f.place(true, f.pos);
               f.level.setBlock(f.pos.above(), source.defaultBlockState(), Block.UPDATE_ALL);
               var before = f.level.getBlockState(f.pos.above());
               tick(f, funnel, 40);
               h.assertValueEqual(amount(funnel), 0, "a source directly above is not the Funnel's moving-fluid inlet");
               tick(f, funnel, 40);
               h.assertValueEqual(amount(funnel), 0, "native rain exposure does not manufacture water under a full fluid cell");
               h.assertValueEqual(f.level.getBlockState(f.pos.above()), before, "rejected direct source is not consumed");
            }
            for (Direction direction : Direction.values()) {
               f.reset();
               f.weather(true, thunder);
               var accumulator = f.place(false, f.pos);
               var source = f.pos.relative(direction);
               f.level.setBlock(source, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
               if (direction != Direction.UP)
                  h.assertTrue(f.level.isRainingAt(f.pos.above()), "side/bottom source fixture also has native rain exposure");
               tick(f, accumulator, 80);
               h.assertValueEqual(amount(accumulator), 250, "each of six adjacent source positions takes precedence over weather");
               h.assertTrue(f.level.getFluidState(source).isSource(), "accumulator leaves neighboring source intact");
            }
            f.reset();
            f.weather(true, thunder);
            var accumulator = f.place(false, f.pos);
            f.level.setBlock(f.pos, accumulator.getBlockState()
               .setValue(MachineBlocks.PassiveFluidAccumulatorBlock.WATERLOGGED, true), Block.UPDATE_ALL);
            tick(f, accumulator, 80);
            h.assertValueEqual(amount(accumulator), thunder ? 200 : 100,
               "own-cell waterlogging is not an adjacent source; only weather is collected");
            h.assertTrue(f.level.getFluidState(f.pos).isSource(), "weather collection leaves own-cell water intact");
         }
      }
      h.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void weather_collection_preserves_capacity_and_incompatible_tank_fluids(GameTestHelper h) {
      try (var f = new WeatherFixture(h)) {
         for (boolean funnel : new boolean[] {true, false}) {
            for (boolean thunder : new boolean[] {false, true}) {
               f.reset();
               f.weather(true, thunder);
               var machine = f.place(funnel, f.pos);
               int capacity = funnel ? 3000 : 4000;
               int quantum = funnel ? (thunder ? 100 : 50) : (thunder ? 200 : 100);
               h.assertValueEqual(tank(machine).getTankCapacity(0), capacity, "existing tank capacity is unchanged");
               h.assertValueEqual(tank(machine).fill(new FluidStack(Fluids.WATER, capacity - quantum), FluidAction.EXECUTE),
                  capacity - quantum, "fixture fills all but one weather quantum");
               tick(f, machine, 80);
               h.assertValueEqual(amount(machine), capacity, "weather can fill the final complete quantum");
               tick(f, machine, 80);
               h.assertValueEqual(amount(machine), capacity, "full tank cannot overflow");
               tank(machine).drain(1, FluidAction.EXECUTE);
               tick(f, machine, 80);
               h.assertValueEqual(amount(machine), capacity - 1, "existing all-or-nothing addFluid policy is retained near capacity");
            }
         }
         h.assertTrue(NeoForgeMod.MILK.isBound(), "existing milk fluid is available");
         for (Fluid fluid : new Fluid[] {Fluids.LAVA, NeoForgeMod.MILK.value()}) {
            f.reset();
            f.weather(true, true);
            var funnel = f.place(true, f.pos);
            h.assertValueEqual(tank(funnel).fill(new FluidStack(fluid, 1000), FluidAction.EXECUTE), 1000,
               "funnel retains existing acceptance of lava/milk");
            tick(f, funnel, 160);
            h.assertTrue(tank(funnel).getFluidInTank(0).is(fluid), "rain never overwrites the incompatible fluid");
            h.assertValueEqual(amount(funnel), 1000, "incompatible tank fluid is neither diluted nor increased");
            f.reset();
            var accumulator = f.place(false, f.pos);
            h.assertValueEqual(tank(accumulator).fill(new FluidStack(fluid, 1000), FluidAction.EXECUTE), 0,
               "accumulator still rejects lava/milk insertion");
         }
      }
      h.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void weather_water_uses_existing_funnel_downward_transfer(GameTestHelper h) {
      try (var f = new WeatherFixture(h)) {
         for (boolean thunder : new boolean[] {false, true}) {
            f.reset();
            f.weather(true, thunder);
            var funnel = f.place(true, f.pos);
            f.level.setBlock(f.pos.below(), ModBlocks.FLUID_BARREL.get().defaultBlockState(), Block.UPDATE_ALL);
            var barrel = (MachineBlockEntity) f.level.getBlockEntity(f.pos.below());
            tick(f, funnel, 79);
            h.assertValueEqual(amount(barrel), 0, "no transfer can mint rain before the first interval");
            tick(f, funnel, 1);
            h.assertValueEqual(amount(barrel), thunder ? 100 : 50, "collected rain transfers downward through the real fluid capability");
            h.assertValueEqual(amount(funnel), 0, "downward transfer removes exactly the amount delivered");
            tick(f, funnel, 80);
            h.assertValueEqual(amount(barrel), thunder ? 200 : 100, "subsequent interval preserves the selected weather rate");
            h.assertTrue(tank(barrel).getFluidInTank(0).is(Fluids.WATER), "receiver gets water only");
         }
      }
      h.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void weather_collectors_reload_tank_inventory_and_saved_tick_phase(GameTestHelper h) throws Exception {
      try (var f = new WeatherFixture(h)) {
         for (boolean funnel : new boolean[] {true, false}) {
            for (boolean thunder : new boolean[] {false, true}) {
               f.reset();
               f.weather(true, thunder);
               var original = f.place(funnel, f.pos);
               original.setItem(0, new ItemStack(Items.BUCKET, 2));
               tick(f, original, 79);
               var loaded = reload(f, original);
               h.assertTrue(loaded != original, "reload reconstructs a block entity from compressed NBT");
               h.assertValueEqual(loaded.getItem(0).getCount(), 2, "reload preserves bucket inventory");
               h.assertValueEqual(amount(loaded), 0, "reload does not generate water");
               tick(f, loaded, 1);
               int quantum = funnel ? (thunder ? 100 : 50) : (thunder ? 200 : 100);
               h.assertValueEqual(amount(loaded), quantum, "saved tick 79 resumes at the original tick-80 boundary");
               loaded = reload(f, loaded);
               h.assertValueEqual(amount(loaded), quantum, "compressed reload preserves weather water");
               tick(f, loaded, 1);
               h.assertValueEqual(amount(loaded), quantum, "reloading cannot duplicate the previous weather quantum");
            }
         }
      }
      h.succeed();
   }

   private static MachineBlockEntity reload(WeatherFixture f, MachineBlockEntity machine) throws Exception {
      var state = machine.getBlockState();
      var bytes = new ByteArrayOutputStream();
      NbtIo.writeCompressed(machine.saveWithFullMetadata(f.level.registryAccess()), bytes);
      var tag = NbtIo.readCompressed(new ByteArrayInputStream(bytes.toByteArray()), NbtAccounter.unlimitedHeap());
      f.level.setBlock(f.pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
      f.level.setBlock(f.pos, state, Block.UPDATE_ALL);
      var loaded = (MachineBlockEntity) f.level.getBlockEntity(f.pos);
      loaded.loadWithComponents(tag, f.level.registryAccess());
      return loaded;
   }

   private static IFluidHandler tank(MachineBlockEntity machine) { return machine.fluidHandler(Direction.UP); }
   private static int amount(MachineBlockEntity machine) { return tank(machine).getFluidInTank(0).getAmount(); }
   private static void tick(WeatherFixture f, MachineBlockEntity machine, int count) {
      for (int i = 0; i < count; i++)
         MachineBlockEntity.serverTick(f.level, machine.getBlockPos(), machine.getBlockState(), machine);
   }

   /** Synchronous native weather/biome fixture: no global world ticks occur before restoration. */
   private static final class WeatherFixture implements AutoCloseable {
      final GameTestHelper helper;
      final ServerLevel level;
      final BlockPos pos;
      final LevelChunkSection section;
      final PalettedContainerRO<Holder<Biome>> originalBiomes;
      final Map<BlockPos, BlockState> originalBlocks = new LinkedHashMap<>();
      final float originalRain;
      final float originalThunder;
      final int quartX, quartY, quartZ;

      WeatherFixture(GameTestHelper helper) {
         this.helper = helper;
         level = helper.getLevel();
         var anchor = helper.absolutePos(new BlockPos(3, 2, 3));
         pos = new BlockPos((anchor.getX() & ~15) + 8, level.getMaxBuildHeight() - 8, (anchor.getZ() & ~15) + 8);
         helper.assertTrue(level.hasChunkAt(pos), "fixture reuses the structure's already-loaded chunk");
         var chunk = level.getChunkAt(pos);
         section = chunk.getSection(level.getSectionIndex(pos.getY()));
         originalBiomes = section.getBiomes();
         quartX = chunk.getPos().x * 4;
         quartY = (pos.getY() >> 4) * 4;
         quartZ = chunk.getPos().z * 4;
         for (var p : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 3, 2))) {
            helper.assertTrue(level.getBlockEntity(p) == null, "high-air fixture must not replace an existing block entity");
            originalBlocks.put(p.immutable(), level.getBlockState(p));
         }
         originalRain = level.getRainLevel(1);
         // Native getThunderLevel is multiplied by rain strength; read the underlying strength at rain=1.
         level.setRainLevel(1);
         originalThunder = level.getThunderLevel(1);
         level.setRainLevel(originalRain);
      }

      void reset() {
         for (var p : originalBlocks.keySet()) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
         biome(Biomes.PLAINS);
      }

      void biome(ResourceKey<Biome> key) {
         var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(key);
         section.fillBiomesFromNoise((x, y, z, sampler) -> biome, level.getChunkSource().randomState().sampler(), quartX, quartY, quartZ);
      }

      void weather(boolean rain, boolean thunder) {
         level.setRainLevel(rain ? 1 : 0);
         level.setThunderLevel(thunder ? 1 : 0);
      }

      MachineBlockEntity place(boolean funnel, BlockPos target) {
         var block = funnel ? ModBlocks.SMALL_FLUID_FUNNEL.get() : ModBlocks.PASSIVE_FLUID_ACCUMULATOR.get();
         level.setBlock(target, block.defaultBlockState(), Block.UPDATE_ALL);
         var entity = level.getBlockEntity(target);
         helper.assertTrue(entity instanceof MachineBlockEntity, "real registered collector creates its block entity");
         return (MachineBlockEntity) entity;
      }

      @Override public void close() {
         for (var entry : originalBlocks.entrySet()) level.setBlock(entry.getKey(), entry.getValue(), Block.UPDATE_ALL);
         section.fillBiomesFromNoise((x, y, z, sampler) -> originalBiomes.get(x & 3, y & 3, z & 3),
            level.getChunkSource().randomState().sampler(), quartX, quartY, quartZ);
         level.setRainLevel(originalRain);
         level.setThunderLevel(originalThunder);
      }
   }
}
