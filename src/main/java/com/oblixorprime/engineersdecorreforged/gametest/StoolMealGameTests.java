package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.ModEntities;
import com.oblixorprime.engineersdecorreforged.stool.StoolMealEvents;
import com.oblixorprime.engineersdecorreforged.stool.StoolSeatEntity;
import com.oblixorprime.engineersdecorreforged.tools.EngineerToolsModule;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StoolMealGameTests {
   private static final BlockPos STOOL = new BlockPos(2, 2, 2);

   private StoolMealGameTests() {
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void stool_right_click_seats_one_player_and_dismount_removes_the_mount(GameTestHelper helper) {
      prepare(helper);
      Player player = player(helper, GameType.SURVIVAL);
      helper.useBlock(STOOL, player);
      StoolSeatEntity seat = seat(helper, player);
      helper.setBlock(STOOL.above(2), Blocks.STONE);
      helper.assertTrue(helper.getLevel().noCollision(player, player.getBoundingBox()), "fixture ceiling must clear the original seated body");
      helper.assertTrue(!seat.isPickable() && !seat.isPushable() && !seat.canBeCollidedWith(), "seat must not add a selectable or collidable hitbox");
      helper.assertTrue(helper.getBlockState(STOOL).getCollisionShape(helper.getLevel(), helper.absolutePos(STOOL),
         CollisionContext.of(player)).isEmpty(), "bent legs must not collide with their own stool");
      Player other = player(helper, GameType.SURVIVAL);
      helper.assertTrue(!helper.getBlockState(STOOL).getCollisionShape(helper.getLevel(), helper.absolutePos(STOOL),
         CollisionContext.of(other)).isEmpty(), "stool must retain its normal collision for everyone else");
      for (int i = 0; i < 5; i++) {
         helper.useBlock(STOOL, player);
         helper.useBlock(STOOL, other);
      }
      helper.assertTrue(!other.isPassenger() && seat.getPassengers().size() == 1, "occupied stool must never steal or duplicate a passenger");
      helper.assertTrue(seats(helper).size() == 1, "repeated right clicks must not duplicate seat entities");
      player.stopRiding();
      helper.assertTrue(!player.isPassenger(), "native dismount must release the passenger");
      helper.assertTrue(helper.getLevel().noCollision(player, player.getBoundingBox()), "native dismount must find a clear standing location");
      seat.tick();
      helper.assertTrue(seat.isRemoved() && seats(helper).isEmpty(), "empty mount must be deleted on its next server tick");
      helper.useBlock(STOOL, other);
      seat(helper, other);
      other.stopRiding();
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void stool_sitting_alone_has_no_passive_food_health_or_effect_bonus(GameTestHelper helper) {
      prepare(helper);
      Player player = player(helper, GameType.SURVIVAL);
      helper.useBlock(STOOL, player);
      StoolSeatEntity seat = seat(helper, player);
      resetFood(player, 8, 2.0F);
      player.setHealth(10.0F);
      for (int i = 0; i < 650; i++) {
         player.tick();
         seat.tick();
      }
      helper.assertValueEqual(player.getFoodData().getFoodLevel(), 8, "sitting past 30 seconds grants no hunger");
      close(helper, player.getFoodData().getSaturationLevel(), 2.0F, "sitting grants no saturation");
      close(helper, player.getHealth(), 10.0F, "sitting grants no healing");
      close(helper, player.getFoodData().getExhaustionLevel(), 0.0F, "sitting does not alter exhaustion");
      helper.assertTrue(player.getActiveEffects().isEmpty(), "no rest, saturation or other passive effect is installed");
      player.stopRiding();
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void seated_native_food_consumption_adds_exactly_twenty_percent_saturation(GameTestHelper helper) {
      prepare(helper);
      for (Item foodItem : List.of(Items.BREAD, Items.COOKED_BEEF, EngineerToolsModule.MUSLI_BAR.get())) {
         for (InteractionHand hand : InteractionHand.values()) {
            Player player = seated(helper, GameType.SURVIVAL);
            resetFood(player, 8, 0.0F);
            ItemStack food = new ItemStack(foodItem, 2);
            FoodProperties properties = food.getFoodProperties(player);
            consume(helper, player, food, hand);
            helper.assertValueEqual(player.getFoodData().getFoodLevel(), 8 + properties.nutrition(), "native nutrition must be granted exactly once");
            close(helper, player.getFoodData().getSaturationLevel(), properties.saturation() * 1.20F,
               "meal bonus is twenty percent of this food's normal saturation");
            helper.assertValueEqual(player.getItemInHand(hand).getCount(), 1, "native food consumption must remove exactly one item");
            helper.assertTrue(player.getActiveEffects().isEmpty(), "ordinary food must not acquire a saturation/rest effect");
            player.stopRiding();
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_meal_finish_is_not_awarded_twice_and_successive_meals_both_count(GameTestHelper helper) {
      prepare(helper);
      Player player = seated(helper, GameType.SURVIVAL);
      AtomicInteger completions = new AtomicInteger();
      Consumer<LivingEntityUseItemEvent.Finish> duplicateListener = event -> {
         if (event.getEntity() == player) {
            completions.incrementAndGet();
            // Deliberately repeat our handler during the actual native event, never synthesize a meal.
            StoolMealEvents.onUseFinish(event);
         }
      };
      NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingEntityUseItemEvent.Finish.class, duplicateListener);
      try {
         resetFood(player, 1, 0.0F);
         consume(helper, player, new ItemStack(Items.APPLE, 2), InteractionHand.MAIN_HAND);
         close(helper, player.getFoodData().getSaturationLevel(), 2.88F, "duplicate handler must not duplicate the apple's bonus");
         // Both complete during this same world tick. A time-based cooldown would wrongly suppress meal two.
         consume(helper, player, player.getMainHandItem(), InteractionHand.MAIN_HAND);
         close(helper, player.getFoodData().getSaturationLevel(), 5.76F, "independent meals must each receive their bonus");
         helper.assertValueEqual(completions.get(), 2, "native food path emits one completion per meal");
         for (int i = 0; i < 40; i++) {
            player.tick();
         }
         close(helper, player.getFoodData().getSaturationLevel(), 5.76F, "later ticks do not repeat meal bonuses");
         helper.assertTrue(player.getMainHandItem().isEmpty(), "two native meals consume exactly two apples");
      } finally {
         NeoForge.EVENT_BUS.unregister(duplicateListener);
         player.stopRiding();
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void stool_bonus_is_capped_at_current_hunger_and_creative_uses_native_rules(GameTestHelper helper) {
      prepare(helper);
      Player player = seated(helper, GameType.SURVIVAL);
      resetFood(player, 19, 19.0F);
      consume(helper, player, new ItemStack(Items.BREAD), InteractionHand.MAIN_HAND);
      helper.assertValueEqual(player.getFoodData().getFoodLevel(), 20, "vanilla caps hunger at twenty");
      close(helper, player.getFoodData().getSaturationLevel(), 20.0F, "bonus must never exceed current hunger");
      player.stopRiding();
      Player creative = seated(helper, GameType.CREATIVE);
      resetFood(creative, 8, 0.0F);
      consume(helper, creative, new ItemStack(Items.BREAD, 2), InteractionHand.MAIN_HAND);
      close(helper, creative.getFoodData().getSaturationLevel(), 7.2F, "a completed creative meal has the same saturation calculation");
      helper.assertValueEqual(creative.getMainHandItem().getCount(), 2, "bonus must preserve native creative item retention");
      creative.stopRiding();
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void stool_preserves_native_last_item_containers_and_food_effects(GameTestHelper helper) {
      prepare(helper);
      Player player = seated(helper, GameType.SURVIVAL);
      resetFood(player, 8, 0.0F);
      consume(helper, player, new ItemStack(Items.MUSHROOM_STEW), InteractionHand.MAIN_HAND);
      helper.assertTrue(player.getMainHandItem().is(Items.BOWL) && player.getMainHandItem().getCount() == 1,
         "last stew must leave exactly its native bowl");
      close(helper, player.getFoodData().getSaturationLevel(), 8.64F, "container foods keep the correct food saturation");
      AtomicInteger effects = new AtomicInteger();
      Consumer<MobEffectEvent.Added> listener = event -> {
         if (event.getEntity() == player) {
            effects.incrementAndGet();
         }
      };
      NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, MobEffectEvent.Added.class, listener);
      try {
         resetFood(player, 8, 0.0F);
         consume(helper, player, new ItemStack(Items.GOLDEN_APPLE), InteractionHand.OFF_HAND);
         helper.assertValueEqual(effects.get(), 2, "golden apple's two native effects must each be applied only once");
         helper.assertTrue(player.getOffhandItem().isEmpty(), "last golden apple must be consumed once");
         close(helper, player.getFoodData().getSaturationLevel(), 11.52F, "golden apple receives only its twenty-percent saturation addition");
      } finally {
         NeoForge.EVENT_BUS.unregister(listener);
         player.stopRiding();
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void standing_and_other_vehicles_receive_only_native_food_saturation(GameTestHelper helper) {
      prepare(helper);
      Player standing = player(helper, GameType.SURVIVAL);
      resetFood(standing, 8, 0.0F);
      consume(helper, standing, new ItemStack(Items.BREAD), InteractionHand.MAIN_HAND);
      close(helper, standing.getFoodData().getSaturationLevel(), 6.0F, "standing player gets no stool bonus");
      Minecart cart = EntityType.MINECART.create(helper.getLevel());
      helper.assertTrue(cart != null, "native minecart must exist");
      cart.setPos(standing.getX(), standing.getY(), standing.getZ());
      helper.getLevel().addFreshEntity(cart);
      helper.assertTrue(standing.startRiding(cart), "negative control must actually ride a native minecart");
      resetFood(standing, 8, 0.0F);
      consume(helper, standing, new ItemStack(Items.BREAD), InteractionHand.MAIN_HAND);
      close(helper, standing.getFoodData().getSaturationLevel(), 6.0F, "other mounts get no stool bonus");
      standing.stopRiding();
      cart.discard();
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void only_the_stool_at_actual_food_completion_qualifies(GameTestHelper helper) {
      prepare(helper);
      Player leaves = seated(helper, GameType.SURVIVAL);
      resetFood(leaves, 8, 0.0F);
      beginUse(helper, leaves, new ItemStack(Items.BREAD), InteractionHand.MAIN_HAND);
      leaves.stopRiding();
      finishUse(helper, leaves);
      close(helper, leaves.getFoodData().getSaturationLevel(), 6.0F, "dismount before completion loses the bonus");
      Player arrives = player(helper, GameType.SURVIVAL);
      resetFood(arrives, 8, 0.0F);
      beginUse(helper, arrives, new ItemStack(Items.BREAD), InteractionHand.MAIN_HAND);
      helper.useBlock(STOOL, arrives);
      seat(helper, arrives);
      finishUse(helper, arrives);
      close(helper, arrives.getFoodData().getSaturationLevel(), 7.2F, "sitting by actual completion qualifies without a rest delay");
      arrives.stopRiding();
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void canceled_interrupted_and_nonfood_uses_never_grant_a_meal_bonus(GameTestHelper helper) {
      prepare(helper);
      Player player = seated(helper, GameType.SURVIVAL);
      resetFood(player, 8, 0.0F);
      Consumer<LivingEntityUseItemEvent.Start> cancel = event -> {
         if (event.getEntity() == player) {
            event.setCanceled(true);
         }
      };
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, LivingEntityUseItemEvent.Start.class, cancel);
      try {
         player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD));
         player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
         helper.assertTrue(!player.isUsingItem(), "cancelled native Start must not eat");
      } finally {
         NeoForge.EVENT_BUS.unregister(cancel);
      }
      close(helper, player.getFoodData().getSaturationLevel(), 0.0F, "cancelled use grants nothing");
      beginUse(helper, player, new ItemStack(Items.BREAD), InteractionHand.MAIN_HAND);
      player.tick();
      player.releaseUsingItem();
      helper.assertTrue(!player.isUsingItem() && player.getMainHandItem().getCount() == 1, "interrupted use keeps its food");
      close(helper, player.getFoodData().getSaturationLevel(), 0.0F, "interrupted use grants nothing");
      consume(helper, player, new ItemStack(Items.MILK_BUCKET), InteractionHand.MAIN_HAND);
      helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "nonfood milk follows native container handling");
      helper.assertValueEqual(player.getFoodData().getFoodLevel(), 8, "nonfood use cannot create hunger");
      close(helper, player.getFoodData().getSaturationLevel(), 0.0F, "nonfood completion grants no saturation");
      consume(helper, player, new ItemStack(Items.BREAD), InteractionHand.MAIN_HAND);
      close(helper, player.getFoodData().getSaturationLevel(), 7.2F, "next real meal still works after interrupted and nonfood uses");
      player.stopRiding();
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void stool_mount_respects_permissions_headroom_and_native_mount_veto(GameTestHelper helper) {
      prepare(helper);
      for (GameType mode : List.of(GameType.ADVENTURE, GameType.SPECTATOR)) {
         Player denied = player(helper, mode);
         helper.useBlock(STOOL, denied);
         helper.assertTrue(!denied.isPassenger() && seats(helper).isEmpty(), "restricted players must not create seats");
      }
      Player player = player(helper, GameType.SURVIVAL);
      player.setPos(player.getX() + 20.0, player.getY(), player.getZ());
      helper.useBlock(STOOL, player);
      helper.assertTrue(!player.isPassenger(), "out-of-reach use must not mount");
      player = player(helper, GameType.SURVIVAL);
      helper.setBlock(STOOL.above(), Blocks.STONE);
      helper.useBlock(STOOL, player);
      helper.assertTrue(!player.isPassenger() && seats(helper).isEmpty(), "solid headroom must prevent seating");
      helper.setBlock(STOOL.above(), Blocks.AIR);
      player.setPose(Pose.SWIMMING);
      helper.useBlock(STOOL, player);
      helper.assertTrue(!player.isPassenger() && seats(helper).isEmpty(), "short crawling/swimming poses must not bypass standing headroom checks");
      player.setPose(Pose.STANDING);
      final Player vetoed = player;
      Consumer<EntityMountEvent> cancel = event -> {
         if (event.getEntityMounting() == vetoed && event.isMounting()) {
            event.setCanceled(true);
         }
      };
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, EntityMountEvent.class, cancel);
      try {
         helper.useBlock(STOOL, player);
         helper.assertTrue(!player.isPassenger() && seats(helper).isEmpty(), "mount cancellation must leave no orphan seat");
      } finally {
         NeoForge.EVENT_BUS.unregister(cancel);
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void stool_break_logout_and_reload_cannot_leave_persistent_seats(GameTestHelper helper) {
      prepare(helper);
      Player player = seated(helper, GameType.SURVIVAL);
      StoolSeatEntity seat = seat(helper, player);
      helper.assertTrue(!seat.save(new CompoundTag()) && !seat.saveAsPassenger(new CompoundTag()), "seat must never be written to chunk or player vehicle saves");
      helper.setBlock(STOOL.above(2), Blocks.STONE);
      helper.setBlock(STOOL, Blocks.AIR);
      helper.assertTrue(!player.isPassenger() && seat.isRemoved() && seats(helper).isEmpty(), "breaking stool immediately dismounts and deletes seat");
      helper.assertTrue(helper.getLevel().noCollision(player, player.getBoundingBox()), "breaking an occupied stool must place its passenger clear of blocks");
      prepare(helper);
      Player loggingOut = seated(helper, GameType.SURVIVAL);
      StoolSeatEntity logoutSeat = seat(helper, loggingOut);
      NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(loggingOut));
      helper.assertTrue(!loggingOut.isPassenger() && logoutSeat.isRemoved() && seats(helper).isEmpty(), "disconnect event cleans up the vehicle");
      StoolSeatEntity loaded = ModEntities.STOOL_SEAT.get().create(helper.getLevel());
      helper.assertTrue(loaded != null, "seat type exists");
      loaded.setPos(helper.absolutePos(STOOL).getX() + 0.5, helper.absolutePos(STOOL).getY() + StoolSeatEntity.SEAT_HEIGHT,
         helper.absolutePos(STOOL).getZ() + 0.5);
      CompoundTag saved = loaded.saveWithoutId(new CompoundTag());
      loaded.load(saved);
      helper.getLevel().addFreshEntity(loaded);
      loaded.tick();
      helper.assertTrue(loaded.isRemoved() && seats(helper).isEmpty(), "defensive NBT reload removes stale seats instead of restoring occupancy");
      helper.succeed();
   }

   private static void prepare(GameTestHelper helper) {
      for (StoolSeatEntity seat : seats(helper)) {
         seat.discard();
      }
      for (BlockPos pos : BlockPos.betweenClosed(STOOL.offset(-1, 0, -1), STOOL.offset(1, 2, 1))) {
         helper.setBlock(pos, Blocks.AIR);
      }
      for (BlockPos pos : BlockPos.betweenClosed(STOOL.offset(-1, -1, -1), STOOL.offset(1, -1, 1))) {
         helper.setBlock(pos, Blocks.STONE);
      }
      helper.setBlock(STOOL, ModBlocks.TREATED_WOOD_STOOL.get());
   }

   private static Player player(GameTestHelper helper, GameType mode) {
      Player player = helper.makeMockPlayer(mode);
      mode.updatePlayerAbilities(player.getAbilities());
      BlockPos absolute = helper.absolutePos(STOOL);
      player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 1.5);
      resetFood(player, 8, 0.0F);
      return player;
   }

   private static Player seated(GameTestHelper helper, GameType mode) {
      Player player = player(helper, mode);
      helper.useBlock(STOOL, player);
      seat(helper, player);
      return player;
   }

   private static StoolSeatEntity seat(GameTestHelper helper, Player player) {
      helper.assertTrue(player.getVehicle() instanceof StoolSeatEntity, "real native startRiding must mount the stool");
      StoolSeatEntity seat = (StoolSeatEntity)player.getVehicle();
      helper.assertTrue(seat.isValidSeatFor(player), "stool vehicle must be live, single-occupant and backed by its block");
      return seat;
   }

   private static List<StoolSeatEntity> seats(GameTestHelper helper) {
      return helper.getLevel().getEntitiesOfClass(StoolSeatEntity.class, new AABB(helper.absolutePos(STOOL)));
   }

   private static void resetFood(Player player, int hunger, float saturation) {
      player.getFoodData().setFoodLevel(hunger);
      player.getFoodData().setSaturation(saturation);
      player.getFoodData().setExhaustion(0.0F);
      player.setHealth(player.getMaxHealth());
   }

   private static void consume(GameTestHelper helper, Player player, ItemStack stack, InteractionHand hand) {
      beginUse(helper, player, stack, hand);
      finishUse(helper, player);
   }

   private static void beginUse(GameTestHelper helper, Player player, ItemStack stack, InteractionHand hand) {
      player.setItemInHand(hand, stack);
      stack.use(helper.getLevel(), player, hand);
      helper.assertTrue(player.isUsingItem(), "test must start actual native held-item use");
   }

   private static void finishUse(GameTestHelper helper, Player player) {
      // Tick the real LivingEntity countdown; direct finishUsingItem would bypass NeoForge's Finish hook.
      for (int i = 0; i < 80 && player.isUsingItem(); i++) {
         player.tick();
      }
      helper.assertTrue(!player.isUsingItem(), "native countdown must complete the use");
   }

   private static void close(GameTestHelper helper, float actual, float expected, String message) {
      helper.assertTrue(Math.abs(actual - expected) < 0.001F, message + ": expected " + expected + ", got " + actual);
   }
}
