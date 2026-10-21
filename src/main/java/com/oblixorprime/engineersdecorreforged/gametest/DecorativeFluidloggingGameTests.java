package com.oblixorprime.engineersdecorreforged.gametest;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DecorativeFluidloggingGameTests {
   private static BlockState wet(BlockState state, Fluid fluid) {
      return NativeFluidloggedBlock.placement(state, ((FlowingFluid)fluid).getSource(false));
   }
   @GameTest(template="empty")
   public static void native_cells_preserve_every_geometry_state(GameTestHelper h) {
      BlockPos pos=h.absolutePos(new BlockPos(3,2,3));
      for(var entry:ModBlocks.ORDERED_BLOCKS) {
         Block block=entry.get();if(!(block instanceof NativeFluidloggedBlock))continue;
         var defaults=block.defaultBlockState();h.assertTrue(defaults.getFluidState().isEmpty(),"default dry: "+entry.getId());
         var states=block.getStateDefinition().getPossibleStates().stream()
            .filter(s->s.equals(NativeFluidloggedBlock.dry(s))).toList();
         for(var dry:states)for(Fluid fluid:java.util.List.of(Fluids.WATER,Fluids.LAVA,NativeFluidloggedBlock.creosote())) {
            var filled=wet(dry,fluid);
            h.assertValueEqual(filled.getLightEmission(h.getLevel(),pos), fluid==Fluids.LAVA?15:dry.getLightEmission(h.getLevel(),pos),
               "lava emits15 while water/creosote preserve existing light and signal state: "+entry.getId());
            h.assertFalse(Shapes.joinIsNotEmpty(dry.getShape(h.getLevel(),pos),filled.getShape(h.getLevel(),pos),BooleanOp.NOT_SAME),"cell preserves selection: "+entry.getId());
            h.assertFalse(Shapes.joinIsNotEmpty(dry.getCollisionShape(h.getLevel(),pos),filled.getCollisionShape(h.getLevel(),pos),BooleanOp.NOT_SAME),"cell preserves collision: "+entry.getId());
         }
      }
      h.succeed();
   }
   @GameTest(template="empty")
   public static void wet_slab_real_merge_keeps_cell_and_two_paid_drops(GameTestHelper h) {
      var level=h.getLevel();BlockPos pos=h.absolutePos(new BlockPos(3,2,3));
      var player=new net.neoforged.neoforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"WetSlab"));
      player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
      for(var entry:ModBlocks.ORDERED_BLOCKS) {
         var block=entry.get();if(!(block instanceof NativeDecorativeBlocks.NativeSlabBlock))continue;
         for(int fluid=0;fluid<4;fluid++) {
            var area=new AABB(pos).inflate(2);
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area).forEach(net.minecraft.world.entity.Entity::discard);
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);
            var bottom=block.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.BOTTOM);
            bottom=fluid==3?StairWaterFlow.withFlow(bottom,Fluids.FLOWING_WATER.getFlowing(5,false)):wet(bottom,java.util.List.of(Fluids.WATER,Fluids.LAVA,NativeFluidloggedBlock.creosote()).get(fluid));
            level.setBlock(pos,bottom,3);var expected=bottom.getFluidState();var stack=new ItemStack(block);
            player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            var context=new BlockPlaceContext(level,player,InteractionHand.MAIN_HAND,stack,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
            h.assertTrue(((BlockItem)stack.getItem()).place(context).consumesAction(),"second real slab merges");
            h.assertTrue(player.getMainHandItem().isEmpty(),"merge consumes one paid slab");
            h.assertValueEqual(level.getBlockState(pos).getValue(SlabBlock.TYPE),SlabType.DOUBLE,"double shape");
            h.assertValueEqual(level.getFluidState(pos),expected,"merge preserves source/current");
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));player.gameMode.destroyBlock(pos);
            h.assertValueEqual(level.getFluidState(pos),expected,"destruction releases exact cell");
            int drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area,e->e.getItem().is(block.asItem())).stream().mapToInt(e->e.getItem().getCount()).sum();
            h.assertValueEqual(drops,2,"double slab refunds two paid slabs");
         }
      }
      h.succeed();
   }
   @GameTest(template="empty")
   public static void wet_door_independent_cells_and_real_break(GameTestHelper h) {
      var level=h.getLevel();BlockPos lower=h.absolutePos(new BlockPos(3,2,3));
      var player=new net.neoforged.neoforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"WetDoor"));
      for(var entry:ModBlocks.ORDERED_BLOCKS) {
         var block=entry.get();if(!(block instanceof NativeDecorativeBlocks.NativeDoorBlock))continue;
         for(boolean creative:new boolean[]{false,true})for(int half=0;half<2;half++)for(int fluid=0;fluid<4;fluid++) {
            var area=new AABB(lower).inflate(2);
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area).forEach(net.minecraft.world.entity.Entity::discard);
            level.setBlock(lower,Blocks.AIR.defaultBlockState(),2);level.setBlock(lower.above(),Blocks.AIR.defaultBlockState(),2);
            level.setBlock(lower.below(),Blocks.STONE.defaultBlockState(),3);
            var bottom=block.defaultBlockState();var top=bottom.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER);
            if(fluid==3){bottom=StairWaterFlow.withFlow(bottom,Fluids.FLOWING_WATER.getFlowing(5,false));top=StairWaterFlow.withFlow(top,Fluids.FLOWING_WATER.getFlowing(4,false));}
            else {var f=java.util.List.of(Fluids.WATER,Fluids.LAVA,NativeFluidloggedBlock.creosote()).get(fluid);bottom=wet(bottom,f);top=wet(top,fluid==0?NativeFluidloggedBlock.creosote():f);}
            var lowerFluid=bottom.getFluidState();var upperFluid=top.getFluidState();
            level.setBlock(lower,bottom,2);level.setBlock(lower.above(),top,3);
            h.assertValueEqual(level.getFluidState(lower),lowerFluid,"vertical sync preserves lower cell");
            h.assertValueEqual(level.getFluidState(lower.above()),upperFluid,"vertical sync preserves upper cell");
            player.gameMode.changeGameModeForPlayer(creative?GameType.CREATIVE:GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));player.gameMode.destroyBlock(lower.above(half));
            h.assertFalse(level.getBlockState(lower).is(block)||level.getBlockState(lower.above()).is(block),"real break removes both halves");
            h.assertValueEqual(level.getFluidState(lower),lowerFluid,"break restores lower source/current");
            h.assertValueEqual(level.getFluidState(lower.above()),upperFluid,"break restores upper source/current");
            int drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area,e->e.getItem().is(block.asItem())).stream().mapToInt(e->e.getItem().getCount()).sum();
            h.assertValueEqual(drops,creative?0:1,"one door in survival only");
         }
      }
      h.succeed();
   }
   @GameTest(template="empty")
   public static void ariadne_real_tool_preserves_source_and_single_charge(GameTestHelper h) {
      var level=h.getLevel();var player=h.makeMockPlayer(GameType.SURVIVAL);BlockPos pos=h.absolutePos(new BlockPos(3,2,3));
      var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(EngineersDecorReforged.MOD_ID,"ariadne_coal"));
      h.assertTrue(item instanceof com.oblixorprime.engineersdecorreforged.tools.AriadneCoalItem,"actual registered Ariadne tool");
      h.assertTrue(ModBlocks.ARIADNE_MARKER.get().asItem()==Items.AIR,"marker has no BlockItem");
      for(Fluid fluid:java.util.List.of(Fluids.WATER,Fluids.LAVA,NativeFluidloggedBlock.creosote())) {
         level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);
         level.setBlock(pos,((FlowingFluid)fluid).getSource(false).createLegacyBlock(),3);
         var stack=new ItemStack(item);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
         var hit=new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0,.5,0),Direction.UP,pos.below(),false);
         h.assertTrue(item.useOn(new net.minecraft.world.item.context.UseOnContext(player,InteractionHand.MAIN_HAND,hit)).consumesAction(),"real tool places marker");
         h.assertTrue(level.getBlockState(pos).is(ModBlocks.ARIADNE_MARKER.get()),"marker remains placed");
         h.assertTrue(level.getFluidState(pos).getType()==fluid && level.getFluidState(pos).isSource(),"tool preserves source");
         h.assertValueEqual(stack.getDamageValue(),1,"one tool charge");
         level.destroyBlock(pos,true);h.assertTrue(level.getFluidState(pos).getType()==fluid,"marker break releases source");
      }
      h.succeed();
   }
}
