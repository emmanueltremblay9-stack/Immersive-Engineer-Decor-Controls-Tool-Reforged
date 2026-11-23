package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CatalogueFluidloggingGameTests {
   @GameTest(template="empty")
   public static void catalogue_registry_inventory(GameTestHelper h) throws Exception {
      var rows = new java.util.ArrayList<String>();
      rows.add("id\tclass\tstates\twaterlogged\tnative_cell\ttransient_water\tcompact_fluid\tdefault_full_collision");
      long registered = BuiltInRegistries.BLOCK.keySet().stream()
         .filter(id -> id.getNamespace().equals(EngineersDecorReforged.MOD_ID)).count();
      h.assertValueEqual((long)ModBlocks.ORDERED_BLOCKS.size(), registered, "inventory includes every registered mod block");
      for (var entry : ModBlocks.ORDERED_BLOCKS) {
         var block = entry.get(); var state = block.defaultBlockState();
         h.assertTrue(block instanceof NativeFluidloggedBlock, "every registered mod block is explicitly integrated: " + entry.getId());
         boolean full = net.minecraft.world.level.block.Block.isShapeFullBlock(state.getCollisionShape(h.getLevel(),h.absolutePos(new BlockPos(3,2,3))));
         rows.add(String.join("\t", entry.getId().toString(), block.getClass().getName(),
            Integer.toString(block.getStateDefinition().getPossibleStates().size()),
            Boolean.toString(state.hasProperty(BlockStateProperties.WATERLOGGED)),
            Boolean.toString(state.hasProperty(NativeFluidloggedBlock.FLUID_CELL)),
            Boolean.toString(state.hasProperty(com.oblixorprime.engineersdecorreforged.block.StairWaterFlow.WATER_FLOW)),
            Boolean.toString(state.hasProperty(NativeFluidloggedBlock.FLUID_CONTENT)), Boolean.toString(full)));
      }
      java.nio.file.Files.write(java.nio.file.Path.of("catalogue-fluidlogging.tsv"), rows);
      for (var entry : ModBlocks.ORDERED_BLOCKS) {
         if (entry.get() instanceof com.oblixorprime.engineersdecorreforged.block.PoleHeadBlock) {
            var state = entry.get().defaultBlockState();
            h.assertTrue(state.hasProperty(NativeFluidloggedBlock.FLUID_CELL), "structural heads explicitly join the decorative native lot");
            h.assertValueEqual(state.getLightEmission(h.getLevel(), h.absolutePos(new BlockPos(3,2,3))), 0, "structural heads retain zero emission");
         }
      }
      h.succeed();
   }

   @GameTest(template="empty", timeoutTicks=220)
   public static void lights_natural_water_and_recession(GameTestHelper h) {
      var blocks = java.util.List.of(ModBlocks.IRON_BULB_LIGHT.get(), ModBlocks.IRON_INSET_LIGHT.get(),
         ModBlocks.IRON_CEILING_EDGE_LIGHT.get(), ModBlocks.IRON_FLOOR_EDGE_LIGHT.get());
      var level=h.getLevel();
      for(int i=0;i<blocks.size();i++) {
         BlockPos cell=h.absolutePos(new BlockPos(3,2,2+i*3));
         for(int x=-1;x<=3;x++) {
            level.setBlock(cell.east(x).below(),Blocks.STONE.defaultBlockState(),3);
            level.setBlock(cell.east(x).north(),Blocks.BRICKS.defaultBlockState(),3);
            level.setBlock(cell.east(x).south(),Blocks.BRICKS.defaultBlockState(),3);
         }
         level.setBlock(cell.west(2),Blocks.BRICKS.defaultBlockState(),3);
         level.setBlock(cell.east(3),Blocks.BRICKS.defaultBlockState(),3);
         level.setBlock(cell,blocks.get(i).defaultBlockState(),3);
         level.setBlock(cell.west(),Blocks.WATER.defaultBlockState(),3);
      }
      h.runAfterDelay(60,()->{
         for(int i=0;i<blocks.size();i++) {
            BlockPos cell=h.absolutePos(new BlockPos(3,2,2+i*3));
            var state=level.getBlockState(cell);
            h.assertTrue(state.is(blocks.get(i)),"arriving water retains light");
            h.assertTrue(!state.getFluidState().isSource() && state.getFluidState().getAmount()==7,"arriving water retains vanilla level7");
            h.assertValueEqual(state.getLightEmission(level,cell),14,"water preserves light emission");
            h.assertTrue(((NativeFluidloggedBlock)blocks.get(i)).pickupBlock(null,level,cell,state).isEmpty(),"flow never yields bucket");
            level.setBlock(cell.west(),Blocks.AIR.defaultBlockState(),3);
         }
         h.runAfterDelay(100,()->{
            for(int i=0;i<blocks.size();i++) {
               BlockPos cell=h.absolutePos(new BlockPos(3,2,2+i*3));
               h.assertTrue(level.getBlockState(cell).is(blocks.get(i)) && level.getFluidState(cell).isEmpty(),"recession retains dry light");
            }
            h.succeed();
         });
      });
   }
}
