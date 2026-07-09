package com.oblixorprime.engineersdecorreforged.compat.jei;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.client.MachineScreen;
import com.oblixorprime.engineersdecorreforged.tools.EngineerToolsModule;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

@JeiPlugin
public final class EngineersDecorJeiPlugin implements IModPlugin {
   private static final ResourceLocation PLUGIN_UID = ResourceLocation.fromNamespaceAndPath(EngineersDecorReforged.MOD_ID, "jei_plugin");
   private static final String INFO_PREFIX = "jei.info." + EngineersDecorReforged.MOD_ID + ".";

   @Override
   public ResourceLocation getPluginUid() {
      return PLUGIN_UID;
   }

   @Override
   public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
      registration.addRecipeCatalyst(ModBlocks.METAL_CRAFTING_TABLE.get(), RecipeTypes.CRAFTING);
      registration.addRecipeCatalyst(ModBlocks.SMALL_LAB_FURNACE.get(), RecipeTypes.SMELTING);
      registration.addRecipeCatalyst(ModBlocks.SMALL_ELECTRICAL_FURNACE.get(), RecipeTypes.SMELTING);
   }

   @Override
   public void registerRecipes(IRecipeRegistration registration) {
      addInfo(registration, EngineerToolsModule.REDIA_TOOL.get(), "redia_tool");
      addInfo(registration, EngineerToolsModule.CRUSHING_HAMMER.get(), "crushing_hammer");
      addInfo(registration, EngineerToolsModule.TRACKER.get(), "tracker");
      addInfo(registration, EngineerToolsModule.ARIADNE_COAL.get(), "ariadne_coal");
      addInfo(registration, EngineerToolsModule.STIMPACK.get(), "stimpack");
      addInfo(registration, EngineerToolsModule.SLEEPING_BAG.get(), "sleeping_bag");
      addInfo(registration, EngineerToolsModule.MATERIAL_BOX.get(), "material_box");
      addInfo(registration, EngineerToolsModule.DIVING_CAPSULE.get(), "diving_capsule");
      addInfo(registration, EngineerToolsModule.MUSLI_BAR_PRESS.get(), "musli_bar_press");
      addInfo(registration, EngineerToolsModule.MUSLI_BAR.get(), "musli_bar");
      addInfo(registration, EngineerToolsModule.CHARGED_LAPIS_SQUEEZER.get(), "charged_lapis_squeezer");
      addInfo(registration, EngineerToolsModule.CHARGED_LAPIS.get(), "charged_lapis");
      addInfo(registration, EngineerToolsModule.IRON_GRIT.get(), "iron_grit");
      addInfo(registration, EngineerToolsModule.GOLD_GRIT.get(), "gold_grit");

      addInfo(registration, ModBlocks.METAL_CRAFTING_TABLE.get(), "metal_crafting_table");
      addInfo(registration, ModBlocks.SMALL_LAB_FURNACE.get(), "small_lab_furnace");
      addInfo(registration, ModBlocks.SMALL_ELECTRICAL_FURNACE.get(), "small_electrical_furnace");
      addInfo(registration, ModBlocks.SMALL_MINERAL_SMELTER.get(), "small_mineral_smelter");
      addInfo(registration, ModBlocks.SMALL_FREEZER.get(), "small_freezer");
      addInfo(registration, ModBlocks.FLUID_BARREL.get(), "fluid_barrel");
      addInfo(registration, ModBlocks.SMALL_FLUID_FUNNEL.get(), "small_fluid_funnel");
      addInfo(registration, ModBlocks.PASSIVE_FLUID_ACCUMULATOR.get(), "passive_fluid_accumulator");
      addInfo(registration, ModBlocks.SMALL_SOLAR_PANEL.get(), "small_solar_panel");
      addInfo(registration, ModBlocks.SMALL_MILKING_MACHINE.get(), "small_milking_machine");
      addInfo(registration, ModBlocks.FACTORY_HOPPER.get(), "factory_hopper");
      addInfo(registration, ModBlocks.FACTORY_DROPPER.get(), "factory_dropper");
      addInfo(registration, ModBlocks.FACTORY_PLACER.get(), "factory_placer");
      addInfo(registration, ModBlocks.SMALL_BLOCK_BREAKER.get(), "small_block_breaker");
      addInfo(registration, ModBlocks.SMALL_TREE_CUTTER.get(), "small_tree_cutter");
   }

   @Override
   public void registerGuiHandlers(IGuiHandlerRegistration registration) {
      registration.addRecipeClickArea(MachineScreen.SmallLabFurnaceScreen.class, 78, 37, 22, 18, RecipeTypes.SMELTING);
      registration.addRecipeClickArea(MachineScreen.SmallElectricalFurnaceScreen.class, 78, 28, 22, 18, RecipeTypes.SMELTING);
   }

   private static void addInfo(IRecipeRegistration registration, ItemLike item, String name) {
      registration.addIngredientInfo(new ItemStack(item), VanillaTypes.ITEM_STACK, Component.translatable(INFO_PREFIX + name));
   }
}
