package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock;
import com.oblixorprime.engineersdecorreforged.block.StairWaterFlow;
import com.oblixorprime.engineersdecorreforged.rsgauges.ControlsModule;
import com.oblixorprime.engineersdecorreforged.rsgauges.ControlsBlockTypes;
import com.oblixorprime.engineersdecorreforged.rsgauges.IndustrialSwitchBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ControlFluidloggingGameTests {
   private static Block control(String name) {
      return ControlsModule.CONTROLS.stream().filter(entry -> entry.getId().getPath().equals(name)).findFirst().orElseThrow().get();
   }
   private static BlockState wet(BlockState state, Fluid fluid) {
      return NativeFluidloggedBlock.placement(state, ((net.minecraft.world.level.material.FlowingFluid)fluid).getSource(false));
   }
   @GameTest(template="empty")
   public static void controls_legacy_defaults_keep_signals_and_no_fluid(GameTestHelper h) {
      for(var entry:ControlsModule.CONTROLS) {
         var state=entry.get().defaultBlockState();
         var tag=net.minecraft.nbt.NbtUtils.writeBlockState(state);
         for(String key:java.util.List.of("waterlogged","fluid_cell","water_flow")) tag.getCompound("Properties").remove(key);
         var restored=net.minecraft.nbt.NbtUtils.readBlockState(net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(),tag);
         h.assertValueEqual(restored,state,"legacy control retains all signal/orientation properties with empty new cells: "+entry.getId());
         h.assertTrue(restored.getFluidState().isEmpty(),"legacy control loads dry");
      }
      h.succeed();
   }
   @GameTest(template="empty")
   public static void cabinet_independent_sources_survive_power_and_pair_destruction(GameTestHelper h) {
      var level=h.getLevel(); var player=h.makeMockPlayer(GameType.SURVIVAL);
      BlockPos lower=new BlockPos(3,2,3), upper=lower.above(); var block=control("industrialswitch");
      for(Fluid lowerFluid:java.util.List.of(Fluids.WATER,Fluids.LAVA,NativeFluidloggedBlock.creosote())) {
         for(Fluid upperFluid:java.util.List.of(Fluids.WATER,Fluids.LAVA,NativeFluidloggedBlock.creosote())) {
            h.setBlock(lower,wet(block.defaultBlockState().setValue(IndustrialSwitchBlock.PART,IndustrialSwitchBlock.Part.LOWER),lowerFluid));
            h.setBlock(upper,wet(block.defaultBlockState().setValue(IndustrialSwitchBlock.PART,IndustrialSwitchBlock.Part.UPPER),upperFluid));
            h.useBlock(lower,player);
            h.assertTrue(level.getFluidState(h.absolutePos(lower)).getType()==lowerFluid,"power change preserves lower cell");
            h.assertTrue(level.getFluidState(h.absolutePos(upper)).getType()==upperFluid,"power synchronization preserves upper independent cell");
            h.assertTrue(h.getBlockState(upper).getValue(ControlsBlockTypes.POWERED),"upper signal mirrors lower");
            // After release, vanilla lava/water chemistry is authoritative. Test pair destruction
            // with non-reacting pairs; asymmetric lava/water cells above still exercise power sync.
            if (!((lowerFluid==Fluids.LAVA && upperFluid==Fluids.WATER) || (lowerFluid==Fluids.WATER && upperFluid==Fluids.LAVA))) {
               level.destroyBlock(h.absolutePos(upper),true);
               h.assertTrue(level.getFluidState(h.absolutePos(lower)).getType()==lowerFluid,"pair-loss restores lower "+lowerFluid+" actual="+h.getBlockState(lower));
               h.assertTrue(level.getFluidState(h.absolutePos(upper)).getType()==upperFluid,"upper destruction restores own source");
            }
            h.setBlock(lower,Blocks.AIR); h.setBlock(upper,Blocks.AIR);
         }
      }
      h.succeed();
   }
   @GameTest(template="empty")
   public static void wet_latching_controls_preserve_cells_through_both_edges(GameTestHelper h) {
      var player=h.makeMockPlayer(GameType.CREATIVE);BlockPos pos=new BlockPos(3,2,3);
      h.setBlock(pos.below(),Blocks.STONE);
      int controls=0;
      for(var entry:ControlsModule.CONTROLS) {
         var block=entry.get();
         if(block.getClass()!=ControlsBlockTypes.ToggleSwitchBlock.class && !(block instanceof IndustrialSwitchBlock))continue;
         controls++;
         for(int cell=0;cell<4;cell++) {
            var state=block.defaultBlockState().setValue(ControlsBlockTypes.FACING,Direction.UP);
            state=cell==3?StairWaterFlow.withFlow(state,Fluids.FLOWING_WATER.getFlowing(5,false)):
               wet(state,java.util.List.of(Fluids.WATER,Fluids.LAVA,NativeFluidloggedBlock.creosote()).get(cell));
            var expected=state.getFluidState();h.setBlock(pos,state);
            h.useBlock(pos,player);
            h.assertTrue(h.getBlockState(pos).getValue(ControlsBlockTypes.POWERED),"wet rising edge remains functional");
            h.assertValueEqual(h.getBlockState(pos).getSignal(h.getLevel(),h.absolutePos(pos),Direction.NORTH),15,"wet lever still emits15");
            h.assertValueEqual(h.getBlockState(pos).getFluidState(),expected,"rising edge preserves cell");
            h.useBlock(pos,player);
            h.assertFalse(h.getBlockState(pos).getValue(ControlsBlockTypes.POWERED),"wet falling edge remains functional");
            h.assertValueEqual(h.getBlockState(pos).getFluidState(),expected,"falling edge preserves cell");
         }
      }
      h.assertValueEqual(controls,23,"all latching families covered");h.succeed();
   }

   @GameTest(template="empty")
   public static void wet_cabinet_native_break_releases_each_cell_and_one_item(GameTestHelper h) {
      var level=h.getLevel();BlockPos lower=h.absolutePos(new BlockPos(3,2,3));var block=control("industrialswitch");
      var player=new net.neoforged.neoforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"WetCabinet"));
      for(boolean creative:new boolean[]{false,true})for(int half=0;half<2;half++)for(int fluid=0;fluid<4;fluid++) {
         var area=new net.minecraft.world.phys.AABB(lower).inflate(2);
         level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area).forEach(net.minecraft.world.entity.Entity::discard);
         level.setBlock(lower,Blocks.AIR.defaultBlockState(),2);level.setBlock(lower.above(),Blocks.AIR.defaultBlockState(),2);
         var bottom=block.defaultBlockState().setValue(IndustrialSwitchBlock.PART,IndustrialSwitchBlock.Part.LOWER);
         var top=block.defaultBlockState().setValue(IndustrialSwitchBlock.PART,IndustrialSwitchBlock.Part.UPPER);
         if(fluid==3){bottom=StairWaterFlow.withFlow(bottom,Fluids.FLOWING_WATER.getFlowing(5,false));top=StairWaterFlow.withFlow(top,Fluids.FLOWING_WATER.getFlowing(4,false));}
         else {var value=java.util.List.of(Fluids.WATER,Fluids.LAVA,NativeFluidloggedBlock.creosote()).get(fluid);bottom=wet(bottom,value);top=wet(top,fluid==0?NativeFluidloggedBlock.creosote():value);}
         var bottomFluid=bottom.getFluidState();var topFluid=top.getFluidState();level.setBlock(lower,bottom,3);level.setBlock(lower.above(),top,3);
         player.gameMode.changeGameModeForPlayer(creative?GameType.CREATIVE:GameType.SURVIVAL);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));
         player.gameMode.destroyBlock(lower.above(half));
         h.assertFalse(level.getBlockState(lower).is(block)||level.getBlockState(lower.above()).is(block),"native cabinet break clears both parts");
         h.assertValueEqual(level.getFluidState(lower),bottomFluid,"native break releases lower independent source/current");
         h.assertValueEqual(level.getFluidState(lower.above()),topFluid,"native break releases upper independent source/current");
         int drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area,e->e.getItem().is(block.asItem())).stream().mapToInt(e->e.getItem().getCount()).sum();
         h.assertValueEqual(drops,creative?0:1,"native wet break returns one paid cabinet in survival only");
      }
      h.succeed();
   }

   @GameTest(template="empty",timeoutTicks=220)
   public static void controls_natural_water_recedes_without_signal_or_block_loss(GameTestHelper h) {
      var level=h.getLevel(); var names=java.util.List.of("industrial_lever","industrial_button","industrial_contact_mat","industrial_red_led");
      for(int i=0;i<names.size();i++) {
         BlockPos cell=h.absolutePos(new BlockPos(3,2,2+i*3));
         for(int x=-1;x<=3;x++) {
            level.setBlock(cell.east(x).below(),Blocks.STONE.defaultBlockState(),3);
            level.setBlock(cell.east(x).north(),Blocks.BRICKS.defaultBlockState(),3);
            level.setBlock(cell.east(x).south(),Blocks.BRICKS.defaultBlockState(),3);
         }
         level.setBlock(cell.west(2),Blocks.BRICKS.defaultBlockState(),3); level.setBlock(cell.east(3),Blocks.BRICKS.defaultBlockState(),3);
         level.setBlock(cell,control(names.get(i)).defaultBlockState().setValue(ControlsBlockTypes.FACING,Direction.UP),3);
         level.setBlock(cell.west(),Blocks.WATER.defaultBlockState(),3);
      }
      h.runAfterDelay(60,()->{
         for(int i=0;i<names.size();i++) {
            BlockPos cell=h.absolutePos(new BlockPos(3,2,2+i*3)); var state=level.getBlockState(cell);
            h.assertTrue(state.is(control(names.get(i))),"arriving water preserves control "+names.get(i));
            h.assertTrue(!state.getFluidState().isSource() && state.getFluidState().getAmount()==7,"control accepts vanilla transient level7");
            h.assertTrue(((NativeFluidloggedBlock)state.getBlock()).pickupBlock(null,level,cell,state).isEmpty(),"no source bucket from control flow");
            if(i<2) {
               h.useBlock(new BlockPos(3,2,2+i*3),h.makeMockPlayer(GameType.CREATIVE));
               h.assertTrue(level.getBlockState(cell).getValue(ControlsBlockTypes.POWERED),"lever/button actuates while water flows");
               h.assertValueEqual(level.getFluidState(cell),state.getFluidState(),"actuation preserves current");
            }
            level.setBlock(cell.west(),Blocks.AIR.defaultBlockState(),3);
         }
         h.runAfterDelay(100,()->{
            for(int i=0;i<names.size();i++) {
               BlockPos cell=h.absolutePos(new BlockPos(3,2,2+i*3));
               h.assertTrue(level.getBlockState(cell).is(control(names.get(i))) && level.getFluidState(cell).isEmpty(),"recession preserves dry control");
               if(i<2)h.assertValueEqual(level.getBlockState(cell).getValue(ControlsBlockTypes.POWERED),i==0,"lever latches and button pulse still resets during recession");
            }
            h.succeed();
         });
      });
   }
}
