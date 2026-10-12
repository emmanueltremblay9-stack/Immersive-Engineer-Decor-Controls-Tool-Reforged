package com.oblixorprime.engineersdecorreforged.gametest;

import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class SynchronizedAccesswayGameTests {
   private static final BlockPos FIRST = new BlockPos(3, 1, 3);

   private SynchronizedAccesswayGameTests() {
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_door_clicks_synchronize_each_facing_and_either_half(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Block door : List.of(Blocks.OAK_DOOR, Blocks.COPPER_DOOR, ModBlocks.OLD_INDUSTRIAL_WOOD_DOOR.get(),
         SynchronizedAccesswayTestBlocks.INHERITED_DOOR)) {
         for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (int clickedHalf = 0; clickedHalf <= 1; clickedHalf++) {
               clear(helper);
               BlockPos second = FIRST.relative(facing.getClockWise());
               placeDoor(helper, FIRST, door, facing, DoorHingeSide.LEFT);
               placeDoor(helper, second, door, facing, DoorHingeSide.RIGHT);
               click(helper, player, FIRST.above(clickedHalf), facing);
               assertDoor(helper, FIRST, true);
               assertDoor(helper, second, true);
               click(helper, player, second.above(1 - clickedHalf), facing);
               assertDoor(helper, FIRST, false);
               assertDoor(helper, second, false);
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_fence_gate_pairs_open_from_both_sides_all_facings(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Block gate : List.of(Blocks.OAK_FENCE_GATE, Blocks.BAMBOO_FENCE_GATE)) {
         for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (Direction playerFacing : List.of(facing, facing.getOpposite())) {
               clear(helper);
               BlockPos second = FIRST.relative(facing.getClockWise());
               placeGate(helper, FIRST, gate, facing, 1);
               placeGate(helper, second, gate, facing, 1);
               click(helper, player, FIRST, playerFacing);
               assertOpen(helper, FIRST, true);
               assertOpen(helper, second, true);
               click(helper, player, second, playerFacing);
               assertOpen(helper, FIRST, false);
               assertOpen(helper, second, false);
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void steel_gate_pairs_normalize_each_segment_and_height(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         for (int height = 1; height <= 2; height++) {
            for (int segment = 0; segment < height; segment++) {
               clear(helper);
               BlockPos second = FIRST.relative(facing.getClockWise());
               placeGate(helper, FIRST, ModBlocks.STEEL_MESH_FENCE_GATE.get(), facing, height);
               placeGate(helper, second, ModBlocks.STEEL_MESH_FENCE_GATE.get(), facing, height);
               click(helper, player, FIRST.above(segment), facing);
               for (int y = 0; y < height; y++) {
                  assertOpen(helper, FIRST.above(y), true);
                  assertOpen(helper, second.above(y), true);
               }
               click(helper, player, second.above(segment), facing);
               for (int y = 0; y < height; y++) {
                  assertOpen(helper, FIRST.above(y), false);
                  assertOpen(helper, second.above(y), false);
               }
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void ambiguous_gate_rows_never_chain_from_any_member(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Block gate : List.of(Blocks.OAK_FENCE_GATE, ModBlocks.STEEL_MESH_FENCE_GATE.get())) {
         for (int clicked = 0; clicked < 3; clicked++) {
            clear(helper);
            for (int i = 0; i < 3; i++) {
               placeGate(helper, FIRST.east(i), gate, Direction.NORTH, 1);
            }
            click(helper, player, FIRST.east(clicked), Direction.NORTH);
            for (int i = 0; i < 3; i++) {
               assertOpen(helper, FIRST.east(i), i == clicked);
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void doors_pair_only_reciprocal_hinges_of_same_type_and_plane(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (int mismatch = 0; mismatch < 5; mismatch++) {
         clear(helper);
         placeDoor(helper, FIRST, Blocks.OAK_DOOR, Direction.NORTH, DoorHingeSide.LEFT);
         BlockPos second = mismatch == 3 ? FIRST.north() : FIRST.east();
         placeDoor(helper, second, mismatch == 0 ? Blocks.SPRUCE_DOOR : Blocks.OAK_DOOR,
            mismatch == 1 ? Direction.SOUTH : Direction.NORTH, mismatch == 2 ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT);
         if (mismatch == 4) {
            helper.getLevel().setBlock(helper.absolutePos(second.above()), Blocks.AIR.defaultBlockState(), 2);
         }
         click(helper, player, FIRST, Direction.NORTH);
         assertDoor(helper, FIRST, true);
         if (mismatch != 4) {
            assertDoor(helper, second, false);
         }
      }
      clear(helper);
      for (int i = 0; i < 3; i++) {
         placeDoor(helper, FIRST.east(i), Blocks.OAK_DOOR, Direction.NORTH, i == 1 ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT);
      }
      click(helper, player, FIRST, Direction.NORTH);
      assertDoor(helper, FIRST, true);
      assertDoor(helper, FIRST.east(), true);
      assertDoor(helper, FIRST.east(2), false);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void iron_and_locked_source_or_companion_are_not_forced(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      clear(helper);
      doorPair(helper, Blocks.IRON_DOOR);
      click(helper, player, FIRST, Direction.NORTH);
      assertDoor(helper, FIRST, false);
      assertDoor(helper, FIRST.east(), false);
      for (boolean lockSource : List.of(true, false)) {
         clear(helper);
         doorPair(helper, SynchronizedAccesswayTestBlocks.LOCKABLE_DOOR);
         BlockPos locked = lockSource ? FIRST : FIRST.east();
         for (int y = 0; y <= 1; y++) {
            helper.setBlock(locked.above(y), helper.getBlockState(locked.above(y)).setValue(SynchronizedAccesswayTestBlocks.LOCKED, true));
         }
         click(helper, player, FIRST, Direction.NORTH);
         assertDoor(helper, FIRST, !lockSource);
         assertDoor(helper, FIRST.east(), false);
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_item_refusal_never_falls_through_or_places_held_block(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (boolean refuseSource : List.of(true, false)) {
         clear(helper);
         doorPair(helper, SynchronizedAccesswayTestBlocks.LOCKABLE_DOOR);
         BlockPos refused = refuseSource ? FIRST : FIRST.east();
         for (int y = 0; y <= 1; y++) {
            helper.setBlock(refused.above(y), helper.getBlockState(refused.above(y)).setValue(SynchronizedAccesswayTestBlocks.REFUSE_ITEM, true));
         }
         player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE, 7));
         click(helper, player, FIRST, Direction.NORTH);
         assertDoor(helper, FIRST, !refuseSource);
         assertDoor(helper, FIRST.east(), false);
         if (!refuseSource) {
            helper.assertTrue(helper.getBlockState(FIRST.east().north()).isAir(), "refused companion must not run held-block placement fallback");
            helper.assertValueEqual(player.getMainHandItem().getCount(), 7, "companion must not consume held items");
         }
         player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void source_and_target_right_click_denials_are_respected(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (boolean denySource : List.of(true, false)) {
         for (boolean cancel : List.of(true, false)) {
            clear(helper);
            doorPair(helper, Blocks.OAK_DOOR);
            BlockPos denied = helper.absolutePos(denySource ? FIRST : FIRST.east());
            Consumer<PlayerInteractEvent.RightClickBlock> listener = event -> {
               if (event.getEntity() == player && event.getPos().equals(denied)) {
                  if (cancel) {
                     event.setCanceled(true);
                     event.setCancellationResult(InteractionResult.FAIL);
                  } else {
                     event.setUseBlock(TriState.FALSE);
                  }
               }
            };
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, PlayerInteractEvent.RightClickBlock.class, listener);
            try {
               click(helper, player, FIRST, Direction.NORTH);
               assertDoor(helper, FIRST, !denySource);
               assertDoor(helper, FIRST.east(), false);
            } finally {
               NeoForge.EVENT_BUS.unregister(listener);
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void source_and_target_block_phase_denials_are_respected(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (boolean denySource : List.of(true, false)) {
         for (ItemInteractionResult refusal : List.of(ItemInteractionResult.FAIL, ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION)) {
            clear(helper);
            doorPair(helper, Blocks.OAK_DOOR);
            BlockPos denied = helper.absolutePos(denySource ? FIRST : FIRST.east());
            Consumer<UseItemOnBlockEvent> listener = event -> {
               if (event.getPlayer() == player && event.getPos().equals(denied) && event.getUsePhase() == UseItemOnBlockEvent.UsePhase.BLOCK) {
                  event.cancelWithResult(refusal);
               }
            };
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, UseItemOnBlockEvent.class, listener);
            try {
               click(helper, player, FIRST, Direction.NORTH);
               assertDoor(helper, FIRST, !denySource);
               assertDoor(helper, FIRST.east(), false);
            } finally {
               NeoForge.EVENT_BUS.unregister(listener);
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void offhand_sneak_and_nonchanging_interactions_do_not_sync(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      clear(helper);
      doorPair(helper, Blocks.OAK_DOOR);
      use(helper, player, FIRST, Direction.NORTH, InteractionHand.OFF_HAND);
      assertDoor(helper, FIRST, false);
      assertDoor(helper, FIRST.east(), false);
      player.setShiftKeyDown(true);
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
      click(helper, player, FIRST, Direction.NORTH);
      assertDoor(helper, FIRST, false);
      assertDoor(helper, FIRST.east(), false);
      player.setShiftKeyDown(false);
      player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
      Consumer<UseItemOnBlockEvent> listener = event -> {
         if (event.getPlayer() == player && event.getUsePhase() == UseItemOnBlockEvent.UsePhase.BLOCK) {
            event.cancelWithResult(ItemInteractionResult.SUCCESS);
         }
      };
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, UseItemOnBlockEvent.class, listener);
      try {
         click(helper, player, FIRST, Direction.NORTH);
         assertDoor(helper, FIRST, false);
         assertDoor(helper, FIRST.east(), false);
      } finally {
         NeoForge.EVENT_BUS.unregister(listener);
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void already_matching_companion_is_not_toggled_again(GameTestHelper helper) {
      clear(helper);
      doorPair(helper, Blocks.OAK_DOOR);
      for (int y = 0; y <= 1; y++) {
         helper.setBlock(FIRST.east().above(y), helper.getBlockState(FIRST.east().above(y)).setValue(DoorBlock.OPEN, true));
      }
      click(helper, player(helper), FIRST, Direction.NORTH);
      assertDoor(helper, FIRST, true);
      assertDoor(helper, FIRST.east(), true);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void repeated_same_tick_clicks_emit_once_per_leaf_without_loops(GameTestHelper helper) {
      clear(helper);
      doorPair(helper, Blocks.OAK_DOOR);
      ServerPlayer player = player(helper);
      AtomicInteger events = new AtomicInteger();
      Consumer<VanillaGameEvent> listener = event -> {
         if (event.getCause() == player && (event.getVanillaEvent() == GameEvent.BLOCK_OPEN || event.getVanillaEvent() == GameEvent.BLOCK_CLOSE)) {
            events.incrementAndGet();
         }
      };
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, VanillaGameEvent.class, listener);
      try {
         for (int i = 0; i < 12; i++) {
            click(helper, player, i % 2 == 0 ? FIRST : FIRST.east().above(), Direction.NORTH);
            assertDoor(helper, FIRST, i % 2 == 0);
            assertDoor(helper, FIRST.east(), i % 2 == 0);
         }
         helper.assertValueEqual(events.get(), 24, "each click must emit exactly one native event per changed leaf");
      } finally {
         NeoForge.EVENT_BUS.unregister(listener);
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void redstone_changes_remain_local_and_powered_companion_is_untouched(GameTestHelper helper) {
      clear(helper);
      doorPair(helper, Blocks.OAK_DOOR);
      ServerPlayer player = player(helper);
      helper.setBlock(FIRST.north(), Blocks.REDSTONE_BLOCK);
      helper.runAfterDelay(2, () -> {
         assertDoor(helper, FIRST, true);
         assertDoor(helper, FIRST.east(), false);
         helper.assertBlockProperty(FIRST, DoorBlock.POWERED, true);
         click(helper, player, FIRST.east(), Direction.NORTH);
         assertDoor(helper, FIRST.east(), true);
         click(helper, player, FIRST.east(), Direction.NORTH);
         assertDoor(helper, FIRST.east(), false);
         assertDoor(helper, FIRST, true);
         helper.assertBlockProperty(FIRST, DoorBlock.POWERED, true);
         helper.succeed();
      });
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void pair_members_survive_nbt_roundtrip_and_partner_removal(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Block block : List.of(Blocks.OAK_DOOR, Blocks.OAK_FENCE_GATE, ModBlocks.STEEL_MESH_FENCE_GATE.get())) {
         clear(helper);
         if (block instanceof DoorBlock) {
            doorPair(helper, block);
         } else {
            placeGate(helper, FIRST, block, Direction.NORTH, block instanceof FenceGateBlock ? 1 : 2);
            placeGate(helper, FIRST.east(), block, Direction.NORTH, block instanceof FenceGateBlock ? 1 : 2);
         }
         for (BlockPos pos : List.of(FIRST, FIRST.above(), FIRST.east(), FIRST.east().above())) {
            BlockState state = helper.getBlockState(pos);
            helper.setBlock(pos, NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), NbtUtils.writeBlockState(state)));
         }
         click(helper, player, FIRST, Direction.NORTH);
         assertOpen(helper, FIRST, true);
         assertOpen(helper, FIRST.east(), true);
         helper.setBlock(FIRST.east(), Blocks.AIR);
         click(helper, player, FIRST, Direction.NORTH);
         assertOpen(helper, FIRST, false);
         helper.assertTrue(helper.getBlockState(FIRST.east()).isAir(), "removing a companion must not recreate it");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void target_event_replacement_is_not_overwritten_from_stale_snapshot(GameTestHelper helper) {
      clear(helper);
      doorPair(helper, Blocks.OAK_DOOR);
      ServerPlayer player = player(helper);
      BlockPos target = helper.absolutePos(FIRST.east());
      Consumer<PlayerInteractEvent.RightClickBlock> listener = event -> {
         if (event.getEntity() == player && event.getPos().equals(target)) {
            helper.setBlock(FIRST.east(), Blocks.STONE);
         }
      };
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, PlayerInteractEvent.RightClickBlock.class, listener);
      try {
         click(helper, player, FIRST, Direction.NORTH);
         assertDoor(helper, FIRST, true);
         helper.assertBlockPresent(Blocks.STONE, FIRST.east());
      } finally {
         NeoForge.EVENT_BUS.unregister(listener);
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void target_block_hook_opening_does_not_get_toggled_closed_again(GameTestHelper helper) {
      clear(helper);
      doorPair(helper, Blocks.OAK_DOOR);
      ServerPlayer player = player(helper);
      BlockPos target = helper.absolutePos(FIRST.east());
      Consumer<UseItemOnBlockEvent> listener = event -> {
         if (event.getPlayer() == player && event.getPos().equals(target) && event.getUsePhase() == UseItemOnBlockEvent.UsePhase.BLOCK) {
            ((DoorBlock)Blocks.OAK_DOOR).setOpen(player, helper.getLevel(), helper.getLevel().getBlockState(target), target, true);
         }
      };
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, UseItemOnBlockEvent.class, listener);
      try {
         click(helper, player, FIRST, Direction.NORTH);
         assertDoor(helper, FIRST, true);
         assertDoor(helper, FIRST.east(), true);
      } finally {
         NeoForge.EVENT_BUS.unregister(listener);
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void stale_click_context_never_turns_later_programmatic_open_into_pair_click(GameTestHelper helper) {
      clear(helper);
      doorPair(helper, SynchronizedAccesswayTestBlocks.LOCKABLE_DOOR);
      ServerPlayer player = player(helper);
      for (int y = 0; y <= 1; y++) {
         helper.setBlock(FIRST.above(y), helper.getBlockState(FIRST.above(y)).setValue(SynchronizedAccesswayTestBlocks.LOCKED, true));
      }
      click(helper, player, FIRST, Direction.NORTH);
      helper.runAfterDelay(2, () -> {
         SynchronizedAccesswayTestBlocks.LOCKABLE_DOOR.setOpen(player, helper.getLevel(), helper.getBlockState(FIRST), helper.absolutePos(FIRST), true);
         assertDoor(helper, FIRST, true);
         assertDoor(helper, FIRST.east(), false);
         helper.succeed();
      });
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void unchanged_native_item_hooks_keep_normal_held_item_convenience(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (var item : List.of(Items.STICK, Items.STONE)) {
         clear(helper);
         doorPair(helper, Blocks.OAK_DOOR);
         player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, 5));
         click(helper, player, FIRST, Direction.NORTH);
         assertDoor(helper, FIRST, true);
         assertDoor(helper, FIRST.east(), true);
         helper.assertValueEqual(player.getMainHandItem().getCount(), 5, "pair interaction must not consume ordinary held items");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void custom_interaction_overrides_fail_closed_even_when_currently_pass(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Block door : List.of(SynchronizedAccesswayTestBlocks.CUSTOM_ITEM_DOOR, SynchronizedAccesswayTestBlocks.CUSTOM_EMPTY_DOOR)) {
         clear(helper);
         doorPair(helper, door);
         click(helper, player, FIRST, Direction.NORTH);
         assertDoor(helper, FIRST, true);
         assertDoor(helper, FIRST.east(), false);
      }
      clear(helper);
      doorPair(helper, Blocks.OAK_DOOR);
      SynchronizedAccesswayTestBlocks.customItemCalls = 0;
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SynchronizedAccesswayTestBlocks.CUSTOM_FIRST_ITEM));
      click(helper, player, FIRST, Direction.NORTH);
      assertDoor(helper, FIRST, true);
      assertDoor(helper, FIRST.east(), false);
      helper.assertValueEqual(SynchronizedAccesswayTestBlocks.customItemCalls, 1, "custom item hook may run only for the real source click");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void companion_default_pass_cancellations_are_final_even_for_late_listeners(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (UseItemOnBlockEvent.UsePhase phase : List.of(UseItemOnBlockEvent.UsePhase.ITEM_BEFORE_BLOCK, UseItemOnBlockEvent.UsePhase.BLOCK)) {
         for (EventPriority priority : List.of(EventPriority.HIGHEST, EventPriority.LOWEST)) {
            clear(helper);
            doorPair(helper, Blocks.OAK_DOOR);
            BlockPos target = helper.absolutePos(FIRST.east());
            Consumer<UseItemOnBlockEvent> listener = event -> {
               if (event.getPlayer() == player && event.getPos().equals(target) && event.getUsePhase() == phase) {
                  event.setCanceled(true); // Default result is PASS_TO_DEFAULT, not FAIL.
               }
            };
            NeoForge.EVENT_BUS.addListener(priority, false, UseItemOnBlockEvent.class, listener);
            try {
               click(helper, player, FIRST, Direction.NORTH);
               assertDoor(helper, FIRST, true);
               assertDoor(helper, FIRST.east(), false);
            } finally {
               NeoForge.EVENT_BUS.unregister(listener);
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void nested_companion_phase_cannot_hide_an_outer_default_pass_veto(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (EventPriority priority : List.of(EventPriority.HIGHEST, EventPriority.LOWEST)) {
         clear(helper);
         doorPair(helper, Blocks.OAK_DOOR);
         BlockPos target = helper.absolutePos(FIRST.east());
         boolean[] nested = { false };
         Consumer<UseItemOnBlockEvent> listener = event -> {
            if (event.getPlayer() == player && event.getPos().equals(target)
               && event.getUsePhase() == UseItemOnBlockEvent.UsePhase.BLOCK && !nested[0]) {
               nested[0] = true;
               helper.getLevel().getBlockState(target).useItemOn(player.getMainHandItem(), helper.getLevel(), player,
                  InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(target), Direction.NORTH, target, false));
               nested[0] = false;
               event.setCanceled(true);
            }
         };
         NeoForge.EVENT_BUS.addListener(priority, false, UseItemOnBlockEvent.class, listener);
         try {
            click(helper, player, FIRST, Direction.NORTH);
            assertDoor(helper, FIRST, true);
            assertDoor(helper, FIRST.east(), false);
         } finally {
            NeoForge.EVENT_BUS.unregister(listener);
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void cancelled_source_context_cannot_leak_to_same_tick_programmatic_open(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (UseItemOnBlockEvent.UsePhase phase : List.of(UseItemOnBlockEvent.UsePhase.ITEM_BEFORE_BLOCK, UseItemOnBlockEvent.UsePhase.BLOCK)) {
         clear(helper);
         doorPair(helper, Blocks.OAK_DOOR);
         BlockPos source = helper.absolutePos(FIRST);
         Consumer<UseItemOnBlockEvent> listener = event -> {
            if (event.getPlayer() == player && event.getPos().equals(source) && event.getUsePhase() == phase) {
               event.cancelWithResult(ItemInteractionResult.FAIL);
            }
         };
         NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, UseItemOnBlockEvent.class, listener);
         try {
            click(helper, player, FIRST, Direction.NORTH);
            assertDoor(helper, FIRST, false);
            ((DoorBlock)Blocks.OAK_DOOR).setOpen(player, helper.getLevel(), helper.getBlockState(FIRST), source, true);
            assertDoor(helper, FIRST, true);
            assertDoor(helper, FIRST.east(), false);
         } finally {
            NeoForge.EVENT_BUS.unregister(listener);
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void companion_hooks_cannot_recreate_removed_source_or_target_halves(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (boolean removeSource : List.of(true, false)) {
         clear(helper);
         doorPair(helper, Blocks.OAK_DOOR);
         BlockPos target = helper.absolutePos(FIRST.east());
         BlockPos removed = removeSource ? FIRST.above() : FIRST.east().above();
         Consumer<UseItemOnBlockEvent> listener = event -> {
            if (event.getPlayer() == player && event.getPos().equals(target) && event.getUsePhase() == UseItemOnBlockEvent.UsePhase.BLOCK) {
               helper.getLevel().setBlock(helper.absolutePos(removed), Blocks.AIR.defaultBlockState(), 2);
            }
         };
         NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, UseItemOnBlockEvent.class, listener);
         try {
            click(helper, player, FIRST, Direction.NORTH);
            helper.assertTrue(helper.getBlockState(removed).isAir(), "companion forwarding must not restore a removed half");
            assertOpen(helper, FIRST.east(), false);
         } finally {
            NeoForge.EVENT_BUS.unregister(listener);
         }
      }
      helper.succeed();
   }

   private static ServerPlayer player(GameTestHelper helper) {
      // A normal mock-player login fires IE custom-payload synchronization without a
      // negotiated client. FakePlayer provides the native server game mode without login.
      return new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "PairTest"));
   }

   private static void clear(GameTestHelper helper) {
      for (BlockPos pos : BlockPos.betweenClosed(FIRST.offset(-3, 0, -3), FIRST.offset(3, 3, 3))) {
         helper.getLevel().setBlock(helper.absolutePos(pos), Blocks.AIR.defaultBlockState(), 2);
      }
      for (BlockPos pos : BlockPos.betweenClosed(FIRST.offset(-3, -1, -3), FIRST.offset(3, -1, 3))) {
         helper.getLevel().setBlock(helper.absolutePos(pos), Blocks.STONE.defaultBlockState(), 2);
      }
   }

   private static void doorPair(GameTestHelper helper, Block door) {
      placeDoor(helper, FIRST, door, Direction.NORTH, DoorHingeSide.LEFT);
      placeDoor(helper, FIRST.east(), door, Direction.NORTH, DoorHingeSide.RIGHT);
   }

   private static void placeDoor(GameTestHelper helper, BlockPos pos, Block door, Direction facing, DoorHingeSide hinge) {
      BlockState lower = door.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HINGE, hinge)
         .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER).setValue(DoorBlock.OPEN, false).setValue(DoorBlock.POWERED, false);
      helper.setBlock(pos, lower);
      helper.setBlock(pos.above(), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
   }

   private static void placeGate(GameTestHelper helper, BlockPos pos, Block gate, Direction facing, int height) {
      BlockState state = gate.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
         .setValue(BlockStateProperties.OPEN, false);
      if (gate instanceof PortedBlocks.FenceGateSegmentBlock) {
         state = state.setValue(PortedBlocks.SEGMENT, 0);
      }
      helper.setBlock(pos, state);
      if (height == 2) {
         helper.setBlock(pos.above(), state.setValue(PortedBlocks.SEGMENT, 1));
      }
   }

   private static void click(GameTestHelper helper, ServerPlayer player, BlockPos relative, Direction playerFacing) {
      use(helper, player, relative, playerFacing, InteractionHand.MAIN_HAND);
   }

   private static void use(GameTestHelper helper, ServerPlayer player, BlockPos relative, Direction playerFacing, InteractionHand hand) {
      BlockPos absolute = helper.absolutePos(relative);
      player.setPos(absolute.getX() + 0.5, helper.absolutePos(FIRST).getY(), absolute.getZ() + 1.5);
      player.setYRot(playerFacing.toYRot());
      BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), playerFacing, absolute, false);
      player.gameMode.useItemOn(player, helper.getLevel(), player.getItemInHand(hand), hand, hit);
   }

   private static void assertDoor(GameTestHelper helper, BlockPos pos, boolean open) {
      assertOpen(helper, pos, open);
      assertOpen(helper, pos.above(), open);
   }

   private static void assertOpen(GameTestHelper helper, BlockPos pos, boolean open) {
      helper.assertBlockProperty(pos, BlockStateProperties.OPEN, open);
   }
}
