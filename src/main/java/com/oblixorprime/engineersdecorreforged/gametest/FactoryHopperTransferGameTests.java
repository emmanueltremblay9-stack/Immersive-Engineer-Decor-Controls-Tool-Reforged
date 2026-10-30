package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FactoryHopperTransferGameTests {
   private static final BlockPos POS = new BlockPos(2, 2, 2);

   @GameTest(template = "empty")
   public static void vanilla_chest_to_barrel_preserves_default_mode_and_partial_stacks(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      MachineBlockEntity hopper = place(helper, Direction.DOWN);
      helper.setBlock(POS.above(), Blocks.CHEST);
      helper.setBlock(POS.below(), Blocks.BARREL);
      Container source = (Container)helper.getLevel().getBlockEntity(helper.absolutePos(POS.above()));
      Container target = (Container)helper.getLevel().getBlockEntity(helper.absolutePos(POS.below()));
      source.setItem(0, new ItemStack(Items.OAK_LOG, 35));
      target.setItem(0, new ItemStack(Items.OAK_LOG, 63));
      hopper.handleGuiAction(player, 2, 32, 0);
      tick(helper, hopper, 10);
      helper.assertValueEqual(3, source.getItem(0).getCount(), "default inverted continuous mode must extract while unpowered");
      helper.assertValueEqual(64, target.getItem(0).getCount(), "fill the existing stack without exceeding its limit");
      helper.assertValueEqual(31, target.getItem(1).getCount(), "insert remaining batch into the next slot");
      helper.assertValueEqual(0, count(hopper), "vanilla transfer must conserve all items");
      helper.succeed();
   }

   @GameTest(template = "empty")
   public static void capability_only_transfer_uses_facing_and_opposite_neighbor_faces(GameTestHelper helper) throws Exception {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      for (Direction output : Direction.values()) {
         MachineBlockEntity hopper = place(helper, output);
         var source = new ItemStackHandler(1);
         var target = new ItemStackHandler(1);
         source.setStackInSlot(0, new ItemStack(Items.OAK_LOG, 9));
         try (var in = ItemHandlerTestTargets.install(helper.getLevel(), helper.absolutePos(POS.relative(output.getOpposite())), output, source);
              var out = ItemHandlerTestTargets.install(helper.getLevel(), helper.absolutePos(POS.relative(output)), output.getOpposite(), target)) {
            helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(POS.relative(output))) == null,
               "fixture must not expose a Container or block entity");
            configure(hopper, player, 4, 4, 0);
            tick(helper, hopper, 10);
            helper.assertValueEqual(5, source.getStackInSlot(0).getCount(), "extract configured batch from " + output);
            helper.assertValueEqual(4, target.getStackInSlot(0).getCount(), "insert configured batch toward " + output);
            helper.assertValueEqual(0, count(hopper), "batch should pass through without duplication");
         }
         helper.setBlock(POS.relative(output), Blocks.AIR);
         helper.setBlock(POS.relative(output.getOpposite()), Blocks.AIR);
      }
      helper.succeed();
   }

   @GameTest(template = "empty")
   public static void capability_faces_rejection_and_replacement_are_respected(GameTestHelper helper) throws Exception {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      MachineBlockEntity hopper = place(helper, Direction.DOWN);
      var source = new ItemStackHandler(1);
      var target = new ItemStackHandler(1);
      source.setStackInSlot(0, new ItemStack(Items.OAK_LOG, 7));
      hopper.setItem(0, new ItemStack(Items.OAK_LOG, 5));
      try (var in = ItemHandlerTestTargets.install(helper.getLevel(), helper.absolutePos(POS.above()), Direction.UP, source);
           var out = ItemHandlerTestTargets.install(helper.getLevel(), helper.absolutePos(POS.below()), Direction.DOWN, target)) {
         configure(hopper, player, 4, 4, 0);
         tick(helper, hopper, 10);
         helper.assertValueEqual(7, source.getStackInSlot(0).getCount(), "denied intake face must not fall back to another side");
         helper.assertValueEqual(5, count(hopper), "denied output must retain stored items");
         helper.assertTrue(target.getStackInSlot(0).isEmpty(), "denied output face must remain untouched");
      }
      try (var in = ItemHandlerTestTargets.install(helper.getLevel(), helper.absolutePos(POS.above()), Direction.DOWN, source);
           var out = ItemHandlerTestTargets.install(helper.getLevel(), helper.absolutePos(POS.below()), Direction.UP, target)) {
         tick(helper, hopper, 10);
         helper.assertValueEqual(3, source.getStackInSlot(0).getCount(), "replacement intake capability must be queried again");
         helper.assertValueEqual(4, target.getStackInSlot(0).getCount(), "replacement output capability must be queried again");
         helper.assertValueEqual(5, count(hopper), "total items must be conserved across denied and allowed cycles");
      }
      helper.succeed();
   }

   @GameTest(template = "empty")
   public static void partial_handler_execution_and_full_storage_preserve_items(GameTestHelper helper) throws Exception {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      MachineBlockEntity hopper = place(helper, Direction.DOWN);
      var source = new ItemStackHandler(1) {
         @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return super.extractItem(slot, simulate ? amount : Math.min(2, amount), simulate);
         }
      };
      var target = new ItemStackHandler(1) {
         @Override public int getSlotLimit(int slot) { return 3; }
      };
      source.setStackInSlot(0, new ItemStack(Items.OAK_LOG, 11));
      try (var in = ItemHandlerTestTargets.install(helper.getLevel(), helper.absolutePos(POS.above()), Direction.DOWN, source);
           var out = ItemHandlerTestTargets.install(helper.getLevel(), helper.absolutePos(POS.below()), Direction.UP, target)) {
         configure(hopper, player, 4, 32, 0);
         tick(helper, hopper, 10);
         helper.assertValueEqual(9, source.getStackInSlot(0).getCount(), "consume only the real extraction result");
         helper.assertValueEqual(2, target.getStackInSlot(0).getCount(), "simulation must not insert extra items");
         tick(helper, hopper, 10);
         helper.assertValueEqual(7, source.getStackInSlot(0).getCount(), "second extraction should remain bounded");
         helper.assertValueEqual(3, target.getStackInSlot(0).getCount(), "partial insertion must honor destination slot limit");
         helper.assertValueEqual(1, count(hopper), "unaccepted output must remain in the hopper");
         for (int slot = 0; slot < hopper.getContainerSize(); slot++) hopper.setItem(slot, new ItemStack(Items.DIRT, 64));
         tick(helper, hopper, 10);
         helper.assertValueEqual(7, source.getStackInSlot(0).getCount(), "full storage must not execute a simulated extraction");
         helper.assertValueEqual(64 * hopper.getContainerSize(), count(hopper), "failed transfers must leave full storage unchanged");
      }
      helper.succeed();
   }

   @GameTest(template = "empty")
   public static void furnace_sided_slots_prevent_output_insertion_and_top_output_extraction(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      MachineBlockEntity hopper = place(helper, Direction.UP);
      helper.setBlock(POS.above(), Blocks.FURNACE);
      helper.setBlock(POS.below(), Blocks.FURNACE);
      Container source = (Container)helper.getLevel().getBlockEntity(helper.absolutePos(POS.below()));
      Container target = (Container)helper.getLevel().getBlockEntity(helper.absolutePos(POS.above()));
      source.setItem(2, new ItemStack(Items.IRON_INGOT, 5));
      hopper.setItem(0, new ItemStack(Items.COBBLESTONE, 4));
      configure(hopper, player, 4, 4, 0);
      tick(helper, hopper, 10);
      helper.assertValueEqual(5, source.getItem(2).getCount(), "furnace output must not be extracted through its top input face");
      helper.assertValueEqual(4, count(hopper), "non-fuel cannot be inserted through a furnace bottom face");
      helper.assertTrue(target.isEmpty(), "furnace output slot must not accept insertion");
      helper.succeed();
   }

   @GameTest(template = "empty")
   public static void redstone_modes_manual_trigger_and_cooldown_keep_existing_contract(GameTestHelper helper) throws Exception {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      for (int logic : new int[]{0, 1, 2, 3, 4, 6}) {
         helper.setBlock(POS.east(), Blocks.AIR);
         MachineBlockEntity hopper = place(helper, Direction.DOWN);
         var target = new ItemStackHandler(1);
         try (var out = ItemHandlerTestTargets.install(helper.getLevel(), helper.absolutePos(POS.below()), Direction.UP, target)) {
            hopper.setItem(0, new ItemStack(Items.OAK_LOG, 32));
            configure(hopper, player, logic, 1, 5); // 20 ticks between batches, checked every 10 ticks.
            tick(helper, hopper, 10);
            int expected = (logic == 3 || (logic & 4) != 0) ? 1 : 0;
            helper.assertValueEqual(expected, target.getStackInSlot(0).getCount(), "initial unpowered mode " + logic);
            tick(helper, hopper, 10);
            helper.assertValueEqual(expected, target.getStackInSlot(0).getCount(), "cooldown/held signal must not transfer mode " + logic);
            tick(helper, hopper, 10);
            if (logic == 3 || (logic & 4) != 0) expected++;
            helper.assertValueEqual(expected, target.getStackInSlot(0).getCount(), "continuous repeat mode " + logic);
            tick(helper, hopper, 10);
            helper.setBlock(POS.east(), Blocks.REDSTONE_BLOCK);
            tick(helper, hopper, 10);
            if (logic == 0 || logic == 2 || (logic & 4) != 0) expected++;
            helper.assertValueEqual(expected, target.getStackInSlot(0).getCount(), "rising edge mode " + logic);
            tick(helper, hopper, 20);
            if (logic == 2 || (logic & 4) != 0) expected++;
            helper.assertValueEqual(expected, target.getStackInSlot(0).getCount(), "held powered mode " + logic);
            helper.setBlock(POS.east(), Blocks.AIR);
            tick(helper, hopper, 20);
            if (logic == 1 || logic == 3 || (logic & 4) != 0) expected++;
            helper.assertValueEqual(expected, target.getStackInSlot(0).getCount(), "falling edge mode " + logic);
            hopper.handleGuiAction(player, 5, 1, 0);
            tick(helper, hopper, 10);
            helper.assertValueEqual(expected + 1, target.getStackInSlot(0).getCount(), "manual trigger overrides redstone and shortens cooldown mode " + logic);
         }
      }
      helper.succeed();
   }

   private static MachineBlockEntity place(GameTestHelper helper, Direction output) {
      helper.setBlock(POS, Blocks.AIR);
      helper.setBlock(POS, ModBlocks.FACTORY_HOPPER.get().defaultBlockState().setValue(MachineBlocks.FACING, output));
      return (MachineBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(POS));
   }

   private static void configure(MachineBlockEntity hopper, Player player, int logic, int count, int period) {
      hopper.handleGuiAction(player, 3, logic, 0);
      hopper.handleGuiAction(player, 2, count, 0);
      hopper.handleGuiAction(player, 4, period, 0);
   }

   private static void tick(GameTestHelper helper, MachineBlockEntity hopper, int ticks) {
      for (int i = 0; i < ticks; i++) MachineBlockEntity.serverTick(helper.getLevel(), hopper.getBlockPos(), hopper.getBlockState(), hopper);
   }

   private static int count(MachineBlockEntity hopper) {
      int total = 0;
      for (int slot = 0; slot < hopper.getContainerSize(); slot++) total += hopper.getItem(slot).getCount();
      return total;
   }

   private FactoryHopperTransferGameTests() {}
}
