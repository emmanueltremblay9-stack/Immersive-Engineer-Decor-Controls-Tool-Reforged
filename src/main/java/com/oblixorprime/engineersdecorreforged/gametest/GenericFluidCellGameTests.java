package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.*;
import com.oblixorprime.engineersdecorreforged.block.*;
import com.oblixorprime.engineersdecorreforged.fluid.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.storage.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GenericFluidCellGameTests {
   private static FlowingFluid fluid(String id) {
      var fluid=BuiltInRegistries.FLUID.get(ResourceLocation.parse(id));
      if(!(fluid instanceof FlowingFluid flowing))throw new AssertionError("Expected registered standard fluid: "+id);
      return flowing;
   }
   private static List<FlowingFluid> samples(){return List.of(Fluids.WATER,Fluids.LAVA,fluid("immersiveengineering:creosote"),fluid("immersiveengineering:ethanol"),fluid("immersiveengineering:biodiesel"),fluid("immersiveengineering:plantoil"));}
   @GameTest(template="empty")
   public static void generic_fluid_immutable_registry_codec_and_unknown_preservation(GameTestHelper h) {
      var pos=h.absolutePos(new BlockPos(3,2,3));var data=FluidCellData.EMPTY;
      for(var fluid:samples())for(boolean falling:new boolean[]{false,true})for(int amount=1;amount<=8;amount++) {
         var state=fluid.getFlowing(amount,falling);var before=data;data=data.with(pos,state);
         h.assertTrue(!before.contains(pos) || before.get(pos)!=state,"old snapshot is not mutated");
         h.assertValueEqual(FluidCellData.decode(data.encode()).get(pos),state,"registry codec retains exact level and falling properties");
         h.assertTrue(data.encode().getCompound(Long.toString(pos.asLong())).contains("Name"),"disk stores names, never numeric fluid IDs");
      }
      var unknown=new CompoundTag();var cell=new CompoundTag();cell.putString("Name","removed_mod:missing_fluid");unknown.put(Long.toString(pos.asLong()),cell);
      var loaded=FluidCellData.decode(unknown);
      h.assertTrue(loaded.contains(pos) && loaded.get(pos).isEmpty(),"missing fluid is inert but its cell remains reserved");
      h.assertValueEqual(loaded.encode(),unknown,"missing-mod data survives save without conversion or deletion");
      h.assertTrue(!NativeFluidCells.standard(Fluids.EMPTY),"non-flowing empty fluid is unsupported");h.succeed();
   }
   @GameTest(template="empty")
   public static void generic_fluid_compressed_chunk_reload(GameTestHelper h)throws Exception {
      var level=h.getLevel();var cp=new net.minecraft.world.level.ChunkPos(h.absolutePos(new BlockPos(3,2,3)));var saved=new LevelChunk(level,cp);
      var data=FluidCellData.EMPTY;int index=0;
      for(var fluid:samples())for(int amount=1;amount<=8;amount++) {
         var pos=new BlockPos(cp.getMinBlockX()+index%16,80+index/16,cp.getMinBlockZ());index++;
         saved.setBlockState(pos,ModBlocks.STEEL_CATWALK.get().defaultBlockState(),false);
         data=data.with(pos,fluid.getFlowing(amount,(amount&1)==0));
      }
      saved.setData(NativeFluidCells.CELLS.get(),data);
      var bytes=new java.io.ByteArrayOutputStream();NbtIo.writeCompressed(ChunkSerializer.write(level,saved),bytes);
      var disk=NbtIo.readCompressed(new java.io.ByteArrayInputStream(bytes.toByteArray()),NbtAccounter.unlimitedHeap());
      var loaded=ChunkSerializer.read(level,level.getPoiManager(),new RegionStorageInfo("generic-fluid-test",level.dimension(),"chunk"),cp,disk);
      var actual=loaded.getData(NativeFluidCells.CELLS.get());
      h.assertValueEqual(actual.encode(),data.encode(),"real compressed chunk attachment retains every fluid and level");
      if(loaded instanceof ImposterProtoChunk wrapper)for(long packed:data.positions())h.assertValueEqual(wrapper.getWrapped().getFluidState(BlockPos.of(packed)),data.get(BlockPos.of(packed)),"loaded level chunk exposes cells positionally");
      h.succeed();
   }
   @GameTest(template="empty")
   public static void generic_fluid_survival_buckets_and_geometry(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(new BlockPos(3,2,3));
      var player=new net.neoforged.neoforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"GenericBuckets"));
      player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
      player.setPos(pos.getX()+.5,pos.getY()+2,pos.getZ()+.5);player.setYRot(0);player.setXRot(90);
      for(var block:List.of(ModBlocks.STEEL_CATWALK.get(),ModBlocks.CLINKER_BRICK_BLOCK.get(),ModBlocks.PASSIVE_FLUID_ACCUMULATOR.get()))for(var fluid:samples()) {
         if(!(fluid.getBucket() instanceof BucketItem))continue; // Milk keeps its native drinking action.
         for(var p:BlockPos.betweenClosed(pos.offset(-1,0,-1),pos.offset(1,3,1)))level.setBlock(p,Blocks.AIR.defaultBlockState(),3);
         level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);level.setBlock(pos,block.defaultBlockState(),3);
         player.setShiftKeyDown(block==ModBlocks.PASSIVE_FLUID_ACCUMULATOR.get());
         for(int cycle=0;cycle<2;cycle++) {
            var dry=level.getBlockState(pos);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(fluid.getBucket()));
            h.assertTrue(player.gameMode.useItem(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction(),"real bucket inserts registered source "+fluid);
            h.assertTrue(player.getMainHandItem().is(Items.BUCKET),"insertion returns one empty bucket");
            var wet=level.getBlockState(pos);h.assertTrue(wet.is(block) && level.getFluidState(pos).getType().isSame(fluid) && level.getFluidState(pos).isSource(),"source and block identity preserved");
            h.assertTrue(!((NativeFluidloggedBlock)block).canPlaceLiquid(player,level,pos,wet,fluid),"second bucket rejected");
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));
            h.assertTrue(player.gameMode.useItem(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction(),"real empty bucket drains cell");
            h.assertTrue(player.getMainHandItem().is(fluid.getBucket()),"exact bucket returned");
            h.assertTrue(level.getFluidState(pos).isEmpty() && level.getBlockState(pos).equals(dry),"drain retains dry geometry/state");
            h.assertTrue(((NativeFluidloggedBlock)block).pickupBlock(player,level,pos,dry).isEmpty(),"no duplicate bucket");
         }
      }
      h.succeed();
   }
   private static void channel(GameTestHelper h,String id) {
      var level=h.getLevel();var cell=h.absolutePos(new BlockPos(3,2,2));var control=cell.south(2);var fluid=fluid(id);
      for(var p:BlockPos.betweenClosed(cell.offset(-2,-1,-1),cell.offset(1,2,3)))level.setBlock(p,p.getY()==cell.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
      for(int x=-2;x<=1;x++)for(int z:new int[]{-1,1,3})level.setBlock(cell.offset(x,0,z),Blocks.BRICKS.defaultBlockState(),3);
      for(var p:List.of(cell.east(),control.east(),cell.west(2),control.west(2)))level.setBlock(p,Blocks.BRICKS.defaultBlockState(),3);
      var block=ModBlocks.STEEL_CATWALK.get();level.setBlock(cell,block.defaultBlockState(),3);
      level.setBlock(cell.west(),fluid.getSource(false).createLegacyBlock(),3);level.setBlock(control.west(),fluid.getSource(false).createLegacyBlock(),3);
      h.runAfterDelay(180,()->{
         var actual=level.getFluidState(cell);var expected=level.getFluidState(control);
         h.assertTrue(!actual.isEmpty() && !actual.isSource(),"native receives transient "+id+" actual="+actual);
         h.assertValueEqual(actual,expected,"amount and falling follow vanilla control");
         h.assertTrue(((NativeFluidloggedBlock)block).pickupBlock(null,level,cell,level.getBlockState(cell)).isEmpty(),"current cannot produce bucket");
         level.setBlock(cell.west(),Blocks.AIR.defaultBlockState(),3);level.setBlock(control.west(),Blocks.AIR.defaultBlockState(),3);
         h.runAfterDelay(400,()->{h.assertTrue(level.getBlockState(cell).is(block) && level.getFluidState(cell).isEmpty(),"recession preserves block and leaves no unpaid source");h.succeed();});
      });
   }
   @GameTest(template="empty",timeoutTicks=650) public static void generic_flow_water(GameTestHelper h){channel(h,"minecraft:water");}
   @GameTest(template="empty",timeoutTicks=650) public static void generic_flow_lava(GameTestHelper h){channel(h,"minecraft:lava");}
   @GameTest(template="empty",timeoutTicks=650) public static void generic_flow_creosote(GameTestHelper h){channel(h,"immersiveengineering:creosote");}
   @GameTest(template="empty",timeoutTicks=650) public static void generic_flow_ethanol(GameTestHelper h){channel(h,"immersiveengineering:ethanol");}
   @GameTest(template="empty",timeoutTicks=650) public static void generic_flow_biodiesel(GameTestHelper h){channel(h,"immersiveengineering:biodiesel");}
   @GameTest(template="empty",timeoutTicks=650) public static void generic_flow_plantoil(GameTestHelper h){channel(h,"immersiveengineering:plantoil");}
   @GameTest(template="empty",timeoutTicks=200)
   public static void generic_fluid_network_palette_delta_and_removal(GameTestHelper h) {
      var level=h.getLevel();var chunk=level.getChunkAt(h.absolutePos(new BlockPos(3,2,3)));var cp=chunk.getPos();var encoded=new CompoundTag();var fluids=samples();
      for(int i=0;i<512;i++){
         var pos=new BlockPos(cp.getMinBlockX()+i%16,80+i/256,cp.getMinBlockZ()+(i/16)%16);
         encoded.put(Long.toString(pos.asLong()),FluidState.CODEC.encodeStart(NbtOps.INSTANCE,fluids.get(i%fluids.size()).getFlowing(3,(i&1)==0)).getOrThrow());
      }
      var original=FluidCellData.decode(encoded);var raw=io.netty.buffer.Unpooled.buffer();
      var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(raw,level.registryAccess());
      try {
         original.writeNetwork(buffer,true);h.assertTrue(buffer.readableBytes()<7000,"initial palette avoids repeated fluid names/properties");
         var received=FluidCellData.readNetwork(buffer,null,chunk);h.assertValueEqual(received.encode(),original.encode(),"initial named palette roundtrip");
         var pos=BlockPos.of(original.positions().iterator().next());var changed=original.with(pos,fluid("immersiveengineering:ethanol").getSource(true));
         buffer.clear();changed.writeNetwork(buffer,false);h.assertTrue(buffer.readableBytes()<256,"one-cell update is a bounded delta");
         received=FluidCellData.readNetwork(buffer,received,chunk);h.assertValueEqual(received.encode(),changed.encode(),"delta keeps all unchanged cells");
         h.assertValueEqual(original.encode(),encoded,"old snapshot is immutable");
         buffer.clear();changed.with(pos,Fluids.EMPTY.defaultFluidState()).writeNetwork(buffer,false);
         received=FluidCellData.readNetwork(buffer,received,chunk);h.assertTrue(!received.contains(pos) && received.positions().size()==511,"removal delta leaves no hidden reservation");
      }finally{raw.release();}h.succeed();
   }
   @GameTest(template="empty")
   public static void generic_fluid_exact_source_states_and_unsupported_world_behavior(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(new BlockPos(3,2,3));var block=ModBlocks.STEEL_CATWALK.get();var container=(NativeFluidloggedBlock)block;
      level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);
      for(var fluid:samples())for(boolean falling:new boolean[]{false,true}){
         level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);level.setBlock(pos,block.defaultBlockState(),3);var expected=fluid.getSource(falling);
         h.assertTrue(container.placeLiquid(level,pos,level.getBlockState(pos),expected),"source insertion accepted");
         h.assertValueEqual(level.getFluidState(pos),expected,"exact source falling/properties retained");
         h.assertTrue(container.pickupBlock(null,level,pos,level.getBlockState(pos)).is(fluid.getBucket()),"source drains once to its own bucket");
         h.assertTrue(level.getFluidState(pos).isEmpty(),"no hidden source remains");
      }
      var milk=fluid("minecraft:milk");h.assertTrue(!NativeFluidCells.standard(milk),"milk has no world liquid block");
      h.assertTrue(!container.placeLiquid(level,pos,level.getBlockState(pos),milk.getSource(false)),"unsupported world behavior is rejected explicitly");
      var tag=new CompoundTag();tag.put(Long.toString(pos.asLong()),FluidState.CODEC.encodeStart(NbtOps.INSTANCE,milk.getSource(false)).getOrThrow());
      var preserved=FluidCellData.decode(tag);h.assertTrue(preserved.contains(pos) && preserved.get(pos).isEmpty(),"unsupported persisted state is inert and reserved");h.assertValueEqual(preserved.encode(),tag,"unsupported data preserved");h.succeed();
   }
   @GameTest(template="empty")
   public static void generic_fluid_lighting_getter_never_requests_world_fluid(GameTestHelper h) {
      var state=ModBlocks.STEEL_CATWALK.get().defaultBlockState();
      net.minecraft.world.level.BlockGetter view=new net.minecraft.world.level.BlockGetter(){
         public net.minecraft.world.level.block.entity.BlockEntity getBlockEntity(BlockPos p){return null;}
         public net.minecraft.world.level.block.state.BlockState getBlockState(BlockPos p){throw new AssertionError("lighting must not load blocks");}
         public FluidState getFluidState(BlockPos p){throw new AssertionError("lighting must not request FULL chunks");}
         public int getHeight(){return 384;}public int getMinBuildHeight(){return -64;}
         public net.neoforged.neoforge.common.world.AuxiliaryLightManager getAuxLightManager(net.minecraft.world.level.ChunkPos cp){return new net.neoforged.neoforge.common.world.AuxiliaryLightManager(){public void setLightAt(BlockPos p,int v){}public int getLightAt(BlockPos p){return 15;}};}
      };
      h.assertValueEqual(state.getLightEmission(view,BlockPos.ZERO),15,"light uses nonblocking auxiliary value");
      var level=h.getLevel();var pos=h.absolutePos(new BlockPos(3,2,3));level.setBlock(pos,ModBlocks.STEEL_CATWALK.get().defaultBlockState(),3);
      NativeFluidCells.place(level,pos,level.getBlockState(pos),Fluids.FLOWING_LAVA.getFlowing(4,false));
      h.assertValueEqual(level.getAuxLightManager(pos).getLightAt(pos),15,"generic flowing lava registers emission");
      NativeFluidCells.set(level,pos,Fluids.EMPTY.defaultFluidState());h.assertValueEqual(level.getAuxLightManager(pos).getLightAt(pos),0,"drain clears auxiliary emission");h.succeed();
   }

}
