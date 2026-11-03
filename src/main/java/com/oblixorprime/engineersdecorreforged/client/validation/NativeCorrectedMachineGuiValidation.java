package com.oblixorprime.engineersdecorreforged.client.validation;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.client.MachineScreen;
import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import com.oblixorprime.engineersdecorreforged.utility.MachineLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.BufferUtils;

/** Native integrated-client captures for seven MachineScreen GUIs and the Tree Cutter badge only. */
final class NativeCorrectedMachineGuiValidation {
   private static final String SUITE = "native-corrected-machine-gui-v1";
   private static final BlockPos POS = new BlockPos(0, 80, 0);
   private static final List<Spec> SPECS = List.of(
      new Spec(MachineKind.FLUID_BARREL, "fluid_barrel", ModBlocks.FLUID_BARREL.get(), Fluids.WATER),
      new Spec(MachineKind.SMALL_MILKING_MACHINE, "small_milking_machine", ModBlocks.SMALL_MILKING_MACHINE.get(), null),
      new Spec(MachineKind.SMALL_BLOCK_BREAKER, "small_block_breaker", ModBlocks.SMALL_BLOCK_BREAKER.get(), null),
      new Spec(MachineKind.SMALL_SOLAR_PANEL, "small_solar_panel", ModBlocks.SMALL_SOLAR_PANEL.get(), null),
      new Spec(MachineKind.SMALL_FREEZER, "small_freezer", ModBlocks.SMALL_FREEZER.get(), null),
      new Spec(MachineKind.SMALL_FLUID_FUNNEL, "small_fluid_funnel", ModBlocks.SMALL_FLUID_FUNNEL.get(), Fluids.WATER),
      new Spec(MachineKind.PASSIVE_FLUID_ACCUMULATOR, "passive_fluid_accumulator", ModBlocks.PASSIVE_FLUID_ACCUMULATOR.get(), Fluids.WATER)
   );
   private static final Spec TREE_CUTTER_BADGE_SPEC = new Spec(
      MachineKind.SMALL_TREE_CUTTER, "small_tree_cutter", ModBlocks.SMALL_TREE_CUTTER.get(), null
   );
   private static final String[] STATES = {"empty", "mid", "full"};

   private enum Stage {
      WAIT_SETUP, WAIT_MENU, WAIT_STATE, WAIT_PANEL_CAPTURE, WAIT_FREEZER_NEAR_END, WAIT_HOVER,
      WAIT_MILKING_INPUT, WAIT_MILKING_INPUT_CAPTURE, WAIT_MILKING_OUTPUT,
      WAIT_MILKING_OUTPUT_CAPTURE, WAIT_MILKING_TRANSFER,
      WAIT_BADGE_SETUP, WAIT_BADGE_MENU, WAIT_BADGE_CAPTURE, DONE
   }

   private static Stage stage;
   private static CompletableFuture<?> pending;
   private static int ticks;
   private static int specIndex;
   private static int stateIndex;
   private static MachineBlockEntity serverMachine;
   private static int serverMenuId = -1;
   private static String serverMenuIdentity = "";
   private static Path gameDirectory;
   private static Path screenshots;
   private static String nonce;
   private static String executedSourceSha;
   private static String pullRequestHeadSha;
   private static String language;
   private static JsonObject milkingCaseProof;
   private static JsonObject milkingInputTransfer;
   private static JsonObject milkingOutputTransfer;
   private static final JsonArray CAPTURES = new JsonArray();
   private static final JsonArray CASES = new JsonArray();

   private NativeCorrectedMachineGuiValidation() {}

   static void begin(Minecraft mc, Path directory, String runNonce) {
      require(mc.getSingleplayerServer() != null && mc.player != null,
         "Corrected GUI fixture requires an integrated player");
      gameDirectory = directory;
      nonce = runNonce;
      executedSourceSha = System.getProperty("iedctr.executedSourceSha", "");
      pullRequestHeadSha = System.getProperty("iedctr.pullRequestHeadSha", "");
      language = System.getProperty("iedctr.machineGuiLanguage", "");
      require(executedSourceSha.matches("[0-9a-f]{40}"), "Fixture requires the executed source SHA");
      require(pullRequestHeadSha.matches("[0-9a-f]{40}"), "Fixture requires the exact PR head SHA");
      require(language.equals("en_us") || language.equals("fr_fr"), "Fixture language must be en_us or fr_fr");
      screenshots = gameDirectory.resolve("corrected-machine-gui-screenshots");
      require(!Files.exists(screenshots), "Refusing stale corrected-machine GUI screenshots");
      while (CAPTURES.size() > 0) CAPTURES.remove(CAPTURES.size() - 1);
      while (CASES.size() > 0) CASES.remove(CASES.size() - 1);
      specIndex = 0;
      stateIndex = 0;
      ticks = 0;
      serverMachine = null;
      serverMenuId = -1;
      serverMenuIdentity = "";
      milkingCaseProof = null;
      milkingInputTransfer = null;
      milkingOutputTransfer = null;
      scheduleMachine(mc, 0);
      stage = Stage.WAIT_SETUP;
   }

   static boolean tick(Minecraft mc, Path directory, String runNonce) throws Exception {
      require(nonce.equals(runNonce) && directory.equals(gameDirectory), "Corrected GUI fixture identity changed");
      switch (stage) {
         case WAIT_SETUP -> {
            if (++ticks > 300) throw new IllegalStateException("Integrated server did not prepare a corrected GUI");
            if (!pending.isDone()) return false;
            pending.join();
            ticks = 0;
            stage = Stage.WAIT_MENU;
            return false;
         }
         case WAIT_MENU -> {
            if (++ticks > 300) throw new IllegalStateException("Real corrected machine screen did not open");
            if (!(mc.screen instanceof MachineScreen screen)
               || !(mc.player.containerMenu instanceof MachineMenu menu)
               || menu.kind() != SPECS.get(specIndex).kind()
               || menu.containerId != serverMenuId
               || !menu.getClass().getName().equals(serverMenuIdentity)) return false;
            verifyMenuGeometry(screen, menu, SPECS.get(specIndex));
            if (ticks < 15) return false;
            stateIndex = 0;
            scheduleState(mc, 0);
            stage = Stage.WAIT_STATE;
            ticks = 0;
            return false;
         }
         case WAIT_STATE -> {
            if (++ticks > 300) throw new IllegalStateException("Corrected GUI live state did not synchronize");
            if (!pending.isDone()) return false;
            pending.join();
            if (!(mc.screen instanceof MachineScreen screen)
               || !(mc.player.containerMenu instanceof MachineMenu menu)
               || menu.kind() != SPECS.get(specIndex).kind()
               || menu.containerId != serverMenuId) return false;
            if (!stateIsVisible(menu, SPECS.get(specIndex), stateIndex)) return false;
            if (SPECS.get(specIndex).kind() == MachineKind.SMALL_FREEZER && stateIndex == 1
               && menu.freezerProgressTicks() < 30) return false;
            if (ticks < 6) return false;
            screen.setSuppressTooltipsForPanelCapture(true);
            moveCursorOutsidePanel(mc);
            stage = Stage.WAIT_PANEL_CAPTURE;
            ticks = 0;
            return false;
         }
         case WAIT_PANEL_CAPTURE -> {
            if (++ticks < 3) return false;
            if (!(mc.screen instanceof MachineScreen screen)
               || !(mc.player.containerMenu instanceof MachineMenu menu)
               || menu.kind() != SPECS.get(specIndex).kind()
               || menu.containerId != serverMenuId
               || !stateIsVisible(menu, SPECS.get(specIndex), stateIndex)) {
               throw new IllegalStateException("Corrected GUI changed before its clean panel capture");
            }
            String state = stateIndex == 2 ? "full-hover" : STATES[stateIndex];
            capture(mc, screen, menu, SPECS.get(specIndex), state);
            if (stateIndex == 2) {
               screen.setSuppressTooltipsForPanelCapture(false);
               moveCursorToFirstSlot(mc, menu);
               stage = Stage.WAIT_HOVER;
               ticks = 0;
               return false;
            }
            if (menu.kind() == MachineKind.SMALL_FREEZER && stateIndex == 1) {
               scheduleFreezerNearEnd(mc);
               stage = Stage.WAIT_FREEZER_NEAR_END;
               ticks = 0;
               return false;
            }
            stateIndex++;
            scheduleState(mc, stateIndex);
            stage = Stage.WAIT_STATE;
            ticks = 0;
            return false;
         }
         case WAIT_FREEZER_NEAR_END -> {
            if (++ticks > 60) throw new IllegalStateException("Freezer near-end GUI state did not synchronize");
            if (!pending.isDone()) return false;
            pending.join();
            if (!(mc.screen instanceof MachineScreen screen)
               || !(mc.player.containerMenu instanceof MachineMenu menu)
               || menu.kind() != MachineKind.SMALL_FREEZER
               || menu.containerId != serverMenuId) return false;
            if (menu.freezerProgressTicks() < MachineBlockEntity.FREEZER_PROCESS_TICKS - 2) return false;
            require(menu.freezerProgressTicks() < MachineBlockEntity.FREEZER_PROCESS_TICKS,
               "Freezer fixture completed before its near-end frame could be captured");
            capture(mc, screen, menu, SPECS.get(specIndex), "near-end");
            stateIndex++;
            scheduleState(mc, stateIndex);
            stage = Stage.WAIT_STATE;
            ticks = 0;
            return false;
         }
         case WAIT_HOVER -> {
            if (++ticks < 6) return false;
            if (!(mc.screen instanceof MachineScreen screen)
               || !(mc.player.containerMenu instanceof MachineMenu menu)
               || !stateIsVisible(menu, SPECS.get(specIndex), 2)) {
               throw new IllegalStateException("Corrected GUI changed before its hover capture");
            }
            MachineLayout.MachineSlot target = menu.layout().machineSlots().get(0);
            JsonObject hover = new JsonObject();
            hover.addProperty("slot_index", target.containerIndex());
            hover.addProperty("slot_local_x", target.x());
            hover.addProperty("slot_local_y", target.y());
            double centerX = (mc.getWindow().getGuiScaledWidth() - menu.layout().imageWidth()) / 2.0 + target.x() + 8;
            double centerY = (mc.getWindow().getGuiScaledHeight() - menu.layout().imageHeight()) / 2.0 + target.y() + 8;
            hover.addProperty("slot_hitbox_center_verified", screen.isMachineSlotHoveredAt(0, centerX, centerY));
            require(hover.get("slot_hitbox_center_verified").getAsBoolean(), "First machine slot does not hover at its center");
            JsonObject panelCapture = CAPTURES.get(CAPTURES.size() - 1).getAsJsonObject();
            captureHoverWindow(mc, panelCapture, hover);
            JsonObject result = caseProof(mc, screen, menu, SPECS.get(specIndex));
            if (menu.kind() == MachineKind.SMALL_MILKING_MACHINE) {
               milkingCaseProof = result;
               scheduleMilkingInput(mc);
               stage = Stage.WAIT_MILKING_INPUT;
               ticks = 0;
               return false;
            }
            CASES.add(result);
            specIndex++;
            if (specIndex >= SPECS.size()) {
               scheduleMachine(mc, TREE_CUTTER_BADGE_SPEC);
               stage = Stage.WAIT_BADGE_SETUP;
               ticks = 0;
               return false;
            }
            scheduleMachine(mc, specIndex);
            stage = Stage.WAIT_SETUP;
            ticks = 0;
            return false;
         }
         case WAIT_MILKING_INPUT -> {
            if (++ticks > 300) throw new IllegalStateException("Milking input quick-move did not synchronize");
            if (!pending.isDone()) return false;
            pending.join();
            if (!(mc.screen instanceof MachineScreen screen)
               || !(mc.player.containerMenu instanceof MachineMenu menu)
               || menu.kind() != MachineKind.SMALL_MILKING_MACHINE || menu.containerId != serverMenuId) return false;
            if (!milkingInputVisible(menu, mc.player.getInventory()) || ticks < 6) return false;
            screen.setSuppressTooltipsForPanelCapture(true);
            moveCursorOutsidePanel(mc);
            stage = Stage.WAIT_MILKING_INPUT_CAPTURE;
            ticks = 0;
            return false;
         }
         case WAIT_MILKING_INPUT_CAPTURE -> {
            if (++ticks < 3) return false;
            if (!(mc.screen instanceof MachineScreen screen)
               || !(mc.player.containerMenu instanceof MachineMenu menu)
               || menu.kind() != MachineKind.SMALL_MILKING_MACHINE || menu.containerId != serverMenuId
               || !milkingInputVisible(menu, mc.player.getInventory())) {
               throw new IllegalStateException("Milking input ItemStack changed before its native capture");
            }
            capture(mc, screen, menu, SPECS.get(specIndex), "input-occupied");
            scheduleMilkingOutput(mc);
            stage = Stage.WAIT_MILKING_OUTPUT;
            ticks = 0;
            return false;
         }
         case WAIT_MILKING_OUTPUT -> {
            if (++ticks > 300) throw new IllegalStateException("Milking output ItemStack did not synchronize");
            if (!pending.isDone()) return false;
            pending.join();
            if (!(mc.screen instanceof MachineScreen screen)
               || !(mc.player.containerMenu instanceof MachineMenu menu)
               || menu.kind() != MachineKind.SMALL_MILKING_MACHINE || menu.containerId != serverMenuId) return false;
            if (!milkingOutputVisible(menu, mc.player.getInventory()) || ticks < 6) return false;
            stage = Stage.WAIT_MILKING_OUTPUT_CAPTURE;
            ticks = 0;
            return false;
         }
         case WAIT_MILKING_OUTPUT_CAPTURE -> {
            if (++ticks < 3) return false;
            if (!(mc.screen instanceof MachineScreen screen)
               || !(mc.player.containerMenu instanceof MachineMenu menu)
               || menu.kind() != MachineKind.SMALL_MILKING_MACHINE || menu.containerId != serverMenuId
               || !milkingOutputVisible(menu, mc.player.getInventory())) {
               throw new IllegalStateException("Milking output ItemStack changed before its native capture");
            }
            capture(mc, screen, menu, SPECS.get(specIndex), "output-occupied");
            scheduleMilkingOutputTransfer(mc);
            stage = Stage.WAIT_MILKING_TRANSFER;
            ticks = 0;
            return false;
         }
         case WAIT_MILKING_TRANSFER -> {
            if (++ticks > 300) throw new IllegalStateException("Milking output quick-move did not synchronize");
            if (!pending.isDone()) return false;
            pending.join();
            if (!(mc.player.containerMenu instanceof MachineMenu menu)
               || menu.kind() != MachineKind.SMALL_MILKING_MACHINE || menu.containerId != serverMenuId) return false;
            if (!milkingTransferVisible(menu, mc.player.getInventory()) || ticks < 6) return false;
            require(milkingCaseProof != null && milkingInputTransfer != null && milkingOutputTransfer != null,
               "Milking real-item quick-move proof is incomplete");
            JsonArray capturedStates = milkingCaseProof.getAsJsonArray("captured_states");
            capturedStates.add("input-occupied");
            capturedStates.add("output-occupied");
            JsonObject itemTransfers = new JsonObject();
            itemTransfers.add("player_to_machine_input", milkingInputTransfer.deepCopy());
            itemTransfers.add("machine_output_to_player", milkingOutputTransfer.deepCopy());
            milkingCaseProof.add("slot_item_transfers", itemTransfers);
            CASES.add(milkingCaseProof);
            milkingCaseProof = null;
            milkingInputTransfer = null;
            milkingOutputTransfer = null;
            specIndex++;
            if (specIndex >= SPECS.size()) {
               scheduleMachine(mc, TREE_CUTTER_BADGE_SPEC);
               stage = Stage.WAIT_BADGE_SETUP;
               ticks = 0;
               return false;
            }
            scheduleMachine(mc, specIndex);
            stage = Stage.WAIT_SETUP;
            ticks = 0;
            return false;
         }
         case WAIT_BADGE_SETUP -> {
            if (++ticks > 300) throw new IllegalStateException("Tree Cutter badge-only fixture did not prepare");
            if (!pending.isDone()) return false;
            pending.join();
            ticks = 0;
            stage = Stage.WAIT_BADGE_MENU;
            return false;
         }
         case WAIT_BADGE_MENU -> {
            if (++ticks > 300) throw new IllegalStateException("Tree Cutter badge-only screen did not open");
            if (!(mc.screen instanceof MachineScreen screen)
               || !(mc.player.containerMenu instanceof MachineMenu menu)
               || menu.kind() != TREE_CUTTER_BADGE_SPEC.kind()
               || menu.containerId != serverMenuId) return false;
            if (ticks < 15) return false;
            screen.setSuppressTooltipsForPanelCapture(true);
            moveCursorOutsidePanel(mc);
            stage = Stage.WAIT_BADGE_CAPTURE;
            ticks = 0;
            return false;
         }
         case WAIT_BADGE_CAPTURE -> {
            if (++ticks < 3) return false;
            if (!(mc.screen instanceof MachineScreen screen)
               || !(mc.player.containerMenu instanceof MachineMenu menu)
               || menu.kind() != TREE_CUTTER_BADGE_SPEC.kind()
               || menu.containerId != serverMenuId) {
               throw new IllegalStateException("Tree Cutter screen changed before its badge-only capture");
            }
            JsonObject badgeCapture = capture(mc, screen, menu, TREE_CUTTER_BADGE_SPEC, "badge-only");
            badgeCapture.addProperty("machine_id", TREE_CUTTER_BADGE_SPEC.id());
            writeProof(mc, runNonce, badgeCapture);
            stage = Stage.DONE;
            return true;
         }
         case DONE -> { return true; }
      }
      throw new IllegalStateException("Unexpected corrected GUI fixture stage: " + stage);
   }

   private static void scheduleMachine(Minecraft mc, int index) {
      scheduleMachine(mc, SPECS.get(index));
   }

   private static void scheduleMachine(Minecraft mc, Spec spec) {
      UUID playerId = mc.player.getUUID();
      var server = mc.getSingleplayerServer();
      pending = server.submit(() -> {
         ServerPlayer player = server.getPlayerList().getPlayer(playerId);
         require(player != null, "Integrated server player disappeared before GUI setup");
         player.closeContainer();
         var level = player.serverLevel();
         player.setGameMode(GameType.CREATIVE);
         player.getInventory().clearContent();
         player.getInventory().setItem(0, new ItemStack(Items.BUCKET));
         player.connection.teleport(POS.getX() + 0.5, POS.getY() + 1.0, POS.getZ() + 3.5, 180.0F, 0.0F);
         level.setBlock(POS.below(), Blocks.STONE.defaultBlockState(), 3);
         level.setBlock(POS, Blocks.AIR.defaultBlockState(), 3);
         level.setBlock(POS, spec.block().defaultBlockState(), 3);
         if (!(level.getBlockEntity(POS) instanceof MachineBlockEntity machine)
            || machine.kind() != spec.kind()) {
            throw new IllegalStateException("Fixture failed to place " + spec.kind());
         }
         if (spec.kind() == MachineKind.SMALL_SOLAR_PANEL) level.setDayTime(18000L);
         serverMachine = machine;
         setState(machine, spec, 0);
         if (player.openMenu(machine).isEmpty()) throw new IllegalStateException("Server refused " + spec.kind() + " menu");
         if (!(player.containerMenu instanceof MachineMenu menu)
            || menu.kind() != spec.kind() || !menu.stillValid(player)
            || (spec != TREE_CUTTER_BADGE_SPEC
               && (menu.visibleMachineSlots() != menu.layout().machineSlots().size()
                  || menu.slots.size() != menu.visibleMachineSlots() + 36))) {
            throw new IllegalStateException("Server opened the wrong menu or slot lattice for " + spec.kind());
         }
         serverMenuId = menu.containerId;
         serverMenuIdentity = menu.getClass().getName();
         menu.broadcastChanges();
      });
   }

   private static void scheduleState(Minecraft mc, int nextState) {
      UUID playerId = mc.player.getUUID();
      Spec spec = SPECS.get(specIndex);
      var server = mc.getSingleplayerServer();
      pending = server.submit(() -> {
         ServerPlayer player = server.getPlayerList().getPlayer(playerId);
         require(player != null && serverMachine != null, "Server fixture lost its live machine");
         setState(serverMachine, spec, nextState);
         if (spec.kind() == MachineKind.SMALL_FREEZER && nextState == 1) {
            require(serverMachine.canPlaceItem(0, new ItemStack(Items.WATER_BUCKET)),
               "Freezer fixture water bucket is not a valid recipe input");
            serverMachine.setItem(0, new ItemStack(Items.WATER_BUCKET));
         }
         if (spec.kind() == MachineKind.SMALL_FREEZER && nextState == 2) {
            serverMachine.setItem(0, ItemStack.EMPTY);
            serverMachine.dataAccessForTests().set(1, 0);
         }
         if (player.containerMenu instanceof MachineMenu menu) menu.broadcastChanges();
      });
   }

   private static void scheduleFreezerNearEnd(Minecraft mc) {
      UUID playerId = mc.player.getUUID();
      var server = mc.getSingleplayerServer();
      pending = server.submit(() -> {
         ServerPlayer player = server.getPlayerList().getPlayer(playerId);
         require(player != null && serverMachine != null, "Freezer fixture lost its live machine");
         serverMachine.dataAccessForTests().set(1, MachineBlockEntity.FREEZER_PROCESS_TICKS - 2);
         serverMachine.dataAccessForTests().set(3, 8);
         serverMachine.setChanged();
         if (player.containerMenu instanceof MachineMenu menu) menu.broadcastChanges();
      });
   }

   private static void scheduleMilkingInput(Minecraft mc) {
      UUID playerId = mc.player.getUUID();
      var server = mc.getSingleplayerServer();
      pending = server.submit(() -> {
         ServerPlayer player = server.getPlayerList().getPlayer(playerId);
         require(player != null && serverMachine != null, "Milking input fixture lost its live machine");
         require(player.containerMenu instanceof MachineMenu, "Milking input fixture lost its live menu");
         MachineMenu menu = (MachineMenu)player.containerMenu;
         require(menu.kind() == MachineKind.SMALL_MILKING_MACHINE && menu.visibleMachineSlots() == 2,
            "Milking input fixture opened the wrong menu");
         setState(serverMachine, SPECS.get(specIndex), 0);
         require(serverMachine.getItem(0).isEmpty() && serverMachine.getItem(1).isEmpty(),
            "Milking input fixture started with a nonempty machine slot");
         player.getInventory().setItem(0, new ItemStack(Items.BUCKET, 3));
         int sourceMenuIndex = menu.visibleMachineSlots() + 27;
         require(menu.getSlot(sourceMenuIndex).getSlotIndex() == 0
            && menu.getSlot(sourceMenuIndex).getItem().is(Items.BUCKET)
            && menu.getSlot(sourceMenuIndex).getItem().getCount() == 3,
            "Milking fixture bucket stack is not in hotbar slot zero");
         ItemStack moved = menu.quickMoveStack(player, sourceMenuIndex);
         ItemStack input = serverMachine.getItem(0);
         require(moved.is(Items.BUCKET) && moved.getCount() == 3
            && input.is(Items.BUCKET) && input.getCount() == 3
            && player.getInventory().getItem(0).isEmpty() && serverMachine.getItem(1).isEmpty(),
            "Milking bucket quick-move did not place all three real items in the input slot");
         milkingInputTransfer = new JsonObject();
         milkingInputTransfer.addProperty("source_menu_index", sourceMenuIndex);
         milkingInputTransfer.addProperty("source_player_inventory_index", 0);
         milkingInputTransfer.addProperty("destination_container_index", 0);
         milkingInputTransfer.addProperty("item_id", itemId(moved));
         milkingInputTransfer.addProperty("source_count_before", 3);
         milkingInputTransfer.addProperty("moved_count", moved.getCount());
         milkingInputTransfer.addProperty("source_count_after", player.getInventory().getItem(0).getCount());
         milkingInputTransfer.addProperty("destination_item_id", itemId(input));
         milkingInputTransfer.addProperty("destination_count_after", input.getCount());
         milkingInputTransfer.addProperty("quick_move_succeeded", true);
         serverMachine.setChanged();
         menu.broadcastChanges();
      });
   }

   private static void scheduleMilkingOutput(Minecraft mc) {
      UUID playerId = mc.player.getUUID();
      var server = mc.getSingleplayerServer();
      pending = server.submit(() -> {
         ServerPlayer player = server.getPlayerList().getPlayer(playerId);
         require(player != null && serverMachine != null, "Milking output fixture lost its live machine");
         require(player.containerMenu instanceof MachineMenu, "Milking output fixture lost its live menu");
         MachineMenu menu = (MachineMenu)player.containerMenu;
         require(menu.kind() == MachineKind.SMALL_MILKING_MACHINE
            && serverMachine.getItem(0).is(Items.BUCKET) && serverMachine.getItem(0).getCount() == 3
            && serverMachine.getItem(1).isEmpty(), "Milking output fixture changed its input slot");
         serverMachine.setItem(1, new ItemStack(Items.MILK_BUCKET));
         serverMachine.setChanged();
         menu.broadcastChanges();
      });
   }

   private static void scheduleMilkingOutputTransfer(Minecraft mc) {
      UUID playerId = mc.player.getUUID();
      var server = mc.getSingleplayerServer();
      pending = server.submit(() -> {
         ServerPlayer player = server.getPlayerList().getPlayer(playerId);
         require(player != null && serverMachine != null, "Milking output transfer lost its live machine");
         require(player.containerMenu instanceof MachineMenu, "Milking output transfer lost its live menu");
         MachineMenu menu = (MachineMenu)player.containerMenu;
         require(menu.kind() == MachineKind.SMALL_MILKING_MACHINE
            && serverMachine.getItem(0).is(Items.BUCKET) && serverMachine.getItem(0).getCount() == 3
            && serverMachine.getItem(1).is(Items.MILK_BUCKET) && serverMachine.getItem(1).getCount() == 1
            && countInventoryItem(player.getInventory(), Items.MILK_BUCKET) == 0,
            "Milking output transfer fixture did not start with one real milk bucket");
         ItemStack moved = menu.quickMoveStack(player, 1);
         int inventorySlot = findInventorySlot(player.getInventory(), Items.MILK_BUCKET);
         int menuSlot = findMenuItemSlot(menu, Items.MILK_BUCKET);
         require(moved.is(Items.MILK_BUCKET) && moved.getCount() == 1
            && serverMachine.getItem(1).isEmpty()
            && countInventoryItem(player.getInventory(), Items.MILK_BUCKET) == 1
            && inventorySlot >= 0 && menuSlot >= menu.visibleMachineSlots(),
            "Milking milk-bucket quick-move did not transfer the real output stack to player inventory");
         milkingOutputTransfer = new JsonObject();
         milkingOutputTransfer.addProperty("source_menu_index", 1);
         milkingOutputTransfer.addProperty("source_container_index", 1);
         milkingOutputTransfer.addProperty("destination_player_inventory_index", inventorySlot);
         milkingOutputTransfer.addProperty("destination_menu_index", menuSlot);
         milkingOutputTransfer.addProperty("item_id", itemId(moved));
         milkingOutputTransfer.addProperty("source_count_before", 1);
         milkingOutputTransfer.addProperty("moved_count", moved.getCount());
         milkingOutputTransfer.addProperty("source_count_after", serverMachine.getItem(1).getCount());
         milkingOutputTransfer.addProperty("destination_inventory_count_after",
            countInventoryItem(player.getInventory(), Items.MILK_BUCKET));
         milkingOutputTransfer.addProperty("quick_move_succeeded", true);
         serverMachine.setChanged();
         menu.broadcastChanges();
      });
   }

   private static void setState(MachineBlockEntity machine, Spec spec, int state) {
      int fraction = switch (state) { case 0 -> 0; case 1 -> 1; case 2 -> 2; default -> throw new IllegalArgumentException("Unknown GUI state"); };
      if (spec.fluid() != null || spec.kind() == MachineKind.SMALL_MILKING_MACHINE) {
         var handler = machine.fluidHandler(Direction.UP);
         require(handler != null && handler.getTanks() > 0, "Fixture has no fluid tank for " + spec.kind());
         handler.drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
         Fluid fluid = spec.fluid() != null ? spec.fluid() : milkFluid();
         int capacity = handler.getTankCapacity(0);
         int amount = fraction == 0 ? 0 : fraction == 1 ? capacity / 2 : capacity;
         if (amount > 0) require(handler.fill(new FluidStack(fluid, amount), FluidAction.EXECUTE) == amount,
            "Fixture could not set " + spec.kind() + " tank to state " + state);
      } else if (spec.kind() == MachineKind.SMALL_SOLAR_PANEL) {
         int amount = fraction * 64000 / 2;
         machine.dataAccessForTests().set(3, amount / 100);
      } else if (spec.kind() == MachineKind.SMALL_BLOCK_BREAKER) {
         machine.dataAccessForTests().set(10, fraction * 32000 / 2);
      } else if (spec.kind() == MachineKind.SMALL_FREEZER) {
         int amount = fraction * 16000 / 2;
         machine.dataAccessForTests().set(3, amount);
      }
      machine.setChanged();
   }

   private static Fluid milkFluid() {
      require(NeoForgeMod.MILK.isBound(), "Milk fluid is not registered in the native GUI fixture");
      return NeoForgeMod.MILK.value();
   }

   private static boolean stateIsVisible(MachineMenu menu, Spec spec, int state) {
      int expected = state == 0 ? 0 : state == 1 ? (spec.fluid() != null || spec.kind() == MachineKind.SMALL_MILKING_MACHINE
         ? menu.fluidCapacity() / 2 : menu.energyCapacity() / 2) : (spec.fluid() != null || spec.kind() == MachineKind.SMALL_MILKING_MACHINE
         ? menu.fluidCapacity() : menu.energyCapacity());
      int visible = spec.fluid() != null || spec.kind() == MachineKind.SMALL_MILKING_MACHINE
         ? menu.fluidAmount() : menu.energyStored();
      if (spec.kind() == MachineKind.SMALL_FREEZER && state == 1) return visible > 0 && menu.freezerProgressTicks() >= 30;
      return visible == expected;
   }

   private static boolean milkingInputVisible(MachineMenu menu, Inventory inventory) {
      return menu.getSlot(0).getItem().is(Items.BUCKET) && menu.getSlot(0).getItem().getCount() == 3
         && menu.getSlot(1).getItem().isEmpty() && countInventoryItem(inventory, Items.BUCKET) == 0;
   }

   private static boolean milkingOutputVisible(MachineMenu menu, Inventory inventory) {
      return menu.getSlot(0).getItem().is(Items.BUCKET) && menu.getSlot(0).getItem().getCount() == 3
         && countInventoryItem(inventory, Items.BUCKET) == 0
         && menu.getSlot(1).getItem().is(Items.MILK_BUCKET) && menu.getSlot(1).getItem().getCount() == 1
         && countInventoryItem(inventory, Items.MILK_BUCKET) == 0;
   }

   private static boolean milkingTransferVisible(MachineMenu menu, Inventory inventory) {
      return menu.getSlot(0).getItem().is(Items.BUCKET) && menu.getSlot(0).getItem().getCount() == 3
         && menu.getSlot(1).getItem().isEmpty() && countInventoryItem(inventory, Items.MILK_BUCKET) == 1;
   }

   private static int countInventoryItem(Inventory inventory, Item item) {
      int count = 0;
      for (int index = 0; index < inventory.getContainerSize(); index++) {
         ItemStack stack = inventory.getItem(index);
         if (stack.is(item)) count += stack.getCount();
      }
      return count;
   }

   private static int findInventorySlot(Inventory inventory, Item item) {
      for (int index = 0; index < inventory.getContainerSize(); index++) {
         if (inventory.getItem(index).is(item)) return index;
      }
      return -1;
   }

   private static int findMenuItemSlot(MachineMenu menu, Item item) {
      for (int index = menu.visibleMachineSlots(); index < menu.slots.size(); index++) {
         if (menu.getSlot(index).getItem().is(item)) return index;
      }
      return -1;
   }

   private static String itemId(ItemStack stack) {
      return BuiltInRegistries.ITEM.getKey(stack.isEmpty() ? Items.AIR : stack.getItem()).toString();
   }

   private static JsonArray machineSlotItems(MachineMenu menu) {
      JsonArray result = new JsonArray();
      for (int index = 0; index < menu.visibleMachineSlots(); index++) {
         ItemStack stack = menu.getSlot(index).getItem();
         JsonObject proof = new JsonObject();
         proof.addProperty("container_index", menu.getSlot(index).getSlotIndex());
         proof.addProperty("item_id", itemId(stack));
         proof.addProperty("count", stack.getCount());
         proof.addProperty("empty", stack.isEmpty());
         result.add(proof);
      }
      return result;
   }

   private static void verifyMenuGeometry(MachineScreen screen, MachineMenu menu, Spec spec) {
      MachineLayout layout = menu.layout();
      require(screen.getClass().getName().contains("MachineScreen$"), "Fixture did not open the registered MachineScreen");
      require(menu.visibleMachineSlots() == layout.machineSlots().size(), "Visible machine slot count differs from layout");
      for (int i = 0; i < layout.machineSlots().size(); i++) {
         MachineLayout.MachineSlot expected = layout.machineSlots().get(i);
         var actual = menu.getSlot(i);
         require(actual.getSlotIndex() == expected.containerIndex() && actual.x == expected.x() && actual.y == expected.y(),
            "Live slot geometry differs from artwork layout for " + spec.id() + " index " + i);
         require(actual.x >= 0 && actual.y >= 0 && actual.x + 16 <= layout.imageWidth()
            && actual.y + 16 <= layout.imageHeight(), "Machine slot hitbox falls outside its panel: " + spec.id());
      }
      int base = layout.machineSlots().size();
      for (int row = 0; row < 3; row++) {
         for (int column = 0; column < 9; column++) {
            var actual = menu.getSlot(base + row * 9 + column);
            require(actual.x == layout.playerSlotX(column) && actual.y == layout.playerSlotY(row),
               "Live player inventory lattice differs from layout for " + spec.id());
         }
      }
      for (int column = 0; column < 9; column++) {
         var actual = menu.getSlot(base + 27 + column);
         require(actual.x == layout.hotbarSlotX(column) && actual.y == layout.hotbarY(),
            "Live hotbar lattice differs from layout for " + spec.id());
      }
      if (spec.kind() == MachineKind.SMALL_FREEZER) {
         require(!menu.getSlot(1).mayPlace(new ItemStack(Items.STONE)),
            "Reserved Freezer Cold slot must reject insertion");
      }
   }

   private static void moveCursorToFirstSlot(Minecraft mc, MachineMenu menu) {
      MachineLayout layout = menu.layout();
      int guiWidth = mc.getWindow().getGuiScaledWidth();
      int guiHeight = mc.getWindow().getGuiScaledHeight();
      MachineLayout.MachineSlot target = layout.machineSlots().get(0);
      double scaleX = mc.getWindow().getScreenWidth() / (double)guiWidth;
      double scaleY = mc.getWindow().getScreenHeight() / (double)guiHeight;
      double x = ((guiWidth - layout.imageWidth()) / 2.0 + target.x() + 8) * scaleX;
      double y = ((guiHeight - layout.imageHeight()) / 2.0 + target.y() + 8) * scaleY;
      GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), x, y);
   }

   private static void moveCursorOutsidePanel(Minecraft mc) {
      GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), mc.getWindow().getScreenWidth() - 3,
         mc.getWindow().getScreenHeight() - 3);
   }

   private static JsonObject capture(Minecraft mc, MachineScreen screen, MachineMenu menu, Spec spec, String state) throws Exception {
      MachineLayout layout = menu.layout();
      int guiWidth = mc.getWindow().getGuiScaledWidth();
      int guiHeight = mc.getWindow().getGuiScaledHeight();
      var cursorX = BufferUtils.createDoubleBuffer(1);
      var cursorY = BufferUtils.createDoubleBuffer(1);
      GLFW.glfwGetCursorPos(mc.getWindow().getWindow(), cursorX, cursorY);
      double cursorGuiX = cursorX.get(0) * guiWidth / mc.getWindow().getScreenWidth();
      double cursorGuiY = cursorY.get(0) * guiHeight / mc.getWindow().getScreenHeight();
      require(screen.isSuppressTooltipsForPanelCapture(), "Panel crop did not suppress pointer tooltips");
      require(screen.isPanelPointerOutsideAt(cursorGuiX, cursorGuiY),
         "Panel crop cursor is still over the machine GUI");
      double scaleX = mc.getWindow().getWidth() / (double)guiWidth;
      double scaleY = mc.getWindow().getHeight() / (double)guiHeight;
      int cropX = (int)Math.round((guiWidth - layout.imageWidth()) / 2.0 * scaleX);
      int cropY = (int)Math.round((guiHeight - layout.imageHeight()) / 2.0 * scaleY);
      int cropWidth = (int)Math.round(layout.imageWidth() * scaleX);
      int cropHeight = (int)Math.round(layout.imageHeight() * scaleY);
      String name = spec.id() + "-" + state + ".png";
      Files.createDirectories(screenshots);
      Path output = screenshots.resolve(name);
      require(!Files.exists(output), "Refusing to overwrite a corrected GUI capture");
      try (NativeImage full = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
         require(cropX >= 0 && cropY >= 0 && cropX + cropWidth <= full.getWidth()
            && cropY + cropHeight <= full.getHeight(), "GUI-only screenshot crop exceeds the live framebuffer");
         try (NativeImage crop = new NativeImage(cropWidth, cropHeight, false)) {
            for (int py = 0; py < cropHeight; py++) {
               for (int px = 0; px < cropWidth; px++) {
                  crop.setPixelRGBA(px, py, full.getPixelRGBA(cropX + px, cropY + py));
               }
            }
            crop.writeToFile(output);
         }
      }
      JsonObject capture = new JsonObject();
      capture.addProperty("name", name);
      capture.addProperty("state", state);
      capture.addProperty("capture_mode", "gui_panel");
      capture.addProperty("panel_cursor_outside", screen.isPanelPointerOutsideAt(cursorGuiX, cursorGuiY));
      capture.addProperty("panel_cursor_gui_x", cursorGuiX);
      capture.addProperty("panel_cursor_gui_y", cursorGuiY);
      capture.addProperty("panel_tooltips_suppressed", screen.isSuppressTooltipsForPanelCapture());
      capture.addProperty("sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(output))));
      capture.addProperty("width", cropWidth);
      capture.addProperty("height", cropHeight);
      capture.addProperty("kind", spec.kind().name());
      capture.addProperty("screen_identity", screen.getClass().getName());
      capture.addProperty("menu_id", menu.containerId);
      capture.addProperty("server_menu_id", serverMenuId);
      capture.addProperty("fluid_amount", menu.fluidAmount());
      capture.addProperty("energy", menu.energyStored());
      capture.addProperty("progress", menu.progress());
      if (menu.kind() == MachineKind.SMALL_FREEZER) {
         capture.addProperty("freezer_progress_ticks", menu.freezerProgressTicks());
      }
      if (menu.kind() == MachineKind.SMALL_MILKING_MACHINE) {
         capture.add("machine_slot_items", machineSlotItems(menu));
      }
      CAPTURES.add(capture);
      return capture;
   }

   private static void captureHoverWindow(Minecraft mc, JsonObject panelCapture, JsonObject hover) throws Exception {
      String tooltipName = SPECS.get(specIndex).id() + "-full-hover-window.png";
      Path output = screenshots.resolve(tooltipName);
      require(!Files.exists(output), "Refusing to overwrite a corrected GUI hover capture");
      try (NativeImage full = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
         full.writeToFile(output);
      }
      panelCapture.addProperty("tooltip_name", tooltipName);
      panelCapture.addProperty("tooltip_capture_mode", "full_window_tooltip");
      panelCapture.addProperty("tooltip_sha256", HexFormat.of().formatHex(
         MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(output))));
      panelCapture.addProperty("tooltip_width", mc.getWindow().getWidth());
      panelCapture.addProperty("tooltip_height", mc.getWindow().getHeight());
      panelCapture.add("hover", hover);
   }

   private static JsonObject caseProof(Minecraft mc, MachineScreen screen, MachineMenu menu, Spec spec) {
      JsonObject result = new JsonObject();
      result.addProperty("machine_id", spec.id());
      result.addProperty("machine_kind", spec.kind().name());
      result.addProperty("screen_identity", screen.getClass().getName());
      result.addProperty("client_menu_identity", menu.getClass().getName());
      result.addProperty("server_menu_identity", serverMenuIdentity);
      result.addProperty("client_menu_id", menu.containerId);
      result.addProperty("server_menu_id", serverMenuId);
      result.addProperty("menu_context_matches", menu.containerId == serverMenuId
         && menu.getClass().getName().equals(serverMenuIdentity));
      result.addProperty("visible_machine_slots", menu.visibleMachineSlots());
      result.addProperty("total_slots", menu.slots.size());
      result.addProperty("panel_width", menu.layout().imageWidth());
      result.addProperty("panel_height", menu.layout().imageHeight());
      result.addProperty("gui_width", mc.getWindow().getGuiScaledWidth());
      result.addProperty("gui_height", mc.getWindow().getGuiScaledHeight());
      result.addProperty("geometry_matches_layout", true);
      result.addProperty("fluid_amount", menu.fluidAmount());
      result.addProperty("fluid_capacity", menu.fluidCapacity());
      result.addProperty("energy", menu.energyStored());
      result.addProperty("energy_capacity", menu.energyCapacity());
      result.addProperty("progress", menu.progress());
      result.addProperty("freezer_progress_ticks", menu.freezerProgressTicks());
      result.addProperty("language", language);
      JsonArray stateNames = new JsonArray();
      stateNames.add("empty");
      stateNames.add("mid");
      stateNames.add("full-hover");
      result.add("captured_states", stateNames);
      JsonArray slots = new JsonArray();
      for (int index = 0; index < menu.visibleMachineSlots(); index++) {
         MachineLayout.MachineSlot layoutSlot = menu.layout().machineSlots().get(index);
         var liveSlot = menu.getSlot(index);
         JsonObject slot = new JsonObject();
         slot.addProperty("container_index", liveSlot.getSlotIndex());
         slot.addProperty("x", liveSlot.x);
         slot.addProperty("y", liveSlot.y);
         slot.addProperty("layout_x", layoutSlot.x());
         slot.addProperty("layout_y", layoutSlot.y());
         slot.addProperty("active", liveSlot.isActive());
       slot.addProperty("insertion_allowed", liveSlot.mayPlace(new ItemStack(Items.STONE)));
         double centerX = (mc.getWindow().getGuiScaledWidth() - menu.layout().imageWidth()) / 2.0 + liveSlot.x + 8;
         double centerY = (mc.getWindow().getGuiScaledHeight() - menu.layout().imageHeight()) / 2.0 + liveSlot.y + 8;
         slot.addProperty("center_hit_test", screen.isMachineSlotHoveredAt(index, centerX, centerY));
         slots.add(slot);
      }
      result.add("machine_slots", slots);
      return result;
   }

   private static void writeProof(Minecraft mc, String runNonce, JsonObject treeCutterBadgeCapture) throws Exception {
      require(nonce.equals(runNonce) && CASES.size() == SPECS.size()
         && CAPTURES.size() == SPECS.size() * STATES.length + 4
         && hasCapture("small_freezer-near-end.png")
         && hasCapture("small_milking_machine-input-occupied.png")
         && hasCapture("small_milking_machine-output-occupied.png")
         && hasCapture("small_tree_cutter-badge-only.png"),
         "Corrected GUI capture matrix is incomplete");
      String selectedLanguage = mc.getLanguageManager().getSelected();
      require(language.equals(selectedLanguage), "Minecraft did not load the requested GUI language");
      JsonObject proof = new JsonObject();
      proof.addProperty("suite", SUITE);
      proof.addProperty("nonce", nonce);
      proof.addProperty("status", "passed");
      proof.addProperty("actual_integrated_world", mc.getSingleplayerServer() != null
         && mc.player.connection.getConnection().isMemoryConnection());
      proof.addProperty("executed_source_sha", executedSourceSha);
      proof.addProperty("pull_request_head_sha", pullRequestHeadSha);
      proof.addProperty("language", language);
      proof.addProperty("selected_language", selectedLanguage);
      proof.addProperty("window_width", mc.getWindow().getWidth());
      proof.addProperty("window_height", mc.getWindow().getHeight());
      proof.addProperty("screenshot_method", "Native Minecraft panel crops with cursor outside, separate full-window hover captures, and a Tree Cutter badge-only crop");
      proof.addProperty("fixture_state_source", "Live MachineBlockEntity fluid handlers and test data synced through the registered MachineMenu");
      proof.add("cases", CASES.deepCopy());
      proof.add("screenshots", CAPTURES.deepCopy());
      proof.add("tree_cutter_badge_only_capture", treeCutterBadgeCapture.deepCopy());
      Files.writeString(gameDirectory.resolve("client-corrected-machine-gui-result.json"),
         new GsonBuilder().setPrettyPrinting().create().toJson(proof) + "\n");
   }

   private static void require(boolean condition, String message) {
      if (!condition) throw new IllegalStateException(message);
   }

   private static boolean hasCapture(String name) {
      for (int index = 0; index < CAPTURES.size(); index++) {
         if (CAPTURES.get(index).getAsJsonObject().get("name").getAsString().equals(name)) return true;
      }
      return false;
   }

   private record Spec(MachineKind kind, String id, Block block, Fluid fluid) {}
}
