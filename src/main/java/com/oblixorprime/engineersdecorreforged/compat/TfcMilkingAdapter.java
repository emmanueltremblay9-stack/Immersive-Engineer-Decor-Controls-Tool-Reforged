package com.oblixorprime.engineersdecorreforged.compat;

import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.utility.MilkingState;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.dries007.tfc.common.entities.livestock.DairyAnimal;
import net.dries007.tfc.common.entities.livestock.Age;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.util.events.AnimalProductEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.fluids.FluidStack;

/** Loaded exclusively through the guarded optional boundary. No native source is copied. */
final class TfcMilkingAdapter {
   private static final ResourceLocation COW = ResourceLocation.fromNamespaceAndPath("tfc", "cow");
   private static final Set<Source> IN_FLIGHT = ConcurrentHashMap.newKeySet();
   private record Source(ServerLevel level, UUID animal) {}

   static MilkingState select(ServerLevel level, AABB area, Set<UUID> deferred, int previousAttemptId) {
      MilkingState best = MilkingState.empty();
      for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, area,
            entity -> COW.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())))) {
         if (!(candidate instanceof DairyAnimal cow) || !present(cow, level, area)) continue;
         MilkingState next = observe(cow);
         // Ready first, then the next eligible recovering cow. Rotate ready IDs
         // after each attempt so bounded refusal-cache eviction cannot starve a herd.
         if (best.source() == MilkingState.NONE || rank(next, deferred) < rank(best, deferred)
               || rank(next, deferred) == rank(best, deferred) && before(next, best, previousAttemptId)) best = next;
      }
      return best;
   }

   private static boolean before(MilkingState next, MilkingState best, int previousAttemptId) {
      if (next.state() == MilkingState.READY)
         return distance(next.entityId(), previousAttemptId) < distance(best.entityId(), previousAttemptId);
      return next.remainingTicks() < best.remainingTicks()
            || next.remainingTicks() == best.remainingTicks() && next.entityId() < best.entityId();
   }

   private static long distance(int id, int previous) {
      long delta = (Integer.toUnsignedLong(id) - Integer.toUnsignedLong(previous)) & 0xFFFFFFFFL;
      return delta == 0 ? 0x100000000L : delta;
   }

   private static int rank(MilkingState state, Set<UUID> deferred) {
      if (state.state() == MilkingState.READY) return deferred.contains(state.animalId()) ? 3 : 0;
      return state.state() == MilkingState.RECOVERING ? 1 : 4;
   }

   static MilkingState inspect(ServerLevel level, AABB area, UUID animal) {
      var entity = level.getEntity(animal);
      return entity instanceof DairyAnimal cow && COW.equals(BuiltInRegistries.ENTITY_TYPE.getKey(cow.getType()))
            && present(cow, level, area) ? observe(cow) : MilkingState.empty();
   }

   private static MilkingState observe(DairyAnimal cow) {
      boolean eligible = cow.isFemale() && cow.getAgeType() == Age.ADULT
            && cow.getFamiliarity() > TFCConfig.SERVER.cowConfig.produceFamiliarity().get();
      long remaining = cow.getProductsCooldown();
      int state = cow.isReadyForAnimalProduct() ? MilkingState.READY
            : eligible && remaining > 0 ? MilkingState.RECOVERING : MilkingState.INELIGIBLE;
      return new MilkingState(MilkingState.TFC, state, cow.getId(), cow.getUUID(), remaining,
            TFCConfig.SERVER.cowConfig.produceTicks().get(), 0);
   }

   static boolean tryMilk(ServerLevel level, BlockPos machine, AABB area, UUID animal, Predicate<FluidStack> accept) {
      var entity = level.getEntity(animal);
      if (entity instanceof DairyAnimal cow && COW.equals(BuiltInRegistries.ENTITY_TYPE.getKey(cow.getType()))
            && ready(cow, level, area)) {
         Source source = new Source(level, cow.getUUID());
         if (!IN_FLIGHT.add(source)) return false;
         // No synthetic bucket is created or exported. This actor represents an empty-handed
         // automated interaction; actual machine bucket conversion occurs only at commit.
         FakePlayer actor = null;
         try {
            actor = new FakePlayer(level, new GameProfile(UUID.nameUUIDFromBytes(
                  ("IEDCTR milker:" + level.dimension().location() + ":" + machine.asLong())
                        .getBytes(StandardCharsets.UTF_8)), "[IEDCTR Milker]"));
            actor.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            actor.setPos(machine.getX() + .5, machine.getY() + .5, machine.getZ() + .5);
            if (CommonHooks.onInteractEntity(actor, cow, InteractionHand.MAIN_HAND) != null
                  || !ready(cow, level, area)) return false;
            AnimalProductEvent event = new AnimalProductEvent(level, cow.blockPosition(), actor, cow,
                  new FluidStack(cow.getMilkFluid(), 1000), ItemStack.EMPTY, 1);
            if (NeoForge.EVENT_BUS.post(event).isCanceled()) return false;
            FluidStack product = event.getFluidProduct().copy();
            // Event callbacks may move/age/milk the animal or change the machine's capacity.
            // The destination commits internal storage only, with no external callbacks.
            if (!ready(cow, level, area) || product.isEmpty() || !accept.test(product)) return false;
            cow.setProductsCooldown();
            cow.addUses(event.getUses());
            return true;
         } finally {
            try {
               // A third-party interaction hook may itself give the actor items. Preserve
               // those real outputs even when it cancels our operation; we supplied no items.
               if (actor != null) {
                  for (int slot = 0; slot < actor.getInventory().getContainerSize(); slot++) {
                     drop(level, machine, actor.getInventory().removeItemNoUpdate(slot));
                  }
                  drop(level, machine, actor.containerMenu.getCarried());
                  actor.containerMenu.setCarried(ItemStack.EMPTY);
               }
            } finally {
               IN_FLIGHT.remove(source);
            }
         }
      }
      return false;
   }

   private static boolean ready(DairyAnimal cow, ServerLevel level, AABB area) {
      return present(cow, level, area) && cow.isReadyForAnimalProduct();
   }

   private static boolean present(DairyAnimal cow, ServerLevel level, AABB area) {
      return cow.level() == level && cow.isAlive() && !cow.isRemoved() && area.intersects(cow.getBoundingBox());
   }

   private static void drop(ServerLevel level, BlockPos machine, ItemStack stack) {
      if (!stack.isEmpty()) Containers.dropItemStack(level,
            machine.getX() + .5, machine.getY() + .5, machine.getZ() + .5, stack);
   }
}
