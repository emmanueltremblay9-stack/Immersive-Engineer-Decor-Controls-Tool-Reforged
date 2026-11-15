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
      tick(helper, hopper, 1);
      helper.assertValueEqual(3, source.getItem(0).getCount(), "default inverted continuous mode must extract while unpowered");
      helper.assertValueEqual(64, target.getItem(0).getCount(), "fill the existing stack without exceeding its limit");
      helper.assertValueEqual(31, target.getItem(1).getCount(), "insert remaining batch into the next slot");
      helper.assertValueEqual(0, count(hopper), "vanilla transfer must conserve all items");
      helper.succeed();
   }

   @GameTest(template = "empty")
   public static void capability_only_transfer_uses_original_vertical_intake_and_output_faces(GameTestHelper helper) throws Exception {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      for (Direction output : Direction.values()) {
         Direction input = output == Direction.UP ? Direction.DOWN : Direction.UP;
         MachineBlockEntity hopper = place(helper, output);
         var source = new ItemStackHandler(1);
         var target = new ItemStackHandler(1);
         source.setStackInSlot(0, new ItemStack(Items.OAK_LOG, 9));
         try (var in = ItemHandlerTestTargets.install(helper.getLevel(), helper.absolutePos(POS.relative(input)), input.getOpposite(), source);
              var out = ItemHandlerTestTargets.install(helper.getLevel(), helper.absolutePos(POS.relative(output)), output.getOpposite(), target)) {
            helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(POS.relative(output))) == null,
               "fixture must not expose a Container or block entity");
            configure(hopper, player, 4, 4, 0);
            tick(helper, hopper, 1);
            helper.assertValueEqual(5, source.getStackInSlot(0).getCount(), "extract configured batch from " + output);
            helper.assertValueEqual(4, target.getStackInSlot(0).getCount(), "insert configured batch toward " + output);
            helper.assertValueEqual(0, count(hopper), "batch should pass through without duplication");
         }
         helper.setBlock(POS.relative(output), Blocks.AIR);
         helper.setBlock(POS.relative(input), Blocks.AIR);
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
         tick(helper, hopper, 1);
         helper.assertValueEqual(7, source.getStackInSlot(0).getCount(), "denied intake face must not fall back to another side");
         helper.assertValueEqual(5, count(hopper), "denied output must retain stored items");
         helper.assertTrue(target.getStackInSlot(0).isEmpty(), "denied output face must remain untouched");
      }
      try (var in = ItemHandlerTestTargets.install(helper.getLevel(), helper.absolutePos(POS.above()), Direction.DOWN, source);
           var out = ItemHandlerTestTargets.install(helper.getLevel(), helper.absolutePos(POS.below()), Direction.UP, target)) {
         tick(helper, hopper, 10);
         helper.assertValueEqual(3, source.getStackInSlot(0).getCount(), "replacement intake capability must be queried again");
         helper.assertTrue(target.getStackInSlot(0).isEmpty(), "missing-handler backoff12 is not bypassed when input resumes");
         helper.assertValueEqual(9, count(hopper), "input continues during backoff");
         tick(helper, hopper, 10);
         helper.assertValueEqual(0, source.getStackInSlot(0).getCount(), "second intake runs independently of output delay");
         helper.assertValueEqual(4, target.getStackInSlot(0).getCount(), "replacement output receives exact budget after backoff");
         helper.assertValueEqual(8, count(hopper), "initial12items conserved between source, hopper and target");
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
         tick(helper, hopper, 1);
         helper.assertValueEqual(9, source.getStackInSlot(0).getCount(), "consume only the real extraction result");
         helper.assertValueEqual(2, target.getStackInSlot(0).getCount(), "simulation must not insert extra items");
         tick(helper, hopper, 16);
         helper.assertValueEqual(5, source.getStackInSlot(0).getCount(), "three work cycles at1,9,17 extract only2 each");
         helper.assertValueEqual(3, target.getStackInSlot(0).getCount(), "partial insertion must honor destination slot limit");
         helper.assertValueEqual(3, count(hopper), "unaccepted output must remain in the hopper");
         for (int slot = 0; slot < hopper.getContainerSize(); slot++) hopper.setItem(slot, new ItemStack(Items.DIRT, 64));
         tick(helper, hopper, 10);
         helper.assertValueEqual(5, source.getStackInSlot(0).getCount(), "full storage must not execute a simulated extraction");
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
   public static void manual_trigger_is_original_powered_edge_not_cooldown_bypass(GameTestHelper helper) throws Exception {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      for (int logic : new int[]{0, 1, 2, 3, 4, 6}) {
         helper.setBlock(POS.east(), Blocks.AIR);
         MachineBlockEntity hopper = place(helper, Direction.DOWN);
         var target = new ItemStackHandler(1);
         try (var out = ItemHandlerTestTargets.install(helper.getLevel(), helper.absolutePos(POS.below()), Direction.UP, target)) {
            hopper.setItem(0, new ItemStack(Items.OAK_LOG, 32));
            configure(hopper, player, logic, 1, 100);
            hopper.handleGuiAction(player, 5, 1, 0);
            tick(helper, hopper, 1);
            int expected = (logic == 0 || logic == 2 || (logic & 4) != 0) ? 1 : 0;
            helper.assertValueEqual(expected, target.getStackInSlot(0).getCount(), "manual sets powered edge, respects inversion mode " + logic);
            if (expected == 1) {
               hopper.handleGuiAction(player, 5, 1, 0);
               tick(helper, hopper, 1);
               helper.assertValueEqual(1, target.getStackInSlot(0).getCount(), "manual must not shorten the insertion delay");
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty")
   public static void input_continues_during_output_delay_and_output_uses_one_source_stack(GameTestHelper helper) throws Exception {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      MachineBlockEntity hopper = place(helper, Direction.NORTH);
      var source = new ItemStackHandler(1);
      var target = new ItemStackHandler(3);
      source.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 32));
      hopper.setItem(0, new ItemStack(Items.OAK_LOG, 2));
      hopper.setItem(1, new ItemStack(Items.DIRT, 10));
      target.setStackInSlot(2, new ItemStack(Items.OAK_LOG, 60));
      try (var in = ItemHandlerTestTargets.install(helper.getLevel(), helper.absolutePos(POS.above()), Direction.DOWN, source);
           var out = ItemHandlerTestTargets.install(helper.getLevel(), helper.absolutePos(POS.north()), Direction.SOUTH, target)) {
         configure(hopper, player, 4, 4, 100);
         tick(helper, hopper, 1);
         helper.assertValueEqual(62, target.getStackInSlot(2).getCount(), "matching destination stack filled before empty earlier slot");
         helper.assertTrue(target.getStackInSlot(0).isEmpty(), "one source stack only, no dirt in same cycle");
         tick(helper, hopper, 10);
         helper.assertTrue(source.getStackInSlot(0).getCount() <= 24, "input keeps pulling despite long output delay");
         helper.assertValueEqual(62, target.getStackInSlot(2).getCount(), "output delay preserved");
      }
      helper.succeed();
   }

   @GameTest(template = "empty")
   public static void opposing_hoppers_do_not_push_back(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      for (boolean vanilla : new boolean[]{true, false}) {
         MachineBlockEntity hopper = place(helper, Direction.NORTH);
         helper.setBlock(POS.north(), vanilla
            ? Blocks.HOPPER.defaultBlockState().setValue(net.minecraft.world.level.block.HopperBlock.FACING, Direction.SOUTH)
            : ModBlocks.FACTORY_HOPPER.get().defaultBlockState().setValue(MachineBlocks.FACING, Direction.SOUTH));
         hopper.setItem(0, new ItemStack(Items.DIRT, 4));
         configure(hopper, player, 4, 4, 0);
         tick(helper, hopper, 10);
         helper.assertValueEqual(4, count(hopper), "opposing hopper must not receive back-transfer");
         helper.assertTrue(((Container)helper.getLevel().getBlockEntity(helper.absolutePos(POS.north()))).isEmpty(), "target remains empty");
         helper.setBlock(POS.north(), Blocks.AIR);
      }
      helper.succeed();
   }

   @GameTest(template = "empty")
   public static void hopper_collects_ground_items_with_original_delay_direct_radius_and_limit(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      MachineBlockEntity hopper = place(helper, Direction.DOWN);
      configure(hopper, player, 4, 1, 100);
      hopper.handleGuiAction(player, 1, 4, 0);
      BlockPos center = helper.absolutePos(POS.above());
      java.util.List<net.minecraft.world.entity.item.ItemEntity> distant = new java.util.ArrayList<>();
      for (int n=0;n<5;n++) {
         var item = new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(),center.getX()+2.0,center.getY()+0.5,center.getZ()+0.5,new ItemStack(Items.DIRT));
         item.setOnGround(true); helper.getLevel().addFreshEntity(item); distant.add(item);
      }
      var delayed = new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(),center.getX()+2,center.getY()+0.5,center.getZ()+1,new ItemStack(Items.DIAMOND));
      delayed.setOnGround(true);delayed.setPickUpDelay(200);helper.getLevel().addFreshEntity(delayed);
      var airborne = new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(),center.getX()+0.5,center.getY()+0.5,center.getZ()+0.5,new ItemStack(Items.EMERALD));
      airborne.setOnGround(false);helper.getLevel().addFreshEntity(airborne);
      var direct = new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(),center.getX()+0.5,center.getY()+0.5,center.getZ()+0.5,new ItemStack(Items.IRON_INGOT));
      direct.setOnGround(true);direct.setPickUpDelay(200);helper.getLevel().addFreshEntity(direct);
      tick(helper,hopper,1);
      helper.assertValueEqual(2,(int)distant.stream().filter(e->e.isAlive()).count(),"at most3 distant entities per sweep");
      helper.assertTrue(delayed.isAlive()&&airborne.isAlive(),"delay and grounded guards retained");
      // Traversal order may reach the distant limit before direct items; a later fall forces next sweep.
      hopper.hopperItemFell();tick(helper,hopper,10);
      helper.assertTrue(!direct.isAlive(),"direct collection ignores pickup delay");
      helper.assertTrue(delayed.isAlive()&&airborne.isAlive(),"second sweep still honors guards");
      helper.succeed();
   }

   @GameTest(template = "empty")
   public static void hopper_transfers_through_inventory_minecarts(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      MachineBlockEntity hopper=place(helper,Direction.NORTH);
      var source=net.minecraft.world.entity.EntityType.CHEST_MINECART.create(helper.getLevel());
      var target=net.minecraft.world.entity.EntityType.CHEST_MINECART.create(helper.getLevel());
      helper.assertTrue(source!=null&&target!=null,"minecart fixtures exist");
      BlockPos in=helper.absolutePos(POS.above()),out=helper.absolutePos(POS.north());
      source.setPos(in.getX()+0.5,in.getY()+0.1,in.getZ()+0.5);
      target.setPos(out.getX()+0.5,out.getY()+0.1,out.getZ()+0.5);
      source.setItem(0,new ItemStack(Items.IRON_INGOT,8));
      helper.getLevel().addFreshEntity(source);helper.getLevel().addFreshEntity(target);
      configure(hopper,player,4,4,100);tick(helper,hopper,1);
      helper.assertValueEqual(4,source.getItem(0).getCount(),"minecart input extracted exact budget");
      helper.assertValueEqual(4,target.getItem(0).getCount(),"minecart output received same items");
      helper.assertValueEqual(0,count(hopper),"minecart path conserves items");
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
