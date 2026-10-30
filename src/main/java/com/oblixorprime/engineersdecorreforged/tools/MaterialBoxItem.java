package com.oblixorprime.engineersdecorreforged.tools;

import com.oblixorprime.engineersdecorreforged.menu.MaterialBoxMenu;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MaterialBoxItem extends TooltipItem {
   public static final int SLOT_COUNT = 54;
   @Deprecated(forRemoval = false)
   public static final int CAPACITY = 512;
   private static final String TAG_STACKS = "stacks";
   private static final String LEGACY_TAG_ITEM = "stored_item";
   private static final String LEGACY_TAG_COUNT = "stored_count";

   public MaterialBoxItem(Properties properties) {
      super("material_box", properties);
   }

   @Override
   public int getMaxStackSize(ItemStack stack) {
      return data(stack).isEmpty() ? super.getMaxStackSize(stack) : 1;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack box = player.getItemInHand(hand);
      if (box.getCount() != 1) {
         return InteractionResultHolder.fail(box);
      }
      if (!level.isClientSide) {
         player.openMenu(
            new SimpleMenuProvider((id, inventory, owner) -> new MaterialBoxMenu(id, inventory, hand), box.getHoverName()),
            buffer -> buffer.writeBoolean(hand == InteractionHand.OFF_HAND)
         );
      }
      return InteractionResultHolder.success(box);
   }

   @Override
   public boolean hasCustomEntity(ItemStack stack) {
      return true;
   }

   @Override
   public Entity createEntity(Level level, Entity location, ItemStack stack) {
      MaterialBoxEntity replacement = new MaterialBoxEntity(
         level,
         location.getX(),
         location.getY(),
         location.getZ(),
         stack,
         stack.getCount() == 1 && hasContents(stack, level.registryAccess())
      );
      replacement.setDeltaMovement(location.getDeltaMovement());
      replacement.setDefaultPickUpDelay();
      replacement.setSilent(true);
      if (location instanceof ItemEntity dropped) {
         replacement.setTarget(dropped.getTarget());
         Entity owner = dropped.getOwner();
         if (owner != null) {
            replacement.setThrower(owner);
         }
      }
      return replacement;
   }

   @Override
   public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
      return true;
   }

   @Override
   public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
      return false;
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      NonNullList<ItemStack> contents = loadContents(stack, context.registries());
      int usedSlots = (int)contents.stream().filter(item -> !item.isEmpty()).count();
      if (usedSlots > 0) {
         tooltip.add(
            Component.translatable(
                  "item.immersive_engineer_decor_controls_tool_reforged.material_box.tip",
                  usedSlots,
                  SLOT_COUNT - usedSlots
               )
               .withStyle(ChatFormatting.AQUA)
         );
      }
   }

   public static NonNullList<ItemStack> loadContents(ItemStack stack, HolderLookup.Provider registries) {
      NonNullList<ItemStack> contents = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
      CompoundTag root = data(stack);
      if (root.contains(TAG_STACKS, CompoundTag.TAG_COMPOUND)) {
         ContainerHelper.loadAllItems(root.getCompound(TAG_STACKS), contents, registries);
         normalizeStackLimit(stack, contents);
         return contents;
      }

      String legacyId = root.getString(LEGACY_TAG_ITEM);
      int legacyCount = Math.min(CAPACITY, Math.max(0, root.getInt(LEGACY_TAG_COUNT)));
      if (!legacyId.isEmpty() && legacyCount > 0) {
         Item legacyItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.tryParse(legacyId));
         if (legacyItem != Items.AIR) {
            for (int slot = 0; slot < contents.size() && legacyCount > 0; slot++) {
               int moving = Math.min(legacyCount, legacyItem.getDefaultMaxStackSize());
               contents.set(slot, new ItemStack(legacyItem, moving));
               legacyCount -= moving;
            }
         }
      }
      normalizeStackLimit(stack, contents);
      return contents;
   }

   public static void saveContents(ItemStack stack, List<ItemStack> contents, HolderLookup.Provider registries) {
      CompoundTag root = data(stack);
      root.remove(LEGACY_TAG_ITEM);
      root.remove(LEGACY_TAG_COUNT);
      root.remove(TAG_STACKS);
      boolean hasContents = contents.stream().anyMatch(item -> !item.isEmpty());
      if (hasContents) {
         CompoundTag storage = new CompoundTag();
         ContainerHelper.saveAllItems(storage, NonNullList.copyOf(contents), registries);
         root.put(TAG_STACKS, storage);
         stack.set(DataComponents.MAX_STACK_SIZE, 1);
      } else {
         stack.set(DataComponents.MAX_STACK_SIZE, 64);
      }
      setData(stack, root);
   }

   public static boolean hasContents(ItemStack stack, HolderLookup.Provider registries) {
      return loadContents(stack, registries).stream().anyMatch(item -> !item.isEmpty());
   }

   public static int storedCount(ItemStack stack) {
      CompoundTag root = data(stack);
      return Math.max(0, root.getInt(LEGACY_TAG_COUNT));
   }

   public static Item storedItem(ItemStack stack) {
      String id = data(stack).getString(LEGACY_TAG_ITEM);
      if (id.isEmpty()) {
         return Items.AIR;
      }
      Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.tryParse(id));
      return item == null ? Items.AIR : item;
   }

   private static CompoundTag data(ItemStack stack) {
      return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
   }

   private static void setData(ItemStack stack, CompoundTag data) {
      if (data.isEmpty()) {
         stack.remove(DataComponents.CUSTOM_DATA);
      } else {
         stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
      }
   }

   private static void normalizeStackLimit(ItemStack stack, List<ItemStack> contents) {
      if (contents.stream().anyMatch(item -> !item.isEmpty())) {
         stack.set(DataComponents.MAX_STACK_SIZE, 1);
      }
   }

   private static final class MaterialBoxEntity extends ItemEntity {
      private static final int FILLED_LIFESPAN = 72_000;
      private static final int DAMAGE_LIFESPAN_PENALTY = 200;
      private final boolean filled;

      private MaterialBoxEntity(Level level, double x, double y, double z, ItemStack stack, boolean filled) {
         super(level, x, y, z, stack);
         this.filled = filled;
         if (filled) {
            this.lifespan = Math.max(this.lifespan, FILLED_LIFESPAN);
            this.setInvulnerable(true);
         }
      }

      @Override
      public boolean hurt(DamageSource source, float amount) {
         if (!this.filled) {
            return super.hurt(source, amount);
         }
         if (!this.level().isClientSide && !this.isRemoved()) {
            this.lifespan = Math.max(DAMAGE_LIFESPAN_PENALTY, this.lifespan - DAMAGE_LIFESPAN_PENALTY);
         }
         return false;
      }
   }
}
