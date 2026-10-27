package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real machine server-tick entry point, not reflective transaction calls. */
@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FunnelRuntimeGameTests {
   @GameTest(template="empty")
   public static void funnel_runtime_16_edge_source_pays_only_at_tick_160(GameTestHelper h) {
      try(var f=new Fixture(h)) {
         var source=f.column(16);var machine=f.machine(f.pos);
         f.tick(machine,39);h.assertValueEqual(f.amount(machine),0,"no work before forty ticks");
         f.tick(machine,81);h.assertValueEqual(f.amount(machine),0,"three work quanta are not unpaid tank fluid");
         h.assertTrue(f.level.getFluidState(source).isSource(),"source survives the first120ticks");
         f.tick(machine,39);h.assertValueEqual(f.amount(machine),0,"no early source credit at tick159");
         f.tick(machine,1);h.assertValueEqual(f.amount(machine),1000,"four250mB quanta pay one bucket at160ticks");
         h.assertFalse(f.level.getFluidState(source).isSource(),"source at path distance16 is really consumed");
         f.tick(machine,160);h.assertValueEqual(f.amount(machine),1000,"remaining flow cannot mint another bucket");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void funnel_runtime_17_edge_or_missing_source_never_produces_fluid(GameTestHelper h) {
      try(var f=new Fixture(h)) {
         var source=f.column(17);var machine=f.machine(f.pos);
         f.tick(machine,400);h.assertValueEqual(f.amount(machine),0,"source beyond16connectededges gives no credit");
         h.assertTrue(f.level.getFluidState(source).isSource(),"out-of-range source remains untouched");
         f.set(source,Blocks.AIR.defaultBlockState());
         f.tick(machine,400);h.assertValueEqual(f.amount(machine),0,"long source-free waterfall gives no free fluid");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void funnel_runtime_requires_top_current_not_side_or_disconnected_source(GameTestHelper h) {
      try(var f=new Fixture(h)) {
         var machine=f.machine(f.pos);
         f.set(f.pos.east(),f.current());f.set(f.pos.east().above(),Blocks.WATER.defaultBlockState());
         f.tick(machine,160);h.assertValueEqual(f.amount(machine),0,"side contact is not an inlet");
         f.set(f.pos.east(),Blocks.AIR.defaultBlockState());f.set(f.pos.east().above(),Blocks.AIR.defaultBlockState());
         f.set(f.pos.above(),f.current());f.set(f.pos.above().west(2),Blocks.WATER.defaultBlockState());
         f.tick(machine,160);h.assertValueEqual(f.amount(machine),0,"nearby source across a dry gap cannot be aspirated");
         f.set(f.pos.above(),Blocks.WATER.defaultBlockState());
         f.tick(machine,160);h.assertValueEqual(f.amount(machine),0,"a source directly on top is not a moving-fluid inlet");
         h.assertTrue(f.level.getFluidState(f.pos.above()).isSource(),"rejected direct source is preserved");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void funnel_runtime_pending_work_and_tick_phase_survive_be_reload(GameTestHelper h) {
      try(var f=new Fixture(h)) {
         var source=f.column(3);var machine=f.machine(f.pos);f.tick(machine,120);
         var saved=machine.saveWithFullMetadata(f.level.registryAccess());
         h.assertValueEqual(saved.getCompound("FunnelFluid").getCompound("Pending").getInt("Progress"),750,"saved work is virtual750mB");
         machine=f.machine(f.pos);machine.loadWithComponents(saved,f.level.registryAccess());
         f.tick(machine,39);h.assertValueEqual(f.amount(machine),0,"reloaded tick phase cannot collect early");
         h.assertTrue(f.level.getFluidState(source).isSource(),"source remains before the paid fourth quantum");
         f.tick(machine,1);h.assertValueEqual(f.amount(machine),1000,"reloaded fourth quantum pays exactly one source");
         h.assertFalse(f.level.getFluidState(source).isSource(),"source removal survives BE reconstruction boundary");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void funnel_runtime_source_loss_resets_work_and_replacement_starts_fresh(GameTestHelper h) {
      try(var f=new Fixture(h)) {
         var source=f.column(3);var machine=f.machine(f.pos);f.tick(machine,120);
         f.set(source,Blocks.AIR.defaultBlockState());f.tick(machine,40);
         h.assertValueEqual(f.amount(machine),0,"lost source cannot turn saved progress into fluid");
         var replacement=f.pos.above().east();f.set(replacement,Blocks.WATER.defaultBlockState());
         f.tick(machine,120);h.assertValueEqual(f.amount(machine),0,"replacement does not inherit old work");
         h.assertTrue(f.level.getFluidState(replacement).isSource(),"replacement still present before its fourth quantum");
         f.tick(machine,40);h.assertValueEqual(f.amount(machine),1000,"replacement's own full work is paid once");
         h.assertFalse(f.level.getFluidState(replacement).isSource(),"replacement is really consumed");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void funnel_runtime_full_bucket_capacity_is_required_before_source_consumption(GameTestHelper h) {
      try(var f=new Fixture(h)) {
         var source=f.column(2);var machine=f.machine(f.pos);
         machine.fluidHandler(null).fill(new FluidStack(Fluids.WATER,2500),FluidAction.EXECUTE);
         f.tick(machine,160);h.assertValueEqual(f.amount(machine),2500,"partial free space does not consume a source");
         h.assertTrue(f.level.getFluidState(source).isSource(),"capacity refusal leaves source intact");
         h.assertValueEqual(machine.fluidHandler(null).drain(1000,FluidAction.SIMULATE).getAmount(),1000,"simulation reports available stored fluid");
         h.assertValueEqual(f.amount(machine),2500,"simulation cannot make capacity or advance work");
         machine.fluidHandler(null).drain(1000,FluidAction.EXECUTE);f.tick(machine,160);
         h.assertValueEqual(f.amount(machine),2500,"paid source adds exactly1000after real space becomes available");
         h.assertFalse(f.level.getFluidState(source).isSource(),"source consumed only after complete capacity is available");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void two_runtime_funnels_cannot_collect_one_source_twice(GameTestHelper h) {
      try(var f=new Fixture(h)) {
         var other=f.pos.south(2);var first=f.machine(f.pos);var second=f.machine(other);
         for(var p:new BlockPos[]{f.pos.above(),f.pos.above().south(),other.above()})f.set(p,f.current());
         var source=f.pos.above().south().east();f.set(source,Blocks.WATER.defaultBlockState());
         for(int i=0;i<160;i++){f.tick(first,1);f.tick(second,1);}
         h.assertValueEqual(f.amount(first)+f.amount(second),1000,"server-serial runtime consumers pay shared source only once");
         h.assertFalse(f.level.getFluidState(source).isSource(),"shared source is actually removed");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void funnel_runtime_qualified_lava_and_ie_creosote_are_paid_without_mixing(GameTestHelper h) {
      for(var fluid:java.util.List.of(Fluids.LAVA,net.minecraft.core.registries.BuiltInRegistries.FLUID.get(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("immersiveengineering","creosote")))) {
         try(var f=new Fixture(h)) {
            var machine=f.machine(f.pos);var opening=f.pos.above();
            f.set(opening,((net.minecraft.world.level.material.FlowingFluid)fluid).getFlowing(8,true).createLegacyBlock());
            var source=opening.above();f.set(source,fluid.defaultFluidState().createLegacyBlock());
            machine.fluidHandler(null).fill(new FluidStack(Fluids.WATER,500),FluidAction.EXECUTE);
            f.tick(machine,160);h.assertValueEqual(f.amount(machine),500,"incompatible stored water prevents source payment");
            h.assertTrue(f.level.getFluidState(source).isSource(),"incompatible source stays intact");
            machine.fluidHandler(null).drain(500,FluidAction.EXECUTE);f.tick(machine,160);
            h.assertValueEqual(f.amount(machine),1000,"qualified fluid is collected after the tank is empty");
            h.assertTrue(machine.fluidHandler(null).getFluidInTank(0).is(fluid),"runtime preserves actual source fluid identity");
            h.assertFalse(f.level.getFluidState(source).isSource(),"qualified modded/vanilla source really disappears");
         }
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void funnel_runtime_refuses_source_updates_next_to_an_unloaded_chunk(GameTestHelper h) {
      try(var boundary=new LoadedChunkBoundaryFixture(h)) {
         var source=boundary.pos;var opening=source.west();var pos=opening.below();
         boundary.set(pos,ModBlocks.SMALL_FLUID_FUNNEL.get().defaultBlockState());
         boundary.set(opening,Fluids.FLOWING_WATER.getFlowing(8,true).createLegacyBlock());
         boundary.set(source,Blocks.WATER.defaultBlockState());
         var machine=(MachineBlockEntity)boundary.level.getBlockEntity(pos);
         for(int i=0;i<160;i++)MachineBlockEntity.serverTick(boundary.level,pos,machine.getBlockState(),machine);
         h.assertTrue(machine.fluidHandler(null).getFluidInTank(0).isEmpty(),"no credit when safe source updates would need an absent chunk");
         h.assertTrue(boundary.level.getFluidState(source).isSource(),"border source is preserved");
         h.assertFalse(boundary.level.hasChunkAt(source.east()),"the missing chunk stays absent");
         h.assertValueEqual(boundary.level.getChunkSource().getLoadedChunksCount(),boundary.loadedBefore,"runtime performs no forced chunk loading");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void funnel_runtime_cannot_collect_through_the_closed_bottom_of_a_fluidlogged_inlet(GameTestHelper h) {
      try(var f=new Fixture(h)) {
         var machine=f.machine(f.pos);var opening=f.pos.above();
         f.set(opening,Blocks.OAK_SLAB.defaultBlockState());
         h.assertTrue(com.oblixorprime.engineersdecorreforged.fluid.NativeFluidCells.supported(f.level.getBlockState(opening)),"vanilla slab supports the existing exterior fluid attachment");
         com.oblixorprime.engineersdecorreforged.fluid.NativeFluidCells.set(f.level,opening,Fluids.FLOWING_WATER.getFlowing(4,false));
         f.set(opening.above(),Blocks.WATER.defaultBlockState());
         h.assertFalse(f.level.getFluidState(opening).isEmpty(),"fixture contains real position-aware moving fluid");
         h.assertFalse(f.level.getFluidState(opening).isSource(),"fixture is not rejected merely for being a source");
         f.tick(machine,160);
         h.assertValueEqual(f.amount(machine),0,"a full lower face separates contained current from the Funnel mouth");
         h.assertTrue(f.level.getFluidState(opening.above()).isSource(),"source above the physical barrier is not drained");
         com.oblixorprime.engineersdecorreforged.fluid.NativeFluidCells.set(f.level,opening,Fluids.EMPTY.defaultFluidState());
      }
      h.succeed();
   }

   private static final class Fixture implements AutoCloseable {
      final ServerLevel level;
      final BlockPos pos;
      final Map<BlockPos,BlockState> original=new LinkedHashMap<>();
      Fixture(GameTestHelper h){level=h.getLevel();pos=h.absolutePos(new BlockPos(3,20,3));}
      BlockState current(){return Fluids.FLOWING_WATER.getFlowing(8,true).createLegacyBlock();}
      void set(BlockPos p,BlockState state){
         if(!level.hasChunkAt(p))throw new IllegalStateException("Fixture must not load chunks");
         original.putIfAbsent(p.immutable(),level.getBlockState(p));
         level.setBlock(p,state,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
      }
      BlockPos column(int distance){
         for(int i=0;i<distance;i++)set(pos.above(1+i),current());
         var source=pos.above(1+distance);set(source,Blocks.WATER.defaultBlockState());return source;
      }
      MachineBlockEntity machine(BlockPos p){set(p,Blocks.AIR.defaultBlockState());set(p,ModBlocks.SMALL_FLUID_FUNNEL.get().defaultBlockState());return (MachineBlockEntity)level.getBlockEntity(p);}
      int amount(MachineBlockEntity m){return m.fluidHandler(null).getFluidInTank(0).getAmount();}
      void tick(MachineBlockEntity m,int count){for(int i=0;i<count;i++)MachineBlockEntity.serverTick(level,m.getBlockPos(),m.getBlockState(),m);}
      @Override public void close(){for(var e:original.entrySet())level.setBlock(e.getKey(),e.getValue(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);}
   }
}
