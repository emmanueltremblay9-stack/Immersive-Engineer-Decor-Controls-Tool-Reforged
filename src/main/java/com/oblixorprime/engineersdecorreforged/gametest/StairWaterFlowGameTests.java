package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import com.oblixorprime.engineersdecorreforged.block.StairWaterFlow;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StairWaterFlowGameTests {
   @GameTest(template="empty",timeoutTicks=220)
   public static void stair_water_flow_east(GameTestHelper h) { channel(h,Direction.EAST,false); }
   @GameTest(template="empty",timeoutTicks=220)
   public static void stair_water_flow_west(GameTestHelper h) { channel(h,Direction.WEST,false); }
   @GameTest(template="empty",timeoutTicks=220)
   public static void stair_water_flow_north(GameTestHelper h) { channel(h,Direction.NORTH,false); }
   @GameTest(template="empty",timeoutTicks=220)
   public static void stair_water_flow_south(GameTestHelper h) { channel(h,Direction.SOUTH,false); }
   @GameTest(template="empty",timeoutTicks=220)
   public static void stair_water_flow_railed(GameTestHelper h) { channel(h,Direction.EAST,true); }

   private static void channel(GameTestHelper h,Direction facing,boolean rails) {
      BlockPos source=h.absolutePos(new BlockPos(5,2,3));
      var level=h.getLevel();
      for(int d=-1;d<=5;d++) {
         BlockPos p=source.west(d);
         level.setBlock(p,Blocks.AIR.defaultBlockState(),3);
         level.setBlock(p.below(),Blocks.STONE.defaultBlockState(),3);
         level.setBlock(p.north(),Blocks.BRICKS.defaultBlockState(),3);
         level.setBlock(p.south(),Blocks.BRICKS.defaultBlockState(),3);
      }
      level.setBlock(source.east(),Blocks.BRICKS.defaultBlockState(),3);
      level.setBlock(source.west(5),Blocks.BRICKS.defaultBlockState(),3);
      BlockPos cell=source.west();
      var stair=ModBlocks.STEEL_CATWALK_STAIRS.get();
      level.setBlock(cell,stair.defaultBlockState().setValue(PortedBlocks.HORIZONTAL_FACING,facing)
         .setValue(PortedBlocks.LEFT_RAILING,rails).setValue(PortedBlocks.RIGHT_RAILING,rails).setValue(PortedBlocks.EDITED_STAIR_SIDES,rails ? 3 : 0),3);
      level.setBlock(source,Blocks.WATER.defaultBlockState(),3);
      h.runAfterDelay(60,()->{
         var state=level.getBlockState(cell);
         h.assertTrue(state.is(stair),"natural water preserves stair");
         if(rails) h.assertTrue(state.getValue(PortedBlocks.LEFT_RAILING) && state.getValue(PortedBlocks.RIGHT_RAILING),"railings survive flow updates");
         h.assertTrue(!state.getFluidState().isSource(),"natural flow creates no source");
         h.assertValueEqual(state.getFluidState().getAmount(),7,"first cell keeps vanilla amount7");
         h.assertValueEqual(level.getFluidState(cell.west()).getAmount(),6,"downstream keeps vanilla amount6");
         h.assertValueEqual(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),NbtUtils.writeBlockState(state)),state,"flow level survives state codec");
         h.assertTrue(((PortedBlocks.CatwalkStairsBlock)stair).pickupBlock(null,level,cell,state).isEmpty(),"no bucket from transient water");
         level.setBlock(source,Blocks.AIR.defaultBlockState(),3);
         h.runAfterDelay(100,()->{
            h.assertTrue(level.getBlockState(cell).is(stair),"recession preserves stair");
            for(int d=0;d<5;d++) h.assertTrue(level.getFluidState(source.west(d)).isEmpty(),"no water/source remains after recession");
            h.succeed();
         });
      });
   }

   @GameTest(template="empty",timeoutTicks=200)
   public static void stair_water_flow_vertical(GameTestHelper h) {
      var level=h.getLevel(); BlockPos cell=h.absolutePos(new BlockPos(3,2,3));
      level.setBlock(cell.below(),Blocks.STONE.defaultBlockState(),3);
      level.setBlock(cell,ModBlocks.STEEL_CATWALK_STAIRS.get().defaultBlockState(),3);
      level.setBlock(cell.above(),Blocks.WATER.defaultBlockState(),3);
      h.runAfterDelay(40,()->{
         var flow=level.getFluidState(cell);
         h.assertTrue(!flow.isSource() && flow.getAmount()==8 && flow.getValue(net.minecraft.world.level.material.FlowingFluid.FALLING),"above source supplies falling water8");
         level.setBlock(cell.above(),Blocks.AIR.defaultBlockState(),3);
         h.runAfterDelay(100,()->{h.assertTrue(level.getFluidState(cell).isEmpty(),"vertical supply recedes");h.succeed();});
      });
   }

   @GameTest(template="empty",timeoutTicks=100)
   public static void stair_water_flow_no_source_promotion(GameTestHelper h) {
      var level=h.getLevel(); BlockPos cell=h.absolutePos(new BlockPos(3,2,3));
      for(int x=-1;x<=1;x++) {
         level.setBlock(cell.offset(x,-1,0),Blocks.STONE.defaultBlockState(),3);
         level.setBlock(cell.offset(x,0,-1),Blocks.BRICKS.defaultBlockState(),3);
         level.setBlock(cell.offset(x,0,1),Blocks.BRICKS.defaultBlockState(),3);
      }
      // Close both immediate drops so vanilla slope selection actually supplies the center.
      level.setBlock(cell.east(2),Blocks.BRICKS.defaultBlockState(),3);
      level.setBlock(cell.west(2),Blocks.BRICKS.defaultBlockState(),3);
      level.setBlock(cell,ModBlocks.STEEL_CATWALK_STAIRS.get().defaultBlockState().setValue(PortedBlocks.HORIZONTAL_FACING,Direction.EAST),3);
      level.setBlock(cell.east(),Blocks.WATER.defaultBlockState(),3);
      level.setBlock(cell.west(),Blocks.WATER.defaultBlockState(),3);
      h.runAfterDelay(40,()->{
         h.assertTrue(!level.getFluidState(cell).isSource() && level.getFluidState(cell).getAmount()==7,"two sources do not promote transient cell: "+level.getBlockState(cell)+" fluid="+level.getFluidState(cell)+" above="+level.getBlockState(cell.above()));
         h.succeed();
      });
   }

   @GameTest(template="empty")
   public static void stair_water_flow_compressed_chunk_reload(GameTestHelper h) throws Exception {
      var level=h.getLevel(); var chunkPos=new net.minecraft.world.level.ChunkPos(h.absolutePos(new BlockPos(3,2,3)));
      var saved=new net.minecraft.world.level.chunk.LevelChunk(level,chunkPos);
      var stair=ModBlocks.STEEL_CATWALK_STAIRS.get().defaultBlockState();
      for(int value=0;value<16;value++) saved.setBlockState(new BlockPos(chunkPos.getMinBlockX()+value,80,chunkPos.getMinBlockZ()),stair.setValue(PortedBlocks.CatwalkStairsBlock.WATER_FLOW,value),false);
      var tag=net.minecraft.world.level.chunk.storage.ChunkSerializer.write(level,saved);
      var bytes=new java.io.ByteArrayOutputStream(); net.minecraft.nbt.NbtIo.writeCompressed(tag,bytes);
      var disk=net.minecraft.nbt.NbtIo.readCompressed(new java.io.ByteArrayInputStream(bytes.toByteArray()),net.minecraft.nbt.NbtAccounter.unlimitedHeap());
      var loaded=net.minecraft.world.level.chunk.storage.ChunkSerializer.read(level,level.getPoiManager(),new net.minecraft.world.level.chunk.storage.RegionStorageInfo("stair-water-test",level.dimension(),"chunk"),chunkPos,disk);
      for(int value=0;value<16;value++) {
         var pos=new BlockPos(chunkPos.getMinBlockX()+value,80,chunkPos.getMinBlockZ());
         h.assertValueEqual(loaded.getBlockState(pos),stair.setValue(PortedBlocks.CatwalkStairsBlock.WATER_FLOW,value),"compressed reload preserves exact transient level/falling state");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void stair_water_flow_bucket_and_source_state(GameTestHelper h) {
      var stair=(PortedBlocks.CatwalkStairsBlock)ModBlocks.STEEL_CATWALK_STAIRS.get();
      BlockPos p=h.absolutePos(new BlockPos(3,2,3));
      var flowing=StairWaterFlow.withFlow(stair.defaultBlockState(),Fluids.FLOWING_WATER.getFlowing(4,false));
      h.getLevel().setBlock(p,flowing,3);
      h.assertTrue(!flowing.getFluidState().isSource(),"level4 is not source");
      h.assertTrue(!stair.placeLiquid(h.getLevel(),p,flowing,Fluids.LAVA.getSource(false)),"lava cannot overwrite transient water");
      h.assertTrue(!stair.placeLiquid(h.getLevel(),p,flowing,((net.minecraft.world.level.material.FlowingFluid)com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock.creosote()).getSource(false)),"creosote cannot overwrite transient water");
      var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"StairFlowBucket"));
      player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
      player.setPos(p.getX()+0.5,p.getY()+2,p.getZ()+0.5); player.setYRot(0); player.setXRot(90);
      player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(Items.BUCKET));
      player.gameMode.useItem(player,h.getLevel(),player.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND);
      h.assertValueEqual(h.getLevel().getBlockState(p),flowing,"bucket attempt leaves current untouched");
      h.assertTrue(player.getMainHandItem().is(Items.BUCKET),"real BucketItem.use cannot collect flowing stair water");

      h.assertTrue(stair.pickupBlock(null,h.getLevel(),p,flowing).isEmpty(),"flow cannot fill bucket");
      h.assertTrue(stair.placeLiquid(h.getLevel(),p,flowing,Fluids.WATER.getSource(false)),"normal bucket can replace transient water with intentional source");
      var filled=h.getLevel().getBlockState(p);
      h.assertTrue(filled.getValue(PortedBlocks.WATERLOGGED),"existing source contract");
      var legacy=NbtUtils.writeBlockState(filled); legacy.getCompound("Properties").remove("water_flow");
      h.assertValueEqual(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),legacy),filled,"old source saves lacking water_flow retain source and default transient0");
      h.assertValueEqual(filled.getValue(PortedBlocks.CatwalkStairsBlock.WATER_FLOW),0,"no hidden old current");
      h.assertTrue(stair.pickupBlock(null,h.getLevel(),p,filled).is(Items.WATER_BUCKET),"source yields one bucket");
      h.assertTrue(h.getLevel().getFluidState(p).isEmpty(),"drained source reveals no hidden flow");
      h.succeed();
   }
}
