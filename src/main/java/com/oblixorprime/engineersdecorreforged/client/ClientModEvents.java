package com.oblixorprime.engineersdecorreforged.client;

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
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;

@EventBusSubscriber(modid = "immersive_engineer_decor_controls_tool_reforged", value = Dist.CLIENT)
public final class ClientModEvents {
   private ClientModEvents() {
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
      event.register((MenuType)ModMenus.LABELED_CRATE.get(), MachineScreen.LabeledCrateScreen::new);
      event.register((MenuType)ModMenus.FACTORY_HOPPER.get(), MachineScreen.FactoryHopperScreen::new);
      event.register((MenuType)ModMenus.FACTORY_DROPPER.get(), MachineScreen.FactoryDropperScreen::new);
      event.register((MenuType)ModMenus.FACTORY_PLACER.get(), MachineScreen.FactoryPlacerScreen::new);
      event.register((MenuType)ModMenus.SMALL_BLOCK_BREAKER.get(), MachineScreen.SmallBlockBreakerScreen::new);
      event.register((MenuType)ModMenus.SMALL_WASTE_INCINERATOR.get(), MachineScreen.SmallWasteIncineratorScreen::new);
      event.register((MenuType)ModMenus.SMALL_LAB_FURNACE.get(), MachineScreen.SmallLabFurnaceScreen::new);
      event.register((MenuType)ModMenus.SMALL_ELECTRICAL_FURNACE.get(), MachineScreen.SmallElectricalFurnaceScreen::new);
      event.register((MenuType)ModMenus.SMALL_MINERAL_SMELTER.get(), MachineScreen.SmallMineralSmelterScreen::new);
      event.register((MenuType)ModMenus.SMALL_FREEZER.get(), MachineScreen.SmallFreezerScreen::new);
      event.register((MenuType)ModMenus.FLUID_BARREL.get(), MachineScreen.FluidBarrelScreen::new);
      event.register((MenuType)ModMenus.SMALL_FLUID_FUNNEL.get(), MachineScreen.SmallFluidFunnelScreen::new);
      event.register((MenuType)ModMenus.PASSIVE_FLUID_ACCUMULATOR.get(), MachineScreen.PassiveFluidAccumulatorScreen::new);
      event.register((MenuType)ModMenus.SMALL_SOLAR_PANEL.get(), MachineScreen.SmallSolarPanelScreen::new);
      event.register((MenuType)ModMenus.SMALL_MILKING_MACHINE.get(), MachineScreen.SmallMilkingMachineScreen::new);
      event.register((MenuType)ModMenus.SMALL_TREE_CUTTER.get(), MachineScreen.SmallTreeCutterScreen::new);
   }

   @SubscribeEvent
   public static void registerBlockEntityRenderers(RegisterRenderers event) {
      event.registerBlockEntityRenderer((BlockEntityType)ModBlockEntities.MACHINE.get(), LabeledCrateBlockEntityRenderer::new);
   }
}
