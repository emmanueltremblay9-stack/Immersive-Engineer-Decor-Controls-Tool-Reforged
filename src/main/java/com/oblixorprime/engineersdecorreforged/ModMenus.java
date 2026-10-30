package com.oblixorprime.engineersdecorreforged;

import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.menu.MaterialBoxMenu;
import com.oblixorprime.engineersdecorreforged.menu.MusliBarPressMenu;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.minecraft.world.InteractionHand;

public final class ModMenus {
   public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, "immersive_engineer_decor_controls_tool_reforged");
   private static final Map<MachineKind, DeferredHolder<MenuType<?>, MenuType<MachineMenu>>> MACHINE_TYPES = new EnumMap<>(MachineKind.class);
   public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> METAL_CRAFTING_TABLE = registerMachine(
      "metal_crafting_table_menu", MachineKind.METAL_CRAFTING_TABLE
   );
   public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> LABELED_CRATE = registerMachine("labeled_crate_menu", MachineKind.LABELED_CRATE);
   public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> FACTORY_HOPPER = registerMachine("factory_hopper_menu", MachineKind.FACTORY_HOPPER);
   public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> FACTORY_DROPPER = registerMachine("factory_dropper_menu", MachineKind.FACTORY_DROPPER);
   public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> FACTORY_PLACER = registerMachine("factory_placer_menu", MachineKind.FACTORY_PLACER);
   public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> SMALL_BLOCK_BREAKER = registerMachine(
      "small_block_breaker_menu", MachineKind.SMALL_BLOCK_BREAKER
   );
   public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> SMALL_WASTE_INCINERATOR = registerMachine(
      "small_waste_incinerator_menu", MachineKind.SMALL_WASTE_INCINERATOR
   );
   public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> SMALL_LAB_FURNACE = registerMachine(
      "small_lab_furnace_menu", MachineKind.SMALL_LAB_FURNACE
   );
   public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> SMALL_ELECTRICAL_FURNACE = registerMachine(
      "small_electrical_furnace_menu", MachineKind.SMALL_ELECTRICAL_FURNACE
   );
   public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> SMALL_MINERAL_SMELTER = registerMachine(
      "small_mineral_smelter_menu", MachineKind.SMALL_MINERAL_SMELTER
   );
   public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> SMALL_FREEZER = registerMachine("small_freezer_menu", MachineKind.SMALL_FREEZER);
   public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> FLUID_BARREL = registerMachine("fluid_barrel_menu", MachineKind.FLUID_BARREL);
   public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> SMALL_FLUID_FUNNEL = registerMachine(
      "small_fluid_funnel_menu", MachineKind.SMALL_FLUID_FUNNEL
   );
   public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> PASSIVE_FLUID_ACCUMULATOR = registerMachine(
      "passive_fluid_accumulator_menu", MachineKind.PASSIVE_FLUID_ACCUMULATOR
   );
   public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> SMALL_SOLAR_PANEL = registerMachine(
      "small_solar_panel_menu", MachineKind.SMALL_SOLAR_PANEL
   );
   public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> SMALL_MILKING_MACHINE = registerMachine(
      "small_milking_machine_menu", MachineKind.SMALL_MILKING_MACHINE
   );
   public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> SMALL_TREE_CUTTER = registerMachine(
      "small_tree_cutter_menu", MachineKind.SMALL_TREE_CUTTER
   );
   public static final DeferredHolder<MenuType<?>, MenuType<MaterialBoxMenu>> MATERIAL_BOX = MENU_TYPES.register(
      "material_box_menu",
      () -> IMenuTypeExtension.create(
         (id, inventory, data) -> new MaterialBoxMenu(
            id,
            inventory,
            data != null && data.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND
         )
      )
   );
   public static final DeferredHolder<MenuType<?>, MenuType<MusliBarPressMenu>> MUSLI_BAR_PRESS = MENU_TYPES.register(
      "musli_bar_press_menu",
      () -> IMenuTypeExtension.create(
         (id, inventory, data) -> new MusliBarPressMenu(
            id,
            inventory,
            data != null && data.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND
         )
      )
   );

   private ModMenus() {
   }

   public static DeferredHolder<MenuType<?>, MenuType<MachineMenu>> typeFor(MachineKind kind) {
      return MACHINE_TYPES.get(kind);
   }

   public static Map<MachineKind, DeferredHolder<MenuType<?>, MenuType<MachineMenu>>> machineTypes() {
      return Collections.unmodifiableMap(MACHINE_TYPES);
   }

   private static DeferredHolder<MenuType<?>, MenuType<MachineMenu>> registerMachine(String name, MachineKind kind) {
      DeferredHolder<MenuType<?>, MenuType<MachineMenu>> holder = MENU_TYPES.register(
         name, () -> new MenuType((id, inventory) -> MachineMenu.client(id, inventory, kind), FeatureFlags.DEFAULT_FLAGS)
      );
      MACHINE_TYPES.put(kind, holder);
      return holder;
   }
}
