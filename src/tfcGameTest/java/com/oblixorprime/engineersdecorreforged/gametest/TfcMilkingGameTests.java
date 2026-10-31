package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlocks;
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
