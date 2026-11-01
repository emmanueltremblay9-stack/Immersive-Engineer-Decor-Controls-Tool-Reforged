package com.oblixorprime.engineersdecorreforged.utility;

import com.oblixorprime.engineersdecorreforged.ModBlocks;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class LabeledCrateItemRules {
   private LabeledCrateItemRules() {
   }

   public static boolean canStore(ItemStack stack) {
      if (stack.isEmpty()) {
         return true;
      }
      String itemPath = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
      if ((itemPath.equals("shulker_box") || itemPath.endsWith("_shulker_box")) || stack.is(ModBlocks.LABELED_CRATE.get().asItem())) {
         return false;
      }
      var container = stack.get(DataComponents.CONTAINER);
      if (container != null && container.stream().findAny().isPresent()) {
         return false;
      }
      var bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
      if (bundle != null && !bundle.isEmpty()) {
         return false;
      }

      CompoundTag blockEntityData = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag();
      if (hasStoredItems(blockEntityData)) {
         return false;
      }

      CompoundTag customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
      return !hasStoredItems(customData.getCompound("BlockEntityTag"));
   }

   private static boolean hasStoredItems(CompoundTag tag) {
      return tag.contains("Items", Tag.TAG_LIST) && !tag.getList("Items", Tag.TAG_COMPOUND).isEmpty();
   }
}