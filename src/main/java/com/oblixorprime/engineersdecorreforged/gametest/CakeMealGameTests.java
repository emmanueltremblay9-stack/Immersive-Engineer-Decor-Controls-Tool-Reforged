package com.oblixorprime.engineersdecorreforged.gametest;

import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.stool.CakeMealEvents;
import com.oblixorprime.engineersdecorreforged.stool.StoolSeatEntity;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CakeMealGameTests {
   private static final BlockPos TABLE = new BlockPos(3, 2, 2);
   private static final BlockPos CAKE = TABLE.above();
   private static final BlockPos STOOL = TABLE.west();

   private CakeMealGameTests() {
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void cake_is_placed_with_its_real_vanilla_item_before_eating_a_slice(GameTestHelper helper) {
      prepare(helper, ModBlocks.TREATED_WOOD_TABLE.get(), 0);
      helper.setBlock(CAKE, Blocks.AIR);
      ServerPlayer player = player(helper);
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CAKE, 2));
      BlockPos table = helper.absolutePos(TABLE);
      BlockHitResult hit = new BlockHitResult(Vec3.atBottomCenterOf(table.above()), Direction.UP, table, false);
      player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
      helper.assertTrue(helper.getBlockState(CAKE).is(Blocks.CAKE), "native cake BlockItem must place real cake on the treated table");
      helper.assertBlockProperty(CAKE, CakeBlock.BITES, 0);
      helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "native placement consumes exactly one cake item");
      assertSlice(helper, player, 0, 8, 0.0F, "placing cake is not eating");
      player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
      click(helper, player);
      assertSlice(helper, player, 1, 10, 0.44F, "actual slice of the natively placed cake");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void candle_cake_extinguishing_is_not_eating_but_its_real_slice_qualifies(GameTestHelper helper) {
      prepare(helper, ModBlocks.TREATED_WOOD_TABLE.get(), 0);
      helper.setBlock(CAKE, Blocks.CANDLE_CAKE.defaultBlockState().setValue(CandleCakeBlock.LIT, true));
      ServerPlayer player = player(helper);
      BlockPos cakePos = helper.absolutePos(CAKE);
      BlockHitResult candleHit = new BlockHitResult(Vec3.atLowerCornerOf(cakePos).add(0.5, 0.75, 0.5), Direction.UP, cakePos, false);
      player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, candleHit);
      helper.assertTrue(helper.getBlockState(CAKE).is(Blocks.CANDLE_CAKE), "extinguishing must keep the candle cake");
      helper.assertBlockProperty(CAKE, CandleCakeBlock.LIT, false);
      assertSlice(helper, player, 0, 8, 0.0F, "extinguishing a lit candle is not eating");
      click(helper, player);
      assertSlice(helper, player, 1, 10, 0.44F, "first native slice from candle cake qualifies");
      helper.assertTrue(helper.getBlockState(CAKE).is(Blocks.CAKE), "native slice converts candle cake to plain cake");
      helper.assertBlockProperty(CAKE, CakeBlock.BITES, 1);
      int candles = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(cakePos).inflate(2.0)).stream()
         .filter(entity -> entity.getItem().is(Items.CANDLE)).mapToInt(entity -> entity.getItem().getCount()).sum();
      helper.assertValueEqual(candles, 1, "native first-slice candle drop must not be duplicated");
      helper.setBlock(CAKE, Blocks.RED_CANDLE_CAKE.defaultBlockState());
      ServerPlayer colored = player(helper);
      click(helper, colored);
      assertSlice(helper, colored, 1, 10, 0.44F, "colored vanilla candle cake also uses native slice proof");
      helper.assertBlockProperty(CAKE, CakeBlock.BITES, 1);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_cake_on_treated_table_adds_ten_percent_only_once_per_slice(GameTestHelper helper) {
      prepare(helper, ModBlocks.TREATED_WOOD_TABLE.get(), 0);
      ServerPlayer player = player(helper);
      Consumer<VanillaGameEvent> duplicateEat = event -> {
         if (event.getCause() == player) {
            CakeMealEvents.onNativeEat(event);
         }
      };
      Consumer<BlockEvent.NeighborNotifyEvent> duplicateUpdate = CakeMealEvents::onCakeStateChanged;
      NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, VanillaGameEvent.class, duplicateEat);
      NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, BlockEvent.NeighborNotifyEvent.class, duplicateUpdate);
      try {
         click(helper, player);
         assertSlice(helper, player, 1, 10, 0.44F, "standing native cake slice");
         helper.assertBlockProperty(CAKE, CakeBlock.BITES, 1);
         click(helper, player);
         assertSlice(helper, player, 2, 12, 0.88F, "second real slice in the same world tick");
         helper.assertBlockProperty(CAKE, CakeBlock.BITES, 2);
         settle(helper);
         settle(helper);
         close(helper, player.getFoodData().getSaturationLevel(), 0.88F, "duplicate callbacks and later ticks never repeat a cake bonus");
         helper.assertTrue(player.getMainHandItem().isEmpty(), "cake slices do not create fake cake items or containers");
      } finally {
         NeoForge.EVENT_BUS.unregister(duplicateEat);
         NeoForge.EVENT_BUS.unregister(duplicateUpdate);
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void steel_crafting_and_unrelated_supports_never_grant_standing_cake_bonus(GameTestHelper helper) {
      for (Block support : List.of(ModBlocks.STEEL_TABLE.get(), ModBlocks.METAL_CRAFTING_TABLE.get(), Blocks.CRAFTING_TABLE, Blocks.STONE)) {
         prepare(helper, support, 0);
         // Even a treated table beside the cake does not count: it must support this cake directly.
         helper.setBlock(TABLE.north(), ModBlocks.TREATED_WOOD_TABLE.get());
         ServerPlayer player = player(helper);
         click(helper, player);
         assertSlice(helper, player, 1, 10, 0.4F, "standing cake on non-treated support");
         helper.setBlock(TABLE.north(), Blocks.AIR);
      }
      prepare(helper, ModBlocks.TREATED_WOOD_TABLE.get(), 0);
      helper.setBlock(TABLE, Blocks.STONE);
      ServerPlayer player = player(helper);
      click(helper, player);
      assertSlice(helper, player, 1, 10, 0.4F, "removed treated table cannot qualify a later slice");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_cake_uses_seated_twenty_or_thirty_percent_without_stacking_standing_bonus(GameTestHelper helper) {
      for (boolean treated : List.of(false, true)) {
         prepare(helper, treated ? ModBlocks.TREATED_WOOD_TABLE.get() : Blocks.STONE, 0);
         helper.setBlock(STOOL, ModBlocks.TREATED_WOOD_STOOL.get());
         ServerPlayer player = player(helper);
         BlockPos stool = helper.absolutePos(STOOL);
         player.setPos(stool.getX() + 0.5, stool.getY(), stool.getZ() + 1.5);
         helper.useBlock(STOOL, player);
         helper.assertTrue(player.getVehicle() instanceof StoolSeatEntity, "cake test must actually sit on the registered treated stool");
         click(helper, player);
         assertSlice(helper, player, 1, 10, treated ? 0.52F : 0.48F, "seated cake uses one replacement rate");
         player.stopRiding();
      }
      prepare(helper, ModBlocks.TREATED_WOOD_TABLE.get(), 0);
      ServerPlayer mounted = player(helper);
      Minecart cart = EntityType.MINECART.create(helper.getLevel());
      helper.assertTrue(cart != null, "native minecart fixture exists");
      cart.setPos(mounted.position());
      helper.getLevel().addFreshEntity(cart);
      helper.assertTrue(mounted.startRiding(cart), "other-mount negative control must really be mounted");
      click(helper, mounted);
      assertSlice(helper, mounted, 1, 10, 0.4F, "another mount is neither standing nor the treated stool");
      mounted.stopRiding();
      cart.discard();
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void two_players_and_last_native_slice_each_receive_their_own_bonus(GameTestHelper helper) {
      prepare(helper, ModBlocks.TREATED_WOOD_TABLE.get(), 5);
      ServerPlayer first = player(helper);
      ServerPlayer second = player(helper);
      click(helper, first);
      assertSlice(helper, first, 1, 10, 0.44F, "first player's penultimate slice");
      helper.assertBlockProperty(CAKE, CakeBlock.BITES, 6);
      click(helper, second);
      assertSlice(helper, second, 1, 10, 0.44F, "second player's last slice");
      helper.assertTrue(helper.getBlockState(CAKE).isAir(), "native final slice removes the cake");
      settle(helper);
      close(helper, first.getFoodData().getSaturationLevel(), 0.44F, "another player never receives or duplicates this slice's bonus");
      close(helper, second.getFoodData().getSaturationLevel(), 0.44F, "last-slice receipt is consumed exactly once");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void denied_native_cake_interactions_cannot_award_saturation(GameTestHelper helper) {
      for (boolean cancel : List.of(false, true)) {
         prepare(helper, ModBlocks.TREATED_WOOD_TABLE.get(), 0);
         ServerPlayer player = player(helper);
         Consumer<PlayerInteractEvent.RightClickBlock> listener = event -> {
            if (event.getEntity() == player) {
               if (cancel) {
                  event.setCanceled(true);
                  event.setCancellationResult(InteractionResult.FAIL);
               } else {
                  event.setUseBlock(TriState.FALSE);
               }
            }
         };
         NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, PlayerInteractEvent.RightClickBlock.class, listener);
         try {
            click(helper, player);
            settle(helper);
            assertSlice(helper, player, 0, 8, 0.0F, "cancelled/denied native block interaction");
            helper.assertBlockProperty(CAKE, CakeBlock.BITES, 0);
         } finally {
            NeoForge.EVENT_BUS.unregister(listener);
         }
      }
      prepare(helper, ModBlocks.TREATED_WOOD_TABLE.get(), 0);
      ServerPlayer player = player(helper);
      Consumer<UseItemOnBlockEvent> refusePhase = event -> {
         if (event.getPlayer() == player && event.getUsePhase() == UseItemOnBlockEvent.UsePhase.BLOCK) {
            event.cancelWithResult(ItemInteractionResult.FAIL);
         }
      };
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, UseItemOnBlockEvent.class, refusePhase);
      try {
         click(helper, player);
         settle(helper);
         assertSlice(helper, player, 0, 8, 0.0F, "cancelled native block phase");
         helper.assertBlockProperty(CAKE, CakeBlock.BITES, 0);
      } finally {
         NeoForge.EVENT_BUS.unregister(refusePhase);
      }
      ServerPlayer spectator = player(helper);
      spectator.gameMode.changeGameModeForPlayer(GameType.SPECTATOR);
      click(helper, spectator);
      settle(helper);
      assertSlice(helper, spectator, 0, 8, 0.0F, "spectator cannot consume cake or receive a meal bonus");
      helper.assertBlockProperty(CAKE, CakeBlock.BITES, 0);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void merely_clicking_breaking_or_emitting_eat_never_creates_a_cake_meal(GameTestHelper helper) {
      prepare(helper, ModBlocks.TREATED_WOOD_TABLE.get(), 6);
      ServerPlayer player = player(helper);
      player.getFoodData().setFoodLevel(20);
      click(helper, player);
      assertSlice(helper, player, 0, 20, 0.0F, "full survival player does not eat cake");
      helper.getLevel().gameEvent(player, GameEvent.EAT, helper.absolutePos(CAKE));
      helper.setBlock(CAKE, Blocks.AIR);
      settle(helper);
      assertSlice(helper, player, 0, 20, 0.0F, "EAT without native slice stat plus destroyed cake is not consumption");
      prepare(helper, ModBlocks.TREATED_WOOD_TABLE.get(), 0);
      ServerPlayer candle = player(helper);
      candle.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CANDLE));
      click(helper, candle);
      settle(helper);
      helper.assertTrue(helper.getBlockState(CAKE).is(Blocks.CANDLE_CAKE), "native candle placement must still work");
      assertSlice(helper, candle, 0, 8, 0.0F, "adding a candle must never count as eating");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void suppressing_cake_vibration_does_not_cancel_the_native_meal_or_bonus(GameTestHelper helper) {
      prepare(helper, ModBlocks.TREATED_WOOD_TABLE.get(), 0);
      ServerPlayer player = player(helper);
      Consumer<VanillaGameEvent> silence = event -> {
         if (event.getCause() == player && event.getVanillaEvent() == GameEvent.EAT) {
            event.setCanceled(true);
         }
      };
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, VanillaGameEvent.class, silence);
      try {
         click(helper, player);
         assertSlice(helper, player, 1, 10, 0.44F, "cancelled vibration propagation still follows actual vanilla consumption");
      } finally {
         NeoForge.EVENT_BUS.unregister(silence);
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void standing_cake_bonus_respects_vanilla_hunger_cap_and_creative_consumption(GameTestHelper helper) {
      prepare(helper, ModBlocks.TREATED_WOOD_TABLE.get(), 0);
      ServerPlayer player = player(helper);
      player.getFoodData().setFoodLevel(19);
      player.getFoodData().setSaturation(19.9F);
      click(helper, player);
      assertSlice(helper, player, 1, 20, 20.0F, "standing cake cap");
      ServerPlayer creative = player(helper);
      creative.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
      click(helper, creative);
      assertSlice(helper, creative, 1, 10, 0.44F, "native creative cake consumption");
      helper.assertBlockProperty(CAKE, CakeBlock.BITES, 2);
      helper.succeed();
   }

   private static void prepare(GameTestHelper helper, Block table, int bites) {
      helper.setBlock(STOOL, Blocks.AIR);
      helper.setBlock(TABLE, table);
      helper.setBlock(CAKE, Blocks.CAKE.defaultBlockState().setValue(CakeBlock.BITES, bites));
      for (BlockPos floor : BlockPos.betweenClosed(STOOL.offset(-1, -1, -1), TABLE.offset(1, -1, 1))) {
         helper.setBlock(floor, Blocks.STONE);
      }
   }

   private static ServerPlayer player(GameTestHelper helper) {
      GameProfile profile = new GameProfile(UUID.randomUUID(), "CakeTest");
      ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), profile, ClientInformation.createDefault());
      // Reuse only NeoForge's no-op connection. This player itself is real ServerPlayer: native stats and mounting stay enabled.
      player.connection = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "CakeTransport")).connection;
      player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
      BlockPos absolute = helper.absolutePos(TABLE);
      player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 1.5);
      player.getFoodData().setFoodLevel(8);
      player.getFoodData().setSaturation(0.0F);
      player.getFoodData().setExhaustion(0.0F);
      return player;
   }

   private static void click(GameTestHelper helper, ServerPlayer player) {
      BlockPos pos = helper.absolutePos(CAKE);
      BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
      player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
   }

   private static void settle(GameTestHelper helper) {
      NeoForge.EVENT_BUS.post(new ServerTickEvent.Post(() -> true, helper.getLevel().getServer()));
   }

   private static void assertSlice(GameTestHelper helper, ServerPlayer player, int slices, int hunger, float saturation, String context) {
      helper.assertValueEqual(player.getStats().getValue(Stats.CUSTOM.get(Stats.EAT_CAKE_SLICE)), slices, context + ": native slice stat");
      helper.assertValueEqual(player.getFoodData().getFoodLevel(), hunger, context + ": native hunger");
      close(helper, player.getFoodData().getSaturationLevel(), saturation, context + ": saturation");
   }

   private static void close(GameTestHelper helper, float actual, float expected, String message) {
      helper.assertTrue(Math.abs(actual - expected) < 0.001F, message + ": expected " + expected + ", got " + actual);
   }
}
