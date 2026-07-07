package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlockEntities;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.ModItems;
import com.oblixorprime.engineersdecorreforged.ModMenus;
import com.oblixorprime.engineersdecorreforged.ModRecipeSerializers;
import com.oblixorprime.engineersdecorreforged.ReforgedConfig;
import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.network.LabeledCrateLabelPayload;
import com.oblixorprime.engineersdecorreforged.rsgauges.ControlsModule;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlocks;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import com.oblixorprime.engineersdecorreforged.utility.MachineLayout;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.registries.DeferredBlock;

@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class MachineGuiGameTests {
   private static final String TEMPLATE = "empty";
   private static final BlockPos TEST_POS = new BlockPos(1, 1, 1);
   private static final List<MachineGuiGameTests.MachineEntry> MACHINE_ENTRIES = List.of(
      new MachineGuiGameTests.MachineEntry(ModBlocks.METAL_CRAFTING_TABLE, MachineKind.METAL_CRAFTING_TABLE, 11, null),
      new MachineGuiGameTests.MachineEntry(ModBlocks.LABELED_CRATE, MachineKind.LABELED_CRATE, 27, null),
      new MachineGuiGameTests.MachineEntry(ModBlocks.FACTORY_HOPPER, MachineKind.FACTORY_HOPPER, 18, "factory_hopper_gui"),
      new MachineGuiGameTests.MachineEntry(ModBlocks.FACTORY_DROPPER, MachineKind.FACTORY_DROPPER, 15, "factory_dropper_gui"),
      new MachineGuiGameTests.MachineEntry(ModBlocks.FACTORY_PLACER, MachineKind.FACTORY_PLACER, 18, "factory_placer_gui"),
      new MachineGuiGameTests.MachineEntry(ModBlocks.SMALL_BLOCK_BREAKER, MachineKind.SMALL_BLOCK_BREAKER, 10, null),
      new MachineGuiGameTests.MachineEntry(ModBlocks.SMALL_WASTE_INCINERATOR, MachineKind.SMALL_WASTE_INCINERATOR, 16, "small_waste_incinerator_gui"),
      new MachineGuiGameTests.MachineEntry(ModBlocks.SMALL_LAB_FURNACE, MachineKind.SMALL_LAB_FURNACE, 11, "small_lab_furnace_gui"),
      new MachineGuiGameTests.MachineEntry(ModBlocks.SMALL_ELECTRICAL_FURNACE, MachineKind.SMALL_ELECTRICAL_FURNACE, 7, "small_electrical_furnace_gui"),
      new MachineGuiGameTests.MachineEntry(ModBlocks.SMALL_MINERAL_SMELTER, MachineKind.SMALL_MINERAL_SMELTER, 3, null),
      new MachineGuiGameTests.MachineEntry(ModBlocks.SMALL_FREEZER, MachineKind.SMALL_FREEZER, 3, null),
      new MachineGuiGameTests.MachineEntry(ModBlocks.FLUID_BARREL, MachineKind.FLUID_BARREL, 2, null),
      new MachineGuiGameTests.MachineEntry(ModBlocks.SMALL_FLUID_FUNNEL, MachineKind.SMALL_FLUID_FUNNEL, 2, null),
      new MachineGuiGameTests.MachineEntry(ModBlocks.PASSIVE_FLUID_ACCUMULATOR, MachineKind.PASSIVE_FLUID_ACCUMULATOR, 2, null),
      new MachineGuiGameTests.MachineEntry(ModBlocks.SMALL_SOLAR_PANEL, MachineKind.SMALL_SOLAR_PANEL, 1, null),
      new MachineGuiGameTests.MachineEntry(ModBlocks.SMALL_MILKING_MACHINE, MachineKind.SMALL_MILKING_MACHINE, 2, null),
      new MachineGuiGameTests.MachineEntry(ModBlocks.SMALL_TREE_CUTTER, MachineKind.SMALL_TREE_CUTTER, 10, null)
   );

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void legacy_registry_ids_alias_to_renamed_mod_namespace(GameTestHelper helper) {
      ResourceLocation legacyCrate = legacyId("labeled_crate");
      helper.assertTrue(
         BuiltInRegistries.BLOCK.get(legacyCrate) == ModBlocks.LABELED_CRATE.get(), "old labeled crate block id should alias to the renamed mod id"
      );
      helper.assertTrue(
         BuiltInRegistries.ITEM.get(legacyCrate) == ((Block)ModBlocks.LABELED_CRATE.get()).asItem(),
         "old labeled crate item id should alias to the renamed mod id"
      );
      helper.assertTrue(
         BuiltInRegistries.ITEM.get(legacyId("redia_tool")) == ModItems.ORDERED_ITEMS.stream()
            .filter(item -> item.getId().getPath().equals("redia_tool"))
            .findFirst()
            .orElseThrow()
            .get(),
         "old REDIA Tool item id should alias to the renamed mod id"
      );
      helper.assertTrue(
         BuiltInRegistries.BLOCK.get(legacyId("industrial_lever")) == BuiltInRegistries.BLOCK.get(currentId("industrial_lever")),
         "old industrial lever block id should alias after controls initialize"
      );
      helper.assertTrue(
         BuiltInRegistries.ITEM.get(legacyId("industrial_lever")) == BuiltInRegistries.ITEM.get(currentId("industrial_lever")),
         "old industrial lever item id should alias after controls initialize"
      );
      helper.assertTrue(
         BuiltInRegistries.BLOCK_ENTITY_TYPE.get(legacyId("machine")) == ModBlockEntities.MACHINE.get(),
         "old machine block entity id should alias to the renamed mod id"
      );
      helper.assertTrue(
         BuiltInRegistries.MENU.get(legacyId("labeled_crate_menu")) == ModMenus.LABELED_CRATE.get(),
         "old labeled crate menu id should alias to the renamed mod id"
      );
      helper.assertTrue(
         BuiltInRegistries.RECIPE_SERIALIZER.get(legacyId("redia_tool_repair")) == ModRecipeSerializers.REDIA_TOOL_REPAIR.get(),
         "old REDIA repair recipe serializer id should alias to the renamed mod id"
      );
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void feature_config_defaults_keep_registry_content_stable(GameTestHelper helper) {
      assertDefaultTrue(helper, ReforgedConfig.ENABLE_DECOR_BLOCKS, "decor block visibility");
      assertDefaultTrue(helper, ReforgedConfig.ENABLE_UTILITY_BLOCKS, "utility block visibility");
      assertDefaultTrue(helper, ReforgedConfig.ENABLE_REDSTONE_CONTROLS, "redstone control visibility");
      assertDefaultTrue(helper, ReforgedConfig.ENABLE_GAUGES, "gauge visibility");
      assertDefaultTrue(helper, ReforgedConfig.ENABLE_INDICATORS, "indicator visibility");
      assertDefaultTrue(helper, ReforgedConfig.ENABLE_SENSORS, "sensor visibility");
      assertDefaultTrue(helper, ReforgedConfig.ENABLE_WIRELESS_CONTROLS, "wireless control visibility");
      assertDefaultTrue(helper, ReforgedConfig.ENABLE_SOUND_INDICATORS, "sound indicator visibility");
      assertDefaultTrue(helper, ReforgedConfig.ENABLE_STYLE_INDUSTRIAL, "industrial style visibility");
      assertDefaultTrue(helper, ReforgedConfig.ENABLE_STYLE_RETRO_INDUSTRIAL, "retro industrial style visibility");
      assertDefaultTrue(helper, ReforgedConfig.ENABLE_STYLE_RUSTIC, "rustic style visibility");
      assertDefaultTrue(helper, ReforgedConfig.ENABLE_STYLE_OLD_FANCY, "old fancy style visibility");
      assertDefaultTrue(helper, ReforgedConfig.ENABLE_STYLE_GLASS, "glass style visibility");
      helper.assertValueEqual(MachineKind.values().length, ModMenus.machineTypes().size(), "utility menus should stay registered for world compatibility");
      for (MachineKind kind : MachineKind.values()) {
         helper.assertTrue(ModMenus.typeFor(kind) != null, kind.registryName() + " menu should stay registered");
      }
      helper.assertValueEqual(
         ControlsModule.configuredControlCount(), ControlsModule.CONTROLS.size(), "redstone controls should stay registered regardless of visibility config"
      );
      helper.assertTrue(ReforgedConfig.isUtilityBlockItem("metal_crafting_table"), "metal crafting table should be classified as utility content");
      helper.assertTrue(ControlsModule.isControlContent("industrial_lever"), "industrial lever should be classified as redstone control content");
      helper.assertTrue(ControlsModule.isControlContent("switchlink_pearl"), "switchlink pearl should follow wireless control visibility");
      helper.succeed();
   }

   private static ResourceLocation currentId(String path) {
      return ResourceLocation.fromNamespaceAndPath(EngineersDecorReforged.MOD_ID, path);
   }

   private static ResourceLocation legacyId(String path) {
      return ResourceLocation.fromNamespaceAndPath(EngineersDecorReforged.LEGACY_MOD_ID, path);
   }

   private static void assertDefaultTrue(GameTestHelper helper, net.neoforged.neoforge.common.ModConfigSpec.BooleanValue value, String name) {
      helper.assertTrue(value.getDefault(), name + " config default should be true");
   }

   private MachineGuiGameTests() {
   }

   @GameTest(template = "empty", timeoutTicks = 120)
   public static void all_machine_blocks_open_machine_menu(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);

      for (MachineGuiGameTests.MachineEntry entry : MACHINE_ENTRIES) {
         MachineBlockEntity machine = placeMachine(helper, entry.block(), entry.kind());
         MachineMenu menu = new MachineMenu(entry.kind(), 0, player.getInventory(), machine, new SimpleContainerData(16));
         helper.assertValueEqual(entry.kind(), menu.kind(), entry.kind().registryName() + " menu should sync the matching machine kind");
         helper.assertValueEqual(
            (MenuType)ModMenus.typeFor(entry.kind()).get(), menu.getType(), entry.kind().registryName() + " should use its dedicated machine menu type"
         );
         helper.assertValueEqual(
            entry.expectedSlots(), menu.visibleMachineSlots(), entry.kind().registryName() + " should expose the parent machine slot count"
         );
         helper.assertValueEqual(
            menu.visibleMachineSlots() + 36, menu.slots.size(), entry.kind().registryName() + " should expose its machine-specific slots plus player inventory"
         );
         if (entry.textureName() != null) {
            helper.assertValueEqual(
               MachineLayout.LayoutStyle.ORIGINAL_TEXTURE, menu.layout().style(), entry.kind().registryName() + " should use the parent GUI texture renderer"
            );
            helper.assertValueEqual(
               entry.textureName(), menu.layout().textureName(), entry.kind().registryName() + " should use the supplied parent GUI texture"
            );
         }

         menu.removed(player);
         if (entry.kind() == MachineKind.LABELED_CRATE) {
            assertLabeledCrateLabelBehavior(helper, machine);
         }
      }

      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void labeled_crate_label_payload_accepts_supplementary_characters(GameTestHelper helper) {
      String supplementaryCharacter = new String(Character.toChars(0x1F4A1));
      String fullLine = supplementaryCharacter.repeat(MachineBlockEntity.LABEL_MAX_LINE_LENGTH);
      LabeledCrateLabelPayload payload = new LabeledCrateLabelPayload(helper.absolutePos(TEST_POS), fullLine, fullLine);
      RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());

      try {
         LabeledCrateLabelPayload.STREAM_CODEC.encode(buffer, payload);
         LabeledCrateLabelPayload decoded = LabeledCrateLabelPayload.STREAM_CODEC.decode(buffer);
         helper.assertValueEqual(fullLine, decoded.line0(), "labeled crate label packet should round-trip supplementary characters in line 0");
         helper.assertValueEqual(fullLine, decoded.line1(), "labeled crate label packet should round-trip supplementary characters in line 1");
      } finally {
         buffer.release();
      }

      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void labeled_crate_label_payload_strips_unicode_controls(GameTestHelper helper) {
      String rawLine = "A\u0000B\u001FC\u007FD\u0085E\u009FF";
      String sanitizedLine = "ABCDEF";
      LabeledCrateLabelPayload payload = new LabeledCrateLabelPayload(helper.absolutePos(TEST_POS), rawLine, rawLine);
      RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());

      try {
         helper.assertValueEqual(sanitizedLine, MachineBlockEntity.sanitizeLabelLine(rawLine), "labeled crate labels should strip all control characters");
         LabeledCrateLabelPayload.STREAM_CODEC.encode(buffer, payload);
         LabeledCrateLabelPayload decoded = LabeledCrateLabelPayload.STREAM_CODEC.decode(buffer);
         helper.assertValueEqual(sanitizedLine, decoded.line0(), "labeled crate label packet should sanitize controls in line 0");
         helper.assertValueEqual(sanitizedLine, decoded.line1(), "labeled crate label packet should sanitize controls in line 1");
      } finally {
         buffer.release();
      }

      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void metal_crafting_table_opens_plate_workstation_menu(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      helper.setBlock(TEST_POS, ((MachineBlocks.MetalCraftingTableBlock)ModBlocks.METAL_CRAFTING_TABLE.get()).defaultBlockState());
      MenuProvider provider = helper.getBlockState(TEST_POS).getMenuProvider(helper.getLevel(), helper.absolutePos(TEST_POS));
      helper.assertTrue(provider != null, "metal crafting table should provide a plate workstation menu");
      AbstractContainerMenu menu = provider.createMenu(0, player.getInventory(), player);
      helper.assertTrue(menu instanceof MachineMenu, "metal crafting table should open its machine workstation menu");
      if (menu instanceof MachineMenu machineMenu) {
         helper.assertValueEqual(MachineKind.METAL_CRAFTING_TABLE, machineMenu.kind(), "metal crafting table should sync the plate workstation kind");
         helper.assertValueEqual(11, machineMenu.visibleMachineSlots(), "metal crafting table should expose hammer, 3x3 grid, and output slots");
      }

      if (menu != null) {
         menu.removed(player);
      }

      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void metal_crafting_table_forms_ie_plates_with_hammer_damage(GameTestHelper helper) {
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.METAL_CRAFTING_TABLE, MachineKind.METAL_CRAFTING_TABLE);
      Item hammer = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("immersiveengineering", "hammer"));
      Item ironPlate = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("immersiveengineering", "plate_iron"));
      helper.assertTrue(hammer != Items.AIR, "IE hammer item should be registered");
      helper.assertTrue(ironPlate != Items.AIR, "IE iron plate item should be registered");
      machine.setItem(0, new ItemStack(hammer));
      machine.setItem(1, new ItemStack(Items.IRON_INGOT));
      helper.succeedWhen(() -> {
         tickMachine(helper, machine, 5);
         helper.assertTrue(machine.getItem(10).is(ironPlate), "metal crafting table should preview the verified IE iron plate in the output");
         helper.assertTrue(machine.getItem(1).is(Items.IRON_INGOT), "metal crafting table should not consume the plate material until the output is taken");
         helper.assertValueEqual(0, machine.getItem(0).getDamageValue(), "metal crafting table should not damage the hammer until the output is taken");
         machine.consumeMetalCraftingResult();
         helper.assertTrue(machine.getItem(10).isEmpty(), "metal crafting table output should clear after taking the plate when no material remains");
         helper.assertTrue(machine.getItem(1).isEmpty(), "metal crafting table should consume one grid material when the plate is taken");
         helper.assertValueEqual(1, machine.getItem(0).getDamageValue(), "metal crafting table should damage the hammer by one use when the plate is taken");
      });
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void metal_crafting_table_grid_accepts_normal_ingredients(GameTestHelper helper) {
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.METAL_CRAFTING_TABLE, MachineKind.METAL_CRAFTING_TABLE);
      IItemHandler handler = machine.itemHandler(Direction.UP);
      helper.assertTrue(handler != null, "metal crafting table should expose an item handler capability");
      ItemStack rejectedJunk = handler.insertItem(0, new ItemStack(Items.COBBLESTONE), false);
      helper.assertValueEqual(1, rejectedJunk.getCount(), "hammer slot should reject non-hammer items");
      helper.assertTrue(machine.getItem(0).isEmpty(), "hammer slot should stay empty after rejecting non-hammer items");
      ItemStack acceptedIngredient = handler.insertItem(1, new ItemStack(Items.COBBLESTONE), false);
      helper.assertTrue(acceptedIngredient.isEmpty(), "crafting grid should accept normal crafting ingredients");
      helper.assertTrue(machine.getItem(1).is(Items.COBBLESTONE), "crafting grid should store normal crafting ingredients");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void metal_crafting_table_shift_click_reaches_last_grid_slot(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.METAL_CRAFTING_TABLE, MachineKind.METAL_CRAFTING_TABLE);
      MachineMenu menu = new MachineMenu(MachineKind.METAL_CRAFTING_TABLE, 8, player.getInventory(), machine, machine.dataAccessForTests());

      for (int slot = 1; slot < 9; slot++) {
         machine.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
      }

      player.getInventory().setItem(9, new ItemStack(Items.DIRT));
      ItemStack moved = menu.quickMoveStack(player, menu.visibleMachineSlots());
      helper.assertTrue(moved.is(Items.DIRT), "shift-click should move the player ingredient stack");
      helper.assertTrue(machine.getItem(9).is(Items.DIRT), "shift-click should reach the last backing grid slot");
      helper.assertTrue(machine.getItem(10).isEmpty(), "shift-click should not target the virtual output slot");
      helper.assertTrue(player.getInventory().getItem(9).isEmpty(), "player inventory source slot should be emptied");
      menu.removed(player);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void metal_crafting_table_shift_click_output_requires_full_player_space(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.METAL_CRAFTING_TABLE, MachineKind.METAL_CRAFTING_TABLE);
      MachineMenu menu = new MachineMenu(MachineKind.METAL_CRAFTING_TABLE, 9, player.getInventory(), machine, machine.dataAccessForTests());
      machine.setItem(1, new ItemStack(Items.OAK_LOG));
      tickMachine(helper, machine, 5);
      helper.assertValueEqual(4, machine.getItem(10).getCount(), "metal crafting table should preview four planks from one log");

      for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
         player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
      }

      player.getInventory().setItem(9, new ItemStack(Items.OAK_PLANKS, 61));
      ItemStack refused = menu.quickMoveStack(player, 1);
      helper.assertTrue(refused.isEmpty(), "shift-click should refuse virtual output when only part of the result fits");
      helper.assertTrue(machine.getItem(1).is(Items.OAK_LOG), "refused shift-click should not consume the crafting input");
      helper.assertValueEqual(4, machine.getItem(10).getCount(), "refused shift-click should keep the full virtual output preview");
      helper.assertValueEqual(61, player.getInventory().getItem(9).getCount(), "refused shift-click should not partially fill the player stack");

      player.getInventory().setItem(10, ItemStack.EMPTY);
      ItemStack moved = menu.quickMoveStack(player, 1);
      helper.assertTrue(moved.is(Items.OAK_PLANKS), "shift-click should move the virtual output once the full result fits");
      helper.assertTrue(machine.getItem(1).isEmpty(), "successful shift-click should consume the crafting input");
      helper.assertValueEqual(65, countItem(player.getInventory(), Items.OAK_PLANKS), "successful shift-click should deliver every crafted plank");
      menu.removed(player);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void shift_click_does_not_merge_player_items_into_output_slots(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.SMALL_MINERAL_SMELTER, MachineKind.SMALL_MINERAL_SMELTER);
      MachineMenu menu = new MachineMenu(MachineKind.SMALL_MINERAL_SMELTER, 10, player.getInventory(), machine, machine.dataAccessForTests());
      machine.setItem(2, new ItemStack(Items.MAGMA_BLOCK));
      player.getInventory().setItem(9, new ItemStack(Items.MAGMA_BLOCK, 5));
      ItemStack refused = menu.quickMoveStack(player, menu.visibleMachineSlots());
      helper.assertTrue(refused.isEmpty(), "shift-click should refuse player items that only match an output slot");
      helper.assertValueEqual(1, machine.getItem(2).getCount(), "shift-click should not merge player items into output-only machine slots");
      helper.assertValueEqual(5, player.getInventory().getItem(9).getCount(), "refused shift-click should keep the player stack untouched");
      menu.removed(player);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void machine_menu_quick_move_rejects_invalid_slot_indices(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.FACTORY_HOPPER, MachineKind.FACTORY_HOPPER);
      MachineMenu menu = new MachineMenu(MachineKind.FACTORY_HOPPER, 11, player.getInventory(), machine, machine.dataAccessForTests());
      helper.assertTrue(menu.quickMoveStack(player, -1).isEmpty(), "quick move should ignore negative slot indices");
      helper.assertTrue(menu.quickMoveStack(player, menu.slots.size()).isEmpty(), "quick move should ignore slot indices just past the menu");
      helper.assertTrue(menu.quickMoveStack(player, menu.slots.size() + 100).isEmpty(), "quick move should ignore far out-of-range slot indices");
      menu.removed(player);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void metal_crafting_table_crafts_vanilla_grid_recipes(GameTestHelper helper) {
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.METAL_CRAFTING_TABLE, MachineKind.METAL_CRAFTING_TABLE);
      machine.setItem(1, new ItemStack(Items.OAK_PLANKS));
      machine.setItem(2, new ItemStack(Items.OAK_PLANKS));
      machine.setItem(4, new ItemStack(Items.OAK_PLANKS));
      machine.setItem(5, new ItemStack(Items.OAK_PLANKS));
      helper.succeedWhen(() -> {
         tickMachine(helper, machine, 5);
         helper.assertTrue(machine.getItem(10).is(Items.CRAFTING_TABLE), "metal crafting table should preview vanilla 3x3 crafting results");
         machine.consumeMetalCraftingResult();
         helper.assertTrue(machine.getItem(10).isEmpty(), "vanilla crafting output should clear after the result is taken");
         helper.assertTrue(machine.getItem(1).isEmpty(), "vanilla crafting should consume grid slot 1");
         helper.assertTrue(machine.getItem(2).isEmpty(), "vanilla crafting should consume grid slot 2");
         helper.assertTrue(machine.getItem(4).isEmpty(), "vanilla crafting should consume grid slot 4");
         helper.assertTrue(machine.getItem(5).isEmpty(), "vanilla crafting should consume grid slot 5");
      });
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void machine_crafting_recipes_are_registered(GameTestHelper helper) {
      for (MachineGuiGameTests.MachineEntry entry : MACHINE_ENTRIES) {
         assertRecipe(helper, entry.kind().registryName());
      }

      assertRecipe(helper, "treated_wood_table");
      assertRecipe(helper, "metal_crafting_table");
      assertRecipe(helper, "straight_pipe_valve");
      assertRecipe(helper, "straight_pipe_valve_redstone");
      assertRecipe(helper, "straight_pipe_valve_redstone_analog");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 260)
   public static void small_lab_furnace_smelts_from_parent_input_and_fuel_slots(GameTestHelper helper) {
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.SMALL_LAB_FURNACE, MachineKind.SMALL_LAB_FURNACE);
      machine.setItem(0, new ItemStack(Items.COBBLESTONE));
      machine.setItem(1, new ItemStack(Items.COAL));
      helper.succeedWhen(() -> {
         helper.assertTrue(machine.getItem(2).is(Items.STONE), "small lab furnace should smelt into the parent output slot");
         helper.assertTrue(machine.getItem(0).isEmpty(), "small lab furnace should consume one input item");
      });
   }

   @GameTest(template = "empty", timeoutTicks = 120)
   public static void small_lab_furnace_external_heater_boost_uses_fe(GameTestHelper helper) {
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.SMALL_LAB_FURNACE, MachineKind.SMALL_LAB_FURNACE);
      Item heater = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("immersiveengineering", "furnace_heater"));
      helper.assertTrue(heater != Items.AIR, "IE external heater item should be registered as furnace_heater");
      helper.assertTrue(machine.energyStorage(null) != null, "small lab furnace should expose FE input for external heater boost");
      machine.setItem(0, new ItemStack(Items.COBBLESTONE));
      machine.setItem(1, new ItemStack(Items.COAL));
      machine.setItem(9, new ItemStack(heater));
      machine.energyStorage(null).receiveEnergy(1000, false);
      tickMachine(helper, machine, 90);
      helper.assertTrue(machine.getItem(2).is(Items.STONE), "small lab furnace external heater boost should speed up smelting");
      helper.assertTrue(machine.energyStorage(null).getEnergyStored() < 1000, "small lab furnace external heater boost should consume FE");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 180)
   public static void small_electrical_furnace_uses_fe_and_parent_slots(GameTestHelper helper) {
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.SMALL_ELECTRICAL_FURNACE, MachineKind.SMALL_ELECTRICAL_FURNACE);
      machine.energyStorage(null).receiveEnergy(8000, false);
      machine.setItem(0, new ItemStack(Items.COBBLESTONE));
      helper.succeedWhen(() -> {
         helper.assertTrue(machine.getItem(2).is(Items.STONE), "small electrical furnace should smelt into the parent output slot");
         helper.assertTrue(machine.energyStorage(null).getEnergyStored() < 8000, "small electrical furnace should consume FE while working");
      });
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void small_electrical_furnace_clears_power_consumption_when_idle(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.SMALL_ELECTRICAL_FURNACE, MachineKind.SMALL_ELECTRICAL_FURNACE);
      MachineMenu menu = new MachineMenu(MachineKind.SMALL_ELECTRICAL_FURNACE, 1, player.getInventory(), machine, machine.dataAccessForTests());
      machine.energyStorage(null).receiveEnergy(16, false);
      machine.setItem(0, new ItemStack(Items.COBBLESTONE));
      tickMachine(helper, machine, 1);
      helper.assertValueEqual(16, menu.field(7), "small electrical furnace should report FE/t while it consumes energy");
      tickMachine(helper, machine, 1);
      helper.assertValueEqual(0, menu.field(7), "small electrical furnace should clear FE/t once it stops consuming energy");
      menu.removed(player);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void small_electrical_furnace_clamps_synced_power_consumption(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.SMALL_ELECTRICAL_FURNACE, MachineKind.SMALL_ELECTRICAL_FURNACE);
      MachineMenu menu = new MachineMenu(MachineKind.SMALL_ELECTRICAL_FURNACE, 1, player.getInventory(), machine, machine.dataAccessForTests());
      machine.dataAccessForTests().set(7, -40);
      helper.assertValueEqual(0, menu.field(7), "small electrical furnace should reject negative synced FE/t values");
      machine.dataAccessForTests().set(7, 999);
      helper.assertValueEqual(64, menu.field(7), "small electrical furnace should cap synced FE/t at its fastest valid consumption");
      menu.removed(player);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void small_electrical_furnace_hopper_aux_feeds_input_and_output_sides(GameTestHelper helper) {
      helper.setBlock(TEST_POS, Blocks.AIR);
      helper.setBlock(TEST_POS.west(), Blocks.CHEST.defaultBlockState());
      helper.setBlock(TEST_POS.east(), Blocks.CHEST.defaultBlockState());
      helper.setBlock(
         TEST_POS,
         (BlockState)((MachineBlocks.LitHorizontalMachineBlock)ModBlocks.SMALL_ELECTRICAL_FURNACE.get())
            .defaultBlockState()
            .setValue(MachineBlocks.HORIZONTAL_FACING, Direction.EAST)
      );
      BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(TEST_POS));
      BlockEntity inputEntity = helper.getLevel().getBlockEntity(helper.absolutePos(TEST_POS.west()));
      BlockEntity outputEntity = helper.getLevel().getBlockEntity(helper.absolutePos(TEST_POS.east()));
      helper.assertTrue(blockEntity instanceof MachineBlockEntity, "small electrical furnace should create a machine block entity");
      helper.assertTrue(inputEntity instanceof Container, "input-side chest should expose a vanilla container");
      helper.assertTrue(outputEntity instanceof Container, "output-side chest should expose a vanilla container");
      MachineBlockEntity machine = (MachineBlockEntity)blockEntity;
      Container input = (Container)inputEntity;
      Container output = (Container)outputEntity;
      input.setItem(0, new ItemStack(Items.APPLE, 3));
      input.setItem(1, new ItemStack(Items.COBBLESTONE, 4));
      machine.setItem(1, new ItemStack(Items.HOPPER));
      machine.setItem(2, new ItemStack(Items.STONE, 2));
      machine.energyStorage(null).receiveEnergy(1000, false);
      tickMachine(helper, machine, 20);
      helper.assertValueEqual(3, input.getItem(0).getCount(), "small electrical furnace feeder should skip non-smeltable input items");
      helper.assertTrue(input.getItem(1).isEmpty(), "small electrical furnace feeder should pull smeltable input from the input side");
      helper.assertValueEqual(
         4, countItems(machine, Items.COBBLESTONE, 0, 3, 4), "small electrical furnace feeder should queue pulled smeltable input in furnace input slots"
      );
      helper.assertTrue(
         machine.getItem(2).isEmpty() && machine.getItem(5).isEmpty() && machine.getItem(6).isEmpty(),
         "small electrical furnace feeder should move output-side results out of the furnace buffer"
      );
      helper.assertTrue(output.getItem(0).is(Items.STONE), "small electrical furnace feeder should push output to the output-side inventory");
      helper.assertValueEqual(2, output.getItem(0).getCount(), "small electrical furnace feeder should preserve the pushed output count");
      helper.assertTrue(machine.energyStorage(null).getEnergyStored() < 1000, "small electrical furnace feeder should consume FE when moving items");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void machine_internal_transfer_respects_item_stack_limit_into_empty_slot(GameTestHelper helper) {
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.SMALL_ELECTRICAL_FURNACE, MachineKind.SMALL_ELECTRICAL_FURNACE);
      machine.setItem(2, new ItemStack(Items.ENDER_PEARL, 16));
      CompoundTag saved = machine.saveWithFullMetadata(helper.getLevel().registryAccess());
      ListTag items = saved.getList("Items", Tag.TAG_COMPOUND);
      helper.assertValueEqual(items.size(), 1, "test setup should save the electrical furnace output stack");
      items.getCompound(0).putInt("count", 32);
      machine.loadWithComponents(saved, helper.getLevel().registryAccess());
      helper.assertValueEqual(machine.getItem(2).getCount(), 32, "test setup should reload an over-limit stack before transfer");
      tickMachine(helper, machine, 20);
      helper.assertValueEqual(machine.getItem(5).getCount(), 16, "internal transfer should cap empty destination slots to the item stack limit");
      helper.assertValueEqual(machine.getItem(2).getCount(), 16, "internal transfer should leave over-limit remainder in the source slot");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void small_waste_incinerator_speedup_uses_fe_not_redstone(GameTestHelper helper) {
      helper.setBlock(TEST_POS.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
      MachineBlockEntity redstonePowered = placeMachine(helper, ModBlocks.SMALL_WASTE_INCINERATOR, MachineKind.SMALL_WASTE_INCINERATOR);
      redstonePowered.setItem(15, new ItemStack(Items.ROTTEN_FLESH, 2));
      tickMachine(helper, redstonePowered, 20);
      helper.assertValueEqual(redstonePowered.getItem(15).getCount(), 2, "small waste incinerator redstone power alone should not use the FE speed-up interval");
      BlockPos feTestPos = TEST_POS.offset(3, 0, 0);
      MachineBlockEntity fePowered = placeMachineAt(helper, feTestPos, ModBlocks.SMALL_WASTE_INCINERATOR, MachineKind.SMALL_WASTE_INCINERATOR);
      fePowered.setItem(15, new ItemStack(Items.ROTTEN_FLESH, 2));
      fePowered.energyStorage(null).receiveEnergy(8, false);
      tickMachine(helper, fePowered, 10);
      helper.assertValueEqual(fePowered.getItem(15).getCount(), 1, "small waste incinerator FE should use the faster burn interval");
      helper.assertValueEqual(fePowered.energyStorage(null).getEnergyStored(), 0, "small waste incinerator FE speed-up should consume stored FE");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void small_waste_incinerator_accepts_last_queue_slot(GameTestHelper helper) {
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.SMALL_WASTE_INCINERATOR, MachineKind.SMALL_WASTE_INCINERATOR);
      IItemHandler itemHandler = machine.itemHandler(Direction.UP);
      ItemStack waste = new ItemStack(Items.ROTTEN_FLESH);
      helper.assertTrue(machine.canPlaceItem(15, waste), "small waste incinerator should accept items in its visible last queue slot");
      helper.assertTrue(itemHandler.insertItem(15, waste.copy(), false).isEmpty(), "small waste incinerator automation should insert into slot 15");
      helper.assertValueEqual(1, machine.getItem(15).getCount(), "small waste incinerator slot 15 should receive inserted waste");
      tickMachine(helper, machine, 30);
      helper.assertTrue(machine.getItem(15).isEmpty(), "small waste incinerator should burn waste from slot 15");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void small_solar_panel_charges_energy_items_in_cell_slot(GameTestHelper helper) {
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.SMALL_SOLAR_PANEL, MachineKind.SMALL_SOLAR_PANEL);
      IItemHandler itemHandler = machine.itemHandler(Direction.UP);
      helper.assertTrue(itemHandler != null, "small solar panel should expose an item handler capability");
      ItemStack blocked = itemHandler.insertItem(0, new ItemStack(Items.COBBLESTONE), false);
      helper.assertValueEqual(1, blocked.getCount(), "small solar panel cell slot should reject non-energy items");
      helper.assertTrue(machine.getItem(0).isEmpty(), "small solar panel cell slot should stay empty after rejected insert");
      Item capacitor = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("immersiveengineering", "capacitor_lv"));
      helper.assertTrue(capacitor != Items.AIR, "IE LV capacitor item should be registered");
      ItemStack cell = new ItemStack(capacitor);
      IEnergyStorage cellEnergy = (IEnergyStorage)cell.getCapability(EnergyStorage.ITEM);
      helper.assertTrue(cellEnergy != null && cellEnergy.canReceive(), "IE LV capacitor item should expose a chargeable FE capability");
      ItemStack accepted = itemHandler.insertItem(0, cell, false);
      helper.assertTrue(accepted.isEmpty(), "small solar panel cell slot should accept FE-capable items");
      machine.dataAccessForTests().set(3, 10);
      tickMachine(helper, machine, 20);
      IEnergyStorage chargedCell = (IEnergyStorage)machine.getItem(0).getCapability(EnergyStorage.ITEM);
      helper.assertTrue(chargedCell != null && chargedCell.getEnergyStored() > 0, "small solar panel should transfer stored FE into the cell slot item");
      helper.assertTrue(machine.energyStorage(null).getEnergyStored() < 1000, "small solar panel should reduce its internal buffer after charging a cell item");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void small_solar_panel_menu_reports_full_energy_capacity(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.SMALL_SOLAR_PANEL, MachineKind.SMALL_SOLAR_PANEL);
      MachineMenu menu = new MachineMenu(MachineKind.SMALL_SOLAR_PANEL, 1, player.getInventory(), machine, machine.dataAccessForTests());
      machine.dataAccessForTests().set(3, 320);
      helper.assertValueEqual(32000, menu.energyStored(), "small solar panel menu should report stored FE from synced data");
      helper.assertValueEqual(64000, menu.energyCapacity(), "small solar panel menu should expose the full solar FE capacity for client meters");
      menu.removed(player);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void energy_machine_menus_report_full_energy_capacity(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      assertMenuEnergyCapacity(helper, player, TEST_POS, ModBlocks.SMALL_LAB_FURNACE, MachineKind.SMALL_LAB_FURNACE, 32000);
      assertMenuEnergyCapacity(
         helper, player, TEST_POS, ModBlocks.SMALL_ELECTRICAL_FURNACE, MachineKind.SMALL_ELECTRICAL_FURNACE, 32000
      );
      assertMenuEnergyCapacity(helper, player, TEST_POS, ModBlocks.SMALL_WASTE_INCINERATOR, MachineKind.SMALL_WASTE_INCINERATOR, 16000);
      assertMenuEnergyCapacity(helper, player, TEST_POS, ModBlocks.SMALL_MINERAL_SMELTER, MachineKind.SMALL_MINERAL_SMELTER, 16000);
      assertMenuEnergyCapacity(helper, player, TEST_POS, ModBlocks.SMALL_FREEZER, MachineKind.SMALL_FREEZER, 16000);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void energy_machine_menus_report_synced_energy_stored(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      assertMenuEnergyStored(helper, player, TEST_POS, ModBlocks.SMALL_LAB_FURNACE, MachineKind.SMALL_LAB_FURNACE, 12345);
      assertMenuEnergyStored(helper, player, TEST_POS, ModBlocks.SMALL_ELECTRICAL_FURNACE, MachineKind.SMALL_ELECTRICAL_FURNACE, 23456);
      assertMenuEnergyStored(helper, player, TEST_POS, ModBlocks.SMALL_WASTE_INCINERATOR, MachineKind.SMALL_WASTE_INCINERATOR, 12000);
      assertMenuEnergyStored(helper, player, TEST_POS, ModBlocks.SMALL_MINERAL_SMELTER, MachineKind.SMALL_MINERAL_SMELTER, 8000);
      assertMenuEnergyStored(helper, player, TEST_POS, ModBlocks.SMALL_FREEZER, MachineKind.SMALL_FREEZER, 9000);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void machine_menus_report_normalized_progress_percentages(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      assertMenuProgress(helper, player, TEST_POS, ModBlocks.SMALL_LAB_FURNACE, MachineKind.SMALL_LAB_FURNACE, 50, 200, 25);
      assertMenuProgress(
         helper, player, TEST_POS.offset(2, 0, 0), ModBlocks.SMALL_ELECTRICAL_FURNACE, MachineKind.SMALL_ELECTRICAL_FURNACE, 75, 150, 50
      );
      assertMenuProgress(helper, player, TEST_POS.offset(4, 0, 0), ModBlocks.SMALL_MINERAL_SMELTER, MachineKind.SMALL_MINERAL_SMELTER, 90, 180, 50);
      assertMenuProgress(helper, player, TEST_POS.offset(6, 0, 0), ModBlocks.SMALL_FREEZER, MachineKind.SMALL_FREEZER, 45, 180, 25);
      assertMenuProgress(helper, player, TEST_POS.offset(8, 0, 0), ModBlocks.SMALL_BLOCK_BREAKER, MachineKind.SMALL_BLOCK_BREAKER, 20, 80, 25);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void small_solar_panel_updates_comparator_after_fe_extraction(GameTestHelper helper) {
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.SMALL_SOLAR_PANEL, MachineKind.SMALL_SOLAR_PANEL);
      machine.dataAccessForTests().set(3, 640);
      helper.assertValueEqual(15, machine.comparatorOutput(), "full solar panel should report a full comparator signal");
      helper.assertValueEqual(64000, machine.energyStorage(null).extractEnergy(64000, false), "small solar panel should allow direct FE extraction");
      helper.assertValueEqual(0, machine.energyStorage(null).getEnergyStored(), "small solar panel direct extraction should drain the internal buffer");
      helper.assertValueEqual(0, machine.comparatorOutput(), "small solar panel comparator output should clear after direct FE extraction");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void machine_load_clamps_energy_before_capability_io(GameTestHelper helper) {
      MachineBlockEntity source = placeMachine(helper, ModBlocks.SMALL_WASTE_INCINERATOR, MachineKind.SMALL_WASTE_INCINERATOR);
      CompoundTag saved = source.saveWithFullMetadata(helper.getLevel().registryAccess());
      saved.putInt("Energy", 999999);
      MachineBlockEntity loaded = new MachineBlockEntity(source.getBlockPos(), source.getBlockState());
      loaded.loadWithComponents(saved, helper.getLevel().registryAccess());
      IEnergyStorage energyStorage = loaded.energyStorage(null);
      helper.assertTrue(energyStorage != null, "loaded small waste incinerator should expose FE storage");
      helper.assertValueEqual(16000, energyStorage.getEnergyStored(), "loaded small waste incinerator should clamp stored FE to capacity");
      helper.assertValueEqual(0, energyStorage.receiveEnergy(1, false), "full loaded FE storage should not report negative inserted energy");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void machine_load_clamps_negative_runtime_timers(GameTestHelper helper) {
      MachineBlockEntity labFurnace = placeMachine(helper, ModBlocks.SMALL_LAB_FURNACE, MachineKind.SMALL_LAB_FURNACE);
      CompoundTag labSave = labFurnace.saveWithFullMetadata(helper.getLevel().registryAccess());
      labSave.putInt("TickCounter", -20);
      labSave.putInt("FifoTimer", -20);
      labSave.putInt("BurnTicks", -100);
      labSave.putInt("FuelBurnTime", -100);
      MachineBlockEntity loadedLabFurnace = new MachineBlockEntity(labFurnace.getBlockPos(), labFurnace.getBlockState());
      loadedLabFurnace.loadWithComponents(labSave, helper.getLevel().registryAccess());
      helper.assertValueEqual(0, loadedLabFurnace.dataAccessForTests().get(0), "loaded lab furnace should clamp negative burn time");
      helper.assertValueEqual(0, loadedLabFurnace.dataAccessForTests().get(1), "loaded lab furnace should clamp negative fuel time");
      CompoundTag normalizedLabSave = loadedLabFurnace.saveWithFullMetadata(helper.getLevel().registryAccess());
      helper.assertValueEqual(0, normalizedLabSave.getInt("TickCounter"), "loaded machine should clamp negative tick counter before saving again");
      helper.assertValueEqual(0, normalizedLabSave.getInt("FifoTimer"), "loaded machine should clamp negative FIFO timer before saving again");

      MachineBlockEntity hopper = placeMachineAt(helper, TEST_POS.offset(3, 0, 0), ModBlocks.FACTORY_HOPPER, MachineKind.FACTORY_HOPPER);
      CompoundTag hopperSave = hopper.saveWithFullMetadata(helper.getLevel().registryAccess());
      hopperSave.putInt("Cooldown", -10);
      MachineBlockEntity loadedLowCooldown = new MachineBlockEntity(hopper.getBlockPos(), hopper.getBlockState());
      loadedLowCooldown.loadWithComponents(hopperSave, helper.getLevel().registryAccess());
      helper.assertValueEqual(0, loadedLowCooldown.dataAccessForTests().get(4), "loaded factory hopper should clamp negative cooldown");
      hopperSave.putInt("Cooldown", 9999);
      MachineBlockEntity loadedHighCooldown = new MachineBlockEntity(hopper.getBlockPos(), hopper.getBlockState());
      loadedHighCooldown.loadWithComponents(hopperSave, helper.getLevel().registryAccess());
      helper.assertValueEqual(400, loadedHighCooldown.dataAccessForTests().get(4), "loaded factory hopper should clamp oversized cooldown");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void factory_hopper_load_clamps_collection_range(GameTestHelper helper) {
      MachineBlockEntity source = placeMachine(helper, ModBlocks.FACTORY_HOPPER, MachineKind.FACTORY_HOPPER);
      CompoundTag saved = source.saveWithFullMetadata(helper.getLevel().registryAccess());
      saved.putInt("HopperRange", 99);
      MachineBlockEntity high = new MachineBlockEntity(source.getBlockPos(), source.getBlockState());
      high.loadWithComponents(saved, helper.getLevel().registryAccess());
      helper.assertValueEqual(high.dataAccessForTests().get(0), 4, "loaded factory hopper should clamp oversized collection range");
      saved.putInt("HopperRange", -99);
      MachineBlockEntity low = new MachineBlockEntity(source.getBlockPos(), source.getBlockState());
      low.loadWithComponents(saved, helper.getLevel().registryAccess());
      helper.assertValueEqual(low.dataAccessForTests().get(0), 0, "loaded factory hopper should clamp negative collection range");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 160)
   public static void factory_machines_ignore_stale_transient_triggers_after_load(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      helper.setBlock(
         TEST_POS,
         (BlockState)((MachineBlocks.DirectionalMachineBlock)ModBlocks.FACTORY_PLACER.get()).defaultBlockState().setValue(MachineBlocks.FACING, Direction.EAST)
      );
      BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(TEST_POS));
      helper.assertTrue(blockEntity instanceof MachineBlockEntity, "factory placer should create a machine block entity");
      MachineBlockEntity placer = (MachineBlockEntity)blockEntity;
      placer.setItem(0, new ItemStack(Items.DIRT));
      CompoundTag placerSave = placer.saveWithFullMetadata(helper.getLevel().registryAccess());
      placerSave.putInt("PlacerLogic", 0);
      placerSave.putBoolean("ManualTrigger", true);
      placer.loadWithComponents(placerSave, helper.getLevel().registryAccess());
      tickMachine(helper, placer, 12);
      helper.assertTrue(helper.getBlockState(TEST_POS.east()).isAir(), "loaded factory placer should ignore stale manual GUI trigger state");
      helper.assertValueEqual(1, placer.getItem(0).getCount(), "loaded factory placer should keep stock when only a stale trigger was saved");
      MachineMenu placerMenu = new MachineMenu(MachineKind.FACTORY_PLACER, 8, player.getInventory(), placer, placer.dataAccessForTests());
      placerMenu.handleAction(player, 21, 1, 0);
      tickMachine(helper, placer, 12);
      helper.assertTrue(helper.getBlockState(TEST_POS.east()).is(Blocks.DIRT), "factory placer should still respond to a fresh manual GUI trigger");

      BlockPos dropperPos = TEST_POS.offset(3, 0, 0);
      MachineBlockEntity dropper = placeMachineAt(helper, dropperPos, ModBlocks.FACTORY_DROPPER, MachineKind.FACTORY_DROPPER);
      dropper.setItem(0, new ItemStack(Items.COBBLESTONE, 2));
      CompoundTag dropperSave = dropper.saveWithFullMetadata(helper.getLevel().registryAccess());
      dropperSave.putBoolean("ManualRedstoneTrigger", true);
      dropperSave.putBoolean("RedstoneUpdated", true);
      dropper.loadWithComponents(dropperSave, helper.getLevel().registryAccess());
      tickMachine(helper, dropper, 8);
      helper.assertValueEqual(2, dropper.getItem(0).getCount(), "loaded factory dropper should ignore stale GUI redstone trigger state");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void smelting_machine_load_resets_out_of_bounds_progress(GameTestHelper helper) {
      MachineBlockEntity labFurnace = placeMachine(helper, ModBlocks.SMALL_LAB_FURNACE, MachineKind.SMALL_LAB_FURNACE);
      labFurnace.setItem(0, new ItemStack(Items.COBBLESTONE));
      labFurnace.setItem(1, new ItemStack(Items.COAL));
      tickMachine(helper, labFurnace, 1);
      CompoundTag labSave = labFurnace.saveWithFullMetadata(helper.getLevel().registryAccess());
      labSave.putInt("Progress", labSave.getInt("ProcessTimeNeeded") + 10);
      labFurnace.loadWithComponents(labSave, helper.getLevel().registryAccess());
      tickMachine(helper, labFurnace, 1);
      helper.assertTrue(labFurnace.getItem(0).is(Items.COBBLESTONE), "loaded lab furnace should not consume input from impossible saved progress");
      helper.assertTrue(labFurnace.getItem(2).isEmpty(), "loaded lab furnace should not finish after one tick from impossible saved progress");

      MachineBlockEntity electricalFurnace = placeMachineAt(
         helper, TEST_POS.offset(3, 0, 0), ModBlocks.SMALL_ELECTRICAL_FURNACE, MachineKind.SMALL_ELECTRICAL_FURNACE
      );
      electricalFurnace.setItem(0, new ItemStack(Items.COBBLESTONE));
      electricalFurnace.energyStorage(null).receiveEnergy(8000, false);
      tickMachine(helper, electricalFurnace, 1);
      CompoundTag electricalSave = electricalFurnace.saveWithFullMetadata(helper.getLevel().registryAccess());
      electricalSave.putInt("Progress", electricalSave.getInt("ProcessTimeNeeded") + 10);
      electricalFurnace.loadWithComponents(electricalSave, helper.getLevel().registryAccess());
      tickMachine(helper, electricalFurnace, 1);
      helper.assertTrue(
         electricalFurnace.getItem(0).is(Items.COBBLESTONE), "loaded electrical furnace should not consume input from impossible saved progress"
      );
      helper.assertTrue(electricalFurnace.getItem(2).isEmpty(), "loaded electrical furnace should not finish after one tick from impossible saved progress");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void temperature_machine_load_resets_out_of_bounds_progress(GameTestHelper helper) {
      MachineBlockEntity smelter = placeMachine(helper, ModBlocks.SMALL_MINERAL_SMELTER, MachineKind.SMALL_MINERAL_SMELTER);
      smelter.setItem(0, new ItemStack(Items.COBBLESTONE));
      smelter.energyStorage(null).receiveEnergy(16000, false);
      CompoundTag smelterSave = smelter.saveWithFullMetadata(helper.getLevel().registryAccess());
      smelterSave.putInt("Progress", 999);
      smelter.loadWithComponents(smelterSave, helper.getLevel().registryAccess());
      tickMachine(helper, smelter, 1);
      helper.assertTrue(smelter.getItem(0).is(Items.COBBLESTONE), "loaded mineral smelter should not consume input from impossible saved progress");
      helper.assertTrue(smelter.getItem(2).isEmpty(), "loaded mineral smelter should not finish after one tick from impossible saved progress");
      helper.assertValueEqual(smelter.dataAccessForTests().get(1), 1, "loaded mineral smelter should restart invalid progress from the first tick");

      MachineBlockEntity freezer = placeMachineAt(helper, TEST_POS.offset(3, 0, 0), ModBlocks.SMALL_FREEZER, MachineKind.SMALL_FREEZER);
      freezer.setItem(0, new ItemStack(Items.ICE));
      freezer.energyStorage(null).receiveEnergy(16000, false);
      CompoundTag freezerSave = freezer.saveWithFullMetadata(helper.getLevel().registryAccess());
      freezerSave.putInt("Progress", 999);
      freezer.loadWithComponents(freezerSave, helper.getLevel().registryAccess());
      tickMachine(helper, freezer, 1);
      helper.assertTrue(freezer.getItem(0).is(Items.ICE), "loaded freezer should not consume input from impossible saved progress");
      helper.assertTrue(freezer.getItem(2).isEmpty(), "loaded freezer should not finish after one tick from impossible saved progress");
      helper.assertValueEqual(freezer.dataAccessForTests().get(1), 1, "loaded freezer should restart invalid progress from the first tick");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 220)
   public static void small_mineral_smelter_outputs_to_parent_output_slot(GameTestHelper helper) {
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.SMALL_MINERAL_SMELTER, MachineKind.SMALL_MINERAL_SMELTER);
      machine.setItem(0, new ItemStack(Items.COBBLESTONE));
      machine.energyStorage(null).receiveEnergy(4000, false);
      helper.succeedWhen(() -> {
         tickMachine(helper, machine, 10);
         helper.assertTrue(machine.getItem(0).isEmpty(), "small mineral smelter should consume one mineral input");
         helper.assertTrue(machine.getItem(2).is(Items.MAGMA_BLOCK), "small mineral smelter should place magma in the parent output slot");
         helper.assertTrue(machine.getItem(1).isEmpty(), "small mineral smelter heat slot should not receive output items");
         helper.assertTrue(machine.energyStorage(null).getEnergyStored() < 4000, "small mineral smelter should consume FE while melting");
      });
   }

   @GameTest(template = "empty", timeoutTicks = 240)
   public static void small_mineral_smelter_waits_when_lava_tank_is_full(GameTestHelper helper) {
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.SMALL_MINERAL_SMELTER, MachineKind.SMALL_MINERAL_SMELTER);
      IFluidHandler fluidHandler = machine.fluidHandler(Direction.UP);
      helper.assertTrue(fluidHandler != null, "small mineral smelter should expose a lava fluid handler");
      helper.assertValueEqual(
         4000, fluidHandler.fill(new FluidStack(Fluids.LAVA, 4000), FluidAction.EXECUTE), "small mineral smelter should accept a full lava tank"
      );
      machine.setItem(0, new ItemStack(Items.COBBLESTONE));
      machine.energyStorage(null).receiveEnergy(4000, false);
      tickMachine(helper, machine, 220);
      helper.assertTrue(machine.getItem(0).is(Items.COBBLESTONE), "full lava tank should prevent consuming mineral input");
      helper.assertTrue(machine.getItem(2).isEmpty(), "full lava tank should prevent magma output");
      helper.assertValueEqual(4000, fluidHandler.getFluidInTank(0).getAmount(), "full lava tank should keep all stored lava");
      helper.assertValueEqual(4000, machine.energyStorage(null).getEnergyStored(), "full lava tank should prevent wasting FE");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 220)
   public static void small_mineral_smelter_clears_processing_phase_after_successful_melt(GameTestHelper helper) {
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.SMALL_MINERAL_SMELTER, MachineKind.SMALL_MINERAL_SMELTER);
      machine.setItem(0, new ItemStack(Items.COBBLESTONE));
      machine.energyStorage(null).receiveEnergy(4000, false);
      tickMachine(helper, machine, 180);
      helper.assertTrue(machine.getItem(0).isEmpty(), "successful mineral melt should consume one mineral input");
      helper.assertTrue(machine.getItem(2).is(Items.MAGMA_BLOCK), "successful mineral melt should create magma output");
      helper.assertBlockProperty(TEST_POS, MachineBlocks.PHASE_0_3, 0);
      helper.assertValueEqual(0, machine.comparatorOutput(), "successful mineral melt should clear the finished processing phase");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 220)
   public static void small_mineral_smelter_cools_lava_to_obsidian_when_disabled(GameTestHelper helper) {
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.SMALL_MINERAL_SMELTER, MachineKind.SMALL_MINERAL_SMELTER);
      IFluidHandler fluidHandler = machine.fluidHandler(Direction.UP);
      helper.assertTrue(fluidHandler != null, "small mineral smelter should expose a lava fluid handler");
      helper.assertValueEqual(
         1000, fluidHandler.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE), "small mineral smelter should accept one bucket of lava for cooling"
      );
      helper.setBlock(TEST_POS.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
      helper.succeedWhen(() -> {
         tickMachine(helper, machine, 10);
         helper.assertTrue(machine.getItem(2).is(Items.OBSIDIAN), "disabled small mineral smelter should cool lava into output-slot obsidian");
         helper.assertValueEqual(0, fluidHandler.getFluidInTank(0).getAmount(), "small mineral smelter should drain one bucket of lava while cooling");
      });
   }

   @GameTest(template = "empty", timeoutTicks = 220)
   public static void small_freezer_outputs_to_parent_output_slot(GameTestHelper helper) {
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.SMALL_FREEZER, MachineKind.SMALL_FREEZER);
      machine.setItem(0, new ItemStack(Items.ICE));
      machine.energyStorage(null).receiveEnergy(3000, false);
      helper.succeedWhen(() -> {
         tickMachine(helper, machine, 10);
         helper.assertTrue(machine.getItem(0).isEmpty(), "small freezer should consume one ice input");
         helper.assertTrue(machine.getItem(1).isEmpty(), "small freezer cold slot should not receive output items");
         helper.assertTrue(machine.getItem(2).is(Items.PACKED_ICE), "small freezer should place packed ice in the parent output slot");
         helper.assertTrue(machine.energyStorage(null).getEnergyStored() < 3000, "small freezer should consume FE while freezing");
      });
   }

   @GameTest(template = "empty", timeoutTicks = 120)
   public static void small_freezer_clears_processing_phase_when_output_becomes_full(GameTestHelper helper) {
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.SMALL_FREEZER, MachineKind.SMALL_FREEZER);
      machine.setItem(0, new ItemStack(Items.ICE));
      machine.energyStorage(null).receiveEnergy(1000, false);
      tickMachine(helper, machine, 60);
      helper.assertValueEqual(4, machine.comparatorOutput(), "small freezer should report active cooling before its output becomes blocked");
      machine.setItem(2, new ItemStack(Items.PACKED_ICE, 64));
      int energyBeforeBlockedTick = machine.energyStorage(null).getEnergyStored();
      tickMachine(helper, machine, 1);
      helper.assertTrue(machine.getItem(0).is(Items.ICE), "full output should keep the freezer input untouched");
      helper.assertValueEqual(64, machine.getItem(2).getCount(), "full output should stay full after the blocked tick");
      helper.assertValueEqual(energyBeforeBlockedTick, machine.energyStorage(null).getEnergyStored(), "blocked freezer output should not waste FE");
      helper.assertValueEqual(0, machine.comparatorOutput(), "blocked freezer output should clear the stale cooling phase");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 220)
   public static void small_freezer_clears_processing_phase_after_successful_freeze(GameTestHelper helper) {
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.SMALL_FREEZER, MachineKind.SMALL_FREEZER);
      machine.setItem(0, new ItemStack(Items.ICE));
      machine.energyStorage(null).receiveEnergy(3000, false);
      tickMachine(helper, machine, 180);
      helper.assertTrue(machine.getItem(0).isEmpty(), "successful freezer cycle should consume one ice input");
      helper.assertTrue(machine.getItem(2).is(Items.PACKED_ICE), "successful freezer cycle should create packed ice output");
      helper.assertBlockProperty(TEST_POS, MachineBlocks.PHASE_0_4, 0);
      helper.assertValueEqual(0, machine.comparatorOutput(), "successful freezer cycle should clear the finished cooling phase");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 220)
   public static void small_freezer_returns_water_bucket_to_input_and_outputs_ice(GameTestHelper helper) {
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.SMALL_FREEZER, MachineKind.SMALL_FREEZER);
      machine.setItem(0, new ItemStack(Items.WATER_BUCKET));
      machine.energyStorage(null).receiveEnergy(3000, false);
      helper.succeedWhen(() -> {
         tickMachine(helper, machine, 10);
         helper.assertTrue(machine.getItem(0).is(Items.BUCKET), "small freezer should return the emptied bucket to the input slot");
         helper.assertTrue(machine.getItem(1).isEmpty(), "small freezer cold slot should not receive water bucket output items");
         helper.assertTrue(machine.getItem(2).is(Items.ICE), "small freezer should place ice in the parent output slot");
         helper.assertTrue(machine.energyStorage(null).getEnergyStored() < 3000, "small freezer should consume FE while freezing water");
      });
   }

   @GameTest(template = "empty", timeoutTicks = 220)
   public static void small_freezer_does_not_process_its_output_slot_as_input(GameTestHelper helper) {
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.SMALL_FREEZER, MachineKind.SMALL_FREEZER);
      machine.setItem(2, new ItemStack(Items.ICE));
      tickMachine(helper, machine, 220);
      helper.assertTrue(machine.getItem(0).isEmpty(), "small freezer input slot should remain empty");
      helper.assertTrue(machine.getItem(1).isEmpty(), "small freezer cold slot should remain empty");
      helper.assertTrue(machine.getItem(2).is(Items.ICE), "small freezer should not upgrade ice already sitting in the output slot");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void small_freezer_direct_water_bucket_use_outputs_ice_to_output_slot(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.SMALL_FREEZER, MachineKind.SMALL_FREEZER);
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
      machine.energyStorage(null).receiveEnergy(2000, false);
      boolean handled = machine.handleItemUse(player.getItemInHand(InteractionHand.MAIN_HAND), player, InteractionHand.MAIN_HAND);
      helper.assertTrue(handled, "small freezer should handle direct water bucket use");
      helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET), "small freezer should return an empty bucket to the player");
      helper.assertTrue(machine.getItem(0).isEmpty(), "small freezer direct bucket use should not place ice in the input slot");
      helper.assertTrue(machine.getItem(1).isEmpty(), "small freezer direct bucket use should not place ice in the cold slot");
      helper.assertTrue(machine.getItem(2).is(Items.ICE), "small freezer direct bucket use should place ice in the output slot");
      helper.assertTrue(machine.energyStorage(null).getEnergyStored() < 2000, "small freezer direct bucket use should consume FE");
      helper.assertValueEqual(0, machine.comparatorOutput(), "small freezer direct bucket use should not leave a stale cooling phase");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 240)
   public static void powered_temperature_machines_do_not_process_without_fe(GameTestHelper helper) {
      MachineBlockEntity mineralSmelter = placeMachine(helper, ModBlocks.SMALL_MINERAL_SMELTER, MachineKind.SMALL_MINERAL_SMELTER);
      mineralSmelter.setItem(0, new ItemStack(Items.COBBLESTONE));
      helper.assertTrue(mineralSmelter.energyStorage(null) != null, "small mineral smelter should expose FE input");
      BlockPos freezerPos = TEST_POS.offset(3, 0, 0);
      MachineBlockEntity freezer = placeMachineAt(helper, freezerPos, ModBlocks.SMALL_FREEZER, MachineKind.SMALL_FREEZER);
      freezer.setItem(0, new ItemStack(Items.ICE));
      helper.assertTrue(freezer.energyStorage(null) != null, "small freezer should expose FE input");
      tickMachine(helper, mineralSmelter, 220);
      tickMachine(helper, freezer, 220);
      helper.assertTrue(mineralSmelter.getItem(0).is(Items.COBBLESTONE), "small mineral smelter should not consume input without FE");
      helper.assertTrue(mineralSmelter.getItem(2).isEmpty(), "small mineral smelter should not output magma without FE");
      helper.assertTrue(freezer.getItem(0).is(Items.ICE), "small freezer should not consume input without FE");
      helper.assertTrue(freezer.getItem(2).isEmpty(), "small freezer should not output packed ice without FE");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 120)
   public static void powered_temperature_machine_comparators_report_processing_phase(GameTestHelper helper) {
      MachineBlockEntity mineralSmelter = placeMachine(helper, ModBlocks.SMALL_MINERAL_SMELTER, MachineKind.SMALL_MINERAL_SMELTER);
      mineralSmelter.setItem(0, new ItemStack(Items.COBBLESTONE));
      mineralSmelter.energyStorage(null).receiveEnergy(4000, false);
      tickMachine(helper, mineralSmelter, 60);
      helper.assertValueEqual(5, mineralSmelter.comparatorOutput(), "small mineral smelter comparator should report the melting phase, not tank fill");
      BlockPos freezerPos = TEST_POS.offset(3, 0, 0);
      MachineBlockEntity freezer = placeMachineAt(helper, freezerPos, ModBlocks.SMALL_FREEZER, MachineKind.SMALL_FREEZER);
      freezer.setItem(0, new ItemStack(Items.ICE));
      freezer.energyStorage(null).receiveEnergy(1000, false);
      tickMachine(helper, freezer, 60);
      helper.assertValueEqual(4, freezer.comparatorOutput(), "small freezer comparator should report the cooling phase, not inventory fullness");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void machine_gui_actions_update_original_parent_fields(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      MachineBlockEntity hopper = placeMachine(helper, ModBlocks.FACTORY_HOPPER, MachineKind.FACTORY_HOPPER);
      MachineMenu hopperMenu = new MachineMenu(MachineKind.FACTORY_HOPPER, 1, player.getInventory(), hopper, hopper.dataAccessForTests());
      hopperMenu.handleAction(player, 1, 4, 0);
      hopperMenu.handleAction(player, 2, 16, 0);
      hopperMenu.handleAction(player, 4, 75, 0);
      helper.assertValueEqual(4, hopperMenu.field(0), "factory hopper GUI range action should update parent field 0");
      helper.assertValueEqual(16, hopperMenu.field(1), "factory hopper GUI count action should update parent field 1");
      helper.assertValueEqual(75, hopperMenu.field(3), "factory hopper GUI period action should update parent field 3");
      MachineBlockEntity dropper = placeMachine(helper, ModBlocks.FACTORY_DROPPER, MachineKind.FACTORY_DROPPER);
      MachineMenu dropperMenu = new MachineMenu(MachineKind.FACTORY_DROPPER, 2, player.getInventory(), dropper, dropper.dataAccessForTests());
      dropperMenu.handleAction(player, 10, 80, 0);
      dropperMenu.handleAction(player, 11, -50, 25);
      dropperMenu.handleAction(player, 12, 7, 0);
      helper.assertValueEqual(80, dropperMenu.field(0), "factory dropper speed button should update parent field 0");
      helper.assertValueEqual(-50, dropperMenu.field(1), "factory dropper x-angle button should update parent field 1");
      helper.assertValueEqual(25, dropperMenu.field(2), "factory dropper y-angle button should update parent field 2");
      helper.assertValueEqual(7, dropperMenu.field(4), "factory dropper stack-size button should update parent field 4");
      MachineBlockEntity placer = placeMachine(helper, ModBlocks.FACTORY_PLACER, MachineKind.FACTORY_PLACER);
      MachineMenu placerMenu = new MachineMenu(MachineKind.FACTORY_PLACER, 3, player.getInventory(), placer, placer.dataAccessForTests());
      placerMenu.handleAction(player, 20, 1, 0);
      placerMenu.handleAction(player, 21, 1, 0);
      placerMenu.handleAction(player, 22, 2, 0);
      helper.assertValueEqual(1, placerMenu.field(0), "factory placer logic button should update parent field 0");
      helper.assertValueEqual(1, placerMenu.field(1), "factory placer manual trigger should light parent field 1");
      helper.assertValueEqual(12, placerMenu.field(2), "factory placer schematic line should select the matching stock row");
      MachineBlockEntity electrical = placeMachine(helper, ModBlocks.SMALL_ELECTRICAL_FURNACE, MachineKind.SMALL_ELECTRICAL_FURNACE);
      MachineMenu electricalMenu = new MachineMenu(MachineKind.SMALL_ELECTRICAL_FURNACE, 4, player.getInventory(), electrical, electrical.dataAccessForTests());
      electricalMenu.handleAction(player, 30, 3, 0);
      helper.assertValueEqual(3, electricalMenu.field(4), "small electrical furnace speed selector should update parent field 4");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void machine_logic_fields_ignore_unknown_gui_and_nbt_bits(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      MachineBlockEntity hopper = placeMachine(helper, ModBlocks.FACTORY_HOPPER, MachineKind.FACTORY_HOPPER);
      MachineMenu hopperMenu = new MachineMenu(MachineKind.FACTORY_HOPPER, 5, player.getInventory(), hopper, hopper.dataAccessForTests());
      hopperMenu.handleAction(player, 3, -1, 0);
      helper.assertValueEqual(hopperMenu.field(2), 7, "factory hopper GUI logic should discard unknown packet bits");
      CompoundTag hopperSave = hopper.saveWithFullMetadata(helper.getLevel().registryAccess());
      hopperSave.putInt("HopperLogic", -1);
      MachineBlockEntity loadedHopper = new MachineBlockEntity(hopper.getBlockPos(), hopper.getBlockState());
      loadedHopper.loadWithComponents(hopperSave, helper.getLevel().registryAccess());
      helper.assertValueEqual(loadedHopper.dataAccessForTests().get(2), 7, "factory hopper load should discard unknown saved logic bits");
      hopperMenu.removed(player);

      MachineBlockEntity dropper = placeMachineAt(helper, TEST_POS.offset(3, 0, 0), ModBlocks.FACTORY_DROPPER, MachineKind.FACTORY_DROPPER);
      MachineMenu dropperMenu = new MachineMenu(MachineKind.FACTORY_DROPPER, 6, player.getInventory(), dropper, dropper.dataAccessForTests());
      dropperMenu.handleAction(player, 14, -1, 0);
      helper.assertValueEqual(dropperMenu.field(5), 51, "factory dropper GUI logic should discard unknown packet bits");
      CompoundTag dropperSave = dropper.saveWithFullMetadata(helper.getLevel().registryAccess());
      dropperSave.putInt("DropperLogic", -1);
      MachineBlockEntity loadedDropper = new MachineBlockEntity(dropper.getBlockPos(), dropper.getBlockState());
      loadedDropper.loadWithComponents(dropperSave, helper.getLevel().registryAccess());
      helper.assertValueEqual(loadedDropper.dataAccessForTests().get(5), 51, "factory dropper load should discard unknown saved logic bits");
      dropperMenu.removed(player);

      MachineBlockEntity placer = placeMachineAt(helper, TEST_POS.offset(6, 0, 0), ModBlocks.FACTORY_PLACER, MachineKind.FACTORY_PLACER);
      MachineMenu placerMenu = new MachineMenu(MachineKind.FACTORY_PLACER, 7, player.getInventory(), placer, placer.dataAccessForTests());
      placerMenu.handleAction(player, 20, -1, 0);
      helper.assertValueEqual(placerMenu.field(0), 7, "factory placer GUI logic should discard unknown packet bits");
      CompoundTag placerSave = placer.saveWithFullMetadata(helper.getLevel().registryAccess());
      placerSave.putInt("PlacerLogic", -1);
      MachineBlockEntity loadedPlacer = new MachineBlockEntity(placer.getBlockPos(), placer.getBlockState());
      loadedPlacer.loadWithComponents(placerSave, helper.getLevel().registryAccess());
      helper.assertValueEqual(loadedPlacer.dataAccessForTests().get(0), 7, "factory placer load should discard unknown saved logic bits");
      placerMenu.removed(player);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void machine_defaulted_saved_fields_ignore_wrong_nbt_types(GameTestHelper helper) {
      MachineBlockEntity hopper = placeMachine(helper, ModBlocks.FACTORY_HOPPER, MachineKind.FACTORY_HOPPER);
      CompoundTag hopperSave = hopper.saveWithFullMetadata(helper.getLevel().registryAccess());
      hopperSave.putString("HopperLogic", "bad");
      MachineBlockEntity loadedHopper = new MachineBlockEntity(hopper.getBlockPos(), hopper.getBlockState());
      loadedHopper.loadWithComponents(hopperSave, helper.getLevel().registryAccess());
      helper.assertValueEqual(loadedHopper.dataAccessForTests().get(2), 3, "factory hopper should use its default logic when saved logic is not numeric");

      MachineBlockEntity dropper = placeMachineAt(helper, TEST_POS.offset(3, 0, 0), ModBlocks.FACTORY_DROPPER, MachineKind.FACTORY_DROPPER);
      CompoundTag dropperSave = dropper.saveWithFullMetadata(helper.getLevel().registryAccess());
      dropperSave.putString("DropperSpeed", "bad");
      dropperSave.putString("DropperCount", "bad");
      dropperSave.putString("DropperLogic", "bad");
      MachineBlockEntity loadedDropper = new MachineBlockEntity(dropper.getBlockPos(), dropper.getBlockState());
      loadedDropper.loadWithComponents(dropperSave, helper.getLevel().registryAccess());
      helper.assertValueEqual(loadedDropper.dataAccessForTests().get(0), 10, "factory dropper should use its default speed when saved speed is not numeric");
      helper.assertValueEqual(loadedDropper.dataAccessForTests().get(4), 1, "factory dropper should use its default stack count when saved count is not numeric");
      helper.assertValueEqual(loadedDropper.dataAccessForTests().get(5), 2, "factory dropper should use its default logic when saved logic is not numeric");

      MachineBlockEntity placer = placeMachineAt(helper, TEST_POS.offset(6, 0, 0), ModBlocks.FACTORY_PLACER, MachineKind.FACTORY_PLACER);
      CompoundTag placerSave = placer.saveWithFullMetadata(helper.getLevel().registryAccess());
      placerSave.putString("PlacerLogic", "bad");
      MachineBlockEntity loadedPlacer = new MachineBlockEntity(placer.getBlockPos(), placer.getBlockState());
      loadedPlacer.loadWithComponents(placerSave, helper.getLevel().registryAccess());
      helper.assertValueEqual(loadedPlacer.dataAccessForTests().get(0), 6, "factory placer should use its default logic when saved logic is not numeric");

      MachineBlockEntity electrical = placeMachineAt(helper, TEST_POS.offset(9, 0, 0), ModBlocks.SMALL_ELECTRICAL_FURNACE, MachineKind.SMALL_ELECTRICAL_FURNACE);
      CompoundTag electricalSave = electrical.saveWithFullMetadata(helper.getLevel().registryAccess());
      electricalSave.putString("ElectricalSpeed", "bad");
      MachineBlockEntity loadedElectrical = new MachineBlockEntity(electrical.getBlockPos(), electrical.getBlockState());
      loadedElectrical.loadWithComponents(electricalSave, helper.getLevel().registryAccess());
      helper.assertValueEqual(loadedElectrical.dataAccessForTests().get(4), 1, "small electrical furnace should use its default speed when saved speed is not numeric");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 120)
   public static void factory_dropper_manual_gui_trigger_drops_configured_stack_size(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.FACTORY_DROPPER, MachineKind.FACTORY_DROPPER);
      MachineMenu menu = new MachineMenu(MachineKind.FACTORY_DROPPER, 4, player.getInventory(), machine, machine.dataAccessForTests());
      machine.setItem(0, new ItemStack(Items.COBBLESTONE, 5));
      menu.handleAction(player, 12, 3, 0);
      menu.handleAction(player, 16, 1, 0);
      tickMachine(helper, machine, 8);
      helper.assertValueEqual(2, machine.getItem(0).getCount(), "factory dropper manual GUI trigger should eject the configured stack size");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 120)
   public static void factory_dropper_duplicate_filters_do_not_emit_partial_matches(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.FACTORY_DROPPER, MachineKind.FACTORY_DROPPER);
      MachineMenu menu = new MachineMenu(MachineKind.FACTORY_DROPPER, 5, player.getInventory(), machine, machine.dataAccessForTests());
      machine.setItem(0, new ItemStack(Items.DIRT, 6));
      machine.setItem(12, new ItemStack(Items.DIRT, 4));
      machine.setItem(13, new ItemStack(Items.DIRT, 4));
      menu.handleAction(player, 16, 1, 0);
      tickMachine(helper, machine, 8);
      helper.assertValueEqual(2, machine.getItem(0).getCount(), "factory dropper should leave stock that cannot satisfy a full duplicate filter");
      AABB dropSearch = new AABB(helper.absolutePos(TEST_POS)).inflate(3.0);
      int droppedDirt = helper.getLevel()
         .getEntitiesOfClass(ItemEntity.class, dropSearch, item -> item.getItem().is(Items.DIRT))
         .stream()
         .mapToInt(item -> item.getItem().getCount())
         .sum();
      helper.assertValueEqual(4, droppedDirt, "factory dropper should only eject one full duplicate filter request");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 120)
   public static void factory_placer_manual_gui_trigger_places_next_block(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      helper.setBlock(TEST_POS, Blocks.AIR);
      helper.setBlock(
         TEST_POS,
         (BlockState)((MachineBlocks.DirectionalMachineBlock)ModBlocks.FACTORY_PLACER.get()).defaultBlockState().setValue(MachineBlocks.FACING, Direction.EAST)
      );
      BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(TEST_POS));
      helper.assertTrue(blockEntity instanceof MachineBlockEntity, "factory placer should create a machine block entity");
      MachineBlockEntity machine = (MachineBlockEntity)blockEntity;
      MachineMenu menu = new MachineMenu(MachineKind.FACTORY_PLACER, 5, player.getInventory(), machine, machine.dataAccessForTests());
      machine.setItem(0, new ItemStack(Items.DIRT));
      menu.handleAction(player, 21, 1, 0);
      tickMachine(helper, machine, 12);
      helper.assertTrue(
         helper.getBlockState(TEST_POS.east()).is(Blocks.DIRT), "factory placer manual GUI trigger should place the next block in front of the machine"
      );
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 120)
   public static void factory_placer_accepts_and_plants_crop_seeds(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      BlockPos placerPos = new BlockPos(1, 2, 1);
      BlockPos cropPos = placerPos.east();
      helper.setBlock(cropPos.below(), Blocks.FARMLAND);
      helper.setBlock(cropPos, Blocks.AIR);
      helper.setBlock(cropPos.above(), Blocks.GLOWSTONE);
      helper.setBlock(
         placerPos,
         (BlockState)((MachineBlocks.DirectionalMachineBlock)ModBlocks.FACTORY_PLACER.get()).defaultBlockState().setValue(MachineBlocks.FACING, Direction.EAST)
      );
      BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(placerPos));
      helper.assertTrue(blockEntity instanceof MachineBlockEntity, "factory placer should create a machine block entity");
      MachineBlockEntity machine = (MachineBlockEntity)blockEntity;
      IItemHandler handler = machine.itemHandler(Direction.UP);
      helper.assertTrue(handler != null, "factory placer should expose an item handler capability");
      ItemStack rejected = handler.insertItem(0, new ItemStack(Items.WHEAT_SEEDS), false);
      helper.assertTrue(rejected.isEmpty(), "factory placer should accept crop seeds advertised by its help text");
      MachineMenu menu = new MachineMenu(MachineKind.FACTORY_PLACER, 6, player.getInventory(), machine, machine.dataAccessForTests());
      menu.handleAction(player, 21, 1, 0);
      tickMachine(helper, machine, 12);
      helper.assertTrue(helper.getBlockState(cropPos).is(Blocks.WHEAT), "factory placer manual GUI trigger should plant wheat seeds above farmland");
      helper.assertTrue(machine.getItem(0).isEmpty(), "factory placer should consume one planted seed");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 120)
   public static void factory_placer_does_not_force_place_unsupported_block_items(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      BlockPos placerPos = new BlockPos(1, 2, 1);
      BlockPos targetPos = placerPos.east();
      helper.setBlock(targetPos, Blocks.AIR);
      helper.setBlock(targetPos.below(), Blocks.AIR);
      helper.setBlock(
         placerPos,
         (BlockState)((MachineBlocks.DirectionalMachineBlock)ModBlocks.FACTORY_PLACER.get()).defaultBlockState().setValue(MachineBlocks.FACING, Direction.EAST)
      );
      BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(placerPos));
      helper.assertTrue(blockEntity instanceof MachineBlockEntity, "factory placer should create a machine block entity");
      MachineBlockEntity machine = (MachineBlockEntity)blockEntity;
      machine.setItem(0, new ItemStack(Items.SUGAR_CANE));
      MachineMenu menu = new MachineMenu(MachineKind.FACTORY_PLACER, 7, player.getInventory(), machine, machine.dataAccessForTests());
      menu.handleAction(player, 21, 1, 0);
      tickMachine(helper, machine, 12);
      helper.assertTrue(helper.getBlockState(targetPos).isAir(), "factory placer should not force-place unsupported sugar cane in mid-air");
      helper.assertTrue(machine.getItem(0).isEmpty(), "manual failed placement should eject the unsupported block item");
      AABB dropSearch = new AABB(helper.absolutePos(placerPos)).inflate(3.0);
      boolean sugarCaneDropFound = helper.getLevel()
         .getEntitiesOfClass(ItemEntity.class, dropSearch, item -> item.getItem().is(Items.SUGAR_CANE))
         .stream()
         .anyMatch(item -> item.getItem().getCount() == 1);
      helper.assertTrue(sugarCaneDropFound, "factory placer should spawn the unsupported block item as an item entity");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 120)
   public static void factory_placer_manual_trigger_spits_blocked_stock(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      helper.setBlock(TEST_POS.east(), Blocks.STONE);
      helper.setBlock(
         TEST_POS,
         (BlockState)((MachineBlocks.DirectionalMachineBlock)ModBlocks.FACTORY_PLACER.get()).defaultBlockState().setValue(MachineBlocks.FACING, Direction.EAST)
      );
      BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(TEST_POS));
      helper.assertTrue(blockEntity instanceof MachineBlockEntity, "factory placer should create a machine block entity");
      MachineBlockEntity machine = (MachineBlockEntity)blockEntity;
      machine.setItem(0, new ItemStack(Items.DIRT));
      MachineMenu menu = new MachineMenu(MachineKind.FACTORY_PLACER, 7, player.getInventory(), machine, machine.dataAccessForTests());
      menu.handleAction(player, 21, 1, 0);
      tickMachine(helper, machine, 12);
      helper.assertTrue(machine.getItem(0).isEmpty(), "factory placer manual trigger should eject one blocked stock item");
      helper.assertTrue(helper.getBlockState(TEST_POS.east()).is(Blocks.STONE), "factory placer should not replace an occupied target position");
      AABB dropSearch = new AABB(helper.absolutePos(TEST_POS)).inflate(3.0);
      boolean dirtDropFound = helper.getLevel()
         .getEntitiesOfClass(ItemEntity.class, dropSearch, item -> item.getItem().is(Items.DIRT))
         .stream()
         .anyMatch(item -> item.getItem().getCount() == 1);
      helper.assertTrue(dirtDropFound, "factory placer should spawn the blocked stock item as an item entity");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 120)
   public static void factory_placer_spit_out_updates_adjacent_comparator(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      BlockPos comparatorPos = TEST_POS.east();
      BlockPos targetPos = TEST_POS.north();
      helper.setBlock(comparatorPos.below(), Blocks.STONE);
      helper.setBlock(targetPos, Blocks.STONE);
      helper.setBlock(
         TEST_POS,
         (BlockState)((MachineBlocks.DirectionalMachineBlock)ModBlocks.FACTORY_PLACER.get()).defaultBlockState().setValue(MachineBlocks.FACING, Direction.NORTH)
      );
      BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(TEST_POS));
      helper.assertTrue(blockEntity instanceof MachineBlockEntity, "factory placer should create a machine block entity");
      MachineBlockEntity machine = (MachineBlockEntity)blockEntity;
      machine.setItem(0, new ItemStack(Items.DIRT));
      helper.setBlock(comparatorPos, (BlockState)Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.WEST));
      helper.getLevel().updateNeighbourForOutputSignal(helper.absolutePos(TEST_POS), helper.getBlockState(TEST_POS).getBlock());
      BlockEntity comparatorEntity = helper.getLevel().getBlockEntity(helper.absolutePos(comparatorPos));
      helper.assertTrue(comparatorEntity instanceof ComparatorBlockEntity, "factory placer comparator test should keep its block entity");
      helper.runAfterDelay(4L, () -> {
         helper.assertTrue(
            ((ComparatorBlockEntity)comparatorEntity).getOutputSignal() > 0, "stocked factory placer should prime the adjacent comparator"
         );
         MachineMenu menu = new MachineMenu(MachineKind.FACTORY_PLACER, 7, player.getInventory(), machine, machine.dataAccessForTests());
         menu.handleAction(player, 21, 1, 0);
         tickMachine(helper, machine, 12);
         helper.assertTrue(machine.getItem(0).isEmpty(), "blocked manual factory placer trigger should spit out its stocked block");
         helper.runAfterDelay(4L, () -> {
            helper.assertValueEqual(
               0,
               ((ComparatorBlockEntity)comparatorEntity).getOutputSignal(),
               "factory placer spit-out should refresh adjacent comparator output"
            );
            helper.succeed();
         });
      });
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void machines_expose_modded_item_handler_capability(GameTestHelper helper) {
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.FACTORY_HOPPER, MachineKind.FACTORY_HOPPER);
      IItemHandler handler = (IItemHandler)helper.getLevel().getCapability(ItemHandler.BLOCK, helper.absolutePos(TEST_POS), Direction.UP);
      helper.assertTrue(handler != null, "factory hopper should expose an item handler capability for modded automation");
      helper.assertValueEqual(18, handler.getSlots(), "factory hopper item capability should expose visible machine slots");
      ItemStack remainder = handler.insertItem(0, new ItemStack(Items.COBBLESTONE, 6), false);
      helper.assertTrue(remainder.isEmpty(), "factory hopper item capability should accept valid inventory input");
      helper.assertValueEqual(6, machine.getItem(0).getCount(), "factory hopper capability insert should update machine inventory");
      ItemStack limitedRemainder = handler.insertItem(1, new ItemStack(Items.SNOWBALL, 32), false);
      helper.assertValueEqual(16, machine.getItem(1).getCount(), "factory hopper capability insert should respect item max stack size");
      helper.assertValueEqual(16, limitedRemainder.getCount(), "factory hopper capability insert should return overflow above item max stack size");
      MachineBlockEntity dropper = placeMachine(helper, ModBlocks.FACTORY_DROPPER, MachineKind.FACTORY_DROPPER);
      IItemHandler dropperHandler = dropper.itemHandler(Direction.UP);
      helper.assertTrue(dropperHandler != null, "factory dropper should expose an item handler capability");
      ItemStack dropperInputAccepted = dropperHandler.insertItem(0, new ItemStack(Items.COBBLESTONE), false);
      helper.assertTrue(dropperInputAccepted.isEmpty(), "factory dropper automation should accept normal input slots");
      ItemStack dropperFilterBlocked = dropperHandler.insertItem(12, new ItemStack(Items.DIRT), false);
      helper.assertValueEqual(1, dropperFilterBlocked.getCount(), "factory dropper filter slots should reject automation inserts");
      helper.assertTrue(dropper.getItem(12).isEmpty(), "factory dropper filter slot should remain empty after rejected automation insert");
      dropper.setItem(12, new ItemStack(Items.DIRT));
      helper.assertTrue(dropperHandler.getStackInSlot(12).isEmpty(), "factory dropper filter slots should be hidden from automation reads");
      helper.assertTrue(dropperHandler.extractItem(12, 1, false).isEmpty(), "factory dropper filter slots should reject automation extraction");
      helper.assertTrue(dropper.getItem(12).is(Items.DIRT), "factory dropper filter slot should keep its manually configured item");
      MachineBlockEntity furnace = placeMachine(helper, ModBlocks.SMALL_LAB_FURNACE, MachineKind.SMALL_LAB_FURNACE);
      IItemHandler furnaceHandler = (IItemHandler)helper.getLevel().getCapability(ItemHandler.BLOCK, helper.absolutePos(TEST_POS), Direction.UP);
      helper.assertTrue(furnaceHandler != null, "small lab furnace should expose an item handler capability");
      ItemStack blocked = furnaceHandler.insertItem(2, new ItemStack(Items.COBBLESTONE), false);
      helper.assertValueEqual(1, blocked.getCount(), "small lab furnace item capability should reject insertion into output slot");
      MachineBlockEntity electricalFurnace = placeMachine(helper, ModBlocks.SMALL_ELECTRICAL_FURNACE, MachineKind.SMALL_ELECTRICAL_FURNACE);
      IItemHandler electricalHandler = electricalFurnace.itemHandler(Direction.UP);
      helper.assertTrue(electricalHandler != null, "small electrical furnace should expose an item handler capability");
      ItemStack auxJunkBlocked = electricalHandler.insertItem(1, new ItemStack(Items.COBBLESTONE), false);
      helper.assertValueEqual(1, auxJunkBlocked.getCount(), "small electrical furnace aux slot should reject non-Hopper automation inserts");
      helper.assertTrue(electricalFurnace.getItem(1).isEmpty(), "small electrical furnace aux slot should stay empty after rejected insert");
      ItemStack auxHopperAccepted = electricalHandler.insertItem(1, new ItemStack(Items.HOPPER), false);
      helper.assertTrue(auxHopperAccepted.isEmpty(), "small electrical furnace aux slot should accept a Hopper feeder");
      helper.assertTrue(electricalFurnace.getItem(1).is(Items.HOPPER), "small electrical furnace aux slot should store the accepted Hopper feeder");
      electricalFurnace.setItem(0, new ItemStack(Items.RAW_IRON));
      electricalFurnace.setItem(3, new ItemStack(Items.RAW_COPPER));
      electricalFurnace.setItem(4, new ItemStack(Items.RAW_GOLD));
      electricalFurnace.setItem(2, new ItemStack(Items.IRON_INGOT));
      electricalFurnace.setItem(5, new ItemStack(Items.COPPER_INGOT));
      helper.assertTrue(electricalHandler.extractItem(0, 1, false).isEmpty(), "small electrical furnace automation should not extract from active input slot");
      helper.assertTrue(electricalHandler.extractItem(1, 1, false).isEmpty(), "small electrical furnace automation should not extract from aux slot");
      helper.assertTrue(electricalHandler.extractItem(3, 1, false).isEmpty(), "small electrical furnace automation should not extract from input FIFO slot");
      helper.assertTrue(electricalHandler.extractItem(4, 1, false).isEmpty(), "small electrical furnace automation should not extract from input FIFO slot");
      helper.assertTrue(electricalFurnace.getItem(0).is(Items.RAW_IRON), "small electrical furnace input should remain after automation extraction attempt");
      helper.assertTrue(electricalFurnace.getItem(3).is(Items.RAW_COPPER), "small electrical furnace input FIFO should remain after automation extraction attempt");
      helper.assertTrue(electricalFurnace.getItem(4).is(Items.RAW_GOLD), "small electrical furnace input FIFO should remain after automation extraction attempt");
      ItemStack extractedElectricalOutput = electricalHandler.extractItem(2, 1, false);
      ItemStack extractedElectricalOutputFifo = electricalHandler.extractItem(5, 1, false);
      helper.assertTrue(extractedElectricalOutput.is(Items.IRON_INGOT), "small electrical furnace automation should extract from output slot");
      helper.assertTrue(extractedElectricalOutputFifo.is(Items.COPPER_INGOT), "small electrical furnace automation should extract from output FIFO slot");
      MachineBlockEntity mineralSmelter = placeMachine(helper, ModBlocks.SMALL_MINERAL_SMELTER, MachineKind.SMALL_MINERAL_SMELTER);
      IItemHandler mineralSmelterHandler = mineralSmelter.itemHandler(Direction.UP);
      helper.assertTrue(mineralSmelterHandler != null, "small mineral smelter should expose an item handler capability");
      ItemStack mineralBlocked = mineralSmelterHandler.insertItem(2, new ItemStack(Items.COBBLESTONE), false);
      helper.assertValueEqual(1, mineralBlocked.getCount(), "layout output slots should reject automated insertion");
      helper.assertTrue(mineralSmelter.getItem(2).isEmpty(), "small mineral smelter output slot should remain empty after rejected insert");
      ItemStack mineralHeatBlocked = mineralSmelterHandler.insertItem(1, new ItemStack(Items.COBBLESTONE), false);
      helper.assertValueEqual(1, mineralHeatBlocked.getCount(), "small mineral smelter heat slot should reject automated mineral insertion");
      helper.assertTrue(mineralSmelter.getItem(1).isEmpty(), "small mineral smelter heat slot should remain empty after rejected insert");
      MachineBlockEntity freezer = placeMachine(helper, ModBlocks.SMALL_FREEZER, MachineKind.SMALL_FREEZER);
      IItemHandler freezerHandler = freezer.itemHandler(Direction.UP);
      helper.assertTrue(freezerHandler != null, "small freezer should expose an item handler capability");
      ItemStack coldSlotBlocked = freezerHandler.insertItem(1, new ItemStack(Items.ICE), false);
      helper.assertValueEqual(1, coldSlotBlocked.getCount(), "small freezer cold slot should reject automated input insertion");
      helper.assertTrue(freezer.getItem(1).isEmpty(), "small freezer cold slot should remain empty after rejected insert");
      MachineBlockEntity placer = placeMachine(helper, ModBlocks.FACTORY_PLACER, MachineKind.FACTORY_PLACER);
      IItemHandler placerHandler = placer.itemHandler(Direction.UP);
      helper.assertTrue(placerHandler != null, "factory placer should expose an item handler capability");
      ItemStack placerNonBlockBlocked = placerHandler.insertItem(0, new ItemStack(Items.APPLE), false);
      helper.assertValueEqual(1, placerNonBlockBlocked.getCount(), "factory placer block slot should reject non-block automation inserts");
      helper.assertTrue(placer.getItem(0).isEmpty(), "factory placer block slot should stay empty after rejected insert");
      ItemStack placerBlockAccepted = placerHandler.insertItem(0, new ItemStack(Items.DIRT), false);
      helper.assertTrue(placerBlockAccepted.isEmpty(), "factory placer block slot should accept block items");
      helper.assertTrue(placer.getItem(0).is(Items.DIRT), "factory placer block slot should store accepted blocks");
      MachineBlockEntity fluidBarrel = placeMachine(helper, ModBlocks.FLUID_BARREL, MachineKind.FLUID_BARREL);
      IItemHandler fluidBarrelHandler = fluidBarrel.itemHandler(Direction.UP);
      helper.assertTrue(fluidBarrelHandler != null, "fluid barrel should expose an item handler capability");
      ItemStack fluidInputBlocked = fluidBarrelHandler.insertItem(0, new ItemStack(Items.COBBLESTONE), false);
      helper.assertValueEqual(1, fluidInputBlocked.getCount(), "fluid barrel empty-bucket slot should reject non-bucket automation inserts");
      helper.assertTrue(fluidBarrel.getItem(0).isEmpty(), "fluid barrel empty-bucket slot should stay empty after rejected insert");
      ItemStack emptyBucketAccepted = fluidBarrelHandler.insertItem(0, new ItemStack(Items.BUCKET), false);
      helper.assertTrue(emptyBucketAccepted.isEmpty(), "fluid barrel empty-bucket slot should accept empty buckets");
      helper.assertTrue(fluidBarrel.getItem(0).is(Items.BUCKET), "fluid barrel empty-bucket slot should store accepted buckets");
      ItemStack filledSlotBlocked = fluidBarrelHandler.insertItem(1, new ItemStack(Items.COBBLESTONE), false);
      helper.assertValueEqual(1, filledSlotBlocked.getCount(), "filled bucket result slot should reject automated insertion");
      helper.assertTrue(fluidBarrel.getItem(1).isEmpty(), "fluid barrel filled bucket slot should remain empty after rejected insert");
      MachineBlockEntity milkingMachine = placeMachine(helper, ModBlocks.SMALL_MILKING_MACHINE, MachineKind.SMALL_MILKING_MACHINE);
      IItemHandler milkingHandler = milkingMachine.itemHandler(Direction.UP);
      helper.assertTrue(milkingHandler != null, "small milking machine should expose an item handler capability");
      ItemStack milkInputBlocked = milkingHandler.insertItem(0, new ItemStack(Items.COBBLESTONE), false);
      helper.assertValueEqual(1, milkInputBlocked.getCount(), "small milking machine bucket input slot should reject non-bucket automation inserts");
      helper.assertTrue(milkingMachine.getItem(0).isEmpty(), "small milking machine bucket input slot should stay empty after rejected insert");
      ItemStack bucketAccepted = milkingHandler.insertItem(0, new ItemStack(Items.BUCKET), false);
      helper.assertTrue(bucketAccepted.isEmpty(), "small milking machine bucket input slot should accept empty buckets");
      helper.assertTrue(milkingMachine.getItem(0).is(Items.BUCKET), "small milking machine bucket input slot should store accepted buckets");
      ItemStack milkSlotBlocked = milkingHandler.insertItem(1, new ItemStack(Items.COBBLESTONE), false);
      helper.assertValueEqual(1, milkSlotBlocked.getCount(), "milk result slot should reject automated insertion");
      helper.assertTrue(milkingMachine.getItem(1).isEmpty(), "small milking machine milk result slot should remain empty after rejected insert");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 220)
   public static void small_milking_machine_outputs_milk_bucket_to_result_slot(GameTestHelper helper) {
      helper.setBlock(TEST_POS, Blocks.AIR);
      helper.setBlock(
         TEST_POS,
         (BlockState)((MachineBlocks.MilkingMachineBlock)ModBlocks.SMALL_MILKING_MACHINE.get())
            .defaultBlockState()
            .setValue(MachineBlocks.HORIZONTAL_FACING, Direction.EAST)
      );
      BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(TEST_POS));
      helper.assertTrue(blockEntity instanceof MachineBlockEntity, "small milking machine should create a machine block entity");
      MachineBlockEntity machine = (MachineBlockEntity)blockEntity;
      machine.setItem(0, new ItemStack(Items.BUCKET));
      helper.spawn(EntityType.COW, TEST_POS.east());
      helper.succeedWhen(() -> {
         tickMachine(helper, machine, 20);
         helper.assertTrue(machine.getItem(0).isEmpty(), "small milking machine should consume one empty bucket from the input slot");
         helper.assertTrue(machine.getItem(1).is(Items.MILK_BUCKET), "small milking machine should place the milk bucket in the milk result slot");
      });
   }

   @GameTest(template = "empty", timeoutTicks = 180)
   public static void small_milking_machine_bucket_output_is_not_blocked_by_full_tank(GameTestHelper helper) {
      helper.assertTrue(NeoForgeMod.MILK.isBound(), "NeoForge milk fluid should be enabled for milking machine tests");
      helper.setBlock(TEST_POS, Blocks.AIR);
      helper.setBlock(
         TEST_POS,
         (BlockState)((MachineBlocks.MilkingMachineBlock)ModBlocks.SMALL_MILKING_MACHINE.get())
            .defaultBlockState()
            .setValue(MachineBlocks.HORIZONTAL_FACING, Direction.EAST)
      );
      BlockEntity fullTankEntity = helper.getLevel().getBlockEntity(helper.absolutePos(TEST_POS));
      helper.assertTrue(fullTankEntity instanceof MachineBlockEntity, "small milking machine should create a machine block entity");
      MachineBlockEntity fullTankMachine = (MachineBlockEntity)fullTankEntity;
      IFluidHandler fullTankHandler = fullTankMachine.fluidHandler(Direction.UP);
      helper.assertTrue(fullTankHandler != null, "small milking machine should expose a fluid handler");
      helper.assertValueEqual(
         4000, fullTankHandler.fill(new FluidStack(NeoForgeMod.MILK.value(), 4000), FluidAction.EXECUTE), "test setup should fill the milk tank"
      );
      fullTankMachine.setItem(0, new ItemStack(Items.BUCKET));
      helper.spawn(EntityType.COW, TEST_POS.east());
      tickMachine(helper, fullTankMachine, 120);
      helper.assertTrue(fullTankMachine.getItem(1).is(Items.MILK_BUCKET), "full tank should not block direct bucket milking");
      helper.assertValueEqual(4000, fullTankHandler.getFluidInTank(0).getAmount(), "bucket milking should not overflow or drain the full tank");

      BlockPos blockedPos = TEST_POS.offset(5, 0, 0);
      helper.setBlock(blockedPos, Blocks.AIR);
      helper.setBlock(
         blockedPos,
         (BlockState)((MachineBlocks.MilkingMachineBlock)ModBlocks.SMALL_MILKING_MACHINE.get())
            .defaultBlockState()
            .setValue(MachineBlocks.HORIZONTAL_FACING, Direction.EAST)
      );
      BlockEntity blockedTankEntity = helper.getLevel().getBlockEntity(helper.absolutePos(blockedPos));
      helper.assertTrue(blockedTankEntity instanceof MachineBlockEntity, "small milking machine should create a second machine block entity");
      MachineBlockEntity blockedTankMachine = (MachineBlockEntity)blockedTankEntity;
      IFluidHandler blockedTankHandler = blockedTankMachine.fluidHandler(Direction.UP);
      helper.assertTrue(blockedTankHandler != null, "second small milking machine should expose a fluid handler");
      helper.assertValueEqual(
         3500,
         blockedTankHandler.fill(new FluidStack(NeoForgeMod.MILK.value(), 3500), FluidAction.EXECUTE),
         "test setup should leave less than one bucket of tank space"
      );
      helper.spawn(EntityType.COW, blockedPos.east());
      tickMachine(helper, blockedTankMachine, 120);
      helper.assertValueEqual(3500, blockedTankHandler.getFluidInTank(0).getAmount(), "blocked tank should not accept a partial milk cycle");
      helper.assertTrue(
         !(Boolean)helper.getBlockState(blockedPos).getValue(MachineBlocks.ACTIVE),
         "milking machine should not show active when no bucket or tank output can accept milk"
      );
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 220)
   public static void small_milking_machine_automates_buckets_with_back_and_bottom_inventories(GameTestHelper helper) {
      helper.setBlock(TEST_POS, Blocks.AIR);
      helper.setBlock(TEST_POS.west(), Blocks.CHEST.defaultBlockState());
      helper.setBlock(TEST_POS.below(), Blocks.CHEST.defaultBlockState());
      helper.setBlock(
         TEST_POS,
         (BlockState)((MachineBlocks.MilkingMachineBlock)ModBlocks.SMALL_MILKING_MACHINE.get())
            .defaultBlockState()
            .setValue(MachineBlocks.HORIZONTAL_FACING, Direction.EAST)
      );
      BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(TEST_POS));
      BlockEntity inputEntity = helper.getLevel().getBlockEntity(helper.absolutePos(TEST_POS.west()));
      BlockEntity outputEntity = helper.getLevel().getBlockEntity(helper.absolutePos(TEST_POS.below()));
      helper.assertTrue(blockEntity instanceof MachineBlockEntity, "small milking machine should create a machine block entity");
      helper.assertTrue(inputEntity instanceof Container, "back chest should expose empty buckets to the milking machine");
      helper.assertTrue(outputEntity instanceof Container, "bottom chest should accept filled milk buckets from the milking machine");
      MachineBlockEntity machine = (MachineBlockEntity)blockEntity;
      Container input = (Container)inputEntity;
      Container output = (Container)outputEntity;
      input.setItem(0, new ItemStack(Items.BUCKET));
      helper.spawn(EntityType.COW, TEST_POS.east());
      helper.succeedWhen(() -> {
         tickMachine(helper, machine, 20);
         helper.assertTrue(input.getItem(0).isEmpty(), "small milking machine should pull an empty bucket from the back inventory");
         helper.assertTrue(machine.getItem(0).isEmpty(), "small milking machine should consume the pulled empty bucket while milking");
         helper.assertTrue(machine.getItem(1).isEmpty(), "small milking machine should push the filled milk bucket out of its result slot");
         helper.assertTrue(output.getItem(0).is(Items.MILK_BUCKET), "small milking machine should insert filled milk buckets into the bottom inventory");
      });
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void small_milking_machine_exposes_milk_fluid_to_pipes(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.SMALL_MILKING_MACHINE, MachineKind.SMALL_MILKING_MACHINE);
      IFluidHandler fluidHandler = machine.fluidHandler(Direction.DOWN);
      helper.assertTrue(fluidHandler != null, "small milking machine should expose a fluid handler");
      helper.assertTrue(NeoForgeMod.MILK.isBound(), "NeoForge milk fluid should be enabled for milking machine pipe output");
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MILK_BUCKET));
      boolean handled = machine.handleItemUse(player.getItemInHand(InteractionHand.MAIN_HAND), player, InteractionHand.MAIN_HAND);
      helper.assertTrue(handled, "small milking machine should accept a milk bucket into its internal tank");
      helper.assertTrue(fluidHandler.getFluidInTank(0).is(NeoForgeMod.MILK.value()), "small milking machine fluid handler should expose stored milk");
      helper.assertValueEqual(1000, fluidHandler.getFluidInTank(0).getAmount(), "small milking machine should store one bucket of milk");
      FluidStack drained = fluidHandler.drain(250, FluidAction.EXECUTE);
      helper.assertTrue(drained.is(NeoForgeMod.MILK.value()), "small milking machine fluid handler should drain milk");
      helper.assertValueEqual(250, drained.getAmount(), "small milking machine fluid handler should drain the requested milk amount");
      helper.assertValueEqual(750, fluidHandler.getFluidInTank(0).getAmount(), "small milking machine should keep remaining milk after fluid drain");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 100)
   public static void small_milking_machine_pushes_milk_fluid_to_tank_below(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      MachineBlockEntity tank = placeMachineAt(helper, TEST_POS.below(), ModBlocks.FLUID_BARREL, MachineKind.FLUID_BARREL);
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.SMALL_MILKING_MACHINE, MachineKind.SMALL_MILKING_MACHINE);
      IFluidHandler tankHandler = tank.fluidHandler(Direction.UP);
      IFluidHandler machineHandler = machine.fluidHandler(Direction.DOWN);
      helper.assertTrue(tankHandler != null, "fluid barrel below milking machine should expose a fluid handler");
      helper.assertTrue(machineHandler != null, "small milking machine should expose a fluid handler");
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MILK_BUCKET));
      helper.assertTrue(
         machine.handleItemUse(player.getItemInHand(InteractionHand.MAIN_HAND), player, InteractionHand.MAIN_HAND),
         "small milking machine should accept milk before pushing to the tank below"
      );
      tickMachine(helper, machine, 20);
      helper.assertTrue(tankHandler.getFluidInTank(0).is(NeoForgeMod.MILK.value()), "tank below should receive milk fluid from the milking machine");
      helper.assertValueEqual(250, tankHandler.getFluidInTank(0).getAmount(), "milking machine should push one fluid transfer interval downward");
      helper.assertValueEqual(750, machineHandler.getFluidInTank(0).getAmount(), "milking machine should keep remaining milk after downward transfer");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void specialized_fluid_machines_reject_wrong_fluid_types(GameTestHelper helper) {
      helper.assertTrue(NeoForgeMod.MILK.isBound(), "NeoForge milk fluid should be enabled for fluid validation");
      MachineBlockEntity milkingMachine = placeMachineAt(helper, TEST_POS, ModBlocks.SMALL_MILKING_MACHINE, MachineKind.SMALL_MILKING_MACHINE);
      IFluidHandler milkingHandler = milkingMachine.fluidHandler(Direction.UP);
      helper.assertTrue(milkingHandler != null, "small milking machine should expose a fluid handler");
      helper.assertTrue(!milkingMachine.canPreviewItemUse(new ItemStack(Items.WATER_BUCKET)), "small milking machine should not preview water insertion");
      helper.assertTrue(!milkingMachine.canPreviewItemUse(new ItemStack(Items.LAVA_BUCKET)), "small milking machine should not preview lava insertion");
      helper.assertTrue(milkingMachine.canPreviewItemUse(new ItemStack(Items.MILK_BUCKET)), "small milking machine should preview milk insertion");
      helper.assertValueEqual(0, milkingHandler.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE), "small milking machine should reject water pipes");
      helper.assertValueEqual(0, milkingHandler.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE), "small milking machine should reject lava pipes");
      helper.assertValueEqual(
         1000,
         milkingHandler.fill(new FluidStack(NeoForgeMod.MILK.value(), 1000), FluidAction.EXECUTE),
         "small milking machine should accept milk pipes"
      );
      MachineBlockEntity mineralSmelter = placeMachineAt(
         helper, TEST_POS.offset(3, 0, 0), ModBlocks.SMALL_MINERAL_SMELTER, MachineKind.SMALL_MINERAL_SMELTER
      );
      IFluidHandler mineralHandler = mineralSmelter.fluidHandler(Direction.UP);
      helper.assertTrue(mineralHandler != null, "small mineral smelter should expose a fluid handler");
      helper.assertTrue(!mineralSmelter.canPreviewItemUse(new ItemStack(Items.WATER_BUCKET)), "small mineral smelter should not preview water insertion");
      helper.assertTrue(mineralSmelter.canPreviewItemUse(new ItemStack(Items.LAVA_BUCKET)), "small mineral smelter should preview lava insertion");
      helper.assertValueEqual(0, mineralHandler.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE), "small mineral smelter should reject water pipes");
      helper.assertValueEqual(
         0,
         mineralHandler.fill(new FluidStack(NeoForgeMod.MILK.value(), 1000), FluidAction.EXECUTE),
         "small mineral smelter should reject milk pipes"
      );
      helper.assertValueEqual(1000, mineralHandler.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE), "small mineral smelter should accept lava pipes");
      MachineBlockEntity accumulator = placeMachineAt(
         helper, TEST_POS.offset(6, 0, 0), ModBlocks.PASSIVE_FLUID_ACCUMULATOR, MachineKind.PASSIVE_FLUID_ACCUMULATOR
      );
      IFluidHandler accumulatorHandler = accumulator.fluidHandler(Direction.UP);
      helper.assertTrue(accumulatorHandler != null, "passive fluid accumulator should expose a fluid handler");
      helper.assertValueEqual(0, accumulatorHandler.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE), "passive fluid accumulator should reject lava pipes");
      helper.assertValueEqual(
         0,
         accumulatorHandler.fill(new FluidStack(NeoForgeMod.MILK.value(), 1000), FluidAction.EXECUTE),
         "passive fluid accumulator should reject milk pipes"
      );
      helper.assertValueEqual(1000, accumulatorHandler.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE), "passive fluid accumulator should accept water pipes");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void fluid_machine_menus_report_internal_tank_capacity(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      MachineBlockEntity barrel = placeMachine(helper, ModBlocks.FLUID_BARREL, MachineKind.FLUID_BARREL);
      MachineMenu barrelMenu = new MachineMenu(MachineKind.FLUID_BARREL, 1, player.getInventory(), barrel, barrel.dataAccessForTests());
      barrel.dataAccessForTests().set(2, 1000);
      helper.assertValueEqual(1000, barrelMenu.fluidAmount(), "fluid barrel menu should report synced tank amount");
      helper.assertValueEqual(4000, barrelMenu.fluidCapacity(), "fluid barrel menu should expose its full tank capacity");
      barrelMenu.removed(player);
      MachineBlockEntity funnel = placeMachineAt(helper, TEST_POS.offset(3, 0, 0), ModBlocks.SMALL_FLUID_FUNNEL, MachineKind.SMALL_FLUID_FUNNEL);
      MachineMenu funnelMenu = new MachineMenu(MachineKind.SMALL_FLUID_FUNNEL, 2, player.getInventory(), funnel, funnel.dataAccessForTests());
      funnel.dataAccessForTests().set(2, 1000);
      helper.assertValueEqual(1000, funnelMenu.fluidAmount(), "small fluid funnel menu should report synced tank amount");
      helper.assertValueEqual(3000, funnelMenu.fluidCapacity(), "small fluid funnel menu should expose its three-bucket capacity");
      funnelMenu.removed(player);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void fluid_barrel_fills_bucket_from_gui_slot(GameTestHelper helper) {
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.FLUID_BARREL, MachineKind.FLUID_BARREL);
      IFluidHandler fluidHandler = machine.fluidHandler(Direction.UP);
      helper.assertTrue(fluidHandler != null, "fluid barrel should expose a fluid handler");
      helper.assertValueEqual(
         1000,
         fluidHandler.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE),
         "fluid barrel should accept one bucket of water before filling a bucket item"
      );
      machine.setItem(0, new ItemStack(Items.BUCKET));
      tickMachine(helper, machine, 1);
      helper.assertTrue(machine.getItem(0).isEmpty(), "fluid barrel should consume the empty bucket from slot 0");
      helper.assertTrue(machine.getItem(1).is(Items.WATER_BUCKET), "fluid barrel should place the filled water bucket in result slot 1");
      helper.assertValueEqual(0, fluidHandler.getFluidInTank(0).getAmount(), "fluid barrel should drain one bucket from its internal tank");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void fluid_barrel_creative_direct_empty_bucket_receives_filled_bucket(GameTestHelper helper) {
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.FLUID_BARREL, MachineKind.FLUID_BARREL);
      IFluidHandler fluidHandler = machine.fluidHandler(Direction.UP);
      helper.assertTrue(fluidHandler != null, "fluid barrel should expose a fluid handler");
      helper.assertValueEqual(
         1000,
         fluidHandler.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE),
         "fluid barrel should accept one bucket of water before direct bucket use"
      );
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      player.getAbilities().instabuild = true;
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
      boolean handled = machine.handleItemUse(player.getItemInHand(InteractionHand.MAIN_HAND), player, InteractionHand.MAIN_HAND);
      helper.assertTrue(handled, "creative empty bucket use should fill a bucket from the fluid barrel");
      helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET), "creative empty bucket use should keep the held empty bucket");
      helper.assertValueEqual(
         1,
         countItem(player.getInventory(), Items.WATER_BUCKET),
         "creative empty bucket use should give the filled water bucket instead of deleting the drained fluid"
      );
      helper.assertValueEqual(0, fluidHandler.getFluidInTank(0).getAmount(), "creative empty bucket use should drain one bucket from the internal tank");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void direct_filled_bucket_use_updates_fluid_block_state(GameTestHelper helper) {
      helper.assertTrue(NeoForgeMod.MILK.isBound(), "NeoForge milk fluid should be enabled for fluid bucket state checks");
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      MachineBlockEntity barrel = placeMachine(helper, ModBlocks.FLUID_BARREL, MachineKind.FLUID_BARREL);
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
      helper.assertTrue(
         barrel.handleItemUse(player.getItemInHand(InteractionHand.MAIN_HAND), player, InteractionHand.MAIN_HAND),
         "fluid barrel should accept a directly used water bucket"
      );
      helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET), "direct water insertion should return an empty bucket");
      helper.assertValueEqual(
         1,
         helper.getBlockState(TEST_POS).getValue(MachineBlocks.LEVEL_0_4),
         "fluid barrel direct bucket insertion should update the visible fluid level"
      );

      BlockPos funnelPos = TEST_POS.offset(3, 0, 0);
      MachineBlockEntity funnel = placeMachineAt(helper, funnelPos, ModBlocks.SMALL_FLUID_FUNNEL, MachineKind.SMALL_FLUID_FUNNEL);
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LAVA_BUCKET));
      helper.assertTrue(
         funnel.handleItemUse(player.getItemInHand(InteractionHand.MAIN_HAND), player, InteractionHand.MAIN_HAND),
         "small fluid funnel should accept a directly used lava bucket"
      );
      helper.assertValueEqual(
         1,
         helper.getBlockState(funnelPos).getValue(MachineBlocks.LEVEL_0_3),
         "small fluid funnel direct bucket insertion should update the visible fluid level"
      );

      BlockPos milkingPos = TEST_POS.offset(6, 0, 0);
      MachineBlockEntity milkingMachine = placeMachineAt(
         helper, milkingPos, ModBlocks.SMALL_MILKING_MACHINE, MachineKind.SMALL_MILKING_MACHINE
      );
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MILK_BUCKET));
      helper.assertTrue(
         milkingMachine.handleItemUse(player.getItemInHand(InteractionHand.MAIN_HAND), player, InteractionHand.MAIN_HAND),
         "small milking machine should accept a directly used milk bucket"
      );
      helper.assertTrue(
         (Boolean)helper.getBlockState(milkingPos).getValue(MachineBlocks.FILLED),
         "small milking machine direct milk insertion should update the filled block state"
      );
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void fluid_barrel_bucket_preview_matches_stored_fluid_state(GameTestHelper helper) {
      MachineBlockEntity machine = placeMachine(helper, ModBlocks.FLUID_BARREL, MachineKind.FLUID_BARREL);
      IFluidHandler fluidHandler = machine.fluidHandler(Direction.UP);
      helper.assertTrue(fluidHandler != null, "fluid barrel should expose a fluid handler");
      helper.assertTrue(machine.canPreviewItemUse(new ItemStack(Items.WATER_BUCKET)), "empty fluid barrel should preview valid water bucket insertion");
      helper.assertTrue(!machine.canPreviewItemUse(new ItemStack(Items.BUCKET)), "empty fluid barrel should not preview empty bucket extraction");
      helper.assertValueEqual(
         1000,
         fluidHandler.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE),
         "fluid barrel should accept one bucket of water before preview checks"
      );
      helper.assertTrue(machine.canPreviewItemUse(new ItemStack(Items.BUCKET)), "filled fluid barrel should preview empty bucket extraction");
      helper.assertTrue(machine.canPreviewItemUse(new ItemStack(Items.WATER_BUCKET)), "part-filled water barrel should preview matching water insertion");
      helper.assertTrue(!machine.canPreviewItemUse(new ItemStack(Items.LAVA_BUCKET)), "part-filled water barrel should not preview mixed lava insertion");
      helper.assertValueEqual(
         3000, fluidHandler.fill(new FluidStack(Fluids.WATER, 3000), FluidAction.EXECUTE), "fluid barrel should fill the remaining water capacity"
      );
      helper.assertTrue(!machine.canPreviewItemUse(new ItemStack(Items.WATER_BUCKET)), "full fluid barrel should not preview another filled bucket insertion");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 140)
   public static void small_block_breaker_processing_time_depends_on_target_hardness(GameTestHelper helper) {
      helper.setBlock(TEST_POS, Blocks.AIR);
      helper.setBlock(
         TEST_POS,
         (BlockState)((MachineBlocks.ActiveHorizontalMachineBlock)ModBlocks.SMALL_BLOCK_BREAKER.get())
            .defaultBlockState()
            .setValue(MachineBlocks.HORIZONTAL_FACING, Direction.EAST)
      );
      BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(TEST_POS));
      helper.assertTrue(blockEntity instanceof MachineBlockEntity, "small block breaker should create a machine block entity");
      MachineBlockEntity machine = (MachineBlockEntity)blockEntity;
      BlockPos target = TEST_POS.east();
      helper.setBlock(target, Blocks.DIRT);
      tickMachine(helper, machine, 30);
      helper.assertTrue(helper.getBlockState(target).isAir(), "small block breaker should finish soft targets quickly");
      helper.assertValueEqual(
         1,
         countItems(machine, Items.DIRT, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
         "small block breaker should route block drops into its internal drop buffer"
      );
      AABB dropSearch = new AABB(helper.absolutePos(TEST_POS)).inflate(3.0);
      helper.assertTrue(
         helper.getLevel().getEntitiesOfClass(ItemEntity.class, dropSearch, item -> item.getItem().is(Items.DIRT)).isEmpty(),
         "small block breaker should not spawn buffered drops into the world"
      );
      helper.setBlock(target, Blocks.STONE);
      tickMachine(helper, machine, 35);
      helper.assertTrue(
         helper.getBlockState(target).is(Blocks.STONE), "small block breaker should still be processing harder targets after the soft-target duration"
      );
      tickMachine(helper, machine, 30);
      helper.assertTrue(helper.getBlockState(target).isAir(), "small block breaker should break harder targets after the hardness-scaled work time");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void small_block_breaker_clears_active_state_when_drop_buffer_full(GameTestHelper helper) {
      helper.setBlock(TEST_POS, Blocks.AIR);
      helper.setBlock(
         TEST_POS,
         (BlockState)((MachineBlocks.ActiveHorizontalMachineBlock)ModBlocks.SMALL_BLOCK_BREAKER.get())
            .defaultBlockState()
            .setValue(MachineBlocks.HORIZONTAL_FACING, Direction.EAST)
      );
      BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(TEST_POS));
      helper.assertTrue(blockEntity instanceof MachineBlockEntity, "small block breaker should create a machine block entity");
      MachineBlockEntity machine = (MachineBlockEntity)blockEntity;

      for (int slot = 0; slot < 10; slot++) {
         machine.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
      }

      BlockPos target = TEST_POS.east();
      helper.setBlock(target, Blocks.DIRT);
      tickMachine(helper, machine, 31);
      helper.assertTrue(helper.getBlockState(target).is(Blocks.DIRT), "full drop buffer should leave the target block intact");
      helper.assertBlockProperty(TEST_POS, MachineBlocks.ACTIVE, false);
      helper.assertValueEqual(0, machine.dataAccessForTests().get(1), "blocked small block breaker should clear stale progress");
      helper.assertValueEqual(0, machine.dataAccessForTests().get(3), "blocked small block breaker should clear stale required work time");
      AABB dropSearch = new AABB(helper.absolutePos(TEST_POS)).inflate(3.0);
      helper.assertTrue(
         helper.getLevel().getEntitiesOfClass(ItemEntity.class, dropSearch, item -> item.getItem().is(Items.DIRT)).isEmpty(),
         "full drop buffer should not spill blocked target drops into the world"
      );
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 120)
   public static void small_tree_cutter_routes_log_drops_into_internal_buffer(GameTestHelper helper) {
      helper.setBlock(TEST_POS, Blocks.AIR);
      helper.setBlock(
         TEST_POS,
         (BlockState)((MachineBlocks.ActiveHorizontalMachineBlock)ModBlocks.SMALL_TREE_CUTTER.get())
            .defaultBlockState()
            .setValue(MachineBlocks.HORIZONTAL_FACING, Direction.EAST)
      );
      BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(TEST_POS));
      helper.assertTrue(blockEntity instanceof MachineBlockEntity, "small tree cutter should create a machine block entity");
      MachineBlockEntity machine = (MachineBlockEntity)blockEntity;
      BlockPos target = TEST_POS.east();
      helper.setBlock(target, Blocks.OAK_LOG);
      helper.setBlock(target.above(), Blocks.OAK_LOG);
      tickMachine(helper, machine, 41);
      helper.assertTrue(helper.getBlockState(target).isAir(), "small tree cutter should remove the first log");
      helper.assertTrue(helper.getBlockState(target.above()).isAir(), "small tree cutter should remove connected logs above the first one");
      helper.assertValueEqual(
         2,
         countItems(machine, Items.OAK_LOG, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
         "small tree cutter should route log drops into its internal drop buffer"
      );
      AABB dropSearch = new AABB(helper.absolutePos(TEST_POS)).inflate(4.0);
      helper.assertTrue(
         helper.getLevel().getEntitiesOfClass(ItemEntity.class, dropSearch, item -> item.getItem().is(Items.OAK_LOG)).isEmpty(),
         "small tree cutter should not spawn buffered log drops into the world"
      );
      helper.assertBlockProperty(TEST_POS, MachineBlocks.ACTIVE, true);
      helper.setBlock(TEST_POS.south(), Blocks.REDSTONE_BLOCK);
      tickMachine(helper, machine, 1);
      helper.assertBlockProperty(TEST_POS, MachineBlocks.ACTIVE, false);
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void fluid_barrel_default_placement_is_standing(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      Block block = (Block)ModBlocks.FLUID_BARREL.get();
      BlockPos absolutePos = helper.absolutePos(TEST_POS);
      BlockHitResult hit = new BlockHitResult(
         new Vec3(absolutePos.getX() + 0.5, absolutePos.getY() + 0.5, absolutePos.getZ() + 0.5), Direction.NORTH, absolutePos, false
      );
      BlockPlaceContext context = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, new ItemStack(block), hit);
      BlockState state = block.getStateForPlacement(context);
      helper.assertTrue(state != null, "fluid barrel placement state should not be null");
      helper.assertValueEqual(
         Direction.UP, (Direction)state.getValue(MachineBlocks.FACING), "fluid barrel should stand upright unless the player uses secondary placement"
      );
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void labeled_crate_secondary_placement_controls_label_direction(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      Block block = (Block)ModBlocks.LABELED_CRATE.get();
      BlockPos absolutePos = helper.absolutePos(TEST_POS);
      BlockHitResult hit = new BlockHitResult(
         new Vec3(absolutePos.getX() + 0.5, absolutePos.getY() + 0.5, absolutePos.getZ() + 0.5), Direction.NORTH, absolutePos, false
      );
      BlockPlaceContext normalContext = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, new ItemStack(block), hit);
      BlockState normalState = block.getStateForPlacement(normalContext);
      helper.assertTrue(normalState != null, "labeled crate normal placement state should not be null");
      helper.assertValueEqual(
         normalContext.getHorizontalDirection().getOpposite(),
         (Direction)normalState.getValue(MachineBlocks.HORIZONTAL_FACING),
         "normal labeled crate placement should face the label back toward the player"
      );
      BlockPlaceContext secondaryContext = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, new ItemStack(block), hit) {
         public boolean isSecondaryUseActive() {
            return true;
         }
      };
      BlockState secondaryState = block.getStateForPlacement(secondaryContext);
      helper.assertTrue(secondaryState != null, "labeled crate secondary placement state should not be null");
      helper.assertValueEqual(
         secondaryContext.getHorizontalDirection(),
         (Direction)secondaryState.getValue(MachineBlocks.HORIZONTAL_FACING),
         "secondary labeled crate placement should face the label in the player look direction"
      );
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 100)
   public static void standing_fluid_barrel_transfers_to_tank_below(GameTestHelper helper) {
      MachineBlockEntity lowerBarrel = placeMachineAt(helper, TEST_POS, ModBlocks.FLUID_BARREL, MachineKind.FLUID_BARREL);
      helper.setBlock(
         TEST_POS.above(),
         (BlockState)((MachineBlocks.LevelDirectionalMachineBlock)ModBlocks.FLUID_BARREL.get())
            .defaultBlockState()
            .setValue(MachineBlocks.FACING, Direction.UP)
      );
      BlockEntity upperEntity = helper.getLevel().getBlockEntity(helper.absolutePos(TEST_POS.above()));
      helper.assertTrue(upperEntity instanceof MachineBlockEntity, "upper fluid barrel should create a machine block entity");
      MachineBlockEntity upperBarrel = (MachineBlockEntity)upperEntity;
      IFluidHandler lowerHandler = lowerBarrel.fluidHandler(Direction.UP);
      IFluidHandler upperHandler = upperBarrel.fluidHandler(Direction.UP);
      helper.assertTrue(lowerHandler != null, "lower fluid barrel should expose a fluid handler");
      helper.assertTrue(upperHandler != null, "upper fluid barrel should expose a fluid handler");
      helper.assertValueEqual(
         1000, upperHandler.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE), "standing fluid barrel should accept water before gravity transfer"
      );
      tickMachine(helper, upperBarrel, 40);
      helper.assertTrue(lowerHandler.getFluidInTank(0).is(Fluids.WATER), "standing fluid barrel should transfer water into a tank below");
      helper.assertValueEqual(250, lowerHandler.getFluidInTank(0).getAmount(), "standing fluid barrel should transfer one interval of water downward");
      helper.assertValueEqual(750, upperHandler.getFluidInTank(0).getAmount(), "standing fluid barrel should keep remaining water after downward transfer");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 100)
   public static void small_fluid_funnel_transfers_to_tank_below(GameTestHelper helper) {
      MachineBlockEntity barrel = placeMachineAt(helper, TEST_POS, ModBlocks.FLUID_BARREL, MachineKind.FLUID_BARREL);
      MachineBlockEntity funnel = placeMachineAt(helper, TEST_POS.above(), ModBlocks.SMALL_FLUID_FUNNEL, MachineKind.SMALL_FLUID_FUNNEL);
      IFluidHandler barrelHandler = barrel.fluidHandler(Direction.UP);
      IFluidHandler funnelHandler = funnel.fluidHandler(Direction.UP);
      helper.assertTrue(barrelHandler != null, "fluid barrel should expose a fluid handler");
      helper.assertTrue(funnelHandler != null, "small fluid funnel should expose a fluid handler");
      helper.assertValueEqual(
         1000, funnelHandler.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE), "small fluid funnel should accept water before gravity transfer"
      );
      tickMachine(helper, funnel, 40);
      helper.assertTrue(barrelHandler.getFluidInTank(0).is(Fluids.WATER), "small fluid funnel should transfer water into a tank below");
      helper.assertValueEqual(250, barrelHandler.getFluidInTank(0).getAmount(), "small fluid funnel should transfer one interval of water downward");
      helper.assertValueEqual(750, funnelHandler.getFluidInTank(0).getAmount(), "small fluid funnel should keep remaining water after downward transfer");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 120)
   public static void factory_hopper_transfer_respects_target_item_stack_limit(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      helper.setBlock(TEST_POS, Blocks.AIR);
      helper.setBlock(TEST_POS.east(), Blocks.CHEST.defaultBlockState());
      helper.setBlock(
         TEST_POS,
         (BlockState)((MachineBlocks.DirectionalMachineBlock)ModBlocks.FACTORY_HOPPER.get()).defaultBlockState().setValue(MachineBlocks.FACING, Direction.EAST)
      );
      BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(TEST_POS));
      BlockEntity targetEntity = helper.getLevel().getBlockEntity(helper.absolutePos(TEST_POS.east()));
      helper.assertTrue(blockEntity instanceof MachineBlockEntity, "factory hopper should create a machine block entity");
      helper.assertTrue(targetEntity instanceof Container, "target chest should expose a vanilla container");
      MachineBlockEntity machine = (MachineBlockEntity)blockEntity;
      Container target = (Container)targetEntity;
      MachineMenu menu = new MachineMenu(MachineKind.FACTORY_HOPPER, 6, player.getInventory(), machine, machine.dataAccessForTests());
      machine.setItem(0, new ItemStack(Items.SNOWBALL, 16));
      menu.handleAction(player, 2, 32, 0);
      menu.handleAction(player, 5, 1, 0);
      tickMachine(helper, machine, 12);
      int targetSlot0 = target.getItem(0).getCount();
      int remaining = machine.getItem(0).getCount();
      helper.assertTrue(
         targetSlot0 == 16,
         "factory hopper should transfer one legal snowball stack without overfilling target slot 0; slot0=" + targetSlot0 + ", remaining=" + remaining
      );
      helper.assertTrue(
         remaining == 0,
         "factory hopper should not lose or retain items from a legal snowball stack transfer; slot0=" + targetSlot0 + ", remaining=" + remaining
      );
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 120)
   public static void factory_hopper_collection_updates_adjacent_comparator(GameTestHelper helper) {
      BlockPos comparatorPos = TEST_POS.east();
      helper.setBlock(TEST_POS, Blocks.AIR);
      helper.setBlock(comparatorPos.below(), Blocks.STONE);
      helper.setBlock(comparatorPos, (BlockState)Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.WEST));
      helper.setBlock(
         TEST_POS,
         (BlockState)((MachineBlocks.DirectionalMachineBlock)ModBlocks.FACTORY_HOPPER.get()).defaultBlockState().setValue(MachineBlocks.FACING, Direction.UP)
      );
      BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(TEST_POS));
      helper.assertTrue(blockEntity instanceof MachineBlockEntity, "factory hopper should create a machine block entity");
      MachineBlockEntity machine = (MachineBlockEntity)blockEntity;
      helper.assertValueEqual(
         0,
         helper.getLevel().getSignal(helper.absolutePos(comparatorPos), Direction.EAST),
         "empty factory hopper should start with no comparator signal"
      );
      BlockPos itemPos = helper.absolutePos(TEST_POS.above());
      ItemEntity looseItem = new ItemEntity(
         helper.getLevel(), itemPos.getX() + 0.5, itemPos.getY() + 0.5, itemPos.getZ() + 0.5, new ItemStack(Items.COBBLESTONE)
      );
      helper.getLevel().addFreshEntity(looseItem);
      tickMachine(helper, machine, 10);
      helper.assertTrue(machine.getItem(0).is(Items.COBBLESTONE), "factory hopper should collect the loose item into inventory");
      helper.runAfterDelay(4L, () -> {
         helper.assertTrue(machine.getItem(0).is(Items.COBBLESTONE), "factory hopper should collect the loose item into inventory");
         BlockEntity comparatorEntity = helper.getLevel().getBlockEntity(helper.absolutePos(comparatorPos));
         helper.assertTrue(comparatorEntity instanceof ComparatorBlockEntity, "factory hopper test comparator should keep its block entity");
         helper.assertTrue(((ComparatorBlockEntity)comparatorEntity).getOutputSignal() > 0, "factory hopper collection should refresh adjacent comparator output");
         helper.succeed();
      });
   }

   @GameTest(template = "empty", timeoutTicks = 120)
   public static void factory_hopper_pulse_mode_respects_redstone_before_collecting_items(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      helper.setBlock(TEST_POS, Blocks.AIR);
      helper.setBlock(
         TEST_POS,
         (BlockState)((MachineBlocks.DirectionalMachineBlock)ModBlocks.FACTORY_HOPPER.get()).defaultBlockState().setValue(MachineBlocks.FACING, Direction.UP)
      );
      BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(TEST_POS));
      helper.assertTrue(blockEntity instanceof MachineBlockEntity, "factory hopper should create a machine block entity");
      MachineBlockEntity machine = (MachineBlockEntity)blockEntity;
      MachineMenu menu = new MachineMenu(MachineKind.FACTORY_HOPPER, 7, player.getInventory(), machine, machine.dataAccessForTests());
      menu.handleAction(player, 3, 0, 0);
      BlockPos itemPos = helper.absolutePos(TEST_POS.above());
      ItemEntity looseItem = new ItemEntity(
         helper.getLevel(), itemPos.getX() + 0.5, itemPos.getY() + 0.5, itemPos.getZ() + 0.5, new ItemStack(Items.COBBLESTONE)
      );
      helper.getLevel().addFreshEntity(looseItem);
      tickMachine(helper, machine, 20);
      helper.assertTrue(machine.getItem(0).isEmpty(), "factory hopper pulse mode should not collect loose items while redstone is inactive");
      helper.assertTrue(looseItem.isAlive(), "redstone-disabled pulse hopper should leave the loose item in the world");
      menu.handleAction(player, 5, 1, 0);
      tickMachine(helper, machine, 12);
      helper.assertTrue(machine.getItem(0).is(Items.COBBLESTONE), "factory hopper manual trigger should collect loose items before transfer");
      helper.assertTrue(!looseItem.isAlive(), "factory hopper manual trigger should remove the collected loose item entity");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 160)
   public static void factory_hopper_pulse_mode_requires_new_redstone_edge_before_collection(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      helper.setBlock(TEST_POS, Blocks.AIR);
      helper.setBlock(TEST_POS.south(), Blocks.REDSTONE_BLOCK);
      helper.setBlock(
         TEST_POS,
         (BlockState)((MachineBlocks.DirectionalMachineBlock)ModBlocks.FACTORY_HOPPER.get()).defaultBlockState().setValue(MachineBlocks.FACING, Direction.UP)
      );
      BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(TEST_POS));
      helper.assertTrue(blockEntity instanceof MachineBlockEntity, "factory hopper should create a machine block entity");
      MachineBlockEntity machine = (MachineBlockEntity)blockEntity;
      MachineMenu menu = new MachineMenu(MachineKind.FACTORY_HOPPER, 7, player.getInventory(), machine, machine.dataAccessForTests());
      menu.handleAction(player, 3, 0, 0);
      tickMachine(helper, machine, 12);

      BlockPos itemPos = helper.absolutePos(TEST_POS.above());
      ItemEntity looseItem = new ItemEntity(
         helper.getLevel(), itemPos.getX() + 0.5, itemPos.getY() + 0.5, itemPos.getZ() + 0.5, new ItemStack(Items.COBBLESTONE)
      );
      helper.getLevel().addFreshEntity(looseItem);
      tickMachine(helper, machine, 20);
      helper.assertTrue(machine.getItem(0).isEmpty(), "factory hopper pulse mode should ignore held redstone power without a new edge");
      helper.assertTrue(looseItem.isAlive(), "held-power pulse hopper should leave the loose item in the world until the next edge");

      helper.setBlock(TEST_POS.south(), Blocks.AIR);
      tickMachine(helper, machine, 12);
      helper.assertTrue(machine.getItem(0).isEmpty(), "factory hopper pulse mode should not collect on a redstone falling edge when non-inverted");
      helper.setBlock(TEST_POS.south(), Blocks.REDSTONE_BLOCK);
      tickMachine(helper, machine, 12);
      helper.assertTrue(machine.getItem(0).is(Items.COBBLESTONE), "factory hopper pulse mode should collect loose items on the next rising edge");
      helper.assertTrue(!looseItem.isAlive(), "factory hopper pulse mode should remove the collected loose item entity after a new edge");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void fluid_machines_expose_modded_fluid_handler_capability(GameTestHelper helper) {
      placeMachine(helper, ModBlocks.FLUID_BARREL, MachineKind.FLUID_BARREL);
      IFluidHandler handler = (IFluidHandler)helper.getLevel().getCapability(FluidHandler.BLOCK, helper.absolutePos(TEST_POS), Direction.UP);
      helper.assertTrue(handler != null, "fluid barrel should expose a fluid handler capability for modded pipes");
      helper.assertValueEqual(1, handler.getTanks(), "fluid barrel should expose one tank");
      helper.assertValueEqual(4000, handler.getTankCapacity(0), "fluid barrel tank should expose its internal capacity");
      helper.assertValueEqual(
         1000, handler.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE), "fluid barrel should accept water through the fluid capability"
      );
      helper.assertTrue(handler.getFluidInTank(0).is(Fluids.WATER), "fluid barrel should store inserted water");
      helper.assertValueEqual(250, handler.drain(250, FluidAction.EXECUTE).getAmount(), "fluid barrel should drain through the fluid capability");
      helper.assertValueEqual(750, handler.getFluidInTank(0).getAmount(), "fluid barrel should keep remaining water after drain");
      helper.assertValueEqual(
         0, handler.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE), "fluid barrel should reject mixing lava into stored water"
      );
      IFluidHandler passiveAccumulator = placeMachine(helper, ModBlocks.PASSIVE_FLUID_ACCUMULATOR, MachineKind.PASSIVE_FLUID_ACCUMULATOR)
         .fluidHandler(Direction.UP);
      helper.assertTrue(passiveAccumulator != null, "passive fluid accumulator should expose a direct fluid handler");
      helper.assertValueEqual(
         1000,
         passiveAccumulator.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE),
         "passive fluid accumulator should accept water through the fluid capability"
      );
      helper.assertTrue(passiveAccumulator.getFluidInTank(0).is(Fluids.WATER), "passive fluid accumulator should report inserted water");
      helper.succeed();
   }

   private static void assertRecipe(GameTestHelper helper, String name) {
      ResourceLocation id = ResourceLocation.fromNamespaceAndPath("immersive_engineer_decor_controls_tool_reforged", name);
      helper.assertTrue(helper.getLevel().getRecipeManager().byKey(id).isPresent(), "missing recipe " + id);
   }

   private static void assertLabeledCrateLabelBehavior(GameTestHelper helper, MachineBlockEntity machine) {
      String longLine = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
      machine.setLabeledCrateLabel("Bolts", longLine);
      helper.assertValueEqual("Bolts", machine.labelLine(0), "labeled crate should store first label line");
      helper.assertValueEqual(longLine.substring(0, 24), machine.labelLine(1), "labeled crate should trim long label lines safely");
      CompoundTag saved = machine.saveWithFullMetadata(helper.getLevel().registryAccess());
      MachineBlockEntity loaded = new MachineBlockEntity(machine.getBlockPos(), machine.getBlockState());
      loaded.loadWithComponents(saved, helper.getLevel().registryAccess());
      helper.assertValueEqual(machine.labelLine(0), loaded.labelLine(0), "labeled crate should reload first label line from saved NBT");
      helper.assertValueEqual(machine.labelLine(1), loaded.labelLine(1), "labeled crate should reload second label line from saved NBT");
      CompoundTag updateTag = machine.getUpdateTag(helper.getLevel().registryAccess());
      MachineBlockEntity synced = new MachineBlockEntity(machine.getBlockPos(), machine.getBlockState());
      synced.loadWithComponents(updateTag, helper.getLevel().registryAccess());
      helper.assertValueEqual(machine.labelLine(0), synced.labelLine(0), "labeled crate update tag should sync first label line");
      helper.assertValueEqual(machine.labelLine(1), synced.labelLine(1), "labeled crate update tag should sync second label line");
      BlockPos pos = TEST_POS;
      BlockState state = (BlockState)((MachineBlocks.LabeledCrateBlock)ModBlocks.LABELED_CRATE.get())
         .defaultBlockState()
         .setValue(MachineBlocks.HORIZONTAL_FACING, Direction.NORTH);
      BlockHitResult labelHit = new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY() + 0.47, pos.getZ()), Direction.NORTH, pos, false);
      BlockHitResult outsideHit = new BlockHitResult(new Vec3(pos.getX() + 0.1, pos.getY() + 0.47, pos.getZ()), Direction.NORTH, pos, false);
      BlockHitResult wrongFaceHit = new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY() + 0.47, pos.getZ() + 1.0), Direction.SOUTH, pos, false);
      helper.assertTrue(MachineBlocks.LabeledCrateBlock.isLabelHit(state, labelHit), "label plate hit should open label editing");
      helper.assertTrue(!MachineBlocks.LabeledCrateBlock.isLabelHit(state, outsideHit), "outside crate face hit should keep normal crate behavior");
      helper.assertTrue(!MachineBlocks.LabeledCrateBlock.isLabelHit(state, wrongFaceHit), "non-label face hit should keep normal crate behavior");
   }

   private static void assertMenuEnergyCapacity(
      GameTestHelper helper,
      Player player,
      BlockPos pos,
      DeferredBlock<? extends MachineBlocks.MachineBlock> block,
      MachineKind kind,
      int expectedCapacity
   ) {
      MachineBlockEntity machine = placeMachineAt(helper, pos, block, kind);
      MachineMenu menu = new MachineMenu(kind, 1, player.getInventory(), machine, machine.dataAccessForTests());
      helper.assertValueEqual(expectedCapacity, menu.energyCapacity(), kind.registryName() + " menu should expose its full FE capacity");
      menu.removed(player);
   }

   private static void assertMenuEnergyStored(
      GameTestHelper helper,
      Player player,
      BlockPos pos,
      DeferredBlock<? extends MachineBlocks.MachineBlock> block,
      MachineKind kind,
      int storedEnergy
   ) {
      MachineBlockEntity machine = placeMachineAt(helper, pos, block, kind);
      IEnergyStorage energyStorage = machine.energyStorage(null);
      helper.assertTrue(energyStorage != null, kind.registryName() + " should expose FE storage");
      helper.assertValueEqual(storedEnergy, energyStorage.receiveEnergy(storedEnergy, false), kind.registryName() + " test setup should charge FE storage");
      MachineMenu menu = new MachineMenu(kind, 1, player.getInventory(), machine, machine.dataAccessForTests());
      helper.assertValueEqual(storedEnergy, menu.energyStored(), kind.registryName() + " menu should expose its synced stored FE");
      menu.removed(player);
   }

   private static void assertMenuProgress(
      GameTestHelper helper,
      Player player,
      BlockPos pos,
      DeferredBlock<? extends MachineBlocks.MachineBlock> block,
      MachineKind kind,
      int elapsed,
      int total,
      int expectedPercent
   ) {
      MachineBlockEntity machine = placeMachineAt(helper, pos, block, kind);
      MachineMenu menu = new MachineMenu(kind, 1, player.getInventory(), machine, machine.dataAccessForTests());
      switch (kind) {
         case SMALL_LAB_FURNACE, SMALL_ELECTRICAL_FURNACE -> {
            machine.dataAccessForTests().set(2, elapsed);
            machine.dataAccessForTests().set(3, total);
         }
         case SMALL_BLOCK_BREAKER -> {
            machine.dataAccessForTests().set(1, elapsed);
            machine.dataAccessForTests().set(3, total);
         }
         case SMALL_MINERAL_SMELTER, SMALL_FREEZER -> machine.dataAccessForTests().set(1, elapsed);
         default -> throw new IllegalArgumentException("Unsupported progress test kind " + kind);
      }

      helper.assertValueEqual(expectedPercent, menu.progress(), kind.registryName() + " menu should report normalized progress percent");
      menu.removed(player);
   }

   private static MachineBlockEntity placeMachine(GameTestHelper helper, DeferredBlock<? extends MachineBlocks.MachineBlock> block, MachineKind kind) {
      return placeMachineAt(helper, TEST_POS, block, kind);
   }

   private static MachineBlockEntity placeMachineAt(
      GameTestHelper helper, BlockPos pos, DeferredBlock<? extends MachineBlocks.MachineBlock> block, MachineKind kind
   ) {
      helper.setBlock(pos, Blocks.AIR);
      helper.setBlock(pos, ((MachineBlocks.MachineBlock)block.get()).defaultBlockState());
      BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
      helper.assertTrue(blockEntity instanceof MachineBlockEntity, kind.registryName() + " should create a machine block entity");
      return (MachineBlockEntity)blockEntity;
   }

   private static void tickMachine(GameTestHelper helper, MachineBlockEntity machine, int times) {
      for (int i = 0; i < times; i++) {
         MachineBlockEntity.serverTick(helper.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
      }
   }

   private static int countItems(MachineBlockEntity machine, Item item, int... slots) {
      int count = 0;

      for (int slot : slots) {
         ItemStack stack = machine.getItem(slot);
         if (stack.is(item)) {
            count += stack.getCount();
         }
      }

      return count;
   }

   private static int countItem(Inventory inventory, Item item) {
      int count = 0;

      for (int i = 0; i < inventory.getContainerSize(); i++) {
         ItemStack stack = inventory.getItem(i);
         if (stack.is(item)) {
            count += stack.getCount();
         }
      }

      return count;
   }

   private record MachineEntry(DeferredBlock<? extends MachineBlocks.MachineBlock> block, MachineKind kind, int expectedSlots, String textureName) {
   }
}
