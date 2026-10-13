package com.oblixorprime.engineersdecorreforged.gametest;

import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Mining regressions through the native server game mode, using only pre-fix APIs. */
@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class SelectiveRailingMiningGameTests {
   private static final BlockPos CENTER = new BlockPos(3, 1, 3);
   private static final Direction[] EDGES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
   private static final BooleanProperty[] SIDES = {PortedBlocks.NORTH, PortedBlocks.EAST, PortedBlocks.SOUTH, PortedBlocks.WEST};

   private SelectiveRailingMiningGameTests() {
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_mining_removes_only_aimed_standalone_side_in_every_direction(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (int edge = 0; edge < EDGES.length; edge++) {
         put(helper, sides(railing(), 15));
         mine(helper, player, EDGES[edge]);
         assertSides(helper, railing(), 15 & ~(1 << edge), "mining one standalone side must preserve the other three");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_catwalk_mining_distinguishes_a_railing_from_the_floor(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      put(helper, sides(catwalk(), 15));
      mine(helper, player, Direction.NORTH);
      assertSides(helper, catwalk(), 14, "mining the north rail must preserve the catwalk floor and other rails");
      mine(helper, player, null);
      helper.assertTrue(helper.getBlockState(CENTER).isAir(), "mining the actual catwalk floor must remove the whole catwalk");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_standalone_mining_removes_four_sides_one_at_a_time_until_empty(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      put(helper, sides(railing(), 15));
      int mask = 15;
      for (int edge = 0; edge < EDGES.length; edge++) {
         mine(helper, player, EDGES[edge]);
         mask &= ~(1 << edge);
         if (mask == 0) {
            helper.assertTrue(helper.getBlockState(CENTER).isAir(), "mining the final standalone railing must remove the block");
         } else {
            assertSides(helper, railing(), mask, "each completed mine must remove exactly one standalone railing");
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 400)
   public static void native_start_stop_packets_preserve_mining_duration_and_wear_the_tool_once(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      put(helper, sides(railing(), 15).setValue(PortedBlocks.PAID_RAILINGS, 3));
      BlockState original = helper.getBlockState(CENTER);
      BlockHitResult hit = aim(helper, player, Direction.NORTH);
      float progress = original.getDestroyProgress(player, helper.getLevel(), hit.getBlockPos());
      helper.assertTrue(progress > 0.0F && progress < 1.0F, "steel must require multiple native mining ticks with this tool");
      int ticks = (int)Math.ceil(1.0F / progress) + 2;
      helper.assertTrue(ticks < 350, "native mining duration must fit this test's bounded timeout");
      start(helper, player, hit);
      action(helper, player, hit, ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK);
      helper.assertValueEqual(helper.getBlockState(CENTER), original, "early STOP must wait for accumulated mining progress");
      helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 0, "starting mining must not wear the tool yet");
      tickMining(helper, player, ticks, () -> {
         assertSides(helper, railing(), 14, "the normal delayed native break must remove only the selected rail");
         helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 1, "a completed mining cycle wears the main-hand pickaxe exactly once");
         helper.assertValueEqual(dropped(helper, railing().asItem()), 1, "normal-duration mining drops exactly one purchased rail");
         helper.succeed();
      });
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_mining_returns_only_paid_material_and_final_standalone_base(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Block block : List.of(catwalk(), railing())) {
         clearDrops(helper);
         put(helper, sides(block, 15).setValue(PortedBlocks.PAID_RAILINGS, block == catwalk() ? 4 : 3));
         for (int edge = 0; edge < EDGES.length; edge++) {
            mine(helper, player, EDGES[edge]);
            helper.assertValueEqual(dropped(helper, railing().asItem()), edge + 1, "each paid rail or final standalone base must drop once");
            helper.assertValueEqual(dropped(helper, catwalk().asItem()), 0, "mining a rail must never drop the catwalk floor");
         }
         if (block == catwalk()) {
            assertSides(helper, catwalk(), 0, "all four rails can be mined without removing the floor");
            mine(helper, player, null);
            helper.assertValueEqual(dropped(helper, catwalk().asItem()), 1, "the actual floor drops exactly one catwalk");
         } else {
            helper.assertTrue(helper.getBlockState(CENTER).isAir(), "the final standalone base must disappear");
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_legacy_and_implicit_north_mining_never_mints_unpaid_railings(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Block block : List.of(catwalk(), railing())) {
         clearDrops(helper);
         put(helper, sides(block, 15));
         for (int edge = 0; edge < EDGES.length; edge++) {
            mine(helper, player, EDGES[edge]);
            int expected = block == railing() && edge == 3 ? 1 : 0;
            helper.assertValueEqual(dropped(helper, railing().asItem()), expected, "legacy rails have no paid credits; only the final standalone base returns");
         }
      }
      clearDrops(helper);
      put(helper, sides(railing(), 0));
      mine(helper, player, Direction.NORTH);
      helper.assertTrue(helper.getBlockState(CENTER).isAir(), "a legacy implicit north rail must be mineable");
      helper.assertValueEqual(dropped(helper, railing().asItem()), 1, "legacy implicit north returns only its base item");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_creative_mining_consumes_paid_credit_without_drops_or_tool_wear(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Block block : List.of(catwalk(), railing())) {
         clearDrops(helper);
         put(helper, sides(block, 15).setValue(PortedBlocks.PAID_RAILINGS, block == catwalk() ? 4 : 3));
         player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
         int damage = player.getMainHandItem().getDamageValue();
         mine(helper, player, Direction.NORTH);
         assertSides(helper, block, 14, "creative mining still removes only the aimed rail");
         helper.assertValueEqual(helper.getBlockState(CENTER).getValue(PortedBlocks.PAID_RAILINGS), block == catwalk() ? 3 : 2,
            "creative removal consumes its paid credit");
         helper.assertValueEqual(dropped(helper, railing().asItem()), 0, "creative removal produces no rail item");
         helper.assertValueEqual(player.getMainHandItem().getDamageValue(), damage, "creative mining must not damage the tool");
         player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
         for (Direction edge : List.of(Direction.EAST, Direction.SOUTH, Direction.WEST)) {
            mine(helper, player, edge);
         }
         helper.assertValueEqual(dropped(helper, railing().asItem()), 3, "survival cannot reclaim the credit destroyed in creative");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_wrong_tool_and_disabled_tile_drops_consume_rails_without_refunds(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      boolean originalRule = helper.getLevel().getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS);
      try {
         for (Block block : List.of(catwalk(), railing())) {
            for (boolean correctTool : new boolean[]{false, true}) {
               clearDrops(helper);
               put(helper, sides(block, 15).setValue(PortedBlocks.PAID_RAILINGS, block == catwalk() ? 4 : 3));
               player.setItemInHand(InteractionHand.MAIN_HAND, correctTool ? new ItemStack(Items.IRON_PICKAXE) : ItemStack.EMPTY);
               helper.getLevel().getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS).set(!correctTool, helper.getLevel().getServer());
               mine(helper, player, Direction.NORTH);
               assertSides(helper, block, 14, "a non-harvesting mine still removes only its selected side");
               helper.assertValueEqual(dropped(helper, railing().asItem()), 0, "wrong tool or doTileDrops=false must suppress refunds");
               helper.assertValueEqual(helper.getBlockState(CENTER).getValue(PortedBlocks.PAID_RAILINGS), block == catwalk() ? 3 : 2,
                  "suppressed loot must not leave a reusable paid credit");
               for (Direction edge : List.of(Direction.EAST, Direction.SOUTH, Direction.WEST)) {
                  mine(helper, player, edge);
               }
               if (block == catwalk()) {
                  mine(helper, player, null);
               }
               helper.assertTrue(helper.getBlockState(CENTER).isAir(), "non-harvesting completion still removes the last rail or floor");
               helper.assertValueEqual(dropped(helper, railing().asItem()), 0, "suppressed harvesting must also suppress the final standalone base");
               helper.assertValueEqual(dropped(helper, catwalk().asItem()), 0, "suppressed harvesting must not drop the catwalk base");
            }
         }
      } finally {
         helper.getLevel().getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS).set(originalRule, helper.getLevel().getServer());
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_selective_mining_uses_the_main_hand_tool_exactly_once(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
      put(helper, sides(catwalk(), 15).setValue(PortedBlocks.PAID_RAILINGS, 4));
      mine(helper, player, Direction.WEST);
      helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 1, "selective mining must use the main-hand tool once");
      helper.assertValueEqual(player.getOffhandItem().getDamageValue(), 0, "selective mining must never use the offhand tool");
      mine(helper, player, null);
      helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 2, "floor mining must use the main-hand tool once too");
      helper.assertValueEqual(dropped(helper, railing().asItem()), 4, "side then floor mining must return all four paid rails exactly once");
      helper.assertValueEqual(dropped(helper, catwalk().asItem()), 1, "floor mining drops one catwalk");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void last_durability_pickaxe_still_drops_the_one_harvested_rail(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Block block : List.of(catwalk(), railing())) {
         clearDrops(helper);
         ItemStack lastUse = new ItemStack(Items.IRON_PICKAXE);
         lastUse.setDamageValue(lastUse.getMaxDamage() - 1);
         player.setItemInHand(InteractionHand.MAIN_HAND, lastUse);
         put(helper, sides(block, 15).setValue(PortedBlocks.PAID_RAILINGS, block == catwalk() ? 4 : 3));
         mine(helper, player, Direction.EAST);
         assertSides(helper, block, 13, "the pickaxe's final use must remove only the selected rail");
         helper.assertTrue(player.getMainHandItem().isEmpty(), "the native mining callback must consume the final tool durability");
         helper.assertValueEqual(dropped(helper, railing().asItem()), 1, "a tool that breaks while mining must still harvest its paid rail");
         helper.assertValueEqual(dropped(helper, catwalk().asItem()), 0, "the final tool use must not release whole-catwalk loot");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void cancelled_native_block_drops_removes_only_the_side_and_consumes_its_credit(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      put(helper, sides(catwalk(), 15).setValue(PortedBlocks.PAID_RAILINGS, 4));
      int[] events = {0};
      Consumer<BlockDropsEvent> cancel = event -> {
         if (event.getBreaker() == player) {
            events[0]++;
            event.setCanceled(true);
         }
      };
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, BlockDropsEvent.class, cancel);
      try {
         mine(helper, player, Direction.NORTH);
         assertSides(helper, catwalk(), 14, "loot cancellation must not roll back the completed side removal");
         helper.assertValueEqual(events[0], 1, "a selective paid removal must enter the standard block-drop hook exactly once");
         helper.assertValueEqual(dropped(helper, railing().asItem()), 0, "cancelled BlockDropsEvent must suppress the paid rail drop");
         helper.assertValueEqual(helper.getBlockState(CENTER).getValue(PortedBlocks.PAID_RAILINGS), 3,
            "cancelled loot must still spend the removed rail's credit");
      } finally {
         NeoForge.EVENT_BUS.unregister(cancel);
      }
      mine(helper, player, null);
      helper.assertValueEqual(dropped(helper, railing().asItem()), 3, "later floor mining must not revive the cancelled paid drop");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void cancelled_native_break_event_preserves_side_credits_loot_and_tool(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      put(helper, sides(catwalk(), 15).setValue(PortedBlocks.PAID_RAILINGS, 4));
      BlockState original = helper.getBlockState(CENTER);
      Consumer<BlockEvent.BreakEvent> cancel = event -> {
         if (event.getPlayer() == player) {
            event.setCanceled(true);
         }
      };
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, BlockEvent.BreakEvent.class, cancel);
      try {
         mine(helper, player, Direction.NORTH);
         helper.assertValueEqual(helper.getBlockState(CENTER), original, "cancelled BreakEvent must preserve every state property");
         helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 0, "cancelled BreakEvent must precede native tool wear");
         helper.assertValueEqual(dropped(helper, railing().asItem()), 0, "cancelled BreakEvent must produce no loot");
      } finally {
         NeoForge.EVENT_BUS.unregister(cancel);
      }
      mine(helper, player, Direction.EAST);
      assertSides(helper, catwalk(), 13, "a later uncancelled attempt must mine its own current target");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void cancelled_native_left_click_cannot_instantly_mine_or_refund(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      put(helper, sides(railing(), 15).setValue(PortedBlocks.PAID_RAILINGS, 3));
      BlockState original = helper.getBlockState(CENTER);
      Consumer<PlayerInteractEvent.LeftClickBlock> cancel = event -> {
         if (event.getEntity() == player) {
            event.setCanceled(true);
         }
      };
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, PlayerInteractEvent.LeftClickBlock.class, cancel);
      try {
         BlockHitResult hit = aim(helper, player, Direction.NORTH);
         start(helper, player, hit);
         action(helper, player, hit, ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK);
         for (int tick = 0; tick < 40; tick++) {
            player.gameMode.tick();
         }
         helper.assertValueEqual(helper.getBlockState(CENTER), original, "cancelled native left-click packets must preserve all railings and credits");
         helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 0, "cancelled left click must not wear the tool");
         helper.assertValueEqual(dropped(helper, railing().asItem()), 0, "cancelled left click must not refund a rail");
      } finally {
         NeoForge.EVENT_BUS.unregister(cancel);
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void stale_rail_target_cannot_become_a_floor_or_replacement_break(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (BlockState replacement : List.of(sides(catwalk(), 14).setValue(PortedBlocks.PAID_RAILINGS, 3),
         sides(catwalk(), 15).setValue(PortedBlocks.PAID_RAILINGS, 3), sides(railing(), 15), Blocks.STONE.defaultBlockState(), Blocks.AIR.defaultBlockState())) {
         clearDrops(helper);
         put(helper, sides(catwalk(), 15).setValue(PortedBlocks.PAID_RAILINGS, 4));
         start(helper, player, aim(helper, player, Direction.NORTH));
         put(helper, replacement);
         player.gameMode.destroyBlock(helper.absolutePos(CENTER));
         helper.assertValueEqual(helper.getBlockState(CENTER), replacement, "completion after target replacement must never mine a different rail, floor or block");
         helper.assertValueEqual(dropped(helper, railing().asItem()), 0, "stale mining target must not produce rail loot");
         helper.assertValueEqual(dropped(helper, catwalk().asItem()), 0, "stale rail target must not fall through to whole-catwalk loot");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void changed_eye_target_and_abort_require_a_fresh_mining_start(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      put(helper, sides(catwalk(), 15).setValue(PortedBlocks.PAID_RAILINGS, 4));
      BlockState original = helper.getBlockState(CENTER);
      start(helper, player, aim(helper, player, Direction.NORTH));
      aim(helper, player, null);
      player.gameMode.destroyBlock(helper.absolutePos(CENTER));
      helper.assertValueEqual(helper.getBlockState(CENTER), original, "a rail mining start must not authorize a later floor target");
      BlockHitResult north = aim(helper, player, Direction.NORTH);
      start(helper, player, north);
      action(helper, player, north, ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK);
      action(helper, player, north, ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK);
      for (int tick = 0; tick < 40; tick++) {
         player.gameMode.tick();
      }
      helper.assertValueEqual(helper.getBlockState(CENTER), original, "aborted mining must not complete on a later STOP");
      mine(helper, player, Direction.EAST);
      assertSides(helper, catwalk(), 13, "a fresh start after retargeting must remove only the newly aimed side");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_eye_ray_selects_near_rail_and_does_not_mine_through_an_occluder(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      BlockPos pos = helper.absolutePos(CENTER);
      put(helper, sides(railing(), 15));
      player.setPos(pos.getX() + 0.5, pos.getY() + 0.75 - player.getEyeHeight(), pos.getZ() - 2.0);
      player.setYRot(0.0F);
      player.setXRot(0.0F);
      BlockHitResult near = eyeHit(helper, player);
      helper.assertValueEqual(near.getBlockPos(), pos, "the actual horizontal ray must hit the target railing");
      helper.assertValueEqual(near.getDirection(), Direction.NORTH, "the north rail must occlude the far south rail");
      start(helper, player, near);
      player.gameMode.destroyBlock(pos);
      assertSides(helper, railing(), 14, "horizontal mining must select the nearest intersected rail");

      put(helper, sides(catwalk(), 15));
      aim(helper, player, Direction.NORTH);
      helper.getLevel().setBlock(pos.above(), Blocks.STONE.defaultBlockState(), 2);
      try {
         helper.assertValueEqual(eyeHit(helper, player).getBlockPos(), pos.above(), "a real intervening block must occlude the catwalk");
         BlockState original = helper.getBlockState(CENTER);
         action(helper, player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false),
            ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
         player.gameMode.destroyBlock(pos);
         helper.assertValueEqual(helper.getBlockState(CENTER), original, "a packet naming an occluded railing cannot bypass the actual eye ray");
      } finally {
         helper.getLevel().setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2);
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_support_mining_does_not_redirect_into_a_standalone_railing(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      put(helper, sides(railing(), 1));
      BlockPos pos = helper.absolutePos(CENTER);
      player.setPos(pos.getX() + 0.5, pos.getY() + 2.5 - player.getEyeHeight(), pos.getZ() + 0.5);
      player.setYRot(0.0F);
      player.setXRot(90.0F);
      BlockHitResult support = eyeHit(helper, player);
      helper.assertValueEqual(support.getBlockPos(), pos.below(), "the open standalone center must expose the actual supporting block");
      start(helper, player, support);
      player.gameMode.destroyBlock(support.getBlockPos());
      helper.assertTrue(helper.getBlockState(CENTER.below()).isAir(), "mining a support must remove that support");
      assertSides(helper, railing(), 1, "support mining must not be redirected into the railing above");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void mined_sides_and_paid_balance_survive_neighbors_and_nbt_reload(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (Block block : List.of(catwalk(), railing())) {
         put(helper, sides(block, 15).setValue(PortedBlocks.PAID_RAILINGS, block == catwalk() ? 4 : 3)
            .setValue(PortedBlocks.WATERLOGGED, true));
         mine(helper, player, Direction.EAST);
         assertSides(helper, block, 13, "the selected east side must be absent before reload");
         BlockState mined = helper.getBlockState(CENTER);
         for (Direction direction : EDGES) {
            BlockPos neighbor = helper.absolutePos(CENTER.relative(direction));
            helper.getLevel().setBlock(neighbor, Blocks.STONE.defaultBlockState(), 3);
            helper.getLevel().setBlock(neighbor, Blocks.AIR.defaultBlockState(), 3);
         }
         helper.assertValueEqual(helper.getBlockState(CENTER), mined, "neighbor updates must not restore a mined side or paid credit");
         var saved = NbtUtils.writeBlockState(mined);
         BlockState loaded = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), saved);
         helper.assertValueEqual(loaded, mined, "NBT round trip must preserve the mined shape and paid balance");
         helper.assertValueEqual(BlockState.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow(), mined, "codec round trip must preserve the complete state");
         put(helper, loaded);
         for (Direction edge : List.of(Direction.NORTH, Direction.SOUTH, Direction.WEST)) {
            mine(helper, player, edge);
         }
         if (block == railing()) {
            helper.assertTrue(helper.getBlockState(CENTER).is(Blocks.WATER), "final waterlogged standalone mining must restore source water");
         } else {
            helper.assertBlockProperty(CENTER, PortedBlocks.WATERLOGGED, true);
         }
      }
      helper.succeed();
   }

   private static ServerPlayer player(GameTestHelper helper) {
      ServerPlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "RailMiningTest"));
      player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
      player.setOnGround(true);
      clearDrops(helper);
      helper.getLevel().setBlock(helper.absolutePos(CENTER.below()), Blocks.STONE.defaultBlockState(), 2);
      return player;
   }

   private static Block catwalk() {
      return ModBlocks.STEEL_CATWALK.get();
   }

   private static Block railing() {
      return ModBlocks.STEEL_RAILING.get();
   }

   private static BlockState sides(Block block, int mask) {
      BlockState state = block.defaultBlockState();
      for (int edge = 0; edge < SIDES.length; edge++) {
         state = state.setValue(SIDES[edge], (mask & (1 << edge)) != 0);
      }
      return state;
   }

   private static void put(GameTestHelper helper, BlockState state) {
      helper.getLevel().setBlock(helper.absolutePos(CENTER), state, 2);
   }

   private static void assertSides(GameTestHelper helper, Block block, int mask, String context) {
      BlockState state = helper.getBlockState(CENTER);
      helper.assertTrue(state.is(block), context + ": expected " + block + ", got " + state);
      for (int edge = 0; edge < SIDES.length; edge++) {
         helper.assertValueEqual(state.getValue(SIDES[edge]), (mask & (1 << edge)) != 0,
            context + ": " + EDGES[edge]);
      }
   }

   /** null selects the real, unobstructed floor at the center of a catwalk. */
   private static BlockHitResult aim(GameTestHelper helper, ServerPlayer player, Direction edge) {
      BlockPos pos = helper.absolutePos(CENTER);
      double x = pos.getX() + 0.5 + (edge == null ? 0.0 : edge.getStepX() * 0.45);
      double z = pos.getZ() + 0.5 + (edge == null ? 0.0 : edge.getStepZ() * 0.45);
      player.setPos(x, pos.getY() + 2.5 - player.getEyeHeight(), z);
      player.setYRot(0.0F);
      player.setXRot(90.0F);
      BlockHitResult hit = eyeHit(helper, player);
      helper.assertValueEqual(hit.getType(), HitResult.Type.BLOCK, "the player's actual eye ray must hit a physical surface");
      helper.assertValueEqual(hit.getBlockPos(), pos, "the player's actual eye ray must target the railing or catwalk");
      helper.assertValueEqual(hit.getDirection(), Direction.UP, "the downward eye ray must hit the selected top surface");
      return hit;
   }

   private static void start(GameTestHelper helper, ServerPlayer player, BlockHitResult hit) {
      action(helper, player, hit, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
   }

   private static void action(GameTestHelper helper, ServerPlayer player, BlockHitResult hit,
      ServerboundPlayerActionPacket.Action action) {
      player.gameMode.handleBlockBreakAction(hit.getBlockPos(), action, hit.getDirection(), helper.getLevel().getMaxBuildHeight(), 0);
   }

   private static BlockHitResult eyeHit(GameTestHelper helper, ServerPlayer player) {
      Vec3 eye = player.getEyePosition();
      return helper.getLevel().clip(new ClipContext(eye,
         eye.add(player.getViewVector(1.0F).scale(player.blockInteractionRange())),
         ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
   }

   private static AABB dropArea(GameTestHelper helper) {
      return new AABB(helper.absolutePos(CENTER)).inflate(1.5);
   }

   private static int dropped(GameTestHelper helper, Item item) {
      return helper.getLevel().getEntitiesOfClass(ItemEntity.class, dropArea(helper), entity -> entity.getItem().is(item))
         .stream().mapToInt(entity -> entity.getItem().getCount()).sum();
   }

   private static void clearDrops(GameTestHelper helper) {
      helper.getLevel().getEntitiesOfClass(ItemEntity.class, dropArea(helper)).forEach(ItemEntity::discard);
   }

   private static void tickMining(GameTestHelper helper, ServerPlayer player, int remaining, Runnable complete) {
      if (remaining == 0) {
         complete.run();
         return;
      }
      helper.runAfterDelay(1L, () -> {
         // FakePlayer is deliberately not in the live player list, so drive its game-mode tick explicitly.
         player.gameMode.tick();
         tickMining(helper, player, remaining - 1, complete);
      });
   }

   private static void mine(GameTestHelper helper, ServerPlayer player, Direction edge) {
      BlockState before = helper.getBlockState(CENTER);
      start(helper, player, aim(helper, player, edge));
      if (player.gameMode.isCreative()) {
         return; // Creative START performs its native immediate completion.
      }
      helper.assertValueEqual(helper.getBlockState(CENTER), before,
         "an ordinary survival START packet must not instantly remove a railing or floor");
      // Completion enters the vanilla late-break path: BreakEvent, tool use, onDestroyedByPlayer and loot.
      // Selective removal intentionally returns false to suppress whole-block destruction and whole-block loot.
      player.gameMode.destroyBlock(helper.absolutePos(CENTER));
   }
}
