package com.oblixorprime.engineersdecorreforged.menu;

import com.oblixorprime.engineersdecorreforged.ModMenus;
import com.oblixorprime.engineersdecorreforged.tools.EngineerToolsModule;
import com.oblixorprime.engineersdecorreforged.tools.MusliBarPressReserve;
import com.oblixorprime.engineersdecorreforged.tools.muslee.MusleeSessionIngress;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Provisional, non-mutating Musli GUI. The existing cursor protocol and reserve
 * planner are intentionally not an authorization to consume or produce items.
 * Runtime activation requires the independently qualified durable participant,
 * total capture-memory bound, FIFO ownership and closed-session recovery.
 */
public final class MusliBarPressMenu extends AbstractContainerMenu {
   public static final int RESERVE_COUNT = 3;
   public static final int PLAYER_SLOT_COUNT = 36;
   public static final boolean TRANSACTIONS_QUALIFIED = false;
   private final Inventory playerInventory;
   private final InteractionHand pressHand;
   private final UUID sessionId;
   private final MusleeSessionIngress ingress;
   private final ContainerData reserveData;
   private boolean closed;

   public MusliBarPressMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
      this(id, inventory, extra.readEnum(InteractionHand.class), extra.readUUID(), null);
   }

   public MusliBarPressMenu(int id, Inventory inventory, InteractionHand hand, MusleeSessionIngress ingress) {
      this(id, inventory, hand, ingress.sessionId(), ingress);
   }

   private MusliBarPressMenu(int id, Inventory inventory, InteractionHand hand, UUID sessionId, MusleeSessionIngress ingress) {
      super(ModMenus.MUSLI_BAR_PRESS.get(), id);
      this.playerInventory = inventory;
      this.pressHand = hand;
      this.sessionId = sessionId;
      this.ingress = ingress;
      this.reserveData = ingress == null ? new SimpleContainerData(RESERVE_COUNT) : new ContainerData() {
         @Override
         public int get(int index) {
            if (index < 0 || index >= RESERVE_COUNT) return 0;
            ItemStack press = MusliBarPressMenu.this.playerInventory.player.getItemInHand(MusliBarPressMenu.this.pressHand);
            var state = MusliBarPressReserve.read(press, MusliBarPressMenu.this.playerInventory.player.level().registryAccess());
            if (state.isEmpty()) return 0;
            return switch (index) {
               case 0 -> state.get().cereal();
               case 1 -> state.get().fruit();
               default -> state.get().seeds();
            };
         }
         @Override
         public void set(int index, int value) {
            // Server values are derived solely from the held press, never from client data.
         }
         @Override
         public int getCount() { return RESERVE_COUNT; }
      };
      this.addDataSlots(this.reserveData);
      for (int row = 0; row < 3; row++) {
         for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, 9 + column + row * 9, 45 + column * 18, 133 + row * 18));
         }
      }
      for (int column = 0; column < 9; column++) {
         this.addSlot(new Slot(inventory, column, 45 + column * 18, 191));
      }
   }

   public UUID sessionId() { return sessionId; }
   public int reserve(int index) {
      return index >= 0 && index < RESERVE_COUNT ? reserveData.get(index) : 0;
   }

   public boolean hasAuthoritativeAccess(ServerPlayer player) {
      return !closed && ingress != null && player != null
         && !player.isRemoved() && !player.isSpectator()
         && player == this.playerInventory.player
         && player.containerMenu == this
         && player.getItemInHand(pressHand).is(EngineerToolsModule.MUSLI_BAR_PRESS.get())
         && ingress.phase() == MusleeSessionIngress.Phase.ACTIVE;
   }

   /** A recognized intent is never a write permit while the durability gate is shut. */
   public MusleeSessionIngress.Status receiveIntent(ServerPlayer player, byte[] encoded) {
      if (ingress == null) return MusleeSessionIngress.Status.UNAUTHORIZED;
      MusleeSessionIngress.Result recognized = ingress.receive(encoded, () -> hasAuthoritativeAccess(player));
      // No use of MusliBarPressCursorPlan here. The source and cursor are not
      // committed before the real transaction/persistence boundary is proven.
      return recognized.status();
   }

   @Override
   public boolean stillValid(Player player) {
      return !closed && player != null && !player.isRemoved()
         && player == playerInventory.player
         && player.getItemInHand(pressHand).is(EngineerToolsModule.MUSLI_BAR_PRESS.get());
   }

   @Override
   public ItemStack quickMoveStack(Player player, int index) {
      // No machine inventory or hidden deposit slot exists.
      return ItemStack.EMPTY;
   }

   @Override
   public void removed(Player player) {
      if (!closed) {
         closed = true;
         if (ingress != null) {
            ingress.invalidate();
            ingress.finishInvalidation();
         }
      }
      // Vanilla owns cursor restitution on normal close or disconnect. Never
      // synthesize a replacement cursor from a stale Musli command or snapshot.
      super.removed(player);
   }
}
