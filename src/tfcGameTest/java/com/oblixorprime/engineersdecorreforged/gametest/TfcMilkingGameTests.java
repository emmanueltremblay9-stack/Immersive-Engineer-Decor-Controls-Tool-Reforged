package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlocks;
import com.oblixorprime.engineersdecorreforged.utility.MilkingState;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.dries007.tfc.common.entities.livestock.DairyAnimal;
import net.dries007.tfc.common.entities.livestock.Gender;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.config.ConfigBuilder;
import net.dries007.tfc.util.events.AnimalProductEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Compiled/registered only by the real TFC dependency profile, never stub cows. */
@GameTestHolder(EngineersDecorReforged.MOD_ID + "_tfc")
@PrefixGameTestTemplate(false)
public final class TfcMilkingGameTests {
   private static final String TEMPLATE = "empty";
   private static final BlockPos POS = new BlockPos(3, 2, 3);

   @GameTest(template = TEMPLATE, batch = "tfc_milking")
   public static void ready_native_cow_fills_tank_and_cooldown_prevents_replay(GameTestHelper h) {
      try (Fixture f = fixture(h)) {
         tick(f, 120);
         h.assertValueEqual(amount(f), 1000, "real native cow fills tank without a bucket");
         h.assertTrue(tank(f).getFluidInTank(0).is(NeoForgeMod.MILK.get()), "native milk identity");
         h.assertValueEqual(f.cow.getUses(), 1, "one native wear update");
         h.assertTrue(f.cow.getProductsCooldown() > 0, "native calendar cooldown");
         tick(f, 240);
         h.assertValueEqual(amount(f), 1000, "no replay during native cooldown");
         h.assertValueEqual(f.cow.getUses(), 1, "cooldown does not consume uses");
      }
      h.succeed();
   }

   @GameTest(template = TEMPLATE, batch = "tfc_milking")
   public static void native_age_sex_familiarity_and_cooldown_rejections(GameTestHelper h) {
      List<Consumer<DairyAnimal>> rejected = List.of(c -> c.setGender(Gender.MALE),
            DairyAnimal::setBirthTickNow, c -> c.setOldTick(-2),
            c -> c.setFamiliarity(0), DairyAnimal::setProductsCooldown);
      for (Consumer<DairyAnimal> change : rejected) try (Fixture f = fixture(h)) {
         change.accept(f.cow);
         h.assertFalse(f.cow.isReadyForAnimalProduct(), "actual TFC rejects the fixture");
         long produced = f.cow.getProducedTick();
         tick(f, 120);
         h.assertValueEqual(amount(f), 0, "ineligible animal produces nothing");
         h.assertValueEqual(f.cow.getUses(), 0, "no wear on rejection");
         h.assertValueEqual(f.cow.getProducedTick(), produced, "no cooldown update on rejection");
      }
      h.succeed();
   }

   @GameTest(template = TEMPLATE, batch = "tfc_milking")
   public static void familiarity_threshold_is_strict(GameTestHelper h) {
      var supplier = TFCConfig.SERVER.cowConfig.produceFamiliarity();
      h.assertTrue(supplier instanceof ConfigBuilder.ServerValue<?>, "native server configuration wrapper");
      var configured = ((ConfigBuilder.ServerValue<?>)supplier).value();
      h.assertTrue(configured instanceof ModConfigSpec.DoubleValue, "native configured threshold");
      var threshold = (ModConfigSpec.DoubleValue)configured;
      double previous = threshold.get();
      try (Fixture f = fixture(h)) {
         threshold.set(.5); // exactly representable by both the native float and double
         f.cow.setFamiliarity(.5f);
         tick(f, 120);
         h.assertValueEqual(amount(f), 0, "equality is not sufficient");
         f.cow.setFamiliarity(Math.nextUp(.5f));
         tick(f, 120);
         h.assertValueEqual(amount(f), 1000, "strictly above the same threshold succeeds");
      } finally {
         threshold.set(previous);
      }
      h.succeed();
   }

   @GameTest(template = TEMPLATE, batch = "tfc_milking")
   public static void protection_and_product_cancellation_do_not_consume(GameTestHelper h) {
      for (boolean protection : new boolean[]{true, false}) try (Fixture f = fixture(h)) {
         AtomicInteger interactions = new AtomicInteger(), products = new AtomicInteger();
         try (var first = listen(PlayerInteractEvent.EntityInteract.class, event -> {
            if (event.getTarget() != f.cow) return;
            interactions.incrementAndGet();
            h.assertTrue(event.getEntity() instanceof FakePlayer, "machine interaction actor");
            h.assertFalse(event.getEntity().getAbilities().instabuild, "noncreative actor");
            if (protection) event.setCanceled(true);
         }); var second = listen(AnimalProductEvent.class, event -> {
            if (event.getEntity() != f.cow) return;
            products.incrementAndGet();
            event.setCanceled(true);
         })) {
            tick(f, 120);
         }
         h.assertValueEqual(interactions.get(), 1, "one protection hook per attempt");
         h.assertValueEqual(products.get(), protection ? 0 : 1, "no product after protection cancellation");
         h.assertValueEqual(amount(f), 0, "cancellation leaves storage empty");
         h.assertValueEqual(f.cow.getUses(), 0, "cancellation leaves wear unchanged");
         h.assertTrue(f.cow.isReadyForAnimalProduct(), "cancellation leaves native readiness");
      }
      h.succeed();
   }

   @GameTest(template = TEMPLATE, batch = "tfc_milking")
   public static void modified_amount_and_uses_are_exact_without_partial_acceptance(GameTestHelper h) {
      for (int product : new int[]{500, 1500, 4000, 4001, Integer.MAX_VALUE, 0}) try (Fixture f = fixture(h)) {
         try (var listener = product(f, event -> {
            event.setProduct(new FluidStack(NeoForgeMod.MILK.get(), product));
            event.setUses(3);
         })) { tick(f, 120); }
         boolean accepted = product > 0 && product <= 4000;
         h.assertValueEqual(amount(f), accepted ? product : 0, "whole actual product, never a partial loss");
         h.assertValueEqual(f.cow.getUses(), accepted ? 3 : 0, "actual event uses only after acceptance");
         h.assertValueEqual(f.cow.isReadyForAnimalProduct(), !accepted, "native cooldown only for accepted product");
      }
      try (Fixture f = fixture(h)) {
         tank(f).fill(new FluidStack(NeoForgeMod.MILK.get(), 3500), FluidAction.EXECUTE);
         tick(f, 120);
         h.assertValueEqual(amount(f), 3500, "insufficient space does not accept half a native product");
         h.assertTrue(f.cow.isReadyForAnimalProduct(), "insufficient space does not consume the source");
         try (var listener = product(f, event -> event.setProduct(new FluidStack(NeoForgeMod.MILK.get(), 500)))) {
            tick(f, 120);
         }
         h.assertValueEqual(amount(f), 4000, "event amount fits even when less than a bucket is available");
      }
      h.succeed();
   }

   @GameTest(template = TEMPLATE, batch = "tfc_milking")
   public static void incompatible_fluid_components_or_item_product_are_refused(GameTestHelper h) {
      List<Consumer<AnimalProductEvent>> modifications = List.of(
            event -> event.setProduct(new FluidStack(Fluids.WATER, 1000)),
            event -> {
               FluidStack named = new FluidStack(NeoForgeMod.MILK.get(), 1000);
               named.set(DataComponents.CUSTOM_NAME, Component.literal("component-bearing milk"));
               event.setProduct(named);
            }, event -> event.setProduct(new ItemStack(Items.DIAMOND)));
      for (var modification : modifications) try (Fixture f = fixture(h)) {
         f.machine.setItem(0, new ItemStack(Items.BUCKET));
         try (var listener = product(f, modification)) { tick(f, 120); }
         h.assertValueEqual(amount(f), 0, "unrepresentable product never becomes standard milk");
         h.assertTrue(f.machine.getItem(0).is(Items.BUCKET), "real input is not consumed");
         h.assertTrue(f.machine.getItem(1).isEmpty(), "no synthesized output");
         h.assertValueEqual(f.cow.getUses(), 0, "no animal wear");
         h.assertTrue(f.cow.isReadyForAnimalProduct(), "no source consumption");
      }
      h.succeed();
   }

   @GameTest(template = TEMPLATE, batch = "tfc_milking")
   public static void callbacks_revalidate_capacity_power_and_animal_position(GameTestHelper h) {
      for (int mode = 0; mode < 4; mode++) try (Fixture f = fixture(h)) {
         final int selected = mode;
         try (var listener = product(f, event -> {
            if (selected == 0) tank(f).fill(new FluidStack(NeoForgeMod.MILK.get(), 4000), FluidAction.EXECUTE);
            if (selected == 1) h.getLevel().setBlock(f.pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
            if (selected == 2) f.cow.setPos(f.pos.getX() + 10, f.pos.getY(), f.pos.getZ());
            if (selected == 3) f.cow.setGender(Gender.MALE);
         })) { tick(f, 120); }
         h.assertValueEqual(amount(f), mode == 0 ? 4000 : 0, "callback changes cannot be overwritten by stale acceptance");
         h.assertValueEqual(f.cow.getUses(), 0, "callback rejection has no source wear");
         h.assertTrue(f.cow.getProductsCooldown() == 0, "callback rejection has no cooldown update");
         h.getLevel().setBlock(f.pos.east(), Blocks.AIR.defaultBlockState(), 3);
      }
      h.succeed();
   }

   @GameTest(template = TEMPLATE, batch = "tfc_milking")
   public static void reentrant_two_machine_attempt_is_exactly_once(GameTestHelper h) {
      try (Fixture f = fixture(h)) {
         MachineBlockEntity other = place(h, POS.east());
         AtomicInteger events = new AtomicInteger();
         try (var listener = product(f, event -> {
            events.incrementAndGet();
            tick(h, other, 120);
            tick(f, 120);
         })) { tick(f, 120); }
         h.assertValueEqual(events.get(), 1, "same-cow callback cannot produce a second native event");
         h.assertValueEqual(amount(f), 1000, "one complete product in the first machine");
         h.assertValueEqual(tank(h, other).getFluidInTank(0).getAmount(), 0, "second machine cannot share one source product");
         h.assertValueEqual(f.cow.getUses(), 1, "native source committed exactly once");
      }
      h.succeed();
   }

   @GameTest(template = TEMPLATE, batch = "tfc_milking")
   public static void bucket_priority_and_full_tank_keep_products_conserved(GameTestHelper h) {
      for (int initial : new int[]{0, 4000}) try (Fixture f = fixture(h)) {
         if (initial > 0) tank(f).fill(new FluidStack(NeoForgeMod.MILK.get(), initial), FluidAction.EXECUTE);
         f.machine.setItem(0, new ItemStack(Items.BUCKET));
         tick(f, 120);
         h.assertTrue(f.machine.getItem(1).is(Items.MILK_BUCKET), "real bucket output");
         h.assertTrue(f.machine.getItem(0).isEmpty(), "one paid input bucket");
         h.assertValueEqual(amount(f), initial, "tank plus output preserves initial fluid and one native product");
         h.assertValueEqual(f.cow.getUses(), 1, "one source commit");
      }
      h.succeed();
   }

   @GameTest(template = TEMPLATE, batch = "tfc_milking", timeoutTicks = 100)
   public static void save_reload_preserves_tank_and_native_cooldown(GameTestHelper h) {
      Fixture f = fixture(h);
      tick(f, 120);
      CompoundTag animal = new CompoundTag();
      f.cow.saveWithoutId(animal);
      CompoundTag machine = f.machine.saveWithFullMetadata(h.getLevel().registryAccess());
      f.cow.discard();
      var restored = (MachineBlockEntity)BlockEntity.loadStatic(f.pos, f.machine.getBlockState(), machine, h.getLevel().registryAccess());
      h.getLevel().removeBlockEntity(f.pos);
      h.getLevel().setBlockEntity(restored);
      h.runAfterDelay(2, () -> {
         DairyAnimal loadedCow = newCow(h);
         loadedCow.load(animal);
         h.assertTrue(h.getLevel().addFreshEntity(loadedCow), "reload the actual saved native entity");
         try {
            tick(h, restored, 120);
            h.assertValueEqual(tank(h, restored).getFluidInTank(0).getAmount(), 1000, "legacy tank NBT survives without replay");
            h.assertValueEqual(loadedCow.getUses(), 1, "native wear survives reload");
            h.assertTrue(loadedCow.getProductsCooldown() > 0, "native calendar cooldown survives reload");
         } finally { loadedCow.discard(); }
         h.succeed();
      });
   }

   @GameTest(template = TEMPLATE, batch = "tfc_milking")
   public static void redstone_range_and_vanilla_behavior_remain_unchanged(GameTestHelper h) {
      try (Fixture f = fixture(h)) {
         h.getLevel().setBlock(f.pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
         tick(f, 120);
         h.assertValueEqual(amount(f), 0, "powered machine does not milk");
         h.getLevel().setBlock(f.pos.east(), Blocks.AIR.defaultBlockState(), 3);
         f.cow.setPos(f.pos.getX() + 10, f.pos.getY(), f.pos.getZ());
         tick(f, 120);
         h.assertValueEqual(amount(f), 0, "outside existing range does not milk");
         var vanilla = EntityType.COW.create(h.getLevel());
         vanilla.setNoAi(true); vanilla.setNoGravity(true);
         vanilla.setPos(f.pos.getX() + .5, f.pos.getY(), f.pos.getZ() - .5);
         h.getLevel().addFreshEntity(vanilla);
         try {
            tick(f, 120);
            h.assertValueEqual(amount(f), 1000, "vanilla path remains independent of TFC readiness");
         } finally { vanilla.discard(); }
      }
      h.succeed();
   }

   @GameTest(template = TEMPLATE, batch = "tfc_milking")
   public static void foreign_actor_outputs_are_preserved_without_a_free_bucket(GameTestHelper h) {
      try (Fixture f = fixture(h)) {
         try (var listener = product(f, event -> {
            h.assertTrue(event.getTool().isEmpty() && event.getPlayer().getMainHandItem().isEmpty(), "no fabricated vessel");
            event.getPlayer().getInventory().add(new ItemStack(Items.DIAMOND));
         })) { tick(f, 120); }
         var dropped = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(f.pos).inflate(4));
         h.assertValueEqual(dropped.stream().filter(e -> e.getItem().is(Items.DIAMOND)).mapToInt(e -> e.getItem().getCount()).sum(), 1, "actual hook output retained once");
         h.assertTrue(dropped.stream().noneMatch(e -> e.getItem().is(Items.BUCKET) || e.getItem().is(Items.MILK_BUCKET)), "no free bucket appears");
         h.assertValueEqual(amount(f), 1000, "fluid product remains exact");
      }
      h.succeed();
   }

   @GameTest(template = TEMPLATE, batch = "tfc_milking", timeoutTicks = 100)
   public static void native_expiry_extracts_without_waiting_for_machine_cycle(GameTestHelper h) {
      Fixture f = fixture(h);
      int duration = TFCConfig.SERVER.cowConfig.produceTicks().get();
      f.cow.setProducedTick(f.cow.calendar().getTicks() - duration + 1);
      tick(f, 7); // Explicitly not the old 120-tick extraction boundary.
      h.assertValueEqual(f.cow.getProductsCooldown(), 1L, "one native tick remains");
      h.assertValueEqual(amount(f), 0, "never extract before native expiry");
      h.assertValueEqual(projection(f.machine).state(), MilkingState.RECOVERING, "native recovery projected");
      h.runAfterDelay(1, () -> {
         try (f) {
            tick(f, 1);
            h.assertValueEqual(amount(f), 1000, "extract on the first native-ready tick");
            h.assertValueEqual(f.cow.getUses(), 1, "exactly one source commit at expiry");
         }
         h.succeed();
      });
   }

   @GameTest(template = TEMPLATE, batch = "tfc_milking")
   public static void external_native_milking_resets_observation_and_clears_refusal_retry(GameTestHelper h) {
      try (Fixture f = fixture(h)) {
         try (var listener = product(f, event -> event.setCanceled(true))) { tick(f, 1); }
         h.assertTrue((projection(f.machine).machineFlags() & MilkingState.RETRY_WAIT) != 0, "refusal is not animal recovery");
         f.cow.setProductsCooldown(); // Same authoritative state transition as manual/native milking.
         tick(f, 1);
         h.assertValueEqual(projection(f.machine).remainingTicks(), f.cow.getProductsCooldown(), "observe external cooldown immediately");
         h.assertValueEqual(projection(f.machine).machineFlags() & MilkingState.RETRY_WAIT, 0, "recovery clears refusal delay");
         f.cow.setProducedTick(-1_000_000_000L);
         tick(f, 1);
         h.assertValueEqual(amount(f), 1000, "native readiness wins over old machine refusal timer");
      }
      h.succeed();
   }

   @GameTest(template = TEMPLATE, batch = "tfc_milking")
   public static void two_cows_choose_next_recovery_then_ready_with_stable_ties(GameTestHelper h) {
      try (Fixture f = fixture(h)) {
         DairyAnimal other = newCow(h);
         CompoundTag saved = new CompoundTag();
         f.cow.saveWithoutId(saved);
         other.load(saved);
         other.setUUID(java.util.UUID.randomUUID());
         h.assertTrue(h.getLevel().addFreshEntity(other), "second native cow");
         try {
            int duration = TFCConfig.SERVER.cowConfig.produceTicks().get();
            f.cow.setProductsCooldown();
            other.setProducedTick(other.calendar().getTicks() - duration + 7);
            tick(f, 1);
            h.assertValueEqual(projection(f.machine).animalId(), other.getUUID(), "earliest eligible recovery selected");
            other.setGender(Gender.MALE);
            tick(f, 1);
            h.assertValueEqual(projection(f.machine).animalId(), f.cow.getUUID(), "ineligible earlier cow cannot hide eligible recovery");
            other.setGender(Gender.FEMALE);
            other.setProducedTick(f.cow.getProducedTick());
            tick(f, 1);
            DairyAnimal expected = f.cow.getId() < other.getId() ? f.cow : other;
            h.assertValueEqual(projection(f.machine).animalId(), expected.getUUID(), "equal recovery uses stable entity-id tie break");
            other.setProducedTick(-1_000_000_000L);
            tick(f, 1);
            h.assertValueEqual(amount(f), 1000, "ready cow is milked without waiting for recovering cow");
            h.assertValueEqual(other.getUses(), 1, "only selected ready cow consumed");
            h.assertValueEqual(f.cow.getUses(), 0, "recovering neighbor untouched");
            h.assertValueEqual(projection(f.machine).animalId(), other.getUUID(), "post-transaction projection is the actual source");
         } finally { other.discard(); }
      }
      h.succeed();
   }

   @GameTest(template = TEMPLATE, batch = "tfc_milking")
   public static void target_departure_and_machine_reload_clear_projection(GameTestHelper h) {
      try (Fixture f = fixture(h)) {
         f.cow.setProductsCooldown();
         tick(f, 1);
         h.assertValueEqual(projection(f.machine).animalId(), f.cow.getUUID(), "observe native UUID");
         CompoundTag saved = f.machine.saveWithFullMetadata(h.getLevel().registryAccess());
         var restored = (MachineBlockEntity)BlockEntity.loadStatic(f.pos, f.machine.getBlockState(), saved, h.getLevel().registryAccess());
         h.assertValueEqual(projection(restored).source(), MilkingState.NONE, "observations are never persisted as authority");
         h.assertTrue(projection(restored).animalId() == null, "reload clears previous UUID");
         f.cow.setPos(f.pos.getX() + 10, f.pos.getY(), f.pos.getZ());
         tick(f, 1);
         h.assertValueEqual(projection(f.machine).source(), MilkingState.NONE, "departure clears source");
         h.assertTrue(projection(f.machine).animalId() == null, "departure clears UUID");
         h.assertValueEqual(projection(f.machine).remainingTicks(), 0L, "departure clears recovery");
      }
      h.succeed();
   }

   @GameTest(template = TEMPLATE, batch = "tfc_milking")
   public static void pause_and_full_storage_do_not_replace_native_recovery(GameTestHelper h) {
      try (Fixture f = fixture(h)) {
         f.cow.setProductsCooldown();
         tank(f).fill(new FluidStack(NeoForgeMod.MILK.get(), 4000), FluidAction.EXECUTE);
         h.getLevel().setBlock(f.pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
         tick(f, 1);
         MilkingState observed = projection(f.machine);
         h.assertValueEqual(observed.state(), MilkingState.RECOVERING, "recovery independent of machine blockers");
         h.assertValueEqual(observed.remainingTicks(), f.cow.getProductsCooldown(), "native time remains authoritative");
         h.assertValueEqual(observed.machineFlags(), MilkingState.PAUSED | MilkingState.STORAGE_BLOCKED, "both blockers coexist with recovery");
         f.cow.setProducedTick(-1_000_000_000L);
         tick(f, 1);
         h.assertValueEqual(projection(f.machine).state(), MilkingState.READY, "native ready state visible even while blocked");
         h.assertValueEqual(f.cow.getUses(), 0, "blocked machine never consumes source");
         h.getLevel().setBlock(f.pos.east(), Blocks.AIR.defaultBlockState(), 3);
         tank(f).drain(4000, FluidAction.EXECUTE);
         tick(f, 1);
         h.assertValueEqual(amount(f), 1000, "unblocking does not restart an extraction countdown");
      } finally { h.getLevel().setBlock(h.absolutePos(POS).east(), Blocks.AIR.defaultBlockState(), 3); }
      h.succeed();
   }

   @GameTest(template = TEMPLATE, batch = "tfc_milking")
   public static void changed_native_configuration_and_large_recovery_are_projected(GameTestHelper h) {
      var supplier = TFCConfig.SERVER.cowConfig.produceTicks();
      var configured = ((ConfigBuilder.ServerValue<?>)supplier).value();
      h.assertTrue(configured instanceof ModConfigSpec.IntValue, "native duration configuration");
      var duration = (ModConfigSpec.IntValue)configured;
      int previous = duration.get();
      try (Fixture f = fixture(h)) {
         duration.set(100_001);
         f.cow.setProductsCooldown();
         tick(f, 1);
         h.assertValueEqual(projection(f.machine).totalTicks(), 100_001, "configured duration, not a built-in constant");
         h.assertValueEqual(projection(f.machine).remainingTicks(), f.cow.getProductsCooldown(), "remaining survives signed-short synchronization");
         h.assertTrue(projection(f.machine).remainingTicks() > 32767, "large duration fixture");
         duration.set(200_003);
         tick(f, 1);
         h.assertValueEqual(projection(f.machine).totalTicks(), 200_003, "configuration changes observed immediately");
         h.assertValueEqual(projection(f.machine).remainingTicks(), f.cow.getProductsCooldown(), "native recovery recomputed after configuration change");
         h.assertValueEqual(amount(f), 0, "configuration observation never produces milk");
      } finally { duration.set(previous); }
      h.succeed();
   }

   @GameTest(template = TEMPLATE, batch = "tfc_milking")
   public static void refused_ready_cow_does_not_starve_other_ready_cow(GameTestHelper h) {
      try (Fixture f = fixture(h)) {
         DairyAnimal other = copyCow(h, f.cow);
         AtomicInteger firstEvents = new AtomicInteger(), otherEvents = new AtomicInteger();
         try (var listener = listen(AnimalProductEvent.class, event -> {
            if (event.getEntity() == f.cow) {
               firstEvents.incrementAndGet();
               event.setCanceled(true);
            } else if (event.getEntity() == other) otherEvents.incrementAndGet();
         })) {
            h.assertTrue(f.cow.getId() < other.getId(), "first ready cow has the lower entity ID");
            h.assertTrue(other.isReadyForAnimalProduct(), "second cow is natively ready");
            tick(f, 1);
            h.assertValueEqual(firstEvents.get(), 1, "first tick attempts lower-ID cow");
            h.assertValueEqual(otherEvents.get(), 0, "no second transaction in the same machine tick");
            h.assertValueEqual(amount(f), 0, "canceled transaction stores nothing");
            h.assertValueEqual(projection(f.machine).animalId(), f.cow.getUUID(), "refusal projection identifies attempted cow");
            h.assertTrue((projection(f.machine).machineFlags() & MilkingState.RETRY_WAIT) != 0, "first cow has refusal backoff");
            tick(f, 1);
            h.assertValueEqual(firstEvents.get(), 1, "deferred cow is not retried next tick");
            h.assertValueEqual(otherEvents.get(), 1, "other ready cow attempted immediately next tick");
            h.assertValueEqual(amount(f), 1000, "other cow supplies one complete product");
            h.assertValueEqual(f.cow.getUses(), 0, "refused source remains untouched");
            h.assertValueEqual(other.getUses(), 1, "only successful source is consumed");
            h.assertValueEqual(projection(f.machine).animalId(), other.getUUID(), "success projection identifies actual attempted source");
            h.assertValueEqual(projection(f.machine).state(), MilkingState.RECOVERING, "successful source recovery is projected");
            h.assertValueEqual(projection(f.machine).machineFlags() & MilkingState.RETRY_WAIT, 0, "other source does not inherit refusal flag");
         } finally { other.discard(); }
      }
      h.succeed();
   }

   @GameTest(template = TEMPLATE, batch = "tfc_milking")
   public static void departed_refused_cow_cannot_delay_new_ready_cow(GameTestHelper h) {
      try (Fixture f = fixture(h)) {
         AtomicInteger refused = new AtomicInteger();
         try (var listener = product(f, event -> {
            refused.incrementAndGet();
            event.setCanceled(true);
         })) { tick(f, 1); }
         h.assertValueEqual(refused.get(), 1, "one canceled attempt before departure");
         h.assertValueEqual(projection(f.machine).animalId(), f.cow.getUUID(), "old UUID is initially observed");
         h.assertTrue((projection(f.machine).machineFlags() & MilkingState.RETRY_WAIT) != 0, "old source has active refusal backoff");
         f.cow.discard();
         DairyAnimal replacement = copyCow(h, f.cow);
         AtomicInteger accepted = new AtomicInteger();
         try (var listener = listen(AnimalProductEvent.class, event -> {
            if (event.getEntity() == replacement) accepted.incrementAndGet();
         })) {
            h.assertTrue(!replacement.getUUID().equals(f.cow.getUUID()), "replacement has a distinct source UUID");
            h.assertTrue(replacement.isReadyForAnimalProduct(), "replacement is natively ready");
            tick(f, 1); // No empty tick between departure and replacement.
            h.assertValueEqual(accepted.get(), 1, "new source is attempted on the very next tick");
            h.assertValueEqual(amount(f), 1000, "departed source refusal does not delay replacement");
            h.assertValueEqual(replacement.getUses(), 1, "exactly one replacement product committed");
            h.assertValueEqual(projection(f.machine).animalId(), replacement.getUUID(), "projection replaces departed UUID");
            h.assertValueEqual(projection(f.machine).machineFlags() & MilkingState.RETRY_WAIT, 0, "replacement never inherits old refusal flag");
         } finally { replacement.discard(); }
      }
      h.succeed();
   }

   @GameTest(template = TEMPLATE, batch = "tfc_milking")
   public static void ready_herd_larger_than_refusal_cache_cannot_starve_last_cow(GameTestHelper h) {
      try (Fixture f = fixture(h)) {
         var herd = new java.util.ArrayList<DairyAnimal>();
         herd.add(f.cow);
         try {
            for (int i = 1; i < 66; i++) herd.add(copyCow(h, f.cow));
            herd.sort(java.util.Comparator.comparingInt(DairyAnimal::getId));
            DairyAnimal permitted = herd.getLast();
            var attempts = new java.util.HashMap<java.util.UUID, Integer>();
            AtomicInteger events = new AtomicInteger();
            try (var listener = listen(AnimalProductEvent.class, event -> {
               if (!(event.getEntity() instanceof DairyAnimal cow) || !herd.contains(cow)) return;
               events.incrementAndGet();
               attempts.merge(cow.getUUID(), 1, Integer::sum);
               if (cow != permitted) event.setCanceled(true);
            })) {
               for (int i = 0; i < 66; i++) {
                  int before = events.get();
                  tick(f, 1);
                  h.assertValueEqual(events.get() - before, 1, "exactly one native attempt per machine tick");
                  h.assertValueEqual(projection(f.machine).animalId(), herd.get(i).getUUID(), "rotation reaches each ready source in order");
                  h.assertValueEqual(amount(f), i == 65 ? 1000 : 0, "only last permitted cow produces");
               }
            }
            h.assertValueEqual(events.get(), 66, "no replay or skipped attempt across refusal cache boundary");
            h.assertValueEqual(attempts.size(), 66, "all distinct ready cows attempted before any repeat");
            for (DairyAnimal cow : herd) {
               h.assertValueEqual(attempts.get(cow.getUUID()), Integer.valueOf(1), "one attempt per source");
               h.assertValueEqual(cow.getUses(), cow == permitted ? 1 : 0, "only permitted source consumed");
            }
            h.assertValueEqual(projection(f.machine).animalId(), permitted.getUUID(), "final projection matches successful source");
            h.assertValueEqual(projection(f.machine).state(), MilkingState.RECOVERING, "final source shows native recovery");
            h.assertValueEqual(projection(f.machine).machineFlags() & MilkingState.RETRY_WAIT, 0, "successful source has no inherited refusal");
         } finally {
            for (DairyAnimal cow : herd) if (cow != f.cow) cow.discard();
         }
      }
      h.succeed();
   }

   private static DairyAnimal copyCow(GameTestHelper h, DairyAnimal source) {
      CompoundTag saved = new CompoundTag();
      source.saveWithoutId(saved);
      DairyAnimal copy = newCow(h);
      copy.load(saved);
      copy.setUUID(java.util.UUID.randomUUID());
      h.assertTrue(h.getLevel().addFreshEntity(copy), "new native cow enters the test world");
      return copy;
   }

   private static MilkingState projection(MachineBlockEntity machine) {
      var data = machine.milkingDataAccessForTests();
      return MilkingState.decode(index -> (short)data.get(index));
   }

   private record Fixture(GameTestHelper helper, MachineBlockEntity machine, DairyAnimal cow, BlockPos pos) implements AutoCloseable {
      public void close() { cow.discard(); helper.getLevel().removeBlock(pos, false); }
   }
   private record Listener(Object callback) implements AutoCloseable {
      public void close() { NeoForge.EVENT_BUS.unregister(callback); }
   }
   private static <E extends Event> Listener listen(Class<E> type, Consumer<E> callback) {
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true, type, callback);
      return new Listener(callback);
   }
   private static Listener product(Fixture f, Consumer<AnimalProductEvent> callback) {
      return listen(AnimalProductEvent.class, event -> { if (event.getEntity() == f.cow) callback.accept(event); });
   }
   private static Fixture fixture(GameTestHelper h) {
      MachineBlockEntity machine = place(h, POS);
      DairyAnimal cow = newCow(h);
      BlockPos pos = machine.getBlockPos();
      cow.setPos(pos.getX() + .5, pos.getY(), pos.getZ() - .5);
      cow.setNoAi(true); cow.setNoGravity(true); cow.setGender(Gender.FEMALE);
      cow.setBirthTick(cow.calendar().getTicks() - 1_000_000_000L);
      cow.setOldTick(-1); cow.setFamiliarity(1); cow.setUses(0); cow.setProducedTick(-1_000_000_000L);
      h.assertTrue(cow.isReadyForAnimalProduct(), "fixture uses actual native readiness");
      h.assertTrue(h.getLevel().addFreshEntity(cow), "real TFC cow enters the test world");
      return new Fixture(h, machine, cow, pos);
   }
   private static DairyAnimal newCow(GameTestHelper h) {
      return (DairyAnimal)BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("tfc", "cow")).create(h.getLevel());
   }
   private static MachineBlockEntity place(GameTestHelper h, BlockPos relative) {
      h.setBlock(relative, Blocks.AIR);
      h.setBlock(relative.below(), Blocks.STONE);
      h.setBlock(relative, ModBlocks.SMALL_MILKING_MACHINE.get().defaultBlockState().setValue(MachineBlocks.HORIZONTAL_FACING, Direction.NORTH));
      return (MachineBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(relative));
   }
   private static IFluidHandler tank(GameTestHelper h, MachineBlockEntity machine) {
      return h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, machine.getBlockPos(), Direction.DOWN);
   }
   private static IFluidHandler tank(Fixture f) { return tank(f.helper, f.machine); }
   private static int amount(Fixture f) { return tank(f).getFluidInTank(0).getAmount(); }
   private static void tick(Fixture f, int ticks) { tick(f.helper, f.machine, ticks); }
   private static void tick(GameTestHelper h, MachineBlockEntity machine, int ticks) {
      for (int tick = 0; tick < ticks; tick++) MachineBlockEntity.serverTick(h.getLevel(), machine.getBlockPos(),
            h.getLevel().getBlockState(machine.getBlockPos()), machine);
   }
}
