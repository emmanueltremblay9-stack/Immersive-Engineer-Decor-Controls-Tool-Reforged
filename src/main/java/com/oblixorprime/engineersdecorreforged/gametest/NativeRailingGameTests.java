package com.oblixorprime.engineersdecorreforged.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Native server interactions: deliberately uses no new production implementation API. */
@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class NativeRailingGameTests {
   private static final BlockPos CENTER = new BlockPos(3, 1, 3);
   private static final Direction[] EDGES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
   private static final BooleanProperty[] SIDES = {PortedBlocks.NORTH, PortedBlocks.EAST, PortedBlocks.SOUTH, PortedBlocks.WEST};

   private NativeRailingGameTests() {
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_catwalk_clicks_toggle_each_side_for_all_masks_and_yaws(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (int mask = 0; mask < 16; mask++) {
         for (int edge = 0; edge < EDGES.length; edge++) {
            for (Direction facing : EDGES) {
               clear(helper);
               put(helper, CENTER, sides(catwalk(), mask));
               giveRailings(player, 32);
               clickEdge(helper, player, CENTER, EDGES[edge], facing);
               assertSides(helper, catwalk(), mask ^ (1 << edge),
                  "catwalk mask " + mask + ", edge " + EDGES[edge] + ", yaw " + facing);
               helper.assertTrue(helper.getBlockState(CENTER.above()).isAir(), "railing click must not place a separate block above catwalk");
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_standalone_clicks_toggle_all_nonempty_masks_independent_of_yaw(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (int mask = 1; mask < 16; mask++) {
         for (int edge = 0; edge < EDGES.length; edge++) {
            for (Direction facing : EDGES) {
               clear(helper);
               put(helper, CENTER, sides(railing(), mask));
               giveRailings(player, 32);
               clickEdge(helper, player, CENTER, EDGES[edge], facing);
               int expected = mask ^ (1 << edge);
               if (expected == 0) {
                  helper.assertTrue(helper.getBlockState(CENTER).isAir(), "removing the final standalone edge must remove the block");
               } else {
                  assertSides(helper, railing(), expected, "standalone mask " + mask + ", edge " + EDGES[edge] + ", yaw " + facing);
               }
               helper.assertTrue(helper.getBlockState(CENTER.above()).isAir(), "native edit must not place a second standalone block");
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_standalone_placement_uses_the_clicked_edge_not_player_yaw(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (int edge = 0; edge < EDGES.length; edge++) {
         for (Direction facing : EDGES) {
            clear(helper);
            giveRailings(player, 32);
            clickEdge(helper, player, CENTER.below(), EDGES[edge], facing);
            assertSides(helper, railing(), 1 << edge, "initial placement edge " + EDGES[edge] + ", yaw " + facing);
            helper.assertValueEqual(player.getMainHandItem().getCount(), 31, "initial standalone placement must consume one railing");
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_railing_clicks_build_four_sides_then_remove_each_independently(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Block block : List.of(catwalk(), railing())) {
         clear(helper);
         giveRailings(player, 16);
         int mask = 0;
         if (block == railing()) {
            clickEdge(helper, player, CENTER.below(), Direction.NORTH, Direction.SOUTH);
            mask = 1;
         } else {
            put(helper, CENTER, sides(block, 0));
         }
         for (int edge = block == railing() ? 1 : 0; edge < EDGES.length; edge++) {
            clickEdge(helper, player, CENTER, EDGES[edge], EDGES[edge].getOpposite());
            mask |= 1 << edge;
            assertSides(helper, block, mask, "successive additions must preserve existing edges");
         }
         assertSides(helper, block, 15, "the complete four-sided model must be attainable in game");
         helper.assertValueEqual(owned(helper, player, railing().asItem()), 12, "four sides must cost exactly four railings");
         roundTripAtCenter(helper);
         for (int edge = EDGES.length - 1; edge >= 0; edge--) {
            clickEdge(helper, player, CENTER, EDGES[edge], EDGES[edge].getOpposite());
            mask &= ~(1 << edge);
            if (block == railing() && mask == 0) {
               helper.assertTrue(helper.getBlockState(CENTER).isAir(), "last standalone side must disappear");
            } else {
               assertSides(helper, block, mask, "successive removals must preserve the remaining edges");
            }
            helper.assertValueEqual(owned(helper, player, railing().asItem()), 16 - edge,
               "each purchased side must refund once after saved-state reload");
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void selected_railing_sides_survive_neighbor_updates_and_saved_state_round_trips(GameTestHelper helper) {
      for (Block block : List.of(catwalk(), railing())) {
         for (int mask = 0; mask < 16; mask++) {
            for (boolean waterlogged : new boolean[]{false, true}) {
               clear(helper);
               BlockState original = sides(block, mask).setValue(PortedBlocks.WATERLOGGED, waterlogged);
               put(helper, CENTER, original);
               roundTripAtCenter(helper);
               for (Direction neighborDirection : Direction.values()) {
                  BlockPos neighbor = helper.absolutePos(CENTER.relative(neighborDirection));
                  for (Block neighborBlock : List.of(catwalk(), railing(), ModBlocks.STEEL_CATWALK_STAIRS.get(),
                     ModBlocks.STEEL_MESH_FENCE_GATE.get(), Blocks.STONE, Blocks.AIR)) {
                     helper.getLevel().setBlock(neighbor, neighborBlock.defaultBlockState(), 3);
                     helper.assertValueEqual(helper.getBlockState(CENTER), original,
                        "neighbor " + neighborBlock + " at " + neighborDirection + " must not alter selected mask " + mask);
                  }
               }
               roundTripAtCenter(helper);
               helper.assertValueEqual(helper.getBlockState(CENTER), original, "neighbor updates and reload must preserve the entire saved state");
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void railing_collision_tracks_selected_edges_and_preserves_open_center(GameTestHelper helper) {
      VoxelShape[] probes = {
         Block.box(6, 5, 0.25, 10, 14, 1.75), Block.box(14.25, 5, 6, 15.75, 14, 10),
         Block.box(6, 5, 14.25, 10, 14, 15.75), Block.box(0.25, 5, 6, 1.75, 14, 10)
      };
      VoxelShape center = Block.box(3, 4, 3, 13, 16, 13);
      VoxelShape floor = Block.box(6, 0.25, 6, 10, 1.75, 10);
      for (Block block : List.of(catwalk(), railing())) {
         for (int mask = 0; mask < 16; mask++) {
            for (boolean waterlogged : new boolean[]{false, true}) {
               BlockState state = sides(block, mask).setValue(PortedBlocks.WATERLOGGED, waterlogged);
               VoxelShape collision = state.getCollisionShape(helper.getLevel(), helper.absolutePos(CENTER));
               int effectiveMask = block == railing() && mask == 0 ? 1 : mask;
               for (int edge = 0; edge < EDGES.length; edge++) {
                  helper.assertValueEqual(Shapes.joinIsNotEmpty(collision, probes[edge], BooleanOp.AND),
                     (effectiveMask & (1 << edge)) != 0, block + " mask " + mask + " collision at " + EDGES[edge]);
               }
               helper.assertTrue(!Shapes.joinIsNotEmpty(collision, center, BooleanOp.AND), "railings must leave center headroom open for mask " + mask);
               helper.assertValueEqual(Shapes.joinIsNotEmpty(collision, floor, BooleanOp.AND), block == catwalk(),
                  "only the catwalk must include the floor");
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_legacy_railing_states_never_mint_refunds_for_unpaid_sides(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Block block : List.of(catwalk(), railing())) {
         for (int mask = 0; mask < 16; mask++) {
            clear(helper);
            put(helper, CENTER, legacyState(block, mask, false));
            giveRailings(player, 16);
            int effectiveMask = block == railing() && mask == 0 ? 1 : mask;
            for (int edge = 0; edge < EDGES.length; edge++) {
               if ((effectiveMask & (1 << edge)) == 0) {
                  continue;
               }
               clickEdge(helper, player, CENTER, EDGES[edge], Direction.SOUTH);
               effectiveMask &= ~(1 << edge);
               if (block == railing() && effectiveMask == 0) {
                  helper.assertTrue(helper.getBlockState(CENTER).isAir(), "legacy final side must remove the standalone block");
               } else {
                  assertSides(helper, block, effectiveMask, "removing one legacy side must preserve other sides");
               }
               helper.assertValueEqual(owned(helper, player, railing().asItem()),
                  block == railing() && effectiveMask == 0 ? 17 : 16,
                  "legacy sides carry no paid refund; only a final standalone base item is returned");
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_legacy_empty_standalone_is_north_on_its_first_edit(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (int edge = 0; edge < EDGES.length; edge++) {
         clear(helper);
         put(helper, CENTER, legacyState(railing(), 0, false));
         giveRailings(player, 16);
         clickEdge(helper, player, CENTER, EDGES[edge], Direction.EAST);
         if (edge == 0) {
            helper.assertTrue(helper.getBlockState(CENTER).isAir(), "legacy implicit north must be removable on the first click");
            helper.assertValueEqual(owned(helper, player, railing().asItem()), 17, "legacy base item returns once");
         } else {
            assertSides(helper, railing(), 1 | (1 << edge), "first edit must preserve the legacy visible north edge");
            helper.assertValueEqual(owned(helper, player, railing().asItem()), 15, "adding to legacy standalone costs one item");
            roundTripAtCenter(helper);
            clickEdge(helper, player, CENTER, EDGES[edge], Direction.WEST);
            assertSides(helper, railing(), 1, "removing purchased legacy addition leaves north");
            helper.assertValueEqual(owned(helper, player, railing().asItem()), 16, "only the purchased side returns");
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_railing_survival_breaks_drop_base_plus_only_paid_additions(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Block block : List.of(catwalk(), railing())) {
         for (int count = block == railing() ? 1 : 0; count <= 4; count++) {
            clear(helper);
            giveRailings(player, 16);
            int start = 0;
            if (block == railing()) {
               clickEdge(helper, player, CENTER.below(), Direction.NORTH, Direction.SOUTH);
               start = 1;
            } else {
               put(helper, CENTER, sides(block, 0));
            }
            for (int edge = start; edge < count; edge++) {
               clickEdge(helper, player, CENTER, EDGES[edge], Direction.SOUTH);
            }
            helper.assertValueEqual(owned(helper, player, railing().asItem()), 16 - count, "building must consume one item per side");
            roundTripAtCenter(helper);
            assertDropCounts(helper, helper.getBlockState(CENTER), block == catwalk() ? 1 : 0, count);
            breakCenter(helper, player);
            helper.assertValueEqual(owned(helper, player, railing().asItem()), 16, "breaking must recover exactly the original railing material");
            helper.assertValueEqual(owned(helper, player, catwalk().asItem()), block == catwalk() ? 1 : 0, "breaking preserves the catwalk base item");
         }
         for (int mask = 0; mask < 16; mask++) {
            clear(helper);
            giveRailings(player, 16);
            put(helper, CENTER, legacyState(block, mask, false));
            assertDropCounts(helper, helper.getBlockState(CENTER), block == catwalk() ? 1 : 0, block == railing() ? 1 : 0);
            breakCenter(helper, player);
            helper.assertValueEqual(owned(helper, player, railing().asItem()), block == railing() ? 17 : 16,
               "breaking legacy railings must never pay for historical auto-connected sides");
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_creative_railing_edits_consume_nothing_and_never_create_paid_credits(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Block block : List.of(catwalk(), railing())) {
         clear(helper);
         player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
         giveRailings(player, 16);
         int start = 0;
         if (block == railing()) {
            clickEdge(helper, player, CENTER.below(), Direction.NORTH, Direction.SOUTH);
            start = 1;
         } else {
            put(helper, CENTER, sides(block, 0));
         }
         for (int edge = start; edge < EDGES.length; edge++) {
            clickEdge(helper, player, CENTER, EDGES[edge], Direction.SOUTH);
         }
         assertSides(helper, block, 15, "creative must reach the four-sided model");
         helper.assertValueEqual(owned(helper, player, railing().asItem()), 16, "creative additions must not consume items");
         roundTripAtCenter(helper);
         assertDropCounts(helper, helper.getBlockState(CENTER), block == catwalk() ? 1 : 0, block == railing() ? 1 : 0);
         for (int edge = EDGES.length - 1; edge >= 0; edge--) {
            clickEdge(helper, player, CENTER, EDGES[edge], Direction.SOUTH);
            helper.assertValueEqual(owned(helper, player, railing().asItem()), 16, "creative removals must never refund");
         }

         // A creative-added side must still be unpaid after changing to survival.
         put(helper, CENTER, sides(block, block == railing() ? 1 : 0));
         clickEdge(helper, player, CENTER, Direction.EAST, Direction.SOUTH);
         roundTripAtCenter(helper);
         player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
         clickEdge(helper, player, CENTER, Direction.EAST, Direction.SOUTH);
         helper.assertValueEqual(owned(helper, player, railing().asItem()), 16, "survival cannot cash in a creative addition");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_creative_removal_spends_paid_credit_without_refunding_or_leaving_duplicate_drops(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Block block : List.of(catwalk(), railing())) {
         clear(helper);
         player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
         giveRailings(player, 16);
         put(helper, CENTER, sides(block, block == railing() ? 1 : 0));
         clickEdge(helper, player, CENTER, Direction.EAST, Direction.SOUTH);
         helper.assertValueEqual(owned(helper, player, railing().asItem()), 15, "survival addition costs one item");
         player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
         clickEdge(helper, player, CENTER, Direction.EAST, Direction.SOUTH);
         helper.assertValueEqual(owned(helper, player, railing().asItem()), 15, "creative removal never refunds a paid addition");
         roundTripAtCenter(helper);
         assertDropCounts(helper, helper.getBlockState(CENTER), block == catwalk() ? 1 : 0, block == railing() ? 1 : 0);
         // Re-adding for free must not revive the discarded paid credit.
         clickEdge(helper, player, CENTER, Direction.EAST, Direction.SOUTH);
         player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
         clickEdge(helper, player, CENTER, Direction.EAST, Direction.SOUTH);
         helper.assertValueEqual(owned(helper, player, railing().asItem()), 15, "creative remove/add cycle must not revive a paid refund");
         player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
         breakCenter(helper, player);
         helper.assertValueEqual(owned(helper, player, railing().asItem()), 15, "creative breaking must yield no drops");
         helper.assertValueEqual(owned(helper, player, catwalk().asItem()), 0, "creative breaking must not drop the catwalk base");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_railing_waterlogging_survives_edits_and_last_standalone_removal_restores_water(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Block block : List.of(catwalk(), railing())) {
         clear(helper);
         giveRailings(player, 16);
         put(helper, CENTER, legacyState(block, block == railing() ? 1 : 0, true));
         clickEdge(helper, player, CENTER, Direction.EAST, Direction.SOUTH);
         helper.assertBlockProperty(CENTER, PortedBlocks.WATERLOGGED, true);
         roundTripAtCenter(helper);
         clickEdge(helper, player, CENTER, Direction.EAST, Direction.SOUTH);
         helper.assertBlockProperty(CENTER, PortedBlocks.WATERLOGGED, true);
         if (block == railing()) {
            clickEdge(helper, player, CENTER, Direction.NORTH, Direction.SOUTH);
            helper.assertTrue(helper.getBlockState(CENTER).is(Blocks.WATER), "removing final waterlogged standalone side must restore source water");
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void real_raycast_clicks_build_every_standalone_mask_without_imaginary_surfaces(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (int mask = 1; mask < 16; mask++) {
         for (Direction facing : EDGES) {
            clear(helper);
            giveRailings(player, 16);
            int built = 0;
            for (int edge = 0; edge < EDGES.length; edge++) {
               if ((mask & (1 << edge)) != 0) {
                  raycastClickEdge(helper, player, EDGES[edge], facing, CENTER.below());
                  built |= 1 << edge;
                  assertSides(helper, railing(), built, "real support-top clicks must reach mask " + mask);
               }
            }
            helper.assertValueEqual(owned(helper, player, railing().asItem()), 16 - Integer.bitCount(mask),
               "real placement merges must charge once per added side");
            for (int edge = 0; edge < EDGES.length; edge++) {
               if ((built & (1 << edge)) != 0) {
                  raycastClickEdge(helper, player, EDGES[edge], facing, CENTER);
                  built &= ~(1 << edge);
                  if (built == 0) {
                     helper.assertTrue(helper.getBlockState(CENTER).isAir(), "last real railing hit must remove the block");
                  } else {
                     assertSides(helper, railing(), built, "real side removal must preserve other edges");
                  }
               }
            }
            helper.assertValueEqual(owned(helper, player, railing().asItem()), 16, "real placement and removal cycle must conserve material");
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void real_raycast_clicks_reach_opposite_l_u_and_four_sides_from_every_start(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (int first = 0; first < EDGES.length; first++) {
         for (boolean oppositeFirst : new boolean[]{false, true}) {
            clear(helper);
            giveRailings(player, 16);
            int second = (first + (oppositeFirst ? 2 : 1)) % 4;
            int third = (first + (oppositeFirst ? 1 : 2)) % 4;
            int fourth = (first + 3) % 4;
            int mask = 0;
            for (int edge : new int[]{first, second, third, fourth}) {
               raycastClickEdge(helper, player, EDGES[edge], EDGES[first].getOpposite(), CENTER.below());
               mask |= 1 << edge;
               assertSides(helper, railing(), mask, "real single/opposite/L/U/four sequence from " + EDGES[first]);
            }
            helper.assertValueEqual(mask, 15, "all four sides must be reachable without temporary pieces");
            helper.assertValueEqual(owned(helper, player, railing().asItem()), 12, "four-sided construction must cost four items");
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void real_catwalk_raycast_clicks_add_and_remove_all_edges(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Direction facing : EDGES) {
         clear(helper);
         giveRailings(player, 16);
         put(helper, CENTER, sides(catwalk(), 0));
         int mask = 0;
         for (int edge = 0; edge < EDGES.length; edge++) {
            raycastClickEdge(helper, player, EDGES[edge], facing, CENTER);
            mask |= 1 << edge;
            assertSides(helper, catwalk(), mask, "catwalk floor or rail surface must provide a real click target");
         }
         for (int edge = 0; edge < EDGES.length; edge++) {
            raycastClickEdge(helper, player, EDGES[edge], facing, CENTER);
            mask &= ~(1 << edge);
            assertSides(helper, catwalk(), mask, "actual rail-surface removal preserves the floor");
         }
         helper.assertValueEqual(owned(helper, player, railing().asItem()), 16, "real catwalk edit cycle must conserve material");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void cancelled_native_railing_edits_leave_shape_state_and_inventory_unchanged(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Block block : List.of(catwalk(), railing())) {
         for (boolean cancelEarly : new boolean[]{false, true}) {
            clear(helper);
            giveRailings(player, 16);
            put(helper, CENTER, sides(block, 1));
            BlockState original = helper.getBlockState(CENTER);
            Consumer<PlayerInteractEvent.RightClickBlock> early = event -> {
               if (event.getEntity() == player) {
                  event.setCanceled(true);
                  event.setCancellationResult(InteractionResult.FAIL);
               }
            };
            Consumer<UseItemOnBlockEvent> blockPhase = event -> {
               if (event.getPlayer() == player && event.getUsePhase() == UseItemOnBlockEvent.UsePhase.BLOCK) {
                  // NeoForge's FAIL permits ITEM_AFTER_BLOCK fallback; CONSUME is
                  // the framework's terminal cancellation for this entire click.
                  event.cancelWithResult(ItemInteractionResult.CONSUME);
               }
            };
            if (cancelEarly) {
               NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, PlayerInteractEvent.RightClickBlock.class, early);
            } else {
               NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, UseItemOnBlockEvent.class, blockPhase);
            }
            try {
               for (Direction edge : List.of(Direction.NORTH, Direction.EAST)) {
                  clickEdge(helper, player, CENTER, edge, Direction.SOUTH);
                  helper.assertValueEqual(helper.getBlockState(CENTER), original, "cancelled edit must preserve complete original state");
                  helper.assertValueEqual(owned(helper, player, railing().asItem()), 16, "cancelled edit must not consume or refund");
                  helper.assertTrue(helper.getBlockState(CENTER.above()).isAir(), "cancelled edit must not run placement fallback");
               }
            } finally {
               NeoForge.EVENT_BUS.unregister(cancelEarly ? early : blockPhase);
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void cancelled_native_support_top_merge_preserves_original_railing_and_items(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      clear(helper);
      giveRailings(player, 16);
      raycastClickEdge(helper, player, Direction.NORTH, Direction.SOUTH, CENTER.below());
      BlockState original = helper.getBlockState(CENTER);
      Consumer<BlockEvent.EntityPlaceEvent> listener = event -> {
         if (event.getEntity() == player && event.getPos().equals(helper.absolutePos(CENTER))) {
            event.setCanceled(true);
         }
      };
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, BlockEvent.EntityPlaceEvent.class, listener);
      try {
         raycastClickEdge(helper, player, Direction.SOUTH, Direction.NORTH, CENTER.below());
         helper.assertValueEqual(helper.getBlockState(CENTER), original, "cancelled merge must restore full original state including paid balance");
         helper.assertValueEqual(owned(helper, player, railing().asItem()), 15, "cancelled merge must not consume another item");
         assertDropCounts(helper, helper.getBlockState(CENTER), 0, 1);
      } finally {
         NeoForge.EVENT_BUS.unregister(listener);
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_railing_edits_respect_adventure_and_spectator_modes(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (GameType mode : List.of(GameType.ADVENTURE, GameType.SPECTATOR)) {
         player.gameMode.changeGameModeForPlayer(mode);
         for (Block block : List.of(catwalk(), railing())) {
            clear(helper);
            giveRailings(player, 16);
            put(helper, CENTER, sides(block, 1));
            BlockState original = helper.getBlockState(CENTER);
            clickEdge(helper, player, CENTER, Direction.NORTH, Direction.SOUTH);
            clickEdge(helper, player, CENTER, Direction.EAST, Direction.SOUTH);
            helper.assertValueEqual(helper.getBlockState(CENTER), original, mode + " must not add or remove railing sides");
            helper.assertValueEqual(owned(helper, player, railing().asItem()), 16, mode + " must not change railing inventory");
            if (block == railing()) {
               raycastClickEdge(helper, player, Direction.SOUTH, Direction.NORTH, CENTER.below());
               helper.assertValueEqual(helper.getBlockState(CENTER), original, mode + " cannot merge through the support");
               helper.assertValueEqual(owned(helper, player, railing().asItem()), 16, mode + " rejected merge must not change inventory");
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_offhand_railing_edits_charge_only_the_interacting_hand(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Block block : List.of(catwalk(), railing())) {
         clear(helper);
         giveRailings(player, 16);
         put(helper, CENTER, sides(block, 1));
         player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK, 9));
         player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(railing().asItem(), 5));
         clickEdge(helper, player, CENTER, Direction.EAST, Direction.SOUTH, InteractionHand.OFF_HAND);
         assertSides(helper, block, 3, "offhand addition must change only the selected edge");
         helper.assertValueEqual(player.getOffhandItem().getCount(), 4, "offhand addition consumes from the offhand");
         helper.assertValueEqual(player.getMainHandItem().getCount(), 9, "offhand addition must not change the main hand");
         clickEdge(helper, player, CENTER, Direction.EAST, Direction.SOUTH, InteractionHand.OFF_HAND);
         assertSides(helper, block, 1, "offhand removal must preserve the other edge");
         helper.assertValueEqual(owned(helper, player, railing().asItem()), 5, "offhand cycle must refund the purchased addition once");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_railing_refunds_drop_safely_when_the_inventory_is_full(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Block block : List.of(catwalk(), railing())) {
         clear(helper);
         giveRailings(player, 16);
         put(helper, CENTER, sides(block, 1));
         clickEdge(helper, player, CENTER, Direction.EAST, Direction.SOUTH);
         for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
         }
         player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(railing().asItem(), 64));
         clickEdge(helper, player, CENTER, Direction.EAST, Direction.SOUTH);
         assertSides(helper, block, 1, "full inventory cannot prevent removal of a paid side");
         helper.assertValueEqual(player.getMainHandItem().getCount(), 64, "refund cannot overflow an already full held stack");
         helper.assertValueEqual(owned(helper, player, railing().asItem()), 65, "full inventory must produce exactly one recoverable refund");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void blocked_native_railing_additions_do_not_charge_or_fall_back_to_new_blocks(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Block block : List.of(catwalk(), railing())) {
         for (int edge = 0; edge < EDGES.length; edge++) {
            clear(helper);
            giveRailings(player, 16);
            put(helper, CENTER, sides(block, block == railing() ? 1 << ((edge + 2) % 4) : 0));
            BlockState original = helper.getBlockState(CENTER);
            BlockPos pos = helper.absolutePos(CENTER);
            var mob = EntityType.ZOMBIE.create(helper.getLevel());
            helper.assertTrue(mob != null, "obstruction fixture must exist");
            // Short enough to leave a separate railing in the block above unobstructed:
            // otherwise a broken FAIL/fallback path could accidentally pass this test.
            mob.setBaby(true);
            mob.setNoAi(true);
            mob.setPos(pos.getX() + 0.5 + EDGES[edge].getStepX() * 0.45,
               pos.getY() + (block == catwalk() ? 3.0 / 16.0 : 0.0),
               pos.getZ() + 0.5 + EDGES[edge].getStepZ() * 0.45);
            helper.assertTrue(helper.getLevel().noCollision(mob), "obstruction must be clear before the proposed new railing");
            helper.assertTrue(helper.getLevel().addFreshEntity(mob), "obstruction must be added to world collision checks");
            try {
               helper.assertTrue(helper.getLevel().isUnobstructed(sides(railing(), 1 << edge), pos.above(), CollisionContext.of(player)),
                  "fixture must permit the unwanted fallback block above so the test detects fallthrough");
               clickEdge(helper, player, CENTER, EDGES[edge], Direction.SOUTH);
               helper.assertValueEqual(helper.getBlockState(CENTER), original, "occupied edge must reject the edit without changing paid state");
               helper.assertValueEqual(owned(helper, player, railing().asItem()), 16, "rejected edit must neither consume nor refund");
               helper.assertTrue(helper.getBlockState(CENTER.above()).isAir(), "rejected edit must not place an unrelated block above");
               for (Direction neighbor : EDGES) {
                  helper.assertTrue(helper.getBlockState(CENTER.relative(neighbor)).isAir(), "rejected edit must not place an unrelated adjacent block");
               }
            } finally {
               mob.discard();
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void railing_rotations_and_mirrors_preserve_selected_edges_water_and_paid_drops(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Block block : List.of(catwalk(), railing())) {
         for (int mask = block == railing() ? 1 : 0; mask < 16; mask++) {
            clear(helper);
            giveRailings(player, 16);
            if (block == catwalk()) {
               put(helper, CENTER, sides(block, 0));
            }
            for (int edge = 0; edge < EDGES.length; edge++) {
               if ((mask & (1 << edge)) != 0) {
                  if (block == railing()) {
                     raycastClickEdge(helper, player, EDGES[edge], Direction.SOUTH, CENTER.below());
                  } else {
                     clickEdge(helper, player, CENTER, EDGES[edge], Direction.SOUTH);
                  }
               }
            }
            assertSides(helper, block, mask, "transform fixture must be built through native interactions");
            for (boolean waterlogged : new boolean[]{false, true}) {
               BlockState original = helper.getBlockState(CENTER).setValue(PortedBlocks.WATERLOGGED, waterlogged);
               for (Rotation rotation : Rotation.values()) {
                  BlockState transformed = original.rotate(rotation);
                  int expected = transformedMask(mask, rotation, Mirror.NONE);
                  assertSides(helper, transformed, block, expected, "rotation " + rotation + " of mask " + mask);
                  assertTransformedState(helper, original, transformed, Integer.bitCount(mask));
               }
               for (Mirror mirror : Mirror.values()) {
                  BlockState transformed = original.mirror(mirror);
                  int expected = transformedMask(mask, Rotation.NONE, mirror);
                  assertSides(helper, transformed, block, expected, "mirror " + mirror + " of mask " + mask);
                  assertTransformedState(helper, original, transformed, Integer.bitCount(mask));
               }
            }
         }
      }
      // Legacy zero-property standalone visibly means north, including structure transforms.
      BlockState legacy = legacyState(railing(), 0, true);
      for (Rotation rotation : Rotation.values()) {
         BlockState transformed = legacy.rotate(rotation);
         assertSides(helper, transformed, railing(), transformedMask(1, rotation, Mirror.NONE), "legacy implicit north rotation");
         assertTransformedState(helper, legacy, transformed, 1);
      }
      for (Mirror mirror : Mirror.values()) {
         BlockState transformed = legacy.mirror(mirror);
         assertSides(helper, transformed, railing(), transformedMask(1, Rotation.NONE, mirror), "legacy implicit north mirror");
         assertTransformedState(helper, legacy, transformed, 1);
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void railing_blockstate_variants_cover_every_saved_state_including_economic_properties(GameTestHelper helper) {
      for (Block block : List.of(catwalk(), railing())) {
         String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
         JsonObject variants = readJson("/assets/immersive_engineer_decor_controls_tool_reforged/blockstates/" + id + ".json").getAsJsonObject("variants");
         helper.assertTrue(variants != null, "railing blockstate must define variants");
         for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            int matches = 0;
            for (var variant : variants.entrySet()) {
               boolean matchesState = true;
               for (String condition : variant.getKey().split(",")) {
                  String[] pair = condition.split("=", 2);
                  if (pair.length != 2 || state.getValues().entrySet().stream().noneMatch(entry ->
                     entry.getKey().getName().equals(pair[0]) && entry.getValue().toString().equals(pair[1]))) {
                     matchesState = false;
                     break;
                  }
               }
               if (matchesState) {
                  matches++;
                  String model = variant.getValue().getAsJsonObject().get("model").getAsString();
                  String[] resource = model.split(":", 2);
                  readJson("/assets/" + resource[0] + "/models/" + resource[1] + ".json");
                  if (state.getValue(PortedBlocks.NORTH) && state.getValue(PortedBlocks.EAST)
                     && state.getValue(PortedBlocks.SOUTH) && state.getValue(PortedBlocks.WEST)) {
                     helper.assertTrue(model.contains("nesw"), "four selected edges must use the four-sided model");
                  }
               }
            }
            helper.assertValueEqual(matches, 1, "exactly one existing model variant must cover " + state);
         }
      }
      helper.succeed();
   }

   private static ServerPlayer player(GameTestHelper helper) {
      // FakePlayer exercises the real game mode without IE login packet synchronization.
      ServerPlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "RailingTest"));
      player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
      return player;
   }

   private static Block catwalk() {
      return ModBlocks.STEEL_CATWALK.get();
   }

   private static Block railing() {
      return ModBlocks.STEEL_RAILING.get();
   }

   private static void giveRailings(ServerPlayer player, int count) {
      player.getInventory().clearContent();
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(railing().asItem(), count));
   }

   private static BlockState sides(Block block, int mask) {
      BlockState state = block.defaultBlockState();
      for (int edge = 0; edge < SIDES.length; edge++) {
         state = state.setValue(SIDES[edge], (mask & (1 << edge)) != 0);
      }
      return state;
   }

   private static void assertSides(GameTestHelper helper, Block block, int mask, String context) {
      BlockState state = helper.getBlockState(CENTER);
      assertSides(helper, state, block, mask, context);
   }

   private static void assertSides(GameTestHelper helper, BlockState state, Block block, int mask, String context) {
      helper.assertTrue(state.is(block), context + ": expected " + block + ", got " + state);
      for (int edge = 0; edge < SIDES.length; edge++) {
         helper.assertValueEqual(state.getValue(SIDES[edge]), (mask & (1 << edge)) != 0,
            context + ": side " + EDGES[edge]);
      }
   }

   private static BlockState legacyState(Block block, int mask, boolean waterlogged) {
      var saved = NbtUtils.writeBlockState(sides(block, mask).setValue(PortedBlocks.WATERLOGGED, waterlogged));
      var properties = saved.getCompound("Properties");
      // Build a real pre-fix save without depending on the new property's Java name.
      for (String name : List.copyOf(properties.getAllKeys())) {
         if (!List.of("north", "east", "south", "west", "waterlogged").contains(name)) {
            properties.remove(name);
         }
      }
      return NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), saved);
   }

   private static int transformedMask(int mask, Rotation rotation, Mirror mirror) {
      int result = 0;
      for (int edge = 0; edge < EDGES.length; edge++) {
         if ((mask & (1 << edge)) != 0) {
            Direction transformed = mirror.mirror(rotation.rotate(EDGES[edge]));
            for (int target = 0; target < EDGES.length; target++) {
               if (EDGES[target] == transformed) {
                  result |= 1 << target;
               }
            }
         }
      }
      return result;
   }

   private static void assertTransformedState(GameTestHelper helper, BlockState original, BlockState transformed, int railings) {
      helper.assertValueEqual(transformed.getValue(PortedBlocks.WATERLOGGED), original.getValue(PortedBlocks.WATERLOGGED),
         "structure transforms must preserve waterlogging");
      assertDropCounts(helper, transformed, original.is(catwalk()) ? 1 : 0, railings);
      var saved = NbtUtils.writeBlockState(transformed);
      helper.assertValueEqual(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), saved), transformed,
         "transformed side and paid state must survive NBT save/load");
      helper.assertValueEqual(BlockState.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow(), transformed,
         "transformed side and paid state must survive codec save/load");
   }

   private static JsonObject readJson(String path) {
      try (InputStream input = NativeRailingGameTests.class.getResourceAsStream(path)) {
         if (input == null) {
            throw new IllegalStateException("Missing model resource: " + path);
         }
         try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
         }
      } catch (java.io.IOException exception) {
         throw new IllegalStateException("Cannot read model resource: " + path, exception);
      }
   }

   private static void roundTripAtCenter(GameTestHelper helper) {
      BlockState original = helper.getBlockState(CENTER);
      var saved = NbtUtils.writeBlockState(original);
      BlockState loaded = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), saved);
      helper.assertValueEqual(loaded, original, "NBT save/load must preserve all side and economic state");
      helper.assertValueEqual(BlockState.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow(), original,
         "block-state codec must preserve all side and economic state");
      put(helper, CENTER, loaded);
   }

   private static AABB dropArea(GameTestHelper helper) {
      return new AABB(helper.absolutePos(CENTER)).inflate(1.5);
   }

   private static int owned(GameTestHelper helper, ServerPlayer player, Item item) {
      int count = 0;
      for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         if (stack.is(item)) {
            count += stack.getCount();
         }
      }
      return count + helper.getLevel().getEntitiesOfClass(ItemEntity.class, dropArea(helper),
         entity -> entity.getItem().is(item)).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
   }

   private static void assertDropCounts(GameTestHelper helper, BlockState state, int catwalks, int railings) {
      List<ItemStack> drops = Block.getDrops(state, helper.getLevel(), helper.absolutePos(CENTER), null);
      helper.assertValueEqual(drops.stream().filter(stack -> stack.is(catwalk().asItem())).mapToInt(ItemStack::getCount).sum(),
         catwalks, "loot must preserve exactly one catwalk base when present");
      helper.assertValueEqual(drops.stream().filter(stack -> stack.is(railing().asItem())).mapToInt(ItemStack::getCount).sum(),
         railings, "loot must include only the standalone base plus purchased railing additions");
      helper.assertValueEqual(drops.stream().mapToInt(ItemStack::getCount).sum(), catwalks + railings, "loot must contain no extra items");
   }

   private static void breakCenter(GameTestHelper helper, ServerPlayer player) {
      // A real survival game-mode break needs a harvesting tool for the steel blocks.
      ItemStack held = player.getMainHandItem();
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
      helper.assertTrue(player.getInventory().add(held), "test inventory must retain the previous held railings while equipping a tool");
      helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(CENTER)), "native game mode must break the block");
      helper.assertTrue(helper.getBlockState(CENTER).isAir() || helper.getBlockState(CENTER).is(Blocks.WATER),
         "breaking must remove the original block");
   }

   private static void clickEdge(GameTestHelper helper, ServerPlayer player, BlockPos relative, Direction edge, Direction facing) {
      clickEdge(helper, player, relative, edge, facing, InteractionHand.MAIN_HAND);
   }

   private static void clickEdge(GameTestHelper helper, ServerPlayer player, BlockPos relative, Direction edge, Direction facing,
      InteractionHand hand) {
      BlockPos pos = helper.absolutePos(relative);
      player.setPos(pos.getX() + 0.5, helper.absolutePos(CENTER).getY(), pos.getZ() + 2.0);
      player.setYRot(facing.toYRot());
      Vec3 hitPoint = new Vec3(pos.getX() + 0.5 + edge.getStepX() * 0.45,
         pos.getY() + 1.0, pos.getZ() + 0.5 + edge.getStepZ() * 0.45);
      player.gameMode.useItemOn(player, helper.getLevel(), player.getItemInHand(hand), hand,
         new BlockHitResult(hitPoint, Direction.UP, pos, false));
   }

   private static void raycastClickEdge(GameTestHelper helper, ServerPlayer player, Direction edge, Direction facing, BlockPos expectedTarget) {
      BlockPos pos = helper.absolutePos(CENTER);
      double x = pos.getX() + 0.5 + edge.getStepX() * 0.45;
      double z = pos.getZ() + 0.5 + edge.getStepZ() * 0.45;
      Vec3 from = new Vec3(x, pos.getY() + 2.5, z);
      Vec3 to = new Vec3(x, pos.getY() - 0.5, z);
      player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 2.0);
      player.setYRot(facing.toYRot());
      BlockHitResult hit = helper.getLevel().clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
      helper.assertValueEqual(hit.getType(), HitResult.Type.BLOCK, "real raycast must hit a physical selectable surface");
      helper.assertValueEqual(hit.getBlockPos(), helper.absolutePos(expectedTarget), "real raycast must hit the expected support or railing");
      helper.assertValueEqual(hit.getDirection(), Direction.UP, "vertical raycast must hit the top face");
      player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
   }

   private static void put(GameTestHelper helper, BlockPos relative, BlockState state) {
      helper.getLevel().setBlock(helper.absolutePos(relative), state, 2);
   }

   private static void clear(GameTestHelper helper) {
      helper.getLevel().getEntitiesOfClass(ItemEntity.class, dropArea(helper)).forEach(ItemEntity::discard);
      for (BlockPos pos : BlockPos.betweenClosed(CENTER.offset(-2, 0, -2), CENTER.offset(2, 2, 2))) {
         put(helper, pos, Blocks.AIR.defaultBlockState());
      }
      for (BlockPos pos : BlockPos.betweenClosed(CENTER.offset(-2, -1, -2), CENTER.offset(2, -1, 2))) {
         put(helper, pos, Blocks.STONE.defaultBlockState());
      }
   }
}
