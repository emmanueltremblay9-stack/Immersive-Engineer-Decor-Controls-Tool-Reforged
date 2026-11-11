package com.oblixorprime.engineersdecorreforged.client;

import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.oblixorprime.engineersdecorreforged.ModBlockEntities;
import com.oblixorprime.engineersdecorreforged.ModMenus;
import com.oblixorprime.engineersdecorreforged.utility.LabeledCrateLabelClientBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;

@EventBusSubscriber(modid = "immersive_engineer_decor_controls_tool_reforged", value = Dist.CLIENT)
public final class ClientModEvents {
   private ClientModEvents() {
   }

   @SubscribeEvent
   public static void registerKeys(RegisterKeyMappingsEvent event) {
      event.register(EngineerManualShortcut.OPEN);
   }

   @SubscribeEvent
   public static void clientSetup(FMLClientSetupEvent event) {
      event.enqueueWork(
         () -> LabeledCrateLabelClientBridge.setOpener((pos, lines) -> Minecraft.getInstance().setScreen(new LabeledCrateEditScreen(pos, lines)))
      );
   }

   @SubscribeEvent
   public static void registerMenuScreens(RegisterMenuScreensEvent event) {
      event.register((MenuType)ModMenus.METAL_CRAFTING_TABLE.get(), MachineScreen.MetalCraftingTableScreen::new);
      event.register((MenuType)ModMenus.LABELED_CRATE.get(), LabeledCrateScreen::new);
      event.register((MenuType)ModMenus.FACTORY_HOPPER.get(), MachineScreen.FactoryHopperScreen::new);
      event.register((MenuType)ModMenus.FACTORY_DROPPER.get(), MachineScreen.FactoryDropperScreen::new);
      event.<MachineMenu, AbstractContainerScreen<MachineMenu>>register(ModMenus.FACTORY_PLACER.get(), (menu, inventory, title) ->
         menu.factoryPlacerV2ReferencePresentation()
            ? new FactoryPlacerV2ReferenceScreen(menu, inventory, title)
            : new MachineScreen.FactoryPlacerScreen(menu, inventory, title));
      event.register((MenuType)ModMenus.SMALL_BLOCK_BREAKER.get(), MachineScreen.SmallBlockBreakerScreen::new);
      event.register((MenuType)ModMenus.SMALL_WASTE_INCINERATOR.get(), MachineScreen.SmallWasteIncineratorScreen::new);
      event.register((MenuType)ModMenus.SMALL_LAB_FURNACE.get(), MachineScreen.SmallLabFurnaceScreen::new);
      event.register((MenuType)ModMenus.SMALL_ELECTRICAL_FURNACE.get(), MachineScreen.SmallElectricalFurnaceScreen::new);
      event.register((MenuType)ModMenus.SMALL_MINERAL_SMELTER.get(), SmallMineralSmelterHybridScreen::new);
      event.register((MenuType)ModMenus.SMALL_FREEZER.get(), MachineScreen.SmallFreezerScreen::new);
      event.<MachineMenu, AbstractContainerScreen<MachineMenu>>register(ModMenus.FLUID_BARREL.get(), (menu, inventory, title) -> menu.barrelReferencePresentation()
         ? new BarrelReferenceScreen(menu, inventory, title) : new MachineScreen.FluidBarrelScreen(menu, inventory, title));
      event.register((MenuType)ModMenus.SMALL_FLUID_FUNNEL.get(), MachineScreen.SmallFluidFunnelScreen::new);
      event.register((MenuType)ModMenus.PASSIVE_FLUID_ACCUMULATOR.get(), MachineScreen.PassiveFluidAccumulatorScreen::new);
      event.<MachineMenu, AbstractContainerScreen<MachineMenu>>register(ModMenus.SMALL_SOLAR_PANEL.get(), (menu, inventory, title) -> menu.solarV2ReferencePresentation()
         ? new SolarV2ReferenceScreen(menu, inventory, title) : new MachineScreen.SmallSolarPanelScreen(menu, inventory, title));
      event.register((MenuType)ModMenus.SMALL_MILKING_MACHINE.get(), MachineScreen.SmallMilkingMachineScreen::new);
      event.register((MenuType)ModMenus.SMALL_TREE_CUTTER.get(), TreeCutterV3ReferenceScreen::new);
   }

   @SubscribeEvent
   public static void registerBlockEntityRenderers(RegisterRenderers event) {
      event.registerBlockEntityRenderer((BlockEntityType)ModBlockEntities.MACHINE.get(), LabeledCrateBlockEntityRenderer::new);
   }
}
