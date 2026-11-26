package com.oblixorprime.iedctrtest.smelter;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.client.ThermalVerticalScreen;
import com.oblixorprime.engineersdecorreforged.menu.MachineMenu;
import com.oblixorprime.engineersdecorreforged.utility.FreezerReferenceGeometry;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlocks;
import com.oblixorprime.engineersdecorreforged.utility.ThermalVerticalGeometry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.FluidStack;
import org.lwjgl.glfw.GLFW;

/** Test the registered thermal vertical Smelter, not the superseded drawer screen. */
final class NativeSmelterGuiValidation {
   private static final BlockPos POS = new BlockPos(0, 80, 0);
   private static final String SUITE = "native-smelter-gui-v2";
   private static final String TITLE_KEY = "block.immersive_engineer_decor_controls_tool_reforged.small_mineral_smelter";
   private enum Stage { WAIT_SERVER, WAIT_SCREEN, WAIT_EMPTY, WAIT_MID, WAIT_FULL, WAIT_ACTIVE, WAIT_TRANSFER }
   private static Stage stage;
   private static CompletableFuture<?> pending;
   private static int ticks;
   private static int serverMenuId = -1;
   private static MachineBlockEntity serverMachine;
   private static String serverMenuClass = "";
   private static boolean serverContextValid;
   private static volatile int serverMoved = -1;
   private static volatile int serverInventoryCount = -1;
   private static volatile int serverOutputCount = -1;
   private static boolean resized;
   private static Path gameDirectory, screenshots;
   private static String nonce, executedSourceSha, pullRequestHeadSha, language;
   private static JsonObject activeState;
   private static final JsonArray CAPTURES = new JsonArray();

   private NativeSmelterGuiValidation() {}

   static void begin(Minecraft mc, Path directory, String runNonce) {
      require(mc.getSingleplayerServer() != null && mc.player != null, "Smelter requires a real integrated client");
      gameDirectory = directory;
      nonce = runNonce;
      executedSourceSha = System.getProperty("iedctr.executedSourceSha", "");
      pullRequestHeadSha = System.getProperty("iedctr.pullRequestHeadSha", "");
      language = System.getProperty("iedctr.smelterLanguage", "");
      require(executedSourceSha.matches("[0-9a-f]{40}") && pullRequestHeadSha.matches("[0-9a-f]{40}"),
         "Smelter proof requires the exact source and PR HEAD");
      require(language.equals("en_us") || language.equals("fr_fr"), "Smelter proof requires en_us or fr_fr");
      screenshots = directory.resolve("smelter-gui-screenshots");
      require(!Files.exists(screenshots), "Refusing stale Smelter screenshot directory");
      while (CAPTURES.size() > 0) CAPTURES.remove(CAPTURES.size() - 1);
      ticks = 0; serverMenuId = -1; serverMachine = null; serverMenuClass = "";
      serverContextValid = false; serverMoved = -1; serverInventoryCount = -1;
      serverOutputCount = -1; resized = false; activeState = null;
      UUID id = mc.player.getUUID();
      var server = mc.getSingleplayerServer();
      pending = server.submit(() -> {
         ServerPlayer player = server.getPlayerList().getPlayer(id);
         require(player != null, "Integrated Smelter player disappeared");
         var level = player.serverLevel();
         player.setGameMode(GameType.CREATIVE);
         player.getInventory().clearContent();
         player.connection.teleport(POS.getX() + 0.5, POS.getY() + 1.0, POS.getZ() + 3.5, 180.0F, 0.0F);
         level.setBlock(POS.below(), Blocks.STONE.defaultBlockState(), 3);
         level.setBlock(POS, ModBlocks.SMALL_MINERAL_SMELTER.get().defaultBlockState()
            .setValue(MachineBlocks.HORIZONTAL_FACING, Direction.NORTH), 3);
         require(level.getBlockEntity(POS) instanceof MachineBlockEntity, "Real Smelter BE missing");
         MachineBlockEntity machine = (MachineBlockEntity)level.getBlockEntity(POS);
         serverMachine = machine;
         require(machine.canPlaceItem(0, new ItemStack(Items.COBBLESTONE)), "Smelter rejected its actual mineral ingredient");
         machine.setItem(0, new ItemStack(Items.COBBLESTONE));
         machine.setItem(2, new ItemStack(Items.MAGMA_BLOCK, 2));
         var rear = level.getCapability(Capabilities.FluidHandler.BLOCK, POS, Direction.SOUTH);
         require(rear != null && level.getCapability(Capabilities.FluidHandler.BLOCK, POS, Direction.UP) == null
            && level.getCapability(Capabilities.FluidHandler.BLOCK, POS, Direction.NORTH) == null
            && level.getCapability(Capabilities.FluidHandler.BLOCK, POS, Direction.EAST) == null
            && level.getCapability(Capabilities.FluidHandler.BLOCK, POS, Direction.WEST) == null,
            "Smelter lava capability must be exposed at the rear only");
         require(rear.fill(new FluidStack(Fluids.LAVA, 2500), FluidAction.EXECUTE) == 2500,
            "Smelter rear input failed to receive actual lava");
         var bottom = level.getCapability(Capabilities.FluidHandler.BLOCK, POS, Direction.DOWN);
         require(bottom != null && bottom.fill(new FluidStack(Fluids.LAVA, 2500), FluidAction.EXECUTE) == 0,
            "Smelter bottom output must reject lava insertion");
         require(machine.energyStorage(null).receiveEnergy(12000, false) == 12000, "Smelter FE fill failed");
         require(!machine.requestMineralModeFromGui(player, 0, 0), "Legacy manual mode request must be inert");
         player.openMenu(machine);
         require(player.containerMenu instanceof MachineMenu, "Server did not open the registered Smelter menu");
         MachineMenu menu = (MachineMenu)player.containerMenu;
         require(menu.isMineralSmelterMenuFor(machine) && menu.stillValid(player)
            && menu.visibleMachineSlots() == 2 && menu.slots.size() == 38
            && menu.getSlot(0).getSlotIndex() == 0 && menu.getSlot(1).getSlotIndex() == 2
            && menu.getSlot(1).isActive() && menu.getSlot(0).getItem().is(Items.COBBLESTONE)
            && menu.getSlot(1).getItem().is(Items.MAGMA_BLOCK)
            && menu.getSlot(1).getItem().getCount() == 2,
            "Server opened wrong Smelter backing inventory or menu");
         serverMenuId = menu.containerId;
         serverMenuClass = menu.getClass().getName();
         serverContextValid = true;
         menu.broadcastChanges();
      });
      stage = Stage.WAIT_SERVER;
   }

   static boolean tick(Minecraft mc, Path directory, String runNonce) throws Exception {
      require(directory.equals(gameDirectory) && runNonce.equals(nonce), "Smelter run nonce/directory changed");
      if (stage == Stage.WAIT_SERVER) {
         if (++ticks > 300) throw new IllegalStateException("Integrated server did not open Smelter");
         if (!pending.isDone()) return false;
         pending.join();
         stage = Stage.WAIT_SCREEN; ticks = 0;
         return false;
      }
      if (stage == Stage.WAIT_SCREEN) {
         if (++ticks > 300) throw new IllegalStateException("Real ThermalVerticalScreen did not open or synchronize");
         if (!(mc.screen instanceof ThermalVerticalScreen screen)) return false;
         MachineMenu menu = currentMenu(mc);
         if (menu == null || !geometryVerified(screen, menu)
            || menu.field(MachineMenu.SMS_UI_FLAGS) != 1
            || menu.field(MachineMenu.SMS_UI_SELECTED_MODE) != -1
            || menu.field(MachineMenu.SMS_UI_BATCH_MODE) != -1
            || menu.field(MachineMenu.SMS_UI_QUEUED_MODE) != -1
            || menu.field(MachineMenu.SMS_UI_BATCH_PROGRESS) <= 0
            || menu.field(MachineMenu.SMS_UI_THERMAL) <= 0
            || menu.field(MachineMenu.SMS_UI_TANK_AMOUNT) != 2500
            || menu.field(MachineMenu.SMS_UI_ENERGY) <= 0
            || menu.field(MachineMenu.SMS_UI_FE_RATE) != 16
            || menu.field(MachineMenu.SMS_UI_STATUS) != 1
            || !menu.getSlot(0).getItem().is(Items.COBBLESTONE)
            || !menu.getSlot(1).getItem().is(Items.MAGMA_BLOCK)
            || menu.getSlot(1).getItem().getCount() != 2) return false;
         if (ticks < 30) return false;
         moveCursorOutside(mc);
         fixtureState(mc, 0, 0, 0, Stage.WAIT_EMPTY);
         return false;
      }
      if (stage == Stage.WAIT_EMPTY || stage == Stage.WAIT_MID || stage == Stage.WAIT_FULL) {
         if (++ticks > 160) throw new IllegalStateException("Smelter thermal projection did not synchronize: " + stage);
         if (!pending.isDone()) return false;
         pending.join();
         if (!(mc.screen instanceof ThermalVerticalScreen screen)) return false;
         MachineMenu menu = currentMenu(mc);
         if (menu == null || !geometryVerified(screen, menu)) return false;
         int expectedProgress = stage == Stage.WAIT_EMPTY ? 0 : stage == Stage.WAIT_MID ? 50 : 90;
         int expectedPhase = stage == Stage.WAIT_EMPTY ? 0 : stage == Stage.WAIT_MID ? 1 : 2;
         if (menu.field(MachineMenu.SMS_UI_BATCH_PROGRESS) != expectedProgress
            || menu.field(MachineMenu.SMS_UI_ENERGY) != 0
            || menu.field(MachineMenu.SMS_UI_STATUS) != 3
            || menu.field(MachineMenu.SMS_UI_PHASE) != expectedPhase) return false;
         if (ticks < 6) return false;
         moveCursorOutside(mc);
         capture(mc, menu, stage == Stage.WAIT_EMPTY ? "progress-empty.png"
            : stage == Stage.WAIT_MID ? "progress-mid.png" : "progress-full.png");
         if (stage == Stage.WAIT_EMPTY) fixtureState(mc, 50, 0, 50, Stage.WAIT_MID);
         else if (stage == Stage.WAIT_MID) fixtureState(mc, 90, 0, 95, Stage.WAIT_FULL);
         else fixtureState(mc, 50, 12000, 95, Stage.WAIT_ACTIVE);
         return false;
      }
      if (stage == Stage.WAIT_ACTIVE) {
         if (++ticks > 180) throw new IllegalStateException("Live Smelter heating did not resume");
         if (!pending.isDone()) return false;
         pending.join();
         if (!(mc.screen instanceof ThermalVerticalScreen screen)) return false;
         MachineMenu menu = currentMenu(mc);
         if (menu == null || !geometryVerified(screen, menu)
            || menu.field(MachineMenu.SMS_UI_BATCH_PROGRESS) <= 0
            || menu.field(MachineMenu.SMS_UI_ENERGY) <= 0
            || menu.field(MachineMenu.SMS_UI_STATUS) != 1
            || menu.field(MachineMenu.SMS_UI_FE_RATE) != 16
            || !menu.getSlot(1).getItem().is(Items.MAGMA_BLOCK)
            || menu.getSlot(1).getItem().getCount() != 2) return false;
         if (ticks < 30) return false;
         int width = screen.width, height = screen.height;
         screen.resize(mc, width - 1, height - 1);
         require(screen.width == width - 1 && screen.height == height - 1,
            "ThermalVerticalScreen did not process native resize callbacks");
         screen.resize(mc, width, height);
         resized = screen.width == width && screen.height == height && geometryVerified(screen, menu);
         require(resized, "ThermalVerticalScreen did not restore its fitted menu after resize");
         mc.options.hideGui = false;
         moveCursorOutside(mc);
         activeState = liveState(menu);
         capture(mc, menu, "screen-active.png");
         outputQuickMove(mc);
         stage = Stage.WAIT_TRANSFER; ticks = 0;
         return false;
      }
      if (stage == Stage.WAIT_TRANSFER) {
         if (++ticks > 120) throw new IllegalStateException("Smelter output transfer did not sync");
         if (!pending.isDone()) return false;
         pending.join();
         if (!(mc.screen instanceof ThermalVerticalScreen screen)) return false;
         MachineMenu menu = currentMenu(mc);
         if (menu == null || !geometryVerified(screen, menu)
            || !menu.getSlot(1).getItem().isEmpty()
            || serverMoved != 2 || serverInventoryCount != 2 || serverOutputCount != 0
            || inventoryMagma(mc) != 2) return false;
         if (ticks < 6) return false;
         capture(mc, menu, "output-extracted.png");
         writeProof(mc, menu);
         return true;
      }
      throw new IllegalStateException("Unexpected Smelter native stage " + stage);
   }

   private static boolean geometryVerified(ThermalVerticalScreen screen, MachineMenu menu) {
      if (!serverContextValid || menu.containerId != serverMenuId
         || !menu.thermalVerticalPresentation() || !menu.kind().name().equals("SMALL_MINERAL_SMELTER")
         || menu.visibleMachineSlots() != 2 || menu.slots.size() != 38
         || menu.getSlot(0).getSlotIndex() != 0 || menu.getSlot(1).getSlotIndex() != 2
         || !menu.getSlot(1).isActive()
         || menu.getSlot(1).mayPlace(new ItemStack(Items.MAGMA_BLOCK))
         || screen.children().stream().anyMatch(child -> child instanceof Button)) return false;
      for (int i = 0; i < menu.slots.size(); i++) {
         int position = i == 0 ? 0 : i == 1 ? 2 : i + 1;
         var anchor = ThermalVerticalGeometry.slot(position, menu.kind());
         var slot = menu.getSlot(i);
         if (slot.x != anchor.x() || slot.y != anchor.y()) return false;
      }
      return true;
   }

   private static MachineMenu currentMenu(Minecraft mc) {
      if (mc.player == null || !(mc.player.containerMenu instanceof MachineMenu menu)
         || !menu.kind().name().equals("SMALL_MINERAL_SMELTER")
         || menu.containerId != serverMenuId) return null;
      return menu;
   }

   private static int inventoryMagma(Minecraft mc) {
      int total = 0;
      for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (stack.is(Items.MAGMA_BLOCK)) total += stack.getCount();
      }
      return total;
   }

   private static void fixtureState(Minecraft mc, int progress, int energy, int thermal, Stage next) {
      UUID id = mc.player.getUUID();
      var server = mc.getSingleplayerServer();
      pending = server.submit(() -> {
         ServerPlayer player = server.getPlayerList().getPlayer(id);
         require(player != null && serverMachine != null, "Smelter state fixture lost the live server");
         // Mutate only actual MachineBlockEntity test fields; no fake client or pre-rendered image.
         serverMachine.dataAccessForTests().set(1, progress);
         serverMachine.dataAccessForTests().set(6, energy);
         serverMachine.dataAccessForTests().set(10, thermal);
         serverMachine.setChanged();
         if (player.containerMenu instanceof MachineMenu menu) menu.broadcastChanges();
      });
      stage = next; ticks = 0;
   }

   private static void outputQuickMove(Minecraft mc) {
      UUID id = mc.player.getUUID();
      var server = mc.getSingleplayerServer();
      pending = server.submit(() -> {
         ServerPlayer player = server.getPlayerList().getPlayer(id);
         require(player != null && player.containerMenu instanceof MachineMenu && serverMachine != null,
            "Output quick-move lost the integrated server menu");
         MachineMenu menu = (MachineMenu)player.containerMenu;
         require(menu.containerId == serverMenuId && menu.getSlot(1).getSlotIndex() == 2
            && serverMachine.getItem(2).is(Items.MAGMA_BLOCK)
            && serverMachine.getItem(2).getCount() == 2,
            "Smelter output no longer has the two source Magma Blocks");
         ItemStack moved = menu.quickMoveStack(player, 1);
         int count = 0;
         for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.MAGMA_BLOCK)) count += stack.getCount();
         }
         serverMoved = moved.is(Items.MAGMA_BLOCK) ? moved.getCount() : -1;
         serverOutputCount = serverMachine.getItem(2).getCount();
         serverInventoryCount = count;
         require(serverMoved == 2 && serverOutputCount == 0 && serverInventoryCount == 2,
            "Output quick-move lost or duplicated Magma");
         menu.broadcastChanges();
      });
   }

   private static void moveCursorOutside(Minecraft mc) {
      GLFW.glfwSetCursorPos(mc.getWindow().getWindow(),
         mc.getWindow().getWidth() - 3, mc.getWindow().getHeight() - 3);
   }

   private static JsonObject liveState(MachineMenu menu) {
      JsonObject state = new JsonObject();
      state.addProperty("selected_mode", menu.field(MachineMenu.SMS_UI_SELECTED_MODE));
      state.addProperty("batch_mode", menu.field(MachineMenu.SMS_UI_BATCH_MODE));
      state.addProperty("queued_mode", menu.field(MachineMenu.SMS_UI_QUEUED_MODE));
      state.addProperty("batch_progress", menu.field(MachineMenu.SMS_UI_BATCH_PROGRESS));
      state.addProperty("thermal", menu.field(MachineMenu.SMS_UI_THERMAL));
      state.addProperty("input_item", "minecraft:cobblestone");
      state.addProperty("output_item", "minecraft:magma_block");
      state.addProperty("output_count", menu.getSlot(1).getItem().getCount());
      state.addProperty("tank_amount", menu.field(MachineMenu.SMS_UI_TANK_AMOUNT));
      state.addProperty("tank_capacity", menu.field(MachineMenu.SMS_UI_TANK_CAPACITY));
      state.addProperty("energy", menu.field(MachineMenu.SMS_UI_ENERGY));
      state.addProperty("energy_capacity", menu.field(MachineMenu.SMS_UI_ENERGY_CAPACITY));
      state.addProperty("fe_rate", menu.field(MachineMenu.SMS_UI_FE_RATE));
      state.addProperty("status", menu.field(MachineMenu.SMS_UI_STATUS));
      return state;
   }

   private static void capture(Minecraft mc, MachineMenu menu, String name) throws Exception {
      Files.createDirectories(screenshots);
      Path output = screenshots.resolve(name);
      require(!Files.exists(output), "Refusing stale Smelter capture");
      try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
         image.writeToFile(output);
      }
      JsonObject c = new JsonObject();
      c.addProperty("name", name);
      c.addProperty("sha256", HexFormat.of().formatHex(
         MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(output))));
      c.addProperty("width", mc.getWindow().getWidth());
      c.addProperty("height", mc.getWindow().getHeight());
      c.addProperty("batch_progress", menu.field(MachineMenu.SMS_UI_BATCH_PROGRESS));
      c.addProperty("thermal", menu.field(MachineMenu.SMS_UI_THERMAL));
      c.addProperty("phase", menu.field(MachineMenu.SMS_UI_PHASE));
      c.addProperty("energy", menu.field(MachineMenu.SMS_UI_ENERGY));
      c.addProperty("status", menu.field(MachineMenu.SMS_UI_STATUS));
      c.addProperty("output_count", menu.getSlot(1).getItem().getCount());
      CAPTURES.add(c);
   }

   private static void writeProof(Minecraft mc, MachineMenu menu) throws Exception {
      require(CAPTURES.size() == 5 && activeState != null && resized
         && serverMoved == 2 && serverInventoryCount == 2 && serverOutputCount == 0,
         "Smelter framebuffer and item-transfer evidence is incomplete");
      String selected = mc.getLanguageManager().getSelected();
      require(language.equals(selected), "Minecraft did not load requested Smelter locale");
      String title = Component.translatable(TITLE_KEY).getString();
      require(!title.equals(TITLE_KEY) && !title.isBlank(), "Smelter localized title is unavailable");
      var fit = FreezerReferenceGeometry.fitCanvas(mc.screen.width, mc.screen.height,
         ThermalVerticalGeometry.CANVAS_WIDTH, ThermalVerticalGeometry.CANVAS_HEIGHT, 0.78F);
      JsonObject proof = new JsonObject();
      proof.addProperty("suite", SUITE);
      proof.addProperty("schema_version", 2);
      proof.addProperty("nonce", nonce);
      proof.addProperty("status", "passed");
      proof.addProperty("actual_integrated_world", mc.getSingleplayerServer() != null
         && mc.player.connection.getConnection().isMemoryConnection());
      proof.addProperty("executed_source_sha", executedSourceSha);
      proof.addProperty("pull_request_head_sha", pullRequestHeadSha);
      proof.addProperty("language", language);
      proof.addProperty("selected_language", selected);
      proof.addProperty("translated_machine_name", title);
      proof.addProperty("screen_identity", mc.screen.getClass().getName());
      proof.addProperty("client_menu_identity", menu.getClass().getName());
      proof.addProperty("server_menu_identity", serverMenuClass);
      proof.addProperty("machine_kind", menu.kind().name());
      proof.addProperty("client_menu_id", menu.containerId);
      proof.addProperty("server_menu_id", serverMenuId);
      proof.addProperty("server_menu_context_valid", serverContextValid);
      proof.addProperty("visible_machine_slots", menu.visibleMachineSlots());
      proof.addProperty("total_slots", menu.slots.size());
      JsonArray indices = new JsonArray();
      indices.add(menu.getSlot(0).getSlotIndex()); indices.add(menu.getSlot(1).getSlotIndex());
      proof.add("machine_slot_backing_indices", indices);
      proof.addProperty("data_fields", MachineMenu.SMALL_MINERAL_SMELTER_V05_DATA_COUNT);
      proof.add("live_state", activeState);
      proof.addProperty("window_width", mc.getWindow().getWidth());
      proof.addProperty("window_height", mc.getWindow().getHeight());
      JsonObject screen = new JsonObject();
      screen.addProperty("gui_width", mc.getWindow().getGuiScaledWidth());
      screen.addProperty("gui_height", mc.getWindow().getGuiScaledHeight());
      screen.addProperty("panel_width", ThermalVerticalGeometry.CANVAS_WIDTH);
      screen.addProperty("panel_height", ThermalVerticalGeometry.CANVAS_HEIGHT);
      screen.addProperty("view_scale", fit.scale());
      screen.addProperty("origin_x", fit.originX());
      screen.addProperty("origin_y", fit.originY());
      screen.addProperty("output_slot_visible", menu.getSlot(1).isActive());
      screen.addProperty("output_slot_insertion_forbidden", !menu.getSlot(1).mayPlace(new ItemStack(Items.MAGMA_BLOCK)));
      screen.addProperty("legacy_drawer_and_mode_tabs_absent", mc.screen.children().stream()
         .noneMatch(child -> child instanceof Button));
      screen.addProperty("resize_restored", resized);
      proof.add("screen", screen);
      JsonObject transfer = new JsonObject();
      transfer.addProperty("method", "integrated_server_MachineMenu.quickMoveStack");
      transfer.addProperty("source_menu_slot", 1);
      transfer.addProperty("source_container_slot", 2);
      transfer.addProperty("moved_magma", serverMoved);
      transfer.addProperty("output_after", serverOutputCount);
      transfer.addProperty("player_magma_after", serverInventoryCount);
      proof.add("output_transfer", transfer);
      proof.add("screenshots", CAPTURES.deepCopy());
      proof.addProperty("screenshot_method", "Minecraft Screenshot.takeScreenshot(getMainRenderTarget())");
      Files.writeString(gameDirectory.resolve("client-smelter-gui-result.json"),
         new GsonBuilder().setPrettyPrinting().create().toJson(proof) + "\n");
   }

   private static void require(boolean valid, String message) {
      if (!valid) throw new IllegalStateException(message);
   }
}
