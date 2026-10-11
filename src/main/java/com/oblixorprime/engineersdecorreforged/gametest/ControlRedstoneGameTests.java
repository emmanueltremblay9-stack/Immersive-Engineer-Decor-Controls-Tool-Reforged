package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.rsgauges.ControlsBlockTypes;
import com.oblixorprime.engineersdecorreforged.rsgauges.ControlsModule;
import com.oblixorprime.engineersdecorreforged.rsgauges.SwitchLinkPearlItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredBlock;

@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class ControlRedstoneGameTests {
   private static final String TEMPLATE = "empty";
   private static final BlockPos TEST_POS = new BlockPos(1, 1, 1);
   private static final int EXPECTED_LATCHING_TOGGLE_COUNT = 23;

   private ControlRedstoneGameTests() {
   }

   @GameTest(template = "empty", timeoutTicks = 120)
   public static void all_latching_lever_controls_toggle_and_emit_redstone(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      List<ResourceLocation> tested = new ArrayList<>();

      for (DeferredBlock<? extends Block> entry : ControlsModule.CONTROLS) {
         Block block = (Block)entry.get();
         if ((ControlsBlockTypes.ToggleSwitchBlock.class.equals(block.getClass())
            || block instanceof com.oblixorprime.engineersdecorreforged.rsgauges.IndustrialSwitchBlock)) {
            ResourceLocation id = entry.getId();
            tested.add(id);
            helper.setBlock(TEST_POS, Blocks.AIR);
            BlockState offState = (BlockState)((BlockState)block.defaultBlockState().setValue(ControlsBlockTypes.FACING, Direction.NORTH))
               .setValue(ControlsBlockTypes.POWERED, false);
            helper.setBlock(TEST_POS, offState);
            helper.assertBlockProperty(TEST_POS, ControlsBlockTypes.POWERED, false);
            helper.assertValueEqual(0, signal(helper), id + " should start with redstone output 0");
            helper.useBlock(TEST_POS, player);
            helper.assertBlockProperty(TEST_POS, ControlsBlockTypes.POWERED, true);
            helper.assertValueEqual(15, signal(helper), id + " should emit full-strength redstone after first use");
            helper.assertRedstoneSignal(TEST_POS, Direction.NORTH, value -> value == 15, () -> id + " should expose full weak redstone signal to neighbors");
            helper.useBlock(TEST_POS, player);
            helper.assertBlockProperty(TEST_POS, ControlsBlockTypes.POWERED, false);
            helper.assertValueEqual(0, signal(helper), id + " should return to redstone output 0 after second use");
         }
      }

      helper.assertValueEqual(23, tested.size(), "latching lever/control coverage count changed; counted " + tested.size() + " controls");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void representative_pulse_controls_reset_after_scheduled_tick(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      assertPulseControl(helper, player, "industrial_button");
      assertPulseControl(helper, player, "glass_interval_timer");
      helper.runAfterDelay(45L, () -> {
         assertPulseReset(helper, "industrial_button", new BlockPos(1, 1, 1));
         assertPulseReset(helper, "glass_interval_timer", new BlockPos(2, 1, 1));
         helper.succeed();
      });
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void contact_switch_triggers_from_entity_inside(GameTestHelper helper) {
      Block block = control("industrial_contact_mat");
      BlockState offState = (BlockState)((BlockState)block.defaultBlockState().setValue(ControlsBlockTypes.FACING, Direction.UP))
         .setValue(ControlsBlockTypes.POWERED, false);
      helper.setBlock(TEST_POS, offState);
      helper.spawn(EntityType.PIG, TEST_POS);
      helper.succeedWhen(() -> {
         helper.assertBlockProperty(TEST_POS, ControlsBlockTypes.POWERED, true);
         helper.assertValueEqual(15, signal(helper), "contact switch should emit redstone after an entity enters it");
      });
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void contact_control_shapes_match_their_model_families(GameTestHelper helper) {
      BlockState contactMat = (BlockState)((BlockState)control("industrial_contact_mat").defaultBlockState().setValue(ControlsBlockTypes.FACING, Direction.UP))
         .setValue(ControlsBlockTypes.POWERED, false);
      BlockState trapdoorClosed = (BlockState)((BlockState)control("industrial_high_sensitive_trapdoor")
            .defaultBlockState()
            .setValue(ControlsBlockTypes.FACING, Direction.UP))
         .setValue(ControlsBlockTypes.POWERED, false);
      BlockState trapdoorOpen = (BlockState)trapdoorClosed.setValue(ControlsBlockTypes.POWERED, true);
      BlockState fallthroughFrame = (BlockState)((BlockState)control("industrial_fallthrough_detector")
            .defaultBlockState()
            .setValue(ControlsBlockTypes.FACING, Direction.UP))
         .setValue(ControlsBlockTypes.POWERED, false);
      BlockState powerPlant = (BlockState)((BlockState)control("red_power_plant").defaultBlockState().setValue(ControlsBlockTypes.FACING, Direction.UP))
         .setValue(ControlsBlockTypes.POWERED, false);
      assertThinFullPanel(helper, contactMat, "industrial contact mat");
      assertTopPanel(helper, trapdoorClosed, "industrial high sensitive trapdoor");
      assertOpenTrapdoorPanel(helper, trapdoorOpen, "powered trapdoor");
      assertEmptyCollision(helper, fallthroughFrame, "fall-through detector should not block entities");
      assertPlantShape(helper, powerPlant, "red power plant");
      assertEmptyCollision(helper, powerPlant, "red power plant should not block movement");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void floor_contact_controls_reject_wall_and_ceiling_placement(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      helper.setBlock(TEST_POS.below(), Blocks.STONE.defaultBlockState());
      helper.setBlock(TEST_POS.west(), Blocks.STONE.defaultBlockState());
      helper.setBlock(TEST_POS.above(), Blocks.STONE.defaultBlockState());

      for (String name : List.of("industrial_contact_mat", "industrial_high_sensitive_trapdoor", "industrial_fallthrough_detector", "red_power_plant")) {
         Block block = control(name);
         BlockState floorState = placementState(helper, player, block, TEST_POS.below(), Direction.UP);
         helper.assertTrue(floorState != null, name + " should place from the top face of a floor support");
         helper.assertTrue(floorState.getValue(ControlsBlockTypes.FACING) == Direction.UP, name + " should keep floor-facing collision when floor placed");
         helper.assertTrue(placementState(helper, player, block, TEST_POS.west(), Direction.EAST) == null, name + " should reject wall placement");
         helper.assertTrue(placementState(helper, player, block, TEST_POS.above(), Direction.DOWN) == null, name + " should reject ceiling placement");
      }

      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void attached_control_blocks_drop_when_support_is_removed(GameTestHelper helper) {
      assertAttachedControlDrops(helper, "industrial_lever", new BlockPos(2, 2, 2), Direction.EAST);
      assertAttachedControlDrops(helper, "industrial_small_digital_gauge", new BlockPos(2, 2, 5), Direction.EAST);
      assertAttachedControlDrops(helper, "industrial_red_led", new BlockPos(2, 2, 8), Direction.EAST);
      assertAttachedControlDrops(helper, "industrial_dimmer", new BlockPos(2, 2, 11), Direction.EAST);
      assertAttachedControlDrops(helper, "industrial_contact_mat", new BlockPos(5, 1, 2), Direction.UP);
      helper.runAfterDelay(1L, () -> {
         assertAttachedControlMissing(helper, new BlockPos(3, 2, 2), "industrial lever");
         assertAttachedControlMissing(helper, new BlockPos(3, 2, 5), "industrial small digital gauge");
         assertAttachedControlMissing(helper, new BlockPos(3, 2, 8), "industrial red LED");
         assertAttachedControlMissing(helper, new BlockPos(3, 2, 11), "industrial dimmer");
         assertAttachedControlMissing(helper, new BlockPos(5, 2, 2), "industrial contact mat");
         helper.succeed();
      });
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void gauge_and_indicator_follow_attached_block_redstone(GameTestHelper helper) {
      Block gauge = control("industrial_small_digital_gauge");
      Block indicator = control("industrial_red_led");
      BlockPos gaugePos = new BlockPos(1, 1, 1);
      BlockPos indicatorPos = new BlockPos(4, 1, 1);
      helper.setBlock(
         gaugePos,
         (BlockState)((BlockState)gauge.defaultBlockState().setValue(ControlsBlockTypes.FACING, Direction.EAST)).setValue(ControlsBlockTypes.POWER, 0)
      );
      helper.setBlock(
         indicatorPos,
         (BlockState)((BlockState)indicator.defaultBlockState().setValue(ControlsBlockTypes.FACING, Direction.EAST))
            .setValue(ControlsBlockTypes.POWER_BOOL, false)
      );
      helper.setBlock(gaugePos.south(), Blocks.REDSTONE_BLOCK);
      helper.setBlock(indicatorPos.south(), Blocks.REDSTONE_BLOCK);
      helper.runAfterDelay(5L, () -> {
         helper.assertBlockProperty(gaugePos, ControlsBlockTypes.POWER, 0);
         helper.assertBlockProperty(indicatorPos, ControlsBlockTypes.POWER_BOOL, false);
         helper.setBlock(gaugePos.west(), Blocks.REDSTONE_BLOCK);
         helper.setBlock(indicatorPos.west(), Blocks.REDSTONE_BLOCK);
         helper.succeedWhen(() -> {
            helper.assertBlockProperty(gaugePos, ControlsBlockTypes.POWER, 15);
            helper.assertBlockProperty(indicatorPos, ControlsBlockTypes.POWER_BOOL, true);
         });
      });
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void gauge_reads_indirect_power_received_by_attached_block(GameTestHelper helper) {
      Block gauge = control("industrial_small_digital_gauge");
      BlockPos gaugePos = TEST_POS;
      BlockPos backingPos = gaugePos.west();
      BlockPos powerSourcePos = backingPos.west();
      helper.setBlock(backingPos, Blocks.STONE.defaultBlockState());
      helper.setBlock(
         gaugePos,
         (BlockState)((BlockState)gauge.defaultBlockState().setValue(ControlsBlockTypes.FACING, Direction.EAST)).setValue(ControlsBlockTypes.POWER, 0)
      );
      helper.setBlock(powerSourcePos, Blocks.REDSTONE_BLOCK);
      helper.succeedWhen(() -> helper.assertBlockProperty(gaugePos, ControlsBlockTypes.POWER, 15));
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void dimmer_sets_output_strength_from_click_height(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      Block dimmer = control("industrial_dimmer");
      helper.setBlock(
         TEST_POS,
         (BlockState)((BlockState)((BlockState)dimmer.defaultBlockState().setValue(ControlsBlockTypes.FACING, Direction.NORTH))
               .setValue(ControlsBlockTypes.POWERED, false))
            .setValue(ControlsBlockTypes.POWER, 0)
      );
      helper.useBlock(TEST_POS, player);
      helper.assertBlockProperty(TEST_POS, ControlsBlockTypes.POWERED, true);
      helper.assertBlockProperty(TEST_POS, ControlsBlockTypes.POWER, 8);
      helper.assertValueEqual(8, signal(helper), "dimmer should emit its selected output strength");
      int directSignal = helper.getLevel().getDirectSignal(helper.absolutePos(TEST_POS), Direction.NORTH);
      helper.assertTrue(directSignal == 8, "dimmer should expose matching direct redstone signal; direct=" + directSignal);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void floor_mounted_dimmer_uses_panel_axis_for_output_strength(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      Block dimmer = control("industrial_dimmer");
      helper.setBlock(
         TEST_POS,
         (BlockState)((BlockState)((BlockState)dimmer.defaultBlockState().setValue(ControlsBlockTypes.FACING, Direction.UP))
               .setValue(ControlsBlockTypes.POWERED, true))
            .setValue(ControlsBlockTypes.POWER, 8)
      );
      useBlockAt(helper, player, TEST_POS, Direction.UP, 0.5, 0.125, 0.05);
      helper.assertBlockProperty(TEST_POS, ControlsBlockTypes.POWERED, false);
      helper.assertBlockProperty(TEST_POS, ControlsBlockTypes.POWER, 0);
      helper.assertValueEqual(0, signal(helper), "floor-mounted dimmer should reach low output from a low panel-axis click");
      useBlockAt(helper, player, TEST_POS, Direction.UP, 0.5, 0.125, 0.95);
      helper.assertBlockProperty(TEST_POS, ControlsBlockTypes.POWERED, true);
      helper.assertBlockProperty(TEST_POS, ControlsBlockTypes.POWER, 15);
      helper.assertValueEqual(15, signal(helper), "floor-mounted dimmer should reach full output from a high panel-axis click");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void comparator_switch_follows_attached_inventory_signal(GameTestHelper helper) {
      Block comparatorSwitch = control("industrial_comparator_switch");
      BlockPos switchPos = TEST_POS;
      BlockPos chestPos = switchPos.west();
      helper.setBlock(chestPos, Blocks.CHEST.defaultBlockState());
      helper.assertTrue(
         helper.getLevel().getBlockEntity(helper.absolutePos(chestPos)) instanceof Container, "comparator switch test chest should expose a container"
      );
      Container chest = (Container)helper.getLevel().getBlockEntity(helper.absolutePos(chestPos));
      chest.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
      helper.setBlock(
         switchPos,
         (BlockState)((BlockState)comparatorSwitch.defaultBlockState().setValue(ControlsBlockTypes.FACING, Direction.EAST))
            .setValue(ControlsBlockTypes.POWERED, false)
      );
      helper.succeedWhen(() -> {
         helper.assertBlockProperty(switchPos, ControlsBlockTypes.POWERED, true);
         helper.assertValueEqual(15, signal(helper, switchPos), "industrial comparator switch should emit when the attached inventory has comparator output");
      });
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void comparator_switch_reads_indirect_power_received_by_attached_block(GameTestHelper helper) {
      Block comparatorSwitch = control("industrial_comparator_switch");
      BlockPos switchPos = TEST_POS;
      BlockPos backingPos = switchPos.west();
      BlockPos powerSourcePos = backingPos.west();
      helper.setBlock(backingPos, Blocks.STONE.defaultBlockState());
      helper.setBlock(
         switchPos,
         (BlockState)((BlockState)comparatorSwitch.defaultBlockState().setValue(ControlsBlockTypes.FACING, Direction.EAST))
            .setValue(ControlsBlockTypes.POWERED, false)
      );
      helper.setBlock(powerSourcePos, Blocks.REDSTONE_BLOCK.defaultBlockState());
      helper.succeedWhen(() -> {
         helper.assertBlockProperty(switchPos, ControlsBlockTypes.POWERED, true);
         helper.assertValueEqual(
            15,
            signal(helper, switchPos),
            "industrial comparator switch should emit when its attached block receives indirect redstone power"
         );
      });
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void comparator_switch_reads_directional_power_received_by_attached_block(GameTestHelper helper) {
      Block comparatorSwitch = control("industrial_comparator_switch");
      BlockPos switchPos = TEST_POS;
      BlockPos backingPos = switchPos.west();
      BlockPos repeaterPos = backingPos.west();
      helper.setBlock(backingPos, Blocks.STONE.defaultBlockState());
      helper.setBlock(
         switchPos,
         (BlockState)((BlockState)comparatorSwitch.defaultBlockState().setValue(ControlsBlockTypes.FACING, Direction.EAST))
            .setValue(ControlsBlockTypes.POWERED, false)
      );
      helper.setBlock(
         repeaterPos,
         (BlockState)((BlockState)Blocks.REPEATER.defaultBlockState().setValue(RepeaterBlock.FACING, Direction.EAST))
            .setValue(RepeaterBlock.POWERED, true)
      );
      helper.succeedWhen(() -> {
         helper.assertBlockProperty(switchPos, ControlsBlockTypes.POWERED, true);
         helper.assertValueEqual(
            15,
            signal(helper, switchPos),
            "industrial comparator switch should emit when directional redstone powers its attached block"
         );
      });
   }

   @GameTest(template = "empty", timeoutTicks = 120)
   public static void comparator_switch_does_not_latch_from_own_output(GameTestHelper helper) {
      Block comparatorSwitch = control("industrial_comparator_switch");
      BlockPos switchPos = TEST_POS;
      BlockPos backingPos = switchPos.west();
      BlockPos powerSourcePos = backingPos.west();
      helper.setBlock(backingPos, Blocks.STONE.defaultBlockState());
      helper.setBlock(powerSourcePos, Blocks.REDSTONE_BLOCK.defaultBlockState());
      helper.setBlock(
         switchPos,
         (BlockState)((BlockState)comparatorSwitch.defaultBlockState().setValue(ControlsBlockTypes.FACING, Direction.EAST))
            .setValue(ControlsBlockTypes.POWERED, false)
      );
      helper.runAfterDelay(25L, () -> {
         helper.assertBlockProperty(switchPos, ControlsBlockTypes.POWERED, true);
         helper.setBlock(powerSourcePos, Blocks.AIR.defaultBlockState());
         helper.runAfterDelay(30L, () -> {
            helper.assertBlockProperty(switchPos, ControlsBlockTypes.POWERED, false);
            helper.assertValueEqual(0, signal(helper, switchPos), "industrial comparator switch should turn off after the external backing signal is removed");
            helper.succeed();
         });
      });
   }

   @GameTest(template = "empty", timeoutTicks = 100)
   public static void switchlink_pearl_links_and_toggles_receiver(GameTestHelper helper) {
      Block receiver = control("industrial_switchlink_receiver");
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      BlockPos receiverPos = TEST_POS;
      BlockPos absoluteReceiverPos = helper.absolutePos(receiverPos);
      player.moveTo(absoluteReceiverPos.getX() + 0.5, absoluteReceiverPos.getY() + 1.0, absoluteReceiverPos.getZ() + 0.5);
      helper.setBlock(
         receiverPos,
         (BlockState)((BlockState)receiver.defaultBlockState().setValue(ControlsBlockTypes.FACING, Direction.NORTH))
            .setValue(ControlsBlockTypes.POWERED, false)
      );
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ENDER_PEARL));
      helper.useBlock(receiverPos, player);
      ItemStack linkedPearl = player.getItemInHand(InteractionHand.MAIN_HAND);
      helper.assertTrue(
         linkedPearl.is((Item)ControlsModule.SWITCHLINK_PEARL.get()), "using an ender pearl on a switch-link receiver should create a linked Switch Link Pearl"
      );
      player.setShiftKeyDown(true);
      linkedPearl.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertBlockProperty(receiverPos, ControlsBlockTypes.POWERED, true);
      helper.assertValueEqual(15, signal(helper, receiverPos), "linked pearl should toggle the linked receiver on and make it emit redstone");
      linkedPearl.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertBlockProperty(receiverPos, ControlsBlockTypes.POWERED, false);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 100)
   public static void switchlink_pearl_triggers_pulse_receiver_then_resets(GameTestHelper helper) {
      Block pulseReceiver = control("industrial_switchlink_pulse_receiver");
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      BlockPos receiverPos = TEST_POS;
      BlockPos absoluteReceiverPos = helper.absolutePos(receiverPos);
      player.moveTo(absoluteReceiverPos.getX() + 0.5, absoluteReceiverPos.getY() + 1.0, absoluteReceiverPos.getZ() + 0.5);
      helper.setBlock(
         receiverPos,
         (BlockState)((BlockState)pulseReceiver.defaultBlockState().setValue(ControlsBlockTypes.FACING, Direction.NORTH))
            .setValue(ControlsBlockTypes.POWERED, false)
      );
      ItemStack linkedPearl = SwitchLinkPearlItem.linkedTo(helper.getLevel(), absoluteReceiverPos);
      player.setItemInHand(InteractionHand.MAIN_HAND, linkedPearl);
      player.setShiftKeyDown(true);
      linkedPearl.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
      helper.assertBlockProperty(receiverPos, ControlsBlockTypes.POWERED, true);
      helper.assertValueEqual(15, signal(helper, receiverPos), "linked pearl should pulse the linked pulse receiver on");
      helper.runAfterDelay(35L, () -> {
         helper.assertBlockProperty(receiverPos, ControlsBlockTypes.POWERED, false);
         helper.assertValueEqual(0, signal(helper, receiverPos), "linked pulse receiver should reset after its scheduled pulse time");
         helper.succeed();
      });
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void switchlink_pearl_block_use_requires_sneaking(GameTestHelper helper) {
      Block receiver = control("industrial_switchlink_receiver");
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      BlockPos receiverPos = TEST_POS;
      BlockPos clickPos = TEST_POS.east();
      BlockPos absoluteReceiverPos = helper.absolutePos(receiverPos);
      player.moveTo(absoluteReceiverPos.getX() + 0.5, absoluteReceiverPos.getY() + 1.0, absoluteReceiverPos.getZ() + 0.5);
      helper.setBlock(
         receiverPos,
         (BlockState)((BlockState)receiver.defaultBlockState().setValue(ControlsBlockTypes.FACING, Direction.NORTH))
            .setValue(ControlsBlockTypes.POWERED, false)
      );
      helper.setBlock(clickPos, Blocks.STONE.defaultBlockState());
      ItemStack linkedPearl = SwitchLinkPearlItem.linkedTo(helper.getLevel(), absoluteReceiverPos);
      player.setItemInHand(InteractionHand.MAIN_HAND, linkedPearl);
      InteractionResult plainResult = linkedPearl.getItem().useOn(useOnContext(helper, player, clickPos));
      helper.assertTrue(plainResult == InteractionResult.PASS, "non-sneak block use with a linked pearl should pass through instead of triggering the receiver");
      helper.assertBlockProperty(receiverPos, ControlsBlockTypes.POWERED, false);
      player.setShiftKeyDown(true);
      InteractionResult sneakingResult = linkedPearl.getItem().useOn(useOnContext(helper, player, clickPos));
      helper.assertTrue(sneakingResult == InteractionResult.SUCCESS, "sneak block use with a linked pearl should trigger the receiver");
      helper.assertBlockProperty(receiverPos, ControlsBlockTypes.POWERED, true);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void switchlink_pearl_tooltip_reports_linked_target(GameTestHelper helper) {
      Block receiver = control("industrial_switchlink_receiver");
      BlockPos receiverPos = TEST_POS;
      helper.setBlock(
         receiverPos,
         (BlockState)((BlockState)receiver.defaultBlockState().setValue(ControlsBlockTypes.FACING, Direction.NORTH))
            .setValue(ControlsBlockTypes.POWERED, false)
      );
      ItemStack linkedPearl = SwitchLinkPearlItem.linkedTo(helper.getLevel(), helper.absolutePos(receiverPos));
      List<Component> tooltip = new ArrayList<>();
      linkedPearl.getItem().appendHoverText(linkedPearl, TooltipContext.of(helper.getLevel()), tooltip, TooltipFlag.NORMAL);
      helper.assertTrue(tooltip.size() >= 2, "linked Switch Link Pearl tooltip should include target name and coordinates");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void switchlink_pearl_ignores_malformed_saved_target_data(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      BlockPos clickPos = TEST_POS;
      helper.setBlock(clickPos, Blocks.STONE.defaultBlockState());
      ItemStack malformedPearl = new ItemStack((Item)ControlsModule.SWITCHLINK_PEARL.get());
      CompoundTag malformed = new CompoundTag();
      malformed.putString("target_dimension", helper.getLevel().dimension().location().toString());
      malformed.putString("target_x", "not-a-number");
      malformed.putString("target_y", "not-a-number");
      malformed.putString("target_z", "not-a-number");
      malformedPearl.set(DataComponents.CUSTOM_DATA, CustomData.of(malformed));
      player.setItemInHand(InteractionHand.MAIN_HAND, malformedPearl);
      player.setShiftKeyDown(true);
      InteractionResult result = malformedPearl.getItem().useOn(useOnContext(helper, player, clickPos));
      helper.assertTrue(result == InteractionResult.PASS, "malformed Switch Link Pearl data should behave as an unlinked pearl instead of targeting the origin");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void cased_switchlink_receivers_are_full_blocks_not_surface_mounts(GameTestHelper helper) {
      assertCasedSwitchLinkFullBlock(helper, "industrial_switchlink_cased_receiver", TEST_POS);
      assertCasedSwitchLinkFullBlock(helper, "industrial_switchlink_cased_pulse_receiver", TEST_POS.offset(3, 0, 0));
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void elevator_button_placement_selects_click_height_variant(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      Block elevatorButton = control("elevator_button");
      assertElevatorPlacementVariant(helper, player, elevatorButton, 0.2, 1, "low click should use down-arrow variant");
      assertElevatorPlacementVariant(helper, player, elevatorButton, 0.5, 0, "middle click should use both-arrow variant");
      assertElevatorPlacementVariant(helper, player, elevatorButton, 0.8, 2, "high click should use up-arrow variant");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void sensitive_glass_placement_reads_existing_redstone_power(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      Block sensitiveGlass = control("sensitive_glass_block");
      BlockPos absolutePos = helper.absolutePos(TEST_POS);
      BlockHitResult hit = new BlockHitResult(
         new Vec3(absolutePos.getX() + 0.5, absolutePos.getY() + 0.5, absolutePos.getZ() + 0.5), Direction.UP, absolutePos, false
      );
      BlockPlaceContext context = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, new ItemStack(sensitiveGlass), hit);
      helper.getLevel().setBlock(context.getClickedPos().east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
      helper.assertTrue(
         helper.getLevel().getBestNeighborSignal(context.getClickedPos()) > 0, "sensitive glass test fixture should place the block beside active redstone"
      );
      BlockState state = sensitiveGlass.getStateForPlacement(context);
      helper.assertTrue(state != null, "sensitive glass placement state should not be null");
      helper.assertTrue((Boolean)state.getValue(ControlsBlockTypes.POWERED), "sensitive glass should initialize powered beside active redstone");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void block_sensor_placement_reads_existing_target_block(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      Block blockDetector = control("industrial_block_detector");
      BlockPos backingPos = helper.absolutePos(TEST_POS.west());
      helper.setBlock(TEST_POS.west(), Blocks.STONE.defaultBlockState());
      helper.setBlock(TEST_POS.east(), Blocks.STONE.defaultBlockState());
      BlockHitResult hit = new BlockHitResult(
         new Vec3(backingPos.getX() + 1.0, backingPos.getY() + 0.5, backingPos.getZ() + 0.5), Direction.EAST, backingPos, false
      );
      BlockPlaceContext context = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, new ItemStack(blockDetector), hit);
      helper.assertTrue(
         context.getClickedPos().equals(helper.absolutePos(TEST_POS)), "block detector test fixture should resolve placement at the test position"
      );
      BlockState state = blockDetector.getStateForPlacement(context);
      helper.assertTrue(state != null, "block detector placement state should not be null");
      helper.assertTrue(
         (Boolean)state.getValue(ControlsBlockTypes.POWERED), "block detector should initialize powered when its target side already contains a block"
      );
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void sensor_click_refreshes_instead_of_manual_toggle(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      Block blockDetector = control("industrial_block_detector");
      BlockPos switchPos = TEST_POS;
      helper.setBlock(switchPos.east(), Blocks.STONE.defaultBlockState());
      helper.setBlock(
         switchPos,
         (BlockState)((BlockState)blockDetector.defaultBlockState().setValue(ControlsBlockTypes.FACING, Direction.EAST))
            .setValue(ControlsBlockTypes.POWERED, true)
      );
      helper.useBlock(switchPos, player);
      helper.assertBlockProperty(switchPos, ControlsBlockTypes.POWERED, true);
      helper.assertValueEqual(15, signal(helper, switchPos), "sensor interaction should refresh from measured state instead of toggling off manually");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void door_sensor_switch_detects_nearby_player(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      Block doorSensor = control("door_sensor_switch");
      BlockPos absolutePos = helper.absolutePos(TEST_POS);
      player.moveTo(absolutePos.getX() + 0.5, absolutePos.getY() + 0.5, absolutePos.getZ() + 0.5);
      helper.getLevel().addFreshEntity(player);
      helper.setBlock(
         TEST_POS,
         (BlockState)((BlockState)doorSensor.defaultBlockState().setValue(ControlsBlockTypes.FACING, Direction.NORTH))
            .setValue(ControlsBlockTypes.POWERED, false)
      );
      helper.succeedWhen(() -> {
         helper.assertBlockProperty(TEST_POS, ControlsBlockTypes.POWERED, true);
         helper.assertValueEqual(15, signal(helper, TEST_POS), "door sensor switch should emit when a player is inside its detection range");
      });
   }

   @GameTest(template = "empty", timeoutTicks = 100)
   public static void linear_entity_detector_only_reads_forward_corridor(GameTestHelper helper) {
      Block detector = control("industrial_linear_entity_detector");
      BlockPos detectorPos = TEST_POS;
      helper.setBlock(
         detectorPos,
         (BlockState)((BlockState)detector.defaultBlockState().setValue(ControlsBlockTypes.FACING, Direction.EAST)).setValue(ControlsBlockTypes.POWERED, false)
      );
      helper.spawn(EntityType.ARMOR_STAND, detectorPos.north(2));
      helper.runAfterDelay(45L, () -> {
         helper.assertBlockProperty(detectorPos, ControlsBlockTypes.POWERED, false);
         helper.assertValueEqual(0, signal(helper, detectorPos), "linear entity detector should ignore entities outside its forward corridor");
         helper.spawn(EntityType.ARMOR_STAND, detectorPos.east(3));
         helper.succeedWhen(() -> {
            helper.assertBlockProperty(detectorPos, ControlsBlockTypes.POWERED, true);
            helper.assertValueEqual(15, signal(helper, detectorPos), "linear entity detector should emit when an entity is in front of it");
         });
      });
   }

   private static int signal(GameTestHelper helper) {
      return signal(helper, TEST_POS);
   }

   private static int signal(GameTestHelper helper, BlockPos pos) {
      return helper.getLevel().getSignal(helper.absolutePos(pos), Direction.NORTH);
   }

   private static UseOnContext useOnContext(GameTestHelper helper, Player player, BlockPos localPos) {
      BlockPos absolutePos = helper.absolutePos(localPos);
      Vec3 hitLocation = Vec3.atCenterOf(absolutePos);
      BlockHitResult hit = new BlockHitResult(hitLocation, Direction.UP, absolutePos, false);
      return new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
   }

   private static InteractionResult useBlockAt(
      GameTestHelper helper, Player player, BlockPos localPos, Direction clickedFace, double localX, double localY, double localZ
   ) {
      BlockPos absolutePos = helper.absolutePos(localPos);
      Vec3 hitLocation = new Vec3(absolutePos.getX() + localX, absolutePos.getY() + localY, absolutePos.getZ() + localZ);
      BlockHitResult hit = new BlockHitResult(hitLocation, clickedFace, absolutePos, false);
      return helper.getBlockState(localPos).useWithoutItem(helper.getLevel(), player, hit);
   }

   private static BlockState placementState(GameTestHelper helper, Player player, Block block, BlockPos supportPos, Direction face) {
      BlockPos absoluteSupportPos = helper.absolutePos(supportPos);
      Vec3 hitLocation = new Vec3(
         absoluteSupportPos.getX() + 0.5 + face.getNormal().getX() * 0.5,
         absoluteSupportPos.getY() + 0.5 + face.getNormal().getY() * 0.5,
         absoluteSupportPos.getZ() + 0.5 + face.getNormal().getZ() * 0.5
      );
      BlockHitResult hit = new BlockHitResult(hitLocation, face, absoluteSupportPos, false);
      BlockPlaceContext context = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, new ItemStack(block), hit);
      return block.getStateForPlacement(context);
   }

   private static void assertElevatorPlacementVariant(GameTestHelper helper, Player player, Block block, double localY, int expectedVariant, String message) {
      BlockPos absolutePos = helper.absolutePos(TEST_POS);
      Vec3 hitLocation = new Vec3(absolutePos.getX() + 0.5, absolutePos.getY() + localY, absolutePos.getZ() + 0.5);
      BlockHitResult hit = new BlockHitResult(hitLocation, Direction.NORTH, absolutePos, false);
      BlockPlaceContext context = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, new ItemStack(block), hit);
      BlockState state = block.getStateForPlacement(context);
      helper.assertTrue(state != null, "elevator button placement state should not be null");
      helper.assertValueEqual(expectedVariant, (Integer)state.getValue(ControlsBlockTypes.VARIANT), message);
   }

   private static void assertPulseControl(GameTestHelper helper, Player player, String name) {
      BlockPos pos = "industrial_button".equals(name) ? new BlockPos(1, 1, 1) : new BlockPos(2, 1, 1);
      Block block = control(name);
      BlockState offState = (BlockState)((BlockState)block.defaultBlockState().setValue(ControlsBlockTypes.FACING, Direction.NORTH))
         .setValue(ControlsBlockTypes.POWERED, false);
      helper.setBlock(pos.relative(Direction.SOUTH), Blocks.STONE.defaultBlockState());
      helper.setBlock(pos, offState);
      helper.useBlock(pos, player);
      helper.assertBlockProperty(pos, ControlsBlockTypes.POWERED, true);
   }

   private static void assertPulseReset(GameTestHelper helper, String name, BlockPos pos) {
      helper.assertBlockProperty(pos, ControlsBlockTypes.POWERED, false);
      helper.assertValueEqual(
         0, helper.getLevel().getSignal(helper.absolutePos(pos), Direction.NORTH), name + " should return to redstone output 0 after scheduled pulse reset"
      );
   }

   private static void assertAttachedControlDrops(GameTestHelper helper, String name, BlockPos supportPos, Direction facing) {
      Block block = control(name);
      BlockPos controlPos = supportPos.relative(facing);
      helper.setBlock(supportPos, Blocks.STONE.defaultBlockState());
      helper.setBlock(controlPos, (BlockState)block.defaultBlockState().setValue(ControlsBlockTypes.FACING, facing));
      helper.assertTrue(helper.getBlockState(controlPos).is(block), name + " should start attached to its support");
      helper.setBlock(supportPos, Blocks.AIR.defaultBlockState());
   }

   private static void assertAttachedControlMissing(GameTestHelper helper, BlockPos pos, String name) {
      helper.assertTrue(helper.getBlockState(pos).isAir(), name + " should drop when its supporting face is removed");
   }

   private static void assertThinFullPanel(GameTestHelper helper, BlockState state, String name) {
      AABB bounds = selectionBounds(helper, state);
      helper.assertTrue(bounds.maxX - bounds.minX >= 0.95, name + " selection should span the block width");
      helper.assertTrue(bounds.maxZ - bounds.minZ >= 0.95, name + " selection should span the block depth");
      helper.assertTrue(bounds.maxY - bounds.minY <= 0.07, name + " selection should be a thin floor panel");
      helper.assertTrue(!collisionShape(helper, state).isEmpty(), name + " should provide a thin walkable collision plate");
   }

   private static void assertTopPanel(GameTestHelper helper, BlockState state, String name) {
      AABB bounds = selectionBounds(helper, state);
      helper.assertTrue(bounds.maxX - bounds.minX >= 0.95, name + " selection should span the block width");
      helper.assertTrue(bounds.maxZ - bounds.minZ >= 0.95, name + " selection should span the block depth");
      helper.assertTrue(bounds.minY >= 0.85, name + " selection should match the top trapdoor panel");
      helper.assertTrue(!collisionShape(helper, state).isEmpty(), name + " should block movement while closed");
   }

   private static void assertOpenTrapdoorPanel(GameTestHelper helper, BlockState state, String name) {
      AABB bounds = selectionBounds(helper, state);
      helper.assertTrue(bounds.maxX - bounds.minX >= 0.95, name + " selection should span the open plate width");
      helper.assertTrue(bounds.maxY - bounds.minY >= 0.95, name + " selection should span the open plate height");
      helper.assertTrue(bounds.maxZ - bounds.minZ <= 0.07, name + " selection should match the vertical open panel");
      helper.assertTrue(!collisionShape(helper, state).isEmpty(), name + " should keep collision on its visible open panel");
   }

   private static void assertPlantShape(GameTestHelper helper, BlockState state, String name) {
      AABB bounds = selectionBounds(helper, state);
      helper.assertTrue(bounds.maxX - bounds.minX < 0.9, name + " selection should stay plant-sized");
      helper.assertTrue(bounds.maxZ - bounds.minZ < 0.9, name + " selection should stay plant-sized");
      helper.assertTrue(bounds.maxY - bounds.minY > 0.5, name + " selection should cover the visible plant height");
   }

   private static void assertEmptyCollision(GameTestHelper helper, BlockState state, String message) {
      helper.assertTrue(collisionShape(helper, state).isEmpty(), message);
   }

   private static void assertCasedSwitchLinkFullBlock(GameTestHelper helper, String name, BlockPos pos) {
      Block block = control(name);
      BlockPos supportPos = pos.west();
      helper.setBlock(supportPos, Blocks.STONE.defaultBlockState());
      helper.setBlock(pos, (BlockState)block.defaultBlockState().setValue(ControlsBlockTypes.FACING, Direction.EAST));
      helper.setBlock(supportPos, Blocks.AIR.defaultBlockState());
      helper.assertTrue(helper.getBlockState(pos).is(block), name + " should survive after adjacent backing support is removed");
      AABB bounds = helper.getBlockState(pos).getCollisionShape(helper.getLevel(), helper.absolutePos(pos)).bounds();
      helper.assertTrue(
         bounds.minX == 0.0
            && bounds.minY == 0.0
            && bounds.minZ == 0.0
            && bounds.maxX == 1.0
            && bounds.maxY == 1.0
            && bounds.maxZ == 1.0,
         name + " should use full-block collision; actual bounds=" + bounds
      );
   }

   private static AABB selectionBounds(GameTestHelper helper, BlockState state) {
      return state.getShape(helper.getLevel(), helper.absolutePos(TEST_POS)).bounds();
   }

   private static VoxelShape collisionShape(GameTestHelper helper, BlockState state) {
      return state.getCollisionShape(helper.getLevel(), helper.absolutePos(TEST_POS));
   }

   private static Block control(String name) {
      for (DeferredBlock<? extends Block> entry : ControlsModule.CONTROLS) {
         if (entry.getId().getPath().equals(name)) {
            return (Block)entry.get();
         }
      }

      throw new IllegalArgumentException("Unknown control block " + name);
   }
}
