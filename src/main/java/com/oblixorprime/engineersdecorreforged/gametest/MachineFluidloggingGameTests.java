package com.oblixorprime.engineersdecorreforged.gametest;
import com.oblixorprime.engineersdecorreforged.*;
import com.oblixorprime.engineersdecorreforged.block.*;
import com.oblixorprime.engineersdecorreforged.utility.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MachineFluidloggingGameTests {
   @GameTest(template="empty")
   public static void machine_cell_changes_preserve_entity_inventory_and_tank(GameTestHelper h) {
      var level=h.getLevel();BlockPos pos=h.absolutePos(new BlockPos(3,2,3));int machines=0;
      for(var entry:ModBlocks.ORDERED_BLOCKS) {
         var block=entry.get();if(!(block instanceof MachineBlocks.MachineBlock))continue;machines++;
         level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);
         level.setBlock(pos,block.defaultBlockState(),3);var entity=(MachineBlockEntity)level.getBlockEntity(pos);
         if(entity.getContainerSize()>0)entity.setItem(0,new ItemStack(Items.IRON_INGOT,3));
         var tank=level.getCapability(Capabilities.FluidHandler.BLOCK,pos,Direction.UP);
         if(tank!=null)tank.fill(new FluidStack(Fluids.WATER,750),FluidAction.EXECUTE);
         var saved=entity.saveWithFullMetadata(level.registryAccess());
         for(Fluid fluid:java.util.List.of(Fluids.WATER,Fluids.LAVA,NativeFluidloggedBlock.creosote())) {
            var state=level.getBlockState(pos);var container=(NativeFluidloggedBlock)block;
            h.assertTrue(container.placeLiquid(level,pos,state,((FlowingFluid)fluid).getSource(false)),"machine accepts exterior source");
            h.assertTrue(level.getBlockEntity(pos)==entity,"cell change retains entity identity");
            h.assertValueEqual(entity.saveWithFullMetadata(level.registryAccess()),saved,"source never changes inventory/tank/entity NBT: "+entry.getId());
            var restored=net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos,level.getBlockState(pos),saved,level.registryAccess());
            h.assertTrue(restored instanceof MachineBlockEntity,"wet state reload keeps registered entity type");
            h.assertValueEqual(restored.saveWithFullMetadata(level.registryAccess()),saved,"wet state/entity NBT reload preserves machine contents");
            h.assertTrue(container.pickupBlock(null,level,pos,level.getBlockState(pos)).is(fluid.getBucket()),"one exact exterior bucket");
            h.assertValueEqual(entity.saveWithFullMetadata(level.registryAccess()),saved,"pickup never changes entity NBT");
         }
      }
      h.assertValueEqual(machines,17,"all entity machines covered, valves have no entity");h.succeed();
   }
   @GameTest(template="empty")
   public static void accumulator_normal_bucket_never_falls_back_to_exterior_cell(GameTestHelper h) {
      var level=h.getLevel();BlockPos pos=h.absolutePos(new BlockPos(3,2,3));var block=ModBlocks.PASSIVE_FLUID_ACCUMULATOR.get();
      var player=new net.neoforged.neoforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TankPriority"));
      player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);player.setPos(pos.getX()+.5,pos.getY()+2,pos.getZ()+.5);player.setXRot(90);
      for(Fluid exterior:java.util.List.of(Fluids.WATER,Fluids.LAVA,NativeFluidloggedBlock.creosote()))for(int amount:new int[]{0,750,1000,4000}) {
         level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);
         var state=NativeFluidloggedBlock.placement(block.defaultBlockState(),((FlowingFluid)exterior).getSource(false));level.setBlock(pos,state,3);
         var tank=level.getCapability(Capabilities.FluidHandler.BLOCK,pos,Direction.UP);if(amount>0)tank.fill(new FluidStack(Fluids.WATER,amount),FluidAction.EXECUTE);
         var hit=new BlockHitResult(Vec3.atCenterOf(pos).add(0,.4375,0),Direction.UP,pos,false);
         player.setShiftKeyDown(false);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));
         var used=player.gameMode.useItemOn(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
         if(!used.consumesAction())player.gameMode.useItem(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND);
         h.assertTrue(player.getMainHandItem().is(amount>=1000?Items.WATER_BUCKET:Items.BUCKET),"normal bucket targets only available tank water");
         h.assertValueEqual(tank.getFluidInTank(0).getAmount(),amount>=1000?amount-1000:amount,"normal transfer exact tank balance");
         h.assertValueEqual(level.getBlockState(pos),state,"normal tank use preserves exterior source");
         player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));
         h.assertFalse(player.gameMode.useItem(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction(),"normal bucket raycast cannot bypass tank priority to drain cell");
         h.assertValueEqual(level.getBlockState(pos),state,"failed tank fallback never drains exterior");
         player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(NativeFluidloggedBlock.creosote().getBucket()));
         used=player.gameMode.useItemOn(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
         if(!used.consumesAction())player.gameMode.useItem(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND);
         h.assertTrue(player.getMainHandItem().is(NativeFluidloggedBlock.creosote().getBucket()),"incompatible normal bucket remains paid");
         h.assertValueEqual(level.getBlockState(pos),state,"incompatible tank use cannot replace cell");
      }
      h.succeed();
   }
   @GameTest(template="empty")
   public static void accumulator_nonwater_exterior_never_generates_tank_water(GameTestHelper h) {
      var level=h.getLevel();BlockPos pos=h.absolutePos(new BlockPos(3,2,3));var block=ModBlocks.PASSIVE_FLUID_ACCUMULATOR.get();
      for(Fluid exterior:java.util.List.of(Fluids.WATER,Fluids.LAVA,NativeFluidloggedBlock.creosote()))for(int neighbor=0;neighbor<3;neighbor++) {
         level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
         for(Direction side:Direction.values())level.setBlock(pos.relative(side),Blocks.STONE.defaultBlockState(),3);
         var state=NativeFluidloggedBlock.placement(block.defaultBlockState(),((FlowingFluid)exterior).getSource(false));level.setBlock(pos,state,3);
         if(neighbor==1)level.setBlock(pos.east(),Blocks.LAVA.defaultBlockState(),3);
         if(neighbor==2)level.setBlock(pos.east(),Fluids.FLOWING_WATER.getFlowing(4,false).createLegacyBlock(),3);
         var entity=(MachineBlockEntity)level.getBlockEntity(pos);
         for(int tick=0;tick<160;tick++)MachineBlockEntity.serverTick(level,pos,level.getBlockState(pos),entity);
         var tank=level.getCapability(Capabilities.FluidHandler.BLOCK,pos,Direction.UP);
         h.assertValueEqual(tank.getTankCapacity(0),4000,"capacity stays4000");
         h.assertValueEqual(tank.getFluidInTank(0).getAmount(),0,"own cell, lava or current cannot create tank water");
      }
      h.succeed();
   }
   @GameTest(template="empty")
   public static void machine_visual_ticks_retain_sources_and_transient_cells(GameTestHelper h) {
      var level=h.getLevel();BlockPos pos=h.absolutePos(new BlockPos(3,2,3));
      for(var entry:ModBlocks.ORDERED_BLOCKS) {
         var block=entry.get();if(!(block instanceof MachineBlocks.MachineBlock))continue;
         for(int cell=0;cell<4;cell++) {
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
            for(Direction side:Direction.values())level.setBlock(pos.relative(side),Blocks.STONE.defaultBlockState(),3);
            var state=block.defaultBlockState();state=cell==3?StairWaterFlow.withFlow(state,Fluids.FLOWING_WATER.getFlowing(5,false)):
               NativeFluidloggedBlock.placement(state,((FlowingFluid)java.util.List.of(Fluids.WATER,Fluids.LAVA,NativeFluidloggedBlock.creosote()).get(cell)).getSource(false));
            level.setBlock(pos,state,3);var fluid=state.getFluidState();var entity=(MachineBlockEntity)level.getBlockEntity(pos);
            for(int tick=0;tick<80;tick++)MachineBlockEntity.serverTick(level,pos,level.getBlockState(pos),entity);
            h.assertTrue(level.getBlockEntity(pos)==entity,"registered machine ticking retains identity");
            h.assertValueEqual(level.getFluidState(pos),fluid,"visual phase/level/activity updates retain exact cell: "+entry.getId());
         }
      }
      h.succeed();
   }
}
