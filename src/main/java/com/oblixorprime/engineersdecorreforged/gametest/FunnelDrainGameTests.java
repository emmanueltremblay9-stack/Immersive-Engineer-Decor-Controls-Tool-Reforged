package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.utility.FlowingFluidIntake;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FunnelDrainGameTests {
   private static final BlockPos CENTER=new BlockPos(3,3,3);
   private static MachineBlockEntity place(GameTestHelper h,BlockPos pos) {
      h.getLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),3);
      h.getLevel().setBlock(pos,ModBlocks.SMALL_FLUID_FUNNEL.get().defaultBlockState(),3);
      return (MachineBlockEntity)h.getLevel().getBlockEntity(pos);
   }
   private static FluidStack marked(int marker,int amount) {
      var fluid=new FluidStack(Fluids.WATER,amount);var data=new CompoundTag();data.putInt("test_marker",marker);
      fluid.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return fluid;
   }

   @GameTest(template="empty")
   public static void funnel_stack_components_single_type_simulation_and_nbt_round_trip(GameTestHelper h) {
      var pos=h.absolutePos(CENTER);var machine=place(h,pos);var tank=machine.fluidHandler(null);
      h.assertValueEqual(tank.fill(marked(7,1000),FluidAction.SIMULATE),1000,"component stack simulation accepted");
      h.assertTrue(tank.getFluidInTank(0).isEmpty(),"simulation does not fill");
      h.assertValueEqual(tank.fill(marked(7,1000),FluidAction.EXECUTE),1000,"marked fluid accepted");
      h.assertValueEqual(tank.fill(marked(8,1000),FluidAction.EXECUTE),0,"same fluid with different components cannot mix");
      h.assertValueEqual(tank.fill(new FluidStack(Fluids.LAVA,1000),FluidAction.EXECUTE),0,"other fluid cannot mix");
      var saved=machine.saveWithFullMetadata(h.getLevel().registryAccess());machine=place(h,pos);machine.loadWithComponents(saved,h.getLevel().registryAccess());
      tank=machine.fluidHandler(null);var drained=tank.drain(marked(7,1000),FluidAction.EXECUTE);
      h.assertTrue(FluidStack.isSameFluidSameComponents(drained,marked(7,1000)),"component stack retained through BE reconstruction");
      h.assertValueEqual(drained.getAmount(),1000,"saved amount conserved");
      h.assertValueEqual(tank.fill(new FluidStack(Fluids.LAVA,1000),FluidAction.EXECUTE),1000,"fluid type can change only after emptying");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void funnel_legacy_tank_migrates_and_unknown_format_is_preserved_locked(GameTestHelper h) {
      var pos=h.absolutePos(CENTER);var machine=place(h,pos);var saved=machine.saveWithFullMetadata(h.getLevel().registryAccess());
      saved.remove("FunnelFluid");saved.putInt("FluidAmount",1750);saved.putString("FluidType","lava");
      machine.loadWithComponents(saved,h.getLevel().registryAccess());
      h.assertValueEqual(machine.fluidHandler(null).getFluidInTank(0).getAmount(),1750,"legacy amount migrates without a bucket rounding loss");
      h.assertTrue(machine.fluidHandler(null).getFluidInTank(0).is(Fluids.LAVA),"legacy type migrates");
      var future=new CompoundTag();future.putInt("Version",99);future.putString("FutureFluid","do-not-discard");
      saved.put("FunnelFluid",future.copy());machine.loadWithComponents(saved,h.getLevel().registryAccess());
      h.assertTrue(machine.fluidHandler(Direction.DOWN)==null,"unknown format refuses automation");
      h.assertValueEqual(machine.saveWithFullMetadata(h.getLevel().registryAccess()).getCompound("FunnelFluid"),future,"unknown future data preserved exactly");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void funnel_search_top_current_nearest_source_and_stable_tie_break(GameTestHelper h) {
      var level=h.getLevel();var opening=h.absolutePos(CENTER);
      for(var p:BlockPos.betweenClosed(opening.offset(-2,-1,-2),opening.offset(2,1,2)))level.setBlock(p,Blocks.AIR.defaultBlockState(),3);
      level.setBlock(opening,Fluids.FLOWING_WATER.getFlowing(4,false).createLegacyBlock(),3);
      level.setBlock(opening.east(),Blocks.WATER.defaultBlockState(),3);level.setBlock(opening.west(),Blocks.WATER.defaultBlockState(),3);
      var result=FlowingFluidIntake.search(level,opening,8);
      h.assertValueEqual(FlowingFluidIntake.maximumVisitedCells(8),833,"eight-edge search has an explicit finite cell bound");
      h.assertValueEqual(FlowingFluidIntake.maximumVisitedCells(16),6017,"sixteen edges has a larger explicit bound, not an unbounded scan");
      h.assertTrue(result.connectedCellsVisited()<=833,"actual connected fluid visits respect the selected fixture bound");
      h.assertFalse(result.incomplete(),"fixture is fully loaded");h.assertTrue(result.source()!=null,"connected source found");
      h.assertValueEqual(result.source().pos(),opening.west(),"equal shortest paths choose ascending XYZ coordinates");
      h.assertValueEqual(result.source().distance(),1,"distance counts connected fluid edges from the opening");
      h.assertTrue(FlowingFluidIntake.canDrain(level,result.source()),"vanilla source adapter qualified");
      h.assertTrue(FlowingFluidIntake.drain(level,result.source()),"source is actually picked up");
      h.assertTrue(level.getFluidState(opening.west()).isEmpty(),"consumed source disappears");
      h.assertTrue(level.getFluidState(opening.east()).isSource(),"unselected source preserved");
      h.assertFalse(FlowingFluidIntake.drain(level,result.source()),"stale snapshot cannot drain twice");
      level.setBlock(opening,Blocks.WATER.defaultBlockState(),3);
      h.assertTrue(FlowingFluidIntake.search(level,opening,8).source()==null,"a source directly above is not a moving-fluid inlet");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void funnel_drain_adapters_preserve_waterlogged_blocks_and_qualified_ie_sources(GameTestHelper h) {
      var level=h.getLevel();var opening=h.absolutePos(CENTER);
      for(var fluid:List.of(Fluids.WATER,Fluids.LAVA,BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("immersiveengineering","creosote")))) {
         h.assertTrue(fluid instanceof FlowingFluid,"test fluid has a real world current");
         for(var p:BlockPos.betweenClosed(opening.offset(-2,-1,-2),opening.offset(2,1,2)))level.setBlock(p,Blocks.AIR.defaultBlockState(),3);
         level.setBlock(opening,((FlowingFluid)fluid).getFlowing(4,false).createLegacyBlock(),3);
         level.setBlock(opening.east(),fluid.defaultFluidState().createLegacyBlock(),3);
         var source=FlowingFluidIntake.search(level,opening,8).source();h.assertTrue(source!=null,"world source found");
         h.assertTrue(FlowingFluidIntake.drain(level,source),"standard liquid pickup works for "+BuiltInRegistries.FLUID.getKey(fluid));
         h.assertTrue(level.getFluidState(opening.east()).isEmpty(),"source removal is real");
      }
      level.setBlock(opening,Fluids.FLOWING_WATER.getFlowing(4,false).createLegacyBlock(),3);
      level.setBlock(opening.east(),Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,true),3);
      var source=FlowingFluidIntake.search(level,opening,8).source();h.assertTrue(source!=null,"open waterlogged slab face connects");
      h.assertTrue(FlowingFluidIntake.drain(level,source),"waterlogged source drained through native bucket API");
      h.assertTrue(level.getBlockState(opening.east()).is(Blocks.OAK_SLAB),"decorative block is never replaced with air");
      h.assertFalse(level.getBlockState(opening.east()).getValue(BlockStateProperties.WATERLOGGED),"only source water removed");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void funnel_reentrant_destination_cannot_mint_a_bucket_without_payment(GameTestHelper h) throws Exception {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);var machine=place(h,pos);
      var saved=machine.saveWithFullMetadata(level.registryAccess());saved.putInt("TickCounter",39);machine.loadWithComponents(saved,level.registryAccess());
      machine.fluidHandler(null).fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);
      machine.setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET));
      var destination=new FluidTank(3000) {
         boolean called;
         @Override public int fill(FluidStack resource,FluidAction action) {
            if(action.execute()&&!called){called=true;MachineBlockEntity.serverTick(level,pos,machine.getBlockState(),machine);}
            return super.fill(resource,action);
         }
      };
      try(var installed=FluidHandlerTestTargets.install(level,pos.below(),destination)) {
         MachineBlockEntity.serverTick(level,pos,machine.getBlockState(),machine);
         h.assertValueEqual(destination.getFluidAmount(),250,"actual destination receives one normal output quantum");
         h.assertValueEqual(machine.fluidHandler(null).getFluidInTank(0).getAmount(),750,"source pays exactly the transferred volume");
         h.assertTrue(machine.getItem(0).is(net.minecraft.world.item.Items.BUCKET)&&machine.getItem(0).getCount()==1,"reentrant tick leaves unpaid empty bucket in input");
         h.assertTrue(machine.getItem(1).isEmpty(),"no free filled bucket exists after reentry");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void funnel_unknown_noncompound_nbt_is_preserved_without_legacy_fallback(GameTestHelper h) {
      var pos=h.absolutePos(CENTER);var machine=place(h,pos);var saved=machine.saveWithFullMetadata(h.getLevel().registryAccess());
      saved.putString("FunnelFluid","unknown-format-do-not-discard");saved.putString("FluidType","water");saved.putInt("FluidAmount",1000);
      machine.loadWithComponents(saved,h.getLevel().registryAccess());
      h.assertTrue(machine.fluidHandler(null)==null,"present unknown format cannot silently become a legacy tank");
      h.assertValueEqual(machine.saveWithFullMetadata(h.getLevel().registryAccess()).get("FunnelFluid"),saved.get("FunnelFluid"),"raw root tag preserved exactly");
      h.assertValueEqual(machine.saveWithFullMetadata(h.getLevel().registryAccess()).getInt("FluidAmount"),1000,"legacy amount alongside an unknown payload is not overwritten");
      h.assertValueEqual(machine.saveWithFullMetadata(h.getLevel().registryAccess()).getString("FluidType"),"water","legacy type alongside an unknown payload is preserved");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void funnel_native_transient_source_pickup_keeps_the_catwalk(GameTestHelper h) {
      var level=h.getLevel();var opening=h.absolutePos(CENTER);
      for(var p:BlockPos.betweenClosed(opening.offset(-2,-1,-2),opening.offset(2,1,2)))level.setBlock(p,Blocks.AIR.defaultBlockState(),3);
      level.setBlock(opening,Fluids.FLOWING_WATER.getFlowing(4,false).createLegacyBlock(),3);
      var wet=ModBlocks.STEEL_CATWALK.get().defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,true);
      level.setBlock(opening.east(),wet,3);
      var source=FlowingFluidIntake.search(level,opening,8).source();h.assertTrue(source!=null,"open native catwalk fluid connects");
      h.assertTrue(FlowingFluidIntake.canDrain(level,source),"known native transient pickup must qualify");
      h.assertTrue(FlowingFluidIntake.drain(level,source),"native source removed");
      h.assertTrue(level.getBlockState(opening.east()).is(ModBlocks.STEEL_CATWALK.get()),"independent catwalk remains");
      h.assertTrue(level.getFluidState(opening.east()).isEmpty(),"only source fluid removed");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void funnel_native_delegating_stair_slab_and_wall_sources_remain_drainable(GameTestHelper h) {
      var level=h.getLevel();var opening=h.absolutePos(CENTER);
      for(var block:List.of(ModBlocks.CLINKER_BRICK_STAIRS.get(),ModBlocks.CLINKER_BRICK_SLAB.get(),ModBlocks.CLINKER_BRICK_WALL.get())) {
         for(var p:BlockPos.betweenClosed(opening.offset(-2,-1,-2),opening.offset(2,1,2)))level.setBlock(p,Blocks.AIR.defaultBlockState(),3);
         level.setBlock(opening,Fluids.FLOWING_WATER.getFlowing(4,false).createLegacyBlock(),3);
         var wet=block.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,true);
         if(wet.hasProperty(BlockStateProperties.HORIZONTAL_FACING))wet=wet.setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.EAST);
         level.setBlock(opening.east(),wet,3);
         var source=FlowingFluidIntake.search(level,opening,8).source();h.assertTrue(source!=null,"an open face connects: "+block);
         h.assertTrue(FlowingFluidIntake.canDrain(level,source),"verified delegating native owner accepted: "+block);
         h.assertTrue(FlowingFluidIntake.drain(level,source),"source pickup succeeds: "+block);
         h.assertTrue(level.getBlockState(opening.east()).is(block),"native decorative block remains");
         h.assertTrue(level.getFluidState(opening.east()).isEmpty(),"source alone is consumed");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void funnel_search_and_controlled_valve_do_not_load_an_unavailable_neighbour(GameTestHelper h) {
      try(var fixture=new LoadedChunkBoundaryFixture(h)) {
         var level=fixture.level;var opening=fixture.pos;
         fixture.set(opening,Fluids.FLOWING_WATER.getFlowing(4,false).createLegacyBlock());
         fixture.set(opening.west(),Blocks.WATER.defaultBlockState());
         var valve=opening.south(2);
         fixture.set(valve,ModBlocks.STRAIGHT_PIPE_VALVE_REDSTONE.get().defaultBlockState().setValue(com.oblixorprime.engineersdecorreforged.utility.MachineBlocks.FACING,Direction.NORTH));
         h.assertValueEqual(level.getChunkSource().getLoadedChunksCount(),fixture.loadedBefore,"boundary fixture itself must not load chunks");
         var result=FlowingFluidIntake.search(level,opening,8);
         h.assertTrue(result.incomplete()&&result.source()==null,"unknown nearer/equal candidate suspends collection");
         var handler=level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,valve,Direction.SOUTH);
         h.assertTrue(handler!=null,"known valve input handler exists");
         h.assertValueEqual(handler.getTankCapacity(0),0,"controlled valve closes when signal neighbourhood is incomplete");
         h.assertValueEqual(handler.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE),0,"unavailable signal neighbourhood cannot forward");
         h.assertTrue(level.getFluidState(opening.west()).isSource(),"suspended search preserves the known source");
         h.assertFalse(level.hasChunkAt(opening.east()),"unavailable neighbour remains unavailable");
         h.assertValueEqual(level.getChunkSource().getLoadedChunksCount(),fixture.loadedBefore,"search and signal reads load no additional chunks");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void funnel_manual_bucket_cannot_discard_fluid_components(GameTestHelper h) {
      var pos=h.absolutePos(CENTER);var machine=place(h,pos);var expected=marked(7,1000);
      machine.fluidHandler(null).fill(expected,FluidAction.EXECUTE);
      var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
      player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET));
      h.assertFalse(machine.handleItemUse(player.getMainHandItem(),player,net.minecraft.world.InteractionHand.MAIN_HAND),"plain bucket refuses a lossy fluid conversion");
      h.assertTrue(player.getMainHandItem().is(net.minecraft.world.item.Items.BUCKET),"unrepresentable fluid does not consume the bucket");
      var actual=machine.fluidHandler(null).getFluidInTank(0);
      h.assertTrue(actual.getAmount()==1000&&FluidStack.isSameFluidSameComponents(actual,expected),"all fluid and components remain in the tank");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void funnel_graph_respects_closed_faces_and_connected_path_distance(GameTestHelper h) {
      var level=h.getLevel();var opening=h.absolutePos(CENTER);
      for(var p:BlockPos.betweenClosed(opening.offset(-2,-1,-2),opening.offset(3,1,2)))level.setBlock(p,Blocks.AIR.defaultBlockState(),3);
      var flowing=Fluids.FLOWING_WATER.getFlowing(4,false).createLegacyBlock();level.setBlock(opening,flowing,3);
      level.setBlock(opening.east(),ModBlocks.CLINKER_BRICK_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,true),3);
      h.assertTrue(FlowingFluidIntake.search(level,opening,8).source()==null,"a source inside a full closed cube is not reachable through its wall");
      for(var p:List.of(opening.north(),opening.north().east(),opening.north().east(2)))level.setBlock(p,flowing,3);
      level.setBlock(opening.east(2),Blocks.WATER.defaultBlockState(),3);
      h.assertTrue(FlowingFluidIntake.search(level,opening,3).source()==null,"geometric distance two does not bypass four connected fluid edges");
      var source=FlowingFluidIntake.search(level,opening,4).source();
      h.assertTrue(source!=null&&source.pos().equals(opening.east(2))&&source.distance()==4,"nearest reachable source is found around the obstacle");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void funnel_owned_attachment_source_drains_without_replacing_wood(GameTestHelper h) {
      var level=h.getLevel();var opening=h.absolutePos(CENTER);
      for(var p:BlockPos.betweenClosed(opening.offset(-2,-1,-2),opening.offset(2,1,2)))level.setBlock(p,Blocks.AIR.defaultBlockState(),3);
      var fluid=(FlowingFluid)BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("immersiveengineering","creosote"));
      level.setBlock(opening,fluid.getFlowing(4,false).createLegacyBlock(),3);
      level.setBlock(opening.east(),ModBlocks.TREATED_WOOD_BEAM.get().defaultBlockState(),3);
      com.oblixorprime.engineersdecorreforged.fluid.NativeFluidCells.set(level,opening.east(),fluid.getSource(false));
      h.assertTrue(com.oblixorprime.engineersdecorreforged.fluid.NativeFluidCells.active(level,opening.east()),"fixture uses the owned attachment path");
      var source=FlowingFluidIntake.search(level,opening,8).source();h.assertTrue(source!=null,"position-aware read sees attachment source");
      h.assertTrue(FlowingFluidIntake.drain(level,source),"qualified owned source cell is removed");
      h.assertTrue(level.getBlockState(opening.east()).is(ModBlocks.TREATED_WOOD_BEAM.get()),"wood block remains independent");
      h.assertTrue(level.getFluidState(opening.east()).isEmpty(),"owned exterior source alone is removed");
      h.succeed();
   }

   // Transaction prototype coverage only. The selected range and real 40-tick scheduler are not yet wired.
   private static boolean collect(MachineBlockEntity machine,int fixtureRange) throws Exception {
      var method=MachineBlockEntity.class.getDeclaredMethod("collectFlowingSource",int.class);
      method.setAccessible(true);return (boolean)method.invoke(machine,fixtureRange);
   }
   private static void clearWaterFixture(GameTestHelper h,BlockPos pos) {
      for(var p:BlockPos.betweenClosed(pos.offset(-2,0,-2),pos.offset(3,3,3)))h.getLevel().setBlock(p,Blocks.AIR.defaultBlockState(),3);
   }

   @GameTest(template="empty")
   public static void funnel_transaction_pays_source_before_any_extractible_credit_and_reloads_work(GameTestHelper h) throws Exception {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);clearWaterFixture(h,pos);var machine=place(h,pos);
      var opening=pos.above();level.setBlock(opening,Fluids.FLOWING_WATER.getFlowing(4,false).createLegacyBlock(),3);
      level.setBlock(opening.east(),Blocks.WATER.defaultBlockState(),3);
      for(int i=0;i<3;i++){h.assertTrue(collect(machine,8),"valid source advances virtual work");h.assertTrue(machine.fluidHandler(null).getFluidInTank(0).isEmpty(),"work is not extractible unpaid fluid");}
      h.assertTrue(level.getFluidState(opening.east()).isSource(),"source remains before the full transaction");
      var saved=machine.saveWithFullMetadata(level.registryAccess());
      h.assertValueEqual(saved.getCompound("FunnelFluid").getCompound("Pending").getInt("Progress"),750,"only virtual progress is saved");
      machine=place(h,pos);machine.loadWithComponents(saved,level.registryAccess());
      h.assertTrue(collect(machine,8),"reloaded work is revalidated before payment");
      h.assertTrue(level.getFluidState(opening.east()).isEmpty(),"one complete source really disappears");
      h.assertValueEqual(machine.fluidHandler(null).getFluidInTank(0).getAmount(),1000,"exactly one paid bucket becomes available");
      h.assertFalse(collect(machine,8),"no source remains for another credit");
      h.assertValueEqual(machine.fluidHandler(null).getFluidInTank(0).getAmount(),1000,"no repeated or late credit");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void funnel_transaction_resets_lost_source_and_requires_full_capacity(GameTestHelper h) throws Exception {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);clearWaterFixture(h,pos);var machine=place(h,pos);
      var opening=pos.above();level.setBlock(opening,Fluids.FLOWING_WATER.getFlowing(4,false).createLegacyBlock(),3);
      level.setBlock(opening.east(),Blocks.WATER.defaultBlockState(),3);
      machine.fluidHandler(null).fill(new FluidStack(Fluids.WATER,2500),FluidAction.EXECUTE);
      for(int i=0;i<4;i++)h.assertFalse(collect(machine,8),"less than a whole bucket of capacity prevents collection");
      h.assertTrue(level.getFluidState(opening.east()).isSource(),"capacity rejection preserves source");
      machine.fluidHandler(null).drain(3000,FluidAction.EXECUTE);
      for(int i=0;i<3;i++)collect(machine,8);
      level.setBlock(opening.east(),Blocks.AIR.defaultBlockState(),3);collect(machine,8);
      h.assertTrue(machine.fluidHandler(null).getFluidInTank(0).isEmpty(),"lost source leaves no free 750 mB");
      level.setBlock(opening.west(),Blocks.WATER.defaultBlockState(),3);collect(machine,8);
      h.assertTrue(level.getFluidState(opening.west()).isSource()&&machine.fluidHandler(null).getFluidInTank(0).isEmpty(),"replacement source starts fresh work");
      for(int i=0;i<3;i++)collect(machine,8);
      h.assertTrue(level.getFluidState(opening.west()).isEmpty(),"replacement source is paid only after its own full work");
      h.assertValueEqual(machine.fluidHandler(null).getFluidInTank(0).getAmount(),1000,"replacement produces exactly its paid volume");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void two_funnel_transactions_cannot_credit_the_same_world_source(GameTestHelper h) throws Exception {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);clearWaterFixture(h,pos);
      var first=place(h,pos);var second=place(h,pos.south(2));var flowing=Fluids.FLOWING_WATER.getFlowing(4,false).createLegacyBlock();
      for(var p:List.of(pos.above(),pos.above().south(),pos.above().south(2)))level.setBlock(p,flowing,3);
      var source=pos.above().south().east();level.setBlock(source,Blocks.WATER.defaultBlockState(),3);
      for(int i=0;i<3;i++){collect(first,8);collect(second,8);}
      collect(first,8);collect(second,8);
      int total=first.fluidHandler(null).getFluidInTank(0).getAmount()+second.fluidHandler(null).getFluidInTank(0).getAmount();
      h.assertValueEqual(total,1000,"server-serial collectors can pay a world source only once");
      h.assertTrue(level.getFluidState(source).isEmpty(),"shared source was actually consumed");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void funnel_refused_source_removal_never_credits_a_returned_bucket(GameTestHelper h) throws Exception {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);clearWaterFixture(h,pos);var machine=place(h,pos);
      var opening=pos.above();var source=opening.east();
      level.setBlock(opening,Fluids.FLOWING_WATER.getFlowing(4,false).createLegacyBlock(),18);
      level.setBlock(source,Blocks.WATER.defaultBlockState(),18);
      boolean[] restored={false};
      java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.NeighborNotifyEvent> listener=event->{
         if(event.getLevel()==level&&event.getPos().equals(source)&&!restored[0]&&level.getFluidState(source).isEmpty()) {
            restored[0]=true;level.setBlock(source,Blocks.WATER.defaultBlockState(),18);
         }
      };
      net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
      try {
         for(int i=0;i<4;i++)collect(machine,8);
         h.assertTrue(restored[0],"real neighbor notification fixture restored the removed vanilla source");
         h.assertTrue(level.getFluidState(source).isSource(),"source remains after the restoring callback");
         h.assertTrue(machine.fluidHandler(null).getFluidInTank(0).isEmpty(),"a returned bucket alone is not proof of source payment");
      } finally {
         net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);
         level.setBlock(source,Blocks.AIR.defaultBlockState(),18);
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void funnel_bottom_ie_pipe_preserves_components_and_respects_full_destination(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);clearWaterFixture(h,pos);var machine=place(h,pos);
      var pipe=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("immersiveengineering","fluid_pipe"));
      var barrel=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("immersiveengineering","metal_barrel"));
      h.assertTrue(pipe!=Blocks.AIR&&barrel!=Blocks.AIR,"pinned IE pipe and barrel resolve");
      level.setBlock(pos.below(2),barrel.defaultBlockState(),3);level.setBlock(pos.below(),pipe.defaultBlockState(),3);
      var pipeEntity=(blusunrize.immersiveengineering.common.blocks.metal.FluidPipeBlockEntity)level.getBlockEntity(pos.below());
      pipeEntity.onNeighborBlockChange(pos);pipeEntity.onNeighborBlockChange(pos.below(2));
      h.assertTrue((pipeEntity.getAvailableConnectionByte()&(1<<Direction.UP.ordinal()))!=0,"IE renders its connection to the Funnel's bottom");
      var saved=machine.saveWithFullMetadata(level.registryAccess());saved.putInt("TickCounter",39);machine.loadWithComponents(saved,level.registryAccess());
      var fluid=BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("immersiveengineering","creosote"));
      var offered=new FluidStack(fluid,1250);var data=new CompoundTag();data.putString("pipe_marker","preserve-generic-components");offered.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
      h.assertValueEqual(machine.fluidHandler(null).fill(offered,FluidAction.EXECUTE),1250,"compatible generic fluid enters the Funnel tank");
      var target=level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,pos.below(2),Direction.UP);
      h.assertTrue(target!=null,"actual IE barrel destination exposes its top capability");
      MachineBlockEntity.serverTick(level,pos,machine.getBlockState(),machine);
      var received=target.getFluidInTank(0);h.assertTrue(received.getAmount()>0&&received.getAmount()<=250,"real pipe acceptance is bounded by the machine offer");
      h.assertTrue(FluidStack.isSameFluidSameComponents(received,offered),"generic components arrive through IE unchanged");
      int left=machine.fluidHandler(null).getFluidInTank(0).getAmount();h.assertValueEqual(left+received.getAmount(),1250,"partial pipe acceptance is paid exactly once");
      int capacity=target.getTankCapacity(0),alreadyReceived=received.getAmount();
      h.assertValueEqual(target.fill(offered.copyWithAmount(capacity),FluidAction.EXECUTE),capacity-alreadyReceived,"fixture fills the actual destination completely");
      for(int i=0;i<40;i++)MachineBlockEntity.serverTick(level,pos,machine.getBlockState(),machine);
      h.assertValueEqual(machine.fluidHandler(null).getFluidInTank(0).getAmount(),left,"full IE destination does not consume or void Funnel fluid");
      h.succeed();
   }
}
