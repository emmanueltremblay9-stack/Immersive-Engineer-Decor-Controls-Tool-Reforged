package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlocks;
import com.oblixorprime.engineersdecorreforged.utility.PipeValveBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FluidRoutingGameTests {
   private static final BlockPos CENTER = new BlockPos(3, 3, 3);
   private static final String IE = "immersiveengineering";
   private static ResourceLocation ie(String path) { return ResourceLocation.fromNamespaceAndPath(IE, path); }
   private static IFluidHandler tank(GameTestHelper h, BlockPos pos, Direction side) {
      return h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
   }
   private static void ticks(GameTestHelper h, MachineBlockEntity machine, int ticks) {
      for (int i=0;i<ticks;i++) MachineBlockEntity.serverTick(h.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
   }
   private static void clear(GameTestHelper h, BlockPos pos) {
      h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
      for (Direction d:Direction.values()) h.getLevel().setBlock(pos.relative(d), Blocks.AIR.defaultBlockState(), 3);
   }

   @GameTest(template="empty")
   public static void fluid_barrel_32000_axial_capabilities_and_simulation(GameTestHelper h) {
      BlockPos pos=h.absolutePos(CENTER);h.getLevel().setBlock(pos,ModBlocks.FLUID_BARREL.get().defaultBlockState(),3);
      for(Direction side:Direction.Plane.HORIZONTAL) h.assertTrue(tank(h,pos,side)==null,"barrel has no lateral capability: "+side);
      for(Direction side:new Direction[]{Direction.UP,Direction.DOWN,null}) {
         IFluidHandler handler=tank(h,pos,side);h.assertTrue(handler!=null,"top/bottom/unsided preserved");
         h.assertValueEqual(handler.getTankCapacity(0),32000,"server capacity is 32 buckets");
         h.assertValueEqual(handler.fill(new FluidStack(Fluids.WATER,40000),FluidAction.SIMULATE),32000,"simulation clamps to capacity");
         h.assertTrue(handler.getFluidInTank(0).isEmpty(),"simulation does not mutate");
      }
      IFluidHandler handler=tank(h,pos,Direction.UP);
      h.assertValueEqual(handler.fill(new FluidStack(Fluids.WATER,40000),FluidAction.EXECUTE),32000,"actual fill clamps");
      h.assertValueEqual(tank(h,pos,Direction.DOWN).drain(1000,FluidAction.SIMULATE).getAmount(),1000,"bottom can drain");
      h.assertValueEqual(handler.getFluidInTank(0).getAmount(),32000,"drain simulation preserves volume");
      h.assertValueEqual(tank(h,pos,Direction.DOWN).drain(1000,FluidAction.EXECUTE).getAmount(),1000,"bottom drains actually");
      h.assertValueEqual(tank(h,pos,Direction.DOWN).fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE),1000,"bottom also accepts fluid");
      h.assertValueEqual(handler.fill(new FluidStack(Fluids.LAVA,1000),FluidAction.EXECUTE),0,"no mixed fluids");
      h.assertValueEqual(h.getLevel().getBlockState(pos).getValue(MachineBlocks.LEVEL_0_4),4,"full world gauge");
      h.assertValueEqual(h.getLevel().getBlockState(pos).getAnalogOutputSignal(h.getLevel(),pos),15,"full comparator");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void fluid_barrel_legacy_and_new_nbt_keep_volume_and_auto_output_rate(GameTestHelper h) {
      var level=h.getLevel();BlockPos pos=h.absolutePos(CENTER);
      level.setBlock(pos,ModBlocks.FLUID_BARREL.get().defaultBlockState().setValue(MachineBlocks.FACING,Direction.UP),3);
      var machine=(MachineBlockEntity)level.getBlockEntity(pos);var handler=tank(h,pos,null);
      handler.fill(new FluidStack(Fluids.WATER,32000),FluidAction.EXECUTE);
      var saved=machine.saveWithFullMetadata(level.registryAccess());
      level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);level.setBlock(pos,ModBlocks.FLUID_BARREL.get().defaultBlockState().setValue(MachineBlocks.FACING,Direction.UP),3);
      machine=(MachineBlockEntity)level.getBlockEntity(pos);machine.loadWithComponents(saved,level.registryAccess());
      h.assertValueEqual(tank(h,pos,null).getFluidInTank(0).getAmount(),32000,"new capacity survives BE reconstruction");
      saved.putInt("FluidAmount",3750);saved.putString("FluidType","water");machine.loadWithComponents(saved,level.registryAccess());
      h.assertValueEqual(tank(h,pos,null).getFluidInTank(0).getAmount(),3750,"legacy amount is retained exactly");
      level.setBlock(pos.below(),ModBlocks.PASSIVE_FLUID_ACCUMULATOR.get().defaultBlockState(),3);
      ticks(h,machine,39);h.assertTrue(tank(h,pos.below(),Direction.UP).getFluidInTank(0).isEmpty(),"no early automatic transfer");
      h.assertValueEqual(level.getBlockState(pos).getValue(MachineBlocks.LEVEL_0_4),1,"legacy volume uses the new 32-bucket gauge on subsequent ticks");
      h.assertValueEqual(level.getBlockState(pos).getAnalogOutputSignal(level,pos),2,"comparator agrees with the new capacity at intermediate fill");
      ticks(h,machine,1);h.assertValueEqual(tank(h,pos.below(),Direction.UP).getFluidInTank(0).getAmount(),250,"existing 250 per 40 ticks retained");
      h.assertValueEqual(tank(h,pos,null).getFluidInTank(0).getAmount(),3500,"transfer conserves water");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void fluid_barrel_recipe_requires_ie_barrel_and_all_eight_steel_plates(GameTestHelper h) {
      var plate=BuiltInRegistries.ITEM.get(ie("plate_steel"));var barrel=BuiltInRegistries.ITEM.get(ie("metal_barrel"));
      h.assertTrue(plate!=Items.AIR&&barrel!=Items.AIR,"pinned IE items resolve");
      var id=ResourceLocation.fromNamespaceAndPath(EngineersDecorReforged.MOD_ID,"fluid_barrel");
      var recipe=h.getLevel().getRecipeManager().byKey(id).orElseThrow().value();
      var slots=new ArrayList<ItemStack>();for(int i=0;i<9;i++)slots.add(new ItemStack(i==4?barrel:plate));
      var input=CraftingInput.of(3,3,slots);var selected=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,h.getLevel());
      h.assertTrue(selected.isPresent()&&selected.get().id().equals(id),"exact IE barrel recipe selected");
      var output=selected.orElseThrow().value().assemble(input,h.getLevel().registryAccess());
      h.assertTrue(output.is(ModBlocks.FLUID_BARREL.get().asItem())&&output.getCount()==1,"exactly one fluid barrel");
      slots.set(4,new ItemStack(Items.BARREL));h.assertFalse(((net.minecraft.world.item.crafting.CraftingRecipe)recipe).matches(CraftingInput.of(3,3,slots),h.getLevel()),"wooden barrel no longer valid");
      for(int i=0;i<9;i++){var missing=new ArrayList<ItemStack>();for(int j=0;j<9;j++)missing.add(j==i?ItemStack.EMPTY:new ItemStack(j==4?barrel:plate));
         h.assertFalse(((net.minecraft.world.item.crafting.CraftingRecipe)recipe).matches(CraftingInput.of(3,3,missing),h.getLevel()),"all nine ingredients required: "+i);}
      h.succeed();
   }

   @GameTest(template="empty")
   public static void valves_forward_only_to_facing_all_six_orientations_without_storage(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);
      for(Direction output:Direction.values()) {
         clear(h,pos);level.setBlock(pos.relative(output),ModBlocks.PASSIVE_FLUID_ACCUMULATOR.get().defaultBlockState(),3);
         level.setBlock(pos,ModBlocks.STRAIGHT_PIPE_VALVE.get().defaultBlockState().setValue(MachineBlocks.FACING,output),3);
         for(Direction face:Direction.values())h.assertValueEqual(tank(h,pos,face)!=null,face.getAxis()==output.getAxis(),"axial connections only");
         h.assertTrue(tank(h,pos,null)==null,"unsided valve does not expose a bypass");
         var input=tank(h,pos,output.getOpposite());var destination=tank(h,pos.relative(output),output.getOpposite());
         h.assertValueEqual(input.fill(new FluidStack(Fluids.WATER,2000),FluidAction.SIMULATE),1000,"per-operation cap with simulated forwarding");
         h.assertTrue(destination.getFluidInTank(0).isEmpty(),"simulation reaches destination without mutation");
         h.assertValueEqual(tank(h,pos,output).fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE),0,"reverse rejected");
         h.assertValueEqual(input.fill(new FluidStack(Fluids.WATER,2000),FluidAction.EXECUTE),1000,"forward cap");
         h.assertValueEqual(destination.getFluidInTank(0).getAmount(),1000,"only destination stores fluid");
         h.assertTrue(input.getFluidInTank(0).isEmpty()&&input.drain(1000,FluidAction.EXECUTE).isEmpty(),"valve neither stores nor drains");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void valve_cached_face_handlers_follow_rotation_and_target_replacement(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);clear(h,pos);
      level.setBlock(pos.east(),ModBlocks.PASSIVE_FLUID_ACCUMULATOR.get().defaultBlockState(),3);
      level.setBlock(pos.west(),ModBlocks.PASSIVE_FLUID_ACCUMULATOR.get().defaultBlockState(),3);
      level.setBlock(pos,ModBlocks.STRAIGHT_PIPE_VALVE.get().defaultBlockState().setValue(MachineBlocks.FACING,Direction.EAST),3);
      var cached=tank(h,pos,Direction.WEST);var be=level.getBlockEntity(pos);
      level.setBlock(pos,level.getBlockState(pos).setValue(MachineBlocks.FACING,Direction.WEST),3);
      h.assertTrue(level.getBlockEntity(pos)==be,"orientation change keeps stateless BE");
      h.assertValueEqual(cached.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE),0,"old cached input cannot bypass rotation");
      h.assertValueEqual(tank(h,pos,Direction.EAST).fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE),1000,"new inlet routes to opposite destination");
      level.setBlock(pos.west(),Blocks.STONE.defaultBlockState(),3);
      h.assertValueEqual(tank(h,pos,Direction.EAST).fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE),0,"missing target never consumes");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void controlled_and_analog_valves_obey_vault_signal_limits(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);
      for(var block:List.of(ModBlocks.STRAIGHT_PIPE_VALVE_REDSTONE.get(),ModBlocks.STRAIGHT_PIPE_VALVE_REDSTONE_ANALOG.get())) {
         clear(h,pos);level.setBlock(pos.east(),ModBlocks.PASSIVE_FLUID_ACCUMULATOR.get().defaultBlockState(),3);
         level.setBlock(pos,block.defaultBlockState().setValue(MachineBlocks.FACING,Direction.EAST),3);
         var wirePos=pos.south();level.setBlock(wirePos.below(),Blocks.STONE.defaultBlockState(),3);
         for(int signal:new int[]{0,1,7,14,15}) {
            var wire=Blocks.REDSTONE_WIRE.defaultBlockState().setValue(net.minecraft.world.level.block.RedStoneWireBlock.POWER,signal);
            for(var property:List.of(net.minecraft.world.level.block.RedStoneWireBlock.NORTH,net.minecraft.world.level.block.RedStoneWireBlock.SOUTH,
                  net.minecraft.world.level.block.RedStoneWireBlock.EAST,net.minecraft.world.level.block.RedStoneWireBlock.WEST))
               wire=wire.setValue(property,net.minecraft.world.level.block.state.properties.RedstoneSide.SIDE);
            level.setBlock(wirePos,wire,2); // Hold the selected vanilla wire state for this synchronous operation.
            // onPlace may update a newly created wire. Restore its selected state after installation.
            level.setBlock(wirePos,wire,2);
            h.assertValueEqual(level.getBestNeighborSignal(pos),signal,"fixture delivers exact native redstone signal");
            int expected=signal==0?0:block==ModBlocks.STRAIGHT_PIPE_VALVE_REDSTONE_ANALOG.get()&&signal<15?signal*66:1000;
            var input=tank(h,pos,Direction.WEST);var destination=tank(h,pos.east(),Direction.WEST);
            h.assertValueEqual(input.fill(new FluidStack(Fluids.WATER,2000),FluidAction.SIMULATE),expected,"selected mode simulation limit at "+signal);
            h.assertTrue(destination.getFluidInTank(0).isEmpty(),"simulation changes no destination volume");
            h.assertValueEqual(input.fill(new FluidStack(Fluids.WATER,2000),FluidAction.EXECUTE),expected,"selected mode actual limit at "+signal);
            h.assertValueEqual(destination.drain(4000,FluidAction.EXECUTE).getAmount(),expected,"forwarded volume exactly conserved");
         }
         level.setBlock(wirePos,Blocks.AIR.defaultBlockState(),3);
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void valve_ie_pipe_connections_and_actual_transit_use_pinned_ie(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);clear(h,pos);
      var pipeBlock=BuiltInRegistries.BLOCK.get(ie("fluid_pipe"));h.assertTrue(pipeBlock!=Blocks.AIR,"pinned IE pipe resolves");
      level.setBlock(pos,ModBlocks.STRAIGHT_PIPE_VALVE.get().defaultBlockState().setValue(MachineBlocks.FACING,Direction.EAST),3);
      level.setBlock(pos.east(2),ModBlocks.PASSIVE_FLUID_ACCUMULATOR.get().defaultBlockState(),3);
      level.setBlock(pos.east(),pipeBlock.defaultBlockState(),3);
      var pipe=(blusunrize.immersiveengineering.common.blocks.metal.FluidPipeBlockEntity)level.getBlockEntity(pos.east());
      pipe.onNeighborBlockChange(pos);pipe.onNeighborBlockChange(pos.east(2));
      h.assertTrue((pipe.getAvailableConnectionByte() & (1<<Direction.WEST.ordinal()))!=0,"IE renders an axial connection to the valve");
      var input=tank(h,pos,Direction.WEST);var destination=tank(h,pos.east(2),Direction.WEST);
      int offered=input.fill(new FluidStack(Fluids.WATER,1000),FluidAction.SIMULATE);
      h.assertTrue(offered>0&&offered<=1000,"IE determines its own actual pipe acceptance, bounded by valve max");
      h.assertTrue(destination.getFluidInTank(0).isEmpty(),"IE simulation is not a transfer");
      int actual=input.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);
      h.assertValueEqual(actual,offered,"stable IE route agrees with simulation");
      h.assertValueEqual(destination.getFluidInTank(0).getAmount(),actual,"real IE route conserves water");
      level.setBlock(pos.east(2),Blocks.STONE.defaultBlockState(),3);pipe.onNeighborBlockChange(pos.east(2));
      h.assertValueEqual(input.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE),0,"IE destination removal is respected");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void valve_closed_ie_loop_does_not_recurse_or_store_fluid(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);clear(h,pos);
      var pipeBlock=BuiltInRegistries.BLOCK.get(ie("fluid_pipe"));
      level.setBlock(pos,ModBlocks.STRAIGHT_PIPE_VALVE.get().defaultBlockState().setValue(MachineBlocks.FACING,Direction.EAST),3);
      var offsets=List.of(pos.east(),pos.east().north(),pos.north(),pos.west().north(),pos.west());
      for(var p:offsets) level.setBlock(p,pipeBlock.defaultBlockState(),3);
      for(var p:offsets) for(Direction side:Direction.values())
         ((blusunrize.immersiveengineering.common.blocks.metal.FluidPipeBlockEntity)level.getBlockEntity(p)).onNeighborBlockChange(p.relative(side));
      var input=tank(h,pos,Direction.WEST);
      for(var action:FluidAction.values()) h.assertValueEqual(input.fill(new FluidStack(Fluids.WATER,1000),action),0,"closed IE loop has no recipient: "+action);
      h.assertTrue(input.getFluidInTank(0).isEmpty(),"closed loop cannot accumulate hidden fluid");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void valve_components_reentry_and_exception_recovery_use_real_capability(GameTestHelper h) throws Exception {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);clear(h,pos);
      level.setBlock(pos,ModBlocks.STRAIGHT_PIPE_VALVE.get().defaultBlockState().setValue(MachineBlocks.FACING,Direction.EAST),3);
      var input=tank(h,pos,Direction.WEST);int[] inner={-1},calls={0};boolean[] throwing={true},probe={false};
      var destination=new net.neoforged.neoforge.fluids.capability.templates.FluidTank(4000) {
         @Override public int fill(FluidStack fluid,FluidAction action) {
            calls[0]++;
            if(throwing[0])throw new IllegalArgumentException("deliberate destination failure");
            if(probe[0]){probe[0]=false;inner[0]=input.fill(fluid.copy(),action);}
            return super.fill(fluid,action);
         }
      };
      var fluid=new FluidStack(Fluids.WATER,1500);var tag=new net.minecraft.nbt.CompoundTag();tag.putString("marker","preserve-components");
      fluid.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
      try(var installed=FluidHandlerTestTargets.install(level,pos.east(),destination)) {
         boolean propagated=false;
         try{input.fill(fluid,FluidAction.EXECUTE);}catch(IllegalArgumentException expected){propagated=expected.getMessage().equals("deliberate destination failure");}
         h.assertTrue(propagated,"unrelated destination exception is not swallowed");
         throwing[0]=false;probe[0]=true;calls[0]=0;
         h.assertValueEqual(input.fill(fluid,FluidAction.SIMULATE),1000,"guard released after exception; bounded simulation works");
         h.assertValueEqual(inner[0],0,"recursive fill is rejected during simulation");
         h.assertValueEqual(calls[0],1,"reentry never reaches destination twice");
         h.assertTrue(destination.isEmpty(),"simulation does not mutate recipient");
         probe[0]=true;calls[0]=0;
         h.assertValueEqual(input.fill(fluid,FluidAction.EXECUTE),1000,"actual transaction succeeds after failure");
         h.assertValueEqual(inner[0],0,"recursive actual fill is rejected");h.assertValueEqual(calls[0],1,"only one actual recipient call");
         h.assertValueEqual(destination.getFluidAmount(),1000,"no recursive duplicate credit");
         h.assertTrue(FluidStack.isSameFluidSameComponents(destination.getFluid(),fluid),"components reach destination unchanged");
         h.assertValueEqual(fluid.getAmount(),1500,"caller stack is not shrunk or mutated");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void valve_redstone_connectors_exclude_axis_after_rotation(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);clear(h,pos);
      level.setBlock(pos.west(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
      level.setBlock(pos.above(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
      level.setBlock(pos,ModBlocks.STRAIGHT_PIPE_VALVE_REDSTONE.get().defaultBlockState().setValue(MachineBlocks.FACING,Direction.EAST),3);
      h.assertFalse(level.getBlockState(pos).getValue(MachineBlocks.RS_W),"pipe axis cannot gain a redstone connector");
      h.assertTrue(level.getBlockState(pos).getValue(MachineBlocks.RS_U),"powered transverse face has its connector");
      level.setBlock(pos,level.getBlockState(pos).setValue(MachineBlocks.FACING,Direction.UP),3);
      h.assertFalse(level.getBlockState(pos).getValue(MachineBlocks.RS_U),"new pipe axis connector cleared immediately");
      h.assertTrue(level.getBlockState(pos).getValue(MachineBlocks.RS_W),"former axis becomes a transverse powered connector");
      h.succeed();
   }
}
