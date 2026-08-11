package com.oblixorprime.engineersdecorreforged;

import com.mojang.logging.LogUtils;
import com.oblixorprime.engineersdecorreforged.network.ModNetworking;
import com.oblixorprime.engineersdecorreforged.rsgauges.ControlsModule;
import com.oblixorprime.engineersdecorreforged.tools.EngineerToolsModule;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod("immersive_engineer_decor_controls_tool_reforged")
public final class EngineersDecorReforged {
   public static final String MOD_ID = "immersive_engineer_decor_controls_tool_reforged";
   public static final String LEGACY_MOD_ID = "engineers_decor_reforged";
   public static final Logger LOGGER = LogUtils.getLogger();
   private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "immersive_engineer_decor_controls_tool_reforged");
   public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_TABS.register(
      "main",
      () -> CreativeModeTab.builder()
         .title(Component.translatable("itemGroup.immersive_engineer_decor_controls_tool_reforged"))
         .withTabsBefore(new ResourceKey[]{CreativeModeTabs.BUILDING_BLOCKS})
         .icon(EngineersDecorReforged::creativeTabIcon)
         .displayItems((parameters, output) -> ModItems.ORDERED_ITEMS.stream()
            .filter(EngineersDecorReforged::shouldShowInCreativeTab)
            .forEach(item -> output.accept((ItemLike)item.get())))
         .build()
   );

   public EngineersDecorReforged(IEventBus modEventBus, ModContainer modContainer) {
      NeoForgeMod.enableMilkFluid();
      modContainer.registerConfig(Type.COMMON, ReforgedConfig.SPEC);
      ControlsModule.init();
      EngineerToolsModule.init();
      addLegacyRegistryAliases();
      ModBlocks.BLOCKS.register(modEventBus);
      ModItems.ITEMS.register(modEventBus);
      ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
      ModMenus.MENU_TYPES.register(modEventBus);
      ModRecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);
      CREATIVE_TABS.register(modEventBus);
      modEventBus.addListener(this::commonSetup);
      modEventBus.addListener(this::registerCapabilities);
      modEventBus.addListener(ModNetworking::register);
   }

   private static void addLegacyRegistryAliases() {
      ModBlocks.BLOCKS.getEntries().forEach(holder -> ModBlocks.BLOCKS.addAlias(legacyId(holder.getId()), holder.getId()));
      ModItems.ITEMS.getEntries().forEach(holder -> ModItems.ITEMS.addAlias(legacyId(holder.getId()), holder.getId()));
      ModBlockEntities.BLOCK_ENTITY_TYPES.addAlias(
         legacyId(ResourceLocation.fromNamespaceAndPath(MOD_ID, "machine")),
         ResourceLocation.fromNamespaceAndPath(MOD_ID, "machine")
      );
      ModMenus.machineTypes().values().forEach(holder -> ModMenus.MENU_TYPES.addAlias(legacyId(holder.getId()), holder.getId()));
      ModRecipeSerializers.RECIPE_SERIALIZERS.addAlias(legacyId(ModRecipeSerializers.REDIA_TOOL_REPAIR.getId()), ModRecipeSerializers.REDIA_TOOL_REPAIR.getId());
      CREATIVE_TABS.addAlias(legacyId(MAIN_TAB.getId()), MAIN_TAB.getId());
   }

   private static ResourceLocation legacyId(ResourceLocation currentId) {
      return ResourceLocation.fromNamespaceAndPath(LEGACY_MOD_ID, currentId.getPath());
   }

   private void commonSetup(FMLCommonSetupEvent event) {
      LOGGER.info(
         "Loaded Engineer's Decor & Controls Reforged with {} registered blocks and {} registered items.",
         ModBlocks.ORDERED_BLOCKS.size(),
         ModItems.ORDERED_ITEMS.size()
      );
   }

   private void registerCapabilities(RegisterCapabilitiesEvent event) {
      event.registerBlockEntity(ItemHandler.BLOCK, ModBlockEntities.MACHINE.get(), (machine, side) -> machine.itemHandler(side));
      event.registerBlockEntity(FluidHandler.BLOCK, ModBlockEntities.MACHINE.get(), (machine, side) -> machine.fluidHandler(side));
      event.registerBlockEntity(EnergyStorage.BLOCK, ModBlockEntities.MACHINE.get(), (machine, side) -> machine.energyStorage(side));
   }

   private static ItemStack creativeTabIcon() {
      if (ReforgedConfig.isEnabled(ReforgedConfig.ENABLE_DECOR_BLOCKS)) {
         return new ItemStack((ItemLike)ModBlocks.CLINKER_BRICK_BLOCK.get());
      }
      if (ReforgedConfig.isEnabled(ReforgedConfig.ENABLE_UTILITY_BLOCKS)) {
         return new ItemStack((ItemLike)ModBlocks.METAL_CRAFTING_TABLE.get());
      }
      return new ItemStack((ItemLike)Items.STONE);
   }

   public static boolean shouldShowInCreativeTab(DeferredItem<? extends Item> item) {
      String name = item.getId().getPath();
      if (ControlsModule.isControlContent(name)) {
         return ControlsModule.shouldShowInCreativeTab(name);
      }
      if (!(item.get() instanceof BlockItem)) {
         return true;
      }
      if (ReforgedConfig.isUtilityBlockItem(name)) {
         return ReforgedConfig.isEnabled(ReforgedConfig.ENABLE_UTILITY_BLOCKS);
      }
      return ReforgedConfig.isEnabled(ReforgedConfig.ENABLE_DECOR_BLOCKS);
   }
}
