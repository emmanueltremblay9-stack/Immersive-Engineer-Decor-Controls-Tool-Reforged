package com.oblixorprime.engineersdecorreforged;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.Items;

public final class ModItems {
   public static final Items ITEMS = DeferredRegister.createItems("immersive_engineer_decor_controls_tool_reforged");
   private static final List<DeferredItem<? extends Item>> MUTABLE_ORDERED_ITEMS = new ArrayList<>();
   public static final List<DeferredItem<? extends Item>> ORDERED_ITEMS = Collections.unmodifiableList(MUTABLE_ORDERED_ITEMS);
   public static final DeferredItem<com.oblixorprime.engineersdecorreforged.block.MicroSupportItem> MICROCONDUIT_SUPPORT =
      registerItem("microconduit_support", () -> new com.oblixorprime.engineersdecorreforged.block.MicroSupportItem(new Item.Properties().stacksTo(64)));

   private ModItems() {
   }

   public static <T extends Block> DeferredItem<BlockItem> registerBlockItem(String name, DeferredBlock<T> block) {
      DeferredItem<BlockItem> item = ITEMS.registerSimpleBlockItem(name, block);
      MUTABLE_ORDERED_ITEMS.add(item);
      return item;
   }

   public static DeferredItem<Item> registerSimpleItem(String name) {
      DeferredItem<Item> item = ITEMS.registerSimpleItem(name, new Properties());
      MUTABLE_ORDERED_ITEMS.add(item);
      return item;
   }

   public static <T extends Item> DeferredItem<T> registerItem(String name, Supplier<T> supplier) {
      DeferredItem<T> item = ITEMS.register(name, supplier);
      MUTABLE_ORDERED_ITEMS.add(item);
      return item;
   }
}
