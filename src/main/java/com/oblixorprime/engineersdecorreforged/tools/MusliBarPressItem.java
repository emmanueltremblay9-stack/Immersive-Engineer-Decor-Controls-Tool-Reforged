package com.oblixorprime.engineersdecorreforged.tools;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import com.oblixorprime.engineersdecorreforged.menu.MusliBarPressMenu;
import com.oblixorprime.engineersdecorreforged.tools.muslee.MusleeSessionIngress;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

public class MusliBarPressItem extends TooltipItem {
   private static final int OUTPUT_COUNT = 4;
   public static final TagKey<Item> ACCEPTED_SEEDS = TagKey.create(
      Registries.ITEM, ResourceLocation.fromNamespaceAndPath("immersive_engineer_decor_controls_tool_reforged", "musli_bar_press_seeds")
   );

   public MusliBarPressItem(Properties properties) {
      super("musli_bar_press", properties);
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack press = player.getItemInHand(hand);
      if (level.isClientSide) return InteractionResultHolder.success(press);
      if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.pass(press);
      MusleeSessionIngress ingress = MusleeSessionIngress.open();
      serverPlayer.openMenu(
         new SimpleMenuProvider(
            (id, inventory, actor) -> new MusliBarPressMenu(id, inventory, hand, ingress),
            Component.translatable("item.immersive_engineer_decor_controls_tool_reforged.musli_bar_press")
         ),
         buffer -> {
            buffer.writeEnum(hand);
            buffer.writeUUID(ingress.sessionId());
         }
      );
      return InteractionResultHolder.success(press);
   }

   /**
    * Historical verification fixture only; live right-click never calls this.
    * It keeps the previous immediate-craft regression tests as historical evidence.
    * Do not reactivate while cursor-only durability and persistence gates are open.
    */
   @Deprecated(forRemoval = true)
   public static InteractionResultHolder<ItemStack> historicalImmediatePressFixture(
      Level level, Player player, InteractionHand hand
   ) {
      ItemStack press = player.getItemInHand(hand);
      Inventory inventory = player.getInventory();
      ItemStack output = new ItemStack((ItemLike)EngineerToolsModule.MUSLI_BAR.get(), 4);
      if (player.getAbilities().instabuild || hasItem(inventory, Items.BREAD) && hasItem(inventory, Items.APPLE) && hasAcceptedSeed(inventory)) {
         if (!canFit(inventory, output, !player.getAbilities().instabuild)) {
            return fail(level, player, press, "item.immersive_engineer_decor_controls_tool_reforged.musli_bar_press.msg.full");
         }

         if (!level.isClientSide) {
            if (!player.getAbilities().instabuild) {
               consumeOne(inventory, Items.BREAD);
               consumeOne(inventory, Items.APPLE);
               consumeAcceptedSeed(inventory);
            }

            inventory.add(output);
            if (!player.getAbilities().instabuild) {
               press.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }

            level.playSound(null, player.blockPosition(), SoundEvents.HONEY_BLOCK_PLACE, player.getSoundSource(), 0.6F, 1.1F);
            player.displayClientMessage(Component.translatable("item.immersive_engineer_decor_controls_tool_reforged.musli_bar_press.msg.pressed"), true);
         }

         return InteractionResultHolder.success(press);
      } else {
         return fail(level, player, press, "item.immersive_engineer_decor_controls_tool_reforged.musli_bar_press.msg.ingredients");
      }
   }

   private static boolean hasItem(Inventory inventory, Item item) {
      for (int i = 0; i < inventory.getContainerSize(); i++) {
         if (inventory.getItem(i).is(item)) {
            return true;
         }
      }

      return false;
   }

   private static boolean hasAcceptedSeed(Inventory inventory) {
      for (int i = 0; i < inventory.getContainerSize(); i++) {
         if (inventory.getItem(i).is(ACCEPTED_SEEDS)) {
            return true;
         }
      }

      return false;
   }

   private static void consumeOne(Inventory inventory, Item item) {
      for (int i = 0; i < inventory.getContainerSize(); i++) {
         ItemStack stack = inventory.getItem(i);
         if (stack.is(item)) {
            stack.shrink(1);
            return;
         }
      }
   }

   private static void consumeAcceptedSeed(Inventory inventory) {
      for (int i = 0; i < inventory.getContainerSize(); i++) {
         ItemStack stack = inventory.getItem(i);
         if (stack.is(ACCEPTED_SEEDS)) {
            stack.shrink(1);
            return;
         }
      }
   }

   private static boolean canFit(Inventory inventory, ItemStack stack, boolean consumesIngredients) {
      int remaining = stack.getCount();
      int[] ingredients = consumesIngredients ? new int[]{1, 1, 1} : new int[]{0, 0, 0};

      for (int i = 0; i < inventory.items.size(); i++) {
         ItemStack existing = (ItemStack)inventory.items.get(i);
         int simulatedCount = simulatedCountAfterIngredientUse(existing, ingredients);
         remaining = accountOutputSpace(inventory, stack, existing, simulatedCount, remaining, true);
         if (remaining <= 0) {
            return true;
         }
      }

      for (int i = 0; i < inventory.armor.size(); i++) {
         simulatedCountAfterIngredientUse((ItemStack)inventory.armor.get(i), ingredients);
      }

      ItemStack offhand = (ItemStack)inventory.offhand.get(0);
      simulatedCountAfterIngredientUse(offhand, ingredients);
      return remaining <= 0;
   }

   private static int simulatedCountAfterIngredientUse(ItemStack stack, int[] ingredients) {
      int count = stack.getCount();
      if (count <= 0) {
         return 0;
      } else if (ingredients[0] > 0 && stack.is(Items.BREAD)) {
         ingredients[0]--;
         return count - 1;
      } else if (ingredients[1] > 0 && stack.is(Items.APPLE)) {
         ingredients[1]--;
         return count - 1;
      } else if (ingredients[2] > 0 && stack.is(ACCEPTED_SEEDS)) {
         ingredients[2]--;
         return count - 1;
      } else {
         return count;
      }
   }

   private static int accountOutputSpace(
      Inventory inventory, ItemStack output, ItemStack existing, int simulatedCount, int remaining, boolean emptySlotCanAccept
   ) {
      if (simulatedCount <= 0) {
         return emptySlotCanAccept ? remaining - Math.min(output.getMaxStackSize(), inventory.getMaxStackSize()) : remaining;
      } else {
         return ItemStack.isSameItemSameComponents(existing, output)
            ? remaining - Math.max(0, Math.min(existing.getMaxStackSize(), inventory.getMaxStackSize()) - simulatedCount)
            : remaining;
      }
   }

   private static InteractionResultHolder<ItemStack> fail(Level level, Player player, ItemStack press, String key) {
      if (!level.isClientSide) {
         player.displayClientMessage(Component.translatable(key), true);
      }

      return InteractionResultHolder.fail(press);
   }
}
