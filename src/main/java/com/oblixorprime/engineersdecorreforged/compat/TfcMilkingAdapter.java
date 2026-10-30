package com.oblixorprime.engineersdecorreforged.compat;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.dries007.tfc.common.entities.livestock.DairyAnimal;
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

   static boolean tryMilk(ServerLevel level, BlockPos machine, AABB area, Predicate<FluidStack> accept) {
      for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, area,
            entity -> COW.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())))) {
         if (!(candidate instanceof DairyAnimal cow) || !ready(cow, level, area)) continue;
         Source source = new Source(level, cow.getUUID());
         if (!IN_FLIGHT.add(source)) continue;
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
                  || !ready(cow, level, area)) continue;
            AnimalProductEvent event = new AnimalProductEvent(level, cow.blockPosition(), actor, cow,
                  new FluidStack(cow.getMilkFluid(), 1000), ItemStack.EMPTY, 1);
            if (NeoForge.EVENT_BUS.post(event).isCanceled()) continue;
            FluidStack product = event.getFluidProduct().copy();
            // Event callbacks may move/age/milk the animal or change the machine's capacity.
            // The destination commits internal storage only, with no external callbacks.
            if (!ready(cow, level, area) || product.isEmpty() || !accept.test(product)) continue;
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
      return cow.level() == level && cow.isAlive() && !cow.isRemoved()
            && area.intersects(cow.getBoundingBox()) && cow.isReadyForAnimalProduct();
   }

   private static void drop(ServerLevel level, BlockPos machine, ItemStack stack) {
      if (!stack.isEmpty()) Containers.dropItemStack(level,
            machine.getX() + .5, machine.getY() + .5, machine.getZ() + .5, stack);
   }
}
