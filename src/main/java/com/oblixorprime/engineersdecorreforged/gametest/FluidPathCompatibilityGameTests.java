package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.fluid.NativeFluidCells;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Exercises vanilla's slope search and Lithium's branching BFS through the same public world state. */
@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FluidPathCompatibilityGameTests {
   @GameTest(template="empty", timeoutTicks=100)
   public static void stored_fluid_branching_paths_match_free_fluid_controls(GameTestHelper h) throws Exception {
      if (Boolean.getBoolean("iedctr.requireLithiumFluidBfs")) {
         var lithium = net.neoforged.fml.ModList.get().getModContainerById("lithium").orElseThrow();
         h.assertValueEqual(lithium.getModInfo().getVersion().toString(), "0.15.4+mc1.21.1", "exact Lithium version loaded");
         h.assertTrue(java.util.Arrays.stream(FlowingFluid.class.getDeclaredMethods())
            .anyMatch(m -> m.getName().contains("calculateComplexFluidFlowDirections")), "Lithium fluid BFS remains enabled");
      }
      Method spread = FlowingFluid.class.getDeclaredMethod("getSpread", Level.class, BlockPos.class, BlockState.class);
      spread.setAccessible(true);
      for (String id : List.of("minecraft:water", "minecraft:lava", "immersiveengineering:creosote",
            "immersiveengineering:ethanol", "immersiveengineering:biodiesel", "immersiveengineering:plantoil")) {
         FlowingFluid fluid = (FlowingFluid) BuiltInRegistries.FLUID.get(ResourceLocation.parse(id));
         for (int scenario=0; scenario<3; scenario++) {
            // 0: occupied source blocks the nearest route. 1: a current allows it.
            // 2: a stored source below the east route must be recognised as a hole.
            Map<Direction, FluidState> control = directions(h, spread, fluid, scenario, false);
            Map<Direction, FluidState> stored = directions(h, spread, fluid, scenario, true);
            Set<Direction> expected = Set.of(scenario == 0 ? Direction.WEST : Direction.EAST);
            h.assertValueEqual(control.keySet(), expected, id+" free-fluid fixture has the expected shortest route "+scenario);
            h.assertValueEqual(stored, control, id+" stored source/current preserves branching flow decisions "+scenario);
         }
      }
      h.succeed();
   }

   @SuppressWarnings("unchecked")
   private static Map<Direction, FluidState> directions(GameTestHelper h, Method spread,
         FlowingFluid fluid, int scenario, boolean stored) throws Exception {
      var level = h.getLevel();
      BlockPos origin = h.absolutePos(new BlockPos(3,2,3));
      for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-3,-2,-3), origin.offset(3,1,3))) {
         level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
      }
      for (int dx=-2; dx<=2; dx++) level.setBlock(origin.offset(dx,0,0), Blocks.AIR.defaultBlockState(), 3);
      // A second viable branch forces Lithium's BFS even when the east source is blocked.
      level.setBlock(origin.north(), Blocks.AIR.defaultBlockState(), 3);
      level.setBlock(origin.west(2).below(), Blocks.AIR.defaultBlockState(), 3);
      level.setBlock(origin.east().below(), Blocks.AIR.defaultBlockState(), 3);
      level.setBlock(origin, fluid.getSource(false).createLegacyBlock(), 3);
      BlockPos cell = scenario == 2 ? origin.east().below() : origin.east();
      FluidState resident = scenario == 1 ? fluid.getFlowing(4,false) : fluid.getSource(false);
      if (stored) {
         // The open pole does not seal the downward face. A catwalk deck would
         // intentionally block that route and invalidate the free-fluid control.
         level.setBlock(cell, ModBlocks.THICK_STEEL_POLE.get().defaultBlockState(), 3);
         h.assertTrue(NativeFluidCells.place(level,cell,level.getBlockState(cell),resident), "fixture stores a qualified fluid");
         h.assertTrue(level.getBlockState(cell).getFluidState().isEmpty(), "fixture requires a positional attachment read");
         h.assertValueEqual(level.getFluidState(cell), resident, "LevelChunk exposes the resident fluid with current mixins");
      } else {
         level.setBlock(cell, resident.createLegacyBlock(), 3);
      }
      return Map.copyOf((Map<Direction, FluidState>) spread.invoke(fluid, level, origin, level.getBlockState(origin)));
   }
}
