package com.oblixorprime.engineersdecorreforged.tools;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.menu.MusliBarPressMenu;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public class MusliBarPressItem extends TooltipItem {
   public static final int MAX_SEEDS = 512;
   public static final int MAX_FOOD = 768;
   public static final int SEEDS_PER_BAR = 1;
   public static final int FOOD_PER_BAR = 8;
   public static final TagKey<Item> ACCEPTED_SEEDS = TagKey.create(
      Registries.ITEM, ResourceLocation.fromNamespaceAndPath(EngineersDecorReforged.MOD_ID, "musli_bar_press_seeds")
   );
   public static final TagKey<Item> FOOD_BLACKLIST = TagKey.create(
      Registries.ITEM, ResourceLocation.fromNamespaceAndPath(EngineersDecorReforged.MOD_ID, "musli_bar_food_blacklisted")
   );
   private static final String TAG_INVENTORY = "inventory";
   private static final String TAG_SEEDS = "seeds_stored";
   private static final String TAG_FOOD = "food_stored";

   public MusliBarPressItem(Properties properties) {
      super("musli_bar_press", properties);
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack press = player.getItemInHand(hand);
      if (!level.isClientSide) {
         player.openMenu(
            new SimpleMenuProvider((id, inventory, owner) -> new MusliBarPressMenu(id, inventory, hand), press.getHoverName()),
            buffer -> buffer.writeBoolean(hand == InteractionHand.OFF_HAND)
         );
      }
      return InteractionResultHolder.success(press);
   }

   public static boolean isValidSeed(ItemStack stack) {
      return !stack.isEmpty() && stack.is(ACCEPTED_SEEDS);
   }

   public static boolean isValidFood(ItemStack stack, Player player) {
      FoodProperties food = stack.getFoodProperties(player);
      return food != null && food.nutrition() > 0 && !stack.is(EngineerToolsModule.MUSLI_BAR.get()) && !stack.is(FOOD_BLACKLIST);
   }

   public static NonNullList<ItemStack> loadInventory(ItemStack press, HolderLookup.Provider registries) {
      NonNullList<ItemStack> contents = NonNullList.withSize(2, ItemStack.EMPTY);
      CompoundTag root = data(press);
      if (root.contains(TAG_INVENTORY, CompoundTag.TAG_COMPOUND)) {
         ContainerHelper.loadAllItems(root.getCompound(TAG_INVENTORY), contents, registries);
      }
      return contents;
   }

   public static int loadSeeds(ItemStack press) {
      return Math.clamp(data(press).getInt(TAG_SEEDS), 0, MAX_SEEDS);
   }

   public static int loadFood(ItemStack press) {
      return Math.clamp(data(press).getInt(TAG_FOOD), 0, MAX_FOOD);
   }

   public static void saveState(ItemStack press, ListLike contents, int seeds, int food, HolderLookup.Provider registries) {
      CompoundTag root = data(press);
      CompoundTag inventory = new CompoundTag();
      ContainerHelper.saveAllItems(inventory, contents.items(), registries);
      root.put(TAG_INVENTORY, inventory);
      root.putInt(TAG_SEEDS, Math.clamp(seeds, 0, MAX_SEEDS));
      root.putInt(TAG_FOOD, Math.clamp(food, 0, MAX_FOOD));
      press.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
   }

   public record ListLike(NonNullList<ItemStack> items) {
      public static ListLike copyOf(java.util.List<ItemStack> stacks) {
         return new ListLike(NonNullList.copyOf(stacks));
      }
   }

   private static CompoundTag data(ItemStack stack) {
      return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
   }
}
