package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ValveManualInteractionGameTests {
   @GameTest(template="empty")
   public static void documented_ie_hammer_use_rotates_all_three_horizontal_valves(GameTestHelper h) {
      var hammer=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("immersiveengineering","hammer"));
      h.assertTrue(hammer!=Items.AIR,"pinned IE hammer resolves");
      // This item hook needs a server-level Player, not a logged-in network client.
      var pos=h.absolutePos(new BlockPos(3,3,3));var player=h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
      for(var block:java.util.List.of(ModBlocks.STRAIGHT_PIPE_VALVE.get(),ModBlocks.STRAIGHT_PIPE_VALVE_REDSTONE.get(),ModBlocks.STRAIGHT_PIPE_VALVE_REDSTONE_ANALOG.get())) {
         h.getLevel().setBlock(pos,block.defaultBlockState().setValue(MachineBlocks.FACING,Direction.NORTH),3);
         var stack=new ItemStack(hammer);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
         var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
         var context=new UseOnContext(player,InteractionHand.MAIN_HAND,hit);
         var result=hammer.onItemUseFirst(stack,context);
         h.assertTrue(result.consumesAction(),"actual IE Use action accepts horizontal rotation");
         h.assertValueEqual(h.getLevel().getBlockState(pos).getValue(MachineBlocks.FACING),Direction.EAST,"the documented Use action turns the outlet clockwise");
      }
      h.succeed();
   }
}
